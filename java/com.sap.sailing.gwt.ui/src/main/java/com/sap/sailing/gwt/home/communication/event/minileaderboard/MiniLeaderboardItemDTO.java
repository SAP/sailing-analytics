package com.sap.sailing.gwt.home.communication.event.minileaderboard;

import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sse.gwt.shared.DTO;

public class MiniLeaderboardItemDTO implements DTO {
    private SimpleCompetitorDTO competitor;
    private int rank;
    private Double netPoints;
    private int raceCount;
    
    @SuppressWarnings("unused")
    private MiniLeaderboardItemDTO() {
    }
    
    public MiniLeaderboardItemDTO(SimpleCompetitorDTO competitor, int rank, Double netPoints, int raceCount) {
        super();
        this.competitor = competitor;
        this.rank = rank;
        this.netPoints = netPoints;
        this.raceCount = raceCount;
    }
    
    public SimpleCompetitorDTO getCompetitor() {
        return competitor;
    }
    
    public Double getNetPoints() {
        return netPoints;
    }
    
    public int getRank() {
        return rank;
    }
    
    public int getRaceCount() {
        return raceCount;
    }
}