package com.sap.sse.datamining.impl.components;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.jupiter.api.Test;
import com.sap.sailing.polars.mining.AbstractEnrichingProcessor;
import com.sap.sse.datamining.components.AdditionalResultDataBuilder;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.components.ProcessorInstruction;
import com.sap.sse.datamining.components.ProcessorInstructionHandler;
import com.sap.sse.datamining.test.util.components.NullProcessor;
import com.sap.sse.util.ThreadPoolUtil;

public class TestWaitingForInstructionsToFinish {
    @Test
    public void testSimpleProcessorRunsImmediately() {
        final Processor<String, Integer> p = new NullProcessor<String, Integer>(String.class, Integer.class);
        final boolean[] waitSucceeded = new boolean[1];
        p.processElement("Humba");
        p.runWhenFinishedProcessing(()->waitSucceeded[0] = true);
        assertTrue(waitSucceeded[0]);
    }
    
    /**
     * Implements a test with the first parallel processor taking a while in the executor to compute its result
     * (e.g., blocked on a barrier/latch), but we register the callback on it immediately after calling
     * processElement(...). The result receivers haven't yet received the result. This test verifies that the callback
     * isn't fired before the result receivers have also processed the result. The test shall fail for
     * 4cac492293155f2afd9b0650c7a30ed0914595ba on branch bug6241 because there we first wait for the result receivers
     * to have a zero count of unfinished instructions, which they have immediately when they haven't been passed any
     * results yet, and then immediately register the callback on the first (entry-point) processor upon reaching zero
     * unfinished instructions, which is the case as soon as its result has been forwarded to the result receivers.<p>
     * 
     * To solve this, the waiting would probably need to be reversed. We know that the entry-point processor will
     * decrement its unfinished instructions count only *after* invoking processElement(...) on all result receivers,
     * which will then already in turn increment theirs.
     */
    @Test
    public void testEarlyCallbackWhenResultReceiversHaventStartedYet() throws InterruptedException, BrokenBarrierException {
        // Set-up:
        final Collection<Processor<Integer, ?>> resultReceivers = new ArrayList<>();
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final CyclicBarrier afterForwardingToResultReceivers = new CyclicBarrier(2);
        final CyclicBarrier afterFinishingEntryPoint = new CyclicBarrier(2);
        final CyclicBarrier resultReceiverBarrier = new CyclicBarrier(2);
        // construct some parallel processor as result receiver, so it processes results it received in executor tasks:
        resultReceivers.add(new AbstractEnrichingProcessor<Integer, Integer>(Integer.class, Integer.class,
                ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor(), Collections.emptySet()) {
            @Override
            protected Integer enrich(Integer element) {
                try {
                    resultReceiverBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
                return element * 2;
            }
        });
        final ProcessorInstructionHandler<?>[] handler = new ProcessorInstructionHandler<?>[1];
        final AbstractParallelProcessor<String, Integer> p = new AbstractParallelProcessor<String, Integer>(
                String.class, Integer.class, ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor(),
                resultReceivers) {
            @Override
            protected ProcessorInstruction<Integer> createInstruction(String element) {
                @SuppressWarnings("unchecked")
                final ProcessorInstructionHandler<Integer> h = (ProcessorInstructionHandler<Integer>) handler[0];
                return new AbstractProcessorInstruction<Integer>(h) {
                    @Override
                    protected Integer computeResult() throws Exception {
                        barrier.await(); // hold back computation in entry-point processor until released by test
                        return element.length();
                    }
                };
            }

            @Override
            public void instructionSucceeded(Integer result) {
                super.instructionSucceeded(result);
                try {
                    afterForwardingToResultReceivers.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public synchronized void afterInstructionFinished(ProcessorInstruction<Integer> instruction) {
                super.afterInstructionFinished(instruction);
                try {
                    afterFinishingEntryPoint.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
            }
        };
        handler[0] = p;
        // running the test now:
        final boolean[] waitSucceeded = new boolean[1];
        p.processElement("Humba"); // schedules a background executor task for the instruction that blocks on the barrier
        // now register the callback while the result receiver hasn't received anything yet:
        p.runWhenFinishedProcessing(()->{
            synchronized (waitSucceeded) {
                waitSucceeded[0] = true;
                waitSucceeded.notifyAll();
            }
        });
        barrier.await(); // this releases the entry-point task's computeResult, so no forwarding to result receivers has taken place so far
        afterForwardingToResultReceivers.await(); // after this we know the result receivers have received the result
        afterFinishingEntryPoint.await(); // after this we know the entry-point callback would have fired if no pending instructions anymore
        assertFalse(waitSucceeded[0]); // because we haven't yet unblocked the barrier for the result receiver
        resultReceiverBarrier.await(); // now the result receiver can continue
        synchronized (waitSucceeded) {
            while (!waitSucceeded[0]) {
                waitSucceeded.wait();
            }
        }
        assertTrue(waitSucceeded[0]);
    }
    
    @Test
    public void testSimpleParallelProcessorWithBlockingResultReceiver() throws InterruptedException, BrokenBarrierException {
        final Collection<Processor<Integer, ?>> resultReceivers = new ArrayList<>();
        final CountDownLatch barrier = new CountDownLatch(2);
        resultReceivers.add(createProcessorUnblockingCyclicBarrier(barrier));
        final AbstractParallelProcessor<String, Integer> p = createAbstractParallelProcessor(resultReceivers);
        final boolean[] waitSucceeded = new boolean[1];
        p.processElement("Humba");
        p.runWhenFinishedProcessing(()->new Thread(()->{
            try {
                barrier.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (waitSucceeded) {
                waitSucceeded[0] = true;
                waitSucceeded.notifyAll();
            }
        }).start());
        assertFalse(waitSucceeded[0]); // because we haven't yet unblocked the barrier
        barrier.countDown();
        synchronized (waitSucceeded) {
            while (!waitSucceeded[0]) {
                waitSucceeded.wait();
            }
        }
        assertTrue(waitSucceeded[0]);
    }

    @Test
    public void testSimpleParallelProcessorWithManyBlockingResultReceivers() throws InterruptedException, BrokenBarrierException {
        final int NUMBER_OF_RECEIVERS = 10000;
        final int NUMBER_OF_ELEMENTS_TO_PROCESS = 1000;
        final Collection<Processor<Integer, ?>> resultReceivers = new ArrayList<>();
        final CountDownLatch barrier = new CountDownLatch(NUMBER_OF_RECEIVERS*NUMBER_OF_ELEMENTS_TO_PROCESS+1);
        for (int i=0; i<NUMBER_OF_RECEIVERS; i++) {
            resultReceivers.add(createProcessorUnblockingCyclicBarrier(barrier));
        }
        final AbstractParallelProcessor<String, Integer> p = createAbstractParallelProcessor(resultReceivers);
        final boolean[] waitSucceeded = new boolean[1];
        for (int i=0; i<NUMBER_OF_ELEMENTS_TO_PROCESS; i++) {
            p.processElement("Humba");
        }
        p.runWhenFinishedProcessing(()->new Thread(()->{
            try {
                barrier.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (waitSucceeded) {
                waitSucceeded[0] = true;
                waitSucceeded.notifyAll();
            }
        }).start());
        assertFalse(waitSucceeded[0]); // because we haven't yet unblocked the barrier
        barrier.countDown();
        synchronized (waitSucceeded) {
            while (!waitSucceeded[0]) {
                waitSucceeded.wait();
            }
        }
        assertTrue(waitSucceeded[0]);
    }

    private AbstractParallelProcessor<String, Integer> createAbstractParallelProcessor(
            final Collection<Processor<Integer, ?>> resultReceivers) {
        final ProcessorInstructionHandler<?>[] handler = new ProcessorInstructionHandler<?>[1];
        final AbstractParallelProcessor<String, Integer> result = new AbstractParallelProcessor<String, Integer>(
                String.class, Integer.class, ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor(),
                resultReceivers) {
            @Override
            protected ProcessorInstruction<Integer> createInstruction(String element) {
                @SuppressWarnings("unchecked")
                final ProcessorInstructionHandler<Integer> h = (ProcessorInstructionHandler<Integer>) handler[0];
                return new AbstractProcessorInstruction<Integer>(h) {
                    @Override
                    protected Integer computeResult() throws Exception {
                        return element.length();
                    }
                };
            }

            @Override
            protected void setAdditionalData(AdditionalResultDataBuilder additionalDataBuilder) {
            }
        };
        handler[0] = result;
        return result;
    }
    
    private AbstractProcessor<Integer, Integer> createProcessorUnblockingCyclicBarrier(final CountDownLatch barrier) {
        return new AbstractProcessor<Integer, Integer>(Integer.class, Integer.class) {
            @Override
            public boolean canProcessElements() {
                return true;
            }

            @Override
            public void processElement(Integer element) {
                barrier.countDown();
            }

            @Override
            public void onFailure(Throwable failure) {
            }

            @Override
            public void finish() throws InterruptedException {
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public void abort() {
            }

            @Override
            public boolean isAborted() {
                return false;
            }

            @Override
            public AdditionalResultDataBuilder getAdditionalResultData(AdditionalResultDataBuilder additionalDataBuilder) {
                return additionalDataBuilder;
            }
        };
    }
}
