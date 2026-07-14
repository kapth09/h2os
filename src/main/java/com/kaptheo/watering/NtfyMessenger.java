package com.kaptheo.watering;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class NtfyMessenger {
    @Value("${ntfy.topic}")
    private String TOPIC;
    private String uri;
    private final String BASE_TITLE = "Bewaesserung";
    private HttpClient httpClient;

    public NtfyMessenger(String address) {
        this.uri = "http://" + address + "/" + TOPIC;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public HttpResponse<String> send(String titleExpansion, String body) {
        String fullTitle = BASE_TITLE + ": " + titleExpansion;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Title", fullTitle)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            System.out.print(Logger.error("Failed to send message via ntfy: %s: %s", titleExpansion, body));
            e.printStackTrace();
        }
        return null;
    }
}