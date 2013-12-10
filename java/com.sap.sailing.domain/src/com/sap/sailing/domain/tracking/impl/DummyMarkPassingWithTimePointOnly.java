package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.IsManagedBySharedDomainFactory;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Team;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.tracking.MarkPassing;

public class DummyMarkPassingWithTimePointOnly implements MarkPassing {
    private static final long serialVersionUID = -5494669910047887984L;
    private final TimePoint timePoint;
    
    public DummyMarkPassingWithTimePointOnly(TimePoint timePoint) {
        super();
        this.timePoint = timePoint;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    @Override
    public Waypoint getWaypoint() {
        throw new UnsupportedOperationException("getWaypoint() not supported");
    }

    @Override
    public Competitor getCompetitor() {
        return new Competitor() {
            private static final long serialVersionUID = 5663644650754031382L;

            @Override
            public String getName() {
                return "Dummy";
            }

            @Override
            public String getId() {
                return "Dummy";
            }

            @Override
            public Team getTeam() {
                return null;
            }

            @Override
            public Boat getBoat() {
                return null;
            }

            @Override
            public IsManagedBySharedDomainFactory resolve(SharedDomainFactory domainFactory) {
                return this;
            }

            @Override
            public Color getColor() {
                return null;
            }
        };
    }

}
