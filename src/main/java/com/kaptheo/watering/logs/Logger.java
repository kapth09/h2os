package com.kaptheo.watering.logs;

import com.kaptheo.watering.Server;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Component
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

    private static final String LOGS_ROOT_DIR = "./volume/logs/";
    private static String LOG_DIR;
    private static Path LOG_PATH;

    private static boolean finishedInitialisation = false;

    private static String formatText(String COLOR, String TEXT, boolean writeToFile, String msg, Object... args) {
        String time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        String formatted = String.format(time + " [" + COLOR + TEXT + ANSI_COLOR_RESET + "]: " + msg, args);
        if (writeToFile) writeLogToFile(formatted);
        return formatted;
    }

    private static String formatException(String COLOR, String TEXT, Exception e, int printCount, boolean writeToFile, String msg, Object... args) {
        String time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        StringBuilder builder = new StringBuilder();
        builder.append(time).append(" [").append(COLOR).append(TEXT).append(ANSI_COLOR_RESET).append("]: ");
        builder.append(String.format(msg, args));
        builder.append("\n");
        builder.append("     ----- [").append(COLOR).append(" Because: ").append(ANSI_COLOR_RESET).append("] -----\n");
        builder.append("     ").append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        builder.append("     ----- [").append(COLOR).append("STACKTRACE").append(ANSI_COLOR_RESET).append("] -----\n");
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
    public static String infoStdoutOnly(String msg, Object... args) { return formatText(ANSI_COLOR_BLUE, INFO, false, msg, args); }
    public static String info(Exception exception, int printCount, String msg, Object... args) { return formatException(ANSI_COLOR_BLUE, INFO, exception, printCount, true, msg, args); }
    public static String infoStdoutOnly(Exception exception, int printCount, String msg, Object... args) { return formatException(ANSI_COLOR_BLUE, INFO, exception, printCount, false, msg, args); }

    public static String warning(String msg, Object... args) { return formatText(ANSI_COLOR_YELLOW, WARNING, true, msg, args); }
    public static String warningStdoutOnly(String msg, Object... args) { return formatText(ANSI_COLOR_YELLOW, WARNING, false, msg, args); }
    public static String warning(Exception exception, int printCount, String msg, Object... args) { return formatException(ANSI_COLOR_YELLOW, WARNING, exception, printCount, true, msg, args); }
    public static String warningStdoutOnly(Exception exception, int printCount, String msg, Object... args) { return formatException(ANSI_COLOR_YELLOW, WARNING, exception, printCount, false, msg, args); }

    public static String error(String msg, Object... args) { return formatText(ANSI_COLOR_RED, ERROR, true, msg, args); }
    public static String errorStdoutOnly(String msg, Object... args) { return formatText(ANSI_COLOR_RED, ERROR, false, msg, args); }
    public static String error(Exception exception, int printCount, String msg, Object... args) { return formatException(ANSI_COLOR_RED, ERROR, exception, printCount, true, msg, args); }
    public static String errorStdoutOnly(Exception exception, int printCount, String msg, Object... args) { return formatException(ANSI_COLOR_RED, ERROR, exception, printCount, false, msg, args); }

    private static void writeLogToFile(String msg) {
        if (!finishedInitialisation) {
            initialise();
        }
        try {
            Files.writeString(LOG_PATH, msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println(Logger.errorStdoutOnly(e, 10, "Writing to log failed"));
        }
    }

    private static void initialise() {
        boolean rootDirExists = createDirectory(LOGS_ROOT_DIR);
        if (!rootDirExists) return;
        LOG_DIR = Server.getApplicationStartTime() + "-logs/";
        boolean logDirExists = createDirectory(LOGS_ROOT_DIR + LOG_DIR);
        if (!logDirExists) return;
        setupLogName();
        finishedInitialisation = true;
    }

    @Scheduled(cron = "0 0 0 * * *")
    private static void setupLogName() {
        String logName = LOGS_ROOT_DIR + LOG_DIR + LocalDate.now() + ".log";
        LOG_PATH = Paths.get(logName);
        Logger.infoStdoutOnly("Created new log file");
    }

    private static boolean createDirectory(String dirName) {
        Path parentDirectory = Paths.get(dirName);
        if (Files.notExists(parentDirectory)) {
            System.out.println(Logger.infoStdoutOnly("Directory %s doesn't exist", parentDirectory.toString()));
            try {
                Files.createDirectory(parentDirectory);
                System.out.println(Logger.infoStdoutOnly("Successfully created directory %s", parentDirectory.toString()));
                return true;
            } catch (IOException e) {
                System.out.println(Logger.errorStdoutOnly(e, 7, "Failed to create directory %s", parentDirectory.toString()));
                return false;
            }
        } else {
            System.out.println(Logger.infoStdoutOnly("Directory %s already exists", parentDirectory.toString()));
            return true;
        }
    }

    public static Path getLogPath() { return LOG_PATH; }
    public static String getLogDir() { return LOG_DIR; }
    public static String getFullLogDir() { return LOGS_ROOT_DIR + LOG_DIR; }
}