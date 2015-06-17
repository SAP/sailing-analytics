package com.sap.sailing.gwt.ui.shared.eventview;

import com.google.gwt.user.client.rpc.IsSerializable;

public class RegattaReferenceDTO implements IsSerializable, Comparable<RegattaReferenceDTO> {
    private String id;
    private String displayName;
    
    public RegattaReferenceDTO() {
    }
    
    public RegattaReferenceDTO(String id, String name) {
        super();
        this.id = id;
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
        return displayName.compareTo(o.displayName);
    }
}
