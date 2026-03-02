package filter;

import com.example.assignment.PermanentAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.entity.AssignmentMetadata;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.filters.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Filter Test")
class FilterTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static long counter = 0;

    private String uniqueName(String base) {
        return base + "_" + System.nanoTime() + "_" + (++counter);
    }

    @Nested
    @DisplayName("User Filters")
    class UserFilterTests {

        private User alice;
        private User anton;

        @BeforeEach
        void setUp() {
            alice = new User("alice", "Alice Smith", "alice@example.com");
            anton = new User("anton", "Anton Ivanov", "anton@company.org");
        }

        @Test
        @DisplayName("Should filter by username and email domain")
        void shouldFilterByUsernameAndEmail() {
            assertAll(
                    () -> assertTrue(UserFilters.byUsername("alice").test(alice)),
                    () -> assertFalse(UserFilters.byUsername("alice").test(anton)),
                    () -> assertTrue(UserFilters.byUsernameContains("AN").test(anton)),
                    () -> assertTrue(UserFilters.byEmailDomain("@example.com").test(alice)),
                    () -> assertFalse(UserFilters.byEmailDomain("@example.com").test(anton))
            );
        }

        @Test
        @DisplayName("Should combine filters with AND/OR")
        void shouldCombineFilters() {
            UserFilter combined = UserFilters.byUsernameContains("a")
                    .and(UserFilters.byEmailDomain("@example.com"));

            assertTrue(combined.test(alice));
            assertFalse(combined.test(anton));
        }
    }

    @Nested
    @DisplayName("Role Filters")
    class RoleFilterTests {

        private Role adminRole;
        private Role viewerRole;

        @BeforeEach
        void setUp() {
            Permission read = new Permission("READ", "users", "View");
            Permission write = new Permission("WRITE", "users", "Edit");
            Permission delete = new Permission("DELETE", "users", "Delete");

            adminRole = Role.create(uniqueName("Administrator"), "Admin",
                    Set.of(read, write, delete));
            viewerRole = Role.create(uniqueName("Viewer"), "View only",
                    Set.of(read));
        }

        @Test
        @DisplayName("Should filter by name and permissions")
        void shouldFilterByNameAndPermissions() {
            assertAll(
                    () -> assertTrue(RoleFilters.byNameContains("Admin").test(adminRole)),
                    () -> assertFalse(RoleFilters.byNameContains("Admin").test(viewerRole)),
                    () -> assertTrue(RoleFilters.hasPermission("WRITE", "users").test(adminRole)),
                    () -> assertFalse(RoleFilters.hasPermission("WRITE", "users").test(viewerRole)),
                    () -> assertTrue(RoleFilters.hasAtLeastNPermissions(3).test(adminRole)),
                    () -> assertFalse(RoleFilters.hasAtLeastNPermissions(3).test(viewerRole))
            );
        }
    }

    @Nested
    @DisplayName("Assignment Filters")
    class AssignmentFilterTests {

        private PermanentAssignment permanentAssignment;
        private TemporaryAssignment activeTemporary;

        @BeforeEach
        void setUp() {
            User user = new User("testuser", "Test", "test@test.com");
            Permission perm = new Permission("READ", "data", "Read");
            Role role = Role.create(uniqueName("Role"), "Test", Set.of(perm));
            AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");

            permanentAssignment = new PermanentAssignment(user, role, meta);

            String futureDate = LocalDateTime.now().plusDays(30).format(FORMATTER);
            activeTemporary = new TemporaryAssignment(user, role, meta, futureDate, true);
        }

        @Test
        @DisplayName("Should filter by type and active status")
        void shouldFilterByTypeAndStatus() {
            assertAll(
                    () -> assertTrue(AssignmentFilters.byType("PERMANENT").test(permanentAssignment)),
                    () -> assertTrue(AssignmentFilters.byType("TEMPORARY").test(activeTemporary)),
                    () -> assertTrue(AssignmentFilters.activeOnly().test(permanentAssignment)),
                    () -> assertTrue(AssignmentFilters.activeOnly().test(activeTemporary))
            );
        }

        @Test
        @DisplayName("Should filter temporary by expiration")
        void shouldFilterByExpiration() {
            AssignmentFilter expiringBefore2030 =
                    AssignmentFilters.expiringBefore("2030-01-01 00:00");

            assertAll(
                    () -> assertTrue(expiringBefore2030.test(activeTemporary)),
                    () -> assertFalse(expiringBefore2030.test(permanentAssignment))
            );
        }
    }
}