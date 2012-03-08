package com.sap.sailing.domain.tracking.impl;

import java.util.NavigableSet;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindWithConfidence;

/**
 * A virtual wind track based on some virtual sequence of raw wind fixes. Subclasses should override
 * {@link WindTrackImpl#getInternalRawFixes()} so that it returns a {@link VirtualWindFixesAsNavigableSet}.
 * The base class's {@link WindTrackImpl#getInternalRawFixes()} may then be used as a cache, if needed.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public abstract class VirtualWindTrackImpl extends WindTrackImpl {
    private final TrackedRace trackedRace;
    
    protected VirtualWindTrackImpl(TrackedRace trackedRace, long millisecondsOverWhichToAverage) {
        super(millisecondsOverWhichToAverage);
        this.trackedRace = trackedRace;
    }
    
    protected TrackedRace getTrackedRace() {
        return trackedRace;
    }
    
    @Override
    protected NavigableSet<Wind> getInternalFixes() {
        return new PartialNavigableSetView<Wind>(getInternalRawFixes()) {
            @Override
            protected boolean isValid(Wind e) {
                return e != null;
            }
        };
    }

    /**
     * This redefinition avoids very long searches in case <code>at</code> is before the race start or after the race's
     * newest event. Should <code>at</code> be out of this range, it is set to the closest border of this range before
     * calling the base class's implementation. If either race start or time of newest event are not known, the known
     * time point is used instead. If both time points are not known, <code>null</code> is returned immediately.
     */
    @Override
    public Wind getAveragedWind(Position p, TimePoint at) {
        Wind result = null;
        TimePoint adjustedAt;
        TimePoint raceStartTimePoint = trackedRace.getStart();
        TimePoint timePointOfNewestEvent = trackedRace.getTimePointOfNewestEvent();
        if (raceStartTimePoint != null) {
            if (timePointOfNewestEvent != null) {
                if (at.compareTo(raceStartTimePoint) < 0) {
                    adjustedAt = raceStartTimePoint;
                } else if (at.compareTo(timePointOfNewestEvent) > 0) {
                    adjustedAt = timePointOfNewestEvent;
                } else {
                    adjustedAt = at;
                }
            } else {
                adjustedAt = raceStartTimePoint;
            }
        } else {
            if (timePointOfNewestEvent != null) {
                adjustedAt = timePointOfNewestEvent;
            } else {
                adjustedAt = null;
            }
        }
        if (adjustedAt != null) {
            // we can use the unsynchronized version here because our getInternalFixes() method operates
            // only on a virtual sequence of wind fixes where no concurrency issues have to be observed
            final WindWithConfidence<Pair<Position, TimePoint>> estimatedWindUnsynchronized = getAveragedWindUnsynchronized(p, adjustedAt);
            result = estimatedWindUnsynchronized == null ? null : estimatedWindUnsynchronized.getObject();
        }
        return result;
    }

}
