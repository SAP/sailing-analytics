package com.sap.sse.datamining.impl.components;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import com.sap.sse.datamining.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;

public class TestAbstractParallelProcessorWithManySimpleInstructions {
    
    private Processor<Integer> processor;
    
    private int sum = 0;
    private boolean receiverWasToldToFinish = false;
    
    @Before
    public void initializeProcessor() {
        Processor<Integer> receiver = new Processor<Integer>() {
            @Override
            public void onElement(Integer element) {
                incrementSum(element);
            }
            @Override
            public void onFailure(Throwable failure) {
            }
            @Override
            public void finish() throws InterruptedException {
                receiverWasToldToFinish = true;
            }
            @Override
            public void abort() {
            }
            @Override
            public AdditionalResultDataBuilder getAdditionalResultData(AdditionalResultDataBuilder additionalDataBuilder) {
                return additionalDataBuilder;
            }
        };
        
        Collection<Processor<Integer>> receivers = new ArrayList<>();
        receivers.add(receiver);
        processor = new AbstractSimpleParallelProcessor<Integer, Integer>(ConcurrencyTestsUtil.getExecutor(), receivers) {
            @Override
            protected Callable<Integer> createInstruction(final Integer element) {
                return new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return element;
                    }
                };
            }
            @Override
            protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
            }
        };
    }
    
    private synchronized void incrementSum(int value) {
        sum += value;
    }

    @Test
    public void testOpenInstructionsHandling() throws InterruptedException {
        int instructionAmount = 1000;
        int valueExclusiveUpperBound = 100;
        int expectedSum = sendElements(instructionAmount, valueExclusiveUpperBound);
        ConcurrencyTestsUtil.sleepFor(200); //Giving the processor time to process the instructions
        assertThat(sum, is(expectedSum));
        
        ConcurrencyTestsUtil.tryToFinishTheProcessorInAnotherThread(processor);
        ConcurrencyTestsUtil.sleepFor(500); //Giving the processor time to finish
        assertThat("The processor didn't finish in the given time.", receiverWasToldToFinish , is(true));
    }

    private int sendElements(int elementAmount, int valueExclusiveUpperBound) {
        int expectedSum = 0;
        for (int instructionNumber = 0; instructionNumber < elementAmount ; instructionNumber++) {
            Integer value = (int) (Math.random() * valueExclusiveUpperBound);
            processor.onElement(value);
            expectedSum += value;
        }
        return expectedSum;
    }

}
