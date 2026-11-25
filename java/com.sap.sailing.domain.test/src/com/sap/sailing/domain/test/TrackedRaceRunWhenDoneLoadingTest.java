package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests {@link com.sap.sailing.domain.tracking.TrackedRace#runWhenDoneLoading(Runnable)}.
 * The &quot;done loading&quot; condition is {@code hasFinishedLoading(status)}, i.e. the status is
 * neither {@link TrackedRaceStatusEnum#PREPARED}, {@link TrackedRaceStatusEnum#LOADING} nor
 * {@link TrackedRaceStatusEnum#ERROR} -- so {@link TrackedRaceStatusEnum#TRACKING} and
 * {@link TrackedRaceStatusEnum#FINISHED} both satisfy it, whereas {@code PREPARED} does not.
 * <p>
 * These are contract tests for the single-threaded behaviour: fire exactly once when the race
 * becomes done loading, fire immediately when already done, and do not fire (but tear the
 * listeners down) when the race is removed before ever becoming done loading. They do NOT
 * reproduce the concurrency defect that motivated the fix: on ARCHIVE a single race drove the
 * restore counters to {@code numberofracesstillloading == -1} because the previous implementation
 * armed a self-removing status listener without a one-shot guard, and
 * {@link com.sap.sailing.domain.tracking.impl.TrackedRaceImpl#notifyListeners} iterates a pre-lock
 * snapshot of the listener set -- so a second past-LOADING transition delivered from another
 * thread while the first dispatch was still iterating that snapshot fired the listener again
 * before {@code removeListener} had shortened the live set. Because
 * {@link com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl#setStatus} dispatches
 * {@code statusChanged} synchronously, a single-threaded test cannot deterministically re-create
 * that interleaving (the removal always becomes visible before the next sequential transition),
 * and a threaded stress test for it would be timing-dependent and flaky. The fix's one-shot
 * guarantee instead rests on the {@code settled} compare-and-set in {@code runWhenDoneLoading};
 * these tests lock the surrounding contract and guard against future single-threaded regressions.
 * See also the sibling {@link TrackedRaceRunWhenPastLoadingTest} (bug 6241).
 *
 * @author Axel Uhl (d043530)
 */
public class TrackedRaceRunWhenDoneLoadingTest extends TrackBasedTest {
    private CompetitorWithBoat competitor;
    private DynamicTrackedRace trackedRace;

    @BeforeEach
    public void setUp() {
        competitor = createCompetitorWithBoat("Test Competitor");
        trackedRace = createTestTrackedRace("Test Regatta", "Test Race", "505",
                createCompetitorAndBoatsMap(competitor), MillisecondsTimePoint.now(),
                /* useMarkPassingCalculator */ false);
    }

    /**
     * When the race is already done loading at the time of the call, the callback must run
     * immediately (synchronously on the caller's thread).
     */
    @Test
    public void testFiresImmediatelyWhenAlreadyDoneLoading() {
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(TrackedRaceStatusEnum.TRACKING, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenDoneLoading(() -> firings.incrementAndGet());
        assertEquals(1, firings.get(), "callback must fire immediately when race is already done loading");
    }

    /**
     * While the race is still in PREPARED, the callback must not fire; PREPARED does not count as
     * done loading. It must fire once the race reaches TRACKING.
     */
    @Test
    public void testDoesNotFireWhilePreparedThenFiresOnTracking() {
        assertEquals(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenDoneLoading(() -> firings.incrementAndGet());
        assertEquals(0, firings.get(), "callback must not fire while race is in PREPARED");
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(1, firings.get(), "callback must fire when race becomes done loading");
    }

    /**
     * The callback must fire exactly once even when the race is driven through LOADING, then
     * TRACKING, then FINISHED -- i.e. two distinct past-LOADING transitions. Note that the old
     * implementation also passed this single-threaded sequence (its {@code removeListener} became
     * visible before the FINISHED transition); the ARCHIVE {@code stillloading == -1} needed a
     * concurrent second transition racing the pre-lock listener snapshot, which this test does not
     * reproduce (see the class Javadoc).
     */
    @Test
    public void testFiresExactlyOnceAcrossMultipleTransitions() {
        assertEquals(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenDoneLoading(() -> firings.incrementAndGet());
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 0.5));
        assertEquals(TrackedRaceStatusEnum.LOADING, trackedRace.getStatus().getStatus());
        assertEquals(0, firings.get(), "callback must not fire while race is still in LOADING");
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(1, firings.get(), "callback must fire once when race becomes done loading");
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.FINISHED, 1.0));
        assertEquals(1, firings.get(), "callback must not fire again on subsequent past-LOADING transitions");
    }

    /**
     * When the race is removed from its regatta before ever becoming done loading, the callback
     * must not fire and the primitive must tear down its listeners.
     */
    @Test
    public void testDoesNotFireWhenRaceIsRemovedBeforeDoneLoading() throws InterruptedException {
        final DynamicTrackedRegattaImpl regatta = (DynamicTrackedRegattaImpl) trackedRace.getTrackedRegatta();
        regatta.addTrackedRace(trackedRace, Optional.empty());
        assertEquals(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenDoneLoading(() -> firings.incrementAndGet());
        assertEquals(0, firings.get());
        regatta.removeTrackedRace(trackedRace, Optional.empty());
        // Give the asynchronous race-listener notification a chance to be processed.
        // TrackedRegattaImpl uses AsynchronousRunnableExecutor for non-synchronous listeners, so
        // the raceRemoved event fires on a background thread. We poll for a moment; the callback
        // should never fire in either case.
        final long deadline = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < deadline && firings.get() == 0) {
            Thread.sleep(20);
        }
        assertEquals(0, firings.get(), "callback must not fire when race was removed before becoming done loading");
    }

    /**
     * When the race is removed <em>after</em> the callback has already fired (because the race
     * became done loading), removal is a no-op regarding the callback -- it must not fire a second
     * time.
     */
    @Test
    public void testRemovalAfterFiringDoesNotCauseSecondFiring() {
        final DynamicTrackedRegattaImpl regatta = (DynamicTrackedRegattaImpl) trackedRace.getTrackedRegatta();
        regatta.addTrackedRace(trackedRace, Optional.empty());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenDoneLoading(() -> firings.incrementAndGet());
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(1, firings.get());
        regatta.removeTrackedRace(trackedRace, Optional.empty());
        assertEquals(1, firings.get(), "removal after firing must not cause a second firing");
    }
}
