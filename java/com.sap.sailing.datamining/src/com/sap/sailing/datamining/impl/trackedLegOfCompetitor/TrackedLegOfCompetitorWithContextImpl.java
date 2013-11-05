package com.sap.sailing.datamining.impl.trackedLegOfCompetitor;

import com.sap.sailing.datamining.WindStrengthCluster;
import com.sap.sailing.datamining.data.TrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.data.TrackedLegOfCompetitorWithContext;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;

public class TrackedLegOfCompetitorWithContextImpl implements TrackedLegOfCompetitorWithContext {

    private TrackedLegOfCompetitor trackedLegOfCompetitor;
    private TrackedLegOfCompetitorContext context;

    public TrackedLegOfCompetitorWithContextImpl(TrackedLegOfCompetitor trackedLegOfCompetitor, TrackedLegOfCompetitorContext context) {
        this.trackedLegOfCompetitor = trackedLegOfCompetitor;
        this.context = context;
    }

    @Override
    public String getRegattaName() {
        return context.getTrackedRace().getTrackedRegatta().getRegatta().getName();
    }

    @Override
    public String getRaceName() {
        return context.getTrackedRace().getRace().getName();
    }

    @Override
    public int getLegNumber() {
        return context.getLegNumber();
    }

    @Override
    public String getCourseAreaName() {
        return context.getCourseArea().getName();
    }

    @Override
    public String getFleetName() {
        return context.getFleet().getName();
    }

    @Override
    public String getBoatClassName() {
        return context.getCompetitor().getBoat().getBoatClass().getName();
    }

    @Override
    public Integer getYear() {
        return context.getYear();
    }

    @Override
    public LegType getLegType() {
        return context.getLegType();
    }

    @Override
    public String getCompetitorName() {
        return context.getCompetitor().getName();
    }

    @Override
    public String getCompetitorSailID() {
        return context.getCompetitor().getBoat().getSailID();
    }

    @Override
    public String getCompetitorNationality() {
        return context.getCompetitor().getTeam().getNationality().getThreeLetterIOCAcronym();
    }

    @Override
    public WindStrengthCluster getWindStrength() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Distance getDistanceTraveled() {
        return trackedLegOfCompetitor.getDistanceTraveled(trackedLegOfCompetitor.getFinishTime());
    }

}
