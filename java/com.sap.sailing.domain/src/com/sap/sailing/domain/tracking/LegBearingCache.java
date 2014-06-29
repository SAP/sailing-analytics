package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.TimePoint;

/**
 * When comprehensive calculations are prone to query a leg's bearing several times for the same leg for
 * the same time point, a cache of this type can be used to eliminate repeated evaluation of common sub-expressions that
 * are expensive to calculate.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface LegBearingCache {
    Bearing getLegBearing(TrackedLeg trackedLeg, TimePoint timePoint);
}
