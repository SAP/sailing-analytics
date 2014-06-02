package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRank;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRankAndManyCompetitors;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sse.common.Util;

public class LeaderboardOfflineTest extends AbstractLeaderboardTest {
    private Set<TrackedRace> testRaces;
    private Map<TrackedRace, RaceColumn> raceColumnsInLeaderboard;
    private Competitor competitor;

    @Before
    public void setUp() {
        competitor = createCompetitor("Wolfgang Hunger");
    }

    public void setupRaces(int numberOfStartedRaces, int numberOfNotStartedRaces) {
        testRaces = new HashSet<TrackedRace>();
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        for (int i=0; i<numberOfStartedRaces; i++) {
            TrackedRace r = new MockedTrackedRaceWithFixedRank(competitor, i+1, /* started */ true);
            testRaces.add(r); // hash set should take care of more or less randomly permuting the races
        }
        for (int i=0; i<numberOfNotStartedRaces; i++) {
            TrackedRace r = new MockedTrackedRaceWithFixedRank(competitor, -1, /* started */ false);
            testRaces.add(r); // hash set should take care of more or less randomly permuting the races
        }
    }

    @Test
    public void testSetup() {
        setupRaces(3, 7);
        assertEquals(10, testRaces.size());
    }

    @Test
    public void simpleLeaderboardTest() throws NoWindException {
        for (int numberOfUntrackedRaces : new int[] { 0, 1, 2, 3 }) {
            for (boolean addOneMedalRace : new boolean[] { false, true }) {
                for (Integer carry : new Integer[] { null, 0, 1, 2, 3 }) {
                    testLeaderboard(0, 9, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(2, 9, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(3, 9, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(4, 9, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(6, 9, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(7, 9, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(8, 9, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(0, 0, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(2, 0, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(3, 0, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(4, 0, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(6, 0, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(7, 0, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                    testLeaderboard(8, 0, 3, 6, carry, addOneMedalRace, numberOfUntrackedRaces);
                }
            }
        }
    }

    @Test
    public void ensureMedalRaceParamIsIgnoredIfRaceColumnAlreadyExists() {
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 5, 8 }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        final String columnName = "abc";
        setupRaces(1, 0);

        leaderboard.addRaceColumn(columnName, /* medalRace */ true);
        leaderboard.addRace(testRaces.iterator().next(), columnName, /* medalRace */ false, defaultFleet);
        assertTrue(leaderboard.getRaceColumnByName(columnName).isMedalRace());
    }
    
    @Test
    public void testRepeatedLeaderboardDTOCacheInvalidation() throws NoWindException, InterruptedException, ExecutionException {
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 5, 8 }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        final String columnName = "abc";
        setupRaces(1, 0);
        leaderboard.addRaceColumn(columnName, /* medalRace */ true);
        leaderboard.addRace(testRaces.iterator().next(), columnName, /* medalRace */ false, defaultFleet);
        final Set<String> emptySet = Collections.emptySet();
        final MillisecondsTimePoint now = MillisecondsTimePoint.now();
        final TrackedRegattaRegistry trackedRegattaRegistry = new TrackedRegattaRegistry() {
            @Override
            public DynamicTrackedRegatta getOrCreateTrackedRegatta(Regatta regatta) {
                return null;
            }
            @Override
            public DynamicTrackedRegatta getTrackedRegatta(Regatta regatta) {
                return null;
            }
            @Override
            public void removeTrackedRegatta(Regatta regatta) {}

            @Override
            public Regatta getRememberedRegattaForRace(Serializable raceID) {
                return null;
            }
            @Override
            public boolean isRaceBeingTracked(Regatta regattaContext, RaceDefinition r) {
                return true;
            }
            @Override
            public void stopTracking(Regatta regatta, RaceDefinition race) throws MalformedURLException, IOException,
                    InterruptedException {
            }
        };
        LeaderboardDTO leaderboardDTO = leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE);
        assertNotNull(leaderboardDTO);
        assertSame(leaderboardDTO, leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE)); // assert it's cached
        leaderboard.getRaceColumnByName(columnName).releaseTrackedRace(defaultFleet); // this should clear the cache
        LeaderboardDTO leaderboardDTO2 = leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE);
        assertNotSame(leaderboardDTO, leaderboardDTO2);
        assertSame(leaderboardDTO2, leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE)); // and cached again
        leaderboard.getRaceColumnByName(columnName).setTrackedRace(defaultFleet, testRaces.iterator().next()); // clear cache again; requires listener(s) to still be attached
        LeaderboardDTO leaderboardDTO3 = leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE);
        assertNotSame(leaderboardDTO2, leaderboardDTO3);
        assertSame(leaderboardDTO3, leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE)); // and cached again
    }

    @Test
    public void testMaxPointsDiscard() throws NoWindException {
        testRaces = new HashSet<TrackedRace>();
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        Competitor c2 = createCompetitor("Marcus Baur");
        Competitor c3 = createCompetitor("Robert Stanjek");
        for (int i=0; i<3; i++) {
            MockedTrackedRaceWithFixedRankAndManyCompetitors r = new MockedTrackedRaceWithFixedRankAndManyCompetitors(competitor, i+1, /* started */ true);
            r.addCompetitor(c2);
            r.addCompetitor(c3); // this makes maxPoints==4
            testRaces.add(r); // hash set should take care of more or less randomly permuting the races
        }
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 1 }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        int i=0;
        int bestScore = Integer.MAX_VALUE;
        RaceColumn bestScoringRaceColumn = null;
        for (TrackedRace race : testRaces) {
            i++;
            RaceColumn raceColumn = leaderboard.addRace(race, "Test Race " + i, /* medalRace */ false, defaultFleet);
            raceColumnsInLeaderboard.put(race, raceColumn);
            if (race.getRank(competitor) < bestScore) {
                bestScore = race.getRank(competitor);
                bestScoringRaceColumn = raceColumn;
            }
        }
        leaderboard.getScoreCorrection().setMaxPointsReason(competitor, bestScoringRaceColumn, MaxPointsReason.DSQ);
        // assert that best scoring but now disqualified race gets max net points because of disqualification:
        assertEquals(Util.size(bestScoringRaceColumn.getTrackedRace(defaultFleet).getRace().getCompetitors())+1,
                leaderboard.getEntry(competitor, bestScoringRaceColumn, MillisecondsTimePoint.now()).getNetPoints(), 0.000000001);
        // now assert that it gets discarded because due to disqualification it scores worse than all others:
        assertEquals(0, leaderboard.getEntry(competitor, bestScoringRaceColumn, MillisecondsTimePoint.now()).getTotalPoints(), 0.000000001);
    }

    @Test
    public void testCarriedPointsCountInSorting() throws NoWindException {
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        Competitor c2 = createCompetitor("Marcus Baur");
        Competitor c3 = createCompetitor("Robert Stanjek");
        TimePoint now = MillisecondsTimePoint.now();
        MockedTrackedRaceWithFixedRankAndManyCompetitors testRace = new MockedTrackedRaceWithFixedRankAndManyCompetitors(
                competitor, /* rank */ 1, /* started */true);
        testRace.addCompetitor(c2);
        testRace.addCompetitor(c3); // this makes maxPoints==4
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 2 }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        leaderboard.addRace(testRace, "R1", /* medalRace */ false, defaultFleet);
        assertEquals(1., leaderboard.getTotalPoints(competitor, now), 0.00000001);
        assertEquals(1., leaderboard.getTotalPoints(c2, now), 0.00000001);
        assertEquals(1., leaderboard.getTotalPoints(c3, now), 0.00000001);
        leaderboard.setCarriedPoints(competitor, 100);
        leaderboard.setCarriedPoints(c2, 50);
        leaderboard.setCarriedPoints(c3, 25);
        assertEquals(101., leaderboard.getTotalPoints(competitor, now), 0.00000001);
        assertEquals(51., leaderboard.getTotalPoints(c2, now), 0.00000001);
        assertEquals(26., leaderboard.getTotalPoints(c3, now), 0.00000001);
        List<Competitor> sortedCompetitors = leaderboard.getCompetitorsFromBestToWorst(now);
        assertSame(c3, sortedCompetitors.get(0));
        assertSame(c2, sortedCompetitors.get(1));
        assertSame(competitor, sortedCompetitors.get(2));
    }

    @Test
    public void testDNDNotDiscardedInUntrackedRace() throws NoWindException {
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        Competitor c2 = createCompetitor("Marcus Baur");
        Competitor c3 = createCompetitor("Robert Stanjek");
        MockedTrackedRaceWithFixedRankAndManyCompetitors testRace = new MockedTrackedRaceWithFixedRankAndManyCompetitors(
                competitor, /* rank */ 1, /* started */true);
        testRace.addCompetitor(c2);
        testRace.addCompetitor(c3); // this makes maxPoints==4
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 2 }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        RaceColumn r1 = leaderboard.addRace(testRace, "R1", /* medalRace */ false, defaultFleet);
        raceColumnsInLeaderboard.put(testRace, r1);
        RaceColumn r2 = leaderboard.addRaceColumn("R2", /* medalRace */ false, defaultFleet);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitor, r2, MaxPointsReason.DND); // non-discardable disqualification
        // assert that max points were given before discarding...
        assertEquals(4.0, leaderboard.getEntry(competitor, r2, MillisecondsTimePoint.now()).getNetPoints(), 0.000000001);
        // ...and after...
        assertEquals(4.0, leaderboard.getEntry(competitor, r2, MillisecondsTimePoint.now()).getTotalPoints(), 0.000000001);
        // ...because it's not discarded
        assertFalse(leaderboard.getEntry(competitor, r2, MillisecondsTimePoint.now()).isDiscarded());
    }

    @Test
    public void testNoDNDDiscard() throws NoWindException {
        testRaces = new HashSet<TrackedRace>();
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        Competitor c2 = createCompetitor("Marcus Baur");
        Competitor c3 = createCompetitor("Robert Stanjek");
        for (int i=0; i<3; i++) {
            MockedTrackedRaceWithFixedRankAndManyCompetitors r = new MockedTrackedRaceWithFixedRankAndManyCompetitors(competitor, i+1, /* started */ true);
            r.addCompetitor(c2);
            r.addCompetitor(c3); // this makes maxPoints==4
            testRaces.add(r); // hash set should take care of more or less randomly permuting the races
        }
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 1 }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        int i=0;
        int bestScore = Integer.MAX_VALUE;
        RaceColumn bestScoringRaceColumn = null;
        for (TrackedRace race : testRaces) {
            i++;
            RaceColumn raceColumn = leaderboard.addRace(race, "Test Race " + i, /* medalRace */ false, defaultFleet);
            raceColumnsInLeaderboard.put(race, raceColumn);
            if (race.getRank(competitor) < bestScore) {
                bestScore = race.getRank(competitor);
                bestScoringRaceColumn = raceColumn;
            }
        }
        leaderboard.getScoreCorrection().setMaxPointsReason(competitor, bestScoringRaceColumn, MaxPointsReason.DND);
        // assert that best scoring but now disqualified race gets max net points because of disqualification:
        assertEquals(Util.size(bestScoringRaceColumn.getTrackedRace(defaultFleet).getRace().getCompetitors())+1,
                leaderboard.getEntry(competitor, bestScoringRaceColumn, MillisecondsTimePoint.now()).getNetPoints(), 0.000000001);
        // now assert that it does not get discarded because it's a DND
        assertEquals(Util.size(bestScoringRaceColumn.getTrackedRace(defaultFleet).getRace().getCompetitors())+1,
                leaderboard.getEntry(competitor, bestScoringRaceColumn, MillisecondsTimePoint.now()).getTotalPoints(), 0.000000001);
    }

