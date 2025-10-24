package com.sap.sailing.polars.datamining.data;

import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.data.HasSomethingOfCompetitorContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;

public interface HasCompetitorPolarContext extends HasSomethingOfCompetitorContext {
    
    @Connector(messageKey="Competitor")
    Competitor getCompetitor();
    
    
    @Override
    default HasLeaderboardGroupContext getLeaderboardGroupContext() {
        return getLegPolarContext().getFleetPolarContext().getRaceColumnPolarContext().getLeaderboardPolarContext().getLeaderboardGroupContext();
    }

    TrackedRace getTrackedRace();
    
    Leg getLeg();
    
    @Connector(scanForStatistics=false)
    HasLegPolarContext getLegPolarContext();
    
    @Dimension(messageKey = "Rank")
    int getRank();

}
