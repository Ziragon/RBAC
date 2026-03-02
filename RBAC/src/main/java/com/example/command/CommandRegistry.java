package com.example.command;

import com.example.assignment.PermanentAssignment;
import com.example.assignment.RoleAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.audit.AuditEntry;
import com.example.entity.AssignmentMetadata;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.filters.AssignmentFilters;
import com.example.filters.RoleFilters;
import com.example.filters.UserFilters;
import com.example.util.ConsoleHelper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandRegistry {

    private final CommandParser parser;

    public CommandRegistry(CommandParser parser) {
        this.parser = parser;
    }

    // Регистрация всех команд
    public void registerAll() {
        registerUserCommands();
        registerRoleCommands();
        registerAssignmentCommands();
        registerPermissionCommands();
        registerSystemCommands();
    }

    // -- User Commands --

    private void registerUserCommands() {

        // user-list
        parser.registerCommand("user-list", "List all users", (scanner, system) -> {
            ConsoleHelper.printHeader("User List");
            List<User> users = system.getUserManager().findAll();

            if (users.isEmpty()) {
                ConsoleHelper.printInfo("No users found.");
                return;
            }

            System.out.printf("%-15s %-25s %-30s%n", "USERNAME", "FULL NAME", "EMAIL");
            ConsoleHelper.printSeparator();

            for (User user : users) {
                System.out.printf("%-15s %-25s %-30s%n",
                        user.username(), user.fullname(), user.email());
            }

            System.out.println("\nTotal: " + users.size() + " user(s)");
        });

        // user-create
        parser.registerCommand("user-create", "Create a new user", (scanner, system) -> {
            ConsoleHelper.printHeader("Create User");

            String username = ConsoleHelper.promptUsername(scanner, "Username");
            String fullName = ConsoleHelper.promptNonEmpty(scanner, "Full name");
            String email = ConsoleHelper.promptEmail(scanner, "Email");

            try {
                User user = new User(username, fullName, email);
                system.getUserManager().add(user);
                system.log("USER_CREATE", username, "Created user: " + fullName);
                ConsoleHelper.printSuccess("User '" + username + "' created successfully.");
            } catch (IllegalArgumentException e) {
                ConsoleHelper.printError(e.getMessage());
            }
        });

        // user-view
        parser.registerCommand("user-view", "View user details", (scanner, system) -> {
            ConsoleHelper.printHeader("User Details");

            String username = ConsoleHelper.promptUsername(scanner, "Username");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            User user = userOpt.get();
            System.out.println("\n" + user.format());

            // Показываем назначенные роли
            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);

            System.out.println("\nAssigned Roles:");
            if (assignments.isEmpty()) {
                System.out.println("  (none)");
            } else {
                for (RoleAssignment a : assignments) {
                    String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                    System.out.printf("  - %s [%s] (%s)%n",
                            a.role().getName(), a.assignmentType(), status);
                }
            }

            // Показываем все права
            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(user);
            System.out.println("\nEffective Permissions:");
            if (permissions.isEmpty()) {
                System.out.println("  (none)");
            } else {
                permissions.forEach(p -> System.out.println("  - " + p.format()));
            }
        });

        // user-update
        parser.registerCommand("user-update", "Update user data", (scanner, system) -> {
            ConsoleHelper.printHeader("Update User");

            String username = ConsoleHelper.promptUsername(scanner, "Username to update");

            if (!system.getUserManager().exists(username)) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            String newFullName = ConsoleHelper.promptNonEmpty(scanner, "New full name");
            String newEmail = ConsoleHelper.promptEmail(scanner, "New email");

            try {
                system.getUserManager().update(username, newFullName, newEmail);
                system.log("USER_UPDATE", username, "Updated user, new full name: " + newFullName + ", new email: " + newEmail);
                ConsoleHelper.printSuccess("User updated successfully.");
            } catch (Exception e) {
                ConsoleHelper.printError(e.getMessage());
            }
        });

        // user-delete
        parser.registerCommand("user-delete", "Delete a user", (scanner, system) -> {
            ConsoleHelper.printHeader("Delete User");

            String username = ConsoleHelper.promptUsername(scanner, "Username to delete");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            User user = userOpt.get();

            if (!ConsoleHelper.confirm(scanner, "Are you sure you want to delete '" + username + "'?")) {
                ConsoleHelper.printInfo("Operation cancelled.");
                return;
            }

            // Удаляем все назначения пользователя
            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(user);
            for (RoleAssignment a : assignments) {
                system.getAssignmentManager().remove(a);
            }

            system.getUserManager().remove(user);
            system.log("USER_DELETE", username, "Deleted user with " + assignments.size() + " assignments");
            ConsoleHelper.printSuccess("User '" + username + "' deleted. " +
                    assignments.size() + " assignment(s) removed.");
        });

        // user-search
        parser.registerCommand("user-search", "Search users by filter", (scanner, system) -> {
            ConsoleHelper.printHeader("Search Users");

            int choice = ConsoleHelper.showMenu(scanner, "Select filter",
                    "By username (contains)",
                    "By email (contains)",
                    "By email domain",
                    "By full name (contains)");

            String query = ConsoleHelper.promptNonEmpty(scanner, "Enter search query");
            List<User> results;

            switch (choice) {
                case 1 -> results = system.getUserManager()
                        .findByFilter(UserFilters.byUsernameContains(query));
                case 2 -> results = system.getUserManager()
                        .findByFilter(u -> u.email().toLowerCase().contains(query.toLowerCase()));
                case 3 -> results = system.getUserManager()
                        .findByFilter(UserFilters.byEmailDomain(query));
                case 4 -> results = system.getUserManager()
                        .findByFilter(UserFilters.byFullNameContains(query));
                default -> {
                    ConsoleHelper.printError("Invalid option");
                    return;
                }
            }

            if (results.isEmpty()) {
                ConsoleHelper.printInfo("No users found.");
            } else {
                System.out.println("\nFound " + results.size() + " user(s):");
                results.forEach(u -> System.out.println("  - " + u.format()));
            }
        });
    }

    // -- Role commands --

    private void registerRoleCommands() {

        // role-list
        parser.registerCommand("role-list", "List all roles", (scanner, system) -> {
            ConsoleHelper.printHeader("Role List");
            List<Role> roles = system.getRoleManager().findAll();

            if (roles.isEmpty()) {
                ConsoleHelper.printInfo("No roles found.");
                return;
            }

            System.out.printf("%-20s %-15s %-30s%n", "NAME", "PERMISSIONS", "ID");
            ConsoleHelper.printSeparator();

            for (Role role : roles) {
                System.out.printf("%-20s %-15d %-30s%n",
                        role.getName(), role.getPermissions().size(), role.getId());
            }

            System.out.println("\nTotal: " + roles.size() + " role(s)");
        });

        // role-create
        parser.registerCommand("role-create", "Create a new role", (scanner, system) -> {
            ConsoleHelper.printHeader("Create Role");

            String name = ConsoleHelper.promptUsername(scanner, "Role name");
            String description = ConsoleHelper.prompt(scanner, "Description");

            try {
                Role role = Role.create(name, description, Set.of());
                system.getRoleManager().add(role);
                system.log("ROLE_CREATE", name, "Created role with description: " + description);
                ConsoleHelper.printSuccess("Role '" + name + "' created.");

                // Предлагаем добавить права
                while (ConsoleHelper.confirm(scanner, "Add a permission?")) {
                    String permName = ConsoleHelper.promptNonEmpty(scanner, "Permission name (e.g., READ)");
                    String resource = ConsoleHelper.promptNonEmpty(scanner, "Resource (e.g., users)");
                    String permDesc = ConsoleHelper.promptNonEmpty(scanner, "Permission description");

                    try {
                        Permission perm = new Permission(permName, resource, permDesc);
                        system.log("ROLE_MODIFY", name, "Added permission: " + permName + ":" + resource);
                        role.addPermission(perm);
                        ConsoleHelper.printSuccess("Permission added.");
                    } catch (IllegalArgumentException e) {
                        ConsoleHelper.printError(e.getMessage());
                    }
                }

            } catch (IllegalArgumentException e) {
                ConsoleHelper.printError(e.getMessage());
            }
        });

        // role-view
        parser.registerCommand("role-view", "View role details", (scanner, system) -> {
            ConsoleHelper.printHeader("Role Details");

            String name = ConsoleHelper.promptNonEmpty(scanner, "Role name");
            Optional<Role> roleOpt = system.getRoleManager().findByName(name);

            if (roleOpt.isEmpty()) {
                ConsoleHelper.printError("Role not found: " + name);
                return;
            }

            System.out.println("\n" + roleOpt.get().format());
        });

        // role-delete
        parser.registerCommand("role-delete", "Delete a role", (scanner, system) -> {
            ConsoleHelper.printHeader("Delete Role");

            String name = ConsoleHelper.promptNonEmpty(scanner, "Role name to delete");
            Optional<Role> roleOpt = system.getRoleManager().findByName(name);

            if (roleOpt.isEmpty()) {
                ConsoleHelper.printError("Role not found: " + name);
                return;
            }

            Role role = roleOpt.get();

            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(role);
            if (!assignments.isEmpty()) {
                ConsoleHelper.printWarning("This role is assigned to " + assignments.size() + " user(s):");
                assignments.forEach(a -> System.out.println("  - " + a.user().username()));
            }

            if (!ConsoleHelper.confirm(scanner, "Are you sure you want to delete role '" + name + "'?")) {
                ConsoleHelper.printInfo("Operation cancelled.");
                return;
            }

            for (RoleAssignment a : assignments) {
                system.getAssignmentManager().remove(a);
            }

            try {
                system.getRoleManager().remove(role);
                system.log("ROLE_DELETE", name, "Deleted role with " + assignments.size() + " assignments");
                ConsoleHelper.printSuccess("Role '" + name + "' deleted.");
            } catch (IllegalStateException e) {
                ConsoleHelper.printError(e.getMessage());
            }
        });

        // role-add-permission
        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, system) -> {
            ConsoleHelper.printHeader("Add Permission to Role");

            String roleName = ConsoleHelper.promptNonEmpty(scanner, "Role name");
            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);

            if (roleOpt.isEmpty()) {
                ConsoleHelper.printError("Role not found: " + roleName);
                return;
            }

            String permName = ConsoleHelper.promptNonEmpty(scanner, "Permission name");
            String resource = ConsoleHelper.promptNonEmpty(scanner, "Resource");
            String description = ConsoleHelper.promptNonEmpty(scanner, "Description");

            try {
                Permission perm = new Permission(permName, resource, description);
                roleOpt.get().addPermission(perm);
                system.log("ROLE_MODIFY", roleName, "Added permission: " + permName + ":" + resource);
                ConsoleHelper.printSuccess("Permission added to role '" + roleName + "'.");
            } catch (IllegalArgumentException e) {
                ConsoleHelper.printError(e.getMessage());
            }
        });

        // role-remove-permission
        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, system) -> {
            ConsoleHelper.printHeader("Remove Permission from Role");

            String roleName = ConsoleHelper.promptNonEmpty(scanner, "Role name");
            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);

            if (roleOpt.isEmpty()) {
                ConsoleHelper.printError("Role not found: " + roleName);
                return;
            }

            Role role = roleOpt.get();
            List<Permission> permissions = role.getPermissions().stream().toList();

            if (permissions.isEmpty()) {
                ConsoleHelper.printInfo("This role has no permissions.");
                return;
            }

            System.out.println("\nPermissions:");
            for (int i = 0; i < permissions.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + permissions.get(i).format());
            }

            int choice = ConsoleHelper.promptInt(scanner, "Select permission to remove", 1, permissions.size());
            Permission toRemove = permissions.get(choice - 1);
            role.removePermission(toRemove);
            system.log("ROLE_MODIFY", roleName, "Removed permission: " + toRemove.name() + ":" + toRemove.resource());
            ConsoleHelper.printSuccess("Permission removed.");
        });

        // role-search
        parser.registerCommand("role-search", "Search roles", (scanner, system) -> {
            ConsoleHelper.printHeader("Search Roles");

            int choice = ConsoleHelper.showMenu(scanner, "Select filter",
                    "By name (contains)",
                    "By permission",
                    "By minimum permissions count");

            List<Role> results;

            switch (choice) {
                case 1 -> {
                    String query = ConsoleHelper.promptNonEmpty(scanner, "Name contains");
                    results = system.getRoleManager().findByFilter(RoleFilters.byNameContains(query));
                }
                case 2 -> {
                    String permName = ConsoleHelper.promptNonEmpty(scanner, "Permission name");
                    String resource = ConsoleHelper.promptNonEmpty(scanner, "Resource");
                    results = system.getRoleManager().findByFilter(RoleFilters.hasPermission(permName, resource));
                }
                case 3 -> {
                    int min = ConsoleHelper.promptInt(scanner, "Minimum permissions", 1, 100);
                    results = system.getRoleManager().findByFilter(RoleFilters.hasAtLeastNPermissions(min));
                }
                default -> {
                    ConsoleHelper.printError("Invalid option");
                    return;
                }
            }

            if (results.isEmpty()) {
                ConsoleHelper.printInfo("No roles found.");
            } else {
                System.out.println("\nFound " + results.size() + " role(s):");
                results.forEach(r -> System.out.println("  - " + r.getName() +
                        " (" + r.getPermissions().size() + " permissions)"));
            }
        });
    }

    // -- Assignment commands --

    private void registerAssignmentCommands() {

        // assign-role
        parser.registerCommand("assign-role", "Assign role to user", (scanner, system) -> {
            ConsoleHelper.printHeader("Assign Role");

            String username = ConsoleHelper.promptNonEmpty(scanner, "Username");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            User user = userOpt.get();

            // Показываем доступные роли
            List<Role> roles = system.getRoleManager().findAll();
            if (roles.isEmpty()) {
                ConsoleHelper.printError("No roles available.");
                return;
            }

            System.out.println("\nAvailable roles:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + roles.get(i).getName());
            }

            int roleChoice = ConsoleHelper.promptInt(scanner, "Select role", 1, roles.size());
            Role role = roles.get(roleChoice - 1);

            int typeChoice = ConsoleHelper.showMenu(scanner, "Assignment type",
                    "Permanent",
                    "Temporary");

            String reason = ConsoleHelper.prompt(scanner, "Reason for assignment");
            AssignmentMetadata metadata = system.createMetadata(reason);

            try {
                if (typeChoice == 1) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    system.getAssignmentManager().add(assignment);
                } else {
                    String expiration = ConsoleHelper.promptFutureDate(scanner,
                            "Expiration date (yyyy-MM-dd HH:mm)");
                    boolean autoRenew = ConsoleHelper.confirm(scanner, "Enable auto-renew?");
                    TemporaryAssignment assignment = new TemporaryAssignment(
                            user, role, metadata, expiration, autoRenew);
                    system.getAssignmentManager().add(assignment);
                }
                system.log("ROLE_ASSIGN", username, "Assigned role: " + role.getName());
                ConsoleHelper.printSuccess("Role '" + role.getName() + "' assigned to '" + username + "'.");
            } catch (Exception e) {
                ConsoleHelper.printError(e.getMessage());
            }
        });

        // revoke-role
        parser.registerCommand("revoke-role", "Revoke role from user", (scanner, system) -> {
            ConsoleHelper.printHeader("Revoke Role");

            String username = ConsoleHelper.promptUsername(scanner, "Username");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get())
                    .stream().filter(RoleAssignment::isActive).toList();

            if (assignments.isEmpty()) {
                ConsoleHelper.printInfo("No active assignments for this user.");
                return;
            }

            System.out.println("\nActive assignments:");
            for (int i = 0; i < assignments.size(); i++) {
                RoleAssignment a = assignments.get(i);
                System.out.println("  " + (i + 1) + ". " + a.role().getName() + " [" + a.assignmentType() + "]");
            }

            int choice = ConsoleHelper.promptInt(scanner, "Select assignment to revoke", 1, assignments.size());
            RoleAssignment toRevoke = assignments.get(choice - 1);
            toRevoke.revoke();
            system.log("ROLE_REVOKE", username, "Revoked role: " + toRevoke.role().getName());
            ConsoleHelper.printSuccess("Assignment revoked.");
        });

        // assignment-list
        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) -> {
            ConsoleHelper.printHeader("All Assignments");

            List<RoleAssignment> assignments = system.getAssignmentManager().findAll();

            if (assignments.isEmpty()) {
                ConsoleHelper.printInfo("No assignments found.");
                return;
            }

            System.out.printf("%-15s %-20s %-12s %-10s %-20s%n",
                    "USER", "ROLE", "TYPE", "STATUS", "ASSIGNED AT");
            ConsoleHelper.printSeparator();

            for (RoleAssignment a : assignments) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("%-15s %-20s %-12s %-10s %-20s%n",
                        a.user().username(),
                        a.role().getName(),
                        a.assignmentType(),
                        status,
                        a.metadata().assignedAt().substring(0, 16));
            }

            System.out.println("\nTotal: " + assignments.size() + " assignment(s)");
        });

        // assignment-list-user
        parser.registerCommand("assignment-list-user", "List assignments for user", (scanner, system) -> {
            ConsoleHelper.printHeader("User Assignments");

            String username = ConsoleHelper.promptUsername(scanner, "Username");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByUser(userOpt.get());

            if (assignments.isEmpty()) {
                ConsoleHelper.printInfo("No assignments for this user.");
                return;
            }

            for (RoleAssignment a : assignments) {
                System.out.println("\n" + a.summary());
                ConsoleHelper.printSeparator();
            }
        });

        // assignment-list-role
        parser.registerCommand("assignment-list-role", "List users with role", (scanner, system) -> {
            ConsoleHelper.printHeader("Role Assignments");

            String roleName = ConsoleHelper.promptNonEmpty(scanner, "Role name");
            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);

            if (roleOpt.isEmpty()) {
                ConsoleHelper.printError("Role not found: " + roleName);
                return;
            }

            List<RoleAssignment> assignments = system.getAssignmentManager().findByRole(roleOpt.get());

            if (assignments.isEmpty()) {
                ConsoleHelper.printInfo("No users have this role.");
                return;
            }

            System.out.println("\nUsers with role '" + roleName + "':");
            for (RoleAssignment a : assignments) {
                String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                System.out.printf("  - %-15s [%s] %s%n",
                        a.user().username(), a.assignmentType(), status);
            }
        });

        // assignment-active
        parser.registerCommand("assignment-active", "List active assignments", (scanner, system) -> {
            ConsoleHelper.printHeader("Active Assignments");

            List<RoleAssignment> active = system.getAssignmentManager().getActiveAssignments();

            if (active.isEmpty()) {
                ConsoleHelper.printInfo("No active assignments.");
                return;
            }

            for (RoleAssignment a : active) {
                System.out.printf("  - %s -> %s [%s]%n",
                        a.user().username(), a.role().getName(), a.assignmentType());
            }

            System.out.println("\nTotal: " + active.size() + " active assignment(s)");
        });

        // assignment-expired
        parser.registerCommand("assignment-expired", "List expired assignments", (scanner, system) -> {
            ConsoleHelper.printHeader("Expired Assignments");

            List<RoleAssignment> expired = system.getAssignmentManager().getExpiredAssignments();

            if (expired.isEmpty()) {
                ConsoleHelper.printInfo("No expired assignments.");
                return;
            }

            for (RoleAssignment a : expired) {
                if (a instanceof TemporaryAssignment temp) {
                    System.out.printf("  - %s -> %s (expired: %s)%n",
                            a.user().username(), a.role().getName(), temp.getExpiresAt());
                }
            }

            System.out.println("\nTotal: " + expired.size() + " expired assignment(s)");
        });

        // assignment-extend
        parser.registerCommand("assignment-extend", "Extend temporary assignment", (scanner, system) -> {
            ConsoleHelper.printHeader("Extend Assignment");

            String username = ConsoleHelper.promptNonEmpty(scanner, "Username");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            List<RoleAssignment> tempAssignments = system.getAssignmentManager()
                    .findByUser(userOpt.get()).stream()
                    .filter(a -> a instanceof TemporaryAssignment)
                    .toList();

            if (tempAssignments.isEmpty()) {
                ConsoleHelper.printInfo("No temporary assignments for this user.");
                return;
            }

            System.out.println("\nTemporary assignments:");
            for (int i = 0; i < tempAssignments.size(); i++) {
                TemporaryAssignment t = (TemporaryAssignment) tempAssignments.get(i);
                System.out.println("  " + (i + 1) + ". " + t.role().getName() +
                        " (expires: " + t.getExpiresAt() + ")");
            }

            int choice = ConsoleHelper.promptInt(scanner, "Select assignment", 1, tempAssignments.size());
            TemporaryAssignment toExtend = (TemporaryAssignment) tempAssignments.get(choice - 1);

            String newDate = ConsoleHelper.promptFutureDate(scanner, "New expiration date (yyyy-MM-dd HH:mm)");

            try {
                toExtend.extend(newDate);
                system.log("ASSIGN_EXTEND", username, "Extended to: " + toExtend.getExpiresAt());
                ConsoleHelper.printSuccess("Assignment extended to " + newDate);
            } catch (Exception e) {
                ConsoleHelper.printError(e.getMessage());
            }
        });

        // assignment-search
        parser.registerCommand("assignment-search", "Search assignments", (scanner, system) -> {
            ConsoleHelper.printHeader("Search Assignments");

            int choice = ConsoleHelper.showMenu(scanner, "Select filter",
                    "By username",
                    "By role name",
                    "By type (permanent/temporary)",
                    "Active only",
                    "Inactive only");

            List<RoleAssignment> results;

            switch (choice) {
                case 1 -> {
                    String username = ConsoleHelper.promptNonEmpty(scanner, "Username");
                    results = system.getAssignmentManager()
                            .findByFilter(AssignmentFilters.byUsername(username));
                }
                case 2 -> {
                    String roleName = ConsoleHelper.promptNonEmpty(scanner, "Role name");
                    results = system.getAssignmentManager()
                            .findByFilter(AssignmentFilters.byRoleName(roleName));
                }
                case 3 -> {
                    String type = ConsoleHelper.promptNonEmpty(scanner, "Type (PERMANENT/TEMPORARY)");
                    results = system.getAssignmentManager()
                            .findByFilter(AssignmentFilters.byType(type));
                }
                case 4 -> results = system.getAssignmentManager()
                        .findByFilter(AssignmentFilters.activeOnly());
                case 5 -> results = system.getAssignmentManager()
                        .findByFilter(AssignmentFilters.inactiveOnly());
                default -> {
                    ConsoleHelper.printError("Invalid option");
                    return;
                }
            }

            if (results.isEmpty()) {
                ConsoleHelper.printInfo("No assignments found.");
            } else {
                System.out.println("\nFound " + results.size() + " assignment(s):");
                for (RoleAssignment a : results) {
                    System.out.printf("  - %s -> %s [%s]%n",
                            a.user().username(), a.role().getName(), a.assignmentType());
                }
            }
        });
    }

    // -- Permission commands --

    private void registerPermissionCommands() {

        // permissions-user
        parser.registerCommand("permissions-user", "Show user permissions", (scanner, system) -> {
            ConsoleHelper.printHeader("User Permissions");

            String username = ConsoleHelper.promptUsername(scanner, "Username");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            Set<Permission> permissions = system.getAssignmentManager().getUserPermissions(userOpt.get());

            if (permissions.isEmpty()) {
                ConsoleHelper.printInfo("User has no permissions.");
                return;
            }

            // Группируем по ресурсам
            var grouped = permissions.stream()
                    .collect(Collectors.groupingBy(Permission::resource));

            System.out.println("\nPermissions for '" + username + "':\n");
            for (var entry : grouped.entrySet()) {
                System.out.println("  [" + entry.getKey() + "]");
                for (Permission p : entry.getValue()) {
                    System.out.println("    - " + p.name() + ": " + p.description());
                }
            }
        });

        // permissions-check
        parser.registerCommand("permissions-check", "Check user permission", (scanner, system) -> {
            ConsoleHelper.printHeader("Check Permission");

            String username = ConsoleHelper.promptUsername(scanner, "Username");
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            String permName = ConsoleHelper.promptNonEmpty(scanner, "Permission name");
            String resource = ConsoleHelper.promptNonEmpty(scanner, "Resource");

            boolean hasPermission = system.getAssignmentManager()
                    .userHasPermission(userOpt.get(), permName, resource);

            if (hasPermission) {
                ConsoleHelper.printSuccess("User HAS permission '" + permName + "' on '" + resource + "'");

                List<RoleAssignment> assignments = system.getAssignmentManager()
                        .findByUser(userOpt.get()).stream()
                        .filter(RoleAssignment::isActive)
                        .filter(a -> a.role().hasPermission(permName, resource))
                        .toList();

                System.out.println("  From role(s):");
                assignments.forEach(a -> System.out.println("    - " + a.role().getName()));
            } else {
                ConsoleHelper.printWarning("User DOES NOT have permission '" + permName + "' on '" + resource + "'");
            }
        });
    }

    // -- System commands --

    private void registerSystemCommands() {

        // help
        parser.registerCommand("help", "Show available commands",
                (scanner, system) -> parser.printHelp());

        // stats
        parser.registerCommand("stats", "Show system statistics",
                (scanner, system) -> System.out.println(system.generateStatistics()));

        // clear
        parser.registerCommand("clear", "Clear screen",
                (scanner, system) -> ConsoleHelper.clearScreen());

        // exit
        parser.registerCommand("exit", "Exit the application", (scanner, system) -> {
            if (ConsoleHelper.confirm(scanner, "Are you sure you want to exit?")) {
                ConsoleHelper.printInfo("Goodbye!");
                System.exit(0);
            }
        });

        // whoami
        parser.registerCommand("whoami", "Show current user", (scanner, system) -> {
            System.out.println("Current user: " + system.getCurrentUser());
        });

        // switch-user
        parser.registerCommand("switch-user", "Switch current user", (scanner, system) -> {
            String username = ConsoleHelper.promptUsername(scanner, "Username");

            if (!system.getUserManager().exists(username)) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            system.setCurrentUser(username);
            system.log("SWITCH_USER", username, "User switched");
            ConsoleHelper.printSuccess("Switched to user: " + username);
        });

        parser.registerCommand("audit-log", "View audit log", (scanner, system) -> {
            ConsoleHelper.printHeader("Audit Log");

            int choice = ConsoleHelper.showMenu(scanner, "Select view",
                    "All entries",
                    "Recent entries",
                    "By performer",
                    "By action",
                    "By target",
                    "Save to file");

            switch (choice) {
                case 1 -> system.getAuditLog().printLog();
                case 2 -> {
                    int count = ConsoleHelper.promptInt(scanner, "Number of entries", 1, 100);
                    var entries = system.getAuditLog().getRecent(count);
                    printAuditEntries(entries);
                }
                case 3 -> {
                    String performer = ConsoleHelper.promptNonEmpty(scanner, "Performer username");
                    var entries = system.getAuditLog().getByPerformer(performer);
                    printAuditEntries(entries);
                }
                case 4 -> {
                    String action = ConsoleHelper.promptNonEmpty(scanner, "Action (e.g., USER_CREATE)");
                    var entries = system.getAuditLog().getByAction(action);
                    printAuditEntries(entries);
                }
                case 5 -> {
                    String target = ConsoleHelper.promptNonEmpty(scanner, "Target");
                    var entries = system.getAuditLog().getByTarget(target);
                    printAuditEntries(entries);
                }
                case 6 -> {
                    String filename = ConsoleHelper.promptNonEmpty(scanner, "Filename");
                    try {
                        system.getAuditLog().saveToFile(filename);
                        ConsoleHelper.printSuccess("Audit log saved to " + filename);
                    } catch (Exception e) {
                        ConsoleHelper.printError("Failed to save: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void printAuditEntries(List<AuditEntry> entries) {
        if (entries.isEmpty()) {
            ConsoleHelper.printInfo("No entries found.");
            return;
        }

        System.out.printf("%n%-20s %-15s %-15s %-20s %s%n",
                "TIMESTAMP", "ACTION", "PERFORMER", "TARGET", "DETAILS");
        ConsoleHelper.printSeparator();

        for (AuditEntry entry : entries) {
            System.out.printf("%-20s %-15s %-15s %-20s %s%n",
                    entry.timestamp(),
                    entry.action(),
                    entry.performer(),
                    entry.target(),
                    entry.details());
        }

        System.out.println("\nTotal: " + entries.size() + " entries");
    }
}