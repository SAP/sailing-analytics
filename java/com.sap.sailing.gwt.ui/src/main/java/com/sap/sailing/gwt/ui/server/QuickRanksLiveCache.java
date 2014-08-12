package com.sap.sailing.gwt.ui.server;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.gwt.ui.shared.QuickRankDTO;
import com.sap.sailing.server.masterdata.DummyTrackedRace;
import com.sap.sailing.util.SmartFutureCache;
import com.sap.sailing.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sailing.util.SmartFutureCache.UpdateInterval;

/**
 * Calculating the quick ranks for many clients for a live race is expensive and therefore benefits from consolidation
 * in a single cache. This cache needs to listen for changes in the races for which it manages those {@link QuickRankDTO} objects
 * and trigger a re-calculation. It uses a {@link SmartFutureCache} to store and update the cache entries. The keys of the
 * {@link SmartFutureCache} are {@link RegattaAndRaceIdentifier}s. In order to properly evict cache entries when the race
 * is no longer reachable, each {@link TrackedRace} is referenced by a {@link WeakReference} which has a queue associated.
 * The cache runs a thread that fetches collected references from the queue and evicts the cache entries for the respective
 * race identifiers.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class QuickRanksLiveCache extends AbstractRaceChangeListener {
    private static final Logger logger = Logger.getLogger(QuickRanksLiveCache.class.getName());
    /**
     * For each weak reference to a tracked race, remembers the race's {@link RegattaAndRaceIdentifier} which is then the key
     * into the {@link SmartFutureCache} from which entries that are no longer referenced shall be removed.
     */
    private final Map<WeakReference<? extends TrackedRace>, RegattaAndRaceIdentifier> fromRefToRaceIdentifier;
    
    private final ReferenceQueue<? extends TrackedRace> referencesToGarbageCollectedRaces;
    
    /**
     * To reliably stop the thread we need a specific reference getting enqueued that we can recognize. Therefore,
     * we create a dummy tracked race here and release the reference to it as soon as the {@link #stop} method is called.
     * When this reference is later enqueued, the thread will terminate.
     */
    private TrackedRace dummyTrackedRace = new DummyTrackedRace("Dummy for QuickRanksLiveCache stopping", /* raceId */
            "Dummy for QuickRanksLiveCache stopping");
    
    private final WeakReference<? extends TrackedRace> stopRef = new WeakReference<TrackedRace>(dummyTrackedRace);
    
    private final SmartFutureCache<RegattaAndRaceIdentifier, List<QuickRankDTO>, CalculateOrPurge> cache;
    
    private final SailingServiceImpl service;
    
    private static class CalculateOrPurge implements UpdateInterval<CalculateOrPurge> {
        private static final CalculateOrPurge CALCULATE = new CalculateOrPurge();
        private static final CalculateOrPurge PURGE = new CalculateOrPurge();
        
        @Override
        public CalculateOrPurge join(CalculateOrPurge otherUpdateInterval) {
            final CalculateOrPurge result;
            if (this == PURGE || otherUpdateInterval == PURGE) {
                result = PURGE;
            } else {
                result = CALCULATE;
            }
            return result;
        }
    }
    
    public QuickRanksLiveCache(final SailingServiceImpl service) {
        this.service = service;
        cache = new SmartFutureCache<RegattaAndRaceIdentifier, List<QuickRankDTO>, CalculateOrPurge>(
                new AbstractCacheUpdater<RegattaAndRaceIdentifier, List<QuickRankDTO>, CalculateOrPurge>() {
                    @Override
                    public List<QuickRankDTO> computeCacheUpdate(RegattaAndRaceIdentifier key,
                            CalculateOrPurge updateInterval) throws Exception {
                        final List<QuickRankDTO> result;
                        if (updateInterval == CalculateOrPurge.PURGE) {
                            result = null;
                        } else {
                            result = service.computeQuickRanks(key, /* time point; null means live */ null);
                        }
                        return result;
                    }
                }, getClass().getName());
        fromRefToRaceIdentifier = new HashMap<>();
        new Thread("QuickRanksLiveCache garbage collector") {
            @Override
            public void run() {
                Reference<?> ref;
                do {
                    try {
                        ref = referencesToGarbageCollectedRaces.remove();
                        if (ref != stopRef) {
                            RegattaAndRaceIdentifier raceIdentifier = fromRefToRaceIdentifier.get(ref);
                            remove(raceIdentifier);
                        }
                    } catch (InterruptedException e) {
                        logger.log(Level.INFO, "Interrupted while waiting for reference in reference queue; quitting", e);
                        break;
                    }
                } while (ref != stopRef);
                logger.info("Received stop in QuickRanksLiveCache garbage collector; terminating");
            }
        }.start();
        referencesToGarbageCollectedRaces = new ReferenceQueue<>();
    }

    private void remove(RegattaAndRaceIdentifier raceIdentifier) {
        cache.remove(raceIdentifier);
    }

    public void stop() {
        dummyTrackedRace = null; // release the dummy tracked race, causing the stopRef to be enqueued
    }

    public List<QuickRankDTO> get(RegattaAndRaceIdentifier raceIdentifier) {
        List<QuickRankDTO> result = cache.get(raceIdentifier, false);
        if (result == null) {
            TrackedRace trackedRace = service.getExistingTrackedRace(raceIdentifier);
            if (trackedRace != null) {
                trackedRace.addListener(new Listener(raceIdentifier)); // register for all changes that may affect the quick ranks
                cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
            }
        }
        result = cache.get(raceIdentifier, /* wait for latest result */ true);
        return result;
    }

    private class Listener implements RaceChangeListener {
        private final RegattaAndRaceIdentifier raceIdentifier;

        public Listener(RegattaAndRaceIdentifier raceIdentifier) {
            this.raceIdentifier = raceIdentifier;
        }

        @Override
        public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void competitorPositionChanged(GPSFixMoving fix, Competitor competitor) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void markPositionChanged(GPSFix fix, Mark mark, boolean firstInTrack) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings,
                Iterable<MarkPassing> markPassings) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void windDataReceived(Wind wind, WindSource windSource) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void windDataRemoved(Wind wind, WindSource windSource) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void raceTimesChanged(TimePoint startOfTracking, TimePoint endOfTracking, TimePoint startTimeReceived) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void delayToLiveChanged(long delayToLiveInMillis) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void windSourcesToExcludeChanged(Iterable<? extends WindSource> windSourcesToExclude) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }

        @Override
        public void statusChanged(TrackedRaceStatus newStatus) {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }
    }
    
}
