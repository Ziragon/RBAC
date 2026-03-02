package com.example.assignment;

import com.example.entity.AssignmentMetadata;
import com.example.entity.Role;
import com.example.entity.User;

import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    protected AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        this.assignmentId = generateId();
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
    }

    private static String generateId() {
        return "assign_" + UUID.randomUUID();
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    @Override
    public abstract boolean isActive();

    @Override
    public abstract String assignmentType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    @Override
    public String summary() {

        return "[" + assignmentType() + "] " +
                role.getName() +
                " assigned to " +
                user.username() +
                " by " +
                metadata.assignedBy() +
                " at " +
                metadata.assignedAt() +
                "\n" +
                "Reason: " +
                (metadata.reason() == null || metadata.reason().isBlank()
                        ? "Not specified"
                        : metadata.reason()) +
                "\n" +
                "Status: " +
                (isActive() ? "ACTIVE" : "INACTIVE");
    }
}
