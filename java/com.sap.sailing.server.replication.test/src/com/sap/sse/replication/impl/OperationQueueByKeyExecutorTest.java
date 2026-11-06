package com.sap.sse.replication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Regression for bug6332: an enqueue must not lose its worker when an empty queue is retired concurrently.
 */
public class OperationQueueByKeyExecutorTest {
    private static final long TIMEOUT_SECONDS = 5;

    @Test
    public void testSchedulingWhileQueueIsRetiredDoesNotLoseOperation() throws Exception {
        // The executor records queue-draining tasks for this test to run explicitly.
        final Queue<Runnable> workerTasks = new ConcurrentLinkedQueue<>();
        final OperationQueueByKeyExecutor operationQueueByKeyExecutor = new OperationQueueByKeyExecutor(workerTasks::add);
        final String key = "replica";
        final List<String> appliedOperations = new CopyOnWriteArrayList<>();
        // Queues BEFORE and submits its queue-draining worker to workerTasks.
        operationQueueByKeyExecutor.schedule(key, () -> appliedOperations.add("BEFORE"));
        final CountDownLatch retiring = new CountDownLatch(/* count */ 1);
        final CountDownLatch allowRetirement = new CountDownLatch(/* count */ 1);
        pauseEmptyQueueCheck(operationQueueByKeyExecutor, key, retiring, allowRetirement);
        final FutureTask<Void> workerTask = new FutureTask<>(workerTasks.remove(), /* result */ null);
        final FutureTask<Void> producerTask = new FutureTask<>(
                () -> operationQueueByKeyExecutor.schedule(key, () -> appliedOperations.add("CONFLICT")), /* result */ null);
        // Runs the previously removed queue-draining task, which applies BEFORE and reaches the intercepted isEmpty().
        final Thread worker = new Thread(workerTask, "queue-retirement-worker");
        final Thread producer = new Thread(producerTask, "queue-retirement-producer");
        try {
            worker.start();
            assertTrue(retiring.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Worker did not reach the empty-queue check after BEFORE");
            // now the isEmpty() call issued by workOnQueueForKey has to wait for the allowRetirement latch...
            producer.start();
            // The producer must wait for queue bookkeeping while the worker is deciding whether to retire its queue.
            awaitBlockedProducer(producer);
            allowRetirement.countDown(); // releases the isEmpty() call
            workerTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            producerTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            // Later submissions still execute, even when CONFLICT was stranded in the retired queue.
            operationQueueByKeyExecutor.schedule(key, () -> appliedOperations.add("CHECKED_FAILURE"));
            operationQueueByKeyExecutor.schedule(key, () -> appliedOperations.add("AFTER"));
            operationQueueByKeyExecutor.schedule(key, () -> appliedOperations.add("NEXT_MESSAGE"));
            Runnable nextWorker;
            while ((nextWorker = workerTasks.poll()) != null) {
                nextWorker.run();
            }
            assertEquals(Arrays.asList("BEFORE", "CONFLICT", "CHECKED_FAILURE", "AFTER", "NEXT_MESSAGE"),
                    appliedOperations, "An operation scheduled during queue retirement must not be discarded");
        } finally {
            allowRetirement.countDown();
            workerTask.cancel(/* mayInterruptIfRunning */ true);
            producerTask.cancel(/* mayInterruptIfRunning */ true);
            worker.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            producer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            assertFalse(worker.isAlive(), "Queue worker must terminate");
            assertFalse(producer.isAlive(), "Queue producer must terminate");
        }
    }

    @Test
    public void testRunningOperationKeepsItsQueueWhileOtherKeysProgress() throws Exception {
        final ExecutorService threadPool = Executors.newFixedThreadPool(/* nThreads */ 2);
        final Queue<Future<?>> workerTasks = new ConcurrentLinkedQueue<>();
        final OperationQueueByKeyExecutor operationQueueByKeyExecutor = new OperationQueueByKeyExecutor(
                (final Runnable task) -> workerTasks.add(threadPool.submit(task)));
        final CountDownLatch firstStarted = new CountDownLatch(/* count */ 1);
        final CountDownLatch finishFirst = new CountDownLatch(/* count */ 1);
        final CountDownLatch otherKeyCompleted = new CountDownLatch(/* count */ 1);
        final List<String> appliedOperations = new CopyOnWriteArrayList<>();
        final String key = "same-key";
        try {
            operationQueueByKeyExecutor.schedule(key, () -> {
                appliedOperations.add("FIRST_STARTED");
                firstStarted.countDown();
                try {
                    assertTrue(finishFirst.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                            "Timed out waiting to finish the first operation");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                appliedOperations.add("FIRST_COMPLETED");
            });
            assertTrue(firstStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "First operation did not start");
            // The first operation has been dequeued but is still running; its successor must use the same worker.
            operationQueueByKeyExecutor.schedule(key, () -> appliedOperations.add("SECOND"));
            operationQueueByKeyExecutor.schedule("other-key", otherKeyCompleted::countDown);
            assertTrue(otherKeyCompleted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Another key must progress while the first operation is blocked");
            assertEquals(Arrays.asList("FIRST_STARTED"), appliedOperations,
                    "Operations with the same key must not execute concurrently");
            finishFirst.countDown();
            for (final Future<?> workerTask : workerTasks) {
                workerTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            assertEquals(Arrays.asList("FIRST_STARTED", "FIRST_COMPLETED", "SECOND"), appliedOperations);
        } finally {
            finishFirst.countDown();
            threadPool.shutdownNow();
            assertTrue(threadPool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Queue workers must terminate");
        }
    }

    @Test
    public void testInlineExecutorQueuesBeforeStartingWorker() {
        final OperationQueueByKeyExecutor operationQueueByKeyExecutor = new OperationQueueByKeyExecutor(Runnable::run);
        final List<String> appliedOperations = new CopyOnWriteArrayList<>();
        final String key = "replica";
        operationQueueByKeyExecutor.schedule(key, () -> {
            assertFalse(Thread.holdsLock(operationQueueByKeyExecutor),
                    "An inline worker must execute operations outside the bookkeeping monitor");
            appliedOperations.add("FIRST_STARTED");
            // A reentrant submission must wait for the current operation to finish, even with an inline executor.
            operationQueueByKeyExecutor.schedule(key, () -> appliedOperations.add("SECOND"));
            appliedOperations.add("FIRST_COMPLETED");
        });
        assertEquals(Arrays.asList("FIRST_STARTED", "FIRST_COMPLETED", "SECOND"), appliedOperations);
    }

    private void awaitBlockedProducer(final Thread producer) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (producer.getState() != Thread.State.BLOCKED && producer.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(/* millis */ 1);
        }
        assertEquals(Thread.State.BLOCKED, producer.getState(),
                "Producer must block on queue bookkeeping before retirement is allowed");
    }

    /**
     * Installs a delegating proxy for the existing queue before its worker starts. When the worker calls
     * {@link Deque#isEmpty()} while holding the bookkeeping monitor, the proxy signals {@code retiring} and waits for
     * {@code allowRetirement} before delegating the check to the original queue. This lets the test wait until a
     * producer is blocked on queue bookkeeping, then release the worker to retire the queue before the producer can
     * enqueue its operation. With atomic enqueueing and retirement, the producer then creates a new queue and worker;
     * the old two-monitor implementation instead appended to the retired queue. All other queue calls delegate
     * immediately.
     *
     * @param executor the executor whose queue to wrap
     * @param key the key of the already-created queue
     * @param retiring signals that the worker has reached the empty-queue check
     * @param allowRetirement releases the worker to perform the check and continue
     */
    private void pauseEmptyQueueCheck(final OperationQueueByKeyExecutor executor, final Object key,
            final CountDownLatch retiring, final CountDownLatch allowRetirement) {
        final Map<Object, Deque<Runnable>> queues = executor.getOperationQueuesByKey();
        final Deque<Runnable> originalQueue;
        synchronized (executor) {
            originalQueue = queues.get(key);
        }
        // Replace the queue before starting its worker. All operations delegate unchanged; only timing is controlled.
        @SuppressWarnings("unchecked")
        final Deque<Runnable> controlledQueue = (Deque<Runnable>) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] { Deque.class },
                (final Object proxy, final Method method, final Object[] arguments) -> {
                    if (method.getName().equals("isEmpty")) {
                        retiring.countDown();
                        assertTrue(allowRetirement.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                                "Timed out waiting to allow queue retirement");
                    }
                    return method.invoke(originalQueue, arguments);
                });
        synchronized (executor) {
            queues.put(key, controlledQueue);
        }
    }
}
