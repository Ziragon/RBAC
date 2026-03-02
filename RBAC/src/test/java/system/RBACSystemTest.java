package system;

import com.example.entity.Role;
import com.example.system.RBACSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RBACSystem Tests")
class RBACSystemTest {

    private RBACSystem system;

    @BeforeEach
    void setUp() {
        Role.clearNameRegistry();
        system = new RBACSystem();
    }

    @Test
    @DisplayName("Should create system with initialized managers")
    void shouldCreateSystemWithManagers() {
        assertAll(
                () -> assertNotNull(system.getUserManager()),
                () -> assertNotNull(system.getRoleManager()),
                () -> assertNotNull(system.getAssignmentManager()),
                () -> assertEquals("system", system.getCurrentUser())
        );
    }

    @Test
    @DisplayName("Should initialize with default data")
    void shouldInitializeWithDefaultData() {
        system.initialize();

        assertAll(
                () -> assertEquals(1, system.getUserManager().count()),
                () -> assertTrue(system.getUserManager().findByUsername("admin").isPresent()),
                () -> assertEquals(3, system.getRoleManager().count()),
                () -> assertTrue(system.getRoleManager().findByName("Administrator").isPresent()),
                () -> assertTrue(system.getRoleManager().findByName("Manager").isPresent()),
                () -> assertTrue(system.getRoleManager().findByName("Viewer").isPresent()),
                () -> assertEquals(1, system.getAssignmentManager().count()),
                () -> assertEquals("admin", system.getCurrentUser())
        );
    }

    @Test
    @DisplayName("Should check current user permissions after initialize")
    void shouldCheckCurrentUserPermissions() {
        system.initialize();

        assertAll(
                () -> assertTrue(system.currentUserHasPermission("READ", "users")),
                () -> assertTrue(system.currentUserHasPermission("WRITE", "users")),
                () -> assertTrue(system.currentUserHasPermission("DELETE", "users")),
                () -> assertTrue(system.currentUserHasPermission("ADMIN", "system")),
                () -> assertFalse(system.currentUserHasPermission("UNKNOWN", "resource"))
        );
    }

    @Test
    @DisplayName("Should generate statistics")
    void shouldGenerateStatistics() {
        system.initialize();

        String stats = system.generateStatistics();

        assertAll(
                () -> assertNotNull(stats),
                () -> assertTrue(stats.contains("SYSTEM STATISTICS")),
                () -> assertTrue(stats.contains("Total Users:")),
                () -> assertTrue(stats.contains("Total Roles:")),
                () -> assertTrue(stats.contains("Administrator")),
                () -> assertTrue(stats.contains("admin"))
        );
    }

    @Test
    @DisplayName("Should set and get current user")
    void shouldSetAndGetCurrentUser() {
        system.setCurrentUser("testuser");
        assertEquals("testuser", system.getCurrentUser());

        assertThrows(IllegalArgumentException.class,
                () -> system.setCurrentUser(null));
        assertThrows(IllegalArgumentException.class,
                () -> system.setCurrentUser("   "));
    }

    @Test
    @DisplayName("Should clear all data")
    void shouldClearAllData() {
        system.initialize();

        system.clearAll();

        assertAll(
                () -> assertEquals(0, system.getUserManager().count()),
                () -> assertEquals(0, system.getRoleManager().count()),
                () -> assertEquals(0, system.getAssignmentManager().count()),
                () -> assertEquals("system", system.getCurrentUser())
        );
    }

    @Test
    @DisplayName("Should create metadata with current user")
    void shouldCreateMetadataWithCurrentUser() {
        system.setCurrentUser("manager");

        var metadata = system.createMetadata("Test reason");

        assertAll(
                () -> assertEquals("manager", metadata.assignedBy()),
                () -> assertEquals("Test reason", metadata.reason()),
                () -> assertNotNull(metadata.assignedAt())
        );
    }
}