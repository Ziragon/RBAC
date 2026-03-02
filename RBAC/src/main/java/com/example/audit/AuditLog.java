package com.example.audit;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<AuditEntry> entries;

    public AuditLog() {
        this.entries = new ArrayList<>();
    }

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);
        entries.add(entry);
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                .filter(e -> e.performer().equalsIgnoreCase(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByTarget(String target) {
        return entries.stream()
                .filter(e -> e.target().equalsIgnoreCase(target))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getRecent(int count) {
        int size = entries.size();
        int from = Math.max(0, size - count);
        return new ArrayList<>(entries.subList(from, size));
    }

    public int count() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }

        System.out.println("\n=== AUDIT LOG ===\n");
        System.out.printf("%-20s %-15s %-15s %-20s %s%n",
                "TIMESTAMP", "ACTION", "PERFORMER", "TARGET", "DETAILS");
        System.out.println("-".repeat(90));

        for (AuditEntry entry : entries) {
            System.out.printf("%-20s %-15s %-15s %-20s %s%n",
                    entry.timestamp(),
                    truncate(entry.action(), 15),
                    truncate(entry.performer(), 15),
                    truncate(entry.target(), 20),
                    entry.details());
        }

        System.out.println("\nTotal: " + entries.size() + " entries");
    }

    public void saveToFile(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("TIMESTAMP,ACTION,PERFORMER,TARGET,DETAILS");

            for (AuditEntry entry : entries) {
                writer.printf("%s,%s,%s,%s,%s%n",
                        entry.timestamp(),
                        entry.action(),
                        entry.performer(),
                        entry.target(),
                        entry.details().replace(",", ";"));
            }
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}