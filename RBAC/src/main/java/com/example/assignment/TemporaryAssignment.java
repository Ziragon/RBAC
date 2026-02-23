package com.example.assignment;

import com.example.entity.AssignmentMetadata;
import com.example.entity.Role;
import com.example.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String expiresAt;
    private boolean autoRenew;
    private boolean revoked;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = validateDate(expiresAt);
        this.autoRenew = autoRenew;
    }

    private String validateDate(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Expiration date cannot be null or empty");
        }
        try {
            LocalDateTime.parse(date, DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid date format. Expected: yyyy-MM-dd HH:mm, got: " + date
            );
        }
        return date;
    }

    public boolean isExpired() {
        return isExpired(LocalDateTime.now());
    }

    public boolean isExpired(LocalDateTime currentDate) {
        LocalDateTime expiration = LocalDateTime.parse(expiresAt, DATE_FORMATTER);
        return currentDate.isAfter(expiration);
    }

    @Override
    public boolean isActive() {
        return !isExpired() || !isRevoked();
    }

    @Override
    public void revoke() {
        this.revoked = true;
    }

    @Override
    public boolean isRevoked() {
        return revoked;
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        this.expiresAt = validateDate(newExpirationDate);
    }

    public String getTimeRemaining() {
        if (isExpired()) {
            return "Expired";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = LocalDateTime.parse(expiresAt, DATE_FORMATTER);

        long days = ChronoUnit.DAYS.between(now, expiration);
        long hours = ChronoUnit.HOURS.between(now, expiration) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, expiration) % 60;

        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();
        return baseSummary + "\n" +
                "Expires at: " + expiresAt + "\n" +
                "Auto-renew: " + (autoRenew ? "Enabled" : "Disabled") + "\n" +
                "Time remaining: " + getTimeRemaining();
    }
}
