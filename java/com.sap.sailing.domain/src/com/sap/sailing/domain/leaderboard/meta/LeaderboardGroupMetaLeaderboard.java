package com.sap.sailing.domain.leaderboard.meta;

import java.util.Collections;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.impl.TrackedRaces;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.leaderboard.HasCourseAreasListener;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupListener;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * A meta leaderboard which considers all regular leaderboards of a {@link LeaderboardGroup}. To stay up to date, this
 * meta leaderboard registers itself as a {@link LeaderboardGroupListener} on the leaderboard group it represents. This
 * way, whenever a leaderboard is added to or removed from the group, this meta leaderboard can in turn inform its
 * {@link RaceColumnListener}s about the impact this change has on the set of {@link TrackedRaces} and therefore the
 * competitors attached to this meta leaderboard.
 * <p>
 * 
 * After an object of this type has been de-serialized, and after all objects referenced by it (leaderboard, leaderboard
 * group) have been initialized, {@link #registerAsScoreCorrectionChangeForwarderAndRaceColumnListenerOnAllLeaderboards()} must be called to
 * ensure that score corrections are propagated as desired. This is because score correction listeners are "transient"
 * and are as such not serialized. Note: calling {@link #registerAsScoreCorrectionChangeForwarderAndRaceColumnListenerOnAllLeaderboards()}
 * during <code>readObject</code> or <code>readResolve</code> is not possible because the object graph hasn't been fully
 * initialized yet when those methods are called. Hence, the structures for registering the score correction listeners
 * are not yet in place at that time.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class LeaderboardGroupMetaLeaderboard extends AbstractMetaLeaderboard implements LeaderboardGroupListener, RaceColumnListener {
    private static final long serialVersionUID = 8087872002175528002L;
    
    private final LeaderboardGroup leaderboardGroup;

    public LeaderboardGroupMetaLeaderboard(LeaderboardGroup leaderboardGroup, ScoringScheme scoringScheme,
            ThresholdBasedResultDiscardingRule resultDiscardingRule) {
        super(getOverallLeaderboardName(leaderboardGroup.getName()), scoringScheme, resultDiscardingRule);
        this.leaderboardGroup = leaderboardGroup;
        leaderboardGroup.addLeaderboardGroupListener(this);
        registerAsScoreCorrectionChangeForwarderAndRaceColumnListenerOnAllLeaderboards();
    }

    public static String getOverallLeaderboardName(String leaderboardGroupName) {
        return leaderboardGroupName + " " + LeaderboardNameConstants.OVERALL;
    }

    public void registerAsScoreCorrectionChangeForwarderAndRaceColumnListenerOnAllLeaderboards() {
        for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
            registerScoreCorrectionAndRaceColumnChangeForwarder(leaderboard);
        }
    }
    
    @Override
    public Iterable<Leaderboard> getLeaderboards() {
        return leaderboardGroup.getLeaderboards();
    }

    @Override
    public void leaderboardAdded(LeaderboardGroup group, Leaderboard leaderboard) {
        registerScoreCorrectionAndRaceColumnChangeForwarder(leaderboard);
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                if (trackedRace != null) {
                    getRaceColumnListeners().notifyListenersAboutTrackedRaceLinked(raceColumn, fleet, trackedRace);
                }
            }
        }
        getRaceColumnListeners().notifyListenersAboutRaceColumnAddedToContainer(getColumnForLeaderboard(leaderboard));
    }

    @Override
    public void leaderboardRemoved(LeaderboardGroup group, Leaderboard leaderboard) {
        leaderboard.removeRaceColumnListener(this);
        unregisterScoreCorrectionChangeForwarder(leaderboard);
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                if (trackedRace != null) {
                    getRaceColumnListeners().notifyListenersAboutTrackedRaceUnlinked(raceColumn, fleet, trackedRace);
                }
            }
        }
        getRaceColumnListeners().notifyListenersAboutRaceColumnRemovedFromContainer(getColumnForLeaderboard(leaderboard));
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
        return LeaderboardType.RegattaMetaLeaderboard;
    }
}
