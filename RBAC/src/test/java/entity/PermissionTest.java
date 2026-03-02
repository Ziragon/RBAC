package entity;

import com.example.entity.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Permission Entity Tests")
class PermissionTest {

    @Test
    @DisplayName("Should create permission with valid data")
    void shouldCreatePermissionWithValidData() {
        Permission permission = new Permission("READ", "users", "Can view user list");

        assertAll(
                () -> assertNotNull(permission),
                () -> assertEquals("READ", permission.name()),
                () -> assertEquals("users", permission.resource())
        );
    }

    @Test
    @DisplayName("Should normalize action and resource to uppercase")
    void shouldNormalizeToUppercase() {
        Permission permission = new Permission("Delete", "USERS", "Can delete users");

        String formatted = permission.format();

        assertNotNull(formatted);
    }

    @Test
    @DisplayName("Should format permission correctly")
    void shouldFormatPermissionCorrectly() {
        Permission permission = new Permission("WRITE", "roles", "Can create and edit roles");

        String formatted = permission.format();

        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple permissions with same action")
    void shouldHandleMultiplePermissionsWithSameAction() {
        Permission readUsers = new Permission("READ", "users", "Can view user list");
        Permission readRoles = new Permission("READ", "roles", "Can view role list");

        assertNotEquals(readUsers, readRoles);
    }
}