package com.sap.sse.datamining.impl.components;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;
import com.sap.sse.datamining.test.util.components.Number;

public class TestParallelExtractionProcessor {
    
    private Collection<Processor<GroupedDataEntry<Integer>, ?>> receivers;
    private Map<GroupKey, Integer> receivedValues;
    
    private Function<Integer> getCrossSumFunction;
    
    /**
     * A function that is invalid with the type {@link Number} and will return <code>null</code> with every invocation.
     */
    private Function<Integer> invalidFunction;
    
    @BeforeEach
    public void initializeReceivers() {
        receivedValues = new HashMap<>();
        
        @SuppressWarnings("unchecked")
        Processor<GroupedDataEntry<Integer>, Void> receiver = new NullProcessor<GroupedDataEntry<Integer>, Void>((Class<GroupedDataEntry<Integer>>)(Class<?>) GroupedDataEntry.class, Void.class) {
            @Override
            public void processElement(GroupedDataEntry<Integer> element) {
                synchronized (receivedValues) {
                    receivedValues.put(element.getKey(), element.getDataEntry());
                }
            }
        };
        
        receivers = new HashSet<>();
        receivers.add(receiver);
    }
    
    @BeforeEach
    public void initializeExtractionFunctions() {
        Method getCrossSumMethod = FunctionTestsUtil.getMethodFromClass(Number.class, "getCrossSum");
        getCrossSumFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(getCrossSumMethod);
        
        Method invalidMethod = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("sideEffectFreeValue");
        invalidFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(invalidMethod);
    }

    @Test
    public void testValueExtraction() {
        Processor<GroupedDataEntry<Number>, GroupedDataEntry<Integer>> processor = new ParallelGroupedElementsValueExtractionProcessor<Number, Integer>(ConcurrencyTestsUtil.getSharedExecutor(), receivers, getCrossSumFunction);
        Collection<GroupedDataEntry<Number>> elements = createElements();
        ConcurrencyTestsUtil.processElements(processor, elements);
        ConcurrencyTestsUtil.sleepFor(100); //Giving the processor time to finish the instructions
        
        Collection<GroupedDataEntry<Integer>> expectedReceivedValues = buildExpectedReceivedValues(elements);
        verifyThatExpectedElementsHaveBeenReceived(expectedReceivedValues);
    }

    private void verifyThatExpectedElementsHaveBeenReceived(Collection<GroupedDataEntry<Integer>> expectedReceivedValues) {
        for (GroupedDataEntry<Integer> expectedReceivedValue : expectedReceivedValues) {
            assertThat("The expected value '" + expectedReceivedValue + "' wasn't received.", receivedValues.containsKey(expectedReceivedValue.getKey()), is(true));
            assertThat(receivedValues.get(expectedReceivedValue.getKey()), is(expectedReceivedValue.getDataEntry()));
        }
    }
    
    @Test
    public void testValueExtractionWithInvalidFunction() {
        Processor<GroupedDataEntry<Number>, GroupedDataEntry<Integer>> processor = new ParallelGroupedElementsValueExtractionProcessor<Number, Integer>(ConcurrencyTestsUtil.getSharedExecutor(), receivers, invalidFunction);
        ConcurrencyTestsUtil.processElements(processor, createElements());
        ConcurrencyTestsUtil.sleepFor(100); //Giving the processor time to finish the instructions
        assertThat("Values have been received, but the processor function is invalid.", receivedValues.isEmpty(), is(true));
    }

    private Collection<GroupedDataEntry<Number>> createElements() {
        Collection<GroupedDataEntry<Number>> elements = new ArrayList<>();
        elements.add(new GroupedDataEntry<Number>(new GenericGroupKey<Integer>(1), new Number(1)));
        elements.add(new GroupedDataEntry<Number>(new GenericGroupKey<Integer>(2), new Number(11)));
        elements.add(new GroupedDataEntry<Number>(new GenericGroupKey<Integer>(3), new Number(111)));
        elements.add(new GroupedDataEntry<Number>(new GenericGroupKey<Integer>(4), new Number(1111)));
        return elements;
    }

    private Collection<GroupedDataEntry<Integer>> buildExpectedReceivedValues(Collection<GroupedDataEntry<Number>> elements) {
        Collection<GroupedDataEntry<Integer>> expectedReceivedValues = new ArrayList<>();
        for (GroupedDataEntry<Number> element : elements) {
            Integer value = getCrossSumFunction.tryToInvoke(element.getDataEntry());
            assertThat("The value couldn't be calculated from the function.", value != null, is(true));
            expectedReceivedValues.add(new GroupedDataEntry<Integer>(element.getKey(), value));
        }
        return expectedReceivedValues;
    }

}
