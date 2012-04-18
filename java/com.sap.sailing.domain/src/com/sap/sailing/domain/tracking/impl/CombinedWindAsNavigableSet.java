package com.sap.sailing.domain.tracking.impl;

import java.util.NavigableSet;

import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindTrack;

/**
 * Delivers what {@link TrackedRace#getWind(Position, TimePoint)} delivers, as a navigable set.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CombinedWindAsNavigableSet extends VirtualWindFixesAsNavigableSet {

    public CombinedWindAsNavigableSet(WindTrack track, TrackedRace trackedRace, long resolutionInMilliseconds) {
        super(track, trackedRace, resolutionInMilliseconds);
    }
    
    public CombinedWindAsNavigableSet(WindTrack track, TrackedRace trackedRace,
            TimePoint from, TimePoint to, long resolutionInMilliseconds) {
        super(track, trackedRace, from, to, resolutionInMilliseconds);
    }
    
    @Override
    protected Wind getWind(Position p, TimePoint timePoint) {
        return getTrackedRace().getWind(p, timePoint);
    }

    @Override
    protected NavigableSet<Wind> createSubset(WindTrack track, TrackedRace trackedRace, TimePoint from, TimePoint to) {
        return new CombinedWindAsNavigableSet(track, trackedRace, from, to, getResolutionInMilliseconds());
    }

    /**
     * Time point up to and including which the GPS fixes are considered in the race's tracks. Returns the value of
     * {@link #to} unless it is <code>null</code>. In this case, the time point of the
     * {@link TrackedRace#getAssumedEnd() assumed end of race},
     * {@link #ceilingToResolution(Wind) ceiled to the resolution of this set} will be returned instead. If no valid
     * time of a newest event can be obtained from the race, <code>MillisecondsTimePoint(1)</code> is returned instead.
     */
    protected TimePoint getTo() {
        return getToInternal() == null ? getTrackedRace().getAssumedEnd() == null ? new MillisecondsTimePoint(1)
                : ceilingToResolution(getTrackedRace().getAssumedEnd()) : getToInternal();
    }

}
