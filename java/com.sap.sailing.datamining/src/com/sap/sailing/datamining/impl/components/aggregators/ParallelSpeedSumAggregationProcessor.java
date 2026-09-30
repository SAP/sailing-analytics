package com.sap.sailing.datamining.impl.components.aggregators;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.shared.GroupKey;

public class ParallelSpeedSumAggregationProcessor extends
        AbstractParallelSumAggregationProcessor<Speed> {
    
    private static final AggregationProcessorDefinition<Speed, Speed> DEFINITION =
            new SimpleAggregationProcessorDefinition<>(Speed.class, Speed.class, "Sum", ParallelSpeedSumAggregationProcessor.class);
    
    public static AggregationProcessorDefinition<Speed, Speed> getDefinition() {
        return DEFINITION;
    }
    
    public ParallelSpeedSumAggregationProcessor(ExecutorService executor, Collection<Processor<Map<GroupKey, Speed>, ?>> resultReceivers) {
        super(executor, resultReceivers);
    }

    @Override
    protected Speed add(Speed t1, Speed t2) {
        return new KnotSpeedImpl(t1.getKnots() + t2.getKnots());
    }
}
