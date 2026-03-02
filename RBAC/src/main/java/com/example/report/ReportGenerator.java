package com.example.report;

import com.example.assignment.RoleAssignment;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.repository.AssignmentManager;
import com.example.repository.RoleManager;
import com.example.repository.UserManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users in the system.\n");
            return sb.toString();
        }

        sb.append(String.format("Total users: %d%n%n", users.size()));

        for (User user : users) {
            sb.append(String.format("User: %s (%s) <%s>%n",
                    user.username(), user.fullname(), user.email()));

            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            long activeCount = assignments.stream().filter(RoleAssignment::isActive).count();

            sb.append(String.format("  Assignments: %d total, %d active%n",
                    assignments.size(), activeCount));

            if (!assignments.isEmpty()) {
                sb.append("  Roles:\n");
                for (RoleAssignment a : assignments) {
                    String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                    sb.append(String.format("    - %-20s [%-10s] %s%n",
                            a.role().getName(), a.assignmentType(), status));
                }
            }

            Set<Permission> permissions = assignmentManager.getUserPermissions(user);
            if (!permissions.isEmpty()) {
                sb.append("  Permissions:\n");

                var grouped = permissions.stream()
                        .collect(Collectors.groupingBy(Permission::resource));

                for (var entry : grouped.entrySet()) {
                    String actions = entry.getValue().stream()
                            .map(Permission::name)
                            .collect(Collectors.joining(", "));
                    sb.append(String.format("    [%s]: %s%n", entry.getKey(), actions));
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<Role> roles = roleManager.findAll();

        if (roles.isEmpty()) {
            sb.append("No roles in the system.\n");
            return sb.toString();
        }

        sb.append(String.format("Total roles: %d%n%n", roles.size()));

        // Таблица ролей
        sb.append(String.format("%-20s %-12s %-12s %-15s%n",
                "ROLE", "USERS", "ACTIVE", "PERMISSIONS"));
        sb.append("-".repeat(60)).append("\n");

        for (Role role : roles) {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long activeUsers = assignments.stream().filter(RoleAssignment::isActive).count();

            sb.append(String.format("%-20s %-12d %-12d %-15d%n",
                    role.getName(),
                    assignments.size(),
                    activeUsers,
                    role.getPermissions().size()));
        }

        // Детали по каждой роли
        sb.append("\n--- Details ---\n\n");

        for (Role role : roles) {
            sb.append(String.format("Role: %s%n", role.getName()));
            sb.append(String.format("  Description: %s%n", role.getDescription()));
            sb.append("  Permissions:\n");

            if (role.getPermissions().isEmpty()) {
                sb.append("    (none)\n");
            } else {
                for (Permission p : role.getPermissions()) {
                    sb.append(String.format("    - %s on %s%n", p.name(), p.resource()));
                }
            }

            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            sb.append("  Assigned to:\n");

            if (assignments.isEmpty()) {
                sb.append("    (no users)\n");
            } else {
                for (RoleAssignment a : assignments) {
                    String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                    sb.append(String.format("    - %s [%s]%n", a.user().username(), status));
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();

        List<User> users = userManager.findAll();

        if (users.isEmpty()) {
            sb.append("No users in the system.\n");
            return sb.toString();
        }

        Set<String> allResources = new TreeSet<>();
        Set<String> allActions = new TreeSet<>();

        for (User user : users) {
            Set<Permission> perms = assignmentManager.getUserPermissions(user);
            for (Permission p : perms) {
                allResources.add(p.resource());
                allActions.add(p.name());
            }
        }

        if (allResources.isEmpty()) {
            sb.append("No permissions assigned to any user.\n");
            return sb.toString();
        }

        List<String> columns = new ArrayList<>();
        for (String resource : allResources) {
            for (String action : allActions) {
                columns.add(action + ":" + resource);
            }
        }

        int userColWidth = 15;
        int permColWidth = 8;

        sb.append(String.format("%-" + userColWidth + "s", "USER"));
        for (String col : columns) {
            String shortCol = abbreviate(col, permColWidth);
            sb.append(String.format(" %-" + permColWidth + "s", shortCol));
        }
        sb.append("\n");
        sb.append("-".repeat(userColWidth + columns.size() * (permColWidth + 1))).append("\n");

        for (User user : users) {
            Set<Permission> userPerms = assignmentManager.getUserPermissions(user);

            sb.append(String.format("%-" + userColWidth + "s", truncate(user.username(), userColWidth)));

            for (String col : columns) {
                String[] parts = col.split(":", 2);
                String action = parts[0];
                String resource = parts[1];

                boolean hasPermission = userPerms.stream()
                        .anyMatch(p -> p.name().equals(action) && p.resource().equals(resource));

                String marker = hasPermission ? "  +" : "  -";
                sb.append(String.format(" %-" + permColWidth + "s", marker));
            }
            sb.append("\n");
        }

        sb.append("\nLegend: + = has permission, - = no permission\n");

        sb.append("\n--- Resource Summary ---\n\n");

        for (String resource : allResources) {
            long usersWithAccess = users.stream()
                    .filter(u -> assignmentManager.getUserPermissions(u).stream()
                            .anyMatch(p -> p.resource().equals(resource)))
                    .count();
            sb.append(String.format("  [%s]: %d user(s) have access%n", resource, usersWithAccess));
        }

        return sb.toString();
    }

    public void exportToFile(String report, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.print(report);
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    private String abbreviate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 1) + ".";
    }
}