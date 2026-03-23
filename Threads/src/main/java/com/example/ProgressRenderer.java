package com.example;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class ProgressRenderer implements Runnable {

    private final List<CalculationTask> tasks;
    private final int renderFreq;
    private volatile boolean running = true;

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}

        while (running) {
            render();
            try {
                Thread.sleep(renderFreq);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        render();
        System.out.println();
    }

    private void render() {
        StringBuilder output = new StringBuilder();

        for (CalculationTask task : tasks) {
            output.append(formatTaskOutput(task)).append("\n");
        }

        System.out.print(output);
        System.out.flush();

        if (running) {
            String moveUpCode = String.format("\033[%dF", tasks.size());
            System.out.print(moveUpCode);
            System.out.flush();
        }
    }

    private String formatTaskOutput(CalculationTask task) {
        StringBuilder progressBar = new StringBuilder("[");
        int progress = task.getCurrentProgress();
        int total = task.getTotalLength();

        for (int i = 0; i < total; i++) {
            if (i < progress) {
                progressBar.append("|");
            } else {
                progressBar.append("-");
            }
        }
        progressBar.append("]");

        String status = task.isFinished() ? String.format("%d ms", task.getTimeSpentMs()) : "calculating";

        String output = String.format("Thread %2d (ID: %4d) %s | %s",
                task.getSequenceNumber(), task.getThreadId(), progressBar, status);

        return output + "\033[K";
    }
}