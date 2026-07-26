package com.kaptheo.watering.logs;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.runtime.ObjectMethods;
import java.util.Arrays;

@RestController
@RequestMapping("/api/logs")
public class LogApiController {
    private ResponseEntity<InputStreamResource> readFile(File file) {
        try  {
            InputStreamResource streamResource = new InputStreamResource(new FileInputStream(file));
            return ResponseEntity.ok()
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType("text/plain; charset=utf-8"))
                    .body(streamResource);
        } catch (IOException e) {
            Logger.error("GET request failed. File %s not found", file.toString());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/current")
    public ResponseEntity<InputStreamResource> getCurrentLog() {
        File logFile = Logger.getLogPath().toFile();
        return readFile(logFile);
    }

    @GetMapping("/list")
    public ResponseEntity<String[]> getAvailableLogs() {
        File logDir = new File(Logger.getFullLogDir());
        String[] logs = logDir.list();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(logs);
    }

    @GetMapping("/file/{logFilename}")
    public ResponseEntity<InputStreamResource> getSpecificLog(@PathVariable String logFilename) {
        File logFile = new File(Logger.getFullLogDir() + logFilename);
        return readFile(logFile);
    }
}