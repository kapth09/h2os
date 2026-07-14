package com.kaptheo.watering;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

public class Logger {
    private static final String ANSI_COLOR_RED      = "\033[31m";
    private static final String ANSI_COLOR_GREEN    = "\033[32m";
    private static final String ANSI_COLOR_YELLOW   = "\033[33m";
    private static final String ANSI_COLOR_BLUE     = "\033[34m";
    private static final String ANSI_COLOR_MAGENTA  = "\033[35m";
    private static final String ANSI_COLOR_CYAN     = "\033[36m";
    private static final String ANSI_COLOR_RESET    = "\033[0m";

    private static final String INFO    = "INFO";
    private static final String WARNING = "WARNING";
    private static final String ERROR   = "ERROR";

    private static final String FILE_PARENT = "logs";
    private static final String FILE_PREFIX = "watering-";

    public static String info(String msg, Object... args) {
        Date date = new Date();
        String formatted = String.format(date + " [" + ANSI_COLOR_BLUE + INFO + ANSI_COLOR_RESET + "]: " + msg, args);
        writeLogToFile(formatted);
        return formatted;
    }
    public static String warning(String msg, Object... args) {
        Date date = new Date();
        String formatted = String.format(date + " [" + ANSI_COLOR_YELLOW + WARNING + ANSI_COLOR_RESET + "]: " + msg, args);
        writeLogToFile(formatted);
        return formatted;
    }
    public static String error(String msg, Object... args) {
        Date date = new Date();
        String formatted = String.format(date + " [" + ANSI_COLOR_RED + ERROR + ANSI_COLOR_RESET + "]: " + msg, args);
        writeLogToFile(formatted);
        return formatted;
    }

    private static void writeLogToFile(String msg) {
        Path filePath = Paths.get(FILE_PARENT + "/" + FILE_PREFIX + Server.getApplicationStartTime() + ".log");
        try {
            Files.writeString(filePath, msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.printf("File %s not found\n", filePath);
        }
    }
}
