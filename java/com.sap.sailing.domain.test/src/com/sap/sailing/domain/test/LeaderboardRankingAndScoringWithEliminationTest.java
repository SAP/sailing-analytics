package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.leaderboard.impl.DelegatingRegattaLeaderboardWithCompetitorElimination;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardRankingAndScoringWithEliminationTest extends LeaderboardScoringAndRankingTest {
    @Override
    protected RegattaLeaderboardWithEliminations createLeaderboard(Regatta regatta, int[] discardingThresholds) {
        return new DelegatingRegattaLeaderboardWithCompetitorElimination(
                ()->super.createLeaderboard(regatta, discardingThresholds), "Test leaderboard with elimination");
    }

    /**
     * See bug 4110: for partial leaderboards that are a "projection" of another leaderboard the suppressed
     * competitors will receive their scores for all races as usual but won't be shown in the resulting
     * leaderboard panel, except for score editing / import. Suppressed competitors, however, will not receive
     * a regatta rank and shall not be part of the set of competitors shown in a leaderboard panel.<p>
     * 
     * This test asserts that suppressing competitors will let worse competitors advance in the total ranking.
     * @throws NoWindException 
     */
    @Test
    public void testEliminationAdvancesWorseCompetitorsInRegattaRankButNotInRaces() throws NoWindException {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint earlier = now.minus(1000000);
        TimePoint later = now.plus(1000000); // first race from "earlier" to "now", second from "now" to "later", third from "later" to "finish"
        TimePoint finish = later.plus(1000000);
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */3, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTotalTimeNotCountedForRacesStartedLaterThanTimePointReqeusted",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        RegattaLeaderboardWithEliminations leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        @SuppressWarnings("unchecked")
        Map<Competitor, TimePoint>[] lastMarkPassingTimesForCompetitors = (Map<Competitor, TimePoint>[]) new HashMap<?, ?>[3];
        lastMarkPassingTimesForCompetitors[0] = new HashMap<>();
        lastMarkPassingTimesForCompetitors[0].put(c[0], now);
        lastMarkPassingTimesForCompetitors[0].put(c[1], now);
        lastMarkPassingTimesForCompetitors[0].put(c[2], now);
        lastMarkPassingTimesForCompetitors[1] = new HashMap<>();
        lastMarkPassingTimesForCompetitors[1].put(c[0], later);
        lastMarkPassingTimesForCompetitors[1].put(c[1], later);
        lastMarkPassingTimesForCompetitors[1].put(c[2], later);
        lastMarkPassingTimesForCompetitors[2] = new HashMap<>();
        lastMarkPassingTimesForCompetitors[2].put(c[0], finish);
        lastMarkPassingTimesForCompetitors[2].put(c[1], finish);
        lastMarkPassingTimesForCompetitors[2].put(c[2], finish);
        createAndAttachTrackedRacesWithStartTimeAndLastMarkPassingTimes(series.get(1), "Default",
                new Competitor[][] { f1 }, new TimePoint[] { earlier, now, later }, lastMarkPassingTimesForCompetitors);
        leaderboard.setEliminated(c[1], true);
        assertEquals(1.0, leaderboard.getTotalPoints(c[0], leaderboard.getRaceColumns().iterator().next(), later), 0.000001);
        assertEquals(2.0, leaderboard.getTotalPoints(c[1], leaderboard.getRaceColumns().iterator().next(), later), 0.000001);
        assertEquals(3.0, leaderboard.getTotalPoints(c[2], leaderboard.getRaceColumns().iterator().next(), later), 0.000001);
        assertEquals(1, leaderboard.getTotalRankOfCompetitor(c[0], later));
        assertEquals(0, leaderboard.getTotalRankOfCompetitor(c[1], later));
        assertEquals(2, leaderboard.getTotalRankOfCompetitor(c[2], later));
    }
}
