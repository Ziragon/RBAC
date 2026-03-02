package entity;

import com.example.entity.Permission;
import com.example.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Role Entity Tests")
class RoleTest {

    private Permission readUsers;
    private Permission writeUsers;
    private static long counter = 0;

    @BeforeEach
    void setUp() {
        readUsers = new Permission("READ", "users", "Can view users");
        writeUsers = new Permission("WRITE", "users", "Can edit users");
    }

    private String uniqueName(String base) {
        return base + "_" + System.nanoTime() + "_" + (++counter);
    }

    @Test
    @DisplayName("Should create role with permissions")
    void shouldCreateRoleWithPermissions() {
        String name = uniqueName("Admin");

        Role role = Role.create(name, "Full access", Set.of(readUsers, writeUsers));

        assertAll(
                () -> assertNotNull(role.getId()),
                () -> assertEquals(name, role.getName()),
                () -> assertEquals(2, role.getPermissions().size())
        );
    }

    @Test
    @DisplayName("Should throw exception for null or empty name")
    void shouldThrowForInvalidName() {
        assertThrows(IllegalArgumentException.class,
                () -> Role.create(null, "Desc", Set.of()));

        assertThrows(IllegalArgumentException.class,
                () -> Role.create("", "Desc", Set.of()));
    }

    @Test
    @DisplayName("Should throw exception for duplicate name")
    void shouldThrowForDuplicateName() {
        String name = uniqueName("Duplicate");
        Role.create(name, "First", Set.of());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Role.create(name, "Second", Set.of())
        );

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should add and check permissions")
    void shouldManagePermissions() {
        Role role = Role.create(uniqueName("Manager"), "Test", Set.of(readUsers));

        role.addPermission(writeUsers);

        assertAll(
                () -> assertTrue(role.hasPermission(readUsers)),
                () -> assertTrue(role.hasPermission(writeUsers)),
                () -> assertTrue(role.hasPermission("WRITE", "users")),
                () -> assertFalse(role.hasPermission("DELETE", "users"))
        );
    }

    @Test
    @DisplayName("Should remove permission")
    void shouldRemovePermission() {
        Role role = Role.create(uniqueName("Remover"), "Test", Set.of(readUsers, writeUsers));

        role.removePermission(readUsers);

        assertAll(
                () -> assertFalse(role.hasPermission(readUsers)),
                () -> assertTrue(role.hasPermission(writeUsers)),
                () -> assertEquals(1, role.getPermissions().size())
        );
    }
}