package com.sap.sse.datamining.impl.components;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.criterias.AbstractFilterCriterion;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;

public class TestRetrieverFilterProcessorChain {
    
    private String retrievedDataTypeMessageKey;
    private Processor<Iterable<Integer>, Integer> retrievalProcessor;
    private Collection<Integer> dataSource;
    
    private Collection<Integer> receivedResults = new ArrayList<>();
    
    @BeforeEach
    public void setUpDataSource() {
        dataSource = Arrays.asList(-2, -1, 0, 1, 2);
    }

    @Test
    public void testFiltration() throws InterruptedException {
        retrievalProcessor.processElement(dataSource);
        retrievalProcessor.finish();
        ConcurrencyTestsUtil.sleepFor(500); //Giving the processor time to finish

        Collection<Integer> expectedResults = Arrays.asList(0, 1, 2);
        assertThatResultsAreEqualIgnoringOrder(receivedResults, expectedResults);
    }
    
    private void assertThatResultsAreEqualIgnoringOrder(Collection<Integer> receivedResults,
                                                        Collection<Integer> expectedResults) {
        Set<Integer> receivedResultsAsSet = new HashSet<>(receivedResults);
        Set<Integer> expectedResultsAsSet = new HashSet<>(expectedResults);
        assertThat(receivedResultsAsSet, is(expectedResultsAsSet));
    }

    @Test
    public void testProvidedAdditionalData() throws InterruptedException {
        retrievalProcessor.processElement(dataSource);
        retrievalProcessor.finish();
        ConcurrencyTestsUtil.sleepFor(500); //Giving the processor time to finish
        
        int expectedRetrievedDataAmount = receivedResults.size();
        
        OverwritingResultDataBuilder resultDataBuilder = new OverwritingResultDataBuilder();
        retrievalProcessor.getAdditionalResultData(resultDataBuilder);
        assertThat(resultDataBuilder.getRetrievedDataAmount(), is(expectedRetrievedDataAmount));
        assertThat(resultDataBuilder.getDataTypeMessageKey(), is(retrievedDataTypeMessageKey));
    }
    
    @Test
    public void testProvidedAdditionalDataWithMultipleRetrievalLayers() throws InterruptedException {
        List<Iterable<Integer>> layeredDataSource = new ArrayList<>();
        layeredDataSource.add(dataSource);
        layeredDataSource.add(dataSource);
        layeredDataSource.add(dataSource);

        @SuppressWarnings("unchecked")
        Class<Iterable<Iterable<Integer>>> inputType = (Class<Iterable<Iterable<Integer>>>)(Class<?>) Iterable.class;
        @SuppressWarnings("unchecked")
        Class<Iterable<Integer>> retrievedType = (Class<Iterable<Integer>>)(Class<?>) Iterable.class;
        Collection<Processor<Iterable<Integer>, ?>> resultReceivers = new ArrayList<>();
        resultReceivers.add(retrievalProcessor);
        String layeredRetrievedDataTypeMessageKey = "NumberList";
        Processor<Iterable<Iterable<Integer>>, Iterable<Integer>> layeredRetrievalProcessor = new AbstractRetrievalProcessor<Iterable<Iterable<Integer>>, Iterable<Integer>>(
                inputType, retrievedType, ConcurrencyTestsUtil.getSharedExecutor(), resultReceivers, 0,
                layeredRetrievedDataTypeMessageKey) {
            @Override
            protected Iterable<Iterable<Integer>> retrieveData(Iterable<Iterable<Integer>> element) {
                return element;
            }
        };
        
        layeredRetrievalProcessor.processElement(layeredDataSource);
        layeredRetrievalProcessor.finish();
        ConcurrencyTestsUtil.sleepFor(500); //Giving the processor time to finish
        
        int expectedRetrievedDataAmount = receivedResults.size();
        
        OverwritingResultDataBuilder resultDataBuilder = new OverwritingResultDataBuilder();
        layeredRetrievalProcessor.getAdditionalResultData(resultDataBuilder);
        assertThat(resultDataBuilder.getRetrievedDataAmount(), is(expectedRetrievedDataAmount));
        assertThat(resultDataBuilder.getDataTypeMessageKey(), is(retrievedDataTypeMessageKey));
    }
    
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUpResultReceiverAndProcessors() {
        Processor<Integer, Void> resultReceiver = new NullProcessor<Integer, Void>(Integer.class, Void.class) {
            @Override
            public void processElement(Integer element) {
                synchronized (receivedResults) {
                    receivedResults.add(element);
                }
            }
        };
        Collection<Processor<Integer, ?>> filtrationResultReceivers = new ArrayList<>();
        filtrationResultReceivers.add(resultReceiver);
        
        FilterCriterion<Integer> elementGreaterZeroFilterCriteria = new AbstractFilterCriterion<Integer>(Integer.class) {
            @Override
            public boolean matches(Integer element) {
                return element >= 0;
            }
        };
        Processor<Integer, Integer> filtrationProcessor = new ParallelFilteringProcessor<>(Integer.class, ConcurrencyTestsUtil.getSharedExecutor(), filtrationResultReceivers, elementGreaterZeroFilterCriteria);
        
        Class<Iterable<Integer>> inputType = (Class<Iterable<Integer>>) (Class<?>) Iterable.class;
        Collection<Processor<Integer, ?>> retrievalResultReceivers = new ArrayList<>();
        retrievalResultReceivers.add(filtrationProcessor);
        retrievedDataTypeMessageKey = "Number";
        retrievalProcessor = new AbstractRetrievalProcessor<Iterable<Integer>, Integer>(inputType, Integer.class,
                ConcurrencyTestsUtil.getSharedExecutor(), retrievalResultReceivers, 1, retrievedDataTypeMessageKey) {
            @Override
            protected Iterable<Integer> retrieveData(Iterable<Integer> element) {
                return element;
            }
        };
    }

}
