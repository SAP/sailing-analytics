package com.sap.sse.replication.impl;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import com.sap.sse.common.TimePoint;
import com.sap.sse.operationaltransformation.Operation;

/**
 * Collaborates with {@link ReplicationReceiverImpl} to allow parallel execution of operations, but only for those
 * that have different {@link Operation#getKeyForAsynchronousExecution() keys for asynchronous execution}. The receiver
 * {@link #schedule(Object, Runnable) schedules} operations (wrapped as {@link Runnable}s) with an object of this type
 * which puts it into a {@link Deque double-ended queue} specific to the key for asynchronous execution as provided by
 * the operation. If it is the first operation in a new {@link Deque} then a task is scheduled for immediate execution
 * which continues to poll the {@link Deque} and apply its operations until drained. This way, if the replication
 * continues to receive operations with the same key while the first operation with that key is being applied,
 * the next operations with an equal key will be enqueued in the same queue and will be picked up by the already running
 * task that is specific to the key. When the queue is drained, the task terminates and the queue is removed from
 * {@link #operationQueuesByKey}.<p>
 * 
 * With this approach, only one task per equal key is scheduled with the {@link Executor} passed to the constructor,
 * ensuring that operations with equal keys don't block each other during parallel execution.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class OperationQueueByKeyExecutor {
    private static final Logger logger = Logger.getLogger(OperationQueueByKeyExecutor.class.getName());

    private final Executor executor;
    
    /**
     * Holds double-ended queues for each key for which a task has been {@link #schedule(Object, Runnable) scheduled}
     * with the {@link #executor} and which hasn't completed yet.
     * <p>
     * 
     * All access to this map and its queues must happen while {@code synchronized} on this
     * {@link OperationQueueByKeyExecutor} object. In particular, looking up a queue and enqueueing an operation must
     * be atomic with respect to checking whether the queue is empty and removing it. A queue stays registered while
     * its worker executes an operation, even if no further operations are queued. Operations execute outside this
     * monitor so different keys can make progress concurrently.
     */
    private final ConcurrentMap<Object, Deque<Runnable>> operationQueuesByKey;
    
    OperationQueueByKeyExecutor(final Executor executor) {
        super();
        this.executor = executor;
        this.operationQueuesByKey = new ConcurrentHashMap<>();
    }

    /**
     * Provides package-local access for queue-retirement tests in the test fragment. Callers must follow the
     * synchronization requirements of {@link #operationQueuesByKey}.
     */
    ConcurrentMap<Object, Deque<Runnable>> getOperationQueuesByKey() {
        return operationQueuesByKey;
    }

    public void schedule(final Object keyForAsynchronousExecution, final Runnable runnable) {
        final Deque<Runnable> queueForKey;
        final boolean queueCreated;
        synchronized (this) {
            if (operationQueuesByKey.containsKey(keyForAsynchronousExecution)) {
                queueForKey = operationQueuesByKey.get(keyForAsynchronousExecution);
                queueCreated = false;
            } else {
                logger.fine(()->"Creating operation queue for key "+keyForAsynchronousExecution);
                queueForKey = new ConcurrentLinkedDeque<>();
                queueCreated = true;
                operationQueuesByKey.put(keyForAsynchronousExecution, queueForKey);
            }
            queueForKey.add(runnable);
        }
        if (queueCreated) {
            // Publish the first operation before starting the worker, which may run inline with some executors.
            executor.execute(()->workOnQueueForKey(keyForAsynchronousExecution));
        }
    }

    /**
     * Removes operations from the queue for the {@code keyForAsynchronousExecution} which can be expected to not be
     * empty when this method is called. Dequeueing and empty-queue retirement use the same monitor as
     * {@link #schedule(Object, Runnable) enqueueing}, so an enqueue cannot target a retired queue. Each operation is
     * executed outside that monitor while its queue remains registered for subsequent submissions with the same key.
     */
    private void workOnQueueForKey(final Object keyForAsynchronousExecution) {
        final Deque<Runnable> queueForKey;
        final int[] count = { 1 };
        final TimePoint[] started = new TimePoint[1];
        logger.fine(()->{
            started[0] = TimePoint.now();
            return "Started work on queue for key "+keyForAsynchronousExecution;
        });
        Runnable nextOperation;
        synchronized (this) {
            queueForKey = operationQueuesByKey.get(keyForAsynchronousExecution);
            nextOperation = queueForKey.pollFirst();
        }
        do {
            nextOperation.run();
            synchronized (this) {
                if (queueForKey.isEmpty()) {
                    nextOperation = null;
                    operationQueuesByKey.remove(keyForAsynchronousExecution);
                } else {
                    nextOperation = queueForKey.pollFirst();
                    count[0]++;
                }
            }
        } while (nextOperation != null);
        logger.fine(()->"Terminating tasks for operation queue for key "+keyForAsynchronousExecution+" after running "+count[0]+" operations "+
                "which took "+started[0].until(TimePoint.now()));
    }
}
