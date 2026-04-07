package com.example.filters;

public class UserFilters {

    private UserFilters() {}

    public static UserFilter byUsername(String username) {
        return user -> user.username().equalsIgnoreCase(username);
    }

    public static UserFilter byUsernameContains(String substring) {
        return user -> user.username().toLowerCase()
                .contains(substring.toLowerCase());
    }

    public static UserFilter byEmailContains(String email) {
        return user -> user.email().toLowerCase()
                .contains(email.toLowerCase());
    }

    public static UserFilter byEmailDomain(String domain) {
        return user -> user.email().toLowerCase()
                .endsWith(domain.toLowerCase());
    }

    public static UserFilter byFullNameContains(String substring) {
        return user -> user.fullname().toLowerCase()
                .contains(substring.toLowerCase());
    }
}