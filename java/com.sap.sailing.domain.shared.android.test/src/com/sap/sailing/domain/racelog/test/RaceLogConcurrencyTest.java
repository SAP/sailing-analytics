package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.AbstractLogImpl;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogChangedVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRevokeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogConcurrencyTest {
    private static final AbstractLogEventAuthor AUTHOR = new LogEventAuthorImpl("concurrency test", /* priority */ 0);

    /** Pauses the writer immediately after publication, without retaining the write lock. */
    private static class PausingAfterWriteRaceLog extends RaceLogImpl {
        private static final long serialVersionUID = -7479915799438088241L;
        private final AtomicBoolean pauseNextWrite = new AtomicBoolean();
        private final CountDownLatch writeUnlocked = new CountDownLatch(/* count */ 1);
        private final CountDownLatch continueWriter = new CountDownLatch(/* count */ 1);

        private PausingAfterWriteRaceLog() {
            super("publication test");
        }

        @Override
        protected void unlockAfterWrite() {
            super.unlockAfterWrite();
            if (pauseNextWrite.compareAndSet(/* expectedValue */ true, /* newValue */ false)) {
                writeUnlocked.countDown();
                await(continueWriter);
            }
        }
    }

    private static RaceLogEvent createEvent(final int index) {
        final TimePoint time = new MillisecondsTimePoint(index);
        // Distinct UUIDs with the same hash code stress bucket insertion, resizing and treeification.
        final UUID id = new UUID(/* mostSigBits */ 0, ((long) index << 32) | index);
        return new RaceLogStartTimeEventImpl(time, time, AUTHOR, id, /* pPassId */ 0, time,
                RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null);
    }

    private static ExecutorService createExecutor(final int threads) {
        return Executors.newFixedThreadPool(threads, (final Runnable runnable) -> {
            final Thread thread = new Thread(runnable, "race-log-concurrency-test");
            // A broken HashMap can spin indefinitely; such a worker must not keep the test JVM alive.
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(/* timeout */ 10, TimeUnit.SECONDS), "Timed out coordinating log access");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void shutdown(final ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(/* timeout */ 5, TimeUnit.SECONDS), "Log workers did not terminate");
    }

    @Test
    public void testParallelAddsRetainEveryEventInIdIndex() throws Exception {
        final int workers = 16;
        final int eventsPerWorker = 256;
        final ExecutorService executor = createExecutor(workers);
        try {
            for (int round = 0; round < 4; round++) {
                final RaceLog log = new RaceLogImpl("parallel additions");
                final List<RaceLogEvent> events = new ArrayList<>();
                for (int index = 0; index < workers * eventsPerWorker; index++) {
                    events.add(createEvent(index));
                }
                final CountDownLatch ready = new CountDownLatch(workers);
                final List<Callable<Void>> tasks = new ArrayList<>();
                for (int worker = 0; worker < workers; worker++) {
                    final int offset = worker * eventsPerWorker;
                    tasks.add(() -> {
                        ready.countDown();
                        await(ready);
                        for (int index = offset; index < offset + eventsPerWorker; index++) {
                            assertTrue(log.add(events.get(index)));
                        }
                        return null;
                    });
                }
                for (final Future<Void> task : executor.invokeAll(tasks, /* timeout */ 30, TimeUnit.SECONDS)) {
                    task.get();
                }
                // Bound lookups too: corruption can also make HashMap.get() loop indefinitely.
                executor.submit(() -> {
                    log.lockForRead();
                    try {
                        assertEquals(events.size(), Util.size(log.getRawFixes()));
                        for (final RaceLogEvent event : events) {
                            assertSame(event, log.getEventById(event.getId()), "Missing indexed event " + event.getId());
                        }
                    } finally {
                        log.unlockAfterRead();
                    }
                }).get(/* timeout */ 30, TimeUnit.SECONDS);
            }
        } finally {
            shutdown(executor);
        }
    }

    @Test
    public void testIndexIsPublishedTogetherWithRawEvent() throws Exception {
        final PausingAfterWriteRaceLog log = new PausingAfterWriteRaceLog();
        final RaceLogEvent event = createEvent(/* index */ 1);
        assertPublishedState(log, event, () -> assertSame(event, log.getEventById(event.getId())));
    }

    @Test
    public void testRevocationIsPublishedTogetherWithRawEvent() throws Exception {
        final PausingAfterWriteRaceLog log = new PausingAfterWriteRaceLog();
        final RaceLogEvent event = createEvent(/* index */ 1);
        log.load(event);
        final RaceLogEvent revoke = new RaceLogRevokeEventImpl(AUTHOR, /* passId */ 0, event, "test revocation");
        assertPublishedState(log, revoke, () -> {
            assertFalse(log.getUnrevokedEvents().contains(event), "Revocation must be visible with its raw event");
            assertSame(revoke, log.getEventById(revoke.getId()));
        });
    }

    private void assertPublishedState(final PausingAfterWriteRaceLog log, final RaceLogEvent event,
            final Runnable checkState) throws Exception {
        final ExecutorService executor = createExecutor(/* threads */ 1);
        log.pauseNextWrite.set(true);
        final Future<Boolean> added = executor.submit(() -> log.add(event));
        try {
            await(log.writeUnlocked);
            log.lockForRead();
            try {
                assertTrue(log.getRawFixes().contains(event));
                checkState.run();
            } finally {
                log.unlockAfterRead();
            }
        } finally {
            log.continueWriter.countDown();
            try {
                assertTrue(added.get(/* timeout */ 10, TimeUnit.SECONDS));
            } finally {
                shutdown(executor);
            }
        }
    }

    @Test
    public void testMergeNotifiesOnlyNewEventsInOrderAfterCompletingStateUpdates() {
        final RaceLog source = new RaceLogImpl("source");
        final RaceLog destination = new RaceLogImpl("destination");
        final RaceLogEvent first = createEvent(/* index */ 1);
        final RaceLogEvent common = createEvent(/* index */ 2);
        final RaceLogEvent last = createEvent(/* index */ 3);
        source.load(first);
        source.load(common);
        source.load(last);
        destination.load(common);
        final List<RaceLogEvent> notifications = new ArrayList<>();
        final List<Integer> observedSizes = new ArrayList<>();
        destination.addListener(new RaceLogChangedVisitor((final RaceLogEvent event) -> {
            notifications.add(event);
            destination.lockForRead();
            try {
                observedSizes.add(Util.size(destination.getRawFixes()));
            } finally {
                destination.unlockAfterRead();
            }
        }));
        destination.merge(source);
        destination.merge(source);
        destination.merge(destination);
        destination.load(createEvent(/* index */ 4));
        assertEquals(Arrays.asList(first, last), notifications);
        assertEquals(Arrays.asList(/* firstCallback */ 3, /* secondCallback */ 3), observedSizes);
    }

    @Test
    public void testFailedMergeStillNotifiesSuccessfullyAddedPrefix() {
        final RaceLog source = new RaceLogImpl("source");
        final RaceLog destination = new RaceLogImpl("destination");
        final RaceLogEvent first = createEvent(/* index */ 1);
        final RaceLogEvent broken = spy(createEvent(/* index */ 2));
        source.load(first);
        source.load(broken);
        doThrow(new IllegalArgumentException("broken event")).when(broken).getAuthor();
        final List<RaceLogEvent> notifications = new ArrayList<>();
        destination.addListener(new RaceLogChangedVisitor(notifications::add));
        assertThrows(IllegalArgumentException.class, () -> destination.merge(source));
        assertEquals(Arrays.asList(first), notifications);
        destination.lockForRead();
        try {
            assertSame(first, destination.getEventById(first.getId()));
        } finally {
            destination.unlockAfterRead();
        }
    }

    @Test
    public void testAddRevocationWarningAllowsWritingFromAnotherThread() throws Exception {
        assertRevocationWarningAllowsWriting(/* merge */ false);
    }

    @Test
    public void testMergeRevocationWarningAllowsWritingToBothLogsFromAnotherThread() throws Exception {
        assertRevocationWarningAllowsWriting(/* merge */ true);
    }

    private void assertRevocationWarningAllowsWriting(final boolean merge) throws Exception {
        final RaceLog source = new RaceLogImpl("source");
        final RaceLog destination = new RaceLogImpl("destination");
        final RaceLogEvent revoke = new RaceLogRevokeEventImpl(AUTHOR, /* passId */ 0,
                createEvent(/* index */ 1), "missing event");
        source.load(revoke);
        final ExecutorService executor = createExecutor(/* threads */ 1);
        final AtomicReference<Throwable> loggingFailure = new AtomicReference<>();
        final AtomicBoolean warningLogged = new AtomicBoolean();
        final Handler handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                if (record.getLevel().equals(Level.WARNING)) {
                    warningLogged.set(true);
                    try {
                        executor.submit(() -> {
                            destination.load(revoke);
                            source.load(revoke);
                        }).get(/* timeout */ 2, TimeUnit.SECONDS);
                    } catch (final Exception e) {
                        loggingFailure.set(e);
                    }
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        final Logger logger = Logger.getLogger(AbstractLogImpl.class.getName());
        final Level previousLevel = logger.getLevel();
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        try {
            if (merge) {
                destination.merge(source);
            } else {
                destination.add(revoke);
            }
            assertTrue(warningLogged.get());
            assertNull(loggingFailure.get(), "Warning was logged while holding a log lock");
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            shutdown(executor);
        }
    }

    @Test
    public void testMergeCallbackAllowsWritingToDestinationFromAnotherThread() throws Exception {
        assertCallbackAllowsWriting(/* merge */ true, /* probeSource */ false);
    }

    @Test
    public void testMergeCallbackAllowsWritingToSourceFromAnotherThread() throws Exception {
        assertCallbackAllowsWriting(/* merge */ true, /* probeSource */ true);
    }

    @Test
    public void testAddCallbackAllowsWritingFromAnotherThread() throws Exception {
        assertCallbackAllowsWriting(/* merge */ false, /* probeSource */ false);
    }

    private void assertCallbackAllowsWriting(final boolean merge, final boolean probeSource) throws Exception {
        final RaceLog source = new RaceLogImpl("source");
        final RaceLog destination = new RaceLogImpl("destination");
        final RaceLogEvent event = createEvent(/* index */ 1);
        source.load(event);
        final ExecutorService executor = createExecutor(/* threads */ 1);
        final AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
        final List<RaceLogEvent> notifications = new ArrayList<>();
        destination.addListener(new RaceLogChangedVisitor((final RaceLogEvent added) -> {
            notifications.add(added);
            try {
                // Loading an existing event still needs the write lock, without producing another callback.
                executor.submit(() -> (probeSource ? source : destination).load(event))
                        .get(/* timeout */ 2, TimeUnit.SECONDS);
            } catch (final Exception e) {
                // AbstractLogImpl catches listener exceptions, so report the failure from the test thread.
                callbackFailure.set(e);
            }
        }));
        try {
            if (merge) {
                destination.merge(source);
            } else {
                destination.add(event);
            }
            assertEquals(Arrays.asList(event), notifications);
            assertNull(callbackFailure.get(), "Listener could not acquire a log's write lock from another thread");
        } finally {
            shutdown(executor);
        }
    }
}
