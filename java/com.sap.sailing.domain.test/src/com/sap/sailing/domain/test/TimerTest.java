package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests around the Timer class; e.g., does it kill a timer if a task throws an exception?
 * 
 * @author Axel Uhl (d043530)
 * 
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class TimerTest {
    @Test
    public void exceptionInTask() throws InterruptedException {
        Timer t = new Timer("Test");
        final TimerTask taskThrowingException = new TimerTask() {
            @Override
            public void run() {
                throw new RuntimeException("An exception that may or may not kill the timer");
            }
        };
        t.schedule(taskThrowingException, 1l);
        try {
            for (int i = 0; i < 100; i++) {
                Thread.sleep(10);
                final boolean[] result = new boolean[1];
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (result) {
                            result[0] = true;
                            result.notifyAll();
                        }
                    }
                }, 1l);
            }
            fail("Expected IllegalStateException because task terminating abnormally cancels the timer");
        } catch (IllegalStateException e) {
            // expected
        }
    }
}
