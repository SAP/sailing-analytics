package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sse.util.ThreadPoolUtil;

@Timeout(value=10, unit=TimeUnit.SECONDS)
public class TestRepeatingTaskCancelsUponException {
    int counter;
    volatile int repetitions;
    final CyclicBarrier barrier = new CyclicBarrier(2);
    
    @Test
    public void testTerminationUponException() throws InterruptedException, BrokenBarrierException {
        counter = 0;
        repetitions = 100;
        final long intervalInMillis = 10;
        ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().scheduleAtFixedRate(this::run,
                intervalInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
        barrier.await();
        assertEquals(repetitions, counter);
    }
    
    private void run() {
        if (++counter == repetitions) {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException("Terminating");
        }
    }
}
