package com.example.filters;

import com.example.assignment.RoleAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.util.DateUtils;

public class AssignmentFilters {

    private AssignmentFilters() {}

    // byUser и byRole уже имеются в менеджерах, смысла от них 0

    public static AssignmentFilter byUsername(String username) {
        return a -> a.user().username().equalsIgnoreCase(username);
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
        return a -> DateUtils.isAfter(a.metadata().assignedAt(), dateStr);
    }

    public static AssignmentFilter expiringBefore(String dateStr) {
        return a -> {
            if (a instanceof TemporaryAssignment temp) {
                return DateUtils.isBefore(temp.getExpiresAt(), dateStr);
            }
            return false;
        };
    }
}