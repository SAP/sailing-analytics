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
     * {@link #addRaceListener(RaceListener)}). This ensures that all events are correctly fired to this listener that
     * are triggered after the listener was added while suppressing inconsistent events before/while the initial
     * {@link TrackedRace} instances are delivered to this listener.</li>
     * <li>Completing the future returned by {@link #removeRaceListener(RaceListener)} to ensure that the receiver gets
     * to know when it is guaranteed that no more event will be fired to the listener.
     * </ul>
     */
    private transient ConcurrentMap<RaceListener, RunnableExecutor> raceListeners;
    
    /**
     * Guards access to {@link #raceListeners}.
     */
    private final NamedReentrantReadWriteLock raceListenersLock;
    
    public TrackedRegattaImpl(Regatta regatta) {
        super();
        this.trackedRacesLock = new NamedReentrantReadWriteLock("trackeRaces lock for tracked regatta "+regatta.getName(), /* fair */ false);
        this.regatta = regatta;
        this.trackedRaces = new HashMap<RaceDefinition, TrackedRace>();
        this.raceListeners = new ConcurrentHashMap<>();
        this.raceListenersLock = new NamedReentrantReadWriteLock(
                "raceListeners lock for tracked regatta " + regatta.getName(), /* fair */ false);
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
        threadLocalTransporter.ifPresent(ThreadLocalTransporter::rememberThreadLocalStates);
        LockUtil.executeWithReadLock(raceListenersLock, () -> {
            raceListeners.forEach((listener, eventQueue) -> {
                eventQueue.addWork(() -> {
                    withBeforeAndAfterHandling(threadLocalTransporter, () -> {
                        fireEventCallback.accept(listener);
                    });
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
        lockTrackedRacesForRead();
        try {
            LockUtil.executeWithWriteLock(raceListenersLock, () -> {
                // This prevents the creation of another WorkQueue if an already known listener is added a second time
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
            });
        } finally {
            unlockTrackedRacesAfterRead();
        }
    }

    @Override
    public Future<Boolean> removeRaceListener(RaceListener listener) {
        final CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
        lockTrackedRacesForRead();
        try {
            final RunnableExecutor eventQueue = LockUtil.executeWithWriteLockAndResult(raceListenersLock,
                    () -> raceListeners.remove(listener));
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
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        logger.log(Level.INFO, "Creating DynamicTrackedRaceImpl for RaceDefinition " + raceDefinition.getName());
        DynamicTrackedRaceImpl result = new DynamicTrackedRaceImpl(this, raceDefinition, sidelines, windStore,
                delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                millisecondsOverWhichToAverageSpeed,
                /* useMarkPassingCalculator */useInternalMarkPassingAlgorithm, getRegatta().getRankingMetricConstructor(), raceLogResolver,
                trackingConnectorInfo, markPassingRaceFingerprintRegistry);
        // adding the raceDefinition to the raceDefinitionSetToUpdate BEFORE calling addTrackedRace helps those who
        // are called back by RaceListener.raceAdded(TrackedRace) and who then expect the update to have happened
        if (raceDefinitionSetToUpdate != null) {
            raceDefinitionSetToUpdate.addRaceDefinition(raceDefinition, result);
        }
        addTrackedRace(result, threadLocalTransporter);
        return result;
    }
}
