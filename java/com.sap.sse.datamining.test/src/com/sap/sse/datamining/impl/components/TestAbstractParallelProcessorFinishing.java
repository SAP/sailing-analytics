package com.sap.sse.datamining.impl.components;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.components.NullProcessor;

public class TestAbstractParallelProcessorFinishing {
    
    private Processor<Integer, Integer> processor;
    private Processor<Integer, ?> receiver;
    
    private boolean instructionIsWorking;
    private boolean receiverWasToldToFinish;
    
    @BeforeEach
    public void initializeProcessorAndReceiver() {
        instructionIsWorking = true;
        receiverWasToldToFinish = false;
        
        receiver = createReceiver();
        
        Collection<Processor<Integer, ?>> receivers = new HashSet<>();
        receivers.add(receiver);
        processor = createProcessor(receivers);
    }

    @Test
    public void testProcessFinishing() {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                processor.processElement(1);
                try {
                    processor.finish();
                } catch (InterruptedException e) {
                    fail("The test was interrupted: " + e.getMessage());
                }
            }
        });
        worker.start();
        ConcurrencyTestsUtil.sleepFor(100); //Wait till the processor tries to finish
        assertThat(receiverWasToldToFinish, is(false));
        instructionIsWorking = false; //The processer should be able to finish after the instruction is done
        ConcurrencyTestsUtil.sleepFor(500); //Giving the processor a chance to finish
        assertThat(receiverWasToldToFinish, is(true));
        
    }

    private Processor<Integer, ?> createReceiver() {
        return new NullProcessor<Integer, Void>(Integer.class, Void.class) {
            @Override
            public void finish() throws InterruptedException {
                receiverWasToldToFinish = true;
            }
        };
    }

    private AbstractParallelProcessor<Integer, Integer> createProcessor(Collection<Processor<Integer, ?>> receivers) {
        return new AbstractParallelProcessor<Integer, Integer>(Integer.class, Integer.class, ConcurrencyTestsUtil.getSharedExecutor(), receivers) {
            @Override
            protected ProcessorInstruction<Integer> createInstruction(Integer partialElement) {
                return new AbstractProcessorInstruction<Integer>(this) {
                    @Override
                    public Integer computeResult() {
                        while (instructionIsWorking) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                fail("Instruction was interrupted.");
                            }
                        }
                        return 0;
                    }
                };
            }
            @Override
            protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
            }
        };
    }

}
