package system;

import com.example.assignment.PermanentAssignment;
import com.example.entity.AssignmentMetadata;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.system.RBACSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RBAC System Concurrency Tests")
class RBACConcurrencyTest {

    private RBACSystem system;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        Role.clearNameRegistry();
        system = new RBACSystem();
        system.initialize();

        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterEach
    void tearDown() {
        system.getAuditLog().stop();
        system.getExecutor().close();
        executor.shutdownNow();
    }

    @Test
    @DisplayName("Should handle 10,000 concurrent user creations without losing data")
    void shouldHandleMassiveConcurrentCreates() throws InterruptedException {
        int threadCount = 100;
        int createsPerThread = 100;

        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                try {
                    startGun.await();
                    for (int j = 0; j < createsPerThread; j++) {
                        String username = "user_" + threadId + "_" + j;
                        system.getUserManager().add(new User(username, "Test User", username + "@test.com"));
                    }
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();

        assertTrue(finishLine.await(5, TimeUnit.SECONDS), "Timeout during concurrent creation");

        int expectedUsers = (threadCount * createsPerThread) + 1;
        assertEquals(expectedUsers, system.getUserManager().count(),
                "Some users were overwritten or dropped.");
    }

    @Test
    @DisplayName("Should prevent concurrent duplicate role assignments to the same user")
    void shouldPreventDuplicateAssignments() throws InterruptedException {
        User targetUser = new User("target", "Target User", "target@test.com");
        system.getUserManager().add(targetUser);

        Role managerRole = system.getRoleManager()
                .findByName("Manager")
                .orElseThrow(() -> new AssertionError("Role 'Manager' not found"));

        int threadCount = 50;
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);

        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    startGun.await();
                    AssignmentMetadata metadata = AssignmentMetadata.now("system", "Mass assign");
                    PermanentAssignment assignment = new PermanentAssignment(targetUser, managerRole, metadata);

                    system.getAssignmentManager().add(assignment);
                } catch (IllegalStateException _) {
                    exceptionCount.incrementAndGet();
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        assertTrue(finishLine.await(5, TimeUnit.SECONDS));

        long assignmentCount = system.getAssignmentManager().findByUser(targetUser).size();
        assertEquals(1, assignmentCount, "User got duplicate assignments for the same role!");

        assertEquals(threadCount - 1, exceptionCount.get(), "Race condition in assignment validation!");
    }

    @Test
    @DisplayName("Should safely process mixed Read, Write, and Filter operations")
    void shouldHandleMixedWorkload() throws InterruptedException {
        int threadCount = 200;
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.execute(() -> {
                try {
                    startGun.await();

                    // Четные потоки делают записи
                    // Нечетные читают и фильтруют

                    if (id % 2 == 0) {
                        String name = "mixed_" + id;
                        system.getUserManager().add(new User(name, "Mixed", name + "@test.com"));
                        Role role = Role.create("Role_" + id, "Desc", Set.of());
                        system.getRoleManager().add(role);
                    } else {
                        system.getUserManager().findAll(u -> u.username().startsWith("mixed"),
                                Comparator.comparing(User::username));
                        system.getRoleManager().findAll();
                    }
                } catch (Exception e) {
                    fail("Exception during mixed workload: " + e.getClass().getSimpleName());
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        assertTrue(finishLine.await(10, TimeUnit.SECONDS));

        // + 1 - админ при инициализации RBAC
        assertEquals(100 + 1, system.getUserManager().count());
        // + 3 - дефолтные роли
        assertEquals(100 + 3, system.getRoleManager().count());
    }
}