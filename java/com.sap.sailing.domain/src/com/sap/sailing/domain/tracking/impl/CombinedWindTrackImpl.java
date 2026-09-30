package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.TrackBasedEstimationWindTrackImpl.EstimatedWindFixesAsNavigableSet;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * A wind track that delivers the result of
 * {@link TrackedRace#getWind(com.sap.sse.common.Position, com.sap.sse.common.TimePoint)},
 * using a {@link CombinedWindAsNavigableSet} as its internal raw fixes collection. Resolution of raw fixes is set to 10s.
 * As the result of {@link TrackedRace#getWind(com.sap.sse.common.Position, com.sap.sse.common.TimePoint)}
 * is already a smoothened, outlier-removed and averaged value, the averaging methods {@link #getAveragedWind(Position, TimePoint)}
 * and {@link #getAveragedWindWithConfidence(Position, TimePoint)} are redefined such that they immediately delegate
 * to {@link TrackedRace#getWindWithConfidence(Position, TimePoint)}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class CombinedWindTrackImpl extends VirtualWindTrackImpl {
    private static final long serialVersionUID = -5019721956924343447L;
    private CombinedWindAsNavigableSet virtualInternalRawFixes;
    
    public CombinedWindTrackImpl(TrackedRace trackedRace, double baseConfidence) {
        super(trackedRace, /* millisecondsOverWhichToAverage not used, see overridden method */ -1, baseConfidence,
                /* useSpeed */ WindSourceType.COMBINED.useSpeed());
        virtualInternalRawFixes = createVirtualInternalRawFixes(trackedRace);
    }

    protected CombinedWindAsNavigableSet createVirtualInternalRawFixes(TrackedRace trackedRace) {
        return new CombinedWindAsNavigableSet(this, trackedRace, /* resolutionInMilliseconds */ 1000l);
    }

    @Override
    protected CombinedWindAsNavigableSet getInternalRawFixes() {
        return virtualInternalRawFixes;
    }
    
    /**
     * Delivers the position to use when a caller of {@link #getAveragedWind(Position, TimePoint)} or
     * {@link #getAveragedWindWithConfidence(Position, TimePoint)} provides a {@code null} position. This position is
     * also used for the fixes produced by the fix iterators for this track. This implementation returns the
     * {@link TrackedRace#getCenterOfCourse(TimePoint) center of the course}.
     */
    protected Position getDefaultPosition(TimePoint at) {
        return getTrackedRace().getCenterOfCourse(at);
    }

    /**
     * The combined wind source already averages the wind from each wind source considered. There is no use in again
     * averaging over a longer period of time. Therefore, we set the averaging interval to two times the resolution of
     * the {@link EstimatedWindFixesAsNavigableSet virtual fixes collection} plus two milliseconds, so that at most one
     * fix before and one fix after the time point requested will be used.
     */
    @Override
    public long getMillisecondsOverWhichToAverageWind() {
        return 2*getInternalRawFixes().getResolutionInMilliseconds()+2;
    }

    @Override
    public Wind getAveragedWind(Position p, TimePoint at) {
        return getTrackedRace().getWind(p==null?getDefaultPosition(at):p, at);
    }

    @Override
    public WindWithConfidence<Util.Pair<Position, TimePoint>> getAveragedWindWithConfidence(Position p, TimePoint at) {
        return getTrackedRace().getWindWithConfidence(p==null?getDefaultPosition(at):p, at);
    }
}
