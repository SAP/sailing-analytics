package com.sap.sailing.gwt.home.server;

import java.util.Arrays;
import java.util.Collections;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO.RaceViewState;
import com.sap.sailing.gwt.home.server.EventActionUtil.RaceCallback;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

/**
 * {@link RaceCallback} implementation, which calculates a "time to live" for the given {@link RaceContext} based on its
 * {@link RaceContext#getLiveRaceViewState() view state}. The TTL for this {@link RaceRefreshCalculator} instance will
 * potentially be recalculated, when {@link #doForRace(RaceContext)} is called repeatedly. The final {@link #getTTL()
 * TTL} can be used for as refresh interval of "Live races" sections.
 */
@GwtIncompatible
public final class RaceRefreshCalculator implements RaceCallback {
    private Duration ttl = Duration.ONE_MINUTE.times(3);

    @Override
    public void doForRace(RaceContext rc) {
        RaceViewState state = rc.getLiveRaceViewState();
        if (state == RaceViewState.RUNNING) {
            ttl = Collections.min(Arrays.asList(ttl, Duration.ONE_SECOND.times(30)));
        } else if (state == RaceViewState.SCHEDULED) {
            final Duration timeTillRace = TimePoint.now().until(rc.getStartTime());
            ttl = Collections.min(Arrays.asList(ttl, Duration.ONE_MINUTE, timeTillRace));
        } else if (state == RaceViewState.POSTPONED || state == RaceViewState.ABANDONED) {
            ttl = Collections.min(Arrays.asList(ttl, Duration.ONE_MINUTE));
        }
    }

    /**
     * Returns the "time to live" calculated for the maybe repeated calls to {@link #doForRace(RaceContext)}.
     * 
     * @return the calculated {@link Duration time to live}
     */
    public Duration getTTL() {
        return ttl;
    }
}
