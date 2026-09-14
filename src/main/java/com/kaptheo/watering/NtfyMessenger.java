package com.kaptheo.watering;

import com.kaptheo.watering.logs.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class NtfyMessenger {
    private final String uri;
    private final HttpClient httpClient;
    @Value("${ntfy.watering.baseTitle}")
    private String BASE_TITLE;

    public NtfyMessenger(@Value("${ntfy.url}") String ntfyUrl, @Value("${ntfy.topic}") String ntfyTopic) {
        this.uri = "http://" + ntfyUrl + "/" + ntfyTopic;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void send(String titleExpansion, String body) {
        String fullTitle = BASE_TITLE + ": " + titleExpansion;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Title", fullTitle)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            String execptionMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            System.out.print(Logger.error("Sending message via ntfy %s: %s, because %s", titleExpansion, body, execptionMsg));
            e.printStackTrace();
        }
    }
}