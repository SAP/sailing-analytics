package com.sap.sse.datamining.impl.components.aggregators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.SimpleAggregationProcessorDefinition;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.data.PairWithStats;
import com.sap.sse.datamining.shared.impl.PairWithStatsImpl;

@SuppressWarnings("rawtypes")
public class ParallelGroupedNumberPairAverageAggregationProcessor
            extends AbstractParallelGroupedDataStoringAggregationProcessor<Pair, PairWithStats<Number>> {
    private static final Class<?> _c = PairWithStats.class;
    @SuppressWarnings("unchecked")
    private static final Class<PairWithStats<Number>> _cc = (Class<PairWithStats<Number>>) _c;
    
    private static final AggregationProcessorDefinition<Pair, PairWithStats<Number>> DEFINITION =
            new SimpleAggregationProcessorDefinition<>(Pair.class,
                    _cc, "Average", ParallelGroupedNumberPairAverageAggregationProcessor.class);
    
    public static AggregationProcessorDefinition<Pair, PairWithStats<Number>> getDefinition() {
        return DEFINITION;
    }

    private final Map<GroupKey, Pair<Number, Number>> sumPerKey;
    private final Map<GroupKey, Pair<Number, Number>> minPerKey;
    private final Map<GroupKey, Pair<Number, Number>> maxPerKey;
    private final Map<GroupKey, AtomicLong> elementAmountPerKey;

    public ParallelGroupedNumberPairAverageAggregationProcessor(ExecutorService executor,
            Collection<Processor<Map<GroupKey, PairWithStats<Number>>, ?>> resultReceivers) {
        super(executor, resultReceivers, "PairAverage");
        elementAmountPerKey = new HashMap<>();
        sumPerKey = new HashMap<>();
        minPerKey = new HashMap<>();
        maxPerKey = new HashMap<>();
    }

    @Override
    protected void storeElement(GroupedDataEntry<Pair> element) {
        if (element.getDataEntry() != null) {
            incrementElementAmount(element);
            // concurrency is not an issue here; needsSynchronization() returns true
            Pair<Number, Number> aggregate = sumPerKey.get(element.getKey());
            Number a = ((Number) element.getDataEntry().getA());
            Number b = ((Number) element.getDataEntry().getB());

            final Double doubleValueA = a == null ? null : a.doubleValue();
            final Double doubleValueB = b == null ? null : b.doubleValue();
            
            if (doubleValueA != null && doubleValueB != null) {
                if(aggregate == null) {
                    aggregate = new Pair<>(doubleValueA, doubleValueB);
                    sumPerKey.put(element.getKey(), aggregate);
                }
                else {
                    sumPerKey.put(element.getKey(), new Pair<>(doubleValueA + aggregate.getA().doubleValue(),doubleValueB + aggregate.getB().doubleValue()));
                }
                if (!minPerKey.containsKey(element.getKey()) || doubleValueA < minPerKey.get(element.getKey()).getA().doubleValue() && doubleValueB < minPerKey.get(element.getKey()).getB().doubleValue()) {
                    minPerKey.put(element.getKey(), new Pair<>(doubleValueA, doubleValueB));
                }
                if (!maxPerKey.containsKey(element.getKey()) || doubleValueA > maxPerKey.get(element.getKey()).getA().doubleValue() && doubleValueB > maxPerKey.get(element.getKey()).getB().doubleValue()) {
                    maxPerKey.put(element.getKey(), new Pair<>(doubleValueA, doubleValueB));
                }
            } 
        }
    }

    private void incrementElementAmount(GroupedDataEntry<Pair> element) {
        GroupKey key = element.getKey();
        // concurrency is not an issue here; needsSynchronization() returns true
        AtomicLong currentAmount = elementAmountPerKey.get(key);
        if (currentAmount == null) {
            elementAmountPerKey.put(key, new AtomicLong(1));
        } else {
            currentAmount.incrementAndGet();
        }
    }

    @Override
    protected Map<GroupKey, PairWithStats<Number>> aggregateResult() {
        Map<GroupKey, PairWithStats<Number>> result = new HashMap<>();
        for (Entry<GroupKey, Pair<Number, Number>> sumAggregationEntry : sumPerKey.entrySet()) {
            if (isAborted()) {
                break;
            }
            GroupKey key = sumAggregationEntry.getKey();
            result.put(key, new PairWithStatsImpl<Number>(new Pair<Number, Number>(sumAggregationEntry.getValue().getA() != null ? sumAggregationEntry.getValue().getA().doubleValue() / elementAmountPerKey.get(key).get() : null, sumAggregationEntry.getValue().getB() != null ? sumAggregationEntry.getValue().getB().doubleValue() / elementAmountPerKey.get(key).get() : null) ,
                    minPerKey.get(key), maxPerKey.get(key),
                    /* median */ null,
                    /* standardDeviation */ null,
                    /* individualPairs */ null,
                    /* count */ elementAmountPerKey.get(key).get(),
                    Pair.class.getName()));
        }
        return result;
    }

}
