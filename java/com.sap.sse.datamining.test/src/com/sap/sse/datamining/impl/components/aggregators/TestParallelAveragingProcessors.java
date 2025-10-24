package com.sap.sse.datamining.impl.components.aggregators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.data.AverageWithStats;
import com.sap.sse.datamining.shared.impl.AverageWithStatsImpl;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;

public class TestParallelAveragingProcessors extends AbstractTestParallelAveragingProcessors<AverageWithStats<Number>> {
    
    @Test
    public void testAverageAggregationProcessor() throws InterruptedException {
        Processor<GroupedDataEntry<Number>, Map<GroupKey, AverageWithStats<Number>>> averageAggregationProcessor = ParallelGroupedNumberDataAverageAggregationProcessor
                .getDefinition().construct(ConcurrencyTestsUtil.getSharedExecutor(), receivers);
        Collection<GroupedDataEntry<Number>> elements = createElements();
        ConcurrencyTestsUtil.processElements(averageAggregationProcessor, elements);
        averageAggregationProcessor.finish();
        Map<GroupKey, AverageWithStats<Number>> expectedReceivedAggregations = computeExpectedAverageAggregations(elements);
        ConcurrencyTestsUtil.verifyResultData(receivedAggregations, expectedReceivedAggregations);
    }

    private Map<GroupKey, AverageWithStats<Number>> computeExpectedAverageAggregations(Collection<GroupedDataEntry<Number>> elements) {
        Map<GroupKey, AverageWithStats<Number>> result = new HashMap<>();
        Map<GroupKey, Number> sumAggregations = computeExpectedSumAggregations(elements);
        Map<GroupKey, Number> minAggregations = computeExpectedMinAggregations();
        Map<GroupKey, Number> maxAggregations = computeExpectedMaxAggregations();
        // TODO need to decide whether we want to pay the memory overhead for median determination for each average calculation
//        Map<GroupKey, Number> medianAggregations = computeExpectedMedianAggregations();
        Map<GroupKey, Double> elementAmountPerKey = countElementAmountPerKey(elements);
        for (Entry<GroupKey, Number> sumAggregationEntry : sumAggregations.entrySet()) {
            GroupKey key = sumAggregationEntry.getKey();
            result.put(key, new AverageWithStatsImpl<Number>(
                    /* average */ sumAggregationEntry.getValue().doubleValue() / elementAmountPerKey.get(key),
                    /* min */ minAggregations.get(key),
                    /* max */ maxAggregations.get(key),
                    /* median */ /* medianAggregations.get(key) */ null,
                    /* standardDeviation */ null,
                    /* count */ elementAmountPerKey.get(key).longValue(),
                    /* resultType */ Number.class.getName()));
        }
        return result;
    }
}
