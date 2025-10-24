package com.sap.sailing.aiagent.impl;

import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;

/**
 * Serializable, but the {@link AIAgentImpl} reference will be left as {@code null} during
 * de-serialization because the {@link #aiAgent} field is {@code transient}. After de-serialization
 * this listener will no longer react to leaderboards getting added or removed to the leaderboard
 * group it listens on.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class LeaderboardGroupListenerImpl implements com.sap.sailing.domain.leaderboard.LeaderboardGroupListener {
    private static final long serialVersionUID = 685113626986312314L;
    private final transient AIAgentImpl aiAgent;

    public LeaderboardGroupListenerImpl(AIAgentImpl aiAgent) {
        super();
        this.aiAgent = aiAgent;
    }

    @Override
    public void leaderboardAdded(LeaderboardGroup group, Leaderboard leaderboard) {
        if (aiAgent != null) { // otherwise, we may have been de-serialized, e.g., on a replica, but there we don't want to act anyhow
            aiAgent.addNewRaceColumnListenerToLeaderboard(leaderboard);
        }
    }

    @Override
    public void leaderboardRemoved(LeaderboardGroup group, Leaderboard leaderboard) {
        if (aiAgent != null) { // otherwise, we may have been de-serialized, e.g., on a replica, but there we don't want to act anyhow
            aiAgent.removeRaceColumnListenerFromLeaderboard(leaderboard);
        }
    }
}
