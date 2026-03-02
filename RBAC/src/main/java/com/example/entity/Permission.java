package com.example.entity;

import com.example.util.ValidationUtils;

public record Permission(
        String name,
        String resource,
        String description
) {

    public Permission {
        ValidationUtils.requireNonEmpty(name, "Permission name");
        ValidationUtils.requireNonEmpty(resource, "Resource");
        ValidationUtils.requireNonEmpty(description, "Description");

        name = name.trim().toUpperCase();
        resource = resource.trim().toLowerCase();
    }

    public String format() {
        return name + " on " + resource + ": " + description;
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatches = namePattern == null || name.contains(namePattern.toUpperCase());
        boolean resourceMatches = resourcePattern == null || resource.contains(resourcePattern.toLowerCase());
        return nameMatches && resourceMatches;
    }
}
