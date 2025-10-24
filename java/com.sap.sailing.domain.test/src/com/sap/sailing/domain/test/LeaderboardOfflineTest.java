package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRank;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRankAndManyCompetitors;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardOfflineTest extends AbstractLeaderboardTest {
    private Set<TrackedRace> testRaces;
    private Map<TrackedRace, RaceColumn> raceColumnsInLeaderboard;
    private CompetitorWithBoat competitorWithBoat;

    @BeforeEach
    public void setUp() {
        competitorWithBoat = createCompetitorWithBoat("Wolfgang Hunger");
    }

    public void setupRaces(int numberOfStartedRaces, int numberOfNotStartedRaces) {
        testRaces = new HashSet<TrackedRace>();
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        for (int i=0; i<numberOfStartedRaces; i++) {
            TrackedRace r = new MockedTrackedRaceWithFixedRank(competitorWithBoat, i+1, /* started */ true);
            testRaces.add(r); // hash set should take care of more or less randomly permuting the races
        }
        for (int i=0; i<numberOfNotStartedRaces; i++) {
            TrackedRace r = new MockedTrackedRaceWithFixedRank(competitorWithBoat, -1, /* started */ false);
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
        final String columnName = "abc";
        setupRaces(1, 0);

        leaderboard.addRaceColumn(columnName, /* medalRace */ true);
        leaderboard.addRace(testRaces.iterator().next(), columnName, /* medalRace */false);
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
        leaderboard.addRace(testRaces.iterator().next(), columnName, /* medalRace */false);
        final Set<String> emptySet = Collections.emptySet();
        final TimePoint now = MillisecondsTimePoint.now();
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
            @Override
            public void removeRace(Regatta regatta, RaceDefinition race)
                    throws MalformedURLException, IOException, InterruptedException {
            }
            @Override
            public void stopTracker(Regatta regatta, RaceTracker tracker)
                    throws MalformedURLException, IOException, InterruptedException {
            }
            @Override
            public Regatta getOrCreateDefaultRegatta(String name, String boatClassName, Serializable id) {
                return null;
            }
            @Override
            public RaceHandle updateRaceCompetitors(Regatta regatta, RaceDefinition race) throws Exception {
                return null;
            }
        };
        LeaderboardDTO leaderboardDTO = leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertNotNull(leaderboardDTO);
        assertSame(leaderboardDTO, leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false)); // assert it's cached
        leaderboard.getRaceColumnByName(columnName).releaseTrackedRace(defaultFleet); // this should clear the cache
        LeaderboardDTO leaderboardDTO2 = leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertNotSame(leaderboardDTO, leaderboardDTO2);
        assertSame(leaderboardDTO2, leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false)); // and cached again
        leaderboard.getRaceColumnByName(columnName).setTrackedRace(defaultFleet, testRaces.iterator().next()); // clear cache again; requires listener(s) to still be attached
        LeaderboardDTO leaderboardDTO3 = leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false);
        assertNotSame(leaderboardDTO2, leaderboardDTO3);
        assertSame(leaderboardDTO3, leaderboard.getLeaderboardDTO(now, emptySet, /* addOverallDetails */ true, trackedRegattaRegistry, DomainFactory.INSTANCE, /* fillTotalPointsUncorrected */ false)); // and cached again
    }

    @Test
    public void testMaxPointsDiscard() throws NoWindException {
        testRaces = new HashSet<TrackedRace>();
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        CompetitorWithBoat c2 = createCompetitorWithBoat("Marcus Baur");
        CompetitorWithBoat c3 = createCompetitorWithBoat("Robert Stanjek");
        for (int i=0; i<3; i++) {
            MockedTrackedRaceWithFixedRankAndManyCompetitors r = new MockedTrackedRaceWithFixedRankAndManyCompetitors(competitorWithBoat, i+1, /* started */ true);
            r.addCompetitorWithBoat(c2);
            r.addCompetitorWithBoat(c3); // this makes maxPoints==4
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
            RaceColumn raceColumn = leaderboard.addRace(race, "Test Race " + i, /* medalRace */false);
            raceColumnsInLeaderboard.put(race, raceColumn);
            if (race.getRank(competitorWithBoat) < bestScore) {
                bestScore = race.getRank(competitorWithBoat);
                bestScoringRaceColumn = raceColumn;
            }
        }
        leaderboard.getScoreCorrection().setMaxPointsReason(competitorWithBoat, bestScoringRaceColumn, MaxPointsReason.DSQ);
        // assert that best scoring but now disqualified race gets max total points because of disqualification:
        assertEquals(Util.size(bestScoringRaceColumn.getTrackedRace(defaultFleet).getRace().getCompetitors())+1,
                leaderboard.getEntry(competitorWithBoat, bestScoringRaceColumn, MillisecondsTimePoint.now()).getTotalPoints(), 0.000000001);
        // now assert that it gets discarded because due to disqualification it scores worse than all others:
        assertEquals(0, leaderboard.getEntry(competitorWithBoat, bestScoringRaceColumn, MillisecondsTimePoint.now()).getNetPoints(), 0.000000001);
    }

    @Test
    public void testCarriedPointsCountInSorting() throws NoWindException {
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        CompetitorWithBoat c2 = createCompetitorWithBoat("Marcus Baur");
        CompetitorWithBoat c3 = createCompetitorWithBoat("Robert Stanjek");
        TimePoint now = MillisecondsTimePoint.now();
        MockedTrackedRaceWithFixedRankAndManyCompetitors testRace = new MockedTrackedRaceWithFixedRankAndManyCompetitors(
                competitorWithBoat, /* rank */ 1, /* started */true);
        testRace.addCompetitorWithBoat(c2); // rank 2
        testRace.addCompetitorWithBoat(c3); // rank 3; this makes maxPoints==4
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 2 }), new LowPoint(), null);
        leaderboard.addRace(testRace, "R1", /* medalRace */false);
        assertEquals(1., leaderboard.getNetPoints(competitorWithBoat, now), 0.00000001);
        assertEquals(2., leaderboard.getNetPoints(c2, now), 0.00000001);
        assertEquals(3., leaderboard.getNetPoints(c3, now), 0.00000001);
        leaderboard.setCarriedPoints(competitorWithBoat, 100);
        leaderboard.setCarriedPoints(c2, 50);
        leaderboard.setCarriedPoints(c3, 25);
        assertEquals(101., leaderboard.getNetPoints(competitorWithBoat, now), 0.00000001);
        assertEquals(52., leaderboard.getNetPoints(c2, now), 0.00000001);
        assertEquals(28., leaderboard.getNetPoints(c3, now), 0.00000001);
        Iterable<Competitor> sortedCompetitors = leaderboard.getCompetitorsFromBestToWorst(now);
        assertSame(c3, Util.get(sortedCompetitors, 0));
        assertSame(c2, Util.get(sortedCompetitors, 1));
        assertSame(competitorWithBoat, Util.get(sortedCompetitors, 2));
    }

    @Test
    public void testDNDNotDiscardedInUntrackedRace() throws NoWindException {
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        CompetitorWithBoat c2 = createCompetitorWithBoat("Marcus Baur");
        CompetitorWithBoat c3 = createCompetitorWithBoat("Robert Stanjek");
        MockedTrackedRaceWithFixedRankAndManyCompetitors testRace = new MockedTrackedRaceWithFixedRankAndManyCompetitors(
                competitorWithBoat, /* rank */ 1, /* started */true);
        testRace.addCompetitorWithBoat(c2);
        testRace.addCompetitorWithBoat(c3); // this makes maxPoints==4
        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(
                new int[] { 2 }), new LowPoint(), null);
        RaceColumn r1 = leaderboard.addRace(testRace, "R1", /* medalRace */false);
        raceColumnsInLeaderboard.put(testRace, r1);
        RaceColumn r2 = leaderboard.addRaceColumn("R2", /* medalRace */false);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitorWithBoat, r2, MaxPointsReason.DND); // non-discardable disqualification
        // assert that max points were given before discarding...
        assertEquals(4.0, leaderboard.getEntry(competitorWithBoat, r2, MillisecondsTimePoint.now()).getTotalPoints(), 0.000000001);
        // ...and after...
        assertEquals(4.0, leaderboard.getEntry(competitorWithBoat, r2, MillisecondsTimePoint.now()).getNetPoints(), 0.000000001);
        // ...because it's not discarded
        assertFalse(leaderboard.getEntry(competitorWithBoat, r2, MillisecondsTimePoint.now()).isDiscarded());
    }

    @Test
    public void testNoDNDDiscard() throws NoWindException {
        testRaces = new HashSet<TrackedRace>();
        raceColumnsInLeaderboard = new HashMap<TrackedRace, RaceColumn>();
        CompetitorWithBoat  c2 = createCompetitorWithBoat("Marcus Baur");
        CompetitorWithBoat  c3 = createCompetitorWithBoat("Robert Stanjek");
        for (int i=0; i<3; i++) {
            MockedTrackedRaceWithFixedRankAndManyCompetitors r = new MockedTrackedRaceWithFixedRankAndManyCompetitors(competitorWithBoat, i+1, /* started */ true);
            r.addCompetitorWithBoat(c2);
            r.addCompetitorWithBoat(c3); // this makes maxPoints==4
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
            RaceColumn raceColumn = leaderboard.addRace(race, "Test Race " + i, /* medalRace */false);
            raceColumnsInLeaderboard.put(race, raceColumn);
            if (race.getRank(competitorWithBoat) < bestScore) {
                bestScore = race.getRank(competitorWithBoat);
                bestScoringRaceColumn = raceColumn;
            }
        }
        leaderboard.getScoreCorrection().setMaxPointsReason(competitorWithBoat, bestScoringRaceColumn, MaxPointsReason.DND);
        // assert that best scoring but now disqualified race gets max total points because of disqualification:
        assertEquals(Util.size(bestScoringRaceColumn.getTrackedRace(defaultFleet).getRace().getCompetitors())+1,
                leaderboard.getEntry(competitorWithBoat, bestScoringRaceColumn, MillisecondsTimePoint.now()).getTotalPoints(), 0.000000001);
        // now assert that it does not get discarded because it's a DND
        assertEquals(Util.size(bestScoringRaceColumn.getTrackedRace(defaultFleet).getRace().getCompetitors())+1,
                leaderboard.getEntry(competitorWithBoat, bestScoringRaceColumn, MillisecondsTimePoint.now()).getNetPoints(), 0.000000001);
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
            /* medalRace */numberOfUntrackedRaces == 0 && addOneMedalRace && i == testRaces.size()));
        }
        // add a few race columns not yet connected to a tracked race
        for (int j=0; j<numberOfUntrackedRaces; j++) {
            i++;
            leaderboard.addRaceColumn("Test Race "+i, /* medalRace */ addOneMedalRace && j == numberOfUntrackedRaces-1);
        }
        if (carry != null) {
            leaderboard.setCarriedPoints(competitorWithBoat, carry);
        }
        List<Integer> ranksOfNonMedalStartedRaces = new ArrayList<Integer>();
        TimePoint now = MillisecondsTimePoint.now();
        int numberOfRacesToCompareToDiscardThresholds = 0;
        for (RaceColumn column : raceColumnsInLeaderboard.values()) {
            if (column.getTrackedRace(defaultFleet) != null && column.getTrackedRace(defaultFleet).hasStarted(now)) {
                numberOfRacesToCompareToDiscardThresholds++;
                if (!column.isMedalRace()) {
                    ranksOfNonMedalStartedRaces.add(column.getTrackedRace(defaultFleet).getRank(competitorWithBoat, now));
                }
            }
        }
        Collections.sort(ranksOfNonMedalStartedRaces);
        int carryInt = (carry == null ? 0 : carry);
        int netPoints = carryInt;
        final ScoringScheme scoringScheme = leaderboard.getScoringScheme();
        int medalRacePoints = getMedalRacePoints(competitorWithBoat, now, defaultFleet);
        for (TrackedRace race : testRaces) {
            RaceColumn raceColumn = raceColumnsInLeaderboard.get(race);
            com.sap.sse.common.Util.Pair<Competitor, RaceColumn> key = new com.sap.sse.common.Util.Pair<Competitor, RaceColumn>(competitorWithBoat, raceColumn);
            if (race.hasStarted(now)) {
                int rank = race.getRank(competitorWithBoat, now);
                assertEquals(rank, leaderboard.getTrackedRank(competitorWithBoat, raceColumn, now));
                assertEquals(rank, leaderboard.getContent(now).get(key).getTrackedRank());
                assertEquals(rank, leaderboard.getEntry(competitorWithBoat, raceColumn, now).getTrackedRank());
                assertEquals(rank, leaderboard.getTotalPoints(competitorWithBoat, raceColumn, now), 0.000000001);
                assertEquals(rank*scoringScheme.getScoreFactor(raceColumn), leaderboard.getContent(now).get(key).getTotalPoints(), 0.000000001);
                assertEquals(rank*scoringScheme.getScoreFactor(raceColumn), leaderboard.getEntry(competitorWithBoat, raceColumn, now).getTotalPoints(), 0.000000001);
                // One race is discarded because four races were started, and for [3-6) one race can be discarded.
                // The discarded race is the worst of those started, so the one with rank 4.
                int expectedNumberOfDiscardedRaces =
                        numberOfRacesToCompareToDiscardThresholds < firstDiscardingThreshold ? 0 :
                            numberOfRacesToCompareToDiscardThresholds < secondDiscardingThreshold ? 1 : 2;
                boolean discarded = ranksOfNonMedalStartedRaces.indexOf(rank) >= ranksOfNonMedalStartedRaces.size()-expectedNumberOfDiscardedRaces;
                int expected = discarded ? 0 : rank==medalRacePoints?2*rank:rank;
                assertEquals(expected, leaderboard.getNetPoints(competitorWithBoat, raceColumn, now), 0.000000001);
                assertEquals(expected, leaderboard.getContent(now).get(key).getNetPoints(), 0.000000001);
                assertEquals(expected, leaderboard.getEntry(competitorWithBoat, raceColumn, now).getNetPoints(), 0.000000001);
                netPoints += leaderboard.getContent(now).get(key).getNetPoints().intValue();
            } else {
                assertEquals(0, leaderboard.getTrackedRank(competitorWithBoat, raceColumn, now));
                assertNull(leaderboard.getTotalPoints(competitorWithBoat, raceColumn, now));
                assertEquals(0, leaderboard.getContent(now).get(key).getTrackedRank());
                assertNull(leaderboard.getContent(now).get(key).getTotalPoints());
                assertEquals(0, leaderboard.getEntry(competitorWithBoat, raceColumn, now).getTrackedRank());
                assertNull(leaderboard.getEntry(competitorWithBoat, raceColumn, now).getTotalPoints());
                // no increment on netPoints
            }
        }
        assertEquals(netPoints, leaderboard.getNetPoints(competitorWithBoat, now), 0.000000001);
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
