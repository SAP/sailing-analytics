package com.sap.sailing.domain.tracking.impl;

import java.util.NavigableSet;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.shared.tracking.impl.PartialNavigableSetView;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * A virtual wind track based on some virtual sequence of raw wind fixes. Subclasses should override
 * {@link WindTrackImpl#getInternalRawFixes()} so that it returns a {@link VirtualWindFixesAsNavigableSet}.
 * The base class's {@link WindTrackImpl#getInternalRawFixes()} may then be used as a cache, if needed.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public abstract class VirtualWindTrackImpl extends WindTrackImpl {
    private static final long serialVersionUID = 6317321456089655749L;
    private final TrackedRace trackedRace;
    
    protected VirtualWindTrackImpl(TrackedRace trackedRace, long millisecondsOverWhichToAverage, double baseConfidence, boolean useSpeed) {
        super(millisecondsOverWhichToAverage, baseConfidence, useSpeed,
                /* nameForReadWriteLock */ VirtualWindTrackImpl.class.getSimpleName()+" for race "+trackedRace.getRace().getName());
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
     * Delegates to {@link #getAveragedWindUnsynchronized(Position, TimePoint)}
     */
    @Override
    public Wind getAveragedWind(Position p, TimePoint at) {
        final WindWithConfidence<Util.Pair<Position, TimePoint>> windWithConfidence = getAveragedWindUnsynchronized(p, at);
        return windWithConfidence == null ? null : windWithConfidence.getObject();
    }
    
    /**
     * This redefinition avoids very long searches in case <code>at</code> is before the race start or after the race's
     * newest event. Should <code>at</code> be out of this range, it is set to the closest border of this range before
     * calling the base class's implementation. If either race start or time of newest event are not known, the known
     * time point is used instead. If both time points are not known, <code>null</code> is returned immediately.
     */
    @Override
    public WindWithConfidence<Util.Pair<Position, TimePoint>> getAveragedWindWithConfidence(Position p, TimePoint at) {
        WindWithConfidence<Util.Pair<Position, TimePoint>> result = null;
        TimePoint adjustedAt;
        final TimePoint startOfRace = getTrackedRace().getStartOfRace();
        TimePoint timePointOfNewestEvent = getTrackedRace().getTimePointOfNewestEvent();
        if (startOfRace != null) {
            TimePoint fourMinutesBeforeRaceStartTimePoint = startOfRace.minus(TrackedRaceImpl.TIME_BEFORE_START_TO_TRACK_WIND_MILLIS); // let wind tracks start four minutes before start
            if (timePointOfNewestEvent != null) {
                if (at.compareTo(fourMinutesBeforeRaceStartTimePoint) < 0) {
                    adjustedAt = fourMinutesBeforeRaceStartTimePoint;
                } else if (at.compareTo(timePointOfNewestEvent) > 0) {
                    adjustedAt = timePointOfNewestEvent;
                } else {
                    adjustedAt = at;
                }
            } else {
                adjustedAt = fourMinutesBeforeRaceStartTimePoint;
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
            result = getAveragedWindUnsynchronized(p, adjustedAt);
        }
        return result;
    }

}
