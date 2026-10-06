package com.example.simplebrowser;

import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;

public final class BrowserLogger {

    private static final int MAX_ENTRIES = 1000;

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
        return getText("");
    }

    public static synchronized String
            getText(String filter) {

        String safeFilter =
                filter == null
                        ? ""
                        : filter.trim()
                                .toLowerCase(
                                        java.util.Locale.US);

        StringBuilder text =
                new StringBuilder();

        for (String line :
                entries) {

            if (!safeFilter.isEmpty() &&
                    !line.toLowerCase(
                            java.util.Locale.US)
                            .contains(safeFilter)) {
                continue;
            }

            if (text.length() > 0) {
                text.append("\n");
            }

            text.append(line);
        }

        return text.toString();
    }

    public static synchronized int
            getCount(String filter) {

        String safeFilter =
                filter == null
                        ? ""
                        : filter.trim()
                                .toLowerCase(
                                        java.util.Locale.US);

        if (safeFilter.isEmpty()) {
            return entries.size();
        }

        int count = 0;

        for (String line :
                entries) {
            if (line.toLowerCase(
                    java.util.Locale.US)
                    .contains(safeFilter)) {
                count++;
            }
        }

        return count;
    }

    public static synchronized void clear() {
        entries.clear();
    }
}
