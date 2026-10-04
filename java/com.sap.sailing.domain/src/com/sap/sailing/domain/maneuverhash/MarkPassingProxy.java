package com.sap.sailing.domain.maneuverhash;

import java.io.Serializable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class MarkPassingProxy implements MarkPassing {
    
    private static final long serialVersionUID = -1038446741597082803L;
    private final int waypointIndex;
    private final Serializable competitorId;
    private final TrackedRace trackedRace;
    private MarkPassing markPassing;
    private final TimePoint timePoint;
    
    public MarkPassingProxy(TimePoint timePoint, int waypointIndex, Serializable  competitorId, TrackedRace trackedRace) {
        super();
        this.timePoint = timePoint;
        this.waypointIndex = waypointIndex;
        this.competitorId = competitorId;
        this.trackedRace = trackedRace;
    }
    
    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    @Override
    public Waypoint getWaypoint() {
       Iterable<Waypoint>  waypoints = trackedRace.getRace().getCourse().getWaypoints();
       return Util.get(waypoints, waypointIndex);
    }

    @Override
    public Competitor getCompetitor() {
        return trackedRace.getRace().getCompetitorById(competitorId);
    }

    @Override
    public MarkPassing getOriginal() {
        isMarkPassing();
        return markPassing == null ? null : markPassing.getOriginal();
    }

    private void isMarkPassing() {
        if (markPassing == null) {
            markPassing = trackedRace.getMarkPassing(getCompetitor(), getWaypoint()); 
        }
    }
}
