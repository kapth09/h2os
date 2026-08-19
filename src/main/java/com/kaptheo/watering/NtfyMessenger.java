package com.kaptheo.watering;

import com.kaptheo.watering.logs.Logger;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class NtfyMessenger {
    @Value("${ntfy.topic}")
    private String TOPIC;
    private final String uri;
    private final HttpClient httpClient;

    public NtfyMessenger(String address) {
        this.uri = "http://" + address + "/" + TOPIC;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void send(String baseTitle, String titleExpansion, String body) {
        String fullTitle = baseTitle + ": " + titleExpansion;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Title", fullTitle)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            System.out.print(Logger.error("Sending message via ntfy %s: %s, because %s", titleExpansion, body, e.getMessage()));
            e.printStackTrace();
        }
    }
}