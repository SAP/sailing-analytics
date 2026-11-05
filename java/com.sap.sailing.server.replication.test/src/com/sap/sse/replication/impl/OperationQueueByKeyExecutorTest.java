package com.sap.sse.replication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
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
        final Queue<Runnable> workerTasks = new ConcurrentLinkedQueue<>();
        final OperationQueueByKeyExecutor executor = new OperationQueueByKeyExecutor(workerTasks::add);
        final String key = "replica";
        final List<String> appliedOperations = new CopyOnWriteArrayList<>();
        executor.schedule(key, () -> appliedOperations.add("BEFORE"));
        final CountDownLatch retiring = new CountDownLatch(/* count */ 1);
        final CountDownLatch allowRetirement = new CountDownLatch(/* count */ 1);
        pauseEmptyQueueCheck(executor, key, retiring, allowRetirement);
        final FutureTask<Void> workerTask = new FutureTask<>(workerTasks.remove(), /* result */ null);
        final FutureTask<Void> producerTask = new FutureTask<>(
                () -> executor.schedule(key, () -> appliedOperations.add("CONFLICT")), /* result */ null);
        final Thread worker = new Thread(workerTask, "queue-retirement-worker");
        final Thread producer = new Thread(producerTask, "queue-retirement-producer");
        try {
            worker.start();
            assertTrue(retiring.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Worker did not reach the empty-queue check after BEFORE");
            producer.start();
            // The worker holds the per-key monitor. The producer captures the old queue and blocks on that monitor.
            awaitBlockedProducer(producer);
            allowRetirement.countDown();
            workerTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            producerTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            // Later submissions still execute, even when CONFLICT was stranded in the retired queue.
            executor.schedule(key, () -> appliedOperations.add("CHECKED_FAILURE"));
            executor.schedule(key, () -> appliedOperations.add("AFTER"));
            executor.schedule(key, () -> appliedOperations.add("NEXT_MESSAGE"));
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

    private void awaitBlockedProducer(final Thread producer) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (producer.getState() != Thread.State.BLOCKED && producer.isAlive() && System.nanoTime() < deadline) {
            Thread.sleep(/* millis */ 1);
        }
        assertEquals(Thread.State.BLOCKED, producer.getState(),
                "Producer must block on queue bookkeeping before retirement is allowed");
    }

    @SuppressWarnings("unchecked")
    private void pauseEmptyQueueCheck(final OperationQueueByKeyExecutor executor, final Object key,
            final CountDownLatch retiring, final CountDownLatch allowRetirement) throws ReflectiveOperationException {
        final Field queuesField = OperationQueueByKeyExecutor.class.getDeclaredField("operationQueuesByKey");
        queuesField.setAccessible(/* flag */ true);
        final Map<Object, Deque<Runnable>> queues = (Map<Object, Deque<Runnable>>) queuesField.get(executor);
        final Deque<Runnable> originalQueue = queues.get(key);
        // Replace the queue before starting its worker. All operations delegate unchanged; only timing is controlled.
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
        queues.put(key, controlledQueue);
    }
}
