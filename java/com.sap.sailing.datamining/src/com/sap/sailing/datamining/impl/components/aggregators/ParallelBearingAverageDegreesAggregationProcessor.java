package com.sap.sailing.datamining.impl.components.aggregators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.BearingCluster;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.impl.components.aggregators.AbstractParallelGroupedDataStoringAggregationProcessor;
import com.sap.sse.datamining.shared.GroupKey;

public class ParallelBearingAverageDegreesAggregationProcessor
            extends AbstractParallelGroupedDataStoringAggregationProcessor<Bearing, Double> {
    
    private static final AggregationProcessorDefinition<Bearing, Double> DEFINITION =
            new SimpleAggregationProcessorDefinition<>(Bearing.class, Double.class, "Average", ParallelBearingAverageDegreesAggregationProcessor.class);
    
    private final Map<GroupKey, BearingCluster> results;

    public static AggregationProcessorDefinition<Bearing, Double> getDefinition() {
        return DEFINITION;
    }

    public ParallelBearingAverageDegreesAggregationProcessor(ExecutorService executor,
            Collection<Processor<Map<GroupKey, Double>, ?>> resultReceivers) {
        super(executor, resultReceivers, "Average");
        results = new HashMap<>();
    }

    @Override
    protected void storeElement(GroupedDataEntry<Bearing> element) {
        BearingCluster cluster = results.get(element.getKey());
        if (cluster == null) {
            cluster = new BearingCluster();
            results.put(element.getKey(), cluster);
        }
        cluster.add(element.getDataEntry());
    }

    @Override
    protected Map<GroupKey, Double> aggregateResult() {
        Map<GroupKey, Double> result = new HashMap<>();
        for (Entry<GroupKey, BearingCluster> clusterEntry : results.entrySet()) {
            if (isAborted()) {
                break;
            }
            GroupKey key = clusterEntry.getKey();
            result.put(key, clusterEntry.getValue().getAverage().getDegrees());
        }
        return result;
    }

}
