package com.sap.sailing.aiagent.impl.rules;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

import com.sap.sailing.aiagent.impl.AIAgentImpl;
import com.sap.sailing.aiagent.impl.RaceListener;
import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sse.common.TimePoint;

/**
 * A rule that is registered with a {@link RaceListener} so it is informed about updates from the races observed by the
 * {@link RaceListener} (a "composite listener" pattern). This "registration" currently happens in the
 * {@link RaceListener#RaceListener(AIAgentImpl, Leaderboard, RaceColumn, Fleet, TrackedRace)} constructor. The rule
 * analyzes the changes and then may decide to emit a prompt to be sent to a large language model to produce a comment.
 * The prompt is delivered to the {@link AIAgent} passed to the rule's constructor.
 * <p>
 * 
 * Rules can maintain state, e.g., as an aggregate of various events received, because each rule is specific to a single
 * {@link TrackedRace} and receives its change notifications from only that {@link #trackedRace}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class Rule extends AbstractRaceChangeListener {
    private final AIAgentImpl aiAgent;
    private final Leaderboard leaderboard;
    private final RaceColumn raceColumn;
    private final Fleet fleet;
    private final TrackedRace trackedRace;

    protected Rule(AIAgentImpl aiAgent, final Leaderboard leaderboard, final RaceColumn raceColumn, final Fleet fleet,
            final TrackedRace trackedRace) {
        super();
        this.aiAgent = aiAgent;
        this.leaderboard = leaderboard;
        this.raceColumn = raceColumn;
        this.fleet = fleet;
        this.trackedRace = trackedRace;
    }
    
    protected void produceComment(String topic, String prompt, TimePoint raceTimepoint, String tagIdentifier) throws UnsupportedOperationException,
            ClientProtocolException, URISyntaxException, IOException, ParseException, RaceLogNotFoundException, ServiceNotFoundException {
        aiAgent.produceCommentFromPrompt(topic, prompt, leaderboard.getName(), raceColumn.getName(), fleet.getName(), raceTimepoint, tagIdentifier);
    }

    protected AIAgentImpl getAiAgent() {
        return aiAgent;
    }

    protected Leaderboard getLeaderboard() {
        return leaderboard;
    }

    protected RaceColumn getRaceColumn() {
        return raceColumn;
    }

    protected Fleet getFleet() {
        return fleet;
    }

    protected TrackedRace getTrackedRace() {
        return trackedRace;
    }

    protected StringBuilder appendCompetitorToPromptBuilder(final Competitor competitor, final StringBuilder promptBuilder) {
        promptBuilder.append(competitor.getName());
        if (competitor.hasBoat()) {
            promptBuilder
                .append(" with sail number ")
                .append(((CompetitorWithBoat) competitor).getBoat().getSailID());
        }
        return promptBuilder;
    }
}
