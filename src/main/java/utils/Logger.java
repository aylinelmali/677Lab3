package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String STATS_FILE = "trading_post_stats.txt";

    // Instance-specific log file
    private static String logFile = "";

    // Date and time formatter
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");

    public Logger(String logFileName) {
        logFile = logFileName;
    }

    // Method to log messages with a timestamp
    public synchronized static void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = timestamp + " " + message;

        // Print to console
        System.out.println(logMessage);

        // Append the message to the log file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }

    }

    public synchronized static void logStats(String message) {

        // Print to console
        System.out.println(message);

        // Append the message to the log file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STATS_FILE, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }

    }
}
