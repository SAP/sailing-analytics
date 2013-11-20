package com.sap.sailing.datamining.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.sap.sailing.datamining.ParallelAggregator;
import com.sap.sailing.datamining.ParallelDataRetriever;
import com.sap.sailing.datamining.ParallelExtractor;
import com.sap.sailing.datamining.ParallelFilter;
import com.sap.sailing.datamining.ParallelGrouper;
import com.sap.sailing.datamining.Query;
import com.sap.sailing.datamining.shared.GroupKey;
import com.sap.sailing.datamining.shared.QueryResult;
import com.sap.sailing.datamining.shared.QueryResultImpl;
import com.sap.sailing.server.RacingEventService;

public class QueryImpl<DataType, ExtractedType, AggregatedType> implements Query<DataType, AggregatedType> {

    private ParallelDataRetriever<DataType> retriever;
    private ParallelFilter<DataType> filter;
    private ParallelGrouper<DataType> grouper;

    private ParallelExtractor<DataType, ExtractedType> extractor;
    private ParallelAggregator<ExtractedType, AggregatedType> aggregator;
    
    public QueryImpl(ParallelDataRetriever<DataType> retriever, ParallelFilter<DataType> filter, ParallelGrouper<DataType> grouper,
                     ParallelExtractor<DataType, ExtractedType> extractor, ParallelAggregator<ExtractedType, AggregatedType> aggregator) {
        this.retriever = retriever;
        this.filter = filter;
        this.grouper = grouper;
        this.extractor = extractor;
        this.aggregator = aggregator;
    }

    @Override
    public QueryResult<AggregatedType> run(RacingEventService racingEventService) throws InterruptedException, ExecutionException {
        final long startTime = System.nanoTime();
        
        Collection<DataType> retrievedData = retriever.start(racingEventService).get();
        Collection<DataType> filteredData = filter.start(retrievedData).get();
        Map<GroupKey, Collection<DataType>> groupedData = grouper.start(filteredData).get();
        Map<GroupKey, Collection<ExtractedType>> extractedData = extractor.start(groupedData).get();
        Map<GroupKey, AggregatedType> aggregatedData = aggregator.start(extractedData).get();

        QueryResultImpl<AggregatedType> result = new QueryResultImpl<AggregatedType>(retrievedData.size(), filteredData.size(), createResultSignifier(), extractor.getUnit(), extractor.getValueDecimals());
        for (Entry<GroupKey, AggregatedType> resultEntry : aggregatedData.entrySet()) {
            result.addResult(resultEntry.getKey(), resultEntry.getValue());
        }
        
        final long endTime = System.nanoTime();
        result.setCalculationTimeInNanos(endTime - startTime);
        return result;
    }

    private String createResultSignifier() {
        return aggregator.getName() + " of the " + extractor.getSignifier();
    }

}
