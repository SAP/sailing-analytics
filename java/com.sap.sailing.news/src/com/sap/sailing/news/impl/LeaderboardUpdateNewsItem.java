package com.sap.sailing.news.impl;

import java.util.Date;
import java.util.UUID;

public class LeaderboardUpdateNewsItem extends AbstractEventNewsItem {
    private String leaderboardName;
    private String boatClass;

    public LeaderboardUpdateNewsItem(UUID eventId, Date updateDate, String leaderboardName, String leaderboardDisplayName, String boatClass) {
        super(eventId, leaderboardDisplayName, /* message */ null, updateDate, /* relatedItemLink */ null);
        this.leaderboardName = leaderboardName;
        this.boatClass = boatClass;
    }
    
    public String getLeaderboardName() {
        return leaderboardName;
    }
    
    public String getBoatClass() {
        return boatClass;
    }
}
