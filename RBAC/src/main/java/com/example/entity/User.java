package com.example.entity;

import com.example.util.ValidationUtils;

import java.util.regex.Pattern;

public record User(
        String username,
        String fullname,
        String email
) {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(".+@.+\\..+");

    public User {
        ValidationUtils.validateUsername(username);
        ValidationUtils.requireNonEmpty(fullname, "Full name");
        ValidationUtils.validateEmail(email);
    }

    public String format() {
        return username + " (" + fullname + ") <" + email + ">";
    }
}
