package com.sap.sailing.datamining.impl.components.aggregators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.impl.components.aggregators.AbstractParallelGroupedDataStoringAggregationProcessor;
import com.sap.sse.datamining.shared.GroupKey;

public class ParallelSpeedMedianAggregationProcessor
             extends AbstractParallelGroupedDataStoringAggregationProcessor<Speed, Speed> {
    
    private static final AggregationProcessorDefinition<Speed, Speed> DEFINITION =
            new SimpleAggregationProcessorDefinition<>(Speed.class, Speed.class, "Median", ParallelSpeedMedianAggregationProcessor.class);
    
    public static AggregationProcessorDefinition<Speed, Speed> getDefinition() {
        return DEFINITION;
    }

    private Map<GroupKey, List<Speed>> groupedValues;

    public ParallelSpeedMedianAggregationProcessor(ExecutorService executor,
            Collection<Processor<Map<GroupKey, Speed>, ?>> resultReceivers) {
        super(executor, resultReceivers, "Median");
        groupedValues = new HashMap<>();
    }

    @Override
    protected void storeElement(GroupedDataEntry<Speed> element) {
        GroupKey key = element.getKey();
        if (!groupedValues.containsKey(key)) {
            groupedValues.put(key, new ArrayList<Speed>());
        }
        groupedValues.get(key).add(element.getDataEntry());
    }

    @Override
    protected Map<GroupKey, Speed> aggregateResult() {
        Map<GroupKey, Speed> result = new HashMap<>();
        for (Entry<GroupKey, List<Speed>> groupedValuesEntry : groupedValues.entrySet()) {
            if (isAborted()) {
                break;
            }
            result.put(groupedValuesEntry.getKey(), getMedianOf(groupedValuesEntry.getValue()));
        }
        return result;
    }

    private Speed getMedianOf(List<Speed> values) {
        Collections.sort(values);
        if (listSizeIsEven(values)) {
            int index1 = (values.size() / 2) - 1;
            int index2 = index1 + 1;
            return new KnotSpeedImpl((values.get(index1).getKnots() + values.get(index2).getKnots()) / 2);
        } else {
            int index = ((values.size() + 1) / 2) - 1;
            return values.get(index);
        }
    }

    private boolean listSizeIsEven(List<?> values) {
        return values.size() % 2 == 0;
    }

}
