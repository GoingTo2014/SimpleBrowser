package com.example.simplebrowser;

import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;

public final class BrowserLogger {

    private static final int MAX_ENTRIES = 500;

    private static final Deque<String> entries =
            new ArrayDeque<>();

    private static final DateFormat FORMAT =
            DateFormat.getTimeInstance(
                    DateFormat.MEDIUM);

    private BrowserLogger() {
    }

    public static synchronized void log(
            String category,
            String message) {

        String safeCategory =
                category == null ||
                category.trim().isEmpty()
                        ? "LOG"
                        : category.trim();

        String safeMessage =
                message == null
                        ? ""
                        : message.trim();

        if (safeMessage.length() > 2000) {
            safeMessage =
                    safeMessage.substring(
                            0,
                            2000) +
                    "...";
        }

        String line =
                "[" +
                FORMAT.format(
                        new Date()) +
                "] [" +
                safeCategory +
                "] " +
                safeMessage;

        entries.addLast(line);

        while (entries.size() >
                MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    public static synchronized List<String>
            getEntries() {

        return new ArrayList<>(
                entries);
    }

    public static synchronized String
            getText() {

        StringBuilder text =
                new StringBuilder();

        for (String line :
                entries) {

            if (text.length() > 0) {
                text.append("\n");
            }

            text.append(line);
        }

        return text.toString();
    }

    public static synchronized void clear() {
        entries.clear();
    }
}
