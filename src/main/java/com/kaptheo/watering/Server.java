package com.kaptheo.watering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableScheduling
public class Server {
	private static String applicationStartTime;

    public static void main(String[] args) {
		LocalDateTime now = LocalDateTime.now();
		applicationStartTime = String.format("%d-%d-%dT-%dH-%dM", now.getYear(), now.getMonth().getValue(), now.getDayOfMonth(), now.getHour(), now.getMinute());
		SpringApplication.run(Server.class, args);
	}

	public static String getApplicationStartTime() {
		return applicationStartTime;
	}
}