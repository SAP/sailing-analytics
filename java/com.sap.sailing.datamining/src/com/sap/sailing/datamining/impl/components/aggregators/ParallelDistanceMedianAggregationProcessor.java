package com.sap.sailing.datamining.impl.components.aggregators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.impl.components.aggregators.AbstractParallelGroupedDataStoringAggregationProcessor;
import com.sap.sse.datamining.shared.GroupKey;

public class ParallelDistanceMedianAggregationProcessor
             extends AbstractParallelGroupedDataStoringAggregationProcessor<Distance, Distance> {
    
    private static final AggregationProcessorDefinition<Distance, Distance> DEFINITION =
            new SimpleAggregationProcessorDefinition<>(Distance.class, Distance.class, "Median", ParallelDistanceMedianAggregationProcessor.class);
    
    public static AggregationProcessorDefinition<Distance, Distance> getDefinition() {
        return DEFINITION;
    }

    private Map<GroupKey, List<Distance>> groupedValues;

    public ParallelDistanceMedianAggregationProcessor(ExecutorService executor,
            Collection<Processor<Map<GroupKey, Distance>, ?>> resultReceivers) {
        super(executor, resultReceivers, "Median");
        groupedValues = new HashMap<>();
    }

    @Override
    protected void storeElement(GroupedDataEntry<Distance> element) {
        GroupKey key = element.getKey();
        if (!groupedValues.containsKey(key)) {
            groupedValues.put(key, new ArrayList<Distance>());
        }
        groupedValues.get(key).add(element.getDataEntry());
    }

    @Override
    protected Map<GroupKey, Distance> aggregateResult() {
        Map<GroupKey, Distance> result = new HashMap<>();
        for (Entry<GroupKey, List<Distance>> groupedValuesEntry : groupedValues.entrySet()) {
            if (isAborted()) {
                break;
            }
            result.put(groupedValuesEntry.getKey(), getMedianOf(groupedValuesEntry.getValue()));
        }
        return result;
    }

    private Distance getMedianOf(List<Distance> values) {
        Collections.sort(values);
        if (listSizeIsEven(values)) {
            int index1 = (values.size() / 2) - 1;
            int index2 = index1 + 1;
            return new MeterDistance((values.get(index1).getMeters() + values.get(index2).getMeters()) / 2);
        } else {
            int index = ((values.size() + 1) / 2) - 1;
            return values.get(index);
        }
    }

    private boolean listSizeIsEven(List<?> values) {
        return values.size() % 2 == 0;
    }

}
