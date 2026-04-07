package system;

import com.example.system.BackgroundExecutor;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BackgroundExecutor Unit Tests")
class BackgroundExecutorTest {

    private BackgroundExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new BackgroundExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    @DisplayName("Should execute runnable task")
    void shouldExecuteRunnable() throws InterruptedException {
        AtomicBoolean flag = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(() -> {
            flag.set(true);
            latch.countDown();
        });

        boolean finished = latch.await(2, TimeUnit.SECONDS);

        assertAll(
                () -> assertTrue(finished, "Task should finish in time"),
                () -> assertTrue(flag.get(), "Flag should be set to true")
        );
    }

    @Test
    @DisplayName("Should submit callable and return result")
    void shouldSubmitCallable() throws Exception {
        Future<String> future = executor.submit(() -> {
            Thread.sleep(100);
            return "Done";
        });

        assertEquals("Done", future.get(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle multiple tasks")
    void shouldHandleMassiveTasks() throws InterruptedException {
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.execute(latch::countDown);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All virtual threads should complete");
    }
}