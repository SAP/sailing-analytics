package com.sap.sailing.gwt.home.communication.eventview;

import com.sap.sse.gwt.shared.DTO;

public class RegattaReferenceDTO implements DTO, Comparable<RegattaReferenceDTO> {
    private String id;
    private String displayName;
    
    public RegattaReferenceDTO() {
    }
    
    public RegattaReferenceDTO(String leaderboardId, String name) {
        super();
        this.id = leaderboardId;
        this.displayName = name;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int compareTo(RegattaReferenceDTO o) {
        int compareByDisplayName = displayName.compareTo(o.displayName);
        if (compareByDisplayName != 0) {
            return compareByDisplayName;
        }
        return id.compareTo(o.id);
    }
}
