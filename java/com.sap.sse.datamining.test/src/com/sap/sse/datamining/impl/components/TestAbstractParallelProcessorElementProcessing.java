package com.sap.sse.datamining.impl.components;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.components.StatefulBlockingInstruction;

public class TestAbstractParallelProcessorElementProcessing {
    
    private ThreadPoolExecutor executor;
    private Processor<Pair<Long, Integer>, Object> processor;
    private List<StatefulBlockingInstruction<?>> createdInstructions;
    
    @Before
    public void initialize() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(corePoolSize, corePoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        createdInstructions = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        Class<Pair<Long, Integer>> inputType = (Class<Pair<Long, Integer>>)(Class<?>) Pair.class;
        processor = new AbstractParallelProcessor<Pair<Long, Integer>, Object>(inputType, Object.class, executor, Collections.emptySet()) {
            @Override
            protected ProcessorInstruction<Object> createInstruction(Pair<Long, Integer> sleepTime) {
                StatefulBlockingInstruction<Object> instruction = new StatefulBlockingInstruction<>(this, sleepTime.getA(), sleepTime.getB());
                createdInstructions.add(instruction);
                return instruction;
            }
            @Override
            protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) { }
        };
    }

    @Test
    public void testSimpleProcessing() throws InterruptedException {
        long stepDuration = 10;
        int numberOfSteps = 1;
        Pair<Long, Integer> input = new Pair<Long, Integer>(stepDuration, numberOfSteps);
        int elementCount = executor.getMaximumPoolSize() * 2;
        for (int i = 0; i < elementCount; i++) {
            processor.processElement(input);
        }
        
        assertThat("Unexpected amount of created instructions", createdInstructions.size(), is(elementCount));
        double executionTime = Math.ceil((double) elementCount / executor.getMaximumPoolSize()) * stepDuration * numberOfSteps;
        executor.shutdown();
        assertTrue("Executor couldn't terminate", executor.awaitTermination((long) (executionTime * 1.2), TimeUnit.SECONDS));
        for (StatefulBlockingInstruction<?> instruction : createdInstructions) {
            assertTrue("run wasn't called", instruction.runWasCalled());
            assertTrue("computeResult wasn't called", instruction.computeResultWasCalled());
            assertTrue("computeResult didn't finish", instruction.computeResultWasFinished());
            assertFalse("computeResult was aborted", instruction.computeResultWasAborted());
        }
    }
    
    @Test
    public void testProcessingAfterFinish() throws InterruptedException {
        long stepDuration = 10;
        int numberOfSteps = 5;
        Pair<Long, Integer> input = new Pair<Long, Integer>(stepDuration, numberOfSteps);
        int elementCount = executor.getMaximumPoolSize() + 1; // Last instruction will be queued
        for (int i = 0; i < elementCount; i++) {
            processor.processElement(input);
        }
        assertThat("Unexpected amount of created instructions", createdInstructions.size(), is(elementCount));
        
        Thread.sleep(stepDuration); // Giving some time to ensure execution of unqueued instructions
        Thread finishingThread = ConcurrencyTestsUtil.tryToFinishTheProcessorInAnotherThread(processor);
        do {
            Thread.sleep(1);
        } while (!finishingThread.isAlive());
        assertFalse("Processor is already finished", processor.isFinished());
        // Processor not yet finished. New elements will be accepted
        processor.processElement(input);
        elementCount++;
        assertThat("Unexpected amount of created instructions", createdInstructions.size(), is(elementCount));
        
        finishingThread.join();
        assertTrue("Processor isn't finished", processor.isFinished());
        processor.processElement(input);
        assertThat("Unexpected amount of created instructions", createdInstructions.size(), is(elementCount));
        
        for (StatefulBlockingInstruction<?> instruction : createdInstructions) {
            assertTrue("run wasn't called", instruction.runWasCalled());
            assertTrue("computeResult wasn't called", instruction.computeResultWasCalled());
            assertTrue("computeResult didn't finish", instruction.computeResultWasFinished());
            assertFalse("computeResult was aborted", instruction.computeResultWasAborted());
        }
    }
    
    @Test
    public void testProcessingAfterAbort() throws InterruptedException {
        long stepDuration = 10;
        int numberOfSteps = 5;
        Pair<Long, Integer> input = new Pair<Long, Integer>(stepDuration, numberOfSteps);
        int elementCount = executor.getMaximumPoolSize()   // Set of finished instructions
                           + executor.getMaximumPoolSize() // Set of started, but not yet finished instructions
                           + 1; // Scheduled instruction
        for (int i = 0; i < elementCount; i++) {
            processor.processElement(input);
        }
        assertThat("Unexpected amount of created instructions", createdInstructions.size(), is(elementCount));

        long instructionDuration = stepDuration * numberOfSteps;
        Thread.sleep(instructionDuration + stepDuration); // Time to finish first set and start second set
        processor.abort();
        processor.processElement(new Pair<>(0L, 0));
        assertThat("Unexpected amount of created instructions", createdInstructions.size(), is(elementCount));
        
        executor.shutdown();
        assertTrue("Executor couldn't terminate", executor.awaitTermination(2 * stepDuration, TimeUnit.MILLISECONDS));
        for (int i = 0; i < createdInstructions.size(); i++) {
            StatefulBlockingInstruction<?> instruction = createdInstructions.get(i);
            // run should be called for all instructions
            assertTrue("run wasn't called", instruction.runWasCalled());
            if (i < executor.getMaximumPoolSize()) {
                // First set of instructions should be processed normally
                assertTrue("computeResult wasn't called", instruction.computeResultWasCalled());
                assertTrue("computeResult didn't finish", instruction.computeResultWasFinished());
                assertFalse("computeResult was aborted", instruction.computeResultWasAborted());
            } else if (i < executor.getMaximumPoolSize() * 2) {
                // Second set of instructions was running when the processor was aborted. computeResult should be aborted
                assertTrue("computeResult wasn't called", instruction.computeResultWasCalled());
                assertTrue("computeResult didn't finish", instruction.computeResultWasFinished());
                assertTrue("computeResult wasn't aborted", instruction.computeResultWasAborted());
            } else {
                // Last instructions was still scheduled when the processor was aborted. computeResult should not be called
                assertFalse("computeResult of last instruction was called", instruction.computeResultWasCalled());
                assertFalse("computeResult of last instruction finished", instruction.computeResultWasFinished());
                assertFalse("computeResult was aborted", instruction.computeResultWasAborted());
            }
        }
    }

}
