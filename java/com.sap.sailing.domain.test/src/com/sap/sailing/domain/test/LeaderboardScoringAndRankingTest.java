package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.HighPointExtremeSailingSeriesOverall;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets10LastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.TimedComparator;
import com.sap.sailing.util.impl.ArrayListNavigableSet;

public class LeaderboardScoringAndRankingTest extends AbstractLeaderboardTest {
    private ArrayList<Series> series;

    private Leaderboard createLeaderboard(Regatta regatta, int[] discardingThresholds) {
        ThresholdBasedResultDiscardingRuleImpl discardingRules = new ThresholdBasedResultDiscardingRuleImpl(discardingThresholds);
        return new RegattaLeaderboardImpl(regatta, discardingRules);
    }

    @Test
    public void testOneStartedRaceWithDifferentScores() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1,
                new String[] { "Default" },
                /* medal */false, "testOneStartedRaceWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        TrackedRace f1 = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        f1Column.setTrackedRace(f1Column.getFleets().iterator().next(), f1);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors, rankedCompetitors);
    }

    /**
     * Regarding bug 912, test adding a disqualification in the middle, with a high-point scoring scheme, and check that
     * all competitors ranked worse advance by one, including getting <em>more</em> points due to the high-point scoring
     * scheme. Note that this does not test the total points given for those competitors.
     */
    @Test
    public void testOneStartedRaceWithDifferentScoresAndDisqualificationUsingHighPointScoringScheme() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1,
                new String[] { "Default" },
                /* medal */false, "testOneStartedRaceWithDifferentScores",
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
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors.subList(0, 5), rankedCompetitors.subList(0, 5));
        assertEquals(competitors.subList(6, 10), rankedCompetitors.subList(5, 9));
        assertEquals(competitors.get(5), rankedCompetitors.get(9));

        // Now test the total points and make sure the other competitors advanced by one, too
        assertEquals(0, leaderboard.getTotalPoints(competitors.get(5), f1Column, now), 0.000000001);
        for (int i=0; i<5; i++) {
            assertEquals(10-i, leaderboard.getTotalPoints(competitors.get(i), f1Column, now), 0.000000001);
        }
        for (int i=6; i<10; i++) {
            assertEquals(10-(i-1), leaderboard.getTotalPoints(competitors.get(i), f1Column, now), 0.000000001);
        }
    }

    @Test
    public void testColumnFactorInRegattaLeaderboard() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "Default" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, "testColumnFactorInRegattaLeaderboard",
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
        assertEquals(Double.valueOf(competitors.size()), leaderboard.getTotalPoints(competitors.get(0), q1Column, later));
        assertEquals(factor * Double.valueOf(1), leaderboard.getTotalPoints(competitors.get(0), q2Column, later), 0.000000001);
        assertEquals(Double.valueOf(factor*1.0+competitors.size()), leaderboard.getTotalPoints(competitors.get(0), later), 0.000000001);
    }

    /**
     * Regarding bug 961, test scoring in a leaderboard that has a qualification series with two unordered groups where for one
     * column only one group has raced (expressed by a mocked TrackedRace attached to the column). Ensure that the column does
     * count for the total points sum but that the competitor ordering is controlled by the number of races.
     */
    @Test
    public void testUnorderedGroupsWithOneGroupNotHavingRacedInAColumn() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */ 2, new String[] { "Yellow", "Blue" }, /* final */0,
                new String[] { "Default" },
                /* medal */false, "testUnorderedGroupsWithOneGroupNotHavingRacedInAcolumn",
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
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertTrue(competitors.get(i) == rankedCompetitors.get(2*i) || competitors.get(i) == rankedCompetitors.get(2*i+1));
            assertTrue(competitors.get(i+5) == rankedCompetitors.get(2*i) || competitors.get(i+5) == rankedCompetitors.get(2*i+1));
            assertEquals((double) (i+1), leaderboard.getTotalPoints(competitors.get(i), later), 0.000000001);
            assertEquals((double) (i+1), leaderboard.getTotalPoints(competitors.get(i+5), later), 0.0000000001);
        }
        // now add one race for yellow fleet and test that it counts for the score but not the ordering
        // because blue fleet is still missing its race for Q2
        TrackedRace q2Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, competitors.subList(3, 8));
        RaceColumn q2Column = qualificationSeries.getRaceColumnByName("Q2");
        q2Column.setTrackedRace(q2Column.getFleetByName("Yellow"), q2Yellow);
        List<Competitor> rankedCompetitorsWithOneRaceMissingInQ2 = leaderboard.getCompetitorsFromBestToWorst(later);
        // scores: C1=1, C2=2, C3=3, C4=5, C5=7, C6=4, C7=6, C8=8, C9=4, C10=5
        // ordered by scores: C1, C2, C3, C6/C9, C4/C10, C7, C5, C8
        // ordered primarily by number of races: C6, C4, C7, C5, C8, C1, C2, C3, C9, C10
        assertEquals(Arrays.asList(new Competitor[] { competitors.get(5), competitors.get(3), competitors.get(6),
                competitors.get(4), competitors.get(7), competitors.get(0), competitors.get(1), competitors.get(2),
                competitors.get(8), competitors.get(9) }), rankedCompetitorsWithOneRaceMissingInQ2);
        double[] points = new double[] { 1, 2, 3, 5, 7, 4, 6, 8, 4, 5 };
        for (int i=0; i<9; i++) {
            assertEquals(points[i], leaderboard.getTotalPoints(competitors.get(i), later), 0.000000001);
        }
        // now add a tracked race for the blue fleet for Q2 and assert that the Q2 scores count for the total points sum
        TrackedRace q2Blue = new MockedTrackedRaceWithStartTimeAndRanks(now,
                Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                        competitors.get(8), competitors.get(9) }));
        q2Column.setTrackedRace(q2Column.getFleetByName("Blue"), q2Blue);
        // the new order in Q2 expected to be { (0, 3), (1, 4), (2, 5), (8, 6), (9, 7) }
        // therefore the new total points, in ascending order, are expected to be:
        // { 0: 1+1=2; 5: 1+3=4; 1: 2+2=4; 3: 4+1=5; 2: 3+3=6; 6: 2+4=6; 4: 5+2=7; 7: 3+5=8; 8: 4+4=8; 9: 5+5=10 }
        double[] expectedTotalPoints = new double[] { 2, 4, 6, 5, 7, 4, 6, 8, 8, 10 };
        int[] expectedOrderAfterTwoFullRacesPlusMinusOne = new int[] { 0, 5, 1, 3, 2, 6, 4, 7, 8, 9 };
        List<Competitor> rankedCompetitorsWithAllRacesInQ2 = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertTrue(competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i]) == rankedCompetitorsWithAllRacesInQ2.get(2*i) ||
                    competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i]) == rankedCompetitorsWithAllRacesInQ2.get(2*i+1));
            assertTrue(competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i+1]) == rankedCompetitorsWithAllRacesInQ2.get(2*i) ||
                    competitors.get(expectedOrderAfterTwoFullRacesPlusMinusOne[2*i+1]) == rankedCompetitorsWithAllRacesInQ2.get(2*i+1));
            assertEquals(expectedTotalPoints[2*i], leaderboard.getTotalPoints(competitors.get(2*i), later), 0.000000001);
            assertEquals(expectedTotalPoints[2*i+1], leaderboard.getTotalPoints(competitors.get(2*i+1), later), 0.000000001);
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
                /* medal */false, "testUnorderedGroupsWithOneGroupNotHavingRacedInAcolumn",
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
        // now add a tracked race for the blue fleet for Q2 that hasn't started yet and assert that the Q2 scores still don't count for the total points sum
        TimePoint muchLater = later.plus(10000000000l);
        TrackedRace q2Blue = new MockedTrackedRaceWithStartTimeAndRanks(muchLater,
                Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                        competitors.get(8), competitors.get(9) }));
        q2Column.setTrackedRace(q2Column.getFleetByName("Blue"), q2Blue);
        List<Competitor> rankedCompetitorsWithOneRaceMissingInQ2 = leaderboard.getCompetitorsFromBestToWorst(later);
        // scores: C1=1, C2=2, C3=3, C4=5, C5=7, C6=4, C7=6, C8=8, C9=4, C10=5
        // ordered by scores: C1, C2, C3, C6/C9, C4/C10, C7, C5, C8
        // ordered primarily by number of races: C6, C4, C7, C5, C8, C1, C2, C3, C9, C10
        assertEquals(Arrays.asList(new Competitor[] { competitors.get(5), competitors.get(3), competitors.get(6),
                competitors.get(4), competitors.get(7), competitors.get(0), competitors.get(1), competitors.get(2),
                competitors.get(8), competitors.get(9) }), rankedCompetitorsWithOneRaceMissingInQ2);
        double[] points = new double[] { 1, 2, 3, 5, 7, 4, 6, 8, 4, 5 };
        for (int i=0; i<9; i++) {
            assertEquals(points[i], leaderboard.getTotalPoints(competitors.get(i), later), 0.000000001);
        }
        // expect all results to be valid because a fleet will have its score counted even if not all fleets have raced;
        // see discussion for bug 961, but more importantly later on bug 1023.
        assertTrue(leaderboard.getScoringScheme().isValidInTotalScore(leaderboard, q2Column, later));
        // now add a score correction for Q2/Blue to make it count:
        leaderboard.getScoreCorrection().correctScore(competitors.get(9), q2Column, 42.);
        assertTrue(leaderboard.getScoringScheme().isValidInTotalScore(leaderboard, q2Column, later));
        // the new order in Q2 expected to be { (9, 3), ... } (we don't know about any competitor in q2Blue but #9
        // therefore the new total points for #9 are
        // { 9: 5+42=47 }
        assertEquals(47.0, leaderboard.getTotalPoints(competitors.get(9), later), 0.00000001);
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
                /* medal */false, "testDiscardsForUnorderedGroupsWithOneGroupNotHavingRacedInAColumn",
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
            assertTrue("Competitor "+competitor+" has no discard but should have one",
                    leaderboard.isDiscarded(competitor, q1Column, later) || leaderboard.isDiscarded(competitor, q2Column, later));
        }
        for (Competitor competitor : Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                competitors.get(8), competitors.get(9) })) {
            assertFalse("Competitor "+competitor+" has a discard but should'nt have one",
                    leaderboard.isDiscarded(competitor, q1Column, later) || leaderboard.isDiscarded(competitor, q2Column, later));
        }
        // now add a tracked race for the blue fleet for Q2 and assert that all competitors have one discard
        TrackedRace q2Blue = new MockedTrackedRaceWithStartTimeAndRanks(now,
                Arrays.asList(new Competitor[] { competitors.get(0), competitors.get(1), competitors.get(2),
                        competitors.get(8), competitors.get(9) }));
        q2Column.setTrackedRace(q2Column.getFleetByName("Blue"), q2Blue);
        for (Competitor competitor : competitors) {
            assertTrue("Competitor "+competitor+" has no discard but should", 
                    leaderboard.isDiscarded(competitor, q1Column, later) || leaderboard.isDiscarded(competitor, q2Column, later));
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
                /* medal */false, "testDiscardsForOrderedGroupsWithOneGroupNotHavingRacedInAColumn",
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
            assertTrue("Competitor "+competitors.get(i)+" has no discard in F1 or F2 but should",
                    leaderboard.isDiscarded(competitors.get(i), f1Column, later) ||
                    leaderboard.isDiscarded(competitors.get(i), f2Column, later));
        }
        for (int i=5; i<10; i++) {
            assertFalse("Competitor "+competitors.get(i)+" has a discard in F1 or F2 but shouldn't",
                    leaderboard.isDiscarded(competitors.get(i), f1Column, later) ||
                    leaderboard.isDiscarded(competitors.get(i), f2Column, later));
        }
    }

    /**
     * Asserts that the competitors ranking worse than the disqualified competitor advance by one in the
     * {@link Leaderboard#getCompetitorsFromBestToWorst(TimePoint)} ordering. Note that this does not test
     * the total points given for those competitors.
     */
    @Test
    public void testOneStartedRaceWithDifferentScoresAndDisqualification() throws NoWindException {
        List<Competitor> competitors = createCompetitors(10);
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */1,
                new String[] { "Default" },
                /* medal */false, "testOneStartedRaceWithDifferentScoresAndDisqualification",
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
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(competitors.subList(0, 5), rankedCompetitors.subList(0, 5));
        assertEquals(competitors.subList(6, 10), rankedCompetitors.subList(5, 9));
        assertEquals(competitors.get(5), rankedCompetitors.get(9));

        // Now test the total points and make sure the other competitors advanced by one, too
        assertEquals(11, leaderboard.getTotalPoints(competitors.get(5), f1Column, now), 0.000000001);
        for (int i=0; i<5; i++) {
            assertEquals(i+1, leaderboard.getTotalPoints(competitors.get(i), f1Column, now), 0.000000001);
        }
        for (int i=6; i<10; i++) {
            assertEquals(i, leaderboard.getTotalPoints(competitors.get(i), f1Column, now), 0.000000001);
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
                /* medal */false, "testDistributionAcrossQualifyingFleetsWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn q1Column = series.get(0).getRaceColumnByName("Q1");
        TrackedRace q1Yellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
        q1Column.setTrackedRace(q1Column.getFleetByName("Yellow"), q1Yellow);
        TrackedRace q1Blue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
        q1Column.setTrackedRace(q1Column.getFleetByName("Blue"), q1Blue);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            Competitor first = rankedCompetitors.get(2*i);
            Competitor second = rankedCompetitors.get(2*i+1);
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
                /* medal */false, "testSimpleLeaderboardWithHighPointScoringScheme",
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
            assertEquals(NUMBER_OF_COMPETITORS/2+1-rank, leaderboard.getNetPoints(goldCompetitor, f1Column, later), 0.00000001);
            rank++;
        }
        rank=1;
        for (Competitor silverCompetitor : silver) {
            assertEquals(rank, f1Column.getTrackedRace(silverCompetitor).getRank(silverCompetitor, later));
            assertEquals(rank, leaderboard.getTrackedRank(silverCompetitor, f1Column, later));
            assertEquals(NUMBER_OF_COMPETITORS/2+1-rank, leaderboard.getNetPoints(silverCompetitor, f1Column, later), 0.00000001);
            rank++;
        }
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertSame(gold.get(i), rankedCompetitors.get(i));
            assertSame(silver.get(i), rankedCompetitors.get(i+5));
        }
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
                /* medal */false, "testDistributionAcrossFinalFleetsWithDifferentScores",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        RaceColumn f1Column = series.get(1).getRaceColumnByName("F1");
        TrackedRace f1Gold = new MockedTrackedRaceWithStartTimeAndRanks(now, gold);
        f1Column.setTrackedRace(f1Column.getFleetByName("Gold"), f1Gold);
        TrackedRace f1Silver = new MockedTrackedRaceWithStartTimeAndRanks(now, silver);
        f1Column.setTrackedRace(f1Column.getFleetByName("Silver"), f1Silver);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        for (int i=0; i<5; i++) {
            assertSame(gold.get(i), rankedCompetitors.get(i));
            assertSame(silver.get(i), rankedCompetitors.get(i+5));
        }
    }

    @Test
    public void testMedalTakesPrecedence() throws NoWindException {
        final int firstMedalCompetitorIndex = 3;
        List<Competitor> competitors = createCompetitors(20);
        List<Competitor> medal = competitors.subList(firstMedalCompetitorIndex, firstMedalCompetitorIndex+10);
        Regatta regatta = createRegatta(/* qualifying */ 0, new String[] { "Default" }, /* final */ 1, new String[] { "Default" },
                /* medal */ true, "testMedalTakesPrecedence",
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
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        // medalists rank top
        for (int i=0; i<10; i++) {
            assertSame(medal.get(i), rankedCompetitors.get(i));
        }
        // others rank according to their non-medal ranking in the final round
        for (int i=10; i<competitors.size(); i++) {
            if (i<10+firstMedalCompetitorIndex) {
                assertSame(competitors.get(i-10), rankedCompetitors.get(i));
            } else {
                assertSame(competitors.get(i), rankedCompetitors.get(i));
            }
        }
    }

    @Test
    public void testLastRaceTakesPrecedenceWithHighPointLastBreaksTieScheme() throws NoWindException {
        List<Competitor> competitors = createCompetitors(20);
        Regatta regatta = createRegatta(/* qualifying */ 0, new String[] { "Default" }, /* final */ 2, new String[] { "Default" },
                /* medal */ false, "testLastRaceTakesPrecedenceWithHighPointLastBreaksTieScheme",
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
            assertEquals(leaderboard.getTotalPoints(firstCompetitor, later), leaderboard.getTotalPoints(competitor, later));
        }
        // assert that the ordering of competitors equals that of the last race
        assertEquals(reversedCompetitors, leaderboard.getCompetitorsFromBestToWorst(later));
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
                /* medal */ false, "testTotalTimeNotCountedForRacesStartedLaterThanTimePointReqeusted",
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
        long totalTimeSailedC0_InRace1 = leaderboard.getTotalTimeSailedInMilliseconds(c[0], earlier.plus(1000));
        assertEquals(1000l, totalTimeSailedC0_InRace1);
        long totalTimeSailedC0_InRace2 = leaderboard.getTotalTimeSailedInMilliseconds(c[0], now.plus(1000));
        assertEquals(now.asMillis()-earlier.asMillis() + 1000, totalTimeSailedC0_InRace2);
        long totalTimeSailedC0_InRace3 = leaderboard.getTotalTimeSailedInMilliseconds(c[0], later.plus(1000));
        assertEquals(later.asMillis()-earlier.asMillis() + 1000, totalTimeSailedC0_InRace3);
        long totalTimeSailedC0_AtEndOfRace3 = leaderboard.getTotalTimeSailedInMilliseconds(c[0], finish);
        assertEquals(finish.asMillis()-earlier.asMillis(), totalTimeSailedC0_AtEndOfRace3);
        long totalTimeSailedC0_AfterRace3 = leaderboard.getTotalTimeSailedInMilliseconds(c[0], finish.plus(1000));
        assertEquals(finish.asMillis()-earlier.asMillis(), totalTimeSailedC0_AfterRace3);
    }

    @Test
    public void testTieBreakWithTwoVersusOneWins() throws NoWindException {
        Competitor[] c = createCompetitors(3).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f2 = new Competitor[] { c[0], c[1], c[2] };
        Competitor[] f3 = new Competitor[] { c[1], c[2], c[0] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */3, new String[] { "Default" },
                /* medal */ false, "testTieBreakWithTwoVersusOneWins",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", f1, f2, f3);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getTotalPoints(c[0], later), leaderboard.getTotalPoints(c[1], later), 0.000000001);
        assertEquals(Arrays.asList(new Competitor[] { c[0], c[1], c[2] }), rankedCompetitors);
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
                /* medal */ false, "testTieBreakWithTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", f1, f2, f3, f4, f5, f6);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getTotalPoints(c[0], later), leaderboard.getTotalPoints(c[1], later), 0.000000001);
        assertTrue(rankedCompetitors.indexOf(c[0]) == rankedCompetitors.indexOf(c[1])-1);
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
                /* medal */ false, "testTieBreakWithTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[] { 4 });
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", f1, f2, f3, f4, f5, f6);
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

    private void checkScoresAfterSomeRaces(Leaderboard leaderboard, List<RaceColumn> raceColumnsToConsider,
            double[][] scoresAfterNRaces, TimePoint timePoint, Competitor[] competitors) throws NoWindException {
        for (int competitorIndex=0; competitorIndex<scoresAfterNRaces.length; competitorIndex++) {
            for (int raceColumnIndex=0; raceColumnIndex<raceColumnsToConsider.size(); raceColumnIndex++) {
                assertEquals(scoresAfterNRaces[competitorIndex][raceColumnIndex],
                        leaderboard.getTotalPoints(competitors[competitorIndex], raceColumnsToConsider.get(raceColumnIndex),
                                raceColumnsToConsider, timePoint), 0.00000001);
            }
        }
    }

    @Test
    public void testTieBreakByMedalRaceScoreOnlyIfEqualTotalScore() throws NoWindException {
        Competitor[] c = createCompetitors(2).toArray(new Competitor[0]);
        Competitor[] f1 = new Competitor[] { c[1], c[0] };
        Competitor[] f2 = new Competitor[] { c[1], c[0] };
        Competitor[] m1 = new Competitor[] { c[0], c[1] };
        Regatta regatta = createRegatta(/* qualifying */0, new String[] { "Default" }, /* final */2, new String[] { "Default" },
                /* medal */ true, "testTieBreakWithTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true), DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", f1, f2);
        createAndAttachTrackedRaces(series.get(2), "Medal", m1);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        // assert that both have equal score
        assertEquals(leaderboard.getTotalPoints(c[0], later), leaderboard.getTotalPoints(c[1], later), 0.000000001);
        // assert that c[0] ranks better than c[1] (reason: c[0] ranked better in medal race)
        assertEquals(rankedCompetitors.indexOf(c[0]), rankedCompetitors.indexOf(c[1])-1);
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
                /* medal */ false, "testTieBreakWithEqualWinsAndTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", f1, f2, f3, f4, f5, f6);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getTotalPoints(c[0], later), leaderboard.getTotalPoints(c[1], later), 0.000000001);
        assertEquals(rankedCompetitors.indexOf(c[0]), rankedCompetitors.indexOf(c[1])-1);
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
                /* medal */ false, "testTieBreakWithEqualWinsAndTwoVersusOneSeconds",
                DomainFactory.INSTANCE.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.HIGH_POINT));
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint later = createAndAttachTrackedRaces(series.get(1), "Default", f1, f2, f3, f4, f5, f6);
        List<Competitor> rankedCompetitors = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(leaderboard.getTotalPoints(c[0], later), leaderboard.getTotalPoints(c[1], later), 0.000000001);
        assertEquals(rankedCompetitors.indexOf(c[0]), rankedCompetitors.indexOf(c[1])-1);
    }

    @Test
    public void testCompetitorsRankedEleventhOrLowerGetOnePointScore() throws NoWindException {
        Competitor[] competitors = createCompetitors(16).toArray(new Competitor[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        FlexibleLeaderboard leaderboardHighPoint10LastBreaksTie = new FlexibleLeaderboardImpl("Test ESS Highpoint 10LastBreaksTie", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointFirstGets10LastBreaksTie(), null);
        leaderboardHighPoint10LastBreaksTie.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors)), "R1", /* medalRace */ false,
                leaderboardHighPoint10LastBreaksTie.getFleet(null));
        assertTrue(leaderboardHighPoint10LastBreaksTie.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboardHighPoint10LastBreaksTie.getTotalPoints(competitors[0], later), leaderboardHighPoint10LastBreaksTie.getTotalPoints(competitors[3], later)) < 0); // c0 better than c3
        assertEquals(16, leaderboardHighPoint10LastBreaksTie.getCompetitorsFromBestToWorst(later).size());
        assertEquals(new Double(10), leaderboardHighPoint10LastBreaksTie.getTotalPoints(competitors[0], later));
        assertEquals(new Double(1), leaderboardHighPoint10LastBreaksTie.getTotalPoints(competitors[15], later));
        // Normal HighPoint leaderboard has no max so that winner gets as many points as there are competitors
        FlexibleLeaderboard leaderboardHighPoint = new FlexibleLeaderboardImpl("Test ESS Highpoint", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPoint(), null);
        leaderboardHighPoint.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors)), "R1", /* medalRace */ false,
                leaderboardHighPoint.getFleet(null));
        assertEquals(16, leaderboardHighPoint.getCompetitorsFromBestToWorst(later).size());
        assertEquals(new Double(16), leaderboardHighPoint.getTotalPoints(competitors[0], later));
        FlexibleLeaderboard leaderboardHighPointESSOverall = new FlexibleLeaderboardImpl("Test ESS Highpoint Overall", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPointExtremeSailingSeriesOverall(), null);
        leaderboardHighPointESSOverall.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitors)), "R1", /* medalRace */ false,
                leaderboardHighPointESSOverall.getFleet(null));
        assertEquals(16, leaderboardHighPointESSOverall.getCompetitorsFromBestToWorst(later).size());
        assertEquals(new Double(10), leaderboardHighPointESSOverall.getTotalPoints(competitors[0], later));
        assertEquals(new Double(9), leaderboardHighPointESSOverall.getTotalPoints(competitors[1], later));
        assertEquals(new Double(8), leaderboardHighPointESSOverall.getTotalPoints(competitors[2], later));
        assertEquals(new Double(7), leaderboardHighPointESSOverall.getTotalPoints(competitors[3], later));
        assertEquals(new Double(6), leaderboardHighPointESSOverall.getTotalPoints(competitors[4], later));
        assertEquals(new Double(5), leaderboardHighPointESSOverall.getTotalPoints(competitors[5], later));
        assertEquals(new Double(4), leaderboardHighPointESSOverall.getTotalPoints(competitors[6], later));
        assertEquals(new Double(3), leaderboardHighPointESSOverall.getTotalPoints(competitors[7], later));
        assertEquals(new Double(2), leaderboardHighPointESSOverall.getTotalPoints(competitors[8], later));
        assertEquals(new Double(1), leaderboardHighPointESSOverall.getTotalPoints(competitors[9], later));
        assertEquals(new Double(1), leaderboardHighPointESSOverall.getTotalPoints(competitors[14], later));
        assertEquals(new Double(1), leaderboardHighPointESSOverall.getTotalPoints(competitors[15], later));
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
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f1)), "R1", /* medalRace */ false,
                leaderboard1.getFleet(null));
        leaderboard1.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f2)), "R2", /* medalRace */ false,
                leaderboard1.getFleet(null));
        assertTrue(leaderboard1.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboard1.getTotalPoints(c[0], later), leaderboard1.getTotalPoints(c[3], later)) < 0); // c0 better than c3
        FlexibleLeaderboard leaderboard2 = new FlexibleLeaderboardImpl("Leaderboard 3", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPoint(), null);
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f3)), "R1", /* medalRace */ false,
                leaderboard1.getFleet(null));
        leaderboard2.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f4)), "R2", /* medalRace */ false,
                leaderboard1.getFleet(null));
        assertTrue(leaderboard2.getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                leaderboard2.getTotalPoints(c[3], later), leaderboard2.getTotalPoints(c[0], later)) < 0); // c3 better than c0
        FlexibleLeaderboard leaderboard3 = new FlexibleLeaderboardImpl("Leaderboard 3", new ThresholdBasedResultDiscardingRuleImpl(/* discarding thresholds */ new int[0]),
                new HighPoint(), null);
        leaderboard3.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f5)), "R1", /* medalRace */ false,
                leaderboard1.getFleet(null));
        leaderboard3.addRace(new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(f6)), "R2", /* medalRace */ false,
                leaderboard1.getFleet(null));
        assertTrue(leaderboard3.getCompetitorsFromBestToWorst(later).indexOf(c[3]) <
                leaderboard3.getCompetitorsFromBestToWorst(later).indexOf(c[0])); // c3 better than c0; won last race
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("Leaderboard Group", "Leaderboard Group", false, Arrays.asList(leaderboard1,
                leaderboard2, leaderboard3));
        leaderboardGroup.setOverallLeaderboard(new LeaderboardGroupMetaLeaderboard(leaderboardGroup, new HighPointExtremeSailingSeriesOverall(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[0])));
        leaderboardGroup.getOverallLeaderboard().setSuppressed(c[1], true);
        leaderboardGroup.getOverallLeaderboard().setSuppressed(c[2], true);
        List<Competitor> rankedCompetitors = leaderboardGroup.getOverallLeaderboard().getCompetitorsFromBestToWorst(later);
        assertFalse(rankedCompetitors.contains(c[1]));
        assertFalse(rankedCompetitors.contains(c[2]));
        assertEquals(2, rankedCompetitors.size());
        assertEquals(28 /* one win, two second */, leaderboardGroup.getOverallLeaderboard().getTotalPoints(c[0], later), 0.000000001);
        assertEquals(29 /* two wins, one second */, leaderboardGroup.getOverallLeaderboard().getTotalPoints(c[3], later), 0.000000001);
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
        }, "R1", /* medalRace */ false,
                leaderboard1.getFleet(null));
        RaceColumn r2 = leaderboard1.addRaceColumn("R2", /* medalRace */ false, new Fleet[] { new FleetImpl("Default Fleet") });
        leaderboard1.getScoreCorrection().correctScore(c[0], r2, 123.);
        assertEquals(2. + 123., leaderboard1.getTotalPoints(c[0], afterEndOfR1), 0.00000001); // correction expected to apply after end of last tracked race before the corrected column
        assertEquals(2. + 0., leaderboard1.getTotalPoints(c[0], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getTotalPoints(c[0], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1
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
        final Waypoint start = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("Start Pin End"), new MarkImpl("Start Committee Boat"), "Start"));
        final Waypoint finish = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("Finish Pin End"), new MarkImpl("Finish Committee Boat"), "Finish"));
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
        RaceColumn r1 = leaderboard1.addRace(trackedRace, "R1", /* medalRace */ false, leaderboard1.getFleet(null));
        RaceColumn r2 = leaderboard1.addRaceColumn("R2", /* medalRace */ false, new Fleet[] { new FleetImpl("Default Fleet") });
        leaderboard1.getScoreCorrection().correctScore(c[0], r2, 123.);
        assertEquals(2. + 123., leaderboard1.getTotalPoints(c[0], afterEndOfR1), 0.00000001); // correction expected to apply after end of last tracked race before the corrected column
        assertEquals(2. + 0., leaderboard1.getTotalPoints(c[0], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getTotalPoints(c[0], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[1], r2, MaxPointsReason.DNS);
        assertEquals(3. + 5., leaderboard1.getTotalPoints(c[1], afterEndOfR1), 0.00000001); // DNS is applied at race start (not known), so at least after the end of R1
        assertEquals(3. + 0., leaderboard1.getTotalPoints(c[1], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getTotalPoints(c[1], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[2], r2, MaxPointsReason.DNF);
        assertEquals(1. + 5., leaderboard1.getTotalPoints(c[2], afterEndOfR1), 0.00000001); // DNF is applied after R2 finish (not known), so at least after the end of R1
        assertEquals(1. + 0., leaderboard1.getTotalPoints(c[2], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(0., leaderboard1.getTotalPoints(c[2], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[3], r1, MaxPointsReason.DNF);
        assertEquals(0., leaderboard1.getTotalPoints(c[3], beforeStartOfR1), 0.00000001); // DNF does not apply before the start
        assertEquals(4., leaderboard1.getTotalPoints(c[3], withinR1), 0.00000001); // correction expected NOT to apply before end of race
        assertEquals(5., leaderboard1.getTotalPoints(c[3], afterEndOfR1), 0.00000001); // after race is finished, DNF applies

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[3], r1, MaxPointsReason.DNS);
        assertEquals(0., leaderboard1.getTotalPoints(c[3], beforeStartOfR1), 0.00000001); // DNS does not apply before the start
        assertEquals(5., leaderboard1.getTotalPoints(c[3], withinR1), 0.00000001); // correction expected to apply after the start
        assertEquals(5., leaderboard1.getTotalPoints(c[3], afterEndOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1

        leaderboard1.getScoreCorrection().setMaxPointsReason(c[3], r1, null);
        leaderboard1.getScoreCorrection().correctScore(c[3], r1, 123.);
        assertEquals(4., leaderboard1.getTotalPoints(c[3], withinR1), 0.00000001); // correction expected NOT to apply before end of last tracked race before the corrected column
        assertEquals(123., leaderboard1.getTotalPoints(c[3], afterEndOfR1), 0.00000001); // correction is applied after R1 finish
        assertEquals(0., leaderboard1.getTotalPoints(c[3], beforeStartOfR1), 0.00000001); // not even the R1 scores apply before the start time of R1
    }

    private TimePoint createAndAttachTrackedRaces(Series theSeries, String fleetName, Competitor[]... competitorLists) {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        Iterator<? extends RaceColumn> columnIter = theSeries.getRaceColumns().iterator();
        for (Competitor[] competitorList : competitorLists) {
            RaceColumn raceColumn = columnIter.next();
            TrackedRace trackedRace = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitorList));
            raceColumn.setTrackedRace(raceColumn.getFleetByName(fleetName), trackedRace);
        }
        return later;
    }

    private void createAndAttachTrackedRacesWithStartTimeAndLastMarkPassingTimes(
            Series theSeries, String fleetName, Competitor[][] competitorLists, TimePoint[] startTimes,
            Map<Competitor, TimePoint>[] lastMarkPassingTimesForCompetitors) {
        Iterator<? extends RaceColumn> columnIter = theSeries.getRaceColumns().iterator();
        int i=0;
        for (Competitor[] competitorList : competitorLists) {
            RaceColumn raceColumn = columnIter.next();
            final Map<Competitor, TimePoint> lastMarkPassingTimes = lastMarkPassingTimesForCompetitors[i];
            final Waypoint start = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("Left StartBuoy"), new MarkImpl("Right StartBuoy"), "Start"));
            final Waypoint finish = new WaypointImpl(new MarkImpl("FinishBuoy"));
            TrackedRace trackedRace = new MockedTrackedRaceWithStartTimeAndRanks(startTimes[i], Arrays.asList(competitorList)) {
                private static final long serialVersionUID = 1L;
                @Override
                public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
                    ArrayListNavigableSet<MarkPassing> result = new ArrayListNavigableSet<>(new TimedComparator());
                    result.add(new MarkPassingImpl(lastMarkPassingTimes.get(competitor), finish, competitor));
                    return result;
                }
            };
            trackedRace.getRace().getCourse().addWaypoint(0, start);
            trackedRace.getRace().getCourse().addWaypoint(1, finish);
            raceColumn.setTrackedRace(raceColumn.getFleetByName(fleetName), trackedRace);
            i++;
        }
    }

    private List<Competitor> createCompetitors(int numberOfCompetitorsToCreate) {
        List<Competitor> result = new ArrayList<Competitor>();
        for (int i=1; i<=numberOfCompetitorsToCreate; i++) {
            result.add(createCompetitor("C"+i));
        }
        return result;
    }

    private Regatta createRegatta(final int numberOfQualifyingRaces, String[] qualifyingFleetNames, final int numberOfFinalRaces,
            String[] finalFleetNames, boolean medalRaceAndSeries, final String regattaBaseName, BoatClass boatClass, ScoringScheme scoringScheme) {
        series = new ArrayList<Series>();

        // -------- qualifying series ------------
        if (qualifyingFleetNames != null && qualifyingFleetNames.length > 0) {
            List<Fleet> qualifyingFleets = new ArrayList<Fleet>();
            for (String qualifyingFleetName : qualifyingFleetNames) {
                qualifyingFleets.add(new FleetImpl(qualifyingFleetName));
            }
            List<String> qualifyingRaceColumnNames = new ArrayList<String>();
            for (int i = 1; i <= numberOfQualifyingRaces; i++) {
                qualifyingRaceColumnNames.add("Q" + i);
            }
            Series qualifyingSeries = new SeriesImpl("Qualifying", /* isMedal */false, qualifyingFleets,
                    qualifyingRaceColumnNames, /* trackedRegattaRegistry */null);
            series.add(qualifyingSeries);
        }

        // -------- final series ------------
        if (finalFleetNames != null && finalFleetNames.length > 0) {
            List<Fleet> finalFleets = new ArrayList<Fleet>();
            int fleetOrdering = 1;
            for (String finalFleetName : finalFleetNames) {
                finalFleets.add(new FleetImpl(finalFleetName, fleetOrdering++));
            }
            List<String> finalRaceColumnNames = new ArrayList<String>();
            for (int i = 1; i <= numberOfFinalRaces; i++) {
                finalRaceColumnNames.add("F" + i);
            }
            Series finalSeries = new SeriesImpl("Final", /* isMedal */false, finalFleets, finalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(finalSeries);
        }

        if (medalRaceAndSeries) {
            // ------------ medal --------------
            List<Fleet> medalFleets = new ArrayList<Fleet>();
            medalFleets.add(new FleetImpl("Medal"));
            List<String> medalRaceColumnNames = new ArrayList<String>();
            medalRaceColumnNames.add("M");
            Series medalSeries = new SeriesImpl("Medal", /* isMedal */true, medalFleets, medalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(medalSeries);
        }

        Regatta regatta = new RegattaImpl(regattaBaseName, boatClass, series, /* persistent */ false, scoringScheme, "123", null);
        return regatta;
    }
}
