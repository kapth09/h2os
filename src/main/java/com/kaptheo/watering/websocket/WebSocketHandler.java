package com.kaptheo.watering.websocket;

import com.kaptheo.watering.Logger;
import com.kaptheo.watering.esp.EspState;
import com.kaptheo.watering.tasks.Task;
import com.kaptheo.watering.tasks.TaskHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private TaskHandler taskHandler;

    public void setTaskManager(TaskHandler taskHandler) {this.taskHandler = taskHandler;}

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        for (EspState state : EspState.values()) {
            Sendable msg = taskHandler.getEspState(state);
            send(session, WebMsgType.EVENT, msg, msg);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage msg) {
        String payload = msg.getPayload();
        MsgResponse request =  objectMapper.readValue(payload, MsgResponse.class);
        switch (request.type()) {
            case REQUEST -> handleRequests(session, MsgRequest.fromInt(request.option()));
            case COMMAND -> handleCommands(MsgCommand.fromInt(request.option()), request.data().toString());
        }
    }

    private void handleRequests(WebSocketSession session, MsgRequest option) {
        if (option == null) return;
        switch (option) {
            case GET_TASKS -> {
                MsgResponse.REQ_GET_TASKS req = new MsgResponse.REQ_GET_TASKS(taskHandler.getSchedule().toArray(new Task[0]));
                send(session, WebMsgType.REQUEST, req, req);
            }
            case GET_ACTIVE_TASK -> {
                MsgResponse.REQ_GET_ACTIVE_TASK req = new MsgResponse.REQ_GET_ACTIVE_TASK(taskHandler.getActiveTask(), taskHandler.getIsNextWeek());
                send(session, WebMsgType.REQUEST, req, req);
            }
            case GET_TODAYS_TASKS -> {
                MsgResponse.REQ_GET_TODAYS_TASKS req = new MsgResponse.REQ_GET_TODAYS_TASKS(taskHandler.getTodaysSchedule().toArray(new Task[0]));
                send(session, WebMsgType.REQUEST, req, req);
            }
        }
    }
    private void handleCommands(MsgCommand option, String data) {
        if (option == null) return;
        switch (option) {
            case SET_TASKS -> {
                if (data.isBlank() || data.equals("null")) return;
                Task[] newTasks = objectMapper.readValue(data, Task[].class);
                taskHandler.setSchedule(newTasks);
            }
            case SET_TODAYS_TASKS -> {
                if (data.isBlank() || data.equals("null")) return;
                Task[] newTasks = objectMapper.readValue(data, Task[].class);
                taskHandler.setTodaysSchedule(newTasks);
            }
            case START_NOW -> {
                if (data.isBlank() || data.equals("null")) return;
                MsgResponse.CMD_START_NOW cmd = objectMapper.readValue(data, MsgResponse.CMD_START_NOW.class);
                taskHandler.startNow(cmd.duration());
            }
            case STOP_NOW -> {
                taskHandler.stopWatering();
            }
            case PAUSE_TASK -> {
                taskHandler.pauseWatering();
            }
            case RESUME_TASK -> {
                taskHandler.resumeWatering();
            }
            case RELOAD_TODAYS_TASKS -> {
                taskHandler.reloadTodaysTasks();
            }
            default -> {
                System.out.println(Logger.error("Invalid CMD message: %s", option.name()));
            }
        }
    }

    private<T> void send(WebSocketSession session, WebMsgType type, Sendable option, T data) {
        MsgResponse response = new MsgResponse(type, option.optionIndex(), data);
        String responseStr = objectMapper.writeValueAsString(response);
        try {
            //System.out.println("Send " + responseStr);
            session.sendMessage(new TextMessage((responseStr)));
        } catch (IOException e) {
            System.out.println(Logger.error("Failed to send WebSocket message"));
        }
    }

    public<T> void broadcastMessage(WebMsgType type, Sendable option, T data) {
        System.out.println(Logger.info("Starting broadcast of message %s: %s", type.name(), option.optionIndex()));
        for (WebSocketSession session : sessions) {
            send(session, type, option, data);
        }
        System.out.println(Logger.info("Finished broadcast of message %s: %s", type.name(), option.optionIndex()));
    }
}