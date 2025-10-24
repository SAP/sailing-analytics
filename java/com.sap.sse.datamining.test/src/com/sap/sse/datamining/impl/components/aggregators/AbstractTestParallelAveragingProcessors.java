package com.sap.sse.datamining.impl.components.aggregators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.test.util.components.NullProcessor;

public abstract class AbstractTestParallelAveragingProcessors<T> {
    
    protected Collection<Processor<Map<GroupKey, T>, ?>> receivers;
    protected Map<GroupKey, T> receivedAggregations;

    protected Map<GroupKey, Number> computeExpectedSumAggregations(Collection<GroupedDataEntry<Number>> elements) {
        Map<GroupKey, Number> expectedSumAggregations = new HashMap<>();
        for (GroupedDataEntry<Number> element : elements) {
            GroupKey key = element.getKey();
            if (!expectedSumAggregations.containsKey(key)) {
                expectedSumAggregations.put(key, 0.0);
            }
            Number currentValue = expectedSumAggregations.get(key);
            expectedSumAggregations.put(key, currentValue.doubleValue() + element.getDataEntry().doubleValue());
        }
        return expectedSumAggregations;
    }

    protected Map<GroupKey, Double> countElementAmountPerKey(Collection<GroupedDataEntry<Number>> elements) {
        Map<GroupKey, Double> elementAmountPerKey = new HashMap<>();
        for (GroupedDataEntry<?> element : elements) {
            GroupKey key = element.getKey();
            if (!elementAmountPerKey.containsKey(key)) {
                elementAmountPerKey.put(key, 0.0);
            }
            Double currentAmount = elementAmountPerKey.get(key);
            elementAmountPerKey.put(key, currentAmount + 1.0);
        }
        return elementAmountPerKey;
    }

    protected Map<GroupKey, Number> computeExpectedMedianAggregations() {
        Map<GroupKey, Number> result = new HashMap<>();
        result.put(new GenericGroupKey<Integer>(1), 7.0);
        result.put(new GenericGroupKey<Integer>(2), 6.0);
        result.put(new GenericGroupKey<Integer>(3), 5.0);
        return result;
    }

    protected Map<GroupKey, Number> computeExpectedMaxAggregations() {
        Map<GroupKey, Number> result = new HashMap<>();
        result.put(new GenericGroupKey<Integer>(1), 10.0);
        result.put(new GenericGroupKey<Integer>(2), 7.0);
        result.put(new GenericGroupKey<Integer>(3), 5.0);
        return result;
    }

    protected Map<GroupKey, Number> computeExpectedMinAggregations() {
        Map<GroupKey, Number> result = new HashMap<>();
        result.put(new GenericGroupKey<Integer>(1), 5.0);
        result.put(new GenericGroupKey<Integer>(2), 3.0);
        result.put(new GenericGroupKey<Integer>(3), 5.0);
        return result;
    }

    protected Collection<GroupedDataEntry<Number>> createElements() {
        Collection<GroupedDataEntry<Number>> elements = new ArrayList<>();
        
        GroupKey firstGroupKey = new GenericGroupKey<Integer>(1);
        elements.add(new GroupedDataEntry<Number>(firstGroupKey, 5.0));
        elements.add(new GroupedDataEntry<Number>(firstGroupKey, 10.0));
        elements.add(new GroupedDataEntry<Number>(firstGroupKey, 7.0));
        
        GroupKey secondGroupKey = new GenericGroupKey<Integer>(2);
        elements.add(new GroupedDataEntry<Number>(secondGroupKey, 5.0));
        elements.add(new GroupedDataEntry<Number>(secondGroupKey, 3.0));
        elements.add(new GroupedDataEntry<Number>(secondGroupKey, 7.0));
        elements.add(new GroupedDataEntry<Number>(secondGroupKey, 7.0));
        
        GroupKey thirdGroupKey = new GenericGroupKey<Integer>(3);
        elements.add(new GroupedDataEntry<Number>(thirdGroupKey, 5.0));
        elements.add(new GroupedDataEntry<Number>(thirdGroupKey, 5.0));
        elements.add(new GroupedDataEntry<Number>(thirdGroupKey, 5.0));
        
        return elements;
    }
    
    @BeforeEach
    public void initializeResultReceivers() {
        @SuppressWarnings("unchecked")
        Processor<Map<GroupKey, T>, Void> receiver = new NullProcessor<Map<GroupKey, T>, Void>((Class<Map<GroupKey, T>>)(Class<?>) Map.class, Void.class) {
            @Override
            public void processElement(Map<GroupKey, T> element) {
                receivedAggregations = element;
            }
        };
        receivers = new ArrayList<>();
        receivers.add(receiver);
    }
}
