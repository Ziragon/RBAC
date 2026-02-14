package com.example.assignment;

import com.example.entity.AssignmentMetadata;
import com.example.entity.Role;
import com.example.entity.User;

public class PermanentAssignment extends AbstractRoleAssignment {

    private boolean revoked;

    public PermanentAssignment(User user, Role role, AssignmentMetadata metadata) {
        super(user, role, metadata);
        this.revoked = false;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public String assignmentType() {
        return "";
    }
}
