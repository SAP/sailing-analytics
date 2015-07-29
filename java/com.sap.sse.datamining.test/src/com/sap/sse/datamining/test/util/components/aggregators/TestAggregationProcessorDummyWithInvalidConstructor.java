package com.sap.sse.datamining.test.util.components.aggregators;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.aggregators.AbstractParallelGroupedDataStoringAggregationProcessor;
import com.sap.sse.datamining.shared.GroupKey;

public class TestAggregationProcessorDummyWithInvalidConstructor extends
        AbstractParallelGroupedDataStoringAggregationProcessor<Object, Double> {

    public TestAggregationProcessorDummyWithInvalidConstructor(ExecutorService executor,
            Collection<Processor<Map<GroupKey, Double>, ?>> resultReceivers, Object constructorInvalidator) {
        super(executor, resultReceivers, "AnyDataAggregator");
    }

    @Override
    protected void storeElement(GroupedDataEntry<Object> element) {
    }

    @Override
    protected Map<GroupKey, Double> aggregateResult() {
        return null;
    }

}
