package com.example.entity;

import java.util.regex.Pattern;

public record Permission(
        String name,
        String resource,
        String description
) {
    private static final Pattern NO_SPACES_PATTERN = Pattern.compile("^\\S+$");

    public Permission {
        notNullValidation(name, resource, description);

        if (!NO_SPACES_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Name cannot contain spaces");
        }

        name = name.trim().toUpperCase();
        resource = resource.trim().toLowerCase();
    }

    private static void notNullValidation(String name, String resource, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is empty");
        }

        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("Resource is empty");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is empty");
        }
    }

    public String format() {
        return name + " on " + resource + ": " + description;
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches = namePattern == null || name.contains(namePattern);
        boolean resourceMatches = resourcePattern == null || resource.contains(resourcePattern);
        return nameMatches && resourceMatches;
    }
}
