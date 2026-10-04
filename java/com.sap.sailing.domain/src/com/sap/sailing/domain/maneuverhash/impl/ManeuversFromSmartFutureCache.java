package com.sap.sailing.domain.maneuverhash.impl;

import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetector;
import com.sap.sailing.domain.maneuverhash.ManeuverCache;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;

public class ManeuversFromSmartFutureCache implements ManeuverCache {
    private final SmartFutureCache<Competitor, List<Maneuver>, EmptyUpdateInterval> smartFutureCache;
    
    public ManeuversFromSmartFutureCache(DynamicTrackedRaceImpl race) {
        this.smartFutureCache = new SmartFutureCache<Competitor, List<Maneuver>, EmptyUpdateInterval>(
              new AbstractCacheUpdater<Competitor, List<Maneuver>, EmptyUpdateInterval>() {
                  @Override
                  public List<Maneuver> computeCacheUpdate(Competitor competitor, EmptyUpdateInterval updateInterval)
                          throws NoWindException {
                      return race.getTrackedRegatta().callWithCPUMeterWithException(()->{
                          Duration averageIntervalBetweenRawFixes = race.getTrack(competitor).getAverageIntervalBetweenRawFixes();
                          if (averageIntervalBetweenRawFixes != null) {
                              ManeuverDetector maneuverDetector;
                              // FIXME The LowGPSSamplingRateManeuverDetectorImpl doesn't work very well; it recognizes many tacks only as bear-away and doesn't seem to have any noticeable benefits... See ORC Worlds 2019 ORC A Long Offshore
  //                            if (averageIntervalBetweenRawFixes.asSeconds() >= 30) {
  //                                maneuverDetector = new LowGPSSamplingRateManeuverDetectorImpl(TrackedRaceImpl.this, competitor);
  //                            } else {
                                  maneuverDetector = race.getManeuverDetectorPerCompetitorCache().getValue(competitor);
  //                            }
                              List<Maneuver> maneuvers = race.computeManeuvers(competitor, maneuverDetector);
                              return maneuvers;
                          } else {
                              return Collections.emptyList();
                          }
                      }, CPUMeteringType.MANEUVER_DETECTION.name());
                  }
              },//.computeCacheUpdate(competitor, null), 
              /* nameForLocks */ "Maneuver cache for race " + race.getRace().getName());
    }   
    
    @Override
    public boolean canBeUpdated() {
        return true;
    }

    @Override
    public void resume() {
        smartFutureCache.resume();
    }

    @Override
    public List<Maneuver> get(Competitor key, boolean waitForLatest) {
        return smartFutureCache.get(key, waitForLatest);
    }

    @Override
    public void suspend() {
        smartFutureCache.suspend();
    }

    @Override
    public void recalculate(Competitor competitor) {
        // need to trigger an update
        triggerUpdate(competitor);
    }

    @Override
    public void triggerUpdate(Competitor key) {
        smartFutureCache.triggerUpdate(key, /* updateInterval */ null);
    }
}
