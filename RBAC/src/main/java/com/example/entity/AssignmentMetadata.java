package com.example.entity;

import com.example.util.ValidationUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(
        String assignedBy,
        String assignedAt,
        String reason
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public AssignmentMetadata {
        ValidationUtils.requireNonEmpty(assignedBy, "Assigned by");
        ValidationUtils.requireNonEmpty(assignedAt, "Assigned at");
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String currentTime = LocalDateTime.now().format(ISO_FORMATTER);
        return new AssignmentMetadata(assignedBy, currentTime, reason);
    }

    public String format() {

        return "Assigned by: " + assignedBy + "\n" +
                "Assigned at: " + assignedAt + "\n" +
                "Reason: " +
                (reason == null || reason.isBlank() ? "Not specified" : reason);
    }
}
