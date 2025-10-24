package com.sap.sailing.gwt.home.communication.regatta;

import com.sap.sse.gwt.shared.DTO;

public class RegattaProgressFleetDTO implements DTO {
    private int raceCount;
    private int finishedRaceCount;
    private int liveRaceCount;
    
    @SuppressWarnings("unused")
    private RegattaProgressFleetDTO() {
    }
    
    public RegattaProgressFleetDTO( int raceCount, int finishedRaceCount, int liveRaceCount) {
        this.raceCount = raceCount;
        this.finishedRaceCount = finishedRaceCount;
        this.liveRaceCount = liveRaceCount;
    }

    public int getRaceCount() {
        return raceCount;
    }
    
    public int getFinishedRaceCount() {
        return finishedRaceCount;
    }
    
    public int getLiveRaceCount() {
        return liveRaceCount;
    }
    
    public int getFinishedAndLiveRaceCount() {
        return finishedRaceCount + liveRaceCount;
    }
}
