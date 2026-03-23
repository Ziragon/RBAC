package com.example;

import java.util.ArrayList;
import java.util.List;

// При запуске через Intellij прогрессбары в консоли выводятся подряд (без перемещения курсора)
// Через gradle курсор консоли перемещается, но у меня возникают визуальные баги при рендере
// Через DELAY и RENDER_FREQ можно сделать консоль плавнее, если она дерганая
public class Main {
    private static final int THREAD_COUNT = 6;
    private static final int CALC_LENGTH = 40;
    private static final int MIN_DELAY_MS = 100;
    private static final int MAX_DELAY_MS = 200;
    private static final int RENDER_FREQUENCY = 100; // частота обновления консоли

    public static void main(String[] args) {

        System.out.println("Multithreading calculations...\n");

        List<CalculationTask> tasks = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(new CalculationTask(i + 1, CALC_LENGTH, MIN_DELAY_MS, MAX_DELAY_MS));
        }

        ProgressRenderer renderer = new ProgressRenderer(tasks, RENDER_FREQUENCY);
        Thread rendererThread = new Thread(renderer);
        rendererThread.start();

        for (CalculationTask task : tasks) {
            Thread t = new Thread(task);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        renderer.stop();
        try {
            rendererThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("End of calculations");
    }
}
