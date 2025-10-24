package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sap.sse.util.ThreadPoolUtil;

public class TestRepeatingTaskCancelsUponException {
    int counter;
    volatile int repetitions;
    
    @Test
    public void testTerminationUponException() throws InterruptedException {
        counter = 0;
        repetitions = 100;
        final long intervalInMillis = 10;
        ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().scheduleAtFixedRate(this::run,
                intervalInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
        Thread.sleep(intervalInMillis*repetitions+500);
        assertEquals(repetitions, counter);
    }
    
    private void run() {
        if (++counter == repetitions) {
            throw new RuntimeException("Terminating");
        }
    }
}
