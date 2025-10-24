package com.sap.sailing.gwt.home.communication.regatta;

import java.util.Map;
import java.util.TreeMap;

import com.sap.sailing.gwt.home.communication.race.FleetMetadataDTO;
import com.sap.sse.gwt.shared.DTO;

public class RegattaProgressSeriesDTO implements DTO {
    private String name;
    private int totalRaceCount;
    private int maxRacesPerFleet;
    private TreeMap<FleetMetadataDTO, RegattaProgressFleetDTO> fleetState = new TreeMap<>();
    
    @SuppressWarnings("unused")
    private RegattaProgressSeriesDTO() {
    }
    
    public RegattaProgressSeriesDTO(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public int getTotalRaceCount() {
        return totalRaceCount;
    }
    
    public int getMaxRacesPerFleet() {
        return maxRacesPerFleet;
    }
    
    public Map<FleetMetadataDTO, RegattaProgressFleetDTO> getFleetState() {
        return fleetState;
    }
    
    public void addFleet(FleetMetadataDTO fleet, RegattaProgressFleetDTO stats) {
        fleetState.put(fleet, stats);
        totalRaceCount += stats.getRaceCount();
        maxRacesPerFleet =  Math.max(maxRacesPerFleet, stats.getRaceCount());
    }
    
    public String[] getFleetNames() {
        String[] fleetNames = new String[fleetState.size()];
        int i = 0;
        for (FleetMetadataDTO fleet : fleetState.keySet()) {
            fleetNames[i++] = fleet.getFleetName();
        }
        return fleetNames;
    }
    
    public boolean isCompleted() {
        for (RegattaProgressFleetDTO stats : fleetState.values()) {
            if (stats.getRaceCount() != stats.getFinishedRaceCount()) {
                return false;
            }
        }
        return !fleetState.isEmpty();
    }
    
    public int getProgressRaceCount() {
        int progressRaceCount = 0;
        for (RegattaProgressFleetDTO stats : fleetState.values()) {
            progressRaceCount += stats.getFinishedRaceCount();
        }
        return progressRaceCount;
    }
}
