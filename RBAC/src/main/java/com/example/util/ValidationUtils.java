package com.example.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PERMISSION_NAME_PATTERN = Pattern.compile("^[A-Z_]{2,20}$");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^[a-z_]{2,30}$");

    private ValidationUtils() { }

    public static boolean isValidUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidDate(String date) {
        if (date == null || date.isBlank()) return false;
        try {
            DateUtils.isFuture(date);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isFutureDate(String date) {
        if (!isValidDate(date)) return false;
        return DateUtils.isFuture(date);
    }

    public static boolean isValidPermissionName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return PERMISSION_NAME_PATTERN.matcher(name.toUpperCase()).matches();
    }

    public static boolean isValidResource(String resource) {
        if (resource == null || resource.isBlank()) {
            return false;
        }
        return RESOURCE_PATTERN.matcher(resource.toLowerCase()).matches();
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }

    // -- Методы валидации для классов --

    public static void validateUsername(String username) {
        requireNonEmpty(username, "Username");
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException(
                    "Invalid username: must be 3-20 characters, only letters, digits, and underscores"
            );
        }
    }

    public static void validateEmail(String email) {
        requireNonEmpty(email, "Email");
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
    }

    public static void validateDate(String date) {
        requireNonEmpty(date, "Date");
        if (!isValidDate(date)) {
            throw new IllegalArgumentException(
                    "Invalid date format. Expected: yyyy-MM-dd HH:mm, got: " + date
            );
        }
    }

    public static void validateFutureDate(String date) {
        validateDate(date);
        if (!isFutureDate(date)) {
            throw new IllegalArgumentException("Date must be in the future: " + date);
        }
    }
}