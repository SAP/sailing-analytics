package com.sap.sse.datamining.impl.components.aggregators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;

public class TestAbstractStoringParallelAggregationProcessor {

    private Collection<Processor<Map<GroupKey, Integer>, ?>> receivers;
    private boolean receiverWasToldToFinish = false;
    private GroupKey groupKey = new GenericGroupKey<>("Key");
    private Map<GroupKey, Integer> result = null;
    
    private Collection<GroupedDataEntry<Integer>> elementStore = new ArrayList<>();
    
    @BeforeEach
    public void initializeReceivers() {
        @SuppressWarnings("unchecked")
        Processor<Map<GroupKey, Integer>, Void> receiver = new NullProcessor<Map<GroupKey,Integer>, Void>((Class<Map<GroupKey, Integer>>)(Class<?>) Map.class, Void.class) {
            @Override
            public void processElement(Map<GroupKey, Integer> element) {
                result = element;
            }
            @Override
            public void finish() throws InterruptedException {
                receiverWasToldToFinish = true;
            }
        };
        
        receivers = new HashSet<>();
        receivers.add(receiver);
    }

    @Test
    public void testAbstractAggregationHandling() throws InterruptedException {
        Processor<GroupedDataEntry<Integer>, Map<GroupKey, Integer>> processor = new AbstractParallelGroupedDataStoringAggregationProcessor<Integer, Integer>(ConcurrencyTestsUtil.getSharedExecutor(), receivers, "Sum") {
            @Override
            protected void storeElement(GroupedDataEntry<Integer> element) {
                elementStore.add(element);
            }
            @Override
            protected Map<GroupKey, Integer> aggregateResult() {
                Map<GroupKey, Integer> result = new HashMap<>();
                for (GroupedDataEntry<Integer> element : elementStore) {
                    GroupKey key = element.getKey();
                    if (!result.containsKey(key)) {
                        result.put(key, 0);
                    }
                    result.put(key, result.get(key) + element.getDataEntry());
                }
                return result;
            }
        };

        processElementAndVerifyThatItWasStored(processor, new GroupedDataEntry<>(groupKey, 42));
        processElementAndVerifyThatItWasStored(processor, new GroupedDataEntry<>(groupKey, 7));
        
        processor.finish();
        assertThat("The receiver wasn't told to finish", receiverWasToldToFinish, is(true));
        int expectedData = 42 + 7;
        assertThat(result.get(groupKey), is(expectedData));
    }

    private void processElementAndVerifyThatItWasStored(Processor<GroupedDataEntry<Integer>, ?> processor, GroupedDataEntry<Integer> element) {
        processor.processElement(element);
        ConcurrencyTestsUtil.sleepFor(100); //Giving the processor time to process the instructions
        assertThat("The element store doesn't contain the previously processed element '" + element + "'", elementStore.contains(element), is(true));
    }
    
    @SuppressWarnings("unchecked")
    @Timeout(value=5, unit=TimeUnit.SECONDS)
    @Test
    public void testThatTheLockIsReleasedAfterStoringFailed() throws InterruptedException {
        Collection<Processor<Map<GroupKey, Integer>, ?>> receivers = new HashSet<>();
        receivers.add(new NullProcessor<Map<GroupKey, Integer>, Void>((Class<Map<GroupKey, Integer>>)(Class<?>) Map.class, Void.class) {
            @Override
            public void processElement(Map<GroupKey, Integer> element) {
                result = element;
            }
            @Override
            public void finish() throws InterruptedException {
                receiverWasToldToFinish = true;
            }
            @Override
            public void onFailure(Throwable failure) {
                if (failure instanceof IllegalArgumentException) {
                    // Do nothing, since a IllegalArgumentException is expected
                } else {
                    super.onFailure(failure);
                }
            }
        });
        Processor<GroupedDataEntry<Integer>, Map<GroupKey, Integer>> processor = new AbstractParallelGroupedDataStoringAggregationProcessor<Integer, Integer>(ConcurrencyTestsUtil.getSharedExecutor(), receivers, "Sum") {
            @Override
            protected void storeElement(GroupedDataEntry<Integer> element) {
                if (element.getDataEntry() < 0) {
                    throw new IllegalArgumentException("The element mustn't be negative");
                }
                elementStore.add(element);
            }
            @Override
            protected Map<GroupKey, Integer> aggregateResult() {
                Map<GroupKey, Integer> result = new HashMap<>();
                for (GroupedDataEntry<Integer> element : elementStore) {
                    GroupKey key = element.getKey();
                    if (!result.containsKey(key)) {
                        result.put(key, 0);
                    }
                    result.put(key, result.get(key) + element.getDataEntry());
                }
                return result;
            }
        };

        processor.processElement(new GroupedDataEntry<>(groupKey, -1));
        processor.processElement(new GroupedDataEntry<>(groupKey, 42));
        processor.processElement(new GroupedDataEntry<>(groupKey, 7));
        
        processor.finish();
        assertThat("The receiver wasn't told to finish", receiverWasToldToFinish, is(true));
        int expectedData = 42 + 7;
        assertThat(result.get(groupKey), is(expectedData));
    }

}
