package com.sap.sailing.gwt.ui.shared.dispatch.event;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sap.sailing.gwt.ui.shared.dispatch.DTO;
import com.sap.sailing.gwt.ui.shared.eventview.RegattaMetadataDTO;

public class RegattasAndLiveRacesDTO implements DTO {
    
    private TreeMap<RegattaMetadataDTO, TreeSet<LiveRaceDTO>> regattasWithRaces;
    private TreeSet<RegattaMetadataDTO> regattasWithoutRaces;
    
    @SuppressWarnings("unused")
    private RegattasAndLiveRacesDTO() {
    }

    public RegattasAndLiveRacesDTO(TreeMap<RegattaMetadataDTO, TreeSet<LiveRaceDTO>> regattasWithRaces, TreeSet<RegattaMetadataDTO> regattasWithoutRaces) {
        super();
        this.regattasWithRaces = regattasWithRaces;
        this.regattasWithoutRaces = regattasWithoutRaces;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<RegattaMetadataDTO, Set<LiveRaceDTO>> getRegattasWithRaces() {
        return (Map)regattasWithRaces;
    }
    
    public Set<RegattaMetadataDTO> getRegattasWithoutRaces() {
        return regattasWithoutRaces;
    }
    
    public boolean hasRegattasWithRaces() {
        return !regattasWithRaces.isEmpty();
    }

    public boolean hasRegattasWithoutRaces() {
        return !regattasWithoutRaces.isEmpty();
    }
}
