package com.example;

import com.example.assignment.PermanentAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.entity.AssignmentMetadata;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.filters.RoleFilter;
import com.example.filters.RoleFilters;
import com.example.filters.UserFilter;
import com.example.filters.UserFilters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class Main {
    static void main() {

        userValidationTest();

        // проверка создания прав и валидации
        System.out.println("\nPermission create... ");

        Permission readUsers = new Permission("READ", "users", "Can view user list");
        Permission writeUsers = new Permission("WRITE", "users", "Can create and edit users");
        Permission deleteUsers = new Permission("Delete", "USERS", "Can delete users");
        Permission writeRoles = new Permission("WRITE", "ROLES", "Can create and edit roles");

        System.out.println(readUsers.format());
        System.out.println(writeUsers.format());
        System.out.println(deleteUsers.format());
        System.out.println(writeRoles.format());

        // создание ролей и проверка на уникальность имени
        System.out.println("\nRoles create... ");
        Role adminRole = Role.create("Administrator", "Full system access",
                Set.of(readUsers, writeUsers, deleteUsers, writeRoles));

        Role moderatorRole = Role.create("Moderator", "Moderator management",
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

        // создание пользователей
        System.out.println("\nUser create... ");
        User alice = new User("alice", "Alice Thymefield", "alice@example.com");
        User anton = new User("anton", "Anton Ivanov", "anton@example.com");
        User zhu = new User("zhu", "Zhu Yuan", "zhu@example.com");

        System.out.println(alice.format());
        System.out.println(anton.format());
        System.out.println(zhu.format());

        // постоянное назначение роли
        System.out.println("\nPermanent assignment... ");
        AssignmentMetadata meta1 = AssignmentMetadata.now("system", "Initial setup");
        PermanentAssignment aliceAdmin = new PermanentAssignment(alice, adminRole, meta1);

        System.out.println(aliceAdmin.summary());

        // временное назначение роли
        System.out.println("\nTemporary assignment... ");
        String antonExpiration = LocalDateTime.now()
                .plusDays(30)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        AssignmentMetadata meta2 = AssignmentMetadata.now("alice", "Cool guy");
        TemporaryAssignment antonModerator = new TemporaryAssignment(
                anton, moderatorRole, meta2, antonExpiration, true
        );

        System.out.println(antonModerator.summary());

        String charlieExpiration = LocalDateTime.now()
                .plusDays(7)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        AssignmentMetadata meta3 = AssignmentMetadata.now("alice", "Not cool guy");
        TemporaryAssignment zhuViewer = new TemporaryAssignment(
                zhu, userRole, meta3, charlieExpiration, false
        );

        System.out.println("\n" + zhuViewer.summary());

        // продление роли
        System.out.println("\nExtend check... ");
        String newExpiration = LocalDateTime.now()
                .plusDays(14)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        zhuViewer.extend(newExpiration);

        System.out.println("\n" + zhuViewer.summary());

        // проверка фильтров
        // userFilter
        System.out.println("\nFilters check... ");
        System.out.println("User filter check... ");
        UserFilter isAlice = UserFilters.byUsername("alice");
        System.out.println("Is it Alice? " + isAlice.test(alice)); // true

        UserFilter containsAn = UserFilters.byUsernameContains("AN");
        System.out.println("Is `Anton` contains `AN`? " + containsAn.test(anton)); // true

        UserFilter complexFilter = UserFilters.byUsernameContains("an")
                .and(UserFilters.byEmailDomain("@example.com"));

        System.out.println("Does Anton fit the filter? " + complexFilter.test(anton)); // true
        System.out.println("Does Alice fit the filter? " + complexFilter.test(alice)); // false

        // roleFilter
        System.out.println("\nRole filter check... ");
        RoleFilter isAdmin = RoleFilters.byNameContains("Admin");
        RoleFilter hasDbAccess = RoleFilters.hasPermission("WRITE", "users");
        RoleFilter isPowerUser = RoleFilters.hasAtLeastNPermissions(5);

        RoleFilter complexSearch = isAdmin.and(hasDbAccess).or(isPowerUser);

        if (complexSearch.test(adminRole)) {
            System.out.println("Admin role fits the filters. (Admin name && has permission WRITE users || dont have at least 5 perms)");
        }
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
