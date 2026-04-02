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
        List<User> users = userManager.findAll();
        if (users.isEmpty()) return "No users in the system.\n";

        String usersData = users.parallelStream()
                .map(user -> formatUserEntry(user, assignmentManager))
                .collect(Collectors.joining("\n"));

        return String.format("Total users: %d%n%n%s", users.size(), usersData);
    }

    private String formatUserEntry(User user, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("User: %s (%s) <%s>%n", user.username(), user.fullname(), user.email()));

        List<RoleAssignment> assignments = assignmentManager.findByUser(user);
        long activeCount = assignments.stream().filter(RoleAssignment::isActive).count();
        sb.append(String.format("  Assignments: %d total, %d active%n", assignments.size(), activeCount));

        appendRoleDetails(sb, assignments);
        appendPermissionDetails(sb, assignmentManager.getUserPermissions(user));

        return sb.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        List<Role> roles = roleManager.findAll();
        if (roles.isEmpty()) return "No roles in the system.\n";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total roles: %d%n%n", roles.size()));

        sb.append(String.format("%-20s %-12s %-12s %-15s%n", "ROLE", "USERS", "ACTIVE", "PERMISSIONS"));
        sb.repeat("-",60).append("\n");

        roles.forEach(role -> {
            List<RoleAssignment> assignments = assignmentManager.findByRole(role);
            long activeUsers = assignments.stream().filter(RoleAssignment::isActive).count();
            sb.append(String.format("%-20s %-12d %-12d %-15d%n",
                    role.getName(), assignments.size(), activeUsers, role.getPermissions().size()));
        });

        sb.append("\n--- Details ---\n\n");
        roles.forEach(role -> appendRoleDetailBlock(sb, role, assignmentManager));

        return sb.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        List<User> users = userManager.findAll();
        if (users.isEmpty()) return "No users in the system.\n";

        List<String> columns = getAllPermissionColumns(users, assignmentManager);
        if (columns.isEmpty()) return "No permissions assigned to any user.\n";

        StringBuilder sb = new StringBuilder();
        int userColWidth = 15;
        int permColWidth = 8;

        // Формирование заголовка (Исправлено форматирование)
        sb.append(String.format("%-" + userColWidth + "s", "USER"));
        columns.forEach(col -> sb.append(String.format(" %-" + permColWidth + "s", abbreviate(col, permColWidth))));
        sb.append("\n").repeat("-",userColWidth + columns.size() * (permColWidth + 1)).append("\n");

        // Параллельная генерация строк матрицы
        String matrixBody = users.parallelStream()
                .map(user -> formatMatrixRow(user, columns, assignmentManager, userColWidth, permColWidth))
                .collect(Collectors.joining("\n"));

        sb.append(matrixBody).append("\n\nLegend: + = has permission, - = no permission\n");
        appendResourceSummary(sb, users, assignmentManager);

        return sb.toString();
    }

    private String formatMatrixRow(User user, List<String> columns, AssignmentManager am, int uWidth, int pWidth) {
        StringBuilder row = new StringBuilder();
        row.append(String.format("%-" + uWidth + "s", truncate(user.username(), uWidth)));

        Set<Permission> userPerms = am.getUserPermissions(user);
        for (String col : columns) {
            String[] parts = col.split(":", 2);
            boolean has = userPerms.stream().anyMatch(p -> p.matches(parts[0], parts[1]));
            row.append(String.format(" %-" + pWidth + "s", has ? "  +" : "  -"));
        }
        return row.toString();
    }

    private List<String> getAllPermissionColumns(List<User> users, AssignmentManager am) {
        Set<String> resources = new TreeSet<>();
        Set<String> actions = new TreeSet<>();

        users.forEach(user -> am.getUserPermissions(user).forEach(p -> {
            resources.add(p.resource());
            actions.add(p.name());
        }));

        List<String> cols = new ArrayList<>();
        for (String res : resources) {
            for (String act : actions) {
                cols.add(act + ":" + res);
            }
        }
        return cols;
    }

    private void appendRoleDetails(StringBuilder sb, List<RoleAssignment> assignments) {
        if (assignments.isEmpty()) return;
        sb.append("  Roles:\n");
        for (RoleAssignment a : assignments) {
            sb.append(String.format("    - %-20s [%-10s] %s%n",
                    a.role().getName(), a.assignmentType(), a.isActive() ? "ACTIVE" : "INACTIVE"));
        }
    }

    private void appendPermissionDetails(StringBuilder sb, Set<Permission> permissions) {
        if (permissions.isEmpty()) return;
        sb.append("  Permissions:\n");
        permissions.stream()
                .collect(Collectors.groupingBy(Permission::resource))
                .forEach((resource, perms) -> {
                    String actions = perms.stream().map(Permission::name).collect(Collectors.joining(", "));
                    sb.append(String.format("    [%s]: %s%n", resource, actions));
                });
    }

    private void appendRoleDetailBlock(StringBuilder sb, Role role, AssignmentManager am) {
        sb.append(String.format("Role: %s%n  Description: %s%n  Permissions:%n",
                role.getName(), role.getDescription()));

        if (role.getPermissions().isEmpty()) {
            sb.append("    (none)\n");
        } else {
            role.getPermissions().forEach(p -> sb.append(String.format("    - %s on %s%n", p.name(), p.resource())));
        }

        List<RoleAssignment> assignments = am.findByRole(role);
        sb.append("  Assigned to:\n");
        if (assignments.isEmpty()) {
            sb.append("    (no users)\n");
        } else {
            assignments.forEach(a -> sb.append(String.format("    - %s [%s]%n",
                    a.user().username(), a.isActive() ? "ACTIVE" : "INACTIVE")));
        }
        sb.append("\n");
    }

    private void appendResourceSummary(StringBuilder sb, List<User> users, AssignmentManager am) {
        sb.append("--- Resource Summary ---\n\n");
        Set<String> allResources = users.stream()
                .flatMap(u -> am.getUserPermissions(u).stream())
                .map(Permission::resource)
                .collect(Collectors.toCollection(TreeSet::new));

        for (String resource : allResources) {
            long count = users.stream()
                    .filter(u -> am.getUserPermissions(u).stream().anyMatch(p -> p.resource().equals(resource)))
                    .count();
            sb.append(String.format("  [%s]: %d user(s) have access%n", resource, count));
        }
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