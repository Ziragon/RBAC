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
import com.example.util.FormatUtils;

import java.util.Comparator;
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
        registerReportCommands();
    }

    // -- User Commands --

    private void registerUserCommands() {

        // user-list
        parser.registerCommand("user-list", "List all users", (scanner, system) -> {
            ConsoleHelper.printHeader("User List");
            List<User> users = system.getUserManager().findAll(
                    _ -> true,
                    Comparator.comparing(User::username)
            );

            if (users.isEmpty()) {
                ConsoleHelper.printInfo("No users found.");
                return;
            }

            String[] headers = {"Username", "Full Name", "Email"};
            List<String[]> rows = users.stream()
                    .map(u -> new String[]{u.username(), u.fullname(), u.email()})
                    .toList();

            System.out.println(FormatUtils.formatTable(headers, rows));
            System.out.println("Total: " + users.size() + " user(s)");
        });

        // user-create
        parser.registerCommand("user-create", "Create a new user", (scanner, system) -> {
            ConsoleHelper.printHeader("Create User");

            String username = ConsoleHelper.promptUsername(scanner, "Username");
            String fullName = ConsoleHelper.promptString(scanner, "Full name", true);
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

            String newFullName = ConsoleHelper.promptString(scanner, "New full name", true);
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

            String query = ConsoleHelper.promptString(scanner, "Enter search query", true);
            List<User> results;

            switch (choice) {
                case 1 -> results = system.getUserManager()
                        .findByFilterParallel(UserFilters.byUsernameContains(query));
                case 2 -> results = system.getUserManager()
                        .findByFilterParallel(u -> u.email().toLowerCase().contains(query.toLowerCase()));
                case 3 -> results = system.getUserManager()
                        .findByFilterParallel(UserFilters.byEmailDomain(query));
                case 4 -> results = system.getUserManager()
                        .findByFilterParallel(UserFilters.byFullNameContains(query));
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
            List<Role> roles = system.getRoleManager().findAll(
                    _ -> true,
                    Comparator.comparing(Role::getName)
            );

            if (roles.isEmpty()) {
                ConsoleHelper.printInfo("No roles found.");
                return;
            }

            String[] headers = {"Name", "Permissions", "ID"};
            List<String[]> rows = roles.stream()
                    .map(r -> new String[]{
                            r.getName(),
                            String.valueOf(r.getPermissions().size()),
                            FormatUtils.truncate(r.getId(), 25)
                    })
                    .toList();

            System.out.println(FormatUtils.formatTable(headers, rows));
            System.out.println("Total: " + roles.size() + " role(s)");
        });

        // role-create
        parser.registerCommand("role-create", "Create a new role", (scanner, system) -> {
            ConsoleHelper.printHeader("Create Role");

            String name = ConsoleHelper.promptString(scanner, "Role name", true);
            String description = ConsoleHelper.promptString(scanner, "Description", false);

            try {
                Role role = Role.create(name, description, Set.of());
                system.getRoleManager().add(role);
                system.log("ROLE_CREATE", name, "Created role with description: " + description);
                ConsoleHelper.printSuccess("Role '" + name + "' created.");

                // Предлагаем добавить права
                while (ConsoleHelper.confirm(scanner, "Add a permission?")) {
                    String permName = ConsoleHelper.promptString(scanner, "Permission name (e.g., READ)", true);
                    String resource = ConsoleHelper.promptString(scanner, "Resource (e.g., users)", true);
                    String permDesc = ConsoleHelper.promptString(scanner, "Permission description", true);

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

            String name = ConsoleHelper.promptString(scanner, "Role name", true);
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

            String name = ConsoleHelper.promptString(scanner, "Role name to delete", true);
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

            String roleName = ConsoleHelper.promptString(scanner, "Role name", true);
            Optional<Role> roleOpt = system.getRoleManager().findByName(roleName);

            if (roleOpt.isEmpty()) {
                ConsoleHelper.printError("Role not found: " + roleName);
                return;
            }

            String permName = ConsoleHelper.promptString(scanner, "Permission name", true);
            String resource = ConsoleHelper.promptString(scanner, "Resource", true);
            String description = ConsoleHelper.promptString(scanner, "Description", true);

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

            String roleName = ConsoleHelper.promptString(scanner, "Role name", true);
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

            Permission toRemove = ConsoleHelper.promptChoice(scanner, "Select permission to remove", permissions);
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
                    String query = ConsoleHelper.promptString(scanner, "Name contains", true);
                    results = system.getRoleManager().findByFilterParallel(RoleFilters.byNameContains(query));
                }
                case 2 -> {
                    String permName = ConsoleHelper.promptString(scanner, "Permission name", true);
                    String resource = ConsoleHelper.promptString(scanner, "Resource", true);
                    results = system.getRoleManager().findRolesWithPermission(permName, resource);
                }
                case 3 -> {
                    int min = ConsoleHelper.promptInt(scanner, "Minimum permissions", 1, 100);
                    results = system.getRoleManager().findByFilterParallel(RoleFilters.hasAtLeastNPermissions(min));
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

            String username = ConsoleHelper.promptString(scanner, "Username", true);
            Optional<User> userOpt = system.getUserManager().findByUsername(username);

            if (userOpt.isEmpty()) {
                ConsoleHelper.printError("User not found: " + username);
                return;
            }

            User user = userOpt.get();

            // Показываем доступные роли
            List<Role> roles = system.getRoleManager().findAll(
                    role -> true,
                    Comparator.comparing(Role::getName)
            );

            if (roles.isEmpty()) {
                ConsoleHelper.printError("No roles available.");
                return;
            }

            Role role = ConsoleHelper.promptChoice(scanner, "Available roles", roles);

            int typeChoice = ConsoleHelper.showMenu(scanner, "Assignment type",
                    "Permanent",
                    "Temporary");

            String reason = ConsoleHelper.promptString(scanner, "Reason for assignment", false);
            AssignmentMetadata metadata = system.createMetadata(reason);

            try {
                if (typeChoice == 1) {
                    PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                    system.getAssignmentManager().add(assignment);
                } else {
                    String expiration = ConsoleHelper.promptFutureDate(scanner,
                            "Expiration date");
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

            List<String> displayNames = assignments.stream()
                    .map(a -> a.role().getName() + " [" + a.assignmentType() + "]")
                    .toList();

            String chosen = ConsoleHelper.promptChoice(scanner, "Select assignment to revoke", displayNames);
            int index = displayNames.indexOf(chosen);
            RoleAssignment toRevoke = assignments.get(index);
            toRevoke.revoke();
            system.log("ROLE_REVOKE", username, "Revoked role: " + toRevoke.role().getName());
            ConsoleHelper.printSuccess("Assignment revoked.");
        });

        // assignment-list
        parser.registerCommand("assignment-list", "List all assignments", (scanner, system) -> {
            ConsoleHelper.printHeader("All Assignments");

            List<RoleAssignment> assignments = system.getAssignmentManager().findAll(
                    _ -> true,
                    Comparator.comparing((RoleAssignment a) -> a.user().username())
                            .thenComparing(a -> a.role().getName())
            );

            if (assignments.isEmpty()) {
                ConsoleHelper.printInfo("No assignments found.");
                return;
            }

            String[] headers = {"User", "Role", "Type", "Status", "Assigned At"};
            List<String[]> rows = assignments.stream()
                    .map(a -> new String[]{
                            a.user().username(),
                            a.role().getName(),
                            a.assignmentType(),
                            a.isActive() ? "ACTIVE" : "INACTIVE",
                            a.metadata().assignedAt().substring(0, 16)
                    })
                    .toList();

            System.out.println(FormatUtils.formatTable(headers, rows));
            System.out.println("Total: " + assignments.size() + " assignment(s)");
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

            String roleName = ConsoleHelper.promptString(scanner, "Role name", true);
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

            String username = ConsoleHelper.promptString(scanner, "Username", true);
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

            List<String> displayNames = tempAssignments.stream()
                    .map(a -> {
                        TemporaryAssignment t = (TemporaryAssignment) a;
                        return t.role().getName() + " (expires: " + t.getExpiresAt() + ")";
                    })
                    .toList();

            String chosen = ConsoleHelper.promptChoice(scanner, "Select assignment to extend", displayNames);
            int index = displayNames.indexOf(chosen);
            TemporaryAssignment toExtend = (TemporaryAssignment) tempAssignments.get(index);

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
                    String username = ConsoleHelper.promptUsername(scanner, "Username");
                    results = system.getAssignmentManager()
                            .findByFilterParallel(AssignmentFilters.byUsername(username));
                }
                case 2 -> {
                    String roleName = ConsoleHelper.promptString(scanner, "Role name", true);
                    results = system.getAssignmentManager()
                            .findByFilterParallel(AssignmentFilters.byRoleName(roleName));
                }
                case 3 -> {
                    String type = ConsoleHelper.promptString(scanner, "Type (PERMANENT/TEMPORARY)", true);
                    results = system.getAssignmentManager()
                            .findByFilterParallel(AssignmentFilters.byType(type));
                }
                case 4 -> results = system.getAssignmentManager()
                        .findByFilterParallel(AssignmentFilters.activeOnly());
                case 5 -> results = system.getAssignmentManager()
                        .findByFilterParallel(AssignmentFilters.inactiveOnly());
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

            String permName = ConsoleHelper.promptString(scanner, "Permission name", true);
            String resource = ConsoleHelper.promptString(scanner, "Resource", true);

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

    // -- Report commands --
    private void registerReportCommands() {

        // report-users
        parser.registerCommand("report-users", "User report with roles and permissions", (scanner, system) -> {
            ConsoleHelper.printHeader("User Report");

            String report = system.getReportGenerator().generateUserReport(
                    system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);

            if (ConsoleHelper.confirm(scanner, "Save to file?")) {
                String filename = ConsoleHelper.promptString(scanner, "Filename", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    system.log("REPORT_EXPORT", filename, "User report exported");
                    ConsoleHelper.printSuccess("Report saved to " + filename);
                } catch (Exception e) {
                    ConsoleHelper.printError("Failed to save: " + e.getMessage());
                }
            }
        });

        // report-roles
        parser.registerCommand("report-roles", "Role report with user counts", (scanner, system) -> {
            ConsoleHelper.printHeader("Role Report");

            String report = system.getReportGenerator().generateRoleReport(
                    system.getRoleManager(), system.getAssignmentManager());
            System.out.println(report);

            if (ConsoleHelper.confirm(scanner, "Save to file?")) {
                String filename = ConsoleHelper.promptString(scanner, "Filename", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    system.log("REPORT_EXPORT", filename, "Role report exported");
                    ConsoleHelper.printSuccess("Report saved to " + filename);
                } catch (Exception e) {
                    ConsoleHelper.printError("Failed to save: " + e.getMessage());
                }
            }
        });

        // report-matrix
        parser.registerCommand("report-matrix", "Permission matrix (users × resources)", (scanner, system) -> {
            ConsoleHelper.printHeader("Permission Matrix");

            String report = system.getReportGenerator().generatePermissionMatrix(
                    system.getUserManager(), system.getAssignmentManager());
            System.out.println(report);

            if (ConsoleHelper.confirm(scanner, "Save to file?")) {
                String filename = ConsoleHelper.promptString(scanner, "Filename", true);
                try {
                    system.getReportGenerator().exportToFile(report, filename);
                    system.log("REPORT_EXPORT", filename, "Permission matrix exported");
                    ConsoleHelper.printSuccess("Report saved to " + filename);
                } catch (Exception e) {
                    ConsoleHelper.printError("Failed to save: " + e.getMessage());
                }
            }
        });

        parser.registerCommand("report-users-async", "Generate user report in background", (scanner, system) -> {
            String filename = ConsoleHelper.promptString(scanner, "Filename to save report", true);
            ConsoleHelper.printInfo("Task submitted: Generating report in background. You can continue working.");

            system.getExecutor().execute(() -> {
                try {
                    // Выполняем тяжелую работу (сбор данных)
                    String report = system.getReportGenerator().generateUserReport(
                            system.getUserManager(), system.getAssignmentManager());

                    // Сохраняем в файл
                    system.getReportGenerator().exportToFile(report, filename);

                    // Уведомляем пользователя поверх консоли
                    System.out.println("\n[BACKGROUND SUCCESS] Report successfully saved to: " + filename + "\n> ");

                    // Логируем успешное действие асинхронно
                    system.getAuditLog().log("REPORT_ASYNC", "system", filename, "Background report generated");
                } catch (Exception e) {
                    System.err.println("\n[BACKGROUND ERROR] Report generation failed: " + e.getMessage() + "\n> ");
                }
            });
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
                    String performer = ConsoleHelper.promptString(scanner, "Performer username", true);
                    var entries = system.getAuditLog().getByPerformer(performer);
                    printAuditEntries(entries);
                }
                case 4 -> {
                    String action = ConsoleHelper.promptString(scanner, "Action (e.g., USER_CREATE)", true);
                    var entries = system.getAuditLog().getByAction(action);
                    printAuditEntries(entries);
                }
                case 5 -> {
                    String target = ConsoleHelper.promptString(scanner, "Target", true);
                    var entries = system.getAuditLog().getByTarget(target);
                    printAuditEntries(entries);
                }
                case 6 -> {
                    String filename = ConsoleHelper.promptString(scanner, "Filename", true);
                    try {
                        system.getAuditLog().saveToFile(filename);
                        ConsoleHelper.printSuccess("Audit log saved to " + filename);
                    } catch (Exception e) {
                        ConsoleHelper.printError("Failed to save: " + e.getMessage());
                    }
                }
            }
        });

        parser.registerCommand("save-async", "Save audit log to file in background", (scanner, system) -> {
            String filename = ConsoleHelper.promptString(scanner, "Filename to save audit log", true);
            ConsoleHelper.printInfo("Task submitted: Saving audit log in background...");

            system.getExecutor().execute(() -> {
                try {
                    system.getAuditLog().saveToFile(filename);
                    System.out.println("\n[BACKGROUND SUCCESS] Audit log saved to: " + filename + "\n> ");
                } catch (Exception e) {
                    System.err.println("\n[BACKGROUND ERROR] Failed to save audit log: " + e.getMessage() + "\n> ");
                }
            });
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