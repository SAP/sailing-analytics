package com.sap.sse.datamining.impl.components.aggregators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import org.omg.CORBA.DoubleHolder;
import org.omg.CORBA.IntHolder;

import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.shared.GroupKey;

public class ParallelGroupedNumberDataAverageAggregationProcessor
            extends AbstractParallelGroupedDataStoringAggregationProcessor<Number, Number> {
    private static final AggregationProcessorDefinition<Number, Number> DEFINITION =
            new SimpleAggregationProcessorDefinition<>(Number.class, Number.class, "Average", ParallelGroupedNumberDataAverageAggregationProcessor.class);
    
    public static AggregationProcessorDefinition<Number, Number> getDefinition() {
        return DEFINITION;
    }

    private final Map<GroupKey, DoubleHolder> sumPerKey;
    private final Map<GroupKey, IntHolder> elementAmountPerKey;

    public ParallelGroupedNumberDataAverageAggregationProcessor(ExecutorService executor,
            Collection<Processor<Map<GroupKey, Number>, ?>> resultReceivers) {
        super(executor, resultReceivers, "Average");
        elementAmountPerKey = new HashMap<>();
        sumPerKey = new HashMap<>();
    }

    @Override
    protected void storeElement(GroupedDataEntry<Number> element) {
        if (element.getDataEntry() != null) {
            incrementElementAmount(element);
            DoubleHolder aggregate = sumPerKey.get(element.getKey());
            if (aggregate == null) {
                aggregate = new DoubleHolder(element.getDataEntry().doubleValue());
                sumPerKey.put(element.getKey(), aggregate);
            } else {
                aggregate.value += element.getDataEntry().doubleValue();
            }
        }
    }

    private void incrementElementAmount(GroupedDataEntry<Number> element) {
        GroupKey key = element.getKey();
        IntHolder currentAmount = elementAmountPerKey.get(key);
        if (currentAmount == null) {
            elementAmountPerKey.put(key, new IntHolder(1));
        } else {
            currentAmount.value++;
        }
    }

    @Override
    protected Map<GroupKey, Number> aggregateResult() {
        Map<GroupKey, Number> result = new HashMap<>();
        for (Entry<GroupKey, DoubleHolder> sumAggregationEntry : sumPerKey.entrySet()) {
            GroupKey key = sumAggregationEntry.getKey();
            result.put(key, sumAggregationEntry.getValue().value / elementAmountPerKey.get(key).value);
        }
        return result;
    }

}
