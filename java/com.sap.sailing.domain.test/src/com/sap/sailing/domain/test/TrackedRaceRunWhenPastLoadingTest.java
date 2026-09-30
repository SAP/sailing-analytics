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
 * Tests {@link com.sap.sailing.domain.tracking.TrackedRace#runWhenPastLoading(Runnable)}.
 * See bug 6241.
 *
 * @author Axel Uhl (d043530)
 */
public class TrackedRaceRunWhenPastLoadingTest extends TrackBasedTest {
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
     * When the race is already past LOADING at the time of the call, the callback must
     * run immediately (synchronously on the caller's thread).
     */
    @Test
    public void testFiresImmediatelyWhenAlreadyPastLoading() {
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(TrackedRaceStatusEnum.TRACKING, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenPastLoading(() -> firings.incrementAndGet());
        assertEquals(1, firings.get(), "callback must fire immediately when race is already past LOADING");
    }

    /**
     * When the race is in PREPARED (initial state) at the time of the call, the callback
     * must not fire until the race transitions past LOADING. A PREPARED to TRACKING jump
     * without ever entering LOADING must still fire the callback -- that's the whole
     * point of "past LOADING" being the condition, matching, e.g., RaceLogRaceTracker
     * races that go straight from PREPARED to TRACKING.
     */
    @Test
    public void testFiresOnPreparedToTrackingTransitionSkippingLoading() {
        assertEquals(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenPastLoading(() -> firings.incrementAndGet());
        assertEquals(0, firings.get(), "callback must not fire while race is in PREPARED");
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(1, firings.get(), "callback must fire when race transitions past LOADING");
    }

    /**
     * Firing happens exactly once even when multiple status transitions past LOADING occur.
     */
    @Test
    public void testFiresExactlyOnceAcrossMultipleTransitions() {
        assertEquals(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenPastLoading(() -> firings.incrementAndGet());
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 0.5));
        assertEquals(TrackedRaceStatusEnum.LOADING, trackedRace.getStatus().getStatus());
        assertEquals(0, firings.get(), "callback must not fire while race is still in LOADING");
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(1, firings.get(), "callback must fire once when race leaves LOADING");
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.FINISHED, 1.0));
        assertEquals(1, firings.get(), "callback must not fire again on subsequent transitions");
    }

    /**
     * When the race is removed from its regatta before ever transitioning past LOADING,
     * the callback must not fire and the primitive must tear down its listeners.
     */
    @Test
    public void testDoesNotFireWhenRaceIsRemovedBeforeReachingPastLoading() throws InterruptedException {
        // add the race to the regatta so that removeTrackedRace has an effect
        final DynamicTrackedRegattaImpl regatta = (DynamicTrackedRegattaImpl) trackedRace.getTrackedRegatta();
        regatta.addTrackedRace(trackedRace, Optional.empty());
        assertEquals(TrackedRaceStatusEnum.PREPARED, trackedRace.getStatus().getStatus());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenPastLoading(() -> firings.incrementAndGet());
        assertEquals(0, firings.get());
        regatta.removeTrackedRace(trackedRace, Optional.empty());
        // Give the asynchronous race-listener notification a chance to be processed.
        // TrackedRegattaImpl uses AsynchronousRunnableExecutor for non-synchronous
        // listeners, so the raceRemoved event fires on a background thread. We poll
        // for a moment; the callback should never fire in either case.
        final long deadline = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < deadline && firings.get() == 0) {
            Thread.sleep(20);
        }
        assertEquals(0, firings.get(), "callback must not fire when race was removed before reaching past LOADING");
    }

    /**
     * When the race is removed <em>after</em> the callback has already fired (because
     * the race reached past LOADING), removal is a no-op regarding the callback -- it
     * must not fire a second time.
     */
    @Test
    public void testRemovalAfterFiringDoesNotCauseSecondFiring() {
        final DynamicTrackedRegattaImpl regatta = (DynamicTrackedRegattaImpl) trackedRace.getTrackedRegatta();
        regatta.addTrackedRace(trackedRace, Optional.empty());
        final AtomicInteger firings = new AtomicInteger(0);
        trackedRace.runWhenPastLoading(() -> firings.incrementAndGet());
        final TrackingDataLoader loader = new TrackingDataLoader() {};
        trackedRace.onStatusChanged(loader, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        assertEquals(1, firings.get());
        regatta.removeTrackedRace(trackedRace, Optional.empty());
        assertEquals(1, firings.get(), "removal after firing must not cause a second firing");
    }
}
