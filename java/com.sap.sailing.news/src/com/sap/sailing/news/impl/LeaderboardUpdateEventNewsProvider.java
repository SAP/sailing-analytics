package com.sap.sailing.news.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoreCorrection;
import com.sap.sailing.news.EventNewsItem;
import com.sap.sailing.news.EventNewsProvider;
import com.sap.sse.common.TimePoint;

/**
 * An event news provider looking at leaderboard updates triggered by changes of timePointOfLatestModification
 * 
 * @author Frank Mittag
 *
 */
public class LeaderboardUpdateEventNewsProvider implements EventNewsProvider {
    private final static int LIMIT = 10;
    
    @Override
    public Collection<? extends EventNewsItem> getNews(Event event) {
        final List<LeaderboardUpdateNewsItem> result = new LinkedList<>();
        final Iterable<LeaderboardGroup> leaderboardGroups = event.getLeaderboardGroups();
        for (LeaderboardGroup leaderboardGroup : leaderboardGroups) {
            for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                if (leaderboard.isPartOfEvent(event)) {
                    final ScoreCorrection scoreCorrection = leaderboard.getScoreCorrection();
                    if (scoreCorrection != null) {
                        final TimePoint timePointOfLatestModification = scoreCorrection.getTimePointOfLastCorrectionsValidity();
                        if (timePointOfLatestModification != null) {
                            String displayName = leaderboard.getDisplayName() != null ? leaderboard.getDisplayName()
                                    : leaderboard.getName();
                            String boatClassName = null;
                            if (leaderboard instanceof RegattaLeaderboard) {
                                final BoatClass boatClass = ((RegattaLeaderboard) leaderboard).getRegatta().getBoatClass();
                                if (boatClass != null) {
                                    boatClassName = boatClass.getName();
                                }
                            }
                            result.add(new LeaderboardUpdateNewsItem(event.getId(), timePointOfLatestModification.asDate(), leaderboard.getName(), displayName, boatClassName));
                        }
                    }
                }
            }
        }
        Collections.sort(result);
        return result.subList(0, Math.min(LIMIT, result.size()));
    }
}
