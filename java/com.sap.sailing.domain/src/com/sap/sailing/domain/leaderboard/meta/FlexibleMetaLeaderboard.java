package com.sap.sailing.domain.leaderboard.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.leaderboard.HasCourseAreasListener;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

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
            registerScoreCorrectionAndRaceColumnChangeForwarder(leaderboard);
            getRaceColumnListeners().notifyListenersAboutRaceColumnAddedToContainer(getColumnForLeaderboard(leaderboard));
        } finally {
            LockUtil.unlockAfterWrite(leaderboardsLock);
        }
    }

    public void removeLeaderboard(Leaderboard leaderboard) {
        LockUtil.lockForWrite(leaderboardsLock);
        try {
            leaderboards.remove(leaderboard);
            getRaceColumnListeners().notifyListenersAboutRaceColumnRemovedFromContainer(getColumnForLeaderboard(leaderboard));
            unregisterScoreCorrectionChangeForwarder(leaderboard);
        } finally {
            LockUtil.unlockAfterWrite(leaderboardsLock);
        }
    }

    @Override
    public Iterable<CourseArea> getCourseAreas() {
        return Collections.emptySet();
    }
    
    @Override
    public void addCourseAreaChangeListener(HasCourseAreasListener listener) {
    }

    @Override
    public void removeCourseAreaChangeListener(HasCourseAreasListener listener) {
    }

    @Override
    public LeaderboardType getLeaderboardType() {
        return LeaderboardType.FlexibleMetaLeaderboard;
    }
}
