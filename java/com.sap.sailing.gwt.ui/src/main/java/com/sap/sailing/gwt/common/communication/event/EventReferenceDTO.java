package com.sap.sailing.gwt.common.communication.event;

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.base.EventBase;

public class EventReferenceDTO implements IsSerializable {
    
    private UUID id;
    private String displayName;
    
    public EventReferenceDTO() {
    }
    
    @GwtIncompatible
    public EventReferenceDTO(EventBase event) {
        this.id = (UUID) event.getId();
        this.displayName = event.getName();
    }

    public EventReferenceDTO(UUID id, String displayName) {
        super();
        this.id = id;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String name) {
        this.displayName = name;
    }
}
