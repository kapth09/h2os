package com.kaptheo.watering.esp;

import com.kaptheo.watering.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class EspRunner implements CommandLineRunner {
    private EspHandler espHandler;

    public EspRunner(EspHandler espHandler) {
        this.espHandler = espHandler;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean initialized = espHandler.start();
        if (initialized == false) {
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
