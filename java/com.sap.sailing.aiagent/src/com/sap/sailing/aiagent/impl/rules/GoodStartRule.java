package com.sap.sailing.aiagent.impl.rules;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.json.simple.parser.ParseException;

import com.sap.sailing.aiagent.impl.AIAgentImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * After the start mark passings of at least half the number of competitors of the {@link #getTrackedRace() tracked race}
 * have been received, a comment is requested on the start of the best starter.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class GoodStartRule extends Rule {
    private final static String TOPIC = "The Start";
    
    private final static Iterable<MaxPointsReason> EARLY_START_CODES = new HashSet<>(Arrays.asList(MaxPointsReason.OCS, MaxPointsReason.UFD, MaxPointsReason.BFD));
    
    private boolean hasFired;
    
    public GoodStartRule(AIAgentImpl aiAgent, Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet,
            TrackedRace trackedRace) {
        super(aiAgent, leaderboard, raceColumn, fleet, trackedRace);
        this.hasFired = false;
    }

    @Override
    public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings,
            Iterable<MarkPassing> markPassings) {
        if (!hasFired) {
            final Iterable<MarkPassing> startMarkPassings = getTrackedRace().getMarkPassingsInOrder(getTrackedRace().getRace().getCourse().getFirstWaypoint());
            if (Util.size(startMarkPassings) >= Util.size(getTrackedRace().getRace().getCompetitors()) / 2) {
                hasFired = true;
                final MarkPassing firstStart = startMarkPassings.iterator().next();
                try {
                    final StringBuilder promptBuilder = new StringBuilder();
                    promptBuilder
                        .append("Describe, very consisely, the fact that ");
                    appendCompetitorToPromptBuilder(firstStart.getCompetitor(), promptBuilder)
                        .append(" started first in race "+getTrackedRace().getRace().getName())
                        .append(". Consider the competitor's start delays in comparison to all other competitors' start delays in previous races of this regatta ")
                        .append(getLeaderboard().getName())
                        .append(" in class "+getLeaderboard().getBoatClass().getName())
                        .append(" that are given below in table form:\n");
                    for (final RaceColumn previousRaceColumn : getLeaderboard().getRaceColumns()) {
                        if (previousRaceColumn == getRaceColumn()) {
                            break;
                        }
                        final TrackedRace previousTrackedRace = previousRaceColumn.getTrackedRace(firstStart.getCompetitor());
                        if (previousTrackedRace != null) { // catch the unlikely case where the competitor did not race in a previous race
                            promptBuilder
                                .append("\nStart delays in race ")
                                .append(previousRaceColumn.getName())
                                .append(":\n");
                            for (final Competitor previousRaceCompetitor : previousTrackedRace.getRace().getCompetitors()) {
                                final MarkPassing previousRaceCompetitorStartMarkPassing = previousTrackedRace.getMarkPassing(previousRaceCompetitor, previousTrackedRace.getRace().getCourse().getFirstWaypoint());
                                if (previousRaceCompetitorStartMarkPassing != null) {
                                    promptBuilder.append("  ");
                                    appendCompetitorToPromptBuilder(previousRaceCompetitor, promptBuilder)
                                        .append(": ")
                                        .append(previousTrackedRace.getStartOfRace().until(previousRaceCompetitorStartMarkPassing.getTimePoint()))
                                        .append("\n");
                                }
                            }
                        }
                    }
                    final MaxPointsReason maxPointsReason = getLeaderboard().getMaxPointsReason(firstStart.getCompetitor(), getRaceColumn(), TimePoint.now());
                    if (Util.contains(EARLY_START_CODES, maxPointsReason)) {
                        promptBuilder.append("Consider that ");
                        appendCompetitorToPromptBuilder(firstStart.getCompetitor(), promptBuilder);
                        promptBuilder.append(" was called over early with penalty code ");
                        promptBuilder.append(maxPointsReason.name());
                        promptBuilder.append(".");
                    }
                    promptBuilder
                        .append("Also, consider in your concise comment the typical start performance of this competitor in previous regattas at other events.");
                    produceComment(TOPIC, promptBuilder.toString(), firstStart.getTimePoint(),
                            getClass().getName()); // there is only one start analysis per race, so the class name is sufficient for identification
                } catch (UnsupportedOperationException | URISyntaxException | IOException | ParseException | RaceLogNotFoundException | ServiceNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
