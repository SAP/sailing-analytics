package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class NonCachingMarkPositionAtTimePointCache implements MarkPositionAtTimePointCache {
    private final TrackedRace trackedRace;
    private final TimePoint timePoint;

    public NonCachingMarkPositionAtTimePointCache(TrackedRace trackedRace, TimePoint timePoint) {
        super();
        this.trackedRace = trackedRace;
        this.timePoint = timePoint;
    }

    @Override
    public Position getEstimatedPosition(Mark mark) {
        return trackedRace.getOrCreateTrack(mark).getEstimatedPosition(timePoint, /* extrapolate */ false);
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint) {
        return trackedRace.getApproximatePosition(waypoint, timePoint, this);
    }

    @Override
    public Bearing getLegBearing(TrackedLeg trackedLeg) {
        return trackedLeg.getLegBearing(timePoint, this);
    }

    @Override
    public TrackedRace getTrackedRace() {
        return trackedRace;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }
}
