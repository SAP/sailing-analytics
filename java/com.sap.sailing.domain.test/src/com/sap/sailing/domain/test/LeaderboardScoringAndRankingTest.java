package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AdditionalScoringInformationFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.scoring.AdditionalScoringInformationType;
import com.sap.sailing.domain.abstractlog.race.scoring.RaceLogAdditionalScoringInformationEvent;
import com.sap.sailing.domain.abstractlog.race.scoring.impl.RaceLogAdditionalScoringInformationEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateImpl;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.HighPointExtremeSailingSeriesOverall;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets10LastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets10Or8AndLastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets12Or8AndLastBreaksTie2017;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.LowPointTieBreakBasedOnLastSeriesOnly;
import com.sap.sailing.domain.leaderboard.impl.LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

public class LeaderboardScoringAndRankingTest extends LeaderboardScoringAndRankingTestBase {
    @Test
    public void testOneStartedRaceWithDifferentScores() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testOneStartedRaceWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace f1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        f1Column.setTrackedRace(f1Column.getFleets().iterator().next(), f1);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors, Util.asList(rankedCompetitors));
    }

    @Test
    public void testAutomaticRdgAdjustment() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */4,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testOneStartedRaceWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_AUTOMATIC_RDG));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace f1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        RaceColumn f2Column = series.get(1).getRaceColumnByName("F2");
        RaceColumn f3Column = series.get(1).getRaceColumnByName("F3");
        RaceColumn f4Column = series.get(1).getRaceColumnByName("F4");
        f1Column.setTrackedRace(f1Column.getFleets().iterator().next(), f1);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitors.get(3), f1Column, MaxPointsReason.RDG);
        leaderboard.getScoreCorrection().correctScore(competitors.get(3), f2Column, 7);
        assertEquals(7, leaderboard.getTotalPoints(competitors.get(3), f1Column, later), 0.0000001);
        leaderboard.getScoreCorrection().correctScore(competitors.get(3), f3Column, 5);
        assertEquals(6, leaderboard.getTotalPoints(competitors.get(3), f1Column, later), 0.0000001);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitors.get(3), f4Column, MaxPointsReason.RDG);
        assertEquals(6, leaderboard.getTotalPoints(competitors.get(3), f1Column, later), 0.0000001);
        assertEquals(6, leaderboard.getTotalPoints(competitors.get(3), f4Column, later), 0.0000001);
    }

    /**
     * Regarding bug 912, test adding a disqualification in the middle, with a high-point scoring scheme, and check that
     * all competitors ranked worse advance by one, including getting <em>more</em> points due to the high-point scoring
     * scheme. Note that this does not test the net points given for those competitors.
     */
    @Test
    public void testOneStartedRaceWithDifferentScoresAndDisqualificationUsingHighPointScoringScheme() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testOneStartedRaceWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series finalSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        seriesIter.next();
        finalSeries = seriesIter.next();
        leaderboard.getScoreCorrection().setMaxPointsReason(competitors.get(5), finalSeries.getRaceColumnByName("F1"), MaxPointsReason.DSQ);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace f1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        f1Column.setTrackedRace(f1Column.getFleets().iterator().next(), f1);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors.subList(0, 5), Util.subList(rankedCompetitors, 0, 5));
        assertEquals(competitors.subList(6, 10), Util.subList(rankedCompetitors, 5, 9));
        assertEquals(competitors.get(5), Util.get(rankedCompetitors, 9));

        // Now test the net points and make sure the other competitors advanced by one, too
        assertEquals(0, leaderboard.getNetPoints(competitors.get(5), f1Column, now), 0.000000001);
        for (int i=0; i<5; i++) {
            assertEquals(10-i, leaderboard.getNetPoints(competitors.get(i), f1Column, now), 0.000000001);
        }
        for (int i=6; i<10; i++) {
            assertEquals(10-(i-1), leaderboard.getNetPoints(competitors.get(i), f1Column, now), 0.000000001);
        }
    }

    @Test
    public void testColumnFactorInRegattaLeaderboard() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "Default" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testColumnFactorInRegattaLeaderboard",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        final double factor = 2.0;
        q2Column.setFactor(factor);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        ArrayList<Competitor> reverseCompetitors = new ArrayList<Competitor>(competitors);
        Collections.reverse(reverseCompetitors);
        TrackedRace q2Default = new MockedTrackedRaceWithStartTimeAndRanks(now, reverseCompetitors);
        q1Column.setTrackedRace(q1Column.getFleetByName("Default"), q1Default);
        q2Column.setTrackedRace(q2Column.getFleetByName("Default"), q2Default);
        assertEquals(Double.valueOf(competitors.size()), leaderboard.getNetPoints(competitors.get(0), q1Column, later));
        assertEquals(factor * Double.valueOf(1), leaderboard.getNetPoints(competitors.get(0), q2Column, later), 0.000000001);
        assertEquals(Double.valueOf(factor*1.0+competitors.size()), leaderboard.getNetPoints(competitors.get(0), later), 0.000000001);
    }

    /**
     * Regarding bug 961, test scoring in a leaderboard that has a qualification series with two unordered groups where for one
     * column only one group has raced (expressed by a mocked TrackedRace attached to the column). Ensure that the column does
     * count for the net points sum but that the competitor ordering is controlled by the number of races.
     */
    @Test
    public void testUnorderedGroupsWithOneGroupNotHavingRacedInAColumn() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "Yellow", "Blue" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testUnorderedGroupsWithOneGroupNotHavingRacedInAcolumn",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 5));
        TrackedRace q1Blue = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(5, 10));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("Yellow"), q1Yellow);
        q1Column.setTrackedRace(q1Column.getFleetByName("Blue"), q1Blue);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertTrue(competitors.get(i) == Util.get(rankedCompetitors, 2*i) || competitors.get(i) == Util.get(rankedCompetitors, 2*i+1));
            assertTrue(competitors.get(i+5) == Util.get(rankedCompetitors, 2*i) || competitors.get(i+5) == Util.get(rankedCompetitors, 2*i+1));
            assertEquals((double) (i+1), leaderboard.getNetPoints(competitors.get(i), later), 0.000000001);
            assertEquals((double) (i+1), leaderboard.getNetPoints(competitors.get(i+5), later), 0.0000000001);
        }
        // now add one race for yellow fleet and test that it counts for the score but not the ordering
        // because blue fleet is still missing its race for Q2
        TrackedRace q2Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(3, 8));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("Yellow"), q2Yellow);
        Iterable<Competitor> rankedCompetitorsWithOneRaceMissingInQ2 = leaderboard.getCompetitorsFromBestToWorst(later);
        // scores: C1=1, C2=2, C3=3, C4=5, C5=7, C6=4, C7=6, C8=8, C9=4, C10=5
        // ordered by scores: C1, C2, C3, C6/C9, C4/C10, C7, C5, C8
        // incomplete fleets are ordered now also by scores
        assertEquals(Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                competitors.get(5), competitors.get(8), competitors.get(3), competitors.get(9), competitors.get(6),
                competitors.get(4), competitors.get(7) }), Util.asList(rankedCompetitorsWithOneRaceMissingInQ2));
        double[] points = new double[] { 1, 2, 3, 5, 7, 4, 6, 8, 4, 5 };
        for (int i=0; i<9; i++) {
            assertEquals(points[i], leaderboard.getNetPoints(competitors.get(i), later), 0.000000001);
        }
        // now add a tracked race for the blue fleet for Q2 and assert that the Q2 scores count for the net points sum
        TrackedRace q2Blue = new MockedTrackedRaceWithStartTimeAndRanks(now,
                Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                        competitors.get(8), competitors.get(9) }));
        q2Column.setTrackedRace(q2Column.getFleetByName("Blue"), q2Blue);
        // the new order in Q2 expected to be { (0, 3), (1, 4), (2, 5), (8, 6), (9, 7) }
        // therefore the new net points, in ascending order, are expected to be:
        // { 0: 1+1=2; 5: 1+3=4; 1: 2+2=4; 3: 4+1=5; 2: 3+3=6; 6: 2+4=6; 4: 5+2=7; 7: 3+5=8; 8: 4+4=8; 9: 5+5=10 }
        double[] expectedTotalPoints = new double[] { 2, 4, 6, 5, 7, 4, 6, 8, 8, 10 };
        int[] expectedOrderAfterTwoFullRacesPlusMinusOne = new int[] { 0, 5, 1, 3, 2, 6, 4, 7, 8, 9 };
        Iterable<Competitor> rankedCompetitorsWithAllRacesInQ2 = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertTrue(competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i]) == Util.get(rankedCompetitorsWithAllRacesInQ2, 2*i) ||
                    competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i]) == Util.get(rankedCompetitorsWithAllRacesInQ2, 2*i+1));
            assertTrue(competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i+1]) == Util.get(rankedCompetitorsWithAllRacesInQ2, 2*i) ||
                    competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i+1]) == Util.get(rankedCompetitorsWithAllRacesInQ2, 2*i+1));
            assertEquals(expectedTotalPoints[2*i], leaderboard.getNetPoints(competitors.get(2*i), later), 0.000000001);
            assertEquals(expectedTotalPoints[2*i+1], leaderboard.getNetPoints(competitors.get(2*i+1), later), 0.000000001);
        }

    }

    /**
     * Regarding bug 961 and 1023, test scoring in a leaderboard that has a qualification series with two unordered
     * groups where for one column only one group has raced (expressed by a mocked TrackedRace attached to the column).
     * Those competitors are expected to rank better than those who haven't raced because they have a greater number of
     * races. Then add the second tracked race for the second column but such that it hasn't started yet at the time
     * point of the query. Still, the competitors of the fleet that did race already shall be scored better. Then add a
     * score correction, simulating the use of a proxy race (that doesn't ever start) and a manual score entry. Now, the
     * second fleet shall be ranked according to the score comparison with the competitors from the first fleet.
     */
    @Test
    public void testUnorderedGroupsWithOneGroupNotHavingStartedRacedInAcolumnAndThenCorrectingScore() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "Yellow", "Blue" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testUnorderedGroupsWithOneGroupNotHavingRacedInAcolumn",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 5));
        TrackedRace q1Blue = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(5, 10));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("Yellow"), q1Yellow);
        q1Column.setTrackedRace(q1Column.getFleetByName("Blue"), q1Blue);
        // now add one race for yellow fleet and test that it doesn't count because blue fleet is still missing its race for Q2
        TrackedRace q2Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(3, 8));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("Yellow"), q2Yellow);
        // now add a tracked race for the blue fleet for Q2 that hasn't started yet and assert that the Q2 scores still don't count for the net points sum
        TimePoint muchLater = later.plus(10000000000l);
        TrackedRace q2Blue = new MockedTrackedRaceWithStartTimeAndRanks(muchLater,
                Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                        competitors.get(8), competitors.get(9) }));
        q2Column.setTrackedRace(q2Column.getFleetByName("Blue"), q2Blue);
        Iterable<Competitor> rankedCompetitorsWithOneRaceMissingInQ2 = leaderboard.getCompetitorsFromBestToWorst(later);
        // scores: C1=1, C2=2, C3=3, C4=5, C5=7, C6=4, C7=6, C8=8, C9=4, C10=5
        // ordered by scores: C1, C2, C3, C6/C9, C4/C10, C7, C5, C8
        // incomplete fleets are ordered now also by scores
        assertEquals(Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                competitors.get(5), competitors.get(8), competitors.get(3), competitors.get(9), competitors.get(6),
                competitors.get(4), competitors.get(7) }), Util.asList(rankedCompetitorsWithOneRaceMissingInQ2));
        double[] points = new double[] { 1, 2, 3, 5, 7, 4, 6, 8, 4, 5 };
        for (int i=0; i<9; i++) {
            assertEquals(points[i], leaderboard.getNetPoints(competitors.get(i), later), 0.000000001);
        }
        // expect all results to be valid because a fleet will have its score counted even if not all fleets have raced;
        // see discussion for bug 961, but more importantly later on bug 1023.
        for (final Competitor competitor : competitors) {
            assertTrue(leaderboard.getScoringScheme().isValidInNetScore(leaderboard, q2Column, competitor, later));
        }
        // now add a score correction for Q2/Blue to make it count:
        leaderboard.getScoreCorrection().correctScore(competitors.get(9), q2Column, 42.);
        for (final Competitor competitor : competitors) {
            assertTrue(leaderboard.getScoringScheme().isValidInNetScore(leaderboard, q2Column, competitor, later));
        }
        // the new order in Q2 expected to be { (9, 3), ... } (we don't know about any competitor in q2Blue but #9
        // therefore the new net points for #9 are
        // { 9: 5+42=47 }
        assertEquals(47.0, leaderboard.getNetPoints(competitors.get(9), later), 0.00000001);
    }

    /**
     * Match races want to prefer a competitor over another with equal score sum based on the direct
     * comparison. When the two competitors matched each other, the result(s) of these matches are to
     * be considered in isolation to break a tie. If this score sum is still equal, the last of these
     * matches shall take precedence, if any.<p>
     *
     * If more than two competitors have equal score sum the comparator needs to ensure that all those
     * competitors can be put in a consistent order. This may not always be possible. Consider three
     * competitors A, B, and C such that A won against B, B won against C and C won against A, but all
     * three having equal total scores. In this case, no consistent ordering of these three competitors
     * is possible, and all three need to be ranked equal.
     */
    @Test
    public void testMatchRaceTieBreak() {
        List<Competitor> competitors = createCompetitors(6);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "M1", "M2", "M3" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testMatchRaceTieBreak",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 2));
        TrackedRace q1M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(2, 4));
        TrackedRace q1M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(4, 6));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("M1"), q1M1);
        q1Column.setTrackedRace(q1Column.getFleetByName("M2"), q1M2);
        q1Column.setTrackedRace(q1Column.getFleetByName("M3"), q1M3);

        TrackedRace q2M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(0), competitors.get(2)));
        TrackedRace q2M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(3), competitors.get(1)));
        TrackedRace q2M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(5), competitors.get(4)));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("M1"), q2M1);
        q2Column.setTrackedRace(q2Column.getFleetByName("M2"), q2M2);
        q2Column.setTrackedRace(q2Column.getFleetByName("M3"), q2M3);

        // point sums for competitors 0..5: 2, 0, 1, 1, 1, 1
        // So it's clear-cut for (0) and (1); all others (2..5) need to be tie-broken based
        // on their direct comparison.
        // (2):  < (3); ? (4); ? (5)
        // (3):         ? (4); ? (5)
        // (4):                > (5)   because (4) won over (5) in Q1, but (5) won over (4) in Q2 which came last
        // With this, multiple orders are possible:
        //   (2), (3), (5), (4)
        //   (2), (5), (3), (4)
        //   (5), (2), (3), (4)
        //   (5), (4), (2), (3)
        //   ...
        // So we have two separate "ordered chains," one being (2)<(3) and the other being (5)<(4). In this
        // case (2) and (5) shall be compared equal, and (3) and (4) shall be compared equal, leaving resolution
        // to a lesser criterion, such as the fallback name-based ordering.

        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors.get(0), Util.get(rankedCompetitors, 0)); // 2 points; winner
        assertEquals(competitors.get(1), Util.last(rankedCompetitors)); // 0 points; loser
        assertTrue(Util.indexOf(rankedCompetitors, competitors.get(2)) < Util.indexOf(rankedCompetitors, competitors.get(3)));
        assertTrue(Util.indexOf(rankedCompetitors, competitors.get(5)) < Util.indexOf(rankedCompetitors, competitors.get(4)));
    }

    @Test
    public void testMatchRaceTieBreakWithMultipleDirectComparisonScoreDifference() {
        List<Competitor> competitors = createCompetitors(6);
        Regatta regatta = createRegatta(/* qualifying */ 4, new String[] { "M1", "M2", "M3" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testMatchRaceTieBreak",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 2));
        TrackedRace q1M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(2, 4));
        TrackedRace q1M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(4, 6));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("M1"), q1M1);
        q1Column.setTrackedRace(q1Column.getFleetByName("M2"), q1M2);
        q1Column.setTrackedRace(q1Column.getFleetByName("M3"), q1M3);

        TrackedRace q2M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(1), competitors.get(0)));
        TrackedRace q2M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(2), competitors.get(3)));
        TrackedRace q2M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(5)));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("M1"), q2M1);
        q2Column.setTrackedRace(q2Column.getFleetByName("M2"), q2M2);
        q2Column.setTrackedRace(q2Column.getFleetByName("M3"), q2M3);

        TrackedRace q3M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 2));
        TrackedRace q3M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(2, 4));
        TrackedRace q3M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(4, 6));
        RaceColumn q3Column = qualificationSeries.getRaceColumnByName("Q3");
        q3Column.setTrackedRace(q2Column.getFleetByName("M1"), q3M1);
        q3Column.setTrackedRace(q2Column.getFleetByName("M2"), q3M2);
        q3Column.setTrackedRace(q2Column.getFleetByName("M3"), q3M3);

        TrackedRace q4M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(1), competitors.get(2)));
        TrackedRace q4M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(3), competitors.get(0)));
        TrackedRace q4M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(5)));
        RaceColumn q4Column = qualificationSeries.getRaceColumnByName("Q4");
        q4Column.setTrackedRace(q2Column.getFleetByName("M1"), q4M1);
        q4Column.setTrackedRace(q2Column.getFleetByName("M2"), q4M2);
        q4Column.setTrackedRace(q2Column.getFleetByName("M3"), q4M3);
        // point sums for competitors 0..5: 2, 2, 3, 1, 4, 0
        // The only tie to break is between (0) and (1) with the following direct comparisons:
        // Q1: (0)->(1); Q2: (1)->(0); Q3: (0)->(1)
        // This gives 2 points for (0) and 1 point for (1), so (0) shall be ranked better (lesser) than (1)

        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(Arrays.asList(competitors.get(4), competitors.get(2), competitors.get(0), competitors.get(1), competitors.get(3), competitors.get(5)),
                Util.asList(rankedCompetitors));
    }

    @Test
    public void testMatchRaceTieBreakWithMultipleDirectComparisonLastRace() {
        List<Competitor> competitors = createCompetitors(6);
        Regatta regatta = createRegatta(/* qualifying */ 4, new String[] { "M1", "M2", "M3" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testMatchRaceTieBreak",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 2));
        TrackedRace q1M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(2, 4));
        TrackedRace q1M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(4, 6));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("M1"), q1M1);
        q1Column.setTrackedRace(q1Column.getFleetByName("M2"), q1M2);
        q1Column.setTrackedRace(q1Column.getFleetByName("M3"), q1M3);

        TrackedRace q2M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(1), competitors.get(0)));
        TrackedRace q2M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(2), competitors.get(3)));
        TrackedRace q2M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(5)));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("M1"), q2M1);
        q2Column.setTrackedRace(q2Column.getFleetByName("M2"), q2M2);
        q2Column.setTrackedRace(q2Column.getFleetByName("M3"), q2M3);

        TrackedRace q3M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 2));
        TrackedRace q3M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(2, 4));
        TrackedRace q3M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(4, 6));
        RaceColumn q3Column = qualificationSeries.getRaceColumnByName("Q3");
        q3Column.setTrackedRace(q2Column.getFleetByName("M1"), q3M1);
        q3Column.setTrackedRace(q2Column.getFleetByName("M2"), q3M2);
        q3Column.setTrackedRace(q2Column.getFleetByName("M3"), q3M3);

        TrackedRace q4M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(1), competitors.get(0)));
        TrackedRace q4M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(2), competitors.get(3)));
        TrackedRace q4M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(5)));
        RaceColumn q4Column = qualificationSeries.getRaceColumnByName("Q4");
        q4Column.setTrackedRace(q2Column.getFleetByName("M1"), q4M1);
        q4Column.setTrackedRace(q2Column.getFleetByName("M2"), q4M2);
        q4Column.setTrackedRace(q2Column.getFleetByName("M3"), q4M3);
        // point sums for competitors 0..5: 2, 2, 4, 0, 4, 0
        // The ties between (2) and (4) with both 4 points cannot be decided because they never met;
        // the same holds for (3) and (5) with both 0 points.
        // The tie between (0) and (1) shall be broken based on their last match because their
        // direct comparison scores are equal, too (both 2). In the last match (1) won over (0).
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(Arrays.asList(competitors.get(1), competitors.get(0)), Util.subList(rankedCompetitors, 2, 4));
    }

    /**
     * Match races want to prefer a competitor over another with equal score sum based on the direct
     * comparison. When the two competitors matched each other, the result(s) of these matches are to
     * be considered in isolation to break a tie. If this score sum is still equal, the last of these
     * matches shall take precedence, if any.<p>
     *
     * If more than two competitors have equal score sum the comparator needs to ensure that all those
     * competitors can be put in a consistent order. This may not always be possible. Consider three
     * competitors A, B, and C such that A won against B, B won against C and C won against A, but all
     * three having equal total scores. In this case, no consistent ordering of these three competitors
     * is possible, and all three need to be ranked equal.
     */
    @Test
    public void testMatchRaceTieBreakWithCycle() {
        List<Competitor> competitors = createCompetitors(6);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "M1", "M2", "M3" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testMatchRaceTieBreak",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(0), competitors.get(1)));
        TrackedRace q1M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(2), competitors.get(3)));
        TrackedRace q1M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(5)));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("M1"), q1M1);
        q1Column.setTrackedRace(q1Column.getFleetByName("M2"), q1M2);
        q1Column.setTrackedRace(q1Column.getFleetByName("M3"), q1M3);

        TrackedRace q2M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(1), competitors.get(2)));
        TrackedRace q2M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(3), competitors.get(4)));
        TrackedRace q2M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(5), competitors.get(0)));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("M1"), q2M1);
        q2Column.setTrackedRace(q2Column.getFleetByName("M2"), q2M2);
        q2Column.setTrackedRace(q2Column.getFleetByName("M3"), q2M3);

        // point sums for competitors 0..5: 1, 1, 1, 1, 1, 1
        // cyclic direct comparison; expect competitors to be ordered by their names
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors, Util.asList(rankedCompetitors));
    }

    /**
     * Match races want to prefer a competitor over another with equal score sum based on the direct
     * comparison. When the two competitors matched each other, the result(s) of these matches are to
     * be considered in isolation to break a tie. If this score sum is still equal, the last of these
     * matches shall take precedence, if any.<p>
     *
     * If more than two competitors have equal score sum the comparator needs to ensure that all those
     * competitors can be put in a consistent order. This may not always be possible. Consider three
     * competitors A, B, and C such that A won against B, B won against C and C won against A, but all
     * three having equal total scores. In this case, no consistent ordering of these three competitors
     * is possible, and all three need to be ranked equal.
     */
    @Test
    public void testMatchRaceTieBreakWithTwoRelatedCycles() {
        List<Competitor> competitors = createCompetitors(6);
        Regatta regatta = createRegatta(/* qualifying */ 3, new String[] { "M1", "M2", "M3" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testMatchRaceTieBreak",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(0), competitors.get(1)));
        TrackedRace q1M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(2)));
        TrackedRace q1M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(3), competitors.get(5)));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("M1"), q1M1);
        q1Column.setTrackedRace(q1Column.getFleetByName("M2"), q1M2);
        q1Column.setTrackedRace(q1Column.getFleetByName("M3"), q1M3);

        TrackedRace q2M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(1), competitors.get(2)));
        TrackedRace q2M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(3), competitors.get(0)));
        TrackedRace q2M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(5), competitors.get(4)));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("M1"), q2M1);
        q2Column.setTrackedRace(q2Column.getFleetByName("M2"), q2M2);
        q2Column.setTrackedRace(q2Column.getFleetByName("M3"), q2M3);

        TrackedRace q3M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(2), competitors.get(0)));
        TrackedRace q3M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(5), competitors.get(1)));
        TrackedRace q3M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(3)));
        RaceColumn q3Column = qualificationSeries.getRaceColumnByName("Q3");
        q3Column.setTrackedRace(q1Column.getFleetByName("M1"), q3M1);
        q3Column.setTrackedRace(q1Column.getFleetByName("M2"), q3M2);
        q3Column.setTrackedRace(q1Column.getFleetByName("M3"), q3M3);

        // point sums for competitors 0..5: 1, 1, 1, 2, 2, 2
        //
        // For 0..2 we have a cycle: (0)<(1)<(2)<(0), so they shall be treated equal, sorted by name
        // Further, we have: (5)<(4)<(3)<(5), so this is another cycle over the remaining three competitors
        // and               (3)          <   (0)
        // and               (5)          <   (1)  connect the chains
        // which could be resolved by comparing all elements of {(3), (4), (5)} lesser than all elements of {(0), (1), (2)};
        // had the links between the chains been inconsistent then all elements of both chains would have to compare equal,
        // resorting to name comparison
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(Arrays.asList(competitors.get(3), competitors.get(4), competitors.get(5)),
                Arrays.asList(Util.get(rankedCompetitors, 0), Util.get(rankedCompetitors, 1), Util.get(rankedCompetitors, 2)));
        assertEquals(Arrays.asList(competitors.get(0), competitors.get(1), competitors.get(2)),
                Arrays.asList(Util.get(rankedCompetitors, 3), Util.get(rankedCompetitors, 4), Util.get(rankedCompetitors, 5)));
    }

    /**
     * Match races want to prefer a competitor over another with equal score sum based on the direct
     * comparison. When the two competitors matched each other, the result(s) of these matches are to
     * be considered in isolation to break a tie. If this score sum is still equal, the last of these
     * matches shall take precedence, if any.<p>
     *
     * If more than two competitors have equal score sum the comparator needs to ensure that all those
     * competitors can be put in a consistent order. This may not always be possible. Consider three
     * competitors A, B, and C such that A won against B, B won against C and C won against A, but all
     * three having equal total scores. In this case, no consistent ordering of these three competitors
     * is possible, and all three need to be ranked equal.
     */
    @Test
    public void testMatchRaceTieBreakWithPartialCycle() {
        List<Competitor> competitors = createCompetitors(6);
        Regatta regatta = createRegatta(/* qualifying */ 3, new String[] { "M1", "M2", "M3" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testMatchRaceTieBreak",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(0), competitors.get(1)));
        TrackedRace q1M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(2)));
        TrackedRace q1M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(3), competitors.get(5)));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("M1"), q1M1);
        q1Column.setTrackedRace(q1Column.getFleetByName("M2"), q1M2);
        q1Column.setTrackedRace(q1Column.getFleetByName("M3"), q1M3);

        TrackedRace q2M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(1), competitors.get(2)));
        TrackedRace q2M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(3), competitors.get(0)));
        TrackedRace q2M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(5)));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("M1"), q2M1);
        q2Column.setTrackedRace(q2Column.getFleetByName("M2"), q2M2);
        q2Column.setTrackedRace(q2Column.getFleetByName("M3"), q2M3);

        TrackedRace q3M1 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(2), competitors.get(0)));
        TrackedRace q3M2 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(5), competitors.get(1)));
        TrackedRace q3M3 = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors.get(4), competitors.get(3)));
        RaceColumn q3Column = qualificationSeries.getRaceColumnByName("Q3");
        q3Column.setTrackedRace(q1Column.getFleetByName("M1"), q3M1);
        q3Column.setTrackedRace(q1Column.getFleetByName("M2"), q3M2);
        q3Column.setTrackedRace(q1Column.getFleetByName("M3"), q3M3);

        // point sums for competitors 0..5: 1, 1, 1, 2, 3, 1
        //
        // For 0..2 we have a cycle: (0)<(1)<(2)<(0), so they shall be treated equal, sorted by name
        // (5) has equal points with (0), (1), and (2) but we have (5)<(1) (based on q3M2), so the
        // "single-element chain" (5) is connected to the cycle and is "less" than the cycle.
        // Further, we have: (4)<(3)<(all others) based on points (high point!)
        // So the expected order is: (4), (3), (5), (0), (1), (2) (where the last three elements' order is resolved by their name)
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(Arrays.asList(competitors.get(4), competitors.get(3), competitors.get(5), competitors.get(0), competitors.get(1), competitors.get(2)),
                Util.asList(rankedCompetitors));
    }

    /**
     * Regarding bug 961, test scoring in a leaderboard that has a qualification series with two unordered groups where for one
     * column only one group has raced (expressed by a mocked TrackedRace attached to the column). Those who already raced their
     * second race will have one discard, the others won't. Test this.
     */
    @Test
    public void testDiscardsForUnorderedGroupsWithOneGroupNotHavingRacedInAColumn() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "Yellow", "Blue" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testDiscardsForUnorderedGroupsWithOneGroupNotHavingRacedInAColumn",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[]{2}); // one discard for two or more races
        Series qualificationSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        qualificationSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace q1Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 5));
        TrackedRace q1Blue = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(5, 10));
        RaceColumn q1Column = qualificationSeries.getRaceColumnByName("Q1");
        q1Column.setTrackedRace(q1Column.getFleetByName("Yellow"), q1Yellow);
        q1Column.setTrackedRace(q1Column.getFleetByName("Blue"), q1Blue);
        // now add one race for yellow fleet and test that there are no discards still because blue fleet is still missing its race for Q2
        TrackedRace q2Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(3, 8));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("Yellow"), q2Yellow);
        for (Competitor competitor : competitors.subList(3, 8)) {
            assertTrue(leaderboard.isDiscarded(competitor, q1Column, later) || leaderboard.isDiscarded(competitor, q2Column, later),
                    "Competitor "+competitor+" has no discard but should have one");
        }
        for (Competitor competitor : Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                competitors.get(8), competitors.get(9) })) {
            assertFalse(leaderboard.isDiscarded(competitor, q1Column, later) || leaderboard.isDiscarded(competitor, q2Column, later),
                    "Competitor "+competitor+" has a discard but should'nt have one");
        }
        // now add a tracked race for the blue fleet for Q2 and assert that all competitors have one discard
        TrackedRace q2Blue = new MockedTrackedRaceWithStartTimeAndRanks(now,
                Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                        competitors.get(8), competitors.get(9) }));
        q2Column.setTrackedRace(q2Column.getFleetByName("Blue"), q2Blue);
        for (Competitor competitor : competitors) {
            assertTrue(leaderboard.isDiscarded(competitor, q1Column, later) || leaderboard.isDiscarded(competitor, q2Column, later),
                    "Competitor "+competitor+" has no discard but should");
        }
    }

    /**
     * Regarding bug 961, test scoring in a leaderboard that has a qualification series with two ordered groups where for one
     * column only one group has raced (expressed by a mocked TrackedRace attached to the column). Ensure that the competitors in
     * that column get their discard.
     */
    @Test
    public void testDiscardsForOrderedGroupsWithOneGroupNotHavingRacedInAColumn() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */ 0, new String[] { "Default" }, /* final */2,
                new String[] { "Gold", "Silver" },
                /* medal */false, /* medal */ 0, "testDiscardsForOrderedGroupsWithOneGroupNotHavingRacedInAColumn",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[]{2}); // one discard for two or more races
        Series finalSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        seriesIter.next();
        finalSeries = seriesIter.next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 5));
        TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(5, 10));
        RaceColumn f1Column = finalSeries.getRaceColumnByName("F1");
        f1Column.setTrackedRace(f1Column.getFleetByName("Gold"), f1Gold);
        f1Column.setTrackedRace(f1Column.getFleetByName("Silver"), f1Silver);
        // now add one race for yellow fleet and test that there are no discards still because blue fleet is still missing its race for Q2
        TrackedRace f2Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 5));
        RaceColumn f2Column = finalSeries.getRaceColumnByName("F2");
        f2Column.setTrackedRace(f2Column.getFleetByName("Gold"), f2Gold);
        for (int i=0; i<5; i++) {
            assertTrue(leaderboard.isDiscarded(competitors.get(i), f1Column, later) ||
            leaderboard.isDiscarded(competitors.get(i), f2Column, later),
                    "Competitor "+competitors.get(i)+" has no discard in F1 or F2 but should");
        }
        for (int i=5; i<10; i++) {
            assertFalse(leaderboard.isDiscarded(competitors.get(i), f1Column, later) ||
            leaderboard.isDiscarded(competitors.get(i), f2Column, later),
                    "Competitor "+competitors.get(i)+" has a discard in F1 or F2 but shouldn't");
        }
    }

    /**
     * Asserts that the competitors ranking worse than the disqualified competitor advance by one in the
     * {@link Leaderboard#getCompetitorsFromBestToWorst(TimePoint)} ordering. Note that this does not test
     * the net points given for those competitors.
     */
    @Test
    public void testOneStartedRaceWithDifferentScoresAndDisqualification() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testOneStartedRaceWithDifferentScoresAndDisqualification",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Series finalSeries;
        Iterator<? extends Series> seriesIter = regatta.getSeries().iterator();
        seriesIter.next();
        finalSeries = seriesIter.next();
        leaderboard.getScoreCorrection().setMaxPointsReason(competitors.get(5), finalSeries.getRaceColumnByName("F1"), MaxPointsReason.DSQ);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace f1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        f1Column.setTrackedRace(f1Column.getFleets().iterator().next(), f1);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors.subList(0, 5), Util.subList(rankedCompetitors, 0, 5));
        assertEquals(competitors.subList(6, 10), Util.subList(rankedCompetitors, 5, 9));
        assertEquals(competitors.get(5), Util.get(rankedCompetitors, 9));

        // Now test the net points and make sure the other competitors advanced by one, too
        assertEquals(11, leaderboard.getNetPoints(competitors.get(5), f1Column, now), 0.000000001);
        for (int i=0; i<5; i++) {
            assertEquals(i+1, leaderboard.getNetPoints(competitors.get(i), f1Column, now), 0.000000001);
        }
        for (int i=6; i<10; i++) {
            assertEquals(i, leaderboard.getNetPoints(competitors.get(i), f1Column, now), 0.000000001);
        }
    }

    @Test
    public void testDistributionAcrossQualifyingFleetsWithDifferentScores() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        List<Competitor> yellow = new ArrayList<Competitor>();
        List<Competitor> blue = new ArrayList<Competitor>();
        for (int i=0; i<5; i++) {
            yellow.add(competitors.get(2*i));
            blue.add(competitors.get(2*i+1));
        }
        Regatta regatta = createRegatta(/* qualifying */1, new String[] { "Yellow", "Blue" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testDistributionAcrossQualifyingFleetsWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn q1Column = series.get(0).getRaceColumnByName("Q1");
        TrackedRace q1Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
        q1Column.setTrackedRace(q1Column.getFleetByName("Yellow"), q1Yellow);
        TrackedRace q1Blue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
        q1Column.setTrackedRace(q1Column.getFleetByName("Blue"), q1Blue);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            Competitor first = Util.get(rankedCompetitors, 2*i);
            Competitor second = Util.get(rankedCompetitors, 2*i+1);
            assertTrue(first == yellow.get(i) || first == blue.get(i));
            assertTrue(second == yellow.get(i) || second == blue.get(i));
        }
    }

    @Test
    public void testSimpleLeaderboardWithHighPointScoringScheme() throws NoWindException {
        final int NUMBER_OF_COMPETITORS = 10;
        List<Competitor> competitors = createCompetitors(NUMBER_OF_COMPETITORS);
        List<Competitor> gold = new ArrayList<Competitor>();
        List<Competitor> silver = new ArrayList<Competitor>();
        for (int i=0; i<5; i++) {
            gold.add(competitors.get(2*i));
            silver.add(competitors.get(2*i+1));
        }
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1, new String[] {
                "Gold", "Silver" },
                /* medal */false, /* medal */ 0, "testSimpleLeaderboardWithHighPointScoringScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("ESS40", /* typicallyStartsUpwind */false),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
        f1Column.setTrackedRace(f1Column.getFleetByName("Gold"), f1Gold);
        TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, silver);
        f1Column.setTrackedRace(f1Column.getFleetByName("Silver"), f1Silver);
        int rank=1;
        for (Competitor goldCompetitor : gold) {
            assertEquals(rank, f1Column.getTrackedRace(goldCompetitor).getRank(goldCompetitor, later));
            assertEquals(rank, leaderboard.getTrackedRank(goldCompetitor, f1Column, later));
            assertEquals(NUMBER_OF_COMPETITORS/2+1-rank, leaderboard.getTotalPoints(goldCompetitor, f1Column, later), 0.00000001);
            rank++;
        }
        rank=1;
        for (Competitor silverCompetitor : silver) {
            assertEquals(rank, f1Column.getTrackedRace(silverCompetitor).getRank(silverCompetitor, later));
            assertEquals(rank, leaderboard.getTrackedRank(silverCompetitor, f1Column, later));
            assertEquals(NUMBER_OF_COMPETITORS/2+1-rank, leaderboard.getTotalPoints(silverCompetitor, f1Column, later), 0.00000001);
            rank++;
        }
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertSame(gold.get(i), Util.get(rankedCompetitors, i));
            assertSame(silver.get(i), Util.get(rankedCompetitors, i+5));
        }
    }

    @Test
    public void testHighPointMatchRacingScoringScheme() throws NoWindException {
        List<Competitor> competitors = createCompetitors(2);
        Competitor c1 = competitors.get(0);
        Competitor c2 = competitors.get(1);
        Regatta regatta = createSimpleRegatta(3, "Match Racing Test",
                DomainFactory.INSTANCE.getOrCreateBoatClass("ESS40", /* typicallyStartsUpwind */false),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_MATCH_RACING));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn r1Column = series.get(0).getRaceColumnByName("R1");
        TrackedRace r1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        r1Column.setTrackedRace(r1Column.getFleetByName("Default"), r1);

        assertEquals(1, r1Column.getTrackedRace(c1).getRank(c1, later));
        assertEquals(1, leaderboard.getTrackedRank(c1, r1Column, later));
        assertEquals(1, leaderboard.getTotalPoints(c1, r1Column, later), 0.00000001);

        assertEquals(2, r1Column.getTrackedRace(c2).getRank(c2, later));
        assertEquals(2, leaderboard.getTrackedRank(c2, r1Column, later));
        assertEquals(0, leaderboard.getTotalPoints(c2, r1Column, later), 0.00000001);

        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertSame(c1, Util.get(rankedCompetitors, 0));
        assertSame(c2, Util.get(rankedCompetitors, 1));
    }

    @Test
    public void testDistributionAcrossFinalFleetsWithDifferentScores() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        List<Competitor> gold = new ArrayList<Competitor>();
        List<Competitor> silver = new ArrayList<Competitor>();
        for (int i=0; i<5; i++) {
            gold.add(competitors.get(2*i));
            silver.add(competitors.get(2*i+1));
        }
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1, new String[] {
                "Gold", "Silver" },
                /* medal */false, /* medal */ 0, "testDistributionAcrossFinalFleetsWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
        f1Column.setTrackedRace(f1Column.getFleetByName("Gold"), f1Gold);
        TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, silver);
        f1Column.setTrackedRace(f1Column.getFleetByName("Silver"), f1Silver);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertSame(gold.get(i), Util.get(rankedCompetitors, i));
            assertSame(silver.get(i), Util.get(rankedCompetitors, i+5));
        }
    }

    @Test
    public void testMedalTakesPrecedence() throws NoWindException {
        final int firstMedalCompetitorIndex = 3;
        List<Competitor> competitors = createCompetitors(20);
        List<Competitor> medal = competitors.subList(firstMedalCompetitorIndex, firstMedalCompetitorIndex+10);
        Regatta regatta = createRegatta(/* qualifying */ 0, new String[] { "Default" }, /* final */ 1, new String[] { "Default" },
                /* medal */ true, /* medal */ 1, "testMedalTakesPrecedence",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */ true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        TrackedRace q1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        f1Column.setTrackedRace(f1Column.getFleetByName("Default"), q1Default);
        TrackedRace medalTrackedRace = new MockedTrackedRaceWithStartTimeAndRanks(now, medal);
        RaceColumn medalColumn = series.get(2).getRaceColumnByName("M");
        medalColumn.setTrackedRace(medalColumn.getFleetByName("Medal"), medalTrackedRace);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        // medalists rank top
        for (int i=0; i<10; i++) {
            assertSame(medal.get(i), Util.get(rankedCompetitors, (i)));
        }
        // others rank according to their non-medal ranking in the final round
        for (int i=10; i<competitors.size(); i++) {
            if (i<10+firstMedalCompetitorIndex) {
                assertSame(competitors.get(i-10), Util.get(rankedCompetitors, i));
            } else {
                assertSame(competitors.get(i), Util.get(rankedCompetitors, i));
            }
        }
    }

    @Test
    public void testLastRaceTakesPrecedenceWithHighPointLastBreaksTieScheme() throws NoWindException {
        List<Competitor> competitors = createCompetitors(20);
        Regatta regatta = createRegatta(/* qualifying */ 0, new String[] { "Default" }, /* final */ 2, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testLastRaceTakesPrecedenceWithHighPointLastBreaksTieScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */ true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_LAST_BREAKS_TIE));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        TrackedRace f1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        f1Column.setTrackedRace(f1Column.getFleetByName("Default"), f1Default);
        List<Competitor> reversedCompetitors = new ArrayList<Competitor>(competitors);
        Collections.reverse(reversedCompetitors);
        RaceColumn f2Column = series.get(1).getRaceColumnByName("F2");
        TrackedRace f2Default = new MockedTrackedRaceWithStartTimeAndRanks(now, reversedCompetitors);
        f2Column.setTrackedRace(f2Column.getFleetByName("Default"), f2Default);

        // assert that all competitors have equal points now
        Competitor firstCompetitor = competitors.iterator().next();
        for (Competitor competitor : competitors) {
            assertEquals(leaderboard.getNetPoints(firstCompetitor, later), leaderboard.getNetPoints(competitor, later));
        }
        // assert that the ordering of competitors equals that of the last race
        assertEquals(reversedCompetitors, Util.asList(leaderboard.getCompetitorsFromBestToWorst(later)));
    }

    @Test
    public void testTotalTimeNotCountedForRacesStartedLaterThanTimePointRequested() {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint earlier = now.minus(1000000);
        TimePoint later = now.plus(1000000); // first race from "earlier" to "now", second from "now" to "later", third from "later" to "finish"
        TimePoint finish = later.plus(1000000);
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f2 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f3 = new Competitor[] { c[1], c[2], c[0] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */3, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTotalTimeNotCountedForRacesStartedLaterThanTimePointReqeusted",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
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
                new Competitor[][] { f1, f2, f3 }, new TimePoint[] { earlier, now, later }, lastMarkPassingTimesForCompetitors);
        Duration totalTimeSailedC0_InRace1 = leaderboard.getTotalTimeSailed(c[0], earlier.plus(1000));
        assertEquals(1000l, totalTimeSailedC0_InRace1.asMillis());
        Duration totalTimeSailedC0_InRace2 = leaderboard.getTotalTimeSailed(c[0], now.plus(1000));
        assertEquals(now.asMillis()-earlier.asMillis() + 1000, totalTimeSailedC0_InRace2.asMillis());
        Duration totalTimeSailedC0_InRace3 = leaderboard.getTotalTimeSailed(c[0], later.plus(1000));
        assertEquals(later.asMillis()-earlier.asMillis() + 1000, totalTimeSailedC0_InRace3.asMillis());
        Duration totalTimeSailedC0_AtEndOfRace3 = leaderboard.getTotalTimeSailed(c[0], finish);
        assertEquals(finish.asMillis()-earlier.asMillis(), totalTimeSailedC0_AtEndOfRace3.asMillis());
        Duration totalTimeSailedC0_AfterRace3 = leaderboard.getTotalTimeSailed(c[0], finish.plus(1000));
        assertEquals(finish.asMillis()-earlier.asMillis(), totalTimeSailedC0_AfterRace3.asMillis());
    }

    /**
     * See bug 3755
     */
    @Test
    public void testSuppressionAffectsInRaceRankForLowPoint() {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint earlier = now.minus(1000000);
        TimePoint later = now.plus(1000000); // first race from "earlier" to "now", second from "now" to "later", third from "later" to "finish"
        TimePoint finish = later.plus(1000000);
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */3, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTotalTimeNotCountedForRacesStartedLaterThanTimePointReqeusted",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
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
        leaderboard.setSuppressed(c[1], true);
        assertEquals(1.0, leaderboard.getTotalPoints(c[0], leaderboard.getRaceColumns().iterator().next(), later), 0.000001);
        assertEquals(2.0, leaderboard.getTotalPoints(c[2], leaderboard.getRaceColumns().iterator().next(), later), 0.000001);
    }

    /**
     * See bug 3755
     */
    @Test
    public void testSuppressionAffectsInRaceRankForHighPoint() {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint earlier = now.minus(1000000);
        TimePoint later = now.plus(1000000); // first race from "earlier" to "now", second from "now" to "later", third from "later" to "finish"
        TimePoint finish = later.plus(1000000);
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */3, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTotalTimeNotCountedForRacesStartedLaterThanTimePointReqeusted",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_TEN));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
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
        leaderboard.setSuppressed(c[1], true);
        assertEquals(10.0, leaderboard.getTotalPoints(c[0], leaderboard.getRaceColumns().iterator().next(), later), 0.000001);
        assertEquals( 9.0, leaderboard.getTotalPoints(c[2], leaderboard.getRaceColumns().iterator().next(), later), 0.000001);
    }

    @Test
    public void testTieBreakWithTwoVersusOneWins() throws NoWindException {
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f2 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f3 = new Competitor[] { c[1], c[2], c[0] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */3, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTwoVersusOneWins",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2, f3);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertEquals(Arrays.asList(new Competitor[] { c[0], c[1], c[2] }), Util.asList(rankedCompetitors));
    }

    @Test
    public void testTieBreakWithTwoVersusOneSeconds() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] }; // c[0] scores 18 points altogether
        Competitor[] f2 = new Competitor[] { c[2], c[0], c[1], c[3] }; // c[1] scores 18 points altogether but has only one second rank
        Competitor[] f3 = new Competitor[] { c[2], c[1], c[0], c[3] };
        Competitor[] f4 = new Competitor[] { c[3], c[2], c[0], c[1] };
        Competitor[] f5 = new Competitor[] { c[3], c[2], c[1], c[0] };
        Competitor[] f6 = new Competitor[] { c[3], c[2], c[1], c[0] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */6, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2, f3, f4, f5, f6);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertTrue(Util.indexOf(rankedCompetitors, c[0]) == Util.indexOf(rankedCompetitors, c[1])-1);
    }

    /**
     * Tests the {@link LowPointTieBreakBasedOnLastSeriesOnly} scoring scheme's tie breaking rule by setting up a
     * leaderboard with a qualification and a final series such that the competitors have equal points but different
     * number of wins in the final series, leading to a different order than it would if sorting by the total number
     * of wins.
     */
    @Test
    public void testTieBreakBasedOnLastNonMedalSeriesNoMedal() throws NoWindException {
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] q1 = new Competitor[] { c[1], c[0], c[2] }; // c[0] wins no race in the qualification, c[1] wins two,
        Competitor[] q2 = new Competitor[] { c[1], c[0], c[2] }; // but that doesn't help c[1] because c[0] wins one in the final
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1] }; // where c[1] wins none. The tie is broken in favor of c[0].
        Competitor[] f2 = new Competitor[] { c[0], c[1], c[2] };
        Regatta regatta = createRegatta(/* qualifying */2, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, q1, q2);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertTrue(leaderboard.getNetPoints(c[0], later) < leaderboard.getNetPoints(c[2], later));
        assertTrue(Util.indexOf(rankedCompetitors, c[0]) < Util.indexOf(rankedCompetitors, c[1]));
    }

    /**
     * Tests the {@link LowPointTieBreakBasedOnLastSeriesOnly} scoring scheme's tie breaking rule. c[0] and c[2] have
     * equal scores after excluding their worst score each. They both exclude a score from the final series. c[1]'s and
     * c[3]'s net points sum is worse than that of c[0] and c[2], so c[1] is not tied. Comparing c[0]'s and c[2]'s final
     * series <em>including the excluded ones</em> breaks the tie because both have won one race in the final series,
     * and c[0] scores a 3.0 in F1, c[2] only scores a 4.0 in F2, so c[0] wins.
     */
    @Test
    public void testTieBreakBasedIncludingDiscardInFinalSeries() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] q1 = new Competitor[] { c[0], c[1], c[2], c[3] };
        Competitor[] q2 = new Competitor[] { c[2], c[0], c[1], c[3] };
        Competitor[] q3 = new Competitor[] { c[2], c[0], c[1], c[3] };
        Competitor[] f1 = new Competitor[] { c[2], c[1], c[0], c[3] }; // c[0] discards this race, but it doesn't matter: the scoring scheme also considers discarded scores
        Competitor[] f2 = new Competitor[] { c[0], c[1], c[3], c[2] }; // c[2] discards this race, but the scoring scheme also uses the discarded scores
        Regatta regatta = createRegatta(/* qualifying */3, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY));
        Leaderboard leaderboard = createLeaderboard(regatta,
                /* discarding thresholds: one discard when four races have been completed */ new int[] { 4 });
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, q1, q2, q3);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[2], later), 0.000000001);
        assertTrue(Util.indexOf(rankedCompetitors, c[0]) < Util.indexOf(rankedCompetitors, c[2]));
    }

    /**
     * Tests the {@link LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal} scoring scheme's
     * advancement rule. c[0] and c[2] have equal scores after excluding their worst score each. They both exclude a
     * score from the final series. c[1]'s and c[3]'s net points sum is worse than that of c[0] and c[2], so c[1] is not
     * tied. Comparing c[0]'s and c[2]'s final series <em>including the excluded ones</em> breaks the tie because both
     * have won one race in the final series, and c[0] scores a 3.0 in F1, c[2] only scores a 4.0 in F2, so c[0] wins.
     */
    @Test
    public void testRankingSurfersAdvancedToSemiAndGrandFinalBetterThanQuarterFinalists() throws NoWindException {
        Competitor[] c = createCompetitors(13).toArray(new Competitor[0]);
        Competitor[] o1 = new Competitor[] { c[3], c[1], c[2], /* QF START */ c[0], c[5], c[7], c[4], c[6], c[12], c[11] /* QF END */ , c[10], c[8], c[9] }; // a somewhat random order at the end of the opening series
        Competitor[] qf = new Competitor[] { c[11], c[6], c[0], c[4], c[7], c[5], c[12] }; // seven competitors sail the quarter final
        Competitor[] sf = new Competitor[] { c[2], c[11], c[6], c[1] }; // the best two quarter-finalists meet the second and third from the opening series
        Competitor[] gf = new Competitor[] { c[11], c[3], c[2] }; // the two best-ranking from the semi-final meet the winner of the opening series
        Regatta regatta = createRegatta(/* opening */3, new String[] { "Default" }, /* final */ 0, /* no final fleets */ null,
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATING_MEDAL_SERIES_PROMOTING_ONE_TO_FINAL_AND_TWO_TO_SEMIFINAL));
        final SeriesImpl qfSeries = new SeriesImpl("Quarter Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("QF"), /* trackedRegattaRegistry */ null);
        qfSeries.setStartsWithZeroScore(true);
        qfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(qfSeries);
        series.add(qfSeries);
        final SeriesImpl sfSeries = new SeriesImpl("Semi Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("SF"), /* trackedRegattaRegistry */ null);
        sfSeries.setStartsWithZeroScore(true);
        sfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(sfSeries);
        series.add(sfSeries);
        final SeriesImpl gfSeries = new SeriesImpl("Grand Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("GF"), /* trackedRegattaRegistry */ null);
        gfSeries.setStartsWithZeroScore(true);
        gfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(gfSeries);
        series.add(gfSeries);
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, o1);
        assertEquals(Arrays.asList(o1), Util.asList(leaderboard.getCompetitorsFromBestToWorst(TimePoint.now())));
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, qf);
        Iterable<Competitor> rankedCompetitorsAfterQF = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(Arrays.asList(/* ADVANCED BEYOND QF: */ c[3], c[1], c[2], /* Quarter Finalists: */ c[11], c[6], c[0], c[4], c[7], c[5], c[12], /* Not qualified to QF: */ c[10], c[8], c[9]), Util.asList(rankedCompetitorsAfterQF));
        TimePoint evenLater = createAndAttachTrackedRaces(series.get(2), "Default", /* withScores */ true, sf);
        Iterable<Competitor> rankedCompetitorsAfterSF = leaderboard.getCompetitorsFromBestToWorst(evenLater);
        assertEquals(Arrays.asList(/* ADVANCED BEYOND SF: */ c[3], /* Semi Finalists */ c[2], c[11], c[6], c[1], /* Quarter Finalists not in SF: */ c[0], c[4], c[7], c[5], c[12], /* Not qualified to QF: */ c[10], c[8], c[9]), Util.asList(rankedCompetitorsAfterSF));
        TimePoint atEnd = createAndAttachTrackedRaces(series.get(3), "Default", /* withScores */ true, gf);
        Iterable<Competitor> rankedCompetitorsAfterGF = leaderboard.getCompetitorsFromBestToWorst(atEnd);
        assertEquals(Arrays.asList(c[11], c[3], c[2], /* Semi Finalists not in GF*/ c[6], c[1], /* Quarter Finalists not in SF: */ c[0], c[4], c[7], c[5], c[12], /* Not qualified to QF: */ c[10], c[8], c[9]), Util.asList(rankedCompetitorsAfterGF));
    }

    /**
     * Tests the {@link LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal} scoring scheme's
     * tie-breaking rule for ties occurring in a medal series. When two competitors are tied in a medal series
     * then the tie shall be resolved based on the ranking after the previous medal series, or if already in the
     * first medal series then by the rank at the end of the opening series.
     */
    @Test
    public void testRankingSurfersTiedInGrandFinalBasedOnPreviousSeries() throws NoWindException {
        Competitor[] c = createCompetitors(13).toArray(new Competitor[0]);
        Competitor[] o1 = new Competitor[] { c[3], c[1], c[2], /* QF START */ c[0], c[5], c[7], c[4], c[6], c[12], c[11] /* QF END */ , c[10], c[8], c[9] }; // a somewhat random order at the end of the opening series
        Competitor[] qf = new Competitor[] { c[6], c[11], c[0], c[4], c[7], c[5], c[12] }; // seven competitors sail the quarter final
        Competitor[] sf = new Competitor[] { c[11], c[2], c[6], c[1] }; // the best two quarter-finalists meet the second and third from the opening series
        Competitor[] gf = new Competitor[] { c[11], c[3], c[2] }; // the two best-ranking from the semi-final meet the winner of the opening series
        Regatta regatta = createRegatta(/* opening */3, new String[] { "Default" }, /* final */ 0, /* no final fleets */ null,
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATING_MEDAL_SERIES_PROMOTING_ONE_TO_FINAL_AND_TWO_TO_SEMIFINAL));
        final SeriesImpl qfSeries = new SeriesImpl("Quarter Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("QF"), /* trackedRegattaRegistry */ null);
        qfSeries.setStartsWithZeroScore(true);
        qfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(qfSeries);
        series.add(qfSeries);
        final SeriesImpl sfSeries = new SeriesImpl("Semi Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("SF"), /* trackedRegattaRegistry */ null);
        sfSeries.setStartsWithZeroScore(true);
        sfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(sfSeries);
        series.add(sfSeries);
        final SeriesImpl gfSeries = new SeriesImpl("Grand Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("GF"), /* trackedRegattaRegistry */ null);
        gfSeries.setStartsWithZeroScore(true);
        gfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(gfSeries);
        series.add(gfSeries);
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, o1);
        createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, qf);
        createAndAttachTrackedRaces(series.get(2), "Default", /* withScores */ true, sf);
        TimePoint atEnd = createAndAttachTrackedRaces(series.get(3), "Default", /* withScores */ true, gf);
        leaderboard.getScoreCorrection().correctScore(c[11], gfSeries.getRaceColumns().iterator().next(), 4.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[11], gfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        leaderboard.getScoreCorrection().correctScore(c[3], gfSeries.getRaceColumns().iterator().next(), 4.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[3], gfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        leaderboard.getScoreCorrection().correctScore(c[2], gfSeries.getRaceColumns().iterator().next(), 4.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[2], gfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        Iterable<Competitor> rankedCompetitorsAfterGF = leaderboard.getCompetitorsFromBestToWorst(atEnd);
        assertEquals(Arrays.asList(/* Grand Finalists' tie broken based on Semi Final: */ c[3], c[11], c[2],
                                   /* Semi Finalists not in GF*/ c[6], c[1],
                                   /* Quarter Finalists not in SF: */ c[0], c[4], c[7], c[5], c[12],
                                   /* Not qualified to QF: */ c[10], c[8], c[9]), Util.asList(rankedCompetitorsAfterGF));
    }

    /**
     * Tests the {@link LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal} scoring scheme's
     * tie-breaking rule for ties occurring in the first medal series. When two competitors are tied in the first medal series
     * then the tie shall be resolved based on the ranking after the opening series.
     */
    @Test
    public void testRankingSurfersTiedInQuarterFinalBasedOnOpeningSeries() throws NoWindException {
        Competitor[] c = createCompetitors(13).toArray(new Competitor[0]);
        Competitor[] o1 = new Competitor[] { c[3], c[1], c[2], /* QF START */ c[0], c[5], c[7], c[4], c[6], c[12], c[11] /* QF END */, c[10], c[8], c[9] }; // a somewhat random order at the end of the opening series
        Competitor[] qf = new Competitor[] { c[6], c[11], c[12], c[4], c[0], c[5], c[7] }; // seven competitors sail the quarter final
        Regatta regatta = createRegatta(/* opening */3, new String[] { "Default" }, /* final */ 0, /* no final fleets */ null,
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATING_MEDAL_SERIES_PROMOTING_ONE_TO_FINAL_AND_TWO_TO_SEMIFINAL));
        final SeriesImpl qfSeries = new SeriesImpl("Quarter Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("QF"), /* trackedRegattaRegistry */ null);
        qfSeries.setStartsWithZeroScore(true);
        qfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(qfSeries);
        series.add(qfSeries);
        final SeriesImpl sfSeries = new SeriesImpl("Semi Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("SF"), /* trackedRegattaRegistry */ null);
        sfSeries.setStartsWithZeroScore(true);
        sfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(sfSeries);
        series.add(sfSeries);
        final SeriesImpl gfSeries = new SeriesImpl("Grand Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("GF"), /* trackedRegattaRegistry */ null);
        gfSeries.setStartsWithZeroScore(true);
        gfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(gfSeries);
        series.add(gfSeries);
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, o1);
        final TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, qf);
        leaderboard.getScoreCorrection().correctScore(c[0], qfSeries.getRaceColumns().iterator().next(), 8.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[0], qfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        leaderboard.getScoreCorrection().correctScore(c[7], qfSeries.getRaceColumns().iterator().next(), 8.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[7], qfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        leaderboard.getScoreCorrection().correctScore(c[12], qfSeries.getRaceColumns().iterator().next(), 8.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[12], qfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        Iterable<Competitor> rankedCompetitorsAfterQF = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(Arrays.asList(/* Grand Finalist and Semi Finalists by advancement: */ c[3], c[1], c[2],
                                   /* Quarter Finalists not BFD: */ c[6], c[11], c[4], c[5],
                                   /* BFD Quarter Finalists based on Opening Series */ c[0], c[7], c[12],
                                   /* not qualified for Quarter Final: */ c[10], c[8], c[9]), Util.asList(rankedCompetitorsAfterQF));
    }

    /**
     * Tests the {@link LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal} scoring scheme's
     * tie-breaking rule for ties occurring in the second medal series. When two competitors are tied in the second medal series
     * then the tie shall be resolved based on the ranking after the first medal series, and if a tie remains then by the opening
     * series.
     */
    @Test
    public void testRankingSurfersTiedInSeimFinalBasedOnQuarterFinalAndOpeningSeries() throws NoWindException {
        Competitor[] c = createCompetitors(13).toArray(new Competitor[0]);
        Competitor[] o1 = new Competitor[] { c[3], c[1], c[2], /* QF START */ c[0], c[5], c[7], c[4], c[11], c[12], c[6] /* QF END */ , c[10], c[8], c[9] }; // a somewhat random order at the end of the opening series
        Competitor[] qf = new Competitor[] { c[6], c[11], c[0], c[4], c[7], c[5], c[12] }; // seven competitors sail the quarter final
        Competitor[] sf = new Competitor[] { c[11], c[2], c[6], c[1] }; // the best two quarter-finalists meet the second and third from the opening series
        Regatta regatta = createRegatta(/* opening */3, new String[] { "Default" }, /* final */ 0, /* no final fleets */ null,
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATING_MEDAL_SERIES_PROMOTING_ONE_TO_FINAL_AND_TWO_TO_SEMIFINAL));
        final SeriesImpl qfSeries = new SeriesImpl("Quarter Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("QF"), /* trackedRegattaRegistry */ null);
        qfSeries.setStartsWithZeroScore(true);
        qfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(qfSeries);
        series.add(qfSeries);
        final SeriesImpl sfSeries = new SeriesImpl("Semi Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("SF"), /* trackedRegattaRegistry */ null);
        sfSeries.setStartsWithZeroScore(true);
        sfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(sfSeries);
        series.add(sfSeries);
        final SeriesImpl gfSeries = new SeriesImpl("Grand Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")), /* raceColumnNames */ Collections.singleton("GF"), /* trackedRegattaRegistry */ null);
        gfSeries.setStartsWithZeroScore(true);
        gfSeries.getRaceColumns().iterator().next().setFactor(1.0);
        regatta.addSeries(gfSeries);
        series.add(gfSeries);
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, o1);
        final TimePoint afterQF = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, qf);
        // let's assume c[6] took a standard penalty of one additional point in the quarter final (e.g., tracker not returned);
        // this makes c[6] tied with c[11] who regularly scored 2.0 points.
        leaderboard.getScoreCorrection().correctScore(c[6], qfSeries.getRaceColumns().iterator().next(), 2.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[6], qfSeries.getRaceColumns().iterator().next(), MaxPointsReason.STP);
        Iterable<Competitor> rankedCompetitorsAfterQF = leaderboard.getCompetitorsFromBestToWorst(afterQF);
        assertEquals(Arrays.asList(/* Leading by opening series results: */ c[3], c[1], c[2],
                                   /* Quarter Finalists with ties broken by opening series */ c[11], c[6], c[0], c[4], c[7], c[5], c[12],
                                   /* Not qualified to QF: */ c[10], c[8], c[9]), Util.asList(rankedCompetitorsAfterQF));
        final TimePoint afterSF = createAndAttachTrackedRaces(series.get(2), "Default", /* withScores */ true, sf);
        // c[6], c[11], and c[1] all take a BFD with 5.0 points in the semi-final; c[1] should rank best among the tied competitors because they were
        // allowed to skip the quarter final due to their good opening series rank; c[6] and c[11] both sailed in the
        // quarter final but were tied, with their tie broken based on the opening series in favor of c[11]
        leaderboard.getScoreCorrection().correctScore(c[6], sfSeries.getRaceColumns().iterator().next(), 5.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[6], sfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        leaderboard.getScoreCorrection().correctScore(c[11], sfSeries.getRaceColumns().iterator().next(), 5.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[11], sfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        leaderboard.getScoreCorrection().correctScore(c[1], sfSeries.getRaceColumns().iterator().next(), 5.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[1], sfSeries.getRaceColumns().iterator().next(), MaxPointsReason.BFD);
        Iterable<Competitor> rankedCompetitorsAfterSF = leaderboard.getCompetitorsFromBestToWorst(afterSF);
        assertEquals(Arrays.asList(/* Winner of Opening Series, advanced to Grand Final: */ c[3],
                                   /* Semi Finalist not penalized */ c[2],
                                   /* Tied Semi Finalist who did not have to sail Quarter Final: */ c[1],
                                   /* Tied Semi Finalists also tied in Quarter Final, ranked by Opening Series: */ c[11], c[6],
                                   /* Remaining Quarter Finalists: */ c[0], c[4], c[7], c[5], c[12],
                                   /* Not qualified to QF: */ c[10], c[8], c[9]), Util.asList(rankedCompetitorsAfterSF));
    }

    /**
     * Tests the {@link LowPointTieBreakBasedOnLastSeriesOnly} scoring scheme's tie breaking rule. c[0] and c[2] have
     * equal scores after excluding their worst score each: c[0] scores 5.0 in the qualification and 4.0 in the final,
     * together 9.0, and excludes as her worst race the 3.0 from the F1 race, ending up with 6.0 net points.
     * c[2] scores 6.0 in the qualification and 4.0 in the final, together 10.0, and excludes as her worst race the
     * 4.0 from the Q1 race, also ending up with 6.0 net points. c[0] excludes a score from the final series, c[2] from the
     * qualification. The rule claims that excluded scores shall still be used for tie-breaking. So from the tie-breaking
     * rule's perspective, both equally score 4.0 points in the final series, hence tie-breaking has to resort to the
     * qualification series where c[0] has one win, c[2] has two, so c[2] shall win this tie break.
     */
    @Test
    public void testTieBreakBasedOnLastNonMedalSeriesNoMedalDiscardsInDifferentSeries() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] q1 = new Competitor[] { c[0], c[1], c[3], c[2] }; // c[2] discards this race with 4.0 points
        Competitor[] q2 = new Competitor[] { c[2], c[0], c[1], c[3] }; //
        Competitor[] q3 = new Competitor[] { c[2], c[0], c[1], c[3] }; // but c[2] has two wins, c[0] only one
        Competitor[] f1 = new Competitor[] { c[2], c[1], c[0], c[3] }; // c[0] discards this race with 3.0 points
        Competitor[] f2 = new Competitor[] { c[0], c[1], c[2], c[3] }; // c[0] and c[2] both have [1.0, 3.0] as scores in the final series
        Regatta regatta = createRegatta(/* qualifying */3, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY));
        Leaderboard leaderboard = createLeaderboard(regatta,
                /* discarding thresholds: one discard when four races have been completed */ new int[] { 4 });
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, q1, q2, q3);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[2], later), 0.000000001);
        assertTrue(Util.indexOf(rankedCompetitors, c[2]) < Util.indexOf(rankedCompetitors, c[0]));
    }

    /**
     * Tests the {@link LowPointTieBreakBasedOnLastSeriesOnly} scoring scheme's tie breaking rule by setting up a
     * leaderboard with a qualification and a final series such that the competitors have equal points but different
     * number of wins in the final series, leading to a different order than it would if sorting by the total number
     * of wins.
     */
    @Test
    public void testTieBreakBasedOnLastNonMedalSeriesNoMedalDecisionByLastRace() throws NoWindException {
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] q1 = new Competitor[] { c[1], c[0], c[2] }; // c[0] and c[1] have equal wins and scores,
        Competitor[] q2 = new Competitor[] { c[0], c[1], c[2] }; // but c[1] has won the last race
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f2 = new Competitor[] { c[1], c[0], c[2] };
        Regatta regatta = createRegatta(/* qualifying */2, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, q1, q2);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertTrue(Util.indexOf(rankedCompetitors, c[1]) < Util.indexOf(rankedCompetitors, c[0])); // based on last race f2
    }

    /**
     * Tests the {@link LowPointTieBreakBasedOnLastSeriesOnly} scoring scheme's tie breaking rule by setting up a
     * leaderboard with a qualification and a final series such that the competitors have equal points, and equal
     * scores in the final series, but one of them having more wins than the other in the qualification. In this
     * case, the qualification counts.
     */
    @Test
    public void testTieBreakBasedOnLastNonMedalSeriesNoMedalDefaultingToQualification() throws NoWindException {
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] q1 = new Competitor[] { c[1], c[0], c[2] }; // c[0] and c[1] have an equal number of wins in the final series and equal total points;
        Competitor[] q2 = new Competitor[] { c[2], c[0], c[1] }; // tie breaking is expected to then take the wins in the qualification series into account
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] }; // where c[1] has one more than c[0]
        Competitor[] f2 = new Competitor[] { c[1], c[0], c[2] }; // tie breaking will have to resort to the qualification, finding the win of c[1]
        Regatta regatta = createRegatta(/* qualifying */2, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesNoMedalDefaultingToQualification",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, q1, q2);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertTrue(Util.indexOf(rankedCompetitors, c[1]) < Util.indexOf(rankedCompetitors, c[0]));
    }

    /**
     * Tests the {@link LowPointTieBreakBasedOnLastSeriesOnly} scoring scheme's tie breaking rule by setting up a
     * leaderboard with a qualification and a final series as well as a medal race such that the competitors have equal
     * points but different numbers of wins in the final series
     */
    @Test
    public void testTieBreakBasedOnLastNonMedalSeriesWithMedal() throws NoWindException {
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] q1 = new Competitor[] { c[1], c[0], c[2] }; // c[0] wins no race in the qualification, c[1] wins two,
        Competitor[] q2 = new Competitor[] { c[1], c[0], c[2] }; // but that doesn't help c[1] because c[0] wins one in the final
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1] }; // where c[1] wins none. The tie is broken in favor of c[0].
        Competitor[] f2 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] m = new Competitor[] { c[0], c[1] }; // c[0] and c[1] got promoted to the medal race, both get disqualified; but in f2 c[2] won, so has to win overall due to count-back
        Regatta regatta = createRegatta(/* qualifying */2, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ true, /* medal */ 1, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesWithMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, q1, q2);
        createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        TimePoint later = createAndAttachTrackedRaces(series.get(2), "Medal", /* withScores */ true, m);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[0], series.get(2).getRaceColumns().iterator().next(), MaxPointsReason.DSQ);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[1], series.get(2).getRaceColumns().iterator().next(), MaxPointsReason.DSQ);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertTrue(Util.indexOf(rankedCompetitors, c[0]) < Util.indexOf(rankedCompetitors, c[1])); // due to more wins in final series
    }

    /**
     * Tests the {@link LowPointTieBreakBasedOnLastSeriesOnly} scoring scheme's tie breaking rule by setting up a
     * leaderboard with a qualification and a final series as well as a medal race such that the competitors have equal
     * points but different numbers of wins in the final series
     */
    @Test
    public void testTieBreakBasedOnLastNonMedalSeriesWithMedalDecisionByLastRace() throws NoWindException {
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] q1 = new Competitor[] { c[1], c[0], c[2] }; // c[0] and c[1] have equal wins and scores,
        Competitor[] q2 = new Competitor[] { c[0], c[1], c[2] }; // but c[1] has won the last race before the medal
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f2 = new Competitor[] { c[1], c[0], c[2] };
        Competitor[] m = new Competitor[] { c[0], c[1] }; // c[0] and c[1] got promoted to the medal race, both get disqualified; but in f2 c[2] won, so has to win overall due to count-back
        Regatta regatta = createRegatta(/* qualifying */2, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ true, /* medal */ 1, "testTieBreakWithTieBreakBasedOnLastNonMedalSeriesWithMedal",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        createAndAttachTrackedRaces(series.get(0), "Default", /* withScores */ true, q1, q2);
        createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        TimePoint later = createAndAttachTrackedRaces(series.get(2), "Medal", /* withScores */ true, m);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[0], series.get(2).getRaceColumns().iterator().next(), MaxPointsReason.DSQ);
        leaderboard.getScoreCorrection().setMaxPointsReason(c[1], series.get(2).getRaceColumns().iterator().next(), MaxPointsReason.DSQ);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertTrue(Util.indexOf(rankedCompetitors, c[1]) < Util.indexOf(rankedCompetitors, c[0])); // due to last non-medal race
    }

    /**
     * Reported by Clemens Fackeldey:<p>
     *
     * <pre>
     *   GER2105: 8/6/(18,DNF)/10/9 => 33
     *   GER2254: 6/10/9/8/(11) => 33
     * </pre>
     * GER2105 must rank better than GER2254 because while the scores are equal when including the discards and
     * considering the discards and ordering the results from best to worst both have 6/8/9/10, the count-back
     * shows that for the last race GER2254 scored worse in the last race (although that was discarded).
     * But RRS A8.2 does not mention discards, so the last race score needs to be considered regardless of
     * whether it was discarded.
     */
    @Test
    public void testTieBreakByCountbackIncludesDiscards() throws NoWindException {
        Competitor[] c = createCompetitors(18).toArray(new Competitor[0]);
        Competitor GER2105 = c[0];
        Competitor GER2254 = c[1];
        Competitor[] f1 = new Competitor[] { c[ 7], c[ 5], c[2], c[3], c[4], c[1], c[6], c[0], c[8], c[9], c[10], c[11], c[12], c[13], c[14], c[15], c[16], c[17] };
        Competitor[] f2 = new Competitor[] { c[ 5], c[ 9], c[2], c[3], c[4], c[0], c[6], c[7], c[8], c[1], c[10], c[11], c[12], c[13], c[14], c[15], c[16], c[17] };
        Competitor[] f3 = new Competitor[] { c[17], c[ 8], c[2], c[3], c[4], c[5], c[6], c[7], c[1], c[9], c[10], c[11], c[12], c[13], c[14], c[15], c[16], c[0] };
        Competitor[] f4 = new Competitor[] { c[ 9], c[ 7], c[2], c[3], c[4], c[5], c[6], c[1], c[8], c[0], c[10], c[11], c[12], c[13], c[14], c[15], c[16], c[17] };
        Competitor[] f5 = new Competitor[] { c[ 8], c[10], c[2], c[3], c[4], c[5], c[6], c[7], c[0], c[9], c[ 1], c[11], c[12], c[13], c[14], c[15], c[16], c[17] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */6, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakByCountbackIncludesDiscards",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[] { 4 });
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2, f3, f4, f5);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(33.0, leaderboard.getNetPoints(GER2105, later), 0.0000001);
        assertEquals(33.0, leaderboard.getNetPoints(GER2254, later), 0.0000001);
        assertTrue(Util.indexOf(rankedCompetitors, GER2105) == Util.indexOf(rankedCompetitors, GER2254)-1);
    }

    @Test
    public void testBasicElminationScoringScheme() throws NoWindException {
        Regatta regatta = createRegattaWithEliminations(1, new int[] { 8, 4, 2, 2 }, "testBasicElminationScoringScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Competitor[] c = createCompetitors(64).toArray(new Competitor[64]);
        // first round with 64 competitors, eight per heat:
        Competitor[][] competitorsForHeatsInRound1 = new Competitor[8][];
        TimePoint later = null;
        for (int heat=0; heat<8; heat++) {
            competitorsForHeatsInRound1[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInRound1[heat][i] = c[8*heat+i];
            }
            later = createAndAttachTrackedRaces(series.get(0), "Heat "+(heat+1), /* withScores */ true, competitorsForHeatsInRound1[heat]);
        }
        // quarter-finals has promoted top four competitors of first round in heats with eight competitors each:
        Competitor[][] competitorsForHeatsInQuarterFinals = new Competitor[4][];
        for (int heat=0; heat<4; heat++) {
            competitorsForHeatsInQuarterFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInQuarterFinals[heat][i] = c[8*(2*heat+(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(1), "Heat "+(heat+9), /* withScores */ true, competitorsForHeatsInQuarterFinals[heat]);
        }
        // semi-finals has promoted top four competitors of quarter finals which are the top four of each other first-round heat:
        Competitor[][] competitorsForHeatsInSemiFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInSemiFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInSemiFinals[heat][i] = c[8*(4*heat+2*(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(2), "Heat "+(heat+13), /* withScores */ true, competitorsForHeatsInSemiFinals[heat]);
        }
        // finals has promoted top four competitors of semi finals which are the top four of first and fifth first-round heats
        // for the final, and the top four of the first round's third and seventh heat
        Competitor[][] competitorsForHeatsInFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInFinals[heat][i] = c[8*(2*heat+4*(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(3), "Heat "+(heat+15), /* withScores */ true, competitorsForHeatsInFinals[heat]);
        }
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertSame(c[0], Util.get(rankedCompetitors, 0)); // should be the winner of the final round's Final heat and take the "crown" for the elimination
        assertEquals(0.7, leaderboard.getNetPoints(c[0], later), 0.000000001);
        assertSame(c[1], Util.get(rankedCompetitors, 1)); // should be the winner of the final round's Final heat and take the "crown" for the elimination
        assertEquals(2, leaderboard.getNetPoints(c[1], later), 0.000000001);
        // first four of second heat get promoted to quarter final but lose their heat
        assertNull(leaderboard.getNetPoints(c[ 8], series.get(0).getRaceColumns().iterator().next(), later));
        assertNull(leaderboard.getNetPoints(c[ 9], series.get(0).getRaceColumns().iterator().next(), later));
        assertNull(leaderboard.getNetPoints(c[10], series.get(0).getRaceColumns().iterator().next(), later));
        assertNull(leaderboard.getNetPoints(c[11], series.get(0).getRaceColumns().iterator().next(), later));
        assertEquals(18.5, leaderboard.getNetPoints(c[ 8], later), 0.000000001);
        assertEquals(22.5, leaderboard.getNetPoints(c[ 9], later), 0.000000001);
        assertEquals(26.5, leaderboard.getNetPoints(c[10], later), 0.000000001);
        assertEquals(30.5, leaderboard.getNetPoints(c[11], later), 0.000000001);
        // last four competitors in last heat of first round don't get promoted and have null scores in all other rounds but the first
        assertEquals(36.5, leaderboard.getNetPoints(c[60], later), 0.000000001);
        assertEquals(44.5, leaderboard.getNetPoints(c[61], later), 0.000000001);
        assertEquals(52.5, leaderboard.getNetPoints(c[62], later), 0.000000001);
        assertEquals(60.5, leaderboard.getNetPoints(c[63], later), 0.000000001);
        for (int i=1; i<=3; i++) {
            for (int j=60; j<=63; j++) {
                assertNull(leaderboard.getNetPoints(c[j], series.get(i).getRaceColumns().iterator().next(), later));
            }
        }
    }

    @Test
    public void testMultiElminationScoringScheme() throws NoWindException {
        final int NUMBER_OF_ELIMINATIONS = 3;
        Regatta regatta = createRegattaWithEliminations(NUMBER_OF_ELIMINATIONS, new int[] { 8, 4, 2, 2 }, "testBasicElminationScoringScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07));
        // also test that no discards occur for three eliminations when discards start to apply only with four races completed
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[] { NUMBER_OF_ELIMINATIONS + 1 });
        Competitor[] c = createCompetitors(64).toArray(new Competitor[64]);
        TimePoint later = null;
        for (int elimination=0; elimination<NUMBER_OF_ELIMINATIONS; elimination++) {
            // first round with 64 competitors, eight per heat:
            Competitor[][] competitorsForHeatsInRound1 = new Competitor[8][];
            for (int heat=0; heat<8; heat++) {
                competitorsForHeatsInRound1[heat] = new Competitor[8];
                for (int i=0; i<8; i++) {
                    competitorsForHeatsInRound1[heat][i] = c[8*heat+i];
                }
                later = createAndAttachTrackedRaces(series.get(4*elimination+0), "Heat "+(heat+1), /* withScores */ true, competitorsForHeatsInRound1[heat]);
            }
            // quarter-finals has promoted top four competitors of first round in heats with eight competitors each:
            Competitor[][] competitorsForHeatsInQuarterFinals = new Competitor[4][];
            for (int heat=0; heat<4; heat++) {
                competitorsForHeatsInQuarterFinals[heat] = new Competitor[8];
                for (int i=0; i<8; i++) {
                    competitorsForHeatsInQuarterFinals[heat][i] = c[8*(2*heat+(i/4))+(i%4)];
                }
                later = createAndAttachTrackedRaces(series.get(4*elimination+1), "Heat "+(heat+9), /* withScores */ true, competitorsForHeatsInQuarterFinals[heat]);
            }
            // semi-finals has promoted top four competitors of quarter finals which are the top four of each other first-round heat:
            Competitor[][] competitorsForHeatsInSemiFinals = new Competitor[2][];
            for (int heat=0; heat<2; heat++) {
                competitorsForHeatsInSemiFinals[heat] = new Competitor[8];
                for (int i=0; i<8; i++) {
                    competitorsForHeatsInSemiFinals[heat][i] = c[8*(4*heat+2*(i/4))+(i%4)];
                }
                later = createAndAttachTrackedRaces(series.get(4*elimination+2), "Heat "+(heat+13), /* withScores */ true, competitorsForHeatsInSemiFinals[heat]);
            }
            // finals has promoted top four competitors of semi finals which are the top four of first and fifth first-round heats
            // for the final, and the top four of the first round's third and seventh heat
            Competitor[][] competitorsForHeatsInFinals = new Competitor[2][];
            for (int heat=0; heat<2; heat++) {
                competitorsForHeatsInFinals[heat] = new Competitor[8];
                for (int i=0; i<8; i++) {
                    competitorsForHeatsInFinals[heat][i] = c[8*(2*heat+4*(i/4))+(i%4)];
                }
                later = createAndAttachTrackedRaces(series.get(4*elimination+3), "Heat "+(heat+15), /* withScores */ true, competitorsForHeatsInFinals[heat]);
            }
        }
        for (int numberOfEliminationsExpectedToScore=NUMBER_OF_ELIMINATIONS; numberOfEliminationsExpectedToScore>=NUMBER_OF_ELIMINATIONS-1; numberOfEliminationsExpectedToScore--) {
            assertEquals(numberOfEliminationsExpectedToScore*18.5, leaderboard.getNetPoints(c[ 8], later), 0.000000001);
            assertEquals(numberOfEliminationsExpectedToScore*22.5, leaderboard.getNetPoints(c[ 9], later), 0.000000001);
            assertEquals(numberOfEliminationsExpectedToScore*26.5, leaderboard.getNetPoints(c[10], later), 0.000000001);
            assertEquals(numberOfEliminationsExpectedToScore*30.5, leaderboard.getNetPoints(c[11], later), 0.000000001);
            // last four competitors in last heat of first round don't get promoted and have null scores in all other rounds but the first
            assertEquals(numberOfEliminationsExpectedToScore*36.5, leaderboard.getNetPoints(c[60], later), 0.000000001);
            assertEquals(numberOfEliminationsExpectedToScore*44.5, leaderboard.getNetPoints(c[61], later), 0.000000001);
            assertEquals(numberOfEliminationsExpectedToScore*52.5, leaderboard.getNetPoints(c[62], later), 0.000000001);
            assertEquals(numberOfEliminationsExpectedToScore*60.5, leaderboard.getNetPoints(c[63], later), 0.000000001);
            Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
            assertSame(c[0], Util.get(rankedCompetitors, 0)); // should be the winner of the final round's Final heat and take the "crown" for the elimination
            assertEquals(numberOfEliminationsExpectedToScore*0.7, leaderboard.getNetPoints(c[0], later), 0.000000001);
            assertSame(c[1], Util.get(rankedCompetitors, 1)); // should be the winner of the final round's Final heat and take the "crown" for the elimination
            assertEquals(numberOfEliminationsExpectedToScore*2, leaderboard.getNetPoints(c[1], later), 0.000000001);
            for (int elimination=0; elimination<NUMBER_OF_ELIMINATIONS; elimination++) {
                // first four of second heat get promoted to quarter final but lose their heat
                assertNull(leaderboard.getNetPoints(c[ 8], series.get(4*elimination+0).getRaceColumns().iterator().next(), later));
                assertNull(leaderboard.getNetPoints(c[ 9], series.get(4*elimination+0).getRaceColumns().iterator().next(), later));
                assertNull(leaderboard.getNetPoints(c[10], series.get(4*elimination+0).getRaceColumns().iterator().next(), later));
                assertNull(leaderboard.getNetPoints(c[11], series.get(4*elimination+0).getRaceColumns().iterator().next(), later));
                for (int i=1; i<=3; i++) {
                    for (int j=60; j<=63; j++) {
                        assertNull(leaderboard.getNetPoints(c[j], series.get(4*elimination+i).getRaceColumns().iterator().next(), later));
                    }
                }
            }
            // for next loop iteration discard the appropriate number of races
            leaderboard.setCrossLeaderboardResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[] { numberOfEliminationsExpectedToScore }));
        }
    }

    @Test
    public void testElminationScoringSchemeWithDifferentlySizedHeatsInFirstRound() throws NoWindException {
        Regatta regatta = createRegattaWithEliminations(1, new int[] { 8, 4, 2, 2 }, "testBasicElminationScoringScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Competitor[] c = createCompetitors(65).toArray(new Competitor[64]);
        // first round with 64 competitors, eight per heat:
        Competitor[][] competitorsForHeatsInRound1 = new Competitor[8][];
        TimePoint later = null;
        for (int heat=0; heat<8; heat++) {
            final int numberOfCompetitorsInHeat = heat==7?9:8;
            competitorsForHeatsInRound1[heat] = new Competitor[numberOfCompetitorsInHeat];
            for (int i=0; i<numberOfCompetitorsInHeat; i++) {
                competitorsForHeatsInRound1[heat][i] = c[8*heat+i];
            }
            later = createAndAttachTrackedRaces(series.get(0), "Heat "+(heat+1), /* withScores */ true, competitorsForHeatsInRound1[heat]);
        }
        // quarter-finals has promoted top four competitors of first round in heats with eight competitors each:
        Competitor[][] competitorsForHeatsInQuarterFinals = new Competitor[4][];
        for (int heat=0; heat<4; heat++) {
            competitorsForHeatsInQuarterFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInQuarterFinals[heat][i] = c[8*(2*heat+(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(1), "Heat "+(heat+9), /* withScores */ true, competitorsForHeatsInQuarterFinals[heat]);
        }
        // semi-finals has promoted top four competitors of quarter finals which are the top four of each other first-round heat:
        Competitor[][] competitorsForHeatsInSemiFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInSemiFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInSemiFinals[heat][i] = c[8*(4*heat+2*(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(2), "Heat "+(heat+13), /* withScores */ true, competitorsForHeatsInSemiFinals[heat]);
        }
        // finals has promoted top four competitors of semi finals which are the top four of first and fifth first-round heats
        // for the final, and the top four of the first round's third and seventh heat
        Competitor[][] competitorsForHeatsInFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInFinals[heat][i] = c[8*(2*heat+4*(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(3), "Heat "+(heat+15), /* withScores */ true, competitorsForHeatsInFinals[heat]);
        }
        // validate first-round points for drop-outs; competitor #64 (the 9th in heat #8) shall have 65 points
        assertEquals(65, leaderboard.getNetPoints(c[64], later), 0.0000001);
        for (int heat = 0; heat < 8; heat++) {
            for (int i = 4; i < 8; i++) {
                assertEquals((8.0*i+1.0  // best overall rank for drop-outs ranking i+1 in own fleet
                             +8.0*i+8.0) // worst overall rank for drop-outs ranking i+1 in own fleet
                             /2.0, leaderboard.getNetPoints(c[8*heat+i], later), 0.0000001);
            }
        }
    }

    @Test
    public void testElminationScoringSchemeWithFinalNotSailed() throws NoWindException {
        final Regatta regatta = createRegattaWithEliminations(1, new int[] { 8, 4, 2, 2 }, "testBasicElminationScoringScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */ true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07));
        final Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        final Competitor[] c = createCompetitors(64).toArray(new Competitor[64]);
        // first round with 64 competitors, eight per heat:
        final Competitor[][] competitorsForHeatsInRound1 = new Competitor[8][];
        TimePoint later = null;
        for (int heat=0; heat<8; heat++) {
            competitorsForHeatsInRound1[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInRound1[heat][i] = c[8*heat+i];
            }
            later = createAndAttachTrackedRaces(series.get(0), "Heat "+(heat+1), /* withScores */ true, competitorsForHeatsInRound1[heat]);
        }
        // quarter-finals has promoted top four competitors of first round in heats with eight competitors each:
        final Competitor[][] competitorsForHeatsInQuarterFinals = new Competitor[4][];
        for (int heat=0; heat<4; heat++) {
            competitorsForHeatsInQuarterFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInQuarterFinals[heat][i] = c[8*(2*heat+(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(1), "Heat "+(heat+9), /* withScores */ true, competitorsForHeatsInQuarterFinals[heat]);
        }
        // semi-finals has promoted top four competitors of quarter finals which are the top four of each other first-round heat:
        final Competitor[][] competitorsForHeatsInSemiFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInSemiFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInSemiFinals[heat][i] = c[8*(4*heat+2*(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(2), "Heat "+(heat+13), /* withScores */ true, competitorsForHeatsInSemiFinals[heat]);
        }
        // finals has promoted top four competitors of semi finals which are the top four of first and fifth first-round heats
        // for the final, and the top four of the first round's third and seventh heat; we assume here that the finals have not
        // been sailed and therefore expect the scores to be obtained by interpolating, using 1.0 instead of 0.7 for averaging.
        final Competitor[][] competitorsForHeatsInFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInFinals[heat][i] = c[8*(2*heat+4*(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(3), "Heat "+(heat+15), /* withScores */ false, competitorsForHeatsInFinals[heat]);
        }
        // 1 instead of 0.7 for a final that was not sailed; clarified with Juergen Bonne in an e-mail as of 18-09-2015T09:03:00Z
        final double expectedPointsForFirstEightBoats = (1.0 + 2.0 + 3.0 + 4.0 + 5.0 + 6.0 + 7.0 + 8.0) / 8.0;
        for (int i=0; i<4; i++) {
            assertEquals(expectedPointsForFirstEightBoats, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
        for (int i=32; i<36; i++) {
            assertEquals(expectedPointsForFirstEightBoats, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
        final double expectedPointsForLastEightBoats = (9.0 + 10.0 + 11.0 + 12.0 + 13.0 + 14.0 + 15.0 + 16.0) / 8.0;
        for (int i=16; i<20; i++) {
            assertEquals(expectedPointsForLastEightBoats, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
        for (int i=48; i<52; i++) {
            assertEquals(expectedPointsForLastEightBoats, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
    }

    @Test
    public void testElminationScoringSchemeWithFinalSailedButNotTracked() throws NoWindException {
        Regatta regatta = createRegattaWithEliminations(1, new int[] { 8, 4, 2, 2 }, "testBasicElminationScoringScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Competitor[] c = createCompetitors(64).toArray(new Competitor[64]);
        // first round with 64 competitors, eight per heat:
        Competitor[][] competitorsForHeatsInRound1 = new Competitor[8][];
        TimePoint later = null;
        for (int heat=0; heat<8; heat++) {
            competitorsForHeatsInRound1[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInRound1[heat][i] = c[8*heat+i];
            }
            later = createAndAttachTrackedRaces(series.get(0), "Heat "+(heat+1), /* withScores */ true, competitorsForHeatsInRound1[heat]);
        }
        // quarter-finals has promoted top four competitors of first round in heats with eight competitors each:
        Competitor[][] competitorsForHeatsInQuarterFinals = new Competitor[4][];
        for (int heat=0; heat<4; heat++) {
            competitorsForHeatsInQuarterFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInQuarterFinals[heat][i] = c[8*(2*heat+(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(1), "Heat "+(heat+9), /* withScores */ true, competitorsForHeatsInQuarterFinals[heat]);
        }
        // semi-finals has promoted top four competitors of quarter finals which are the top four of each other first-round heat:
        Competitor[][] competitorsForHeatsInSemiFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInSemiFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInSemiFinals[heat][i] = c[8*(4*heat+2*(i/4))+(i%4)];
            }
            later = createAndAttachTrackedRaces(series.get(2), "Heat "+(heat+13), /* withScores */ true, competitorsForHeatsInSemiFinals[heat]);
        }
        // finals has promoted top four competitors of semi finals which are the top four of first and fifth first-round heats
        // for the final, and the top four of the first round's third and seventh heat; we assume here that the finals have not
        // been sailed and therefore expect the scores to be obtained by interpolating, using 1.0 instead of 0.7 for averaging.
        Competitor[][] competitorsForHeatsInFinals = new Competitor[2][];
        for (int heat=0; heat<2; heat++) {
            competitorsForHeatsInFinals[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                final Competitor comp = c[8*(2*heat+4*(i/4))+(i%4)];
                competitorsForHeatsInFinals[heat][i] = comp;
                leaderboard.getScoreCorrection().correctScore(comp, series.get(3).getRaceColumns().iterator().next(), heat==0&&i==0 ? 0.7 : 8*heat+i+1);
            }
        }
        for (int i=0; i<4; i++) {
            assertEquals(i==0?0.7:i+1, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
        for (int i=32; i<36; i++) {
            assertEquals(i-27, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
        for (int i=16; i<20; i++) {
            assertEquals(i-7, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
        for (int i=48; i<52; i++) {
            assertEquals(i-35, leaderboard.getNetPoints(c[i], later), 0.000000001);
        }
    }

    @Test
    public void testElminationScoringSchemeWithOnlyFirstRoundSailed() throws NoWindException {
        Regatta regatta = createRegattaWithEliminations(1, new int[] { 8, 4, 2, 2 }, "testBasicElminationScoringScheme",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        Competitor[] c = createCompetitors(64).toArray(new Competitor[64]);
        // first round with 64 competitors, eight per heat:
        Competitor[][] competitorsForHeatsInRound1 = new Competitor[8][];
        TimePoint later = null;
        for (int heat=0; heat<8; heat++) {
            competitorsForHeatsInRound1[heat] = new Competitor[8];
            for (int i=0; i<8; i++) {
                competitorsForHeatsInRound1[heat][i] = c[8*heat+i];
            }
            later = createAndAttachTrackedRaces(series.get(0), "Heat "+(heat+1), /* withScores */ true, competitorsForHeatsInRound1[heat]);
        }
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        // Clarified with Juergen Bonne in an e-mail as of 18-09-2015T09:03:00Z that a final race's winner
        // is scored with 0.7 only if the final race has actually been sailed. If the competitors are qualified
        // for the final race but it's not sailed, average scores are to be assigned to all competitors qualified
        // for the final race, but this average assumes 1.0 points for the first rank instead of the 0.7 assigned
        // to the winner if the race is actually sailed. Similarly, if not even the semi-finals have been sailed,
        // score averaging uses 1.0 for the first rank.
        final double expectedPointsForFirstEightBoats = (1.0 + 2.0 + 3.0 + 4.0 + 5.0 + 6.0 + 7.0 + 8.0) / 8.0;
        for (int i=0; i<8; i++) {
            assertEquals(expectedPointsForFirstEightBoats, leaderboard.getNetPoints(c[8*i], later), 0.0000001);
            assertEquals(0,
                    Arrays.asList(c).indexOf(Util.get(rankedCompetitors, i))%8, "Competitor "+Util.get(rankedCompetitors, i)+" not in list of best eight "); // each first competitor in a heat ranks in the top 8
        }
        for (int i=1; i<=3; i++) {
            for (Competitor comp : c) {
                assertNull(leaderboard.getNetPoints(comp, series.get(i).getRaceColumns().iterator().next(), later));
            }
        }
    }

    @Test
    public void testScoringConsideringNotAllRaces() throws NoWindException {
        // one discard at four races
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        // Leaderboard:                     Accumulated with incremental discards
        //       R1 R2 R3 R4 R5 R6          R1 R2 R3 R4 R5 R6
        // c[0]: 2  2  3  3 (4) 4            2  4  7  7 10 14
        // c[1]: 3  3  2 (4) 3  3            3  6  8  8 11 14
        // c[2]: 1  1  1 (2) 2  2            1  2  3  3  5  7
        // c[3]:(4) 4  4  1  1  1            4  8 12  9 10 11
        double[][] scoresAfter3Races = new double[][] {
                { 2, 2, 3 },
                { 3, 3, 2 },
                { 1, 1, 1 },
                { 4, 4, 4 } };
        double[][] scoresAfter4Races = new double[][] {
                { 2, 2, 0, 3 },
                { 3, 3, 2, 0 },
                { 1, 1, 1, 0 },
                { 0, 4, 4, 1 } };
        double[][] scoresAfter5Races = new double[][] {
                { 2, 2, 3, 3, 0 },
                { 3, 3, 2, 0, 3 },
                { 1, 1, 1, 0, 2 },
                { 0, 4, 4, 1, 1 } };
        double[][] scoresAfter6Races = new double[][] {
                { 2, 2, 3, 3, 0, 4 },
                { 3, 3, 2, 0, 3, 3 },
                { 1, 1, 1, 0, 2, 2 },
                { 0, 4, 4, 1, 1, 1 } };
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] };
        Competitor[] f2 = new Competitor[] { c[2], c[0], c[1], c[3] };
        Competitor[] f3 = new Competitor[] { c[2], c[1], c[0], c[3] };
        Competitor[] f4 = new Competitor[] { c[3], c[2], c[0], c[1] };
        Competitor[] f5 = new Competitor[] { c[3], c[2], c[1], c[0] };
        Competitor[] f6 = new Competitor[] { c[3], c[2], c[1], c[0] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */6, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[] { 4 });
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2, f3, f4, f5, f6);
        Map<RaceColumn, List<Competitor>> rankedCompetitorsFromBestToWorstAfterEachRaceColumn =
                leaderboard.getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(later);
        assertEquals(Arrays.asList(c[2], c[0], c[1], c[3]),
                rankedCompetitorsFromBestToWorstAfterEachRaceColumn.get(Util.get(leaderboard.getRaceColumns(), 0)));
        assertEquals(Arrays.asList(c[2], c[0], c[1], c[3]),
                rankedCompetitorsFromBestToWorstAfterEachRaceColumn.get(Util.get(leaderboard.getRaceColumns(), 1)));
        assertEquals(Arrays.asList(c[2], c[0], c[1], c[3]),
                rankedCompetitorsFromBestToWorstAfterEachRaceColumn.get(Util.get(leaderboard.getRaceColumns(), 2)));
        assertEquals(Arrays.asList(c[2], c[0], c[1], c[3]),
                rankedCompetitorsFromBestToWorstAfterEachRaceColumn.get(Util.get(leaderboard.getRaceColumns(), 3)));
        assertEquals(Arrays.asList(c[2], c[3], c[0], c[1]), // c[3] has one win, c[0] none
                rankedCompetitorsFromBestToWorstAfterEachRaceColumn.get(Util.get(leaderboard.getRaceColumns(), 4)));
        assertEquals(Arrays.asList(c[2], c[3], c[0], c[1]), // c[0] has more second places than c[1] (2 vs. 1)
                rankedCompetitorsFromBestToWorstAfterEachRaceColumn.get(Util.get(leaderboard.getRaceColumns(), 5)));
        List<RaceColumn> raceColumnsToConsider = new ArrayList<>();
        int raceColumnNumber=0;
        while (raceColumnNumber<3) {
            final RaceColumn raceColumn = Util.get(leaderboard.getRaceColumns(), raceColumnNumber++);
            raceColumnsToConsider.add(raceColumn);
        }
        checkScoresAfterSomeRaces(leaderboard, raceColumnsToConsider, scoresAfter3Races, later, c);
        raceColumnsToConsider.add(Util.get(leaderboard.getRaceColumns(), raceColumnNumber++));
        checkScoresAfterSomeRaces(leaderboard, raceColumnsToConsider, scoresAfter4Races, later, c);
        raceColumnsToConsider.add(Util.get(leaderboard.getRaceColumns(), raceColumnNumber++));
        checkScoresAfterSomeRaces(leaderboard, raceColumnsToConsider, scoresAfter5Races, later, c);
        raceColumnsToConsider.add(Util.get(leaderboard.getRaceColumns(), raceColumnNumber++));
        checkScoresAfterSomeRaces(leaderboard, raceColumnsToConsider, scoresAfter6Races, later, c);
    }

    @Test
    public void testTieBreakByMedalRaceScoreOnlyIfEqualNetScore() throws NoWindException {
        Competitor[] c = createCompetitors(2).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[1], c[0] };
        Competitor[] f2 = new Competitor[] { c[1], c[0] };
        Competitor[] m1 = new Competitor[] { c[0], c[1] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ true, /* medal */ 1, "testTieBreakByMedalRaceScoreOnlyIfEqualTotalScore",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2);
        createAndAttachTrackedRaces(series.get(2), "Medal", /* withScores */ true, m1);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        // assert that both have equal score
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        // assert that c[0] ranks better than c[1] (reason: c[0] ranked better in medal race)
        assertEquals(Util.indexOf(rankedCompetitors, c[0]), Util.indexOf(rankedCompetitors, c[1])-1);
    }

    @Test
    public void testTieBreakByMedalRacesScoreOnlyIfEqualTotalScore() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2], c[3]};
        Competitor[] f2 = new Competitor[] { c[1], c[3], c[0], c[2]};
        Competitor[] f3 = new Competitor[] { c[2], c[0], c[1], c[3]};
        // points in series: c0 = 6, c1 = 6, c2 = 8, c3 = 12

        Competitor[] m1 = new Competitor[] { c[1], c[0], c[2]};
        Competitor[] m2 = new Competitor[] { c[0], c[2], c[1] };
        Competitor[] m3 = new Competitor[] { c[1], c[0], c[3] };
        // points in series: c0 = 10, c1 = 10, c2 = 16

        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */3, new String[] { "Default" },
                /* medal */ true, /* medal */ 3, "testTieBreakByMedalRacesScoreOnlyIfEqualTotalScore",
                DomainFactory.INSTANCE.getOrCreateBoatClass("J/70", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2, f3);
        createAndAttachTrackedRaces(series.get(2), "Medal", /* withScores */ true, m1, m2, m3);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        // assert that both have equal score
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        // assert that c[1] ranks better than c[0] (reason: c[1] ranked better in medal race)
        assertEquals(Util.indexOf(rankedCompetitors, c[1]), Util.indexOf(rankedCompetitors, c[0])-1);
    }

    @Test
    public void testTieBreakWithEqualWinsAndTwoVersusOneSeconds() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] }; // c[0] scores 16
        Competitor[] f2 = new Competitor[] { c[2], c[0], c[1], c[3] }; // c[1] scores 16 points altogether but has only one second rank
        Competitor[] f3 = new Competitor[] { c[2], c[1], c[3], c[0] }; // c[2] scores  9
        Competitor[] f4 = new Competitor[] { c[3], c[2], c[0], c[1] }; // c[3] scores 29
        Competitor[] f5 = new Competitor[] { c[0], c[2], c[1], c[3] };
        Competitor[] f6 = new Competitor[] { c[1], c[2], c[3], c[0] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */6, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithEqualWinsAndTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2, f3, f4, f5, f6);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertEquals(Util.indexOf(rankedCompetitors, c[0]), Util.indexOf(rankedCompetitors, c[1])-1);
    }

    @Test
    public void testTieBreakWithEqualWinsAndTwoVersusOneSecondsWithHighPointScoringScheme() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] }; // c[0] scores 14
        Competitor[] f2 = new Competitor[] { c[2], c[0], c[1], c[3] }; // c[1] scores 14 points altogether but has only one second rank
        Competitor[] f3 = new Competitor[] { c[2], c[1], c[3], c[0] }; // c[2] scores 21
        Competitor[] f4 = new Competitor[] { c[3], c[2], c[0], c[1] }; // c[3] scores 11
        Competitor[] f5 = new Competitor[] { c[0], c[2], c[1], c[3] };
        Competitor[] f6 = new Competitor[] { c[1], c[2], c[3], c[0] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */6, new String[] { "Default" },
                /* medal */ false, /* medal */ 0, "testTieBreakWithEqualWinsAndTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", /* withScores */ true, f1, f2, f3, f4, f5, f6);
        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getNetPoints(c[0], later), leaderboard.getNetPoints(c[1], later), 0.000000001);
        assertEquals(Util.indexOf(rankedCompetitors, c[0]), Util.indexOf(rankedCompetitors, c[1])-1);
    }

    @Test
    public void testAdditionalScoreInformationLeadsToChangedScoreForOneColumn() throws NoWindException {
        TrackedRegattaRegistry trackedRegattaRegistry = mock(TrackedRegattaRegistry.class);
        Regatta dummyRegatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, "Dummy",
                new BoatClassImpl("Extreme40", false),
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, trackedRegattaRegistry,
                new HighPointFirstGets10Or8AndLastBreaksTie(), "578876345345",
                new CourseAreaImpl("Humba", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null),
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        trackedRegattaRegistry.getOrCreateTrackedRegatta(dummyRegatta);
        Competitor[] competitors = createCompetitors(10).toArray(new Competitor[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        FlexibleLeaderboard leaderboardHighPoint10Or8AndLastBreaksTie = new FlexibleLeaderboardImpl("Test ESS Highpoint 10Or8AndLastBreaksTie", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets10Or8AndLastBreaksTie(), null);
        leaderboardHighPoint10Or8AndLastBreaksTie.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors), dummyRegatta), "R1", /* medalRace */ false);
        assertTrue(leaderboardHighPoint10Or8AndLastBreaksTie.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[3], later)) < 0); // c0 better than c3
        assertEquals(10, Util.size(leaderboardHighPoint10Or8AndLastBreaksTie.getCompetitorsFromBestToWorst(later)));
        assertEquals(Double.valueOf(10), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(9), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(1), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        leaderboardHighPoint10Or8AndLastBreaksTie.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors), dummyRegatta), "R2", /* medalRace */ false);
        assertEquals(Double.valueOf(20), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(18), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(2), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        RaceLog raceLogForRace2 = leaderboardHighPoint10Or8AndLastBreaksTie.getRaceColumnByName("R2").getRaceLog(leaderboardHighPoint10Or8AndLastBreaksTie.getFleet(null));
        raceLogForRace2.add(new RaceLogAdditionalScoringInformationEventImpl(now, later, new LogEventAuthorImpl("Plopp", 1), "12345678", 0, AdditionalScoringInformationType.UNKNOWN));
        assertEquals(Double.valueOf(20), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(18), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(2), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        raceLogForRace2.add(new RaceLogAdditionalScoringInformationEventImpl(now, later.plus(Duration.ONE_MINUTE), new LogEventAuthorImpl("Plopp", 1), "123456789873773762", 0, AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE));
        assertEquals(Double.valueOf(18), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(16), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(2), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        leaderboardHighPoint10Or8AndLastBreaksTie.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors), dummyRegatta), "R3", /* medalRace */ false);
        assertEquals(Double.valueOf(28), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(25), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(3), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        AdditionalScoringInformationFinder finder = new AdditionalScoringInformationFinder(raceLogForRace2);
        RaceLogAdditionalScoringInformationEvent event = finder.analyze(/*filterBy*/AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
        assert event != null;
        try {
            raceLogForRace2.revokeEvent(new LogEventAuthorImpl("Plopp", 1), event);
        } catch (NotRevokableException e1) {
            e1.printStackTrace();
        }
        event = finder.analyze(/*filterBy*/AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
        assert event == null;
        assertEquals(Double.valueOf(30), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(3), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        RaceState raceStateForRace2 = RaceStateImpl.create(mock(RaceLogResolver.class), raceLogForRace2, new LogEventAuthorImpl("Simon", 1));
        assertFalse(raceStateForRace2.isAdditionalScoringInformationEnabled(AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE));
        raceStateForRace2.setAdditionalScoringInformationEnabled(later.plus(Duration.ONE_MINUTE).plus(Duration.ONE_SECOND), /*enable*/true, AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
        assertTrue(raceStateForRace2.isAdditionalScoringInformationEnabled(AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE));
        assertEquals(Double.valueOf(28), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(3), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        raceStateForRace2.setAdditionalScoringInformationEnabled(later.plus(Duration.ONE_MINUTE).plus(Duration.ONE_SECOND.plus(10000)), /*enable*/false, AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
        assertFalse(raceStateForRace2.isAdditionalScoringInformationEnabled(AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE));
        assertEquals(Double.valueOf(30), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(3), leaderboardHighPoint10Or8AndLastBreaksTie.getNetPoints(competitors[9], later));
        LeaderboardDTO leaderboardDTO = null;
        try {
            leaderboardDTO = leaderboardHighPoint10Or8AndLastBreaksTie.getLeaderboardDTO(later.plus(Duration.ONE_HOUR), Collections.<String>emptyList(), false, trackedRegattaRegistry, DomainFactory.INSTANCE, false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        assert leaderboardDTO != null;
        assertEquals(30.0, leaderboardDTO.rows.get(leaderboardDTO.competitors.get(0)).netPoints, 0.000000000001);
        raceStateForRace2.setAdditionalScoringInformationEnabled(later.plus(Duration.ONE_MINUTE).plus(Duration.ONE_SECOND.plus(20000)), /*enable*/true, AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
        try {
            leaderboardDTO = leaderboardHighPoint10Or8AndLastBreaksTie.getLeaderboardDTO(later.plus(Duration.ONE_HOUR), Collections.<String>emptyList(), false, trackedRegattaRegistry, DomainFactory.INSTANCE, false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        assertEquals(28.0, leaderboardDTO.rows.get(leaderboardDTO.competitors.get(0)).netPoints, 0.000000000001);
        assertEquals(25.0, leaderboardDTO.rows.get(leaderboardDTO.competitors.get(1)).netPoints, 0.000000000001);
    }

    @Test
    public void testCompetitorsRankedEleventhOrLowerGetOnePointScore() throws NoWindException {
        Competitor[] competitors = createCompetitors(16).toArray(new Competitor[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        FlexibleLeaderboard leaderboardHighPoint10LastBreaksTie = new FlexibleLeaderboardImpl("Test ESS Highpoint 10LastBreaksTie", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets10LastBreaksTie(), null);
        leaderboardHighPoint10LastBreaksTie
                .addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors)), "R1", /* medalRace */
                        false);
        assertTrue(leaderboardHighPoint10LastBreaksTie.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboardHighPoint10LastBreaksTie.getNetPoints(competitors[0], later), leaderboardHighPoint10LastBreaksTie.getNetPoints(competitors[3], later)) < 0); // c0 better than c3
        assertEquals(16, Util.size(leaderboardHighPoint10LastBreaksTie.getCompetitorsFromBestToWorst(later)));
        assertEquals(Double.valueOf(10), leaderboardHighPoint10LastBreaksTie.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(1), leaderboardHighPoint10LastBreaksTie.getNetPoints(competitors[15], later));
        // Normal HighPoint leaderboard has no max so that winner gets as many points as there are competitors
        FlexibleLeaderboard leaderboardHighPoint = new FlexibleLeaderboardImpl("Test ESS Highpoint", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPoint(), null);
        leaderboardHighPoint.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors)), "R1", /* medalRace */
                false);
        assertEquals(16, Util.size(leaderboardHighPoint.getCompetitorsFromBestToWorst(later)));
        assertEquals(Double.valueOf(16), leaderboardHighPoint.getNetPoints(competitors[0], later));
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("ESS", "ESS", /* displayName */ null,
                /* displayGroupsInReverseOrder */ false, Arrays.asList(new Leaderboard[] { leaderboardHighPoint10LastBreaksTie }));
        LeaderboardGroupMetaLeaderboard leaderboardHighPointESSOverall = new LeaderboardGroupMetaLeaderboard(
                leaderboardGroup, new HighPointExtremeSailingSeriesOverall(),
                new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]));
        assertEquals(16, Util.size(leaderboardHighPointESSOverall.getCompetitorsFromBestToWorst(later)));
        assertEquals(Double.valueOf(10), leaderboardHighPointESSOverall.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(9), leaderboardHighPointESSOverall.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(8), leaderboardHighPointESSOverall.getNetPoints(competitors[2], later));
        assertEquals(Double.valueOf(7), leaderboardHighPointESSOverall.getNetPoints(competitors[3], later));
        assertEquals(Double.valueOf(6), leaderboardHighPointESSOverall.getNetPoints(competitors[4], later));
        assertEquals(Double.valueOf(5), leaderboardHighPointESSOverall.getNetPoints(competitors[5], later));
        assertEquals(Double.valueOf(4), leaderboardHighPointESSOverall.getNetPoints(competitors[6], later));
        assertEquals(Double.valueOf(3), leaderboardHighPointESSOverall.getNetPoints(competitors[7], later));
        assertEquals(Double.valueOf(2), leaderboardHighPointESSOverall.getNetPoints(competitors[8], later));
        assertEquals(Double.valueOf(1), leaderboardHighPointESSOverall.getNetPoints(competitors[9], later));
        assertEquals(Double.valueOf(1), leaderboardHighPointESSOverall.getNetPoints(competitors[14], later));
        assertEquals(Double.valueOf(1), leaderboardHighPointESSOverall.getNetPoints(competitors[15], later));
    }

    @Test
    public void testESS2017WithCompetitorsBeingDSQScoringTwoLessThanLastCompetingCompetitor() throws NoWindException {
        Competitor[] competitors = createCompetitors(8).toArray(new Competitor[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        FlexibleLeaderboard leaderboardHighPoint12LastBreaksTie2017 = new FlexibleLeaderboardImpl("Test ESS Highpoint",
        		new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets12Or8AndLastBreaksTie2017(), null);
        RaceColumn raceColumn = leaderboardHighPoint12LastBreaksTie2017
                .addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors)), "R1", /* medalRace */
                        false);
        // test basic scores with best getting 12 and worst getting 5 in a 12 point scheme
        assertTrue(leaderboardHighPoint12LastBreaksTie2017.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[0], later),
                leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[3], later)) < 0); // c0 better than c3
        assertEquals(8, Util.size(leaderboardHighPoint12LastBreaksTie2017.getCompetitorsFromBestToWorst(later)));
        assertEquals(Double.valueOf(12), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(5), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[7], later));
        // now let's assume that the first competitor is DSQ so it gets 4 points
        leaderboardHighPoint12LastBreaksTie2017.getScoreCorrection().setMaxPointsReason(competitors[0], raceColumn, MaxPointsReason.DSQ);
        assertEquals(8, Util.size(leaderboardHighPoint12LastBreaksTie2017.getCompetitorsFromBestToWorst(later)));
        assertEquals(Double.valueOf(12), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(4), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(6), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[7], later));
        leaderboardHighPoint12LastBreaksTie2017.getScoreCorrection().setMaxPointsReason(competitors[0], raceColumn, MaxPointsReason.NONE);
        assertEquals(Double.valueOf(12), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(5), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[7], later));
        leaderboardHighPoint12LastBreaksTie2017.getScoreCorrection().setMaxPointsReason(competitors[0], raceColumn, MaxPointsReason.DNS);
        assertEquals(Double.valueOf(12), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(5), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[0], later));
        assertEquals(Double.valueOf(6), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[7], later));
        // two competitors not started leading to tie
        leaderboardHighPoint12LastBreaksTie2017.getScoreCorrection().setMaxPointsReason(competitors[1], raceColumn, MaxPointsReason.DNS);
        assertEquals(Double.valueOf(12), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[2], later));
        assertEquals(Double.valueOf(6), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[1], later));
        assertEquals(Double.valueOf(6), leaderboardHighPoint12LastBreaksTie2017.getNetPoints(competitors[0], later));
    }

    @Test
    public void testOverallLeaderboardWithESSHighPointScoring() throws NoWindException {
        // Let c0 lead the series, c3 trail it, c1 and c2 are one-time competitors which are then suppressed in the
        // overall leaderboard, expecting c3 to rank second overall, after c0 ranking first, with c1 and c2 not appearing
        // in the overall leaderboard's sorted competitor list
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] };
        Competitor[] f2 = new Competitor[] { c[2], c[0], c[1], c[3] };
        Competitor[] f3 = new Competitor[] { c[1], c[3], c[0] };
        Competitor[] f4 = new Competitor[] { c[3], c[0], c[1] };
        Competitor[] f5 = new Competitor[] { c[0], c[3] };
        Competitor[] f6 = new Competitor[] { c[3], c[0] };
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Leaderboard 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPoint(), null);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f1)), "R1", /* medalRace */
                false);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f2)), "R2", /* medalRace */
                false);
        assertTrue(leaderboard1.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboard1.getNetPoints(c[0], later), leaderboard1.getNetPoints(c[3], later)) < 0); // c0 better than c3
        FlexibleLeaderboard leaderboard2 = new FlexibleLeaderboardImpl("Leaderboard 3", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPoint(), null);
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f3)), "R1", /* medalRace */
                false);
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f4)), "R2", /* medalRace */
                false);
        assertTrue(leaderboard2.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboard2.getNetPoints(c[3], later), leaderboard2.getNetPoints(c[0], later)) < 0); // c3 better than c0
        FlexibleLeaderboard leaderboard3 = new FlexibleLeaderboardImpl("Leaderboard 3", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPoint(), null);
        leaderboard3.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f5)), "R1", /* medalRace */
                false);
        leaderboard3.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f6)), "R2", /* medalRace */
                false);
        assertTrue(Util.indexOf(leaderboard3.getCompetitorsFromBestToWorst(later), c[3]) <
                Util.indexOf(leaderboard3.getCompetitorsFromBestToWorst(later), c[0])); // c3 better than c0; won last race
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("Leaderboard Group", "Leaderboard Group", /* displayName */ null, false, Arrays.asList(leaderboard1,
                leaderboard2, leaderboard3));
        leaderboardGroup.setOverallLeaderboard(new LeaderboardGroupMetaLeaderboard(leaderboardGroup, new HighPointExtremeSailingSeriesOverall(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[0])));
        leaderboardGroup.getOverallLeaderboard().setSuppressed(c[1], true);
        leaderboardGroup.getOverallLeaderboard().setSuppressed(c[2], true);
        Iterable<Competitor> rankedCompetitors = leaderboardGroup.getOverallLeaderboard().getCompetitorsFromBestToWorst(later);
        assertFalse(Util.contains(rankedCompetitors, c[1]));
        assertFalse(Util.contains(rankedCompetitors, c[2]));
        assertEquals(2, Util.size(rankedCompetitors));
        assertEquals(28 /* one win, two second */, leaderboardGroup.getOverallLeaderboard().getNetPoints(c[0], later), 0.000000001);
        assertEquals(29 /* two wins, one second */, leaderboardGroup.getOverallLeaderboard().getNetPoints(c[3], later), 0.000000001);
    }

    @Test
    public void testOverallLeaderboardWithESSHighPointScoringWithTwoCompetitorsHavingWonTheSameNumberOfRegattas() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] };
        Competitor[] f2 = new Competitor[] { c[0], c[2], c[1], c[3] };
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Regatta 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets10Or8AndLastBreaksTie(), null);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f1)), "R1", /* medalRace */
                false);
        assertTrue(leaderboard1.getNetPoints(c[2], later) > leaderboard1.getNetPoints(c[0], later)); // c2 better than c[0]
        assertEquals(10.0, leaderboard1.getNetPoints(c[2], later), 0.1);
        assertEquals(9.0, leaderboard1.getNetPoints(c[0], later), 0.1);
        FlexibleLeaderboard leaderboard2 = new FlexibleLeaderboardImpl("Regatta 2", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets10Or8AndLastBreaksTie(), null);
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f2)), "R1", /* medalRace */
                false);
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("Leaderboard Group", "Leaderboard Group", /* displayName */ null, false, Arrays.asList(leaderboard1,
                leaderboard2));
        leaderboardGroup.setOverallLeaderboard(new LeaderboardGroupMetaLeaderboard(leaderboardGroup, new HighPointExtremeSailingSeriesOverall(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[0])));
        Iterable<Competitor> rankedCompetitors = leaderboardGroup.getOverallLeaderboard().getCompetitorsFromBestToWorst(later);
        assertEquals(4, Util.size(rankedCompetitors));
        assertEquals(19, leaderboardGroup.getOverallLeaderboard().getNetPoints(c[0], later), 0.000000001);
        assertEquals(19, leaderboardGroup.getOverallLeaderboard().getNetPoints(c[2], later), 0.000000001);
        assertEquals(16, leaderboardGroup.getOverallLeaderboard().getNetPoints(c[1], later), 0.000000001);
        assertEquals(14, leaderboardGroup.getOverallLeaderboard().getNetPoints(c[3], later), 0.000000001);
        assertEquals(c[0], Util.get(rankedCompetitors, 0));
        assertEquals(c[2], Util.get(rankedCompetitors, 1));
    }

    @Test
    public void testHighPointScoringEightWithInterpolationWhenFleetNotComplete() throws NoWindException {
        int competitorsCount = 21;
        List<Competitor> competitors = createCompetitors(competitorsCount);
        Regatta regatta = createRegatta(/* qualifying */ 1, new String[] { "Fleet1", "Fleet2", "Fleet3" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, /* medal */ 0, "testHighPointScoringEightWithInterpolationWhenFleetNotComplete",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_WINNER_GETS_EIGHT_AND_INTERPOLATION));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);

        Series firstSeries = regatta.getSeries().iterator().next();
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        // fleet 1 has 8 competitors
        TrackedRace r1Fleet1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(0, 8));
        // fleet 2 has 7 competitors
        TrackedRace r1Fleet2 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(8, 15));
        // fleet 1 has 6 competitors
        TrackedRace r1Fleet3 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(15, 21));
        RaceColumn r1Column = firstSeries.getRaceColumnByName("Q1");
        r1Column.setTrackedRace(r1Column.getFleetByName("Fleet1"), r1Fleet1);
        r1Column.setTrackedRace(r1Column.getFleetByName("Fleet2"), r1Fleet2);
        r1Column.setTrackedRace(r1Column.getFleetByName("Fleet3"), r1Fleet3);

        // first fleet is complete
        double[] expectedResultsFleet1 = { 8, 7, 6, 5, 4, 3, 2, 1 };
        // second fleet has 1 missing boat
        double diffX1 = 7.0 / 6.0;
        double[] expectedResultsFleet2 = { 8, 8 - diffX1 * 1, 8 - diffX1 * 2, 8 - diffX1 * 3, 8 - diffX1 * 4, 8 - diffX1 * 5, 1 };
        // third fleet has 2 missing boats
        double diffX2 = 7.0 / 5.0;
        double[] expectedResultsFleet3 = { 8, 8 - diffX2 * 1, 8 - diffX2 * 2, 8 - diffX2 * 3, 8 - diffX2 * 4, 1 };
        // check first race
        Iterator<Competitor> rankedCompetitorsFleet1 = r1Fleet1.getCompetitorsFromBestToWorst(later).iterator();
        int i = 0;
        while (rankedCompetitorsFleet1.hasNext() ) {
            Competitor currentCompetitor = rankedCompetitorsFleet1.next();
            assertSame(currentCompetitor, competitors.get(i));
            assertEquals(expectedResultsFleet1[i], leaderboard.getNetPoints(currentCompetitor, later), 0.000000001);
            i++;
        }
        assertEquals(8, i);
        // check second race
        Iterator<Competitor> rankedCompetitorsFleet2 = r1Fleet2.getCompetitorsFromBestToWorst(later).iterator();
        i = 0;
        while (rankedCompetitorsFleet2.hasNext()) {
            Competitor currentCompetitor = rankedCompetitorsFleet2.next();
            assertSame(currentCompetitor, competitors.get(8+i));
            assertEquals(expectedResultsFleet2[i], leaderboard.getNetPoints(currentCompetitor, later), 0.000000001);
            i++;
        }
        assertEquals(7, i);
        // checked third race
        Iterator<Competitor> rankedCompetitorsFleet3 = r1Fleet3.getCompetitorsFromBestToWorst(later).iterator();
        i = 0;
        while (rankedCompetitorsFleet3.hasNext()) {
            Competitor currentCompetitor = rankedCompetitorsFleet3.next();
            assertSame(currentCompetitor, competitors.get(15+i));
            assertEquals(expectedResultsFleet3[i], leaderboard.getNetPoints(currentCompetitor, later), 0.000000001);
            i++;
        }
        assertEquals(6, i);

        Iterable<Competitor> allCompetitorsFromBestToWorst = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitorsCount, Util.size(allCompetitorsFromBestToWorst));
        // check that the fleet winners have all 8 points
        assertEquals(8.0, leaderboard.getNetPoints(Util.get(allCompetitorsFromBestToWorst, 0), later), 0.000000001);
        assertEquals(8.0, leaderboard.getNetPoints(Util.get(allCompetitorsFromBestToWorst, 1), later), 0.000000001);
        assertEquals(8.0, leaderboard.getNetPoints(Util.get(allCompetitorsFromBestToWorst, 2), later), 0.000000001);
        // check that the fleet looser have all 1 point
        assertEquals(1.0, leaderboard.getNetPoints(Util.get(allCompetitorsFromBestToWorst, competitorsCount-1), later), 0.000000001);
        assertEquals(1.0, leaderboard.getNetPoints(Util.get(allCompetitorsFromBestToWorst, competitorsCount-2), later), 0.000000001);
        assertEquals(1.0, leaderboard.getNetPoints(Util.get(allCompetitorsFromBestToWorst, competitorsCount-3), later), 0.000000001);
    }

    @Test
    public void testOverallLeaderboardWithESSHighPointScoringAndTieBreakInLastRegattaOfLeaderboardGroup() throws NoWindException {
        Competitor[] c = createCompetitors(12).toArray(new Competitor[0]);
        //                                              10      9     8    7    6    5     4      3     2     1     1      1
        Competitor[] regattaRace1_1 = new Competitor[] { c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[10], c[11] };
        Competitor[] regattaRace2_1 = new Competitor[] { c[1], c[10], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[11], c[0] };
        Competitor[] regattaRace1_2 = new Competitor[] { c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[10], c[11] };
        Competitor[] regattaRace2_2 = new Competitor[] { c[1], c[10], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[11], c[0] };
        Competitor[] regattaRace3_2 = new Competitor[] { c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[11], c[10], c[0] };
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Leaderboard 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets10LastBreaksTie(), null);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(regattaRace1_1)), "R1", /* medalRace */
                false);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(regattaRace2_1)), "R2", /* medalRace */
                false);
        FlexibleLeaderboard leaderboard2 = new FlexibleLeaderboardImpl("Leaderboard 2", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets10LastBreaksTie(), null);
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(regattaRace1_2)), "R1", /* medalRace */
                false);
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(regattaRace2_2)), "R2", /* medalRace */
                false);
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(regattaRace3_2)), "R3", /* medalRace */
                false);
        // [C2, C3, C4, C5, C6, C7, C1, C11, C8, C9, C10, C12]
        // [29, 25, 22, 19, 16, 12, 13, 11,  10,  7,  4,  3]
        Iterable<Competitor> rankedCompetitorsInLeaderboard = leaderboard2.getCompetitorsFromBestToWorst(later);
        List<Double> scoresForCompetitors = new ArrayList<Double>();
        for (Competitor competitor : rankedCompetitorsInLeaderboard) {
            scoresForCompetitors.add(leaderboard2.getNetPoints(competitor, later));
        }
        assertEquals(12, Util.size(rankedCompetitorsInLeaderboard));
        assertEquals(c[1], Util.get(rankedCompetitorsInLeaderboard, 0));
        assertEquals(29, leaderboard2.getNetPoints(c[1], later), 0.000000001);
        assertEquals(7, leaderboard2.getNetPoints(c[8], later), 0.000000001);
        assertEquals(4, leaderboard2.getNetPoints(c[9], later), 0.000000001);
        assertEquals(3, leaderboard2.getNetPoints(c[11], later), 0.000000001);

        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("Leaderboard Group ESS Overall", "Leaderboard Group", /* displayName */ null,
                false, Arrays.asList(leaderboard1, leaderboard2));
        leaderboardGroup.setOverallLeaderboard(new LeaderboardGroupMetaLeaderboard(leaderboardGroup, new HighPointExtremeSailingSeriesOverall(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[0])));
        leaderboardGroup.getOverallLeaderboard().setSuppressed(c[c.length-1], true);
        // ranking must match the regatta ranks - as for 11 competitors the ranking is random
        // in the faulty implementation we test with many iterations to make sure that
        // we get the faulty random value
        for (int i=0;i<=1000;i++) {
            Iterable<Competitor> rankedCompetitors = leaderboardGroup.getOverallLeaderboard().getCompetitorsFromBestToWorst(later);
            assertFalse(Util.contains(rankedCompetitors, c[c.length-1]));
            assertEquals(11, Util.size(rankedCompetitors));
            assertEquals(c[8], Util.get(rankedCompetitors, 9));
            assertEquals(c[9], Util.get(rankedCompetitors, 10));
        }
    }

    @Test
    public void testApplicationOfScoreCorrectionsInRacesWithNoTrackedRaceAfterLastTrackedRaceWithoutMarkPassings() throws NoWindException {
        // Let c0 lead the series, c3 trail it, c1 and c2 are one-time competitors which are then suppressed in the
        // overall leaderboard, expecting c3 to rank second overall, after c0 ranking first, with c1 and c2 not appearing
        // in the overall leaderboard's sorted competitor list
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] };
        final TimePoint endOfR1 = MillisecondsTimePoint.now();
        final TimePoint withinR1 = endOfR1.minus(1000);
        final TimePoint startOfR1 = withinR1.minus(10000);
        final TimePoint beforeStartOfR1 = startOfR1.minus(10000);
        final TimePoint afterEndOfR1 = endOfR1.plus(1000);
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Leaderboard 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new LowPoint(), null);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(startOfR1, Arrays.asList(f1)) {
            private static final long serialVersionUID = 8705622361027154428L;
            public TimePoint getEndOfRace() {
                return endOfR1;
            }
        }, "R1", /* medalRace */false);
        RaceColumn r2 = leaderboard1.addRaceColumn("R2", /* medalRace */false);
        leaderboard1.getScoreCorrection().correctScore(c[0], r2, 123.);
        assertEquals(2. + 123., leaderboard1.getNetPoints(c[0], afterEndOfR1), 0.00000001); // correction expected to apply after end of last tracked race before the corrected column
        assertEquals(2. + 0., leaderboard1.getNetPoints(c[0], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getNetPoints(c[0], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1
    }

    /**
     * When a DNS score correction is made then both, the MaxPointsReason and the score shall apply at the start of the race.
     */
    @Test
    public void testDNSScoreAppliesBeforeEndOfRace() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        final TimePoint endOfR1 = MillisecondsTimePoint.now();
        final TimePoint withinR1 = endOfR1.minus(1000);
        final TimePoint startOfR1 = withinR1.minus(10000);
        final TimePoint beforeStartOfR1 = startOfR1.minus(10000);
        final TimePoint afterEndOfR1 = endOfR1.plus(1000);
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Leaderboard 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new LowPoint(), null);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(startOfR1, Arrays.asList(c)) {
            private static final long serialVersionUID = 8705622361027154428L;
            public TimePoint getEndOfRace() {
                return endOfR1;
            }
        }, "R1", /* medalRace */false);
        leaderboard1.getScoreCorrection().correctScore(c[0], leaderboard1.getRaceColumnByName("R1"), 123.);
        leaderboard1.getScoreCorrection().setMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), MaxPointsReason.DNS);
        assertEquals(MaxPointsReason.NONE, leaderboard1.getMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), beforeStartOfR1));
        assertEquals(MaxPointsReason.DNS, leaderboard1.getMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), withinR1));
        assertEquals(MaxPointsReason.DNS, leaderboard1.getMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), afterEndOfR1));
        assertNull(leaderboard1.getTotalPoints(c[0], leaderboard1.getRaceColumnByName("R1"), beforeStartOfR1));
        assertEquals(123., leaderboard1.getTotalPoints(c[0], leaderboard1.getRaceColumnByName("R1"), withinR1), 0.00000001);
        assertEquals(123., leaderboard1.getTotalPoints(c[0], leaderboard1.getRaceColumnByName("R1"), afterEndOfR1), 0.00000001);
    }

    /**
     * When a DNS score correction is made then both, the MaxPointsReason and the score shall apply at the start of the race.
     */
    @Test
    public void testDNFScoreDoesNotApplyBeforeEndOfRace() throws NoWindException {
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        final TimePoint endOfR1 = MillisecondsTimePoint.now();
        final TimePoint withinR1 = endOfR1.minus(1000);
        final TimePoint startOfR1 = withinR1.minus(10000);
        final TimePoint beforeStartOfR1 = startOfR1.minus(10000);
        final TimePoint afterEndOfR1 = endOfR1.plus(1000);
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Leaderboard 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new LowPoint(), null);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(startOfR1, Arrays.asList(c)) {
            private static final long serialVersionUID = 8705622361027154428L;
            public TimePoint getEndOfRace() {
                return endOfR1;
            }
        }, "R1", /* medalRace */false);
        leaderboard1.getScoreCorrection().correctScore(c[0], leaderboard1.getRaceColumnByName("R1"), 123.);
        leaderboard1.getScoreCorrection().setMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), MaxPointsReason.DNF);
        assertEquals(MaxPointsReason.NONE, leaderboard1.getMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), beforeStartOfR1));
        assertEquals(MaxPointsReason.NONE, leaderboard1.getMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), withinR1));
        assertEquals(MaxPointsReason.DNF, leaderboard1.getMaxPointsReason(c[0], leaderboard1.getRaceColumnByName("R1"), afterEndOfR1));
        assertNull(leaderboard1.getTotalPoints(c[0], leaderboard1.getRaceColumnByName("R1"), beforeStartOfR1));
        assertEquals(1.0, leaderboard1.getTotalPoints(c[0], leaderboard1.getRaceColumnByName("R1"), withinR1), 0.00000001); // tracked rank is 1
        assertEquals(123., leaderboard1.getTotalPoints(c[0], leaderboard1.getRaceColumnByName("R1"), afterEndOfR1), 0.00000001);
    }


    /**
     * A test case for bug 1802. Competitors that didn't have a fleet (null fleet) were sorted to the end of the column even
     * if they had a score correction assigned.
     */
    @Test
    public void testSortingOfNullFleetCompetitorsWithScoreCorrection() throws NoWindException {
        Competitor[] c1 = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] c2 = new Competitor[] { c1[1], c1[2], c1[3] }; // bug c1[0] will then get a good score correction
        final TimePoint endOfR1 = MillisecondsTimePoint.now();
        final TimePoint withinR1 = endOfR1.minus(1000);
        final TimePoint startOfR1 = withinR1.minus(10000);
        final TimePoint afterEndOfR1 = endOfR1.plus(1000);
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Leaderboard 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new LowPoint(), null);
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(startOfR1, Arrays.asList(c1)) {
            private static final long serialVersionUID = 8705622361027154428L;
            public TimePoint getEndOfRace() {
                return endOfR1;
            }
        }, "R1", /* medalRace */false);
        RaceColumn r2 = leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(afterEndOfR1, Arrays.asList(c2)) {
            private static final long serialVersionUID = 8705622361027154428L;
            public TimePoint getEndOfRace() {
                return afterEndOfR1;
            }
                }, "R2", /* medalRace */false);
        leaderboard1.getScoreCorrection().correctScore(c1[0], r2, .9);
        assertEquals(1. + .9, leaderboard1.getNetPoints(c1[0], afterEndOfR1), 0.00000001);
        assertEquals(1, leaderboard1.getTotalRankOfCompetitor(c1[0], afterEndOfR1));
        // now assert that the competitor c1[0] is best in r2 because of the score correction, although c1[0] doesn't have a fleet
        assertEquals(0, Util.indexOf(leaderboard1.getCompetitorsFromBestToWorst(r2, afterEndOfR1), c1[0]));
    }

    @Test
    public void testApplicationOfScoreCorrectionsInRacesWithNoTrackedRaceAfterLastTrackedRaceWithMarkPassings() throws NoWindException {
        // Let c0 lead the series, c3 trail it, c1 and c2 are one-time competitors which are then suppressed in the
        // overall leaderboard, expecting c3 to rank second overall, after c0 ranking first, with c1 and c2 not appearing
        // in the overall leaderboard's sorted competitor list
        Competitor[] c = createCompetitors(4).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[2], c[0], c[1], c[3] };
        final TimePoint endOfR1 = MillisecondsTimePoint.now();
        final TimePoint withinR1 = endOfR1.minus(1000);
        final TimePoint startOfR1 = withinR1.minus(10000);
        final TimePoint beforeStartOfR1 = startOfR1.minus(10000);
        final TimePoint afterEndOfR1 = endOfR1.plus(1000);
        final Waypoint start = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("Start Pin End"),
                new MarkImpl("Start Committee Boat"), "Start", "Start"));
        final Waypoint finish = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("Finish Pin End"),
                new MarkImpl("Finish Committee Boat"), "Finish", "Finish"));
        FlexibleLeaderboard leaderboard1 = new FlexibleLeaderboardImpl("Leaderboard 1", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new LowPoint(), null);
        final MockedTrackedRaceWithStartTimeAndRanks trackedRace = new MockedTrackedRaceWithStartTimeAndRanks(startOfR1, Arrays.asList(f1)) {
            private static final long serialVersionUID = 8705622361027154428L;
            @Override
            public TimePoint getEndOfRace() {
                return endOfR1;
            }
            @Override
            public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
                ArrayListNavigableSet<MarkPassing> result = new ArrayListNavigableSet<MarkPassing>(new TimedComparator());
                result.add(new MarkPassingImpl(startOfR1, start, competitor));
                result.add(new MarkPassingImpl(endOfR1, finish, competitor));
                return result;
            }
        };
        trackedRace.getRace().getCourse().addWaypoint(0, start);
        trackedRace.getRace().getCourse().addWaypoint(1, finish);
        RaceColumn r1 = leaderboard1.addRace(trackedRace, "R1", /* medalRace */false);
        RaceColumn r2 = leaderboard1.addRaceColumn("R2", /* medalRace */false);
        leaderboard1.getScoreCorrection().correctScore(c[0], r2, 123.);
        assertEquals(2. + 123., leaderboard1.getNetPoints(c[0], afterEndOfR1), 0.00000001); // correction expected to apply after end of last tracked race before the corrected column
        assertEquals(2. + 0., leaderboard1.getNetPoints(c[0], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getNetPoints(c[0], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[1], r2, MaxPointsReason.DNS);
        assertEquals(3. + 5., leaderboard1.getNetPoints(c[1], afterEndOfR1), 0.00000001); // DNS is applied at race start (not known), so at least after the end of R1
        assertEquals(3. + 0., leaderboard1.getNetPoints(c[1], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getNetPoints(c[1], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[2], r2, MaxPointsReason.DNF);
        assertEquals(1. + 5., leaderboard1.getNetPoints(c[2], afterEndOfR1), 0.00000001); // DNF is applied after R2 finish (not known), so at least after the end of R1
        assertEquals(1. + 0., leaderboard1.getNetPoints(c[2], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getNetPoints(c[2], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[3], r1, MaxPointsReason.DNF);
        assertEquals(0., leaderboard1.getNetPoints(c[3], beforeStartOfR1), 0.00000001); // DNF does not apply before the start
        assertEquals(4., leaderboard1.getNetPoints(c[3], withinR1), 0.00000001); // correction expected NOT to apply before end of race
        assertEquals(5., leaderboard1.getNetPoints(c[3], afterEndOfR1), 0.00000001); // after race is finished, DNF applies

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[3], r1, MaxPointsReason.DNS);
        assertEquals(0., leaderboard1.getNetPoints(c[3], beforeStartOfR1), 0.00000001); // DNS does not apply before the start
        assertEquals(5., leaderboard1.getNetPoints(c[3], withinR1), 0.00000001); // correction expected to apply after the start
        assertEquals(5., leaderboard1.getNetPoints(c[3], afterEndOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[3], r1, null);
        leaderboard1.getScoreCorrection().correctScore(c[3], r1, 123.);
        assertEquals(4., leaderboard1.getNetPoints(c[3], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(123., leaderboard1.getNetPoints(c[3], afterEndOfR1), 0.00000001); // correction is applied after R1 finish
        assertEquals(0., leaderboard1.getNetPoints(c[3], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1
    }

    /**
     * See bug 1260. There must be a possibility to have ordered fleets that are scored such that the winner of the race of the best fleet
     * gets the best score in that column; and the winner of the second-best fleet gets the n-th best score in the column with n being the
     * number of competitors in the best fleet plus one; and so on. At the same time, while for a regular ISAF regatta format the participation
     * in a better fleet will always let those competitors rank better than all participants of worse fleets, here the fleet pertinence shall
     * not matter for the global ranking.
     */
    @Test
    public void testTotalRankComparatorForOrderedSplitFleetsWhoseOrderingDoesNotPersist() throws NoWindException {
        series = new ArrayList<Series>();
        final int numberOfBeforeRaces = 1;
        // -------- before series ------------
        {
            Set<? extends Fleet> beforeFleets = Collections.singleton(new FleetImpl("Default"));
            List<String> beforeRaceColumnNames = new ArrayList<String>();
            for (int i = 1; i <= numberOfBeforeRaces; i++) {
                beforeRaceColumnNames.add("R" + i);
            }
            Series qualifyingSeries = new SeriesImpl("Before", /* isMedal */false, /* isFleetsCanRunInParallel */ true, beforeFleets,
                    beforeRaceColumnNames, /* trackedRegattaRegistry */null);
            series.add(qualifyingSeries);
        }
        // -------- knock-out qualification series ------------
        {
            List<Fleet> qualificationFleets = new ArrayList<Fleet>();
            for (String qualificationFleetName : new String[] { "Yellow", "Blue" }) {
                qualificationFleets.add(new FleetImpl(qualificationFleetName));
            }
            List<String> qualificationRaceColumnNames = new ArrayList<String>();
            qualificationRaceColumnNames.add("Q");
            Series qualificationSeries = new SeriesImpl("Qualification", /* isMedal */false, /* isFleetsCanRunInParallel */ true, qualificationFleets, qualificationRaceColumnNames, /* trackedRegattaRegistry */ null);
            // discard the one and only qualification race; it doesn't score
            qualificationSeries.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[] { 1 }));
            series.add(qualificationSeries);
        }

        // -------- knock-out final series ------------
        {
            List<Fleet> finalFleets = new ArrayList<Fleet>();
            int fleetOrdering = 1;
            for (String finalFleetName : new String[] { "Gold", "Silver" }) {
                finalFleets.add(new FleetImpl(finalFleetName, fleetOrdering++));
            }
            List<String> finalRaceColumnNames = new ArrayList<String>();
            finalRaceColumnNames.add("F");
            Series finalSeries = new SeriesImpl("Final", /* isMedal */false, /* isFleetsCanRunInParallel */ true, finalFleets, finalRaceColumnNames, /* trackedRegattaRegistry */ null);
            finalSeries.setSplitFleetContiguousScoring(true);
            series.add(finalSeries);
        }
        // -------- after series ------------
        {
            final int numberOfFinalRaces = 1;
            Set<? extends Fleet> afterFleets = Collections.singleton(new FleetImpl("Default"));
            List<String> finalRaceColumnNames = new ArrayList<String>();
            for (int i = numberOfBeforeRaces+1; i <= numberOfBeforeRaces+numberOfFinalRaces; i++) {
                finalRaceColumnNames.add("R" + i);
            }
            Series finalSeries = new SeriesImpl("After", /* isMedal */false, /* isFleetsCanRunInParallel */ true, afterFleets, finalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(finalSeries);
        }
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("Extreme40", /* typicallyStartsUpwind */ false);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null,
                series, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT_FIRST_GETS_TEN), "123",
                /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        List<Competitor> competitors = createCompetitors(12);
        final int firstYellowCompetitorIndex = 3;
        List<Competitor> yellow = new ArrayList<>(competitors.subList(firstYellowCompetitorIndex, firstYellowCompetitorIndex+6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);
        final int firstGoldCompetitorIndex = 5;
        List<Competitor> gold = new ArrayList<>(competitors.subList(firstGoldCompetitorIndex, firstGoldCompetitorIndex+6));
        List<Competitor> silver = new ArrayList<>(competitors);
        silver.removeAll(gold);
        Collections.shuffle(gold);
        Collections.shuffle(silver);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn r1Column = series.get(0).getRaceColumnByName("R1");
        TrackedRace r1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        r1Column.setTrackedRace(r1Column.getFleetByName("Default"), r1Default);
        RaceColumn qColumn = series.get(1).getRaceColumnByName("Q");
        TrackedRace qYellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
        qColumn.setTrackedRace(qColumn.getFleetByName("Yellow"), qYellow);
        TrackedRace qBlue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
        qColumn.setTrackedRace(qColumn.getFleetByName("Blue"), qBlue);
        RaceColumn f1Column = series.get(2).getRaceColumnByName("F");
        TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
        f1Column.setTrackedRace(f1Column.getFleetByName("Gold"), f1Gold);
        TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, silver);
        f1Column.setTrackedRace(f1Column.getFleetByName("Silver"), f1Silver);
        RaceColumn r2Column = series.get(3).getRaceColumnByName("R2");
        TrackedRace r2Default = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        r2Column.setTrackedRace(r2Column.getFleetByName("Default"), r2Default);

        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        Map<Competitor, Double> netPoints = new LinkedHashMap<>();
        for (Competitor rankedCompetitor : rankedCompetitors) {
            netPoints.put(rankedCompetitor, leaderboard.getNetPoints(rankedCompetitor, later));
        }
        // assert that the final column competitors are ranked from top to bottom:
        double scoreInFinalRace = 10.0;
        for (Competitor rankedCompetitor : leaderboard.getCompetitorsFromBestToWorst(f1Column, later)) {
            assertEquals(scoreInFinalRace, leaderboard.getNetPoints(rankedCompetitor, f1Column, later), 0.000001);
            scoreInFinalRace = Math.max(scoreInFinalRace-1, 1);
        }
        double lastScore = Double.MAX_VALUE;
        // assert that only the points matter for ranking; not the fleet assignment
        for (Competitor rankedCompetitor : rankedCompetitors) {
            // with a high-point scoring scheme, assert that as competitors get worse, scores get less
            double rankedCompetitorScore = netPoints.get(rankedCompetitor);
            assertTrue(rankedCompetitorScore <= lastScore, "Expected " + rankedCompetitor + " with rank "
                            + (Util.indexOf(rankedCompetitors, rankedCompetitor) + 1)
                            + " to have worse (lesser) score than its immediate better competitor who scored " + lastScore
                            + " but was " + rankedCompetitorScore);
            lastScore = rankedCompetitorScore;
            // assert that the qualification race consistently has zero points for all competitors because it is discarded
            assertTrue(leaderboard.isDiscarded(rankedCompetitor, qColumn, later));
        }
    }


    /**
     * See bug 3752: when the medal race participants do not race in a "Last Race" gold fleet and the "Last Race" column comes before the
     * medal race column, medal race participants must still rank better than all others; participants not in the medal race and not in
     * any of the gold and silver fleet of the last race may rank between gold and silver fleet based on the "extreme fleet" idea.
     */
    @Test
    public void testTotalRankComparatorForOrderedSplitFleetsWithMedalRaceParticipantsNotRacingInLastRaceGoldFleet() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        {
            List<Fleet> qualificationFleets = new ArrayList<Fleet>();
            for (String qualificationFleetName : new String[] { "Yellow", "Blue" }) {
                qualificationFleets.add(new FleetImpl(qualificationFleetName));
            }
            List<String> qualificationRaceColumnNames = new ArrayList<String>();
            qualificationRaceColumnNames.add("Q");
            Series qualificationSeries = new SeriesImpl("Qualification", /* isMedal */false, /* isFleetsCanRunInParallel */ true, qualificationFleets, qualificationRaceColumnNames, /* trackedRegattaRegistry */ null);
            // discard the one and only qualification race; it doesn't score
            qualificationSeries.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[] { 1 }));
            series.add(qualificationSeries);
        }

        // -------- final series ------------
        {
            List<Fleet> finalFleets = new ArrayList<Fleet>();
            int fleetOrdering = 1;
            for (String finalFleetName : new String[] { "Gold", "Silver" }) {
                finalFleets.add(new FleetImpl(finalFleetName, fleetOrdering++));
            }
            List<String> finalRaceColumnNames = new ArrayList<String>();
            finalRaceColumnNames.add("F");
            Series finalSeries = new SeriesImpl("Final", /* isMedal */false, /* isFleetsCanRunInParallel */ true, finalFleets, finalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(finalSeries);
        }
        // -------- last race series ------------
        {
            List<Fleet> lastRaceFleets = new ArrayList<Fleet>();
            int fleetOrdering = 1;
            for (String finalFleetName : new String[] { "Gold", "Silver" }) {
                lastRaceFleets.add(new FleetImpl(finalFleetName, fleetOrdering++));
            }
            List<String> lastRaceColumnNames = new ArrayList<String>();
            lastRaceColumnNames.add("L");
            Series lastRaceSeries = new SeriesImpl("Last Race", /* isMedal */false, /* isFleetsCanRunInParallel */ true, lastRaceFleets, lastRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(lastRaceSeries);
        }
        // -------- medal series ------------
        {
            Set<? extends Fleet> medalFleets = Collections.singleton(new FleetImpl("Default"));
            List<String> medalRaceColumnNames = new ArrayList<String>();
            medalRaceColumnNames.add("M");
            Series medalSeries = new SeriesImpl("Medal", /* isMedal */true, /* isFleetsCanRunInParallel */ true, medalFleets, medalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(medalSeries);
        }
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null,
                series, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                "123", /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        List<Competitor> competitors = createCompetitors(12);
        final int firstYellowCompetitorIndex = 3;
        List<Competitor> yellow = new ArrayList<>(competitors.subList(firstYellowCompetitorIndex, firstYellowCompetitorIndex+6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);
        final int firstGoldCompetitorIndex = 5;
        List<Competitor> gold = new ArrayList<>(competitors.subList(firstGoldCompetitorIndex, firstGoldCompetitorIndex+6));
        List<Competitor> silver = new ArrayList<>(competitors);
        silver.removeAll(gold);
        Collections.shuffle(gold);
        Collections.shuffle(silver);
        List<Competitor> lastRaceSilver = new ArrayList<>(silver);
        final Competitor theUntrackedCompetitorInLastRace = lastRaceSilver.get(lastRaceSilver.size()-1);
        lastRaceSilver.remove(theUntrackedCompetitorInLastRace); // one participant accidentally not tracked; expected to end up between silver and gold
        List<Competitor> medal = new ArrayList<>(gold.subList(0, 2)); // take two gold race participants as medal race participants
        List<Competitor> lastRaceGold = new ArrayList<>(gold);
        lastRaceGold.removeAll(medal); // no medal race participant participates in the last race's gold fleet

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn qColumn = series.get(0).getRaceColumnByName("Q");
        TrackedRace qYellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
        qColumn.setTrackedRace(qColumn.getFleetByName("Yellow"), qYellow);
        TrackedRace qBlue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
        qColumn.setTrackedRace(qColumn.getFleetByName("Blue"), qBlue);
        RaceColumn fColumn = series.get(1).getRaceColumnByName("F");
        TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
        fColumn.setTrackedRace(fColumn.getFleetByName("Gold"), f1Gold);
        TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, silver);
        fColumn.setTrackedRace(fColumn.getFleetByName("Silver"), f1Silver);
        RaceColumn lastRaceColumn = series.get(2).getRaceColumnByName("L");
        TrackedRace lGold = new MockedTrackedRaceWithStartTimeAndRanks(now, lastRaceGold);
        lastRaceColumn.setTrackedRace(lastRaceColumn.getFleetByName("Gold"), lGold);
        TrackedRace lSilver = new MockedTrackedRaceWithStartTimeAndRanks(now, lastRaceSilver);
        lastRaceColumn.setTrackedRace(lastRaceColumn.getFleetByName("Silver"), lSilver);
        RaceColumn medalColumn = series.get(3).getRaceColumnByName("M");
        TrackedRace mDefault = new MockedTrackedRaceWithStartTimeAndRanks(now, medal);
        medalColumn.setTrackedRace(medalColumn.getFleetByName("Default"), mDefault);

        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        Map<Competitor, Double> netPoints = new LinkedHashMap<>();
        for (Competitor rankedCompetitor : rankedCompetitors) {
            netPoints.put(rankedCompetitor, leaderboard.getNetPoints(rankedCompetitor, later));
        }
        // assert that all medal participants rank better than all other participants
        for (final Competitor medalCompetitor : medal) {
            for (final Competitor c : competitors) {
                if (!medal.contains(c)) {
                    assertTrue(Util.indexOf(rankedCompetitors, medalCompetitor) < Util.indexOf(rankedCompetitors, c));
                }
            }
        }
        // assert that all last race's gold participants rank better than all silver participants
        for (final Competitor lastRaceGoldParticipant : lastRaceGold) {
            for (final Competitor silverParticipant : silver) {
                assertTrue(Util.indexOf(rankedCompetitors, lastRaceGoldParticipant) < Util.indexOf(rankedCompetitors, silverParticipant));
            }
        }
        // assert that theUntrackedCompetitorInLastRace ended up ranked worse than all participants of ranked fleets:
        for (final Competitor c : competitors) {
            if (c != theUntrackedCompetitorInLastRace) {
                assertTrue(Util.indexOf(rankedCompetitors, c) < Util.indexOf(rankedCompetitors, theUntrackedCompetitorInLastRace));
            }
        }
    }

    /**
     * See bug 3752 comment #6: when in an earlier race a competitor does not have a fleet assigned and the other one does, but in
     * a later race in the same series both competitors are assigned to the same fleet, the authoritative answer based on the fleets
     * must be that they compare equal; an earlier default based on "extreme fleet" considerations must be discarded if fleet equality
     * is definitely established.
     */
    @Test
    public void testTotalRankComparatorForOrderedSplitFleetsWithUnknownFleetInPreviousRaceAndSameFleetInLaterRace() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        {
            List<Fleet> qualificationFleets = new ArrayList<Fleet>();
            for (String qualificationFleetName : new String[] { "Yellow", "Blue" }) {
                qualificationFleets.add(new FleetImpl(qualificationFleetName));
            }
            List<String> qualificationRaceColumnNames = new ArrayList<String>();
            qualificationRaceColumnNames.add("Q");
            Series qualificationSeries = new SeriesImpl("Qualification", /* isMedal */false, /* isFleetsCanRunInParallel */ true, qualificationFleets, qualificationRaceColumnNames, /* trackedRegattaRegistry */ null);
            // discard the one and only qualification race; it doesn't score
            qualificationSeries.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[] { 1 }));
            series.add(qualificationSeries);
        }

        // -------- final series ------------
        {
            List<Fleet> finalFleets = new ArrayList<Fleet>();
            int fleetOrdering = 1;
            for (String finalFleetName : new String[] { "Gold", "Silver" }) {
                finalFleets.add(new FleetImpl(finalFleetName, fleetOrdering++));
            }
            List<String> finalRaceColumnNames = Arrays.asList("F1", "F2");
            Series finalSeries = new SeriesImpl("Final", /* isMedal */false, /* isFleetsCanRunInParallel */ true, finalFleets, finalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(finalSeries);
        }
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null,
                series, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                "123", /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        List<Competitor> competitors = createCompetitors(12);
        final int firstYellowCompetitorIndex = 3;
        List<Competitor> yellow = new ArrayList<>(competitors.subList(firstYellowCompetitorIndex, firstYellowCompetitorIndex+6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);
        final int firstGoldCompetitorIndex = 5;
        List<Competitor> gold = new ArrayList<>(competitors.subList(firstGoldCompetitorIndex, firstGoldCompetitorIndex+6));
        List<Competitor> silver = new ArrayList<>(competitors);
        silver.removeAll(gold);
        Collections.shuffle(gold);
        Collections.shuffle(silver);
        List<Competitor> lastRaceSilver = new ArrayList<>(silver);
        final Competitor theUntrackedCompetitorInLastRace = lastRaceSilver.get(lastRaceSilver.size()-1);
        lastRaceSilver.remove(theUntrackedCompetitorInLastRace); // one participant accidentally not tracked; expected to end up between silver and gold
        List<Competitor> medal = new ArrayList<>(gold.subList(0, 2)); // take two gold race participants as medal race participants
        List<Competitor> lastRaceGold = new ArrayList<>(gold);
        lastRaceGold.removeAll(medal); // no medal race participant participates in the last race's gold fleet

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn qColumn = series.get(0).getRaceColumnByName("Q");
        TrackedRace qYellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
        qColumn.setTrackedRace(qColumn.getFleetByName("Yellow"), qYellow);
        TrackedRace qBlue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
        qColumn.setTrackedRace(qColumn.getFleetByName("Blue"), qBlue);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
        f1Column.setTrackedRace(f1Column.getFleetByName("Gold"), f1Gold);
        final List<Competitor> f1SilverWithOneMissing = new ArrayList<>(silver);
        final Competitor missing = silver.get(silver.size()-1); // this competitor is not assigned to a fleet for F1, but for F2
        f1SilverWithOneMissing.remove(missing);
        TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, f1SilverWithOneMissing);
        f1Column.setTrackedRace(f1Column.getFleetByName("Silver"), f1Silver);
        leaderboard.getScoreCorrection().correctScore(missing, f1Column, silver.size()); // set score to what it would have been
        RaceColumn f2Column = series.get(1).getRaceColumnByName("F2");
        TrackedRace f2Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
        f2Column.setTrackedRace(f2Column.getFleetByName("Gold"), f2Gold);
        TrackedRace f2Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, silver);
        f2Column.setTrackedRace(f2Column.getFleetByName("Silver"), f2Silver);

        Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        // the competitor missing from F1 Silver has worst score in both Silver fleet races and must rank last;
        // with the bug still present, she would have ranked between Gold and Silver
        assertEquals(missing, Util.last(rankedCompetitors));
    }

    /**
     * Tests the default behavior of contiguously scored fleets when the ranks of the fleets are zero.
     * <p>
     * Expected behavior:<br>
     * Fleets are treated as if there was no contiguous scoring. Accordingly, ranks are present twice, with fleets always
     * alternating. Within a fleet rank, the order is determined by the {@link Competitor#getName() name} of the
     * competitor. The ordering within a rank is not tested within this test.
     */
    @Test
    public void testTotalRankComparatorForOrderedSplitFleetsWithZeroAsRank() throws NoWindException {
        testTotalRankComparatorForOrderedSplitFleets(/* fleetOrdering */ 0);
    }

    /**
     * Tests the default behavior of contiguously scored fleets when the ranks of the fleets are not zero.
     * This test was added because the internal behavior for fleets with a non-zero rank is different from
     * the behavior of fleets with a rank equal to zero.<p>
     * Expected behavior: <br>
     * Fleets are treated as if there was no contiguous scoring. Accordingly, ranks are present twice, with fleets always
     * alternating. Within a fleet rank, the order is determined by the {@link Competitor#getName() name} of the
     * competitor. The ordering within a rank is not tested within this test.
     */
    @Test
    public void testTotalRankComparatorForOrderedSplitFleetsWithOneAsRank() throws NoWindException {
        testTotalRankComparatorForOrderedSplitFleets(/* fleetOrdering */ 1);
    }

    private void testTotalRankComparatorForOrderedSplitFleets(final int fleetOrdering) {
        series = new ArrayList<>();
        // -------- series with zero as rank ------------
        {
            final List<Fleet> fleets = new ArrayList<>();
            for (String FleetName : new String[] { "Yellow", "Blue" }) {
                fleets.add(new FleetImpl(FleetName, fleetOrdering));
            }
            final List<String> raceColumnNames = new ArrayList<>();
            raceColumnNames.add("R1");
            final Series zeroRankSeries = new SeriesImpl("zero Rank", /* isMedal */false, /* isFleetsCanRunInParallel */ true, fleets, raceColumnNames, /* trackedRegattaRegistry */ null);
            zeroRankSeries.setSplitFleetContiguousScoring(true);
            series.add(zeroRankSeries);
        }
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true);
        final Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null,
                series, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                "123", /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        final List<Competitor> competitors = createCompetitors(12);
        final int firstYellowCompetitorIndex = 3;
        final List<Competitor> yellow = new ArrayList<>(competitors.subList(firstYellowCompetitorIndex, firstYellowCompetitorIndex+4));
        final List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);
        final Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        final TimePoint now = MillisecondsTimePoint.now();
        final TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        final RaceColumn r1Column = series.get(0).getRaceColumnByName("R1");
        final TrackedRace r1Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
        r1Column.setTrackedRace(r1Column.getFleetByName("Yellow"), r1Yellow);
        final TrackedRace r1Blue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
        r1Column.setTrackedRace(r1Column.getFleetByName("Blue"), r1Blue);
        final Iterable<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        final Map<Competitor, Double> netPoints = new LinkedHashMap<>();
        for (Competitor rankedCompetitor : rankedCompetitors) {
            netPoints.put(rankedCompetitor, leaderboard.getNetPoints(rankedCompetitor, later));
        }

        /*
         * Checks all ranks that have been awarded twice (in both fleets).
         * This is done by iterating over each "rank pair".
         */
        for (int i = 0; i < Math.min(yellow.size(), blue.size()) * 2; i = i + 2) {
            final Competitor comp1 = Util.get(rankedCompetitors, i);
            final Competitor comp2 = Util.get(rankedCompetitors, i + 1);
            assertEquals(netPoints.get(comp1), netPoints.get(comp2));
            assertNotSame(r1Column.getFleetOfCompetitor(comp1), r1Column.getFleetOfCompetitor(comp2));
        }
        // Checks all ranks that have been awarded once.
        for (int i = Math.min(yellow.size(), blue.size()) * 2; i < (yellow.size() + blue.size()) - 1; i++) {
            final Competitor comp1 = Util.get(rankedCompetitors, i);
            final Competitor comp2 = Util.get(rankedCompetitors, i + 1);
            assertNotEquals(netPoints.get(comp1), netPoints.get(comp2));
            assertSame(r1Column.getFleetOfCompetitor(comp1), r1Column.getFleetOfCompetitor(comp2));
        }
    }



    /**
     * See bug 3798: special discarding rule that limits the number of discards for the final series; configured
     * by {@link Series#getMaximumNumberOfDiscards()}.
     */
    @Test
    public void testDiscardingWithLimitOnFinalSeries() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        {
            List<Fleet> qualificationFleets = new ArrayList<Fleet>();
            for (String qualificationFleetName : new String[] { "Yellow", "Blue" }) {
                qualificationFleets.add(new FleetImpl(qualificationFleetName));
            }
            List<String> qualificationRaceColumnNames = new ArrayList<String>();
            for (int q=1; q<=7; q++) {
                qualificationRaceColumnNames.add("Q"+q);
            }
            Series qualificationSeries = new SeriesImpl("Qualification", /* isMedal */false, /* isFleetsCanRunInParallel */ true, qualificationFleets, qualificationRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(qualificationSeries);
        }

        // -------- final series ------------
        {
            List<Fleet> finalFleets = new ArrayList<Fleet>();
            int fleetOrdering = 1;
            for (String finalFleetName : new String[] { "Gold", "Silver" }) {
                finalFleets.add(new FleetImpl(finalFleetName, fleetOrdering++));
            }
            List<String> finalRaceColumnNames = new ArrayList<String>();
            for (int f=1; f<=7; f++) {
                finalRaceColumnNames.add("F"+f);
            }
            Series finalSeries = new SeriesImpl("Final", /* isMedal */false, /* isFleetsCanRunInParallel */ true, finalFleets, finalRaceColumnNames, /* trackedRegattaRegistry */ null);
            finalSeries.setMaximumNumberOfDiscards(1);
            series.add(finalSeries);
        }
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null,
                series, /* persistent */false, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                "123", /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        final int TOTAL_NUMBER_OF_COMPETITORS = 100;
        List<Competitor> competitors = createCompetitors(TOTAL_NUMBER_OF_COMPETITORS);
        final int firstYellowCompetitorIndex = TOTAL_NUMBER_OF_COMPETITORS/4;
        List<Competitor> yellow = new ArrayList<>(competitors.subList(firstYellowCompetitorIndex, firstYellowCompetitorIndex+TOTAL_NUMBER_OF_COMPETITORS/2));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);
        final int firstGoldCompetitorIndex = TOTAL_NUMBER_OF_COMPETITORS/3;
        List<Competitor> gold = new ArrayList<>(competitors.subList(firstGoldCompetitorIndex, firstGoldCompetitorIndex+TOTAL_NUMBER_OF_COMPETITORS/2));
        List<Competitor> silver = new ArrayList<>(competitors);
        silver.removeAll(gold);
        Collections.shuffle(gold);
        Collections.shuffle(silver);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[] { 5, 10 });
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        for (int q = 1; q <= 7; q++) {
            RaceColumn qColumn = series.get(0).getRaceColumnByName("Q"+q);
            TrackedRace qYellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
            qColumn.setTrackedRace(qColumn.getFleetByName("Yellow"), qYellow);
            TrackedRace qBlue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
            qColumn.setTrackedRace(qColumn.getFleetByName("Blue"), qBlue);
        }
        for (int f = 1; f <= 7; f++) {
            RaceColumn fColumn = series.get(1).getRaceColumnByName("F"+f);
            TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
            fColumn.setTrackedRace(fColumn.getFleetByName("Gold"), f1Gold);
            TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, silver);
            fColumn.setTrackedRace(fColumn.getFleetByName("Silver"), f1Silver);
        }

        for (final Competitor c : competitors) {
            int totalDiscards = 0;
            int discardsInFinalSeries = 0;
            int numberOfRacesInFinalThatWereWorseThanWorstQualificationRace = 0;
            double worstQualificationScore = 0;
            for (final RaceColumn rc : leaderboard.getRaceColumns()) {
                double score = leaderboard.getTotalPoints(c, rc, later);
                if (rc instanceof RaceColumnInSeries && ((RaceColumnInSeries) rc).getSeries() == regatta.getSeriesByName("Qualification")) {
                    if (score > worstQualificationScore) {
                        worstQualificationScore = score;
                    }
                } else {
                    if (score > worstQualificationScore) {
                        numberOfRacesInFinalThatWereWorseThanWorstQualificationRace++;
                    }
                }
                if (leaderboard.isDiscarded(c, rc, later)) {
                    totalDiscards++;
                    if (rc instanceof RaceColumnInSeries && ((RaceColumnInSeries) rc).getSeries() == regatta.getSeriesByName("Final")) {
                        discardsInFinalSeries++;
                    }
                }
            }
            assertEquals(2, totalDiscards);
            assertTrue(discardsInFinalSeries <= 1);
            assertTrue(numberOfRacesInFinalThatWereWorseThanWorstQualificationRace == 0 || discardsInFinalSeries > 0);
        }
    }
}
