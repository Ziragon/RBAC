package com.example;

import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;

import java.util.Set;

public class Main {
    static void main() {

        userValidationTest();

        System.out.println("\nPermission create... ");

        Permission readUsers = new Permission("READ", "users", "Can view user list");
        Permission writeUsers = new Permission("WRITE", "users", "Can create and edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");
        Permission writeRoles = new Permission("WRITE", "ROLES", "Can create and edit roles");

        System.out.println(readUsers.format());
        System.out.println(writeUsers.format());
        System.out.println(deleteUsers.format());
        System.out.println(writeRoles.format());

        System.out.println("\nRoles create... ");
        Role adminRole = Role.create("Administrator", "Full system access",
                Set.of(readUsers, writeUsers, deleteUsers, writeRoles));

        Role moderatorRole = Role.create("moderator", "Moderator management",
                Set.of(readUsers, writeUsers));

        Role userRole = Role.create("User", "Read-only access",
                Set.of(readUsers));

        System.out.println("\n" + adminRole.format());
        System.out.println("\n" + moderatorRole.format());
        System.out.println("\n" + userRole.format());

        System.out.println("\nUnique name test... ");

        try {
            Role.create("Administrator", "Zhu Yuan", Set.of());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("\nUser create... ");
        User alice = new User("alice", "Alice Thymefield", "alice@example.com");
        User bob = User.create("anton", "bob@company.com");
        User charlie = User.create("charlie", "charlie@company.com");
    }

    private static void userValidationTest() {

        System.out.println("\nValidation test: ");

        User user1 = new User("zhu_yuan", "Zhu Yuan", "email@example.com");
        System.out.println(user1.format());

        // null username
        try {
            new User(null, "Zhu Yuan", "email@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // короткий username
        try {
            new User("zu", "Zhu Yuan", "email@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // неразрешенные символы в username
        try {
            new User("юзер", "Zhu Yuan", "email@example.com");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // email без точки после @
        try {
            new User("zhu_yuan", "Zhu Yuan", "email@example");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
