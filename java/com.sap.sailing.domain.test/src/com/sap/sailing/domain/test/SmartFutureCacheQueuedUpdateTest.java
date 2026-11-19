package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sse.util.SmartFutureCache.UpdateInterval;

/** Exercises queued updates through public calls, with the first calculation held until readers are waiting. */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class SmartFutureCacheQueuedUpdateTest {
    private static final String KEY = "competitor";
    private static final long TIMEOUT_SECONDS = 5;
    private static final Integer LATEST_VALUE = 2;

    private static final class Range implements UpdateInterval<Range> {
        private final int from;
        private final int to;

        private Range(final int from, final int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Range join(final Range other) {
            return new Range(Math.min(from, other.from), Math.max(to, other.to));
        }
    }

    private static final class Reader {
        private final FutureTask<Integer> result;
        private final Thread thread;

        private Reader(final SmartFutureCache<String, Integer, Range> cache) {
            result = new FutureTask<>(() -> cache.get(KEY, /* waitForLatest */ true));
            thread = new Thread(result, "SmartFutureCache queued-update reader");
            thread.setDaemon(true);
            thread.start();
        }

        private void awaitQueuedResult() {
            // Observe the blocked public call, without modifying the cache or assuming a reader registration delay.
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
            boolean waiting = false;
            do {
                for (final StackTraceElement frame : thread.getStackTrace()) {
                    waiting |= frame.getClassName().equals(SmartFutureCache.class.getName() + "$SettableFuture")
                            && frame.getMethodName().equals("get");
                }
                if (!waiting) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(/* duration */ 1));
                }
            } while (!waiting && !result.isDone() && System.nanoTime() < deadline);
            assertTrue(waiting, "Reader must reach the queued future while the first calculation is held");
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final CountDownLatch firstStarted = new CountDownLatch(/* count */ 1);
        private final CountDownLatch releaseFirst = new CountDownLatch(/* count */ 1);
        private final CountDownLatch secondStarted = new CountDownLatch(/* count */ 1);
        private final AtomicInteger computations = new AtomicInteger();
        private final List<Reader> readers = new ArrayList<>();
        private final IllegalStateException queuedFailure = new IllegalStateException("queued computation failed");
        private final SmartFutureCache<String, Integer, Range> cache;
        private volatile Range secondInterval;

        private Fixture(final boolean failQueuedUpdate) {
            cache = new SmartFutureCache<>(new AbstractCacheUpdater<String, Integer, Range>() {
                @Override
                public Integer computeCacheUpdate(final String key, final Range interval) throws InterruptedException {
                    final int result;
                    if (key.equals(KEY)) {
                        result = computations.incrementAndGet();
                        if (result == 1) {
                            firstStarted.countDown();
                            assertTrue(releaseFirst.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Release first calculation");
                        } else {
                            secondInterval = interval;
                            secondStarted.countDown();
                            if (failQueuedUpdate) {
                                throw queuedFailure;
                            }
                        }
                    } else {
                        result = interval.to;
                    }
                    return result;
                }
            }, "SmartFutureCacheQueuedUpdateTest");
        }

        private void startFirstUpdate() throws InterruptedException {
            cache.triggerUpdate(KEY, /* updateInterval */ null);
            assertTrue(firstStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "First calculation must start");
        }

        private Reader startWaitingReader() {
            final Reader reader = new Reader(cache);
            readers.add(reader);
            reader.awaitQueuedResult();
            return reader;
        }

        private void assertLatest(final Reader reader) throws Exception {
            assertEquals(LATEST_VALUE, reader.result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        @Override
        public void close() throws InterruptedException {
            releaseFirst.countDown();
            for (final Reader reader : readers) {
                reader.result.cancel(/* mayInterruptIfRunning */ true);
            }
            for (final Reader reader : readers) {
                reader.thread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
                assertFalse(reader.thread.isAlive(), "Reader must be cleaned up even when a regression fails");
            }
        }
    }

    @Test
    public void singleReaderReceivesQueuedUpdateWhileSuspended() throws Exception {
        try (final Fixture fixture = new Fixture(/* failQueuedUpdate */ false)) {
            fixture.startFirstUpdate();
            fixture.cache.suspend();
            fixture.cache.triggerUpdate(KEY, /* updateInterval */ null);
            final Reader reader = fixture.startWaitingReader();
            fixture.releaseFirst.countDown();
            fixture.assertLatest(reader);
            assertEquals(LATEST_VALUE.intValue(), fixture.computations.get());
        }
    }

    @Test
    public void secondReaderDoesNotOrphanFirstReader() throws Exception {
        assertBothReadersComplete(/* suspended */ false);
    }

    @Test
    public void secondReaderDoesNotOrphanFirstReaderWhileSuspended() throws Exception {
        assertBothReadersComplete(/* suspended */ true);
    }

    private void assertBothReadersComplete(final boolean suspended) throws Exception {
        try (final Fixture fixture = new Fixture(/* failQueuedUpdate */ false)) {
            fixture.startFirstUpdate();
            if (suspended) {
                fixture.cache.suspend();
            }
            fixture.cache.triggerUpdate(KEY, /* updateInterval */ null);
            final Reader first = fixture.startWaitingReader();
            final Reader second = fixture.startWaitingReader();
            fixture.releaseFirst.countDown();
            fixture.assertLatest(second);
            fixture.assertLatest(first);
            assertEquals(LATEST_VALUE.intValue(), fixture.computations.get());
        }
    }

    @Test
    public void resumePreservesQueuedUpdateAndWaitingReader() throws Exception {
        try (final Fixture fixture = new Fixture(/* failQueuedUpdate */ false)) {
            fixture.startFirstUpdate();
            fixture.cache.suspend();
            fixture.cache.triggerUpdate(KEY, /* updateInterval */ null);
            final Reader reader = fixture.startWaitingReader();
            fixture.cache.resume();
            fixture.cache.resume();
            fixture.releaseFirst.countDown();
            fixture.assertLatest(reader);
            assertEquals(LATEST_VALUE.intValue(), fixture.computations.get());
        }
    }

    @Test
    public void resumePreservesJoinedIntervalWithoutReadersAndSchedulesOtherKeys() throws Exception {
        try (final Fixture fixture = new Fixture(/* failQueuedUpdate */ false)) {
            fixture.startFirstUpdate();
            fixture.cache.suspend();
            fixture.cache.triggerUpdate(KEY, new Range(/* from */ 20, /* to */ 30));
            fixture.cache.triggerUpdate(KEY, new Range(/* from */ 10, /* to */ 25));
            fixture.cache.triggerUpdate("other competitor", new Range(/* from */ 40, /* to */ 50));
            fixture.cache.triggerUpdate("third competitor", new Range(/* from */ 60, /* to */ 70));
            fixture.cache.resume();
            fixture.releaseFirst.countDown();
            // Observe automatic scheduling before issuing a synchronous get that could otherwise mask lost work.
            assertTrue(fixture.secondStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "Queued update must run on resume");
            assertEquals(LATEST_VALUE, fixture.cache.get(KEY, /* waitForLatest */ true));
            assertEquals(/* expected */ 10, fixture.secondInterval.from);
            assertEquals(/* expected */ 30, fixture.secondInterval.to);
            assertEquals(Integer.valueOf(/* i */ 50), fixture.cache.get("other competitor", /* waitForLatest */ true));
            assertEquals(Integer.valueOf(/* i */ 70), fixture.cache.get("third competitor", /* waitForLatest */ true));
            assertEquals(LATEST_VALUE.intValue(), fixture.computations.get());
        }
    }

    @Test
    public void queuedFailureReachesEveryWaitingReader() throws Exception {
        try (final Fixture fixture = new Fixture(/* failQueuedUpdate */ true)) {
            fixture.startFirstUpdate();
            fixture.cache.triggerUpdate(KEY, /* updateInterval */ null);
            final Reader first = fixture.startWaitingReader();
            final Reader second = fixture.startWaitingReader();
            fixture.releaseFirst.countDown();
            for (final Reader reader : new Reader[] { second, first }) {
                final ExecutionException exception = assertThrows(ExecutionException.class,
                        () -> reader.result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                assertSame(fixture.queuedFailure, exception.getCause().getCause());
            }
        }
    }
}
