package com.example.audit;

public record AuditEntry(
        String timestamp,
        String action,
        String performer,
        String target,
        String details
) {
    public String format() {
        return String.format("[%s] %s | %s -> %s | %s",
                timestamp, action, performer, target, details);
    }
}