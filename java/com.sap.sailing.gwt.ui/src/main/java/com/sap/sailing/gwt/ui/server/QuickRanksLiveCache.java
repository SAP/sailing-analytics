package com.sap.sailing.gwt.ui.server;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.tracking.DummyTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.gwt.ui.shared.QuickRankDTO;
import com.sap.sailing.gwt.ui.shared.QuickRanksDTO;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sse.util.SmartFutureCache.UpdateInterval;

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
public class QuickRanksLiveCache {
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
    
    private final SmartFutureCache<RegattaAndRaceIdentifier, QuickRanksDTO, CalculateOrPurge> cache;
    
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
        cache = new SmartFutureCache<RegattaAndRaceIdentifier, QuickRanksDTO, CalculateOrPurge>(
                new AbstractCacheUpdater<RegattaAndRaceIdentifier, QuickRanksDTO, CalculateOrPurge>() {
                    @Override
                    public QuickRanksDTO computeCacheUpdate(RegattaAndRaceIdentifier key,
                            CalculateOrPurge updateInterval) throws Exception {
                        final TrackedRegatta cpuMeter = service.getTrackedRace(key).getTrackedRegatta();
                        return cpuMeter.callWithCPUMeterWithException(()->{
                            logger.fine("Computing cache update for live QuickRanks of race "+key);
                            final QuickRanksDTO quickRanks;
                            if (updateInterval == CalculateOrPurge.PURGE) {
                                quickRanks = null;
                            } else {
                                quickRanks = service.computeQuickRanks(key, /* time point; null means live */ null);
                            }
                            return quickRanks;
                        }, CPUMeteringType.QUICK_RANKS.name());
                    }
                }, getClass().getName());
        fromRefToRaceIdentifier = new HashMap<>();
        referencesToGarbageCollectedRaces = new ReferenceQueue<>();
        Thread t = new Thread("QuickRanksLiveCache garbage collector") {
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
        };
        t.setDaemon(true);
        t.start();
    }

    private void remove(RegattaAndRaceIdentifier raceIdentifier) {
        cache.remove(raceIdentifier);
    }

    public void stop() {
        dummyTrackedRace = null; // release the dummy tracked race, causing the stopRef to be enqueued
    }

    public QuickRanksDTO get(RegattaAndRaceIdentifier raceIdentifier) {
        // The following may throw an exception that was stored upon an earlier re-compute attempt;
        // see also the changes of bug6245, so we need to recompute if we read null or if
        // we get an exception
        QuickRanksDTO result = null;
        try {
            result = cache.get(raceIdentifier, false);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception while reading QuickRanksCache for race "+raceIdentifier
                    + "; re-computing");
        }
        if (result == null) {
            final TrackedRace trackedRace = service.getExistingTrackedRace(raceIdentifier);
            if (trackedRace != null) {
                trackedRace.addListener(new Listener(raceIdentifier)); // register for all changes that may affect the quick ranks
                cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
            }
            result = cache.get(raceIdentifier, /* wait for latest result */ true);
        }
        return result;
    }

    private class Listener extends AbstractRaceChangeListener {
        private final RegattaAndRaceIdentifier raceIdentifier;

        public Listener(RegattaAndRaceIdentifier raceIdentifier) {
            this.raceIdentifier = raceIdentifier;
        }
        
        @Override
        protected void defaultAction() {
            cache.triggerUpdate(raceIdentifier, CalculateOrPurge.CALCULATE);
        }
    }
    
}
