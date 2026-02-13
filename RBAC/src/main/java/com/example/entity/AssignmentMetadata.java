package com.example.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(
        String assignedBy,
        String assignedAt,
        String reason
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AssignmentMetadata {
        notNullValidation(assignedBy, assignedAt);
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String currentTime = LocalDateTime.now().format(ISO_FORMATTER);
        return new AssignmentMetadata(assignedBy, currentTime, reason);
    }

    private static void notNullValidation(String assignedBy, String assignedAt) {
        if (assignedBy == null || assignedBy.isBlank()) {
            throw new IllegalArgumentException("AssignedBy is empty");
        }

        if (assignedAt == null || assignedAt.isBlank()) {
            throw new IllegalArgumentException("AssignedAt is empty");
        }
    }

    public String format() {

        return "Assigned by: " + assignedBy + "\n" +
                "Assigned at: " + assignedAt + "\n" +
                "Reason: " +
                (reason == null || reason.isBlank() ? "Not specified" : reason);
    }
}
