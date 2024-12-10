package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StatsConverter {

    private static String STATS_FILE = "oversold_350.txt";

    public static void main(String[] args) {
        String text = readFile();

        String[] lines = text.split("\n");

        double oversold = 0;
        double sum = 0;

        for (String line : lines) {
            String[] lineSplit = line.split(" ");
            if (lineSplit[lineSplit.length - 1].equals("oversold")) {
                oversold++;
            }
            sum++;
        }

        System.out.println(oversold / sum);
    }

    private static String readFile() {

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader reader = Files.newBufferedReader(Paths.get(STATS_FILE), StandardCharsets.UTF_8);

            for (String line = reader.readLine(); line != null; line = reader.readLine())
                text.append(line).append("\n");

            reader.close();
        } catch (IOException e) {
            return null;
        }

        return text.toString();
    }
}
