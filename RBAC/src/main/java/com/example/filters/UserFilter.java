package com.example.filters;

import com.example.entity.User;

@FunctionalInterface
public interface UserFilter {
    boolean test(User user);

    // комбинирование фильтров
    default UserFilter and(UserFilter other) {
        return user -> this.test(user) && other.test(user);
    }

    default UserFilter or(UserFilter other) {
        return user -> this.test(user) || other.test(user);
    }
}
