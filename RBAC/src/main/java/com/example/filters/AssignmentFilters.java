package com.example.filters;

import com.example.assignment.RoleAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.entity.Role;
import com.example.entity.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AssignmentFilters {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AssignmentFilter byUser(User user) {
        return a -> a.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return a -> a.user().username().equalsIgnoreCase(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return a -> a.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return a -> a.role().getName().equalsIgnoreCase(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return a -> !a.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return a -> a.assignmentType().equalsIgnoreCase(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return a -> a.metadata().assignedBy().equalsIgnoreCase(username);
    }

    public static AssignmentFilter assignedAfter(String dateStr) {
        LocalDateTime date = LocalDateTime.parse(dateStr, FORMATTER);
        return a -> {
            LocalDateTime assignedAt = LocalDateTime.parse(a.metadata().assignedAt(), FORMATTER);
            return assignedAt.isAfter(date);
        };
    }

    public static AssignmentFilter expiringBefore(String dateStr) {
        LocalDateTime limit = LocalDateTime.parse(dateStr, FORMATTER);
        return a -> {
            if (a instanceof TemporaryAssignment temp) {
                LocalDateTime expiry = LocalDateTime.parse(temp.getExpiresAt(), FORMATTER);
                return expiry.isBefore(limit);
            }
            return false;
        };
    }
}