package com.example.system;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BackgroundExecutor implements AutoCloseable {

    private final ExecutorService executor;

    public BackgroundExecutor() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    // Runnable
    public void execute(Runnable task) {
        executor.execute(task);
    }

    // Callable
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}