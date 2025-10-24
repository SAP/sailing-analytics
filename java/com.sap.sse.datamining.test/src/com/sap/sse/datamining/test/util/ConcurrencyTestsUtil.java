package com.sap.sse.datamining.test.util;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.DataMiningExecutorService;
import com.sap.sse.datamining.shared.GroupKey;

public class ConcurrencyTestsUtil extends TestsUtil {
    
    private static final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);
    private static final ExecutorService executor = new DataMiningExecutorService(THREAD_POOL_SIZE);

    public static ExecutorService getSharedExecutor() {
        return executor;
    }

    public static void sleepFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            fail("The test was interrupted: " + e.getMessage());
        }
    }

    public static <T> void processElements(Processor<? super T, ?> processor, Collection<T> elements) {
        for (T element : elements) {
            processor.processElement(element);
        }
    }

    public static <ResultDataType> void verifyResultData(Map<GroupKey, ResultDataType> resultData, Map<GroupKey, ResultDataType> expectedResultData) {
        assertThat("No aggregation has been received.", resultData, notNullValue());
        for (Entry<GroupKey, ResultDataType> expectedReceivedAggregationEntry : expectedResultData.entrySet()) {
            assertThat("The expected aggregation entry '" + expectedReceivedAggregationEntry + "' wasn't received.",
                    resultData.containsKey(expectedReceivedAggregationEntry.getKey()), is(true));
            assertThat("The result for group '" + expectedReceivedAggregationEntry.getKey() + "' isn't correct.",
                    resultData.get(expectedReceivedAggregationEntry.getKey()), is(expectedReceivedAggregationEntry.getValue()));
        }
    }

    public static Thread tryToFinishTheProcessorInAnotherThread(final Processor<?, ?> processor) {
        Thread finishingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processor.finish();
                } catch (InterruptedException e) {
                    fail("The test was interrupted: " + e.getMessage());
                }
            }
        });
        finishingThread.start();
        return finishingThread;
    }

    protected ConcurrencyTestsUtil() {
    }

}