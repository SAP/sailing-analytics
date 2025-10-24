package com.sap.sailing.domain.base;

import com.sap.sailing.domain.leaderboard.LeaderboardGroup;

public interface EventListener {
    void leaderboardGroupAdded(Event event, LeaderboardGroup leaderboardGroup);

    void leaderboardGroupRemoved(Event event, LeaderboardGroup leaderboardGroup);
}
