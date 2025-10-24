package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import org.junit.jupiter.api.Test;

/**
 * Tests the concept of a {@link ForkJoinPool} by producing a deep hierarchy of {@link RecursiveTask}s
 * that are submitted to the pool for execution, and parent tasks wait for the children to complete.
 * With a regular thread pool and regular synchronous waiting for the completion of sub-tasks the
 * thread pool would get depleted, and the system would deadlock. The {@link ForkJoinPool}'s promise
 * is that the threads blocked by waiting for sub-tasks will steal work from the pool, leading to the
 * completion of those sub-tasks.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ForkJoinPoolTest {
    private static final int MAX_LEVEL = 20;
    
    private static class SomeRandomWork extends RecursiveTask<Long> {
        private static final long serialVersionUID = -8187414464974452503L;
        private final int level;
        
        public SomeRandomWork(int level) {
            this.level = level;
        }

        @Override
        protected Long compute() {
            final long result;
            if (level < MAX_LEVEL) {
                final SomeRandomWork leftChild = new SomeRandomWork(level+1);
                final SomeRandomWork rightChild = new SomeRandomWork(level+1);
                invokeAll(leftChild, rightChild);
                try {
                    result = leftChild.get() + rightChild.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                result = 1;
            }
            return result;
        }
    }
    
    @Test
    public void testDeepRecursiveWait() throws InterruptedException, ExecutionException {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        final Long result = pool.submit(new SomeRandomWork(0)).get();
        assertEquals(1l << MAX_LEVEL, result.longValue());
    }
}
