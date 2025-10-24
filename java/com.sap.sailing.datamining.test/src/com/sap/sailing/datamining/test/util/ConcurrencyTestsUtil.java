package com.sap.sailing.datamining.test.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.DataMiningExecutorService;

public class ConcurrencyTestsUtil {

    private static final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);
    private static final ExecutorService executor = new DataMiningExecutorService(THREAD_POOL_SIZE);

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void sleepFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            fail("The test was interrupted: " + e.getMessage());
        }
    }

    public static void tryToFinishTheProcessorInAnotherThread(final Processor<?, ?> processor) {
        Runnable finishingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    processor.finish();
                } catch (InterruptedException e) {
                    fail("The test was interrupted: " + e.getMessage());
                }
            }
        };
        getExecutor().execute(finishingRunnable);
    }

    protected ConcurrencyTestsUtil() {
    }

}