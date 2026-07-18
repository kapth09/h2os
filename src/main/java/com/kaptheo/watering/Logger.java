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

    private static final String FILE_PARENT = "./volume/logs";
    private static final String FILE_PREFIX = "watering-";

    private static boolean finishedInitialisation = false;

    private static String formatText(String COLOR, String TEXT, boolean writeToFile, String msg, Object... args) {
        Date date = new Date();
        String formatted = String.format(date + " [" + COLOR + TEXT + ANSI_COLOR_RESET + "]: " + msg, args);
        if (writeToFile) writeLogToFile(formatted);
        return formatted;
    }

    private static String formatException(String COLOR, String TEXT, Exception e, int printCount, boolean writeToFile, String msg, Object... args) {
        Date date = new Date();
        StringBuilder builder = new StringBuilder();
        builder.append(date + " [" + COLOR + TEXT + ANSI_COLOR_RESET + "]: ");
        builder.append(String.format(msg, args));
        builder.append("\n");
        builder.append("     ----- [" + COLOR + " Because: " + ANSI_COLOR_RESET + "] -----\n");
        builder.append("     " + e.getClass().getName() + ": " + e.getMessage() + "\n");
        builder.append("     ----- [" + COLOR + "STACKTRACE" + ANSI_COLOR_RESET + "] -----\n");
        String offset = " ".repeat(5);
        StackTraceElement[] stacktrace = e.getStackTrace();
        for (int i = 0; i < stacktrace.length && i < printCount; i++) {
            builder.append(offset);
            builder.append(stacktrace[i].toString());
            builder.append("\n");
        }
        if (writeToFile) writeLogToFile(builder.toString());
        return builder.toString();
    }

    public static String info(String msg, Object... args) {
        return formatText(ANSI_COLOR_BLUE, INFO, true, msg, args);
    }
    public static String infoStdoutOnly(String msg, Object... args) {
        return formatText(ANSI_COLOR_BLUE, INFO, false, msg, args);
    }
    public static String info(Exception exception, int printCount, String msg, Object... args) {
        return formatException(ANSI_COLOR_BLUE, INFO, exception, printCount, true, msg, args);
    }
    public static String infoStdoutOnly(Exception exception, int printCount, String msg, Object... args) {
        return formatException(ANSI_COLOR_BLUE, INFO, exception, printCount, false, msg, args);
    }

    public static String warning(String msg, Object... args) {
        return formatText(ANSI_COLOR_YELLOW, WARNING, true, msg, args);
    }
    public static String warningStdoutOnly(String msg, Object... args) {
        return formatText(ANSI_COLOR_YELLOW, WARNING, false, msg, args);
    }
    public static String warning(Exception exception, int printCount, String msg, Object... args) {
        return formatException(ANSI_COLOR_YELLOW, WARNING, exception, printCount, true, msg, args);
    }
    public static String warningStdoutOnly(Exception exception, int printCount, String msg, Object... args) {
        return formatException(ANSI_COLOR_YELLOW, WARNING, exception, printCount, false, msg, args);
    }

    public static String error(String msg, Object... args) {
        return formatText(ANSI_COLOR_RED, ERROR, true, msg, args);
    }
    public static String errorStdoutOnly(String msg, Object... args) {
        return formatText(ANSI_COLOR_RED, ERROR, false, msg, args);
    }
    public static String error(Exception exception, int printCount, String msg, Object... args) {
        return formatException(ANSI_COLOR_RED, ERROR, exception, printCount, true, msg, args);
    }
    public static String errorStdoutOnly(Exception exception, int printCount, String msg, Object... args) {
        return formatException(ANSI_COLOR_RED, ERROR, exception, printCount, false, msg, args);
    }

    private static void writeLogToFile(String msg) {
        if (!finishedInitialisation) {
            initialise();
        }
        Path filePath = Paths.get(FILE_PARENT + "/" + FILE_PREFIX + Server.getApplicationStartTime() + ".log");
        try {
            Files.writeString(filePath, msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println(Logger.errorStdoutOnly(e, 10, "Writing to log failed"));
        }
    }

    private static void initialise() {
        Path parentDirectory = Paths.get(FILE_PARENT);
        if (Files.notExists(parentDirectory)) {
            System.out.println(Logger.infoStdoutOnly("Directory %s doesn't exist", parentDirectory.toString()));
            try {
                Files.createDirectory(parentDirectory);
                System.out.println(Logger.infoStdoutOnly("Successfully created directory %s", parentDirectory.toString()));
                finishedInitialisation = true;
            } catch (IOException e) {
                System.out.println(Logger.errorStdoutOnly(e, 7, "Failed to create directory %s", parentDirectory.toString()));
            }
        } else {
            System.out.println(Logger.infoStdoutOnly("Directory %s already exists", parentDirectory.toString()));
            finishedInitialisation = true;
        }
    }
}
