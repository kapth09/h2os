package com.kaptheo.watering;

import jdk.net.ExtendedSocketOptions;
import org.apache.juli.logging.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.StandardSocketOptions;
import java.util.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;

@Component
public class EspHandler {
	public static final int TCP_SERVER_PORT = 8282;
	private boolean isWatering;
	private Selector selector;
	private ServerSocketChannel servSockChannel;
	private ByteBuffer tcpMessage;
	private SocketChannel espSocket;
	private TaskHandler taskHandler;
	private EspChecker espChecker;

	@Value("${ntfy.url}")
	private String NTFY_URL;
	private NtfyMessenger ntfyMessenger;

	public EspHandler() {
		this.tcpMessage = ByteBuffer.allocate(4);
		this.espSocket = null;
		this.espChecker = new EspChecker(5000);
		this.ntfyMessenger = new NtfyMessenger(NTFY_URL);
	}

	public void setTaskHandler(TaskHandler taskHandler) { this.taskHandler = taskHandler; }

	public boolean start() {
		try {
			selector = Selector.open();
			servSockChannel = ServerSocketChannel.open();
			servSockChannel.bind(new InetSocketAddress(TCP_SERVER_PORT));
			servSockChannel.configureBlocking(false);
			servSockChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println(Logger.info("Started TCP-Server at port %d", TCP_SERVER_PORT));
			return true;
		} catch (IOException e) {
			System.out.println(Logger.error("ESP_Handler init failed"));
			e.printStackTrace();
			return false;
		}
	}

	private void register(Selector selector, ServerSocketChannel servSockChannel) throws IOException {
		SocketChannel client = servSockChannel.accept();
		client.configureBlocking(false);
		client.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		client.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 5);
		client.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, 2);
		client.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 3);
		client.register(selector, SelectionKey.OP_READ);
		String fqHostname = client.socket().getInetAddress().getHostName();
		String[] hostname = fqHostname.split("\\.");
		espSocket = client;
		System.out.println(Logger.info("ESP connected: " + hostname[0]));
		taskHandler.espConnected();
	}

	public void listen() throws IOException {
		while (true) {
			if (espChecker.deadlineReached()) {
				System.out.println(Logger.error("Expected response of type %s was never received", espChecker.getExpectedRes()));
			}
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> iter = selectedKeys.iterator();
			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				iter.remove();
				if (key.isAcceptable()) {
					register(selector, servSockChannel);
				}
				if (key.isReadable()) {
					SocketChannel channel = (SocketChannel) key.channel();
					try {
						tcpMessage.clear();
						int bytesRead = channel.read(tcpMessage);
						tcpMessage.flip();
						int data = tcpMessage.get(0);
						ESP_MsgTypes msgType = ESP_MsgTypes.fromInt(data);
						handleTcpMsg(msgType);
						System.out.println(Logger.info("Message %s read (%d Bytes)", msgType, bytesRead));
					} catch (IOException e) {
						key.cancel();
						channel.close();
						taskHandler.espDisconnected();
						System.out.println(Logger.error("Connection to ESP lost"));
					}
				}
			}
		}
	}

	public int writeEsp(ESP_MsgTypes msgType) {
        try {
			SocketChannel esp = espSocket;
			if (esp == null) return -1;
			tcpMessage.clear();
			tcpMessage.put((byte) msgType.ordinal());
			tcpMessage.flip();
            int bytesWritten = esp.write(tcpMessage);
			System.out.println(Logger.info("Message %s(%d) written (%d Bytes)", msgType.name(), msgType.ordinal(), bytesWritten));
			espChecker.setExpectedResponse(msgType);
			return bytesWritten;
        } catch (IOException e) {
			System.out.println(Logger.error("Failed to send message %s(%d) to ESP", msgType.name(), msgType.ordinal()));
			e.printStackTrace();
        }
		return -1;
	}

	private void handleTcpMsg(ESP_MsgTypes msgType) throws IOException {
		if (!espChecker.isExpected(msgType)) {
			System.out.println(Logger.warning("Expected Message %s, but got %s", espChecker.getExpectedRes().name(), msgType.name()));
			return;
		}
		espChecker.reset();
		switch (msgType) {
			case MSG_STARTED -> {
				isWatering = true;
				System.out.println(Logger.info("ESP started"));
				taskHandler.espStarted();
			}
			case MSG_STOPPED -> {
				isWatering = false;
				System.out.println(Logger.info("ESP stopped"));
				taskHandler.espStopped();
			}
			case MSG_LEAK_DETECTED -> {
				System.out.println(Logger.warning("Water leak detected"));
				ntfyMessenger.send("Wassersensor", "Wasser ist eingedrungen");
			}
			case MSG_LEAK_RESOLVED -> {
				System.out.println(Logger.info("Water leak resolved"));
				ntfyMessenger.send("Wassersensor", "Eingedrungenes Wasser wurde beseitigt");
			}
			default -> {
				System.out.println(Logger.error("Unknown TCP message %d", msgType.ordinal()));
			}
		}
	}

	public boolean isWatering() {
		return isWatering;
	}

	private class EspChecker {
		private ESP_MsgTypes expectedRes;
		private long expectedResDeadline;
		private long expectedResDuration;
		private boolean responseResolved = false;

		public EspChecker(long expectedResDuration) {
			this.expectedResDuration = expectedResDuration;
		}

		public ESP_MsgTypes getExpectedRes() {
			return expectedRes;
		}

		public void setExpectedResponse(ESP_MsgTypes sentMsg) {
			if (sentMsg == ESP_MsgTypes.MSG_START) {
				expectedRes = ESP_MsgTypes.MSG_STARTED;
			} else if (sentMsg == ESP_MsgTypes.MSG_STOP) {
				expectedRes = ESP_MsgTypes.MSG_STOPPED;
			}
			this.expectedResDeadline = System.currentTimeMillis() + this.expectedResDuration;
			this.responseResolved = false;
		}

		public boolean isExpected(ESP_MsgTypes msg) {
			if (this.expectedRes == null) return true;
			return this.expectedRes == msg;
		}

		public void reset() {
			this.expectedRes = null;
			this.responseResolved = true;
		}

		public boolean deadlineReached() {
			if (expectedRes == null) return false;
			long timeSince = System.currentTimeMillis();
			if (timeSince > expectedResDeadline) {
				return true;
			}
			return false;
		}
	}
}