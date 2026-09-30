package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

/**
 * Some calculations that span several method calls require access to the same {@link Mark} positions for the same
 * {@link TimePoint} within the scope of the same {@link TrackedRace} repeatedly. Usually, for such a compound
 * calculation it doesn't matter whether a new fix has arrived in between, making it valid to cache the mark
 * positions---once calculated---for the remainder of the compound operation. Based on these positions, approximate
 * waypoint positions and leg bearings can also be calculated and cached.
 * <p>
 * 
 * Implementations of this interface may cache the results of computing a mark's position for the duration of this
 * object.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface MarkPositionAtTimePointCache {
    /**
     * Gets the non-extrapolated mark position in this cache's tracked race at the time point
     * specified.
     * 
     * @see TrackedRace#getOrCreateTrack(Mark)
     * @see GPSFixTrack#getEstimatedPosition(TimePoint, boolean)
     */
    Position getEstimatedPosition(Mark mark);
    
    /**
     * Gets the approximate position of a waypoint in the scope of this cache's tracked race at
     * the time point specified.
     * 
     * @see TrackedRace#getApproximatePosition(Waypoint, TimePoint)
     */
    Position getApproximatePosition(Waypoint waypoint);
    
    /**
     * Gets the bearing of the tracked leg at the time point specified, optionally based on mark position
     * information already stored in this cache.
     * 
     * @see TrackedLeg#getLegBearing(TimePoint)
     */
    Bearing getLegBearing(TrackedLeg trackedLeg);

    /**
     * @return the time point for which this cache holds values
     */
    TimePoint getTimePoint();
    
    /**
     * @return the tracked race for which this cache holds values
     */
    TrackedRace getTrackedRace();
}
