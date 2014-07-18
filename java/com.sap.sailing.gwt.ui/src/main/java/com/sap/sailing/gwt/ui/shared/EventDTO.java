package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventDTO extends EventBaseDTO {
    private static final long serialVersionUID = -7100030301376959817L;
    public List<RegattaDTO> regattas;

    private Date currentServerTime;
    
    private List<LeaderboardGroupDTO> leaderboardGroups; // keeps the more specific type accessible in a type-safe way
    
    public EventDTO() {
        this(new ArrayList<LeaderboardGroupDTO>());
    }
    
    private EventDTO(List<LeaderboardGroupDTO> leaderboardGroups) {
        super(leaderboardGroups);
        this.leaderboardGroups = leaderboardGroups;
        initCurrentServerTime();
    }

    public EventDTO(String name) {
        this(name, new ArrayList<LeaderboardGroupDTO>());
    }
    
    private EventDTO(String name, List<LeaderboardGroupDTO> leaderboardGroups) {
        super(name, leaderboardGroups);
        this.leaderboardGroups = leaderboardGroups;
        initCurrentServerTime();
        regattas = new ArrayList<RegattaDTO>();
    }
    
    private void initCurrentServerTime() {
        currentServerTime = new Date();
    }

    public Date getCurrentServerTime() {
        return currentServerTime;
    }
    
    public void addLeaderboardGroup(LeaderboardGroupDTO leaderboardGroup) {
        leaderboardGroups.add(leaderboardGroup);
    }
    
    public List<LeaderboardGroupDTO> getLeaderboardGroups() {
        return leaderboardGroups;
    }
    
}
