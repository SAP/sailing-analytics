package com.sap.sse.datamining.impl.components;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.criterias.AbstractFilterCriterion;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;

public class TestFilteringProcessors {
    
    private Collection<Processor<Integer, ?>> receivers;
    /**
     * Contains the last received elements as key and the amount of the received times as value;
     */
    private Map<Integer, Integer> receivedElements;

    @Test
    public void testFilteringProcessor() {
        FilterCriterion<Integer> elementIsEvenCriteria = new AbstractFilterCriterion<Integer>(Integer.class) {
            @Override
            public boolean matches(Integer element) {
                return element % 2 == 0;
            }
        };
        Processor<Integer, Integer> filteringProcessor = new ParallelFilteringProcessor<Integer>(Integer.class, ConcurrencyTestsUtil.getSharedExecutor(), receivers, elementIsEvenCriteria);
        ConcurrencyTestsUtil.processElements(filteringProcessor, createElementsToProcess());
        ConcurrencyTestsUtil.sleepFor(100); // Giving the processor time to process the instructions
        
        Collection<Integer> evenElements = new ArrayList<>();
        evenElements.add(2);
        evenElements.add(4);
        verifyThatExpectedElementsHasBeenReceived(evenElements);
        
        Collection<Integer> unevenElements = new ArrayList<>();
        unevenElements.add(1);
        unevenElements.add(3);
        verifyThatUnexpectedElementsHasNotBeenReceived(unevenElements);
    }

    private void verifyThatUnexpectedElementsHasNotBeenReceived(Collection<Integer> unexpectedElements) {
        for (Integer unexpectedElement : unexpectedElements) {
            assertThat("The unexpected element '" + unexpectedElement + "' was received", receivedElements.containsKey(unexpectedElement), is(false));
        }
    }

    private Collection<Integer> createElementsToProcess() {
        Collection<Integer> elements = new ArrayList<>();
        elements.add(1);
        elements.add(2);
        elements.add(3);
        elements.add(4);
        return elements;
    }
    
    private void verifyThatExpectedElementsHasBeenReceived(Iterable<Integer> expectedElements) {
        for (Integer expectedElement : expectedElements) {
            assertThat("The expected element '" + expectedElement + "' wasn't received", receivedElements.containsKey(expectedElement), is(true));
        }
    }
    
    @BeforeEach
    public void initializeReceivers() {
        receivedElements = new HashMap<>();
        
        Processor<Integer, Void> receiver = new NullProcessor<Integer, Void>(Integer.class, Void.class) {
            @Override
            public void processElement(Integer element) {
                synchronized (receivedElements) {
                    if (!receivedElements.containsKey(element)) {
                        receivedElements.put(element, 0);
                    }
                    Integer elementAmount = receivedElements.get(element) + 1;
                    receivedElements.put(element, elementAmount);
                }
            }
        };
        
        receivers = new HashSet<>();
        receivers.add(receiver);
    }

}
