package com.example.assignment;

import com.example.entity.AssignmentMetadata;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.util.DateUtils;
import com.example.util.ValidationUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TemporaryAssignment extends AbstractRoleAssignment {

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
        ValidationUtils.validateDate(date);
        return date;
    }

    public boolean isExpired() {
        return DateUtils.isPast(expiresAt);
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
        if (isExpired()) return "Expired";
        return DateUtils.formatRelativeTime(expiresAt);
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
