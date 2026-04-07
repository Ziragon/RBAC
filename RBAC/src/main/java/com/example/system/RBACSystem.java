package com.example.system;

import com.example.assignment.PermanentAssignment;
import com.example.assignment.RoleAssignment;
import com.example.audit.AuditLog;
import com.example.entity.*;
import com.example.report.ReportGenerator;
import com.example.repository.AssignmentManager;
import com.example.repository.RoleManager;
import com.example.repository.UserManager;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RBACSystem {

    private static final int CLEANUP_PERIOD_SECONDS = 30;
    private static final String SYSTEM_USER = "system";

    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private final ReportGenerator reportGenerator;
    private final BackgroundExecutor backgroundExecutor;

    private String currentUser;

    public RBACSystem() {
        this.assignmentManager = new AssignmentManager();
        this.roleManager = new RoleManager(assignmentManager);
        this.userManager = new UserManager();
        this.auditLog = new AuditLog();
        this.reportGenerator = new ReportGenerator();
        this.backgroundExecutor = new BackgroundExecutor();
        this.auditLog.startAsyncLogger(this.backgroundExecutor);
        this.currentUser = SYSTEM_USER;

        startMaintenanceTask();
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public ReportGenerator getReportGenerator() {
        return reportGenerator;
    }

    public BackgroundExecutor getExecutor() {
        return backgroundExecutor;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        String oldUser = this.currentUser;
        this.currentUser = username;
        auditLog.log("SWITCH_USER", oldUser, username, "User switched");
    }

    // Действие от текущего пользователя
    public void log(String action, String target, String details) {
        auditLog.log(action, currentUser, target, details);
    }

    public void initialize() {
        Role.clearNameRegistry();

        createDefaultPermissionsAndRoles();
        createDefaultAdmin();

        log("SYSTEM_INIT", SYSTEM_USER, "System initialized with default data");

        System.out.println("System initialized successfully!");
        System.out.println(generateStatistics());
    }

    private void createDefaultPermissionsAndRoles() {
        Permission readUsers = new Permission("READ", "users", "View user list and details");
        Permission writeUsers = new Permission("WRITE", "users", "Create and edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Delete users");

        Permission readRoles = new Permission("READ", "roles", "View role list and details");
        Permission writeRoles = new Permission("WRITE", "roles", "Create and edit roles");
        Permission deleteRoles = new Permission("DELETE", "roles", "Delete roles");

        Permission readAssignments = new Permission("READ", "assignments", "View assignments");
        Permission writeAssignments = new Permission("WRITE", "assignments", "Create and revoke assignments");

        Permission adminSystem = new Permission("ADMIN", SYSTEM_USER, "Full system administration");
        Permission readReports = new Permission("READ", "reports", "View system reports and statistics");

        Role adminRole = Role.create("Administrator", "Full system access with all permissions",
                Set.of(
                        readUsers, writeUsers, deleteUsers,
                        readRoles, writeRoles, deleteRoles,
                        readAssignments, writeAssignments,
                        adminSystem, readReports
                ));
        roleManager.add(adminRole);

        Role managerRole = Role.create("Manager", "User and assignment management",
                Set.of(
                        readUsers, writeUsers,
                        readRoles,
                        readAssignments, writeAssignments,
                        readReports
                ));
        roleManager.add(managerRole);

        Role viewerRole = Role.create("Viewer", "Read-only access to system data",
                Set.of(
                        readUsers,
                        readRoles,
                        readAssignments
                ));
        roleManager.add(viewerRole);
    }

    private void createDefaultAdmin() {
        User admin = new User("admin", "System Administrator", "admin@system.local");
        userManager.add(admin);

        Role adminRole = roleManager.findByName("Administrator")
                .orElseThrow(() -> new IllegalStateException("Administrator role not found"));

        AssignmentMetadata metadata = AssignmentMetadata.now(SYSTEM_USER, "Initial system setup");
        PermanentAssignment adminAssignment = new PermanentAssignment(admin, adminRole, metadata);
        assignmentManager.add(adminAssignment);

        setCurrentUser("admin");
    }

    public String generateStatistics() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("+----------------------------------------------------------+\n");
        sb.append("|                    SYSTEM STATISTICS                     |\n");
        sb.append("+----------------------------------------------------------+\n");

        sb.append(String.format("|  %-29s %24d  |%n", "Total Users:", userManager.count()));
        sb.append(String.format("|  %-29s %24d  |%n", "Total Roles:", roleManager.count()));
        sb.append(String.format("|  %-29s %24d  |%n", "Total Assignments:", assignmentManager.count()));

        sb.append("+----------------------------------------------------------+\n");

        long activeAssignments = assignmentManager.getActiveAssignments().size();
        long expiredAssignments = assignmentManager.getExpiredAssignments().size();

        sb.append(String.format("|  %-29s %24d  |%n", "Active Assignments:", activeAssignments));
        sb.append(String.format("|  %-29s %24d  |%n", "Expired Assignments:", expiredAssignments));
        sb.append(String.format("|  %-29s %24d  |%n", "Audit Log Entries:", auditLog.count()));

        sb.append("+----------------------------------------------------------+\n");

        sb.append("|  Roles breakdown:                                        |\n");

        for (Role role : roleManager.findAll()) {
            long usersWithRole = assignmentManager.findByRole(role).stream()
                    .filter(RoleAssignment::isActive)
                    .count();
            int permCount = role.getPermissions().size();

            sb.append(String.format("|    • %-20s users: %3d, permissions: %3d   |%n",
                    truncate(role.getName()), usersWithRole, permCount));
        }

        sb.append("+----------------------------------------------------------|\n");
        sb.append(String.format("|  Current User: %-41s |%n", currentUser));
        sb.append("+----------------------------------------------------------+\n");

        return sb.toString();
    }

    private String truncate(String str) {
        if (str == null) return "";
        if (str.length() <= 20) return str;
        return str.substring(0, 20 - 3) + "...";
    }

    public AssignmentMetadata createMetadata(String reason) {
        return AssignmentMetadata.now(currentUser, reason);
    }

    public boolean currentUserHasPermission(String permissionName, String resource) {
        return userManager.findByUsername(currentUser)
                .map(user -> assignmentManager.userHasPermission(user, permissionName, resource))
                .orElse(false);
    }

    public void clearAll() {
        assignmentManager.clear();
        roleManager.clear();
        userManager.clear();
        auditLog.clear();
        currentUser = SYSTEM_USER;
    }

    public void printSystemInfo() {
        System.out.println("\n=== RBAC System ===");
        System.out.println("Current user: " + currentUser);
        System.out.println("Users: " + userManager.count());
        System.out.println("Roles: " + roleManager.count());
        System.out.println("Assignments: " + assignmentManager.count());
    }

    private void startMaintenanceTask() {
        backgroundExecutor.scheduleTask(() -> {
            try {
                CleanupResult result = assignmentManager.processExpiredAssignments();

                // Если ничего не произошло - скип сообщения
                if (result.revokedCount() == 0 && result.renewedCount() == 0) {
                    return;
                }

                int users = userManager.count();
                int roles = roleManager.count();
                int activeAssignments = assignmentManager.count();

                String statsReport = String.format(
                        "Cleanup: %d revoked, %d auto-renewed. System Stats: [Users: %d, Roles: %d, Assignments: %d]",
                        result.revokedCount(), result.renewedCount(), users, roles, activeAssignments
                );

                auditLog.log("SYSTEM_MAINTENANCE", SYSTEM_USER, "all", statsReport);

            } catch (Exception e) {
                System.err.println("[MAINTENANCE ERROR] " + e.getMessage());
            }
        }, 5, CLEANUP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }
}