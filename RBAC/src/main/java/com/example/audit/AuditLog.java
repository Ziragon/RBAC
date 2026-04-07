package com.example.audit;

import com.example.system.BackgroundExecutor;
import com.example.util.DateUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AuditLog {

    private static final AuditEntry POISON_PILL =
            new AuditEntry("", "", "", "", "");

    private final List<AuditEntry> entries;

    private final BlockingQueue<AuditEntry> logQueue;

    public AuditLog() {
        this.entries = new ArrayList<>();
        this.logQueue = new LinkedBlockingQueue<>();
    }

    public void startAsyncLogger(BackgroundExecutor executor) {
        executor.execute(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    AuditEntry entry = logQueue.take();

                    if (entry == POISON_PILL) {
                        break;
                    }

                    entries.add(entry);
                }
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                System.err.println("Async logger interrupted.");
            }
        });
    }

    // Немедленная остановка AuditLog (метод нужен для быстрого прохождения тестов)
    public void stop() {
        try {
            logQueue.put(POISON_PILL);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    public void log(String action, String performer, String target, String details) {
        String timestamp = DateUtils.getCurrentDateTime();
        AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);

        boolean accepted = logQueue.offer(entry);

        if (!accepted) {
            System.err.printf("[AUDIT LOSS] Queue full! %s: %s by %s on %s%n",
                    timestamp, action, performer, target);
        }
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                .filter(e -> e.performer().equalsIgnoreCase(performer))
                .toList();
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(action))
                .toList();
    }

    public List<AuditEntry> getByTarget(String target) {
        return entries.stream()
                .filter(e -> e.target().equalsIgnoreCase(target))
                .toList();
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