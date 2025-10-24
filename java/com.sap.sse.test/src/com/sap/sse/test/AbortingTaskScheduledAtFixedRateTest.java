package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.sap.sse.util.ThreadPoolUtil;

public class AbortingTaskScheduledAtFixedRateTest {
    private static final int RATE_IN_MILLIS = 100;
    private static final Logger logger = Logger.getLogger(AbortingTaskScheduledAtFixedRateTest.class.getName());
    
    /**
     * Asserts that calling {@link ScheduledFuture#cancel(boolean)} on an object returned by
     * {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} keeps the
     * task from executing again.
     */
    @Test
    public void testAbortTaskScheduledAtFixedRate() throws InterruptedException {
        final int[] executions = new int[1];
        final boolean[] aborted = new boolean[1];
        final ScheduledExecutorService executor = ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor();
        final Runnable runnable = () -> {
                synchronized(AbortingTaskScheduledAtFixedRateTest.this) {
                    if (!aborted[0]) {
                        executions[0]++;
                        AbortingTaskScheduledAtFixedRateTest.this.notifyAll();
                    }
                }
                logger.info("Task run");
        };
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(runnable, 0, RATE_IN_MILLIS, TimeUnit.MILLISECONDS);
        synchronized(this) {
            while (executions[0] == 0) {
                this.wait();
            }
            logger.info("detected execution of task");
            future.cancel(/* mayInterruptIfRunning */ false);
            aborted[0] = true;
        }
        Thread.sleep(10*RATE_IN_MILLIS);
        assertEquals(1, executions[0]);
    }
}
