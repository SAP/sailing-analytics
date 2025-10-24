package com.sap.sailing.domain.tracking;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.Future;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.impl.TrackedRaces;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sse.common.TimePoint;
import com.sap.sse.metering.HasCPUMeter;
import com.sap.sse.util.ThreadLocalTransporter;

/**
 * Manages a set of {@link TrackedRace} objects that belong to the same {@link Regatta} (regatta, sailing regatta for a
 * single boat class). It therefore represents the entry point into the tracking-related objects for such an regatta.
 * Allows clients to find a {@link TrackedRace} by the {@link RaceDefinition} for which it holds the tracking data.
 * <p>
 * 
 * Please note that the result of calling {@link #getRegatta()}.{@link Regatta#getAllRaces() getAllRaces()} is not
 * guaranteed to match up with the races obtained by calling {@link TrackedRace#getRace()} on all {@link TrackedRaces}
 * resulting from {@link #getTrackedRaces()}. In other words, the processes for adding and removing races to the
 * server do not guarantee to update the master and tracking data for races atomically.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public interface TrackedRegatta extends Serializable, HasCPUMeter {
    Regatta getRegatta();

    /**
     * Callers must {@link #lockTrackedRacesForRead() acquire the read lock} before calling this method and hold on to the lock
     * while iterating over the data structure returned. Example:
     * <pre>
     *     trackedRegatta.lockTrackedRacesForRead();
     *     try {
     *         for (TrackedRace trackedRace : trackedRegatta.getTrackedRaces()) {
     *             // do something
     *         }
     *     } finally {
     *         trackedRegatta.unlockTrackedRacesAfterRead();
     *     }
     * </pre>
     * The method will throw an {@link IllegalArgumentException} if the caller fails to do so.
     */
    Iterable<? extends TrackedRace> getTrackedRaces();

    void lockTrackedRacesForRead();
    
    void unlockTrackedRacesAfterRead();
    
    void lockTrackedRacesForWrite();

    void unlockTrackedRacesAfterWrite();

    /**
     * Creates a {@link TrackedRace} based on the parameter specified and {@link #addTrackedRace(TrackedRace) adds} it
     * to this tracked regatta. Afterwards, calling {@link #getTrackedRace(RaceDefinition) getTrackedRace(raceDefinition)}
     * will return the result of this method call.
     * @param raceDefinitionSetToUpdate
     *            if not <code>null</code>, after creating the {@link TrackedRace}, the <code>raceDefinition</code> is
     *            {@link DynamicRaceDefinitionSet#addRaceDefinition(RaceDefinition, DynamicTrackedRace) added} to that object.
     */
    DynamicTrackedRace createTrackedRace(RaceDefinition raceDefinition, Iterable<Sideline> sidelines, WindStore windStore,
            long delayToLiveInMillis, long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            DynamicRaceDefinitionSet raceDefinitionSetToUpdate, boolean useInternalMarkPassingAlgorithm, RaceLogAndTrackedRaceResolver raceLogResolver,
            Optional<ThreadLocalTransporter> beforeAndAfterNotificationHandler, TrackingConnectorInfo trackingConnectorInfo,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry);

    /**
     * Obtains the tracked race for <code>race</code>. Blocks until the tracked race has been created
     * and added to this tracked regatta (see {@link #addTrackedRace(TrackedRace)}).
     */
    TrackedRace getTrackedRace(RaceDefinition race);

    /**
     * Non-blocking call that returns <code>null</code> if no tracking information currently exists
     * for <code>race</code>. See also {@link #getTrackedRace(RaceDefinition)} for a blocking variant.
     */
    TrackedRace getExistingTrackedRace(RaceDefinition race);
    
    void addTrackedRace(TrackedRace trackedRace, Optional<ThreadLocalTransporter> beforeAndAfterNotificationHandler);

    void removeTrackedRace(TrackedRace trackedRace, Optional<ThreadLocalTransporter> beforeAndAfterNotificationHandler);

    /**
     * Listener will be notified when {@link #addTrackedRace(TrackedRace)} is called and upon registration for each
     * tracked race already known. Therefore, the listener won't miss any tracked race.<br>
     * 
     * Events for synchronous listeners are processed in the calling thread. This implies that implementations must not
     * block for events triggered only by other callbacks to implementations of this interface, or else they risk a
     * deadlock. For example, trying a blocking wait for another {@link TrackedRace} to appear is a bad idea because the
     * appearance of that other race may have to be signalled by a {@link #raceAdded(TrackedRace)} callback.<p>
     * 
     * Note that for asynchronous listeners you have to ensure that callbacks in particular to their {@link RaceListener#raceAdded(TrackedRace)}
     * and {@link RaceListener#raceRemoved(TrackedRace)} methods cannot block, either. A particular risk for them blocking
     * is when {@link #removeRaceListener(RaceListener)} is invoked while holding any object monitors that any of the tasks
     * enqueued for the {@link RaceListener} to be removed may be waiting for any of these monitors. Then, those pending tasks
     * will not be able to complete, and conversely, {@link #removeRaceListener(RaceListener)} won't, either, because it
     * will wait for all tasks enqueued for that listener to complete before returning. See also bug5879.
     * 
     * @param listener
     *            the listener to add
     * @param beforeAndAfterNotificationHandler
     *            can be used to carry across the state of any {@link ThreadLocal}s from the thread where the callback
     *            is triggered to the thread executing the listener's code; this is only useful for listeners registering
     *            for asynchronous callbacks ({@code synchronous==false}).
     * @param synchronous
     *            if {@code true}, the listener will be invoked synchronously where the callback is triggered; there is no need
     *            for transporting {@link ThreadLocal} state to the executing thread in this case. If {@code false}, a work queue
     *            will be created specifically for the {@code listener} registered by this call, and a separate thread for this
     *            queue will execute its callback invocations, preserving the "per-listener callback order" but without
     *            guaranteeing any ordering across several distinct listeners.
     */
    void addRaceListener(RaceListener listener, Optional<ThreadLocalTransporter> beforeAndAfterNotificationHandler, boolean synchronous);
    
    /**
     * Removes the given listener and returns a {@link Future} that will be completed
     * when it is guaranteed that no more events will be fired to the listener.
     */
    Future<Boolean> removeRaceListener(RaceListener listener);

    int getTotalPoints(Competitor competitor, TimePoint timePoint) throws NoWindException;

}