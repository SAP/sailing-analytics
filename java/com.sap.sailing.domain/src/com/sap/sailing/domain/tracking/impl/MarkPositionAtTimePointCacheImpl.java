package com.sap.sailing.domain.tracking.impl;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class MarkPositionAtTimePointCacheImpl extends NonCachingMarkPositionAtTimePointCache {
    private final Map<Mark, Position> estimatedMarkPositions;
    private final Map<Waypoint, Position> approximateWaypointPositions;
    private final Map<TrackedLeg, Bearing> legBearings;
    
    public MarkPositionAtTimePointCacheImpl(TrackedRace trackedRace, TimePoint timePoint) {
        super(trackedRace, timePoint);
        estimatedMarkPositions = new HashMap<>();
        approximateWaypointPositions = new HashMap<>();
        legBearings = new HashMap<>();
    }

    @Override
    public Position getEstimatedPosition(Mark mark) {
        final Position result;
        if (estimatedMarkPositions.containsKey(mark)) {
            result = estimatedMarkPositions.get(mark);
        } else {
            result = super.getEstimatedPosition(mark);
            estimatedMarkPositions.put(mark, result);
        }
        return result;
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint) {
        final Position result;
        if (approximateWaypointPositions.containsKey(waypoint)) {
            result = approximateWaypointPositions.get(waypoint);
        } else {
            result = super.getApproximatePosition(waypoint);
            approximateWaypointPositions.put(waypoint, result);
        }
        return result;
    }

    @Override
    public Bearing getLegBearing(TrackedLeg trackedLeg) {
        final Bearing result;
        if (legBearings.containsKey(trackedLeg)) {
            result = legBearings.get(trackedLeg);
        } else {
            result = super.getLegBearing(trackedLeg);
            legBearings.put(trackedLeg, result);
        }
        return result;
    }

}
