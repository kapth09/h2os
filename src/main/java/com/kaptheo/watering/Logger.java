package com.kaptheo.watering;

import org.slf4j.LoggerFactory;

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

    private static final String FILE_PARENT = "./volume/logs";
    private static final String FILE_PREFIX = "watering-";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Logger.class);

    private static boolean logfileError = false;

    private static String formatText(String COLOR, String TEXT, String msg, Object... args) {
        Date date = new Date();
        String formatted = String.format(date + " [" + COLOR + TEXT + ANSI_COLOR_RESET + "]: " + msg, args);
        writeLogToFile(formatted);
        return formatted;
    }

    private static String formatStacktrace(String COLOR, String TEXT, StackTraceElement[] stacktrace, int printCount, String msg, Object... args) {
        Date date = new Date();
        StringBuilder builder = new StringBuilder();
        builder.append(date + " [" + COLOR + TEXT + ANSI_COLOR_RESET + "]: ");
        builder.append(String.format(msg, args));
        builder.append("\n");
        builder.append("     ----- [" + COLOR + "STACKTRACE" + ANSI_COLOR_RESET + "] ----- \n");
        String offset = " ".repeat(5);
        for (int i = 0; i < stacktrace.length && i < printCount; i++) {
            builder.append(offset);
            builder.append(stacktrace[i].toString());
            builder.append("\n");
        }
        writeLogToFile(builder.toString());
        return builder.toString();
    }

    public static String info(String msg, Object... args) {
        return formatText(ANSI_COLOR_BLUE, INFO, msg, args);
    }
    public static String info(StackTraceElement[] stacktrace, int printCount, String msg, Object... args) {
        return formatStacktrace(ANSI_COLOR_BLUE, INFO, stacktrace, printCount, msg, args);
    }

    public static String warning(String msg, Object... args) {
        return formatText(ANSI_COLOR_YELLOW, WARNING, msg, args);
    }
    public static String warning(StackTraceElement[] stacktrace, int printCount, String msg, Object... args) {
        return formatStacktrace(ANSI_COLOR_YELLOW, WARNING, stacktrace, printCount, msg, args);
    }

    public static String error(String msg, Object... args) {
        return formatText(ANSI_COLOR_RED, ERROR, msg, args);
    }
    public static String error(StackTraceElement[] stacktrace, int printCount, String msg, Object... args) {
        return formatStacktrace(ANSI_COLOR_RED, ERROR, stacktrace, printCount, msg, args);
    }

    public static String errorOnly(String msg, Object... args) {
        Date date = new Date();
        return String.format(date + " [" + ANSI_COLOR_RED + ERROR + ANSI_COLOR_RESET + "]: " + msg, args);
    }

    private static void writeLogToFile(String msg) {
        Path filePath = Paths.get(FILE_PARENT + "/" + FILE_PREFIX + Server.getApplicationStartTime() + ".log");
        try {
            Files.writeString(filePath, msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            if (!logfileError) {
                System.out.println(Logger.errorOnly("File %s not found", filePath));
                logfileError = true;
            }
        }
    }
}
