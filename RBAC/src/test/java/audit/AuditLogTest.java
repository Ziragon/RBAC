package audit;

import com.example.audit.AuditEntry;
import com.example.audit.AuditLog;
import com.example.system.BackgroundExecutor;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Async AuditLog Tests")
class AuditLogTest {

    private AuditLog auditLog;
    private BackgroundExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new BackgroundExecutor();
        auditLog = new AuditLog();
        auditLog.startAsyncLogger(executor);
    }

    @AfterEach
    void tearDown() {
        auditLog.stop();
        executor.close();
    }

    @Test
    @DisplayName("Should add and retrieve log entry asynchronously")
    void shouldAddAndRetrieveEntry() throws InterruptedException {
        auditLog.log("USER_CREATE", "admin", "john_doe", "Created user");

        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(latch::countDown);

        boolean finished = latch.await(2, TimeUnit.SECONDS);

        List<AuditEntry> entries = auditLog.getAll();

        assertAll(
                () -> assertTrue(finished, "Timeout waiting for async log"),
                () -> assertEquals(1, entries.size(), "Entry should be processed by background thread"),
                () -> assertEquals("USER_CREATE", entries.getFirst().action())
        );
    }

    @Test
    @DisplayName("Should save to file correctly after async processing")
    void shouldSaveToFile() throws IOException, InterruptedException {
        auditLog.log("USER_CREATE", "admin", "john", "Created user");
        auditLog.log("ROLE_ASSIGN", "admin", "john", "Assigned role");

        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(latch::countDown);

        boolean finished = latch.await(2, TimeUnit.SECONDS);

        String filename = "test_audit_async.csv";
        auditLog.saveToFile(filename);

        File file = new File(filename);
        try {
            assertAll(
                    () -> assertTrue(finished, "Timeout waiting for async log"),
                    () -> assertTrue(file.exists())
            );
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }
}