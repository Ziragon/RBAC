package com.example;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
class CalculationTask implements Runnable {

    private final int sequenceNumber;
    private final int totalLength;
    private final int minDelay;
    private final int maxDelay;

    private long threadId;
    private int currentProgress = 0;
    private long timeSpentMs = 0;
    private boolean isFinished = false;

    @Override
    public void run() {
        this.threadId = Thread.currentThread().threadId();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalLength; i++) {
            try {
                int delay = minDelay + (int) (Math.random() * (maxDelay - minDelay));
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            currentProgress++;
        }

        this.timeSpentMs = System.currentTimeMillis() - startTime;
        this.isFinished = true;
    }
}