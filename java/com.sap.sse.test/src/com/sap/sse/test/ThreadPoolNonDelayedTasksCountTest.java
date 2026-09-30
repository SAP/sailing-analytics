package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Tests the
 * {@link ThreadPoolUtil#getTasksDelayedByLessThan(java.util.concurrent.ScheduledExecutorService, com.sap.sse.common.Duration)}
 * method.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ThreadPoolNonDelayedTasksCountTest {
    private ScheduledThreadPoolExecutor defaultBackgroundThreadPoolExecutor;
    private int corePoolSize;
    
    @BeforeEach
    public void setUp() {
        defaultBackgroundThreadPoolExecutor = (ScheduledThreadPoolExecutor) ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor();
        corePoolSize = defaultBackgroundThreadPoolExecutor.getCorePoolSize();
    }
    
    @AfterEach
    public void tearDown() {
        for (final Runnable task : defaultBackgroundThreadPoolExecutor.getQueue()) {
            final ScheduledFuture<?> scheduledFuture = (ScheduledFuture<?>) task;
            scheduledFuture.cancel(/* mayInterruptIfRunning */ true);
        }
    }
    
    @Test
    public void testAddingDelayedAndUndelayedThenCounting() throws InterruptedException {
        final int IMMEDIATE_TASK_COUNT = 10000;
        final int DELAYED_TASK_COUNT = 100;
        final Duration DELAY = Duration.ONE_HOUR.times(2);
        for (int i=0; i<IMMEDIATE_TASK_COUNT; i++) {
            defaultBackgroundThreadPoolExecutor.submit(()->{
                try {
                    Thread.sleep(IMMEDIATE_TASK_COUNT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        for (int i=0; i<DELAYED_TASK_COUNT; i++) {
            defaultBackgroundThreadPoolExecutor.schedule(()->{
                try {
                    Thread.sleep(IMMEDIATE_TASK_COUNT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, DELAY.asMillis(), TimeUnit.MILLISECONDS);
        }
        Thread.sleep(100); // wait for non-delayed tasks to get scheduled
        assertEquals(IMMEDIATE_TASK_COUNT-corePoolSize, Util.size(ThreadPoolUtil.INSTANCE.getTasksDelayedByLessThan(defaultBackgroundThreadPoolExecutor, DELAY.divide(2))));
        assertEquals(IMMEDIATE_TASK_COUNT+DELAYED_TASK_COUNT-corePoolSize, Util.size(ThreadPoolUtil.INSTANCE.getTasksDelayedByLessThan(defaultBackgroundThreadPoolExecutor, DELAY.times(2))));
    }
}
