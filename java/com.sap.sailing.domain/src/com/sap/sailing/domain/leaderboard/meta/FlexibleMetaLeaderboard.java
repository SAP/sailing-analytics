package com.sap.sailing.domain.leaderboard.meta;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.NamedReentrantReadWriteLock;

public class FlexibleMetaLeaderboard extends AbstractMetaLeaderboard {
    private static final long serialVersionUID = 789076326144062944L;
    
    private final List<Leaderboard> leaderboards;
    
    private final NamedReentrantReadWriteLock leaderboardsLock;

    public FlexibleMetaLeaderboard(String name, ScoringScheme scoringScheme,
            ThresholdBasedResultDiscardingRule resultDiscardingRule) {
        super(name, scoringScheme, resultDiscardingRule);
        leaderboards = new ArrayList<Leaderboard>();
        leaderboardsLock = new NamedReentrantReadWriteLock("leaderboards collection of "+FlexibleMetaLeaderboard.class.getSimpleName()+" "+getName(),
                /* fair */ false);
    }

    @Override
    public Iterable<Leaderboard> getLeaderboards() {
        LockUtil.lockForRead(leaderboardsLock);
        try {
            return new ArrayList<Leaderboard>(leaderboards);
        } finally {
            LockUtil.unlockAfterRead(leaderboardsLock);
        }
    }

    public void addLeaderboard(Leaderboard leaderboard) {
        LockUtil.lockForWrite(leaderboardsLock);
        try {
            leaderboards.add(leaderboard);
            registerScoreCorrectionChangeForwarder(leaderboard);
        } finally {
            LockUtil.unlockAfterWrite(leaderboardsLock);
        }
    }

    public void removeLeaderboard(Leaderboard leaderboard) {
        LockUtil.lockForWrite(leaderboardsLock);
        try {
            leaderboards.remove(leaderboard);
            unregisterScoreCorrectionChangeForwarder(leaderboard);
        } finally {
            LockUtil.unlockAfterWrite(leaderboardsLock);
        }
    }
}
