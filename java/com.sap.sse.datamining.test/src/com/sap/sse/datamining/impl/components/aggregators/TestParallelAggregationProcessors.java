package com.sap.sse.datamining.impl.components.aggregators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;

public class TestParallelAggregationProcessors extends AbstractTestParallelAveragingProcessors<Number> {
    
    @Test
    public void testSumAggregationProcessor() throws InterruptedException {
        Processor<GroupedDataEntry<Number>, Map<GroupKey, Number>> sumAggregationProcessor = ParallelGroupedNumberDataSumAggregationProcessor.getDefinition().construct(ConcurrencyTestsUtil.getSharedExecutor(), receivers);
        Collection<GroupedDataEntry<Number>> elements = createElements();
        ConcurrencyTestsUtil.processElements(sumAggregationProcessor, elements);
        
        sumAggregationProcessor.finish();
        Map<GroupKey, Number> expectedReceivedAggregations = computeExpectedSumAggregations(elements);
        ConcurrencyTestsUtil.verifyResultData(receivedAggregations, expectedReceivedAggregations);
    }

    @Test
    public void testMedianAggregationProcessor() throws InterruptedException {
        Processor<GroupedDataEntry<Number>, Map<GroupKey, Number>> medianAggregationProcessor = ParallelGroupedNumberDataMedianAggregationProcessor.getDefinition().construct(ConcurrencyTestsUtil.getSharedExecutor(), receivers);
        Collection<GroupedDataEntry<Number>> elements = createElements();
        ConcurrencyTestsUtil.processElements(medianAggregationProcessor, elements);
        
        medianAggregationProcessor.finish();
        Map<GroupKey, Number> expectedReceivedAggregations = computeExpectedMedianAggregations();
        ConcurrencyTestsUtil.verifyResultData(receivedAggregations, expectedReceivedAggregations);
    }

    @Test
    public void testMaxAggregationProcessor() throws InterruptedException {
        Processor<GroupedDataEntry<Number>, Map<GroupKey, Number>> maxAggregationProcessor = ParallelGroupedNumberDataMaxAggregationProcessor.getDefinition().construct(ConcurrencyTestsUtil.getSharedExecutor(), receivers);
        Collection<GroupedDataEntry<Number>> elements = createElements();
        ConcurrencyTestsUtil.processElements(maxAggregationProcessor, elements);
        
        maxAggregationProcessor.finish();
        Map<GroupKey, Number> expectedReceivedAggregations = computeExpectedMaxAggregations();
        ConcurrencyTestsUtil.verifyResultData(receivedAggregations, expectedReceivedAggregations);
    }

    @Test
    public void testMinAggregationProcessor() throws InterruptedException {
        Processor<GroupedDataEntry<Number>, Map<GroupKey, Number>> minAggregationProcessor = ParallelGroupedNumberDataMinAggregationProcessor.getDefinition().construct(ConcurrencyTestsUtil.getSharedExecutor(), receivers);
        Collection<GroupedDataEntry<Number>> elements = createElements();
        ConcurrencyTestsUtil.processElements(minAggregationProcessor, elements);
        
        minAggregationProcessor.finish();
        Map<GroupKey, Number> expectedReceivedAggregations = computeExpectedMinAggregations();
        ConcurrencyTestsUtil.verifyResultData(receivedAggregations, expectedReceivedAggregations);
    }

    @Test
    public void testCountAggregationProcessor() throws InterruptedException {
        Processor<GroupedDataEntry<Object>, Map<GroupKey, Number>> countAggregationProcessor = ParallelGroupedDataCountAggregationProcessor.getDefinition().construct(ConcurrencyTestsUtil.getSharedExecutor(), receivers);
        @SuppressWarnings("unchecked")
        Collection<GroupedDataEntry<Object>> elements = (Collection<GroupedDataEntry<Object>>)(Collection<?>) createElements();
        ConcurrencyTestsUtil.processElements(countAggregationProcessor, elements);
        countAggregationProcessor.finish();
        Map<GroupKey, Number> expectedReceivedAggregations = computeExpectedCountAggregations();
        ConcurrencyTestsUtil.verifyResultData(receivedAggregations, expectedReceivedAggregations);
    }

    private Map<GroupKey, Number> computeExpectedCountAggregations() {
        Map<GroupKey, Number> result = new HashMap<>();
        result.put(new GenericGroupKey<Integer>(1), 3.0);
        result.put(new GenericGroupKey<Integer>(2), 4.0);
        result.put(new GenericGroupKey<Integer>(3), 3.0);
        return result;
    }
}
