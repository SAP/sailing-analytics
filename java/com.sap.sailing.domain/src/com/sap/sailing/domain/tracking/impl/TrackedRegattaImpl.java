package com.sap.sailing.domain.tracking.impl;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.metering.CPUMeter;
import com.sap.sse.util.ThreadLocalTransporter;

public abstract class TrackedRegattaImpl implements TrackedRegatta {
    private static final long serialVersionUID = 6480508193567014285L;

    private static final Logger logger = Logger.getLogger(TrackedRegattaImpl.class.getName());
    
    private final Regatta regatta;
    
    /**
     * Guards access to {@link #trackedRaces}. Callers of {@link #getTrackedRaces()} need to acquire the
     * read lock before iterating.
     */
    private final NamedReentrantReadWriteLock trackedRacesLock;
    
    /**
     * Guarded by {@link #trackedRacesLock}
     */
    private final Map<RaceDefinition, TrackedRace> trackedRaces;
    
    /**
     * These are the {@link RaceListener RaceListeners} attached to this {@link TrackedRegatta}. There are listeners
     * registered for synchronous callback execution and such registered for asynchronous callback execution. For every
     * {@link RaceListener} registered for asynchronous callback execution there is an
     * {@link AsynchronousRunnableExecutor} to do all event related work for the specific listener. This ensures that
     * e.g. events are received in order. The following cases need to be handled through this queue:
     * <ul>
     * <li>Firing events when adding/removing {@link TrackedRace} instances (see {@link #enqueEvent}). The list of
     * listeners to fire the event to need to be the list of listeners existing when enqueuing the event. This ensures
     * that newly added listeners only receive events after the initial {@link TrackedRace} instances are delivered to
     * this listener.</li>
     * <li>Firing events for the already existing {@link TrackedRace} instances when adding a new listener (see
     * {@link #addRaceListener(RaceListener, Optional, boolean)}). This ensures that all events are correctly fired to
     * this listener that are triggered after the listener was added while suppressing inconsistent events before/while
     * the initial {@link TrackedRace} instances are delivered to this listener.</li>
     * <li>Completing the future returned by {@link #removeRaceListener(RaceListener)} to ensure that the receiver gets
     * to know when it is guaranteed that no more event will be fired to the listener.
     * </ul>
     *
     * <p>Concurrency contract: this is a {@link ConcurrentHashMap} and no dedicated lock guards it. Instead, the
     * required serialization comes from {@link #trackedRacesLock}:
     * <ul>
     * <li>{@link #enqueEvent} is only ever called from {@link #addTrackedRace} and {@link #removeTrackedRace}, both of
     * which hold {@link #trackedRacesLock} for write while calling it. During that write region, no thread can be
     * inside {@link #addRaceListener} or {@link #removeRaceListener}, both of which need
     * {@link #trackedRacesLock} for read at their entry (line {@code lockTrackedRacesForRead()}) -- read blocks on
     * write. So the listener set is stable for the whole duration of a dispatch, without needing a dedicated
     * {@code raceListenersLock}.</li>
     * <li>{@link #addRaceListener} and {@link #removeRaceListener} do their own mutations through
     * {@link ConcurrentHashMap#computeIfAbsent} / {@link ConcurrentHashMap#remove}, both of which are atomic per key
     * on {@link ConcurrentHashMap}. Concurrent add/add or remove/remove for the same listener collapse safely; for
     * different listeners the map's own synchronization handles it.</li>
     * <li>Iterating this map from {@link #enqueEvent} uses {@link ConcurrentHashMap#forEach}, which is weakly
     * consistent; combined with the outer {@link #trackedRacesLock} write hold, iteration observes a stable snapshot
     * of the listener set.</li>
     * </ul>
     *
     * <p>A dedicated {@code raceListenersLock} used to exist here. It was dropped as part of issue #6241 because it
     * caused a read-then-write self-deadlock: {@link #enqueEvent} would hold {@code raceListenersLock} for read and
     * dispatch synchronously to listeners; if a listener's callback in turn called
     * {@link #addRaceListener} (e.g. via a wind-estimation installation primitive that registers a race-removal
     * listener), the {@code raceListenersLock} write acquisition would block on the same thread's own read hold, and
     * {@link java.util.concurrent.locks.ReentrantReadWriteLock} does not allow upgrading.
     */
    private transient ConcurrentMap<RaceListener, RunnableExecutor> raceListeners;

    public TrackedRegattaImpl(Regatta regatta) {
        super();
        this.trackedRacesLock = new NamedReentrantReadWriteLock("trackeRaces lock for tracked regatta "+regatta.getName(), /* fair */ false);
        this.regatta = regatta;
        this.trackedRaces = new HashMap<RaceDefinition, TrackedRace>();
        this.raceListeners = new ConcurrentHashMap<>();
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.raceListeners = new ConcurrentHashMap<>();
    }
    
    @Override
    public CPUMeter getCPUMeter() {
        return getRegatta().getCPUMeter();
    }

    @Override
    public void lockTrackedRacesForRead() {
        LockUtil.lockForRead(trackedRacesLock);
    }

    @Override
    public void unlockTrackedRacesAfterRead() {
        LockUtil.unlockAfterRead(trackedRacesLock);
    }

    @Override
    public void lockTrackedRacesForWrite() {
        LockUtil.lockForWrite(trackedRacesLock);
    }

    @Override
    public void unlockTrackedRacesAfterWrite() {
        LockUtil.unlockAfterWrite(trackedRacesLock);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        lockTrackedRacesForRead();
        try {
            oos.defaultWriteObject();
        } finally {
            unlockTrackedRacesAfterRead();
        }
    }
    
    @Override
    public void addTrackedRace(TrackedRace trackedRace, Optional<ThreadLocalTransporter> threadLocalTransporter) {
        final TrackedRace oldTrackedRace;
        lockTrackedRacesForWrite();
        try {
            logger.info("adding tracked race for "+trackedRace.getRace()+" to tracked regatta "+getRegatta().getName()+
                    " with regatta hash code "+getRegatta().hashCode());
            oldTrackedRace = trackedRaces.put(trackedRace.getRace(), trackedRace);
            if (oldTrackedRace != trackedRace) {
                notifyListenersAboutTrackedRaceAdded(trackedRace, threadLocalTransporter);
            }
        } finally {
            unlockTrackedRacesAfterWrite();
        }
    }

    protected void notifyListenersAboutTrackedRaceAdded(TrackedRace trackedRace, Optional<ThreadLocalTransporter> threadLocalTransporter) {
        enqueEvent(listener -> listener.raceAdded(trackedRace), threadLocalTransporter);
    }
    
    /**
     * Firing events is handled through {@link EventQueue} instances per {@link RaceListener} to ensure that events are fired in order. This method
     * enqueues an event for each currently known listeners.
     */
    protected void enqueEvent(Consumer<RaceListener> fireEventCallback, Optional<ThreadLocalTransporter> threadLocalTransporter) {
        // No dedicated lock on raceListeners here: callers hold trackedRacesLock for write
        // (via addTrackedRace / removeTrackedRace), which excludes addRaceListener / removeRaceListener
        // (both of which need trackedRacesLock for read). See the raceListeners field's Javadoc.
        threadLocalTransporter.ifPresent(ThreadLocalTransporter::rememberThreadLocalStates);
        raceListeners.forEach((listener, eventQueue) -> {
            eventQueue.addWork(() -> {
                withBeforeAndAfterHandling(threadLocalTransporter, () -> {
                    fireEventCallback.accept(listener);
                });
            });
        });
    }
    
    private void withBeforeAndAfterHandling(Optional<ThreadLocalTransporter> threadLocalTransporter, Runnable action) {
        threadLocalTransporter.ifPresent(ThreadLocalTransporter::pushThreadLocalStates);
        try {
            action.run();
        } finally {
            threadLocalTransporter.ifPresent(ThreadLocalTransporter::popThreadLocalStates);
        }
    }
    
    @Override
    public void removeTrackedRace(TrackedRace trackedRace, Optional<ThreadLocalTransporter> threadLocalTransporter) {
        lockTrackedRacesForWrite();
        try {
            trackedRaces.remove(trackedRace.getRace());
            notifyListenersAboutTrackedRaceRemoved(trackedRace, threadLocalTransporter);
        } finally {
            unlockTrackedRacesAfterWrite();
        }
    }

    protected void notifyListenersAboutTrackedRaceRemoved(TrackedRace trackedRace, Optional<ThreadLocalTransporter> threadLocalTransporter) {
        enqueEvent(listener -> listener.raceRemoved(trackedRace), threadLocalTransporter);
    }

    @Override
    public Regatta getRegatta() {
        return regatta;
    }

    @Override
    public Iterable<? extends TrackedRace> getTrackedRaces() {
        if (trackedRacesLock.getReadHoldCount() <= 0 && trackedRacesLock.getWriteHoldCount() <= 0) {
            throw new IllegalStateException("Callers of TrackedRegatta.getTrackedRaces() must hold the read lock; see TrackedRegatta.lockTrackedRacesForRead()");
        }
        return trackedRaces.values();
    }

    @Override
    public TrackedRace getTrackedRace(RaceDefinition race) {
        boolean interrupted = false;
        TrackedRace result = getExistingTrackedRace(race);
        if (!interrupted && result == null) {
            final Object mutex = new Object();
            final RaceListener listener = new RaceListener() {
                @Override
                public void raceRemoved(TrackedRace trackedRace) {}
                
                @Override
                public void raceAdded(TrackedRace trackedRace) {
                    synchronized (mutex) { // TODO possible improvement: only notify if trackedRace.getRace() == race; otherwise it cannot have made a difference for getExistingTrackedRace(race)...
                        mutex.notifyAll();
                    }
                }
            };
            addRaceListener(listener, Optional.empty(), /* synchronous */ false);
            try {
                synchronized (mutex) {
                    if (getRegatta().getRaceByName(race.getName()) == null) {
                        throw new IllegalStateException("Race "+race.getName()+" not in regatta "+getRegatta().getName()+
                                "; not blocking for it to appear. It most likely won't");
                    }
                    result = getExistingTrackedRace(race);
                    while (!interrupted && result == null) {
                        try {
                            mutex.wait();
                            result = getExistingTrackedRace(race);
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                }
            } finally {
                removeRaceListener(listener);
            }
        }
        return result;
    }
    
    @Override
    public TrackedRace getExistingTrackedRace(RaceDefinition race) {
        lockTrackedRacesForRead();
        try {
            return trackedRaces.get(race);
        } finally {
            unlockTrackedRacesAfterRead();
        }
    }

    @Override
    public void addRaceListener(RaceListener listener, Optional<ThreadLocalTransporter> threadLocalTransporter, boolean synchronous) {
        assert synchronous == false || !threadLocalTransporter.isPresent(); // transporting thread locals doesn't make sense for synchronous listeners
        // Hold trackedRacesLock for read so that no addTrackedRace / removeTrackedRace runs
        // concurrently and races with our catch-up snapshot below. The tracked races cannot
        // change while we hold this lock. Registration into raceListeners itself is atomic via
        // ConcurrentHashMap.computeIfAbsent, which also collapses duplicate registrations of
        // the same listener; no dedicated lock on raceListeners is needed.
        lockTrackedRacesForRead();
        try {
            raceListeners.computeIfAbsent(listener, listenerToAdd -> {
                final RunnableExecutor eventQueue = synchronous ? new SynchronousRunnableExecutor() : new AsynchronousRunnableExecutor();
                final List<TrackedRace> trackedRacesCopy = new ArrayList<>();
                Util.addAll(getTrackedRaces(), trackedRacesCopy);
                threadLocalTransporter.ifPresent(ThreadLocalTransporter::rememberThreadLocalStates);
                eventQueue.addWork(() -> {
                    withBeforeAndAfterHandling(threadLocalTransporter, () -> {
                        for (TrackedRace trackedRace : trackedRacesCopy) {
                            listenerToAdd.raceAdded(trackedRace);
                        }
                    });
                });
                return eventQueue;
            });
        } finally {
            unlockTrackedRacesAfterRead();
        }
    }

    @Override
    public Future<Boolean> removeRaceListener(RaceListener listener) {
        final CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
        // Hold trackedRacesLock for read so that no addTrackedRace / removeTrackedRace runs
        // concurrently while we're removing the listener; that keeps enqueEvent from possibly
        // enqueuing an event on a listener we're about to consider gone. The remove itself is
        // atomic via ConcurrentHashMap.remove; no dedicated lock on raceListeners is needed.
        lockTrackedRacesForRead();
        try {
            final RunnableExecutor eventQueue = raceListeners.remove(listener);
            if (eventQueue != null) {
                eventQueue.addWork(() -> {
                    result.complete(Boolean.TRUE);
                });
            } else {
                result.complete(Boolean.TRUE);
            }
        } finally {
            unlockTrackedRacesAfterRead();
        }
        return result;
    }

    @Override
    public int getTotalPoints(Competitor competitor, TimePoint timePoint) throws NoWindException {
        int result = 0;
        lockTrackedRacesForRead();
        try {
            for (TrackedRace trackedRace : getTrackedRaces()) {
                result += trackedRace.getRank(competitor, timePoint);
            }
            return result;
        } finally {
            unlockTrackedRacesAfterRead();
        }
    }

    @Override
    public DynamicTrackedRace createTrackedRace(RaceDefinition raceDefinition, Iterable<Sideline> sidelines,
            WindStore windStore, long delayToLiveInMillis,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            DynamicRaceDefinitionSet raceDefinitionSetToUpdate, boolean useInternalMarkPassingAlgorithm, RaceLogAndTrackedRaceResolver raceLogResolver,
            Optional<ThreadLocalTransporter> threadLocalTransporter, TrackingConnectorInfo trackingConnectorInfo,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry, ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry ) {
        logger.log(Level.INFO, "Creating DynamicTrackedRaceImpl for RaceDefinition " + raceDefinition.getName());
        DynamicTrackedRaceImpl result = new DynamicTrackedRaceImpl(this, raceDefinition, sidelines, windStore,
                delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                millisecondsOverWhichToAverageSpeed,
                /* useMarkPassingCalculator */useInternalMarkPassingAlgorithm, getRegatta().getRankingMetricConstructor(), raceLogResolver,
                trackingConnectorInfo, markPassingRaceFingerprintRegistry, maneuverRaceFingerprintRegistry);
        // adding the raceDefinition to the raceDefinitionSetToUpdate BEFORE calling addTrackedRace helps those who
        // are called back by RaceListener.raceAdded(TrackedRace) and who then expect the update to have happened
        if (raceDefinitionSetToUpdate != null) {
            raceDefinitionSetToUpdate.addRaceDefinition(raceDefinition, result);
        }
        addTrackedRace(result, threadLocalTransporter);
        return result;
    }
}
