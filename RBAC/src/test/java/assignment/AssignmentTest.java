package assignment;

import com.example.assignment.PermanentAssignment;
import com.example.assignment.TemporaryAssignment;
import com.example.entity.AssignmentMetadata;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Assignment Tests")
class AssignmentTest {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private User user;
    private Role role;
    private AssignmentMetadata metadata;
    private static long counter = 0;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "Test User", "test@example.com");
        Permission perm = new Permission("READ", "users", "View users");
        role = Role.create("TestRole_" + System.nanoTime() + "_" + (++counter),
                "Test", Set.of(perm));
        metadata = AssignmentMetadata.now("admin", "Test assignment");
    }

    @Nested
    @DisplayName("Permanent Assignment")
    class PermanentAssignmentTests {

        @Test
        @DisplayName("Should create active permanent assignment")
        void shouldCreateActiveAssignment() {
            PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

            assertAll(
                    () -> assertTrue(assignment.isActive()),
                    () -> assertFalse(assignment.isRevoked()),
                    () -> assertEquals("PERMANENT", assignment.assignmentType())
            );
        }

        @Test
        @DisplayName("Should revoke assignment")
        void shouldRevokeAssignment() {
            PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

            assignment.revoke();

            assertAll(
                    () -> assertFalse(assignment.isActive()),
                    () -> assertTrue(assignment.isRevoked())
            );
        }
    }

    @Nested
    @DisplayName("Temporary Assignment")
    class TemporaryAssignmentTests {

        @Test
        @DisplayName("Should create active temporary assignment with future expiration")
        void shouldCreateActiveWithFutureExpiration() {
            String futureDate = LocalDateTime.now().plusDays(30).format(FORMATTER);

            TemporaryAssignment assignment = new TemporaryAssignment(
                    user, role, metadata, futureDate, true);

            assertAll(
                    () -> assertTrue(assignment.isActive()),
                    () -> assertFalse(assignment.isExpired()),
                    () -> assertEquals("TEMPORARY", assignment.assignmentType()),
                    () -> assertTrue(assignment.isAutoRenew())
            );
        }

        @Test
        @DisplayName("Should be expired with past expiration date")
        void shouldBeExpiredWithPastDate() {
            String pastDate = LocalDateTime.now().minusDays(1).format(FORMATTER);

            TemporaryAssignment assignment = new TemporaryAssignment(
                    user, role, metadata, pastDate, false);

            assertTrue(assignment.isExpired());
        }

        @Test
        @DisplayName("Should extend expiration date")
        void shouldExtendExpirationDate() {
            String initialDate = LocalDateTime.now().plusDays(7).format(FORMATTER);
            String extendedDate = LocalDateTime.now().plusDays(30).format(FORMATTER);

            TemporaryAssignment assignment = new TemporaryAssignment(
                    user, role, metadata, initialDate, true);
            assignment.extend(extendedDate);

            assertEquals(extendedDate, assignment.getExpiresAt());
        }

        @Test
        @DisplayName("Should throw for invalid date format")
        void shouldThrowForInvalidDateFormat() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TemporaryAssignment(user, role, metadata, "invalid-date", false));

            assertThrows(IllegalArgumentException.class,
                    () -> new TemporaryAssignment(user, role, metadata, null, false));
        }
    }
}