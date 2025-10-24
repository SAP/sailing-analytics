package com.sap.sse.datamining.impl.components;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;

public class TestAbstractParallelProcessorWithManySimpleInstructions {
    
    private Processor<Integer, Integer> processor;
    
    private int sum = 0;
    private boolean receiverWasToldToFinish = false;
    
    @BeforeEach
    public void initializeProcessor() {
        Processor<Integer, Void> receiver = new NullProcessor<Integer, Void>(Integer.class, Void.class) {
            @Override
            public void processElement(Integer element) {
                incrementSum(element);
            }
            @Override
            public void finish() throws InterruptedException {
                receiverWasToldToFinish = true;
            }
        };
        
        Collection<Processor<Integer, ?>> receivers = new ArrayList<>();
        receivers.add(receiver);
        processor = new AbstractParallelProcessor<Integer, Integer>(Integer.class, Integer.class, ConcurrencyTestsUtil.getSharedExecutor(), receivers) {
            @Override
            protected ProcessorInstruction<Integer> createInstruction(final Integer element) {
                return new AbstractProcessorInstruction<Integer>(this) {
                    @Override
                    public Integer computeResult() {
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
            processor.processElement(value);
            expectedSum += value;
        }
        return expectedSum;
    }

}
