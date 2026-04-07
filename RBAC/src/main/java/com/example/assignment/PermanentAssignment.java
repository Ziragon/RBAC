package com.example.assignment;

import com.example.entity.AssignmentMetadata;
import com.example.entity.Role;
import com.example.entity.User;

public class PermanentAssignment extends AbstractRoleAssignment {

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
    }

    @Override
    public boolean isActive() {
        return !isRevoked();
    }

    @Override
    public String assignmentType() {
        return "PERMANENT";
    }
}