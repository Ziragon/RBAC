package com.example.entity;

import java.util.*;

public class Role {

    private static final Set<String> usedNames = new HashSet<>();

    private final String id;

    private final String name;

    private final String description;

    private final Set<Permission> permissions;

    private Role(String id, String name, String description, Set<Permission> permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>(permissions);
    }

    public static Role create(String name, String description, Set<Permission> permissions) {
        validateName(name);
        String id = generateId();
        return new Role(id, name, description, new HashSet<>(permissions != null ? permissions : Collections.emptySet()));
    }

    private static String generateId() {
        return "role_" + UUID.randomUUID();
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя роли не может быть null или пустым");
        }
        synchronized (usedNames) {
            if (usedNames.contains(name)) {
                throw new IllegalArgumentException("Роль с именем '" + name + "' уже существует в системе");
            }
            usedNames.add(name);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void addPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        return permissions.stream()
                .anyMatch(p -> p.matches(permissionName, resource));
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", permissionsCount=" + permissions.size() +
                '}';
    }

    public String format() {
        StringBuilder sb = new StringBuilder();

        sb.append("Role: ").append(name)
                .append(" [ID: ").append(id).append("]\n");

        sb.append("Description: ")
                .append(description.isEmpty() ? "No description" : description)
                .append("\n");

        sb.append("Permissions (").append(permissions.size()).append("):\n");

        if (permissions.isEmpty()) {
            sb.append(" (none)\n");
        } else {
            permissions.stream()
                    .sorted(Comparator
                            .comparing(Permission::name)
                            .thenComparing(Permission::resource))
                    .forEach(p -> sb.append(" - ")
                            .append(p.name())
                            .append(" on ")
                            .append(p.resource())
                            .append(": ")
                            .append(p.description())
                            .append("\n"));
        }

        return sb.toString().trim();
    }
}
