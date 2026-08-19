package com.kaptheo.watering.esp;

import com.kaptheo.watering.logs.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class EspRunner implements CommandLineRunner {
    private final EspHandler espHandler;

    public EspRunner(EspHandler espHandler) {
        this.espHandler = espHandler;
    }

    @Override
    public void run(String @NonNull ... args) {
        boolean initialized = espHandler.start();
        if (!initialized) {
            return;
        }
        Thread espThread = new Thread(() -> {
            try {
                espHandler.listen();
            } catch (IOException e) {
                System.err.println(Logger.error("Error in EspHandler listen"));
                e.printStackTrace();
            }
        });

        espThread.setName("ESP-HANDLER-THREAD");
        espThread.setDaemon(true);
        espThread.start();
        System.out.println(Logger.info("Spawned %s", espThread.getName()));
    }
}
