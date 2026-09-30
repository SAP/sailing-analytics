package com.sap.sailing.domain.tracking.impl;

import java.util.NavigableSet;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl.DummyWind;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Delivers what {@link TrackedRace#getWind(Position, TimePoint)} delivers, as a navigable set.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CombinedWindAsNavigableSet extends VirtualWindFixesAsNavigableSet {

    private static final long serialVersionUID = -153959652212518644L;

    public CombinedWindAsNavigableSet(CombinedWindTrackImpl track, TrackedRace trackedRace, long resolutionInMilliseconds) {
        super(track, trackedRace, resolutionInMilliseconds);
    }
    
    public CombinedWindAsNavigableSet(WindTrack track, TrackedRace trackedRace,
            TimePoint from, TimePoint to, long resolutionInMilliseconds) {
        super(track, trackedRace, from, to, resolutionInMilliseconds);
    }
    
    @Override
    protected CombinedWindTrackImpl getTrack() {
        return (CombinedWindTrackImpl) super.getTrack();
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
     * {@link TrackedRace#getEndOfRace() assumed end of race}, {@link #ceilingToResolution(Wind) ceiled to the
     * resolution of this set} will be considered instead. If no assumed end of race can be determined either, the time
     * point of the last event received so far is used. If no valid time of a newest event can be obtained from the
     * race, <code>MillisecondsTimePoint(1)</code> is returned instead.
     */
    protected TimePoint getTo() {
        return getToInternal() == null ? getTrackedRace().getEndOfRace() == null ? getTrackedRace().getTimePointOfLastEvent() == null ?
                new MillisecondsTimePoint(1) : getTrackedRace().getTimePointOfLastEvent()
                : ceilingToResolution(getTrackedRace().getEndOfRace()) : getToInternal();
    }

    /**
     * Uses the {@link TrackedRace#getCenterOfCourse(TimePoint) center of the course} as the position for which to
     * compute the combined wind
     */
    @Override
    protected DummyWind createDummyWindFix(TimePoint timePoint) {
        return new DummyWind(timePoint, getTrack().getDefaultPosition(timePoint));
    }

}