    protected void testLeaderboard(int numberOfStartedRaces, int numberOfNotStartedRaces, int firstDiscardingThreshold,
            int secondDiscardingThreshold, Integer carry, boolean addOneMedalRace, int numberOfUntrackedRaces) throws NoWindException {
        setupRaces(numberOfStartedRaces, numberOfNotStartedRaces);
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { firstDiscardingThreshold, secondDiscardingThreshold }), new LowPoint(), null);
        Fleet defaultFleet = leaderboard.getFleet(null);
        int i=0;
        for (TrackedRace race : testRaces) {
            i++;
            raceColumnsInLeaderboard.put(race, leaderboard.addRace(race, "Test Race "+i,
                    /* medalRace */ numberOfUntrackedRaces == 0 && addOneMedalRace && i == testRaces.size(), 
                    defaultFleet));
        }
        // add a few race columns not yet connected to a tracked race
        for (int j=0; j<numberOfUntrackedRaces; j++) {
            i++;
            leaderboard.addRaceColumn("Test Race "+i, /* medalRace */ addOneMedalRace && j == numberOfUntrackedRaces-1);
        }
        if (carry != null) {
            leaderboard.setCarriedPoints(competitor, carry);
        }
        List<Integer> ranksOfNonMedalStartedRaces = new ArrayList<Integer>();
        TimePoint now = MillisecondsTimePoint.now();
        int numberOfRacesToCompareToDiscardThresholds = 0;
        for (RaceColumn column : raceColumnsInLeaderboard.values()) {
            if (column.getTrackedRace(defaultFleet) != null && column.getTrackedRace(defaultFleet).hasStarted(now)) {
                numberOfRacesToCompareToDiscardThresholds++;
                if (!column.isMedalRace()) {
                    ranksOfNonMedalStartedRaces.add(column.getTrackedRace(defaultFleet).getRank(competitor, now));
                }
            }
        }
        Collections.sort(ranksOfNonMedalStartedRaces);
        int carryInt = (carry == null ? 0 : carry);
        int totalPoints = carryInt;
        int medalRacePoints = getMedalRacePoints(competitor, now, defaultFleet);
        for (TrackedRace race : testRaces) {
            RaceColumn raceColumn = raceColumnsInLeaderboard.get(race);
            com.sap.sse.common.UtilNew.Pair<Competitor, RaceColumn> key = new com.sap.sse.common.UtilNew.Pair<Competitor, RaceColumn>(competitor, raceColumn);
            if (race.hasStarted(now)) {
                int rank = race.getRank(competitor, now);
                assertEquals(rank, leaderboard.getTrackedRank(competitor, raceColumn, now));
                assertEquals(rank, leaderboard.getContent(now).get(key).getTrackedRank());
                assertEquals(rank, leaderboard.getEntry(competitor, raceColumn, now).getTrackedRank());
                assertEquals(rank, leaderboard.getNetPoints(competitor, raceColumn, now), 0.000000001);
                assertEquals(rank, leaderboard.getContent(now).get(key).getNetPoints(), 0.000000001);
                assertEquals(rank, leaderboard.getEntry(competitor, raceColumn, now).getNetPoints(), 0.000000001);
                // One race is discarded because four races were started, and for [3-6) one race can be discarded.
                // The discarded race is the worst of those started, so the one with rank 4.
                int expectedNumberOfDiscardedRaces =
                        numberOfRacesToCompareToDiscardThresholds < firstDiscardingThreshold ? 0 :
                            numberOfRacesToCompareToDiscardThresholds < secondDiscardingThreshold ? 1 : 2;
                boolean discarded = ranksOfNonMedalStartedRaces.indexOf(rank) >= ranksOfNonMedalStartedRaces.size()-expectedNumberOfDiscardedRaces;
                int expected = discarded ? 0 : rank==medalRacePoints?2*rank:rank;
                assertEquals(expected, leaderboard.getTotalPoints(competitor, raceColumn, now), 0.000000001);
                assertEquals(expected, leaderboard.getContent(now).get(key).getTotalPoints(), 0.000000001);
                assertEquals(expected, leaderboard.getEntry(competitor, raceColumn, now).getTotalPoints(), 0.000000001);
                totalPoints += leaderboard.getContent(now).get(key).getTotalPoints();
            } else {
                assertEquals(0, leaderboard.getTrackedRank(competitor, raceColumn, now));
                assertNull(leaderboard.getNetPoints(competitor, raceColumn, now));
                assertEquals(0, leaderboard.getContent(now).get(key).getTrackedRank());
                assertNull(leaderboard.getContent(now).get(key).getNetPoints());
                assertEquals(0, leaderboard.getEntry(competitor, raceColumn, now).getTrackedRank());
                assertNull(leaderboard.getEntry(competitor, raceColumn, now).getNetPoints());
                // no increment on totalPoints
            }
        }
        assertEquals(totalPoints, leaderboard.getTotalPoints(competitor, now), 0.000000001);
    }

    private int getMedalRacePoints(Competitor competitor, TimePoint at, Fleet fleet) throws NoWindException {
        for (TrackedRace r : testRaces) {
            if (raceColumnsInLeaderboard.get(r) != null && raceColumnsInLeaderboard.get(r).isMedalRace() &&
                    raceColumnsInLeaderboard.get(r).getTrackedRace(fleet).hasStarted(at)) {
                return raceColumnsInLeaderboard.get(r).getTrackedRace(fleet).getRank(competitor, at);
            }
        }
        return 0;
    }
}
