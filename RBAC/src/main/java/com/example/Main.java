package com.example;

import com.example.assignment.PermanentAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.entity.AssignmentMetadata;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.filters.*;
import com.example.repository.AssignmentManager;
import com.example.repository.RoleManager;
import com.example.repository.UserManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        PermanentAssignment aliceAdministrator = new PermanentAssignment(alice, adminRole, meta1);

        System.out.println(aliceAdministrator.summary());

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

        // assignmentFilter
        System.out.println("\nAssignment filter check... ");
        AssignmentFilter monitorFilter = AssignmentFilters.activeOnly()
                .and(AssignmentFilters.byType("TEMPORARY"))
                .and(AssignmentFilters.expiringBefore("2026-12-31 23:59"));

        System.out.println("Does AntonAssignment fit the filter? " + monitorFilter.test(antonModerator));

        // проверка репозиториев
        // userManager
        System.out.println("\nManagers check... ");
        System.out.println("UserManager check... ");
        UserManager userManager = new UserManager();

        userManager.add(alice);
        userManager.add(anton);
        userManager.add(zhu);

        userManager.findByUsername("zhu").ifPresent(u ->
                System.out.println("Found by username 'zhu': " + u.fullname())
        );

        UserFilter exampleFilter = UserFilters.byEmailDomain("@example.com");
        List<User> exampleUsers = userManager.findByFilter(exampleFilter);
        System.out.println("Users with @example.com: " + exampleUsers.size());

        System.out.println("\nTesting user update...");
        userManager.findByUsername("anton").ifPresent(user ->
                System.out.println("Before update: " + user.format())
        );

        userManager.update("anton", "Anton P. Ivanov", "anton_new@example.com");

        userManager.findByUsername("anton").ifPresent(u ->
                System.out.println("After update:  " + u.format())
        );

        // role and assignment Managers
        System.out.println("\nRole and Assignment Managers check... ");

        AssignmentManager assignmentManager = new AssignmentManager();
        RoleManager roleManager = new RoleManager(assignmentManager);

        roleManager.add(adminRole);
        roleManager.add(moderatorRole);

        assignmentManager.add(aliceAdministrator);
        assignmentManager.add(antonModerator);

        System.out.println("Alice permissions count: " + assignmentManager.getUserPermissions(alice).size());
        boolean canDelete = assignmentManager.userHasPermission(alice, "DELETE", "users");
        System.out.println("Can Alice delete users? " + canDelete);

        System.out.println("\nTesting Role Deletion Safety...");
        try {
            roleManager.remove(adminRole);
        } catch (IllegalStateException e) {
            System.out.println("Caught expected error: " + e.getMessage());
        }

        assignmentManager.remove(aliceAdministrator);
        System.out.println("\nAssignment removed for Alice.");

        if (roleManager.remove(adminRole)) {
            System.out.println("Role 'Administrator' successfully removed after revoking assignments.");
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
