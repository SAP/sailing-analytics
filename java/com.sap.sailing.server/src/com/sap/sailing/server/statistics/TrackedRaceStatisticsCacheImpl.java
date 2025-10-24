package com.sap.sailing.server.statistics;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.TrackedRaces;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.AbstractTrackedRegattaAndRaceObserver;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Implementation of {@link TrackedRaceStatisticsCache} that observes all {@link TrackedRegatta} and {@link TrackedRace}
 * instances to calculate and update the internal statistics cache.
 */
public class TrackedRaceStatisticsCacheImpl extends AbstractTrackedRegattaAndRaceObserver implements TrackedRaceStatisticsCache {
    private static final Logger logger = Logger.getLogger(TrackedRaceStatisticsCacheImpl.class.getName());
    
    private static final Duration MINIMUM_DELAY_FOR_CACHE_RECALCULATION = Duration.ONE_SECOND.times(10);
    
    /**
     * Listeners added a {@link TrackedRaces} that need to be cleaned when {@link TrackedRace}s are removed. 
     */
    private final Map<TrackedRace, Listener> listeners;
    
    /**
     * Cache that holds and updates {@link TrackedRaceStatistics} instances per known {@link TrackedRace}.
     */
    private final SmartFutureCache<TrackedRace, TrackedRaceStatistics, ?> cache;
    
    private final ScheduledExecutorService executor;
    
    /**
     * We don't want to flood the CPU with cache re-calculations. Therefore, we enqueue triggers with the
     * {@link #executor} when nothing is scheduled yet. Once the trigger is forwarded to the actual
     * {@link SmartFutureCache}, the respective entry for the {@link TrackedRace} is removed while holding the
     * monitor of the {@link #scheduledTriggers} map.
     */
    private final WeakHashMap<TrackedRace, Future<?>> scheduledTriggers;
    
    public TrackedRaceStatisticsCacheImpl() {
        executor = ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor();
        scheduledTriggers = new WeakHashMap<>();
        listeners = new ConcurrentHashMap<>();
        cache = new SmartFutureCache<>(new Updater(), TrackedRaceStatisticsCacheImpl.class.getSimpleName());
    }

    @Override
    public TrackedRaceStatistics getStatistics(TrackedRace trackedRace) {
        return cache.get(trackedRace, false);
    }
    
    /**
     * For testing purposes only!
     */
    public TrackedRaceStatistics getStatisticsWaitingForLatest(TrackedRace trackedRace) throws InterruptedException {
        synchronized (listeners) {
            while (!listeners.containsKey(trackedRace)) {
                listeners.wait();
            }
        }
        return cache.get(trackedRace, true);
    }
    
    @Override
    protected void onRaceAdded(RegattaAndRaceIdentifier raceIdentifier, DynamicTrackedRegatta trackedRegatta,
            DynamicTrackedRace trackedRace) {
        Listener listener = new Listener(trackedRace);
        synchronized (listeners) {
            listeners.put(trackedRace, listener);
            trackedRace.addListener(listener);
            triggerUpdateDirect(trackedRace);
            listeners.notifyAll();
        }
    }

    private void triggerUpdateScheduled(final DynamicTrackedRace trackedRace) {
        synchronized (scheduledTriggers) {
            if (scheduledTriggers.get(trackedRace) == null) {
                final long delay = MINIMUM_DELAY_FOR_CACHE_RECALCULATION.asMillis();
                logger.log(Level.FINEST, ()->"Scheduling statistics update trigger for race " + trackedRace.getRaceIdentifier()+
                        " in "+delay+"ms");
                scheduledTriggers.put(trackedRace, executor.schedule(()->{
                    synchronized (scheduledTriggers) {
                        scheduledTriggers.remove(trackedRace);
                        // This line needs to be executed in the synchronized block.
                        // In onRaceRemoved we get the future to ensure, that no scheduled job is running.
                        // When this line wouldn't get executed in the synchronized block there would be a chance that
                        // a job isn't found but one is already running and will trigger an update afterwards
                        triggerUpdateDirect(trackedRace);
                    }
                }, delay, TimeUnit.MILLISECONDS));
            }
        }
    }

    private void triggerUpdateDirect(final DynamicTrackedRace trackedRace) {
        cache.triggerUpdate(trackedRace, null);
        logger.log(Level.FINEST, ()->"Triggering statistics update for race " + trackedRace.getRaceIdentifier());
    }

    @Override
    protected void onRaceRemoved(DynamicTrackedRace trackedRace) {
        synchronized (listeners) {
            Listener listener = listeners.remove(trackedRace);
            if (listener != null) {
                trackedRace.removeListener(listener);
            }
            listeners.notifyAll();
        }

        // To prevent leakage of TrackedRace instances as keys in the cache,
        // it must be ensured that no scheduled job or cache update is waiting/running.
        // As first step, a potentially existing scheduled job needs to be cancelled or waited for to finish.
        Future<?> updateFuture = null;
        synchronized (scheduledTriggers) {
            updateFuture = scheduledTriggers.remove(trackedRace);
        }
        if (updateFuture != null && !updateFuture.cancel(false)) {
            try {
                // If cancel doesn't work, due to the update being calculated right now,
                // it needs to waited for the job to finish
                updateFuture.get();
            } catch (Exception e) {
                logger.log(Level.FINEST, () -> "Error while waiting for statistics cache update to finish for race: "
                        + trackedRace.getRaceIdentifier());
            }
        }
        // We can be sure that either a cache update was triggered or no further updated will be triggered
        // We now wait for any result to be updated so that no new entry is generated after removing the entry.
        // The triggerUpdate will at least have been called once.
        final Thread t = new Thread(()->{
            cache.get(trackedRace, true);
            // it's now safe to remove the entry without the risk that it is added again later
            cache.remove(trackedRace);
        }, "Cache cleaner for tracked race "+trackedRace.getRace().getName());
        t.setDaemon(true);
        t.start();
    }

    private class Updater extends AbstractCacheUpdater<TrackedRace, TrackedRaceStatistics, EmptyUpdateInterval> {
        @Override
        public TrackedRaceStatistics computeCacheUpdate(TrackedRace trackedRace, EmptyUpdateInterval updateInterval)
                throws Exception {
            logger.log(Level.FINE, ()->"Updating statistics for race " + trackedRace.getRaceIdentifier());
            return new TrackedRaceStatisticsCalculator(trackedRace, true, true).getStatistics();
        }
    }
    
    private class Listener extends AbstractRaceChangeListener {
        private final DynamicTrackedRace trackedRace;

        public Listener(DynamicTrackedRace trackedRace) {
            this.trackedRace = trackedRace;
        }
        
        @Override
        public void competitorPositionChanged(GPSFixMoving fix, Competitor item, AddResult addedOrReplaced) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void markPositionChanged(GPSFix fix, Mark mark, boolean firstInTrack, AddResult addedOrReplaced) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void windDataReceived(Wind wind, WindSource windSource) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void windDataRemoved(Wind wind, WindSource windSource) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void finishedTimeChanged(TimePoint oldFinishedTime, TimePoint newFinishedTime) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
            triggerUpdateScheduled(trackedRace);
        }
        
        @Override
        public void statusChanged(TrackedRaceStatus newStatus, TrackedRaceStatus oldStatus) {
            triggerUpdateScheduled(trackedRace);
        }
    }
}
