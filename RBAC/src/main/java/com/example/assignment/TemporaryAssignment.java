package com.example.assignment;

import com.example.entity.AssignmentMetadata;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.util.DateUtils;
import com.example.util.ValidationUtils;

@SuppressWarnings("java:S2160")
public class TemporaryAssignment extends AbstractRoleAssignment {

    private volatile String expiresAt;
    private volatile boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expiresAt, boolean autoRenew) {
        super(user, role, metadata);

        ValidationUtils.validateDate(expiresAt);
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    public boolean isExpired() {
        return DateUtils.isPast(expiresAt);
    }

    @Override
    public boolean isActive() {
        return !isExpired() && !isRevoked();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public void extend(String newExpirationDate) {
        ValidationUtils.validateDate(newExpirationDate);
        this.expiresAt = newExpirationDate;
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
