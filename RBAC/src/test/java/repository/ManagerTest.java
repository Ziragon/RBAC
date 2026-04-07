package repository;

import com.example.assignment.PermanentAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.entity.AssignmentMetadata;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.filters.UserFilters;
import com.example.repository.AssignmentManager;
import com.example.repository.RoleManager;
import com.example.repository.UserManager;
import com.example.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Manager Test")
class ManagerTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static long counter = 0;

    private String uniqueName(String base) {
        return base + "_" + System.nanoTime() + "_" + (++counter);
    }

    @Nested
    @DisplayName("UserManager")
    class UserManagerTests {

        private UserManager userManager;
        private User alice;
        private User anton;

        @BeforeEach
        void setUp() {
            userManager = new UserManager();
            alice = new User("alice", "Alice Smith", "alice@example.com");
            anton = new User("anton", "Anton Ivanov", "anton@example.com");
        }

        @Test
        @DisplayName("Should add and find user")
        void shouldAddAndFindUser() {
            userManager.add(alice);

            assertAll(
                    () -> assertEquals(1, userManager.count()),
                    () -> assertTrue(userManager.findByUsername("alice").isPresent()),
                    () -> assertEquals("Alice Smith", userManager.findByUsername("alice").get().fullname())
            );
        }

        @Test
        @DisplayName("Should throw for duplicate username")
        void shouldThrowForDuplicateUsername() {
            userManager.add(alice);

            assertThrows(IllegalArgumentException.class,
                    () -> userManager.add(new User("alice", "Another Alice", "other@test.com")));
        }

        @Test
        @DisplayName("Should update user")
        void shouldUpdateUser() {
            userManager.add(anton);

            userManager.update("anton", "Anton P. Ivanov", "anton_new@example.com");

            User updated = userManager.findByUsername("anton").orElseThrow();
            assertAll(
                    () -> assertEquals("Anton P. Ivanov", updated.fullname()),
                    () -> assertEquals("anton_new@example.com", updated.email())
            );
        }

        @Test
        @DisplayName("Should filter users")
        void shouldFilterUsers() {
            userManager.add(alice);
            userManager.add(anton);

            var result = userManager.findByFilterParallel(UserFilters.byEmailDomain("@example.com"));

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Concurrency: Multiple threads adding same user")
        void shouldHandleConcurrentUserAdditions() throws InterruptedException {
            int threads = 50;
            var successCount = new AtomicInteger(0);
            var latch = new CountDownLatch(1);

            try (var service = Executors.newFixedThreadPool(threads)) {
                for (int i = 0; i < threads; i++) {
                    service.execute(() -> {
                        try {
                            latch.await();
                            userManager.add(new User("concurrent_user", "Test", "test@test.com"));
                            successCount.incrementAndGet();
                        } catch (IllegalArgumentException | InterruptedException _) {
                            // Юзер создастся только 1 раз, остальные потоки выкинут Exception
                        }
                    });
                }

                latch.countDown();
                service.shutdown();
                boolean terminated = service.awaitTermination(10, TimeUnit.SECONDS);
                assertTrue(terminated, "All threads should complete within timeout");
            }

            assertEquals(1, successCount.get(), "Only one thread should succeed");
            assertEquals(1, userManager.count(), "Only one user should exist");
        }
    }

    @Nested
    @DisplayName("RoleManager")
    class RoleManagerTests {

        private RoleManager roleManager;
        private AssignmentManager assignmentManager;
        private Role adminRole;

        @BeforeEach
        void setUp() {
            assignmentManager = new AssignmentManager();
            roleManager = new RoleManager(assignmentManager);
            Permission readPerm = new Permission("READ", "users", "View users");
            adminRole = Role.create(uniqueName("Admin"), "Admin role", Set.of(readPerm));
        }

        @Test
        @DisplayName("Should add and find role")
        void shouldAddAndFindRole() {
            roleManager.add(adminRole);

            assertAll(
                    () -> assertEquals(1, roleManager.count()),
                    () -> assertTrue(roleManager.findById(adminRole.getId()).isPresent()),
                    () -> assertTrue(roleManager.findByName(adminRole.getName()).isPresent())
            );
        }

        @Test
        @DisplayName("Should prevent deletion of role with active assignments")
        void shouldPreventDeletionWithActiveAssignments() {
            roleManager.add(adminRole);

            User user = new User("testuser", "Test", "test@test.com");
            AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
            assignmentManager.add(new PermanentAssignment(user, adminRole, meta));

            assertThrows(IllegalStateException.class, () -> roleManager.remove(adminRole));
        }

        @Test
        @DisplayName("Should delete role without assignments")
        void shouldDeleteRoleWithoutAssignments() {
            roleManager.add(adminRole);

            assertTrue(roleManager.remove(adminRole));
            assertEquals(0, roleManager.count());
        }

        @Test
        @DisplayName("Concurrency: Role creation safety")
        void shouldHandleConcurrentRoleCreation() throws InterruptedException {
            int threads = 50;
            var roleName = "SHARED_ROLE";
            var successCount = new AtomicInteger(0);
            var latch = new CountDownLatch(1);

            try (var service = Executors.newFixedThreadPool(threads)) {
                for (int i = 0; i < threads; i++) {
                    service.execute(() -> {
                        try {
                            latch.await();
                            roleManager.add(Role.create(roleName, "desc", null));
                            successCount.incrementAndGet();
                        } catch (IllegalArgumentException | InterruptedException _) {
                            // Создастся только 1 роль
                        }
                    });
                }

                latch.countDown();
                service.shutdown();
                var terminated = service.awaitTermination(5, TimeUnit.SECONDS);
                assertTrue(terminated, "All threads should complete within timeout");
            }

            assertEquals(1, successCount.get(), "Only one thread should succeed");
            assertEquals(1, roleManager.count(), "Only one role should exist");
        }
    }

    @Nested
    @DisplayName("AssignmentManager")
    class AssignmentManagerTests {

        private AssignmentManager assignmentManager;
        private User user;
        private Role role;
        private AssignmentMetadata meta;

        @BeforeEach
        void setUp() {
            assignmentManager = new AssignmentManager();
            user = new User("testuser", "Test User", "test@test.com");
            Permission perm = new Permission("DELETE", "users", "Delete users");
            role = Role.create(uniqueName("TestRole"), "Test", Set.of(perm));
            meta = AssignmentMetadata.now("admin", "Test assignment");
        }

        @Test
        @DisplayName("Should add assignment and check permissions")
        void shouldAddAssignmentAndCheckPermissions() {
            PermanentAssignment assignment = new PermanentAssignment(user, role, meta);
            assignmentManager.add(assignment);

            assertAll(
                    () -> assertEquals(1, assignmentManager.count()),
                    () -> assertTrue(assignmentManager.userHasRole(user, role)),
                    () -> assertTrue(assignmentManager.userHasPermission(user, "DELETE", "users")),
                    () -> assertEquals(1, assignmentManager.getUserPermissions(user).size())
            );
        }

        @Test
        @DisplayName("Should prevent duplicate role assignment")
        void shouldPreventDuplicateRoleAssignment() {
            assignmentManager.add(new PermanentAssignment(user, role, meta));

            assertThrows(IllegalStateException.class,
                    () -> assignmentManager.add(new PermanentAssignment(user, role, meta)));
        }

        @Test
        @DisplayName("Should revoke and extend assignments")
        void shouldRevokeAndExtendAssignments() {
            String futureDate = LocalDateTime.now().plusDays(7).format(FORMATTER);
            TemporaryAssignment temp = new TemporaryAssignment(user, role, meta, futureDate, true);
            assignmentManager.add(temp);

            // Revoke
            assignmentManager.revokeAssignment(temp.assignmentId());
            assertTrue(temp.isRevoked());

            // Extend
            String newDate = LocalDateTime.now().plusDays(30).format(FORMATTER);
            assignmentManager.extendTemporaryAssignment(temp.assignmentId(), newDate);
            assertEquals(newDate, temp.getExpiresAt());
        }

        @Test
        @DisplayName("Should throw when extending permanent assignment")
        void shouldThrowWhenExtendingPermanent() {
            PermanentAssignment perm = new PermanentAssignment(user, role, meta);
            assignmentManager.add(perm);

            assertThrows(IllegalArgumentException.class,
                    () -> assignmentManager.extendTemporaryAssignment(perm.assignmentId(), "2030-01-01 00:00"));
        }

        @Test
        @DisplayName("Concurrency: Prevent double assignment")
        void shouldHandleConcurrentAssignment() throws InterruptedException {
            int threads = 20;
            var successCount = new AtomicInteger(0);
            var latch = new CountDownLatch(1);

            try (var service = Executors.newFixedThreadPool(threads)) {
                for (int i = 0; i < threads; i++) {
                    service.execute(() -> {
                        try {
                            latch.await();
                            assignmentManager.add(new PermanentAssignment(user, role, meta));
                            successCount.incrementAndGet();
                        } catch (IllegalStateException | InterruptedException _) {
                            // Добавится только 1 назначение
                        }
                    });
                }

                latch.countDown();
                service.shutdown();
                var terminated = service.awaitTermination(5, TimeUnit.SECONDS);
                assertTrue(terminated, "All threads should complete within timeout");
            }

            assertEquals(1, successCount.get(), "Only one thread should succeed");
            assertEquals(1, assignmentManager.count(), "Only one assignment should exist");
        }

        @Test
        @DisplayName("Should revoke only expired temporary assignments")
        void shouldRevokeOnlyExpired() {
            TemporaryAssignment expired = new TemporaryAssignment(user, role, meta, "2000-01-01 00:00", false);

            Role otherRole = Role.create(uniqueName("TestRole"), "Test", Set.of());
            TemporaryAssignment nonExpired = new TemporaryAssignment(user, otherRole, meta,
                    DateUtils.addDays(DateUtils.getCurrentDateTimeShort(), 1), false);

            assignmentManager.add(expired);
            assignmentManager.add(nonExpired);

            int revoked = assignmentManager.revokeExpiredAssignments();

            assertEquals(1, revoked, "One assignment should be revoked");
        }
    }
}