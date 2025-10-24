package com.sap.sailing.gwt.home.communication.event;

import java.util.UUID;

import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;

public class EventAndLeaderboardReferenceWithStateDTO extends EventReferenceDTO {
    
    // TODO replace with RegattaState
    private EventState state;
    private String leaderboardName;
    
    protected EventAndLeaderboardReferenceWithStateDTO() {
    }
    
    public EventAndLeaderboardReferenceWithStateDTO(UUID eventId, String leaderboardName, String displayName, EventState state) {
        super(eventId, displayName);
        this.leaderboardName = leaderboardName;
        this.state = state;
    }

    public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }

    public String getLeaderboardName() {
        return leaderboardName;
    }
}
