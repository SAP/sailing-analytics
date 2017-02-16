package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;

public interface HasLeaderboardContext {
    Leaderboard getLeaderboard();

    PolarDataService getPolarDataService();

    @Dimension(messageKey="Leaderboard", ordinal=1)
    String getName();
    
    @Connector(messageKey="BoatClass", ordinal=2)
    BoatClass getBoatClass();
}
