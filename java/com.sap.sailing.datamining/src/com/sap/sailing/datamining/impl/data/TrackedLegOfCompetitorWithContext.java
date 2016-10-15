package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;

public class TrackedLegOfCompetitorWithContext implements HasTrackedLegOfCompetitorContext {

    private final HasTrackedLegContext trackedLegContext;
    
    private final TrackedLegOfCompetitor trackedLegOfCompetitor;
    private final Competitor competitor;

    public TrackedLegOfCompetitorWithContext(HasTrackedLegContext trackedLegContext, TrackedLegOfCompetitor trackedLegOfCompetitor) {
        this.trackedLegContext = trackedLegContext;
        this.trackedLegOfCompetitor = trackedLegOfCompetitor;
        this.competitor = trackedLegOfCompetitor.getCompetitor();
    }
    
    @Override
    public HasTrackedLegContext getTrackedLegContext() {
        return trackedLegContext;
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLegOfCompetitor() {
        return trackedLegOfCompetitor;
    }

    @Override
    public Competitor getCompetitor() {
        return competitor;
    }

    @Override
    public String getCompetitorSearchTag() {
        return getCompetitor().getSearchTag();
    }
    
    @Override
    public Distance getDistanceTraveled() {
        TimePoint timePoint = getTrackedLegContext().getTrackedRaceContext().getTrackedRace().getEndOfTracking();
        return getTrackedLegOfCompetitor().getDistanceTraveled(timePoint);
    }
    
    @Override
    public Double getRankGainsOrLosses() {
        TrackedRace trackedRace = getTrackedLegContext().getTrackedRaceContext().getTrackedRace();
        Double rankAtStart = Double.valueOf(trackedRace.getRank(getCompetitor(), getTrackedLegOfCompetitor().getStartTime()));
        Double rankAtFinish = Double.valueOf(trackedRace.getRank(getCompetitor(), getTrackedLegOfCompetitor().getFinishTime()));
        return rankAtStart - rankAtFinish;
    }

}
