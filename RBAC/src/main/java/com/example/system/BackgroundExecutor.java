package com.example.system;

import java.util.concurrent.*;

public class BackgroundExecutor implements AutoCloseable {

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;

    public BackgroundExecutor() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Maintenance-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // Runnable
    public void execute(Runnable task) {
        executor.execute(task);
    }

    // Callable
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public void scheduleTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    @Override
    public void close() {
        executor.shutdown();
        scheduler.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) executor.shutdownNow();
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) scheduler.shutdownNow();
        } catch (InterruptedException _) {
            executor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}