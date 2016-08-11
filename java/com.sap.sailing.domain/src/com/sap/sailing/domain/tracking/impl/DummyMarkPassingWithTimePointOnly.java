package com.sap.sailing.domain.tracking.impl;

import java.net.URI;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorChangeListener;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Team;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.IsManagedByCache;
import com.sap.sse.common.TimePoint;

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
            public IsManagedByCache<SharedDomainFactory> resolve(SharedDomainFactory domainFactory) {
                return this;
            }

            @Override
            public Color getColor() {
                return null;
            }

            @Override
            public void addCompetitorChangeListener(CompetitorChangeListener listener) {
            }

            @Override
            public void removeCompetitorChangeListener(CompetitorChangeListener listener) {
            }

            @Override
            public String getEmail() {
                return null;
            }

            @Override
            public boolean hasEmail() {
                return false;
            }

            @Override
            public URI getFlagImage() {
                return null;
            }

            @Override
            public Double getTimeOnTimeFactor() {
                return null;
            }

            @Override
            public Duration getTimeOnDistanceAllowancePerNauticalMile() {
                return null;
            }

            @Override
            public String getSearchTag() {
                return null;
            }
        };
    }

    @Override
    public MarkPassing getOriginal() {
        return this;
    }
}
