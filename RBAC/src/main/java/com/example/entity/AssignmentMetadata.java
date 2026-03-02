package com.example.entity;

import com.example.util.DateUtils;
import com.example.util.ValidationUtils;

public record AssignmentMetadata(
        String assignedBy,
        String assignedAt,
        String reason
) {

    public AssignmentMetadata {
        ValidationUtils.requireNonEmpty(assignedBy, "Assigned by");
        ValidationUtils.requireNonEmpty(assignedAt, "Assigned at");
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String currentTime = DateUtils.getCurrentDateTime();
        return new AssignmentMetadata(assignedBy, currentTime, reason);
    }

    public String format() {

        return "Assigned by: " + assignedBy + "\n" +
                "Assigned at: " + assignedAt + "\n" +
                "Reason: " +
                (reason == null || reason.isBlank() ? "Not specified" : reason);
    }
}
