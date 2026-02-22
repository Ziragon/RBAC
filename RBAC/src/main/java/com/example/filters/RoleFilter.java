package com.example.filters;

import com.example.entity.Role;

@FunctionalInterface
public interface RoleFilter {
    boolean test(Role role);

    // комбинирование фильтров
    default RoleFilter and(RoleFilter other) {
        return role -> this.test(role) && other.test(role);
    }

    default RoleFilter or(RoleFilter other) {
        return role -> this.test(role) || other.test(role);
    }
}