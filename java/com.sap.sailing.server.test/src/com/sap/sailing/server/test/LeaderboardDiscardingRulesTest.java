package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.test.AbstractLeaderboardTest;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRank;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRankAndManyCompetitors;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardDiscardingRulesTest {

    private static final String LEADERBOARDNAME = "TESTBOARD";

    private RacingEventService racingEventService;

    @BeforeEach
    public void setUp() {
        racingEventService = new RacingEventServiceImpl();
    }

    @Test
    public void testDiscardingRules() {
        racingEventService.removeLeaderboard(LEADERBOARDNAME);
        racingEventService.addFlexibleLeaderboard(LEADERBOARDNAME, null, new int[] { 1, 4 }, new LowPoint(), Collections.emptySet());
        FlexibleLeaderboard leaderboard = (FlexibleLeaderboard) racingEventService.getLeaderboardByName(LEADERBOARDNAME);
        assertNotNull(leaderboard);
        int[] discardingRulesNew = new int[] { 1, 5 };
        leaderboard.setCrossLeaderboardResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(discardingRulesNew));
        racingEventService.updateStoredLeaderboard(leaderboard);
        Leaderboard leaderboardNew = racingEventService.getLeaderboardByName(LEADERBOARDNAME);
        assertNotNull(leaderboardNew);
        int[] result = ((ThresholdBasedResultDiscardingRule) leaderboardNew.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces();
        assertArrayEquals(discardingRulesNew, result);
    }

    /**
     * See bug 892: If a competitor has two or more equally-scored races of which the to-be-discarded number
     * has to be discarded, ensure that not only one but all of them are discarded and that the sailor isn't awarded
     * more races sailed than the competition
     */
    @Test
    public void testDiscardingRulesForMultipleEquallyBadRaces() throws NoWindException {
        racingEventService.removeLeaderboard(LEADERBOARDNAME);
        racingEventService.addFlexibleLeaderboard(LEADERBOARDNAME, null, new int[] { 1, 2 }, new LowPoint(), Collections.emptySet());
        FlexibleLeaderboard leaderboard = (FlexibleLeaderboard) racingEventService.getLeaderboardByName(LEADERBOARDNAME);
        assertNotNull(leaderboard);
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);

        CompetitorWithBoat hasso = AbstractLeaderboardTest.createCompetitorWithBoat("Dr. Hasso Plattner");
        final TrackedRace race1 = new MockedTrackedRaceWithFixedRank(hasso, /* rank */ 123, /* started */ true, boatClass);
        leaderboard.addRace(race1, "R1", /* medalRace */false);
        final TrackedRace race2 = new MockedTrackedRaceWithFixedRank(hasso, /* rank */ 123, /* started */ true, boatClass);
        leaderboard.addRace(race2, "R2", /* medalRace */false);

        CompetitorWithBoat wolfgang = AbstractLeaderboardTest.createCompetitorWithBoat("Wolfgang Hunger");
        final TrackedRace race3 = new MockedTrackedRaceWithFixedRank(wolfgang, /* rank */ 123, /* started */ true, boatClass);
        leaderboard.addRace(race3, "R3", /* medalRace */false);
        final TrackedRace race4 = new MockedTrackedRaceWithFixedRank(wolfgang, /* rank */ 124, /* started */ true, boatClass);
        leaderboard.addRace(race4, "R4", /* medalRace */false);

        leaderboard.getScoreCorrection().correctScore(hasso, leaderboard.getRaceColumnByName("R3"), 123.);
        leaderboard.getScoreCorrection().correctScore(hasso, leaderboard.getRaceColumnByName("R4"), 123.);
        leaderboard.getScoreCorrection().correctScore(wolfgang, leaderboard.getRaceColumnByName("R1"), 122.);
        leaderboard.getScoreCorrection().correctScore(wolfgang, leaderboard.getRaceColumnByName("R2"), 122.);
        
        TimePoint now = MillisecondsTimePoint.now();
        // Hunger scores better than Plattner in this case because Hunger's two discards are 123/124, keeping 122/122
        // whereas Plattner is to discard two of the four 123 results, keeping 123/123, being two points worse than Hunger:
        double netPointsHasso = leaderboard.getNetPoints(hasso, now);
        double netPointsWolfgang = leaderboard.getNetPoints(wolfgang, now);
        assertEquals(122.+122., netPointsWolfgang, 0.0000000001);
        assertEquals(123.+123., netPointsHasso,  0.000000001);
    }

    /**
     * See bug 892: If a competitor has two or more equally-scored races of which the to-be-discarded number
     * has to be discarded, ensure that not only one but all of them are discarded and that the sailor isn't awarded
     * more races sailed than the competition
     */
    @Test
    public void testDiscardingRulesWithColumnFactor() throws NoWindException {
        racingEventService.removeLeaderboard(LEADERBOARDNAME);
        racingEventService.addFlexibleLeaderboard(LEADERBOARDNAME, null, new int[] { 2 }, new LowPoint(), Collections.emptySet());
        FlexibleLeaderboard leaderboard = (FlexibleLeaderboard) racingEventService.getLeaderboardByName(LEADERBOARDNAME);
        assertNotNull(leaderboard);
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("505", /* typicallyStartsUpwind */ true);
        CompetitorWithBoat hasso = AbstractLeaderboardTest.createCompetitorWithBoat("Dr. Hasso Plattner");
        final TrackedRace race1 = new MockedTrackedRaceWithFixedRank(hasso, /* rank */ 5, /* started */ true, boatClass);
        leaderboard.addRace(race1, "R1", /* medalRace */false).setFactor(2.0); // results in score 10
        final TrackedRace race2 = new MockedTrackedRaceWithFixedRank(hasso, /* rank */ 9, /* started */ true, boatClass);
        leaderboard.addRace(race2, "R2", /* medalRace */false);
        CompetitorWithBoat wolfgang = AbstractLeaderboardTest.createCompetitorWithBoat("Wolfgang Hunger");
        final TrackedRace race3 = new MockedTrackedRaceWithFixedRank(wolfgang, /* rank */ 123, /* started */ true, boatClass);
        leaderboard.addRace(race3, "R3", /* medalRace */false);
        final TrackedRace race4 = new MockedTrackedRaceWithFixedRank(wolfgang, /* rank */ 124, /* started */ true, boatClass);
        leaderboard.addRace(race4, "R4", /* medalRace */false);
        leaderboard.getScoreCorrection().correctScore(hasso, leaderboard.getRaceColumnByName("R3"), 6.);
        leaderboard.getScoreCorrection().correctScore(hasso, leaderboard.getRaceColumnByName("R4"), 3.);
        leaderboard.getScoreCorrection().correctScore(wolfgang, leaderboard.getRaceColumnByName("R1"), 1.); // results in score 2.0
        leaderboard.getScoreCorrection().correctScore(wolfgang, leaderboard.getRaceColumnByName("R2"), 2.);
        TimePoint now = MillisecondsTimePoint.now();
        // Hunger scores better than Plattner in this case because Hunger's two discards are 123/124, keeping 122/122
        // whereas Plattner is to discard two of the four 123 results, keeping 123/123, being two points worse than Hunger:
        double netPointsHasso = leaderboard.getNetPoints(hasso, now);
        double netPointsWolfgang = leaderboard.getNetPoints(wolfgang, now);
        assertEquals(123. + 2. + 2., netPointsWolfgang, 0.0000000001); // discards the 124.0 in R4
        assertEquals(9. + 6. + 3., netPointsHasso,  0.000000001);
    }

    /**
     * See bug 892: If a competitor has two or more equally-scored races of which the to-be-discarded number
     * has to be discarded, ensure that not only one but all of them are discarded and that the sailor isn't awarded
     * more races sailed than the competition
     */
    @Test
    public void testDiscardingRulesForMultipleEquallyBadRacesWithHighPointScoringScheme() throws NoWindException {
        racingEventService.removeLeaderboard(LEADERBOARDNAME);
        racingEventService.addFlexibleLeaderboard(LEADERBOARDNAME, null, new int[] { 1, 2 }, new HighPoint(), Collections.emptySet());
        final FlexibleLeaderboard leaderboard = (FlexibleLeaderboard) racingEventService.getLeaderboardByName(LEADERBOARDNAME);
        assertNotNull(leaderboard);
        final CompetitorWithBoat c1 = AbstractLeaderboardTest.createCompetitorWithBoat("C1");
        final CompetitorWithBoat c2 = AbstractLeaderboardTest.createCompetitorWithBoat("C2");
        final CompetitorWithBoat rasmus = AbstractLeaderboardTest.createCompetitorWithBoat("Rasmus");
        final CompetitorWithBoat jes = AbstractLeaderboardTest.createCompetitorWithBoat("Jes");
        final CompetitorWithBoat nobody = AbstractLeaderboardTest.createCompetitorWithBoat("Nobody");
        // R1:
        final MockedTrackedRaceWithFixedRankAndManyCompetitors race1 = new MockedTrackedRaceWithFixedRankAndManyCompetitors(c1, /* rank */ 1, /* started */ true);
        race1.addCompetitorWithBoat(rasmus); // score 2.0
        leaderboard.addRace(race1, "R1", /* medalRace */false);
        // R2:
        final MockedTrackedRaceWithFixedRankAndManyCompetitors race2 = new MockedTrackedRaceWithFixedRankAndManyCompetitors(c1, /* rank */ 1, /* started */ true);
        race2.addCompetitorWithBoat(rasmus); // score 2.0
        leaderboard.addRace(race2, "R2", /* medalRace */false);
        // R3:
        final MockedTrackedRaceWithFixedRankAndManyCompetitors race3 = new MockedTrackedRaceWithFixedRankAndManyCompetitors(c1, /* rank */ 1, /* started */ true);
        race3.addCompetitorWithBoat(jes); // score 2.0
        leaderboard.addRace(race3, "R3", /* medalRace */false);
        // R4:
        final MockedTrackedRaceWithFixedRankAndManyCompetitors race4 = new MockedTrackedRaceWithFixedRankAndManyCompetitors(c1, /* rank */ 1, /* started */ true);
        race4.addCompetitorWithBoat(c2);
        race4.addCompetitorWithBoat(jes); // score 3.0
        leaderboard.addRace(race4, "R4", /* medalRace */false);
        // R5:
        final MockedTrackedRaceWithFixedRankAndManyCompetitors race5 = new MockedTrackedRaceWithFixedRankAndManyCompetitors(c1, /* rank */ 1, /* started */ true);
        race5.addCompetitorWithBoat(c2);
        race5.addCompetitorWithBoat(nobody); // score 3.0
        leaderboard.addRace(race5, "R5", /* medalRace */false);
        // correct a few scores:
        leaderboard.getScoreCorrection().correctScore(rasmus, leaderboard.getRaceColumnByName("R3"), 2.0);
        leaderboard.getScoreCorrection().correctScore(rasmus, leaderboard.getRaceColumnByName("R4"), 2.0);
        leaderboard.getScoreCorrection().correctScore(jes, leaderboard.getRaceColumnByName("R1"), 3.0);
        leaderboard.getScoreCorrection().correctScore(jes, leaderboard.getRaceColumnByName("R2"), 3.0);
        // now test:
        final TimePoint now = MillisecondsTimePoint.now();
        // Jes scores better than Rasmus in this case because Jes's two discards are 1.0/2.0, keeping 3.0/3.0
        // whereas Rasmus is to discard two of the four 2.0 results, keeping 2.0/2.0, being two points worse than Hunger:
        final double netPointsRasmus = leaderboard.getNetPoints(rasmus, now);
        final double netPointsJes = leaderboard.getNetPoints(jes, now);
        assertEquals(5, Util.size(leaderboard.getCompetitors())); // c1, c2, Rasmus, Jes, Nobody
        assertEquals(3.+3., netPointsJes, 0.0000000001);
        assertEquals(2.+2., netPointsRasmus,  0.000000001);
    }

    /**
     * See bug 892: If a competitor has two or more equally-scored races of which the to-be-discarded number
     * has to be discarded, ensure that not only one but all of them are discarded and that the sailor isn't awarded
     * more races sailed than the competition
     */
    @Test
    public void testDiscardingRulesForMultipleEquallyBadRacesWithNonDiscardableDisqualification() throws NoWindException {
        racingEventService.removeLeaderboard(LEADERBOARDNAME);
        racingEventService.addFlexibleLeaderboard(LEADERBOARDNAME, null, new int[] { 1, 2 }, new LowPoint(), Collections.emptySet());
        FlexibleLeaderboard leaderboard = (FlexibleLeaderboard) racingEventService.getLeaderboardByName(LEADERBOARDNAME);
        assertNotNull(leaderboard);
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("29erXX", /* typicallyStartsUpwind */ true);

        CompetitorWithBoat hasso = AbstractLeaderboardTest.createCompetitorWithBoat("Dr. Hasso Plattner");
        final TrackedRace race1 = new MockedTrackedRaceWithFixedRank(hasso, /* rank */ 123, /* started */ true, boatClass);
        leaderboard.addRace(race1, "R1", /* medalRace */false);
        final TrackedRace race2 = new MockedTrackedRaceWithFixedRank(hasso, /* rank */ 123, /* started */ true, boatClass);
        leaderboard.addRace(race2, "R2", /* medalRace */false);

        CompetitorWithBoat wolfgang = AbstractLeaderboardTest.createCompetitorWithBoat("Wolfgang Hunger");
        final TrackedRace race3 = new MockedTrackedRaceWithFixedRank(wolfgang, /* rank */ 123, /* started */ true, boatClass);
        leaderboard.addRace(race3, "R3", /* medalRace */false);
        final TrackedRace race4 = new MockedTrackedRaceWithFixedRank(wolfgang, /* rank */ 124, /* started */ true, boatClass);
        leaderboard.addRace(race4, "R4", /* medalRace */false);

        leaderboard.getScoreCorrection().correctScore(hasso, leaderboard.getRaceColumnByName("R3"), 123.);
        leaderboard.getScoreCorrection().correctScore(hasso, leaderboard.getRaceColumnByName("R4"), 123.);
        leaderboard.getScoreCorrection().setMaxPointsReason(hasso, leaderboard.getRaceColumnByName("R1"), MaxPointsReason.DNE);
        leaderboard.getScoreCorrection().correctScore(hasso, leaderboard.getRaceColumnByName("R1"), 123.);
        leaderboard.getScoreCorrection().correctScore(wolfgang, leaderboard.getRaceColumnByName("R1"), 122.);
        leaderboard.getScoreCorrection().correctScore(wolfgang, leaderboard.getRaceColumnByName("R2"), 122.);
        
        TimePoint now = MillisecondsTimePoint.now();
        // Hunger scores better than Plattner in this case because Hunger's two discards are 123/124, keeping 122/122
        // whereas Plattner is to discard two of the four 123 results, keeping 123/123, being two points worse than Hunger:
        double netPointsHasso = leaderboard.getNetPoints(hasso, now);
        double netPointsWolfgang = leaderboard.getNetPoints(wolfgang, now);
        assertEquals(122.+122., netPointsWolfgang, 0.0000000001);
        assertEquals(123.+123., netPointsHasso,  0.000000001);
    }
}
