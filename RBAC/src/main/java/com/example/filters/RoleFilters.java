package com.example.filters;

public class RoleFilters {

    private RoleFilters() {}

    // byName удален, его замена уже прописана в RoleManager

    public static RoleFilter byNameContains(String substring) {
        return role -> role.getName().toLowerCase()
                .contains(substring.toLowerCase());
    }

    // hasPermission(Permission _) тоже самое

    public static RoleFilter hasPermission(String permissionName, String resource) {
        return role -> role.hasPermission(permissionName, resource);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) {
        return role -> role.getPermissions().size() >= n;
    }
}
