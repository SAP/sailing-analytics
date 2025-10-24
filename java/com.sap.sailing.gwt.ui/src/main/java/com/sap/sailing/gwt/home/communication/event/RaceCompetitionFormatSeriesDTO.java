package com.sap.sailing.gwt.home.communication.event;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sse.gwt.shared.DTO;

public class RaceCompetitionFormatSeriesDTO implements DTO {

    private String seriesName;
    private int flightCount = 0;
    private int raceCount = 0;
    private int competitorCount = 0;
    private ArrayList<RaceCompetitionFormatFleetDTO> fleets = new ArrayList<>();

    protected RaceCompetitionFormatSeriesDTO() {
    }

    public RaceCompetitionFormatSeriesDTO(String seriesName) {
        this.seriesName = seriesName;
    }
    
    public void addFleet(RaceCompetitionFormatFleetDTO fleet) {
        fleets.add(fleet);
        flightCount = Math.max(flightCount, fleet.getRaces().size());
        raceCount += fleet.getRaces().size();
    }
    
    public void setCompetitorCount(int competitorCount) {
        this.competitorCount = competitorCount;
    }

    public String getSeriesName() {
        return seriesName;
    }
    
    public int getFlightCount() {
        return flightCount;
    }

    public int getRaceCount() {
        return raceCount;
    }

    public int getCompetitorCount() {
        return competitorCount;
    }

    public Collection<RaceCompetitionFormatFleetDTO> getFleets() {
        return fleets;
    }

}
