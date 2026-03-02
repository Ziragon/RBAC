package audit;

import com.example.audit.AuditEntry;
import com.example.audit.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLog Tests")
class AuditLogTest {

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    @Test
    @DisplayName("Should add and retrieve log entry")
    void shouldAddAndRetrieveEntry() {
        auditLog.log("USER_CREATE", "admin", "john_doe", "Created user");

        List<AuditEntry> entries = auditLog.getAll();

        assertAll(
                () -> assertEquals(1, entries.size()),
                () -> assertEquals("USER_CREATE", entries.getFirst().action()),
                () -> assertEquals("admin", entries.getFirst().performer()),
                () -> assertEquals("john_doe", entries.getFirst().target())
        );
    }

    @Test
    @DisplayName("Should filter by performer")
    void shouldFilterByPerformer() {
        auditLog.log("USER_CREATE", "admin", "user1", "Created");
        auditLog.log("USER_DELETE", "admin", "user2", "Deleted");
        auditLog.log("ROLE_CREATE", "manager", "role1", "Created");

        List<AuditEntry> adminEntries = auditLog.getByPerformer("admin");

        assertEquals(2, adminEntries.size());
    }

    @Test
    @DisplayName("Should filter by action")
    void shouldFilterByAction() {
        auditLog.log("USER_CREATE", "admin", "user1", "Created");
        auditLog.log("USER_CREATE", "manager", "user2", "Created");
        auditLog.log("ROLE_CREATE", "admin", "role1", "Created");

        List<AuditEntry> createUsers = auditLog.getByAction("USER_CREATE");

        assertEquals(2, createUsers.size());
    }

    @Test
    @DisplayName("Should get recent entries")
    void shouldGetRecentEntries() {
        for (int i = 0; i < 10; i++) {
            auditLog.log("ACTION", "user", "target" + i, "Details");
        }

        List<AuditEntry> recent = auditLog.getRecent(3);

        assertAll(
                () -> assertEquals(3, recent.size()),
                () -> assertEquals("target9", recent.get(2).target())
        );
    }

    @Test
    @DisplayName("Should save to file")
    void shouldSaveToFile() throws IOException {
        auditLog.log("USER_CREATE", "admin", "john", "Created user");
        auditLog.log("ROLE_ASSIGN", "admin", "john", "Assigned role");

        String filename = "test_audit.csv";
        auditLog.saveToFile(filename);

        File file = new File(filename);
        assertTrue(file.exists());

        List<String> lines = Files.readAllLines(file.toPath());
        assertAll(
                () -> assertTrue(lines.getFirst().contains("TIMESTAMP")),
                () -> assertTrue(lines.get(1).contains("USER_CREATE")),
                () -> assertEquals(3, lines.size()) // header + 2 entries
        );

        file.delete();
    }

    @Test
    @DisplayName("Should clear log")
    void shouldClearLog() {
        auditLog.log("ACTION", "user", "target", "Details");
        auditLog.log("ACTION", "user", "target", "Details");

        auditLog.clear();

        assertEquals(0, auditLog.count());
    }
}