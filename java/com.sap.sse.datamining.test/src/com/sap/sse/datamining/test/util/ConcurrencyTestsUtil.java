package com.sap.sse.datamining.test.util;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sap.sse.datamining.components.Processor;

public class ConcurrencyTestsUtil extends TestsUtil {

    private static final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);
    private static ThreadPoolExecutor executor = createExecutor();

    public static ThreadPoolExecutor getExecutor() {
        if (executor.isShutdown()) {
            executor = createExecutor();
        }
        return executor;
    }

    private static ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }
    
    public static void sleepFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            fail("The test was interrupted: " + e.getMessage());
        }
    }

    public static <T> void processElements(Processor<T> processor,
                                 Collection<T> elements) {
        for (T element : elements) {
            processor.processElement(element);
        }
    }

    public static void tryToFinishTheProcessorInAnotherThread(final Processor<?> processor) {
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