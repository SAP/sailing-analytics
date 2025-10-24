package com.sap.sailing.datamining.data;

public interface HasSomethingOfCompetitorContextWithTrackedRaceContext extends HasSomethingOfCompetitorContext {
    HasTrackedRaceContext getTrackedRaceContext();
    
    @Override
    default HasLeaderboardGroupContext getLeaderboardGroupContext() {
        return getTrackedRaceContext().getLeaderboardContext().getLeaderboardGroupContext();
    }
}
