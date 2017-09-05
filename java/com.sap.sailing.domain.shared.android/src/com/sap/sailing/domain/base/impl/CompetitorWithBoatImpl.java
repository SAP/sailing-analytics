package com.sap.sailing.domain.base.impl;

import java.io.Serializable;
import java.net.URI;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;

public class CompetitorWithBoatImpl extends CompetitorImpl implements DynamicCompetitorWithBoat {
    private static final long serialVersionUID = 22679449208503264L;
    private DynamicBoat boat;
    
    public CompetitorWithBoatImpl(Serializable id, String name, String shortName, Color color, String email,
            URI flagImage, DynamicTeam team, Double timeOnTimeFactor, Duration timeOnDistanceAllowancePerNauticalMile,
            String searchTag, DynamicBoat boat) {
        super(id, name, shortName, color, email, flagImage, team, timeOnTimeFactor, timeOnDistanceAllowancePerNauticalMile,
                searchTag);
        this.boat = boat;
    }

    public CompetitorWithBoatImpl(Competitor competitor, DynamicBoat boat) {
        this(competitor.getId(), competitor.getName(), competitor.getShortName(), competitor.getColor(), competitor.getEmail(), 
                competitor.getFlagImage(), (DynamicTeam) competitor.getTeam(), competitor.getTimeOnTimeFactor(),
                competitor.getTimeOnDistanceAllowancePerNauticalMile(), competitor.getSearchTag(), boat);
    }

    @Override
    public DynamicBoat getBoat() {
        return boat;
    }

    @Override
    public void setBoat(DynamicBoat boat) {
        this.boat = boat;
    }
}
