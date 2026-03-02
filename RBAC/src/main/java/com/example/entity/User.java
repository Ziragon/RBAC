package com.example.entity;

import com.example.util.ValidationUtils;

public record User(
        String username,
        String fullname,
        String email
) {
    public User {
        ValidationUtils.validateUsername(username);
        ValidationUtils.requireNonEmpty(fullname, "Full name");
        ValidationUtils.validateEmail(email);
    }

    public String format() {
        return username + " (" + fullname + ") <" + email + ">";
    }
}
