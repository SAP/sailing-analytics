package com.sap.sailing.domain.base.impl;

import java.util.UUID;

public class StrippedLeaderboardGroupImpl extends LeaderboardGroupBaseImpl {
    private static final long serialVersionUID = 7263952130620919924L;
    private final boolean hasOverallLeaderboard;
    private final String overallLeaderboardName;

    public StrippedLeaderboardGroupImpl(UUID id, String name, String description, String displayName, boolean hasOverallLeaderboard, String overallLeaderboardName) {
        super(id, name, description, displayName);
        this.hasOverallLeaderboard = hasOverallLeaderboard;
        this.overallLeaderboardName = overallLeaderboardName;
    }

    @Override
    public boolean hasOverallLeaderboard() {
        return hasOverallLeaderboard;
    }

    @Override
    public String getOverallLeaderboardName() {
        return overallLeaderboardName;
    }
}
