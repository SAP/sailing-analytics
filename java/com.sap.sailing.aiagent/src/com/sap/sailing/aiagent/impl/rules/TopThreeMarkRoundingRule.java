package com.sap.sailing.aiagent.impl.rules;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.parser.ParseException;

import com.sap.sailing.aiagent.impl.AIAgentImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * After having seen mark passings of at least three (or the number of competitors if fewer than three) competitors of
 * the {@link #getTrackedRace() tracked race}, a comment is produced about how the top competitors did in the leg they
 * finished.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TopThreeMarkRoundingRule extends Rule {
    private static final Logger logger = Logger.getLogger(TopThreeMarkRoundingRule.class.getName());

    private final static String TOPIC_TEMPLATE = "Rounding waypoint #%d";
    
    private final ConcurrentMap<Waypoint, Boolean> hasFired;
    
    public TopThreeMarkRoundingRule(AIAgentImpl aiAgent, Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet,
            TrackedRace trackedRace) {
        super(aiAgent, leaderboard, raceColumn, fleet, trackedRace);
        this.hasFired = new ConcurrentHashMap<>();
    }

    @Override
    public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings,
            Iterable<MarkPassing> markPassings) {
        final int numberOfCompetitors = Util.size(getTrackedRace().getRace().getCompetitors());
        for (final MarkPassing markPassing : markPassings) {
            // this rule does not fire on the start; there are separate rules analyzing the start performance
            final int waypointIndex = getTrackedRace().getRace().getCourse().getIndexOfWaypoint(markPassing.getWaypoint());
            if (waypointIndex > 0 && !(hasFired.get(markPassing.getWaypoint()) == Boolean.TRUE)) {
                final RankingInfo rankingInfo = getTrackedRace().getRankingMetric().getRankingInfo(markPassing.getTimePoint());
                final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(markPassing.getTimePoint());
                final Iterable<MarkPassing> waypointMarkPassingsCopy;
                final Iterable<MarkPassing> waypointMarkPassings = getTrackedRace().getMarkPassingsInOrder(markPassing.getWaypoint());
                getTrackedRace().lockForRead(waypointMarkPassings);
                try {
                    waypointMarkPassingsCopy = Util.asList(waypointMarkPassings);
                } finally {
                    getTrackedRace().unlockAfterRead(waypointMarkPassings);
                }
                if (Util.size(waypointMarkPassingsCopy) >= Math.min(numberOfCompetitors, 3)) {
                    final TrackedLeg previousTrackedLeg = waypointIndex > 1 ? getTrackedRace().getTrackedLeg(getTrackedRace().getRace().getCourse().getLeg(waypointIndex-2)) : null;
                    final LinkedHashMap<Competitor, Integer> ranksAfterPreviousLeg = waypointIndex > 1 ? previousTrackedLeg.getRanks(markPassing.getTimePoint()) : null;
                    // this rule hasn't fired yet for the markPassing's waypoint, but now we have three (or the number of
                    // competitors, whichever is less) mark passings for that waypoint and hence will fire the rule:
                    hasFired.put(markPassing.getWaypoint(), Boolean.TRUE);
                    final StringBuilder promptBuilder = new StringBuilder();
                    promptBuilder.append("Describe, very consisely, the fact that ");
                    int i=0;
                    final Iterator<MarkPassing> markPassingsIterator = waypointMarkPassingsCopy.iterator();
                    final List<Duration> gaps = new ArrayList<>();
                    boolean first = true;
                    TimePoint lastOfTheThreeMarkPassingTime = null;
                    while (i++ < 3 && markPassingsIterator.hasNext()) {
                        if (first) {
                            first = false;
                        } else {
                            promptBuilder.append(" and ");
                        }
                        final MarkPassing waypointMarkPassing = markPassingsIterator.next();
                        appendCompetitorToPromptBuilder(waypointMarkPassing.getCompetitor(), promptBuilder);
                        if (waypointIndex == getTrackedRace().getRace().getCourse().getNumberOfWaypoints()-1) {
                            promptBuilder.append(" finished the race");
                        } else {
                            promptBuilder.append(" rounded waypoint \"");
                            promptBuilder.append(waypointMarkPassing.getWaypoint().getName());
                            promptBuilder.append("\"");
                        }
                        promptBuilder.append(" in position #");
                        promptBuilder.append(i);
                        promptBuilder.append(" at ");
                        lastOfTheThreeMarkPassingTime = waypointMarkPassing.getTimePoint();
                        promptBuilder.append(lastOfTheThreeMarkPassingTime);
                        if (waypointIndex > 1) {
                            promptBuilder.append(" after starting into the leg at position #");
                            promptBuilder.append(ranksAfterPreviousLeg.get(waypointMarkPassing.getCompetitor()));
                            gaps.add(getTrackedRace().getRankingMetric()
                                    .getLegGapToLegLeaderInOwnTime(
                                            getTrackedRace().getTrackedLeg(waypointMarkPassing.getCompetitor(),
                                                    previousTrackedLeg.getLeg()),
                                            markPassing.getTimePoint(), rankingInfo, cache));
                        }
                    }
                    if (waypointIndex > 1) {
                        promptBuilder.append(", where their gaps to the race leader at the previous waypoint were ");
                        promptBuilder.append(Util.joinStrings(", ", gaps));
                        promptBuilder.append(", respectively.");
                    }
                    try {
                        produceComment(String.format(TOPIC_TEMPLATE, waypointIndex), promptBuilder.toString(), lastOfTheThreeMarkPassingTime,
                                getClass().getName());
                    } catch (UnsupportedOperationException | RaceLogNotFoundException | URISyntaxException | IOException
                            | ParseException | ServiceNotFoundException e) {
                        logger.log(Level.WARNING, "Problem trying to produce an AI comment", e);
                    }
                }
            }
        }
    }
}
