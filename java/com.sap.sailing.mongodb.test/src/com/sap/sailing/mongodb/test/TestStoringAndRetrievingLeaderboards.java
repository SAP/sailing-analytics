package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRank;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRankAndManyCompetitors;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TestStoringAndRetrievingLeaderboards extends AbstractMongoDBTest {
    public TestStoringAndRetrievingLeaderboards() throws UnknownHostException, MongoException {
        super();
    }
    
    @Before
    public void clearCompetitorStore() {
        DomainFactory.INSTANCE.getCompetitorStore().clear();
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithSpecificColumnFactors() {
        final String leaderboardName = "TestLeaderboard";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboard.addRaceColumn("R1", /* medalRace */ false);
        RaceColumn r2 = leaderboard.addRaceColumn("R2", /* medalRace */ false);
        r2.setFactor(1.5);
        RaceColumn r3 = leaderboard.addRaceColumn("R3", /* medalRace */ false);
        r3.setFactor(2.5);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        Leaderboard loadedLeaderboard = new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        assertEquals(1.0, loadedLeaderboard.getRaceColumnByName("R1").getFactor(), 0.000000001);
        assertEquals(1.5, loadedLeaderboard.getRaceColumnByName("R2").getFactor(), 0.000000001);
        assertEquals(2.5, loadedLeaderboard.getRaceColumnByName("R3").getFactor(), 0.000000001);
    }
    
    @Test
    public void testStoreAndRetrieveLeaderboardWithSuppressedCompetitors() {
        final String leaderboardName = "TestLeaderboard";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor wolfgang = createCompetitor();
        Competitor hasso = new CompetitorImpl(234, "Hasso Plattner", Color.RED, null, null,
                        new TeamImpl("STG", Collections.singleton(
                                new PersonImpl("Hasso Plattner", new NationalityImpl("GER"),
                                /* dateOfBirth */ null, "This is famous Dr. Hasso Plattner")), new PersonImpl("Lutz Patrunky", new NationalityImpl("GER"),
                                        /* dateOfBirth */ null, "This is Patty, the coach")), new BoatImpl("Dr. Hasso Plattner's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        final String raceColumnName1 = "My First Race 1";
        MockedTrackedRaceWithFixedRankAndManyCompetitors raceWithTwoCompetitors = new MockedTrackedRaceWithFixedRankAndManyCompetitors(wolfgang, /* rank */ 1, /* started */ true);
        raceWithTwoCompetitors.addCompetitor(hasso);
        leaderboard.addRace(raceWithTwoCompetitors, raceColumnName1, /* medalRace */ false);
        leaderboard.setSuppressed(wolfgang, true);
        assertTrue(Util.contains(leaderboard.getSuppressedCompetitors(), wolfgang));
        assertFalse(Util.contains(leaderboard.getSuppressedCompetitors(), hasso));
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        Leaderboard loadedLeaderboard = new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        MockedTrackedRaceWithFixedRankAndManyCompetitors raceWithTwoCompetitors2 = new MockedTrackedRaceWithFixedRankAndManyCompetitors(wolfgang, /* rank */ 1, /* started */ true);
        raceWithTwoCompetitors2.addCompetitor(hasso);
        loadedLeaderboard.getRaceColumnByName(raceColumnName1).setTrackedRace(loadedLeaderboard.getFleet(null), raceWithTwoCompetitors2);
        assertTrue(Util.contains(loadedLeaderboard.getSuppressedCompetitors(), wolfgang));
        assertFalse(Util.contains(loadedLeaderboard.getSuppressedCompetitors(), hasso));
    }
    
    @Test
    public void testStoreAndRetrieveLeaderboardWithCommentedScoreCorrection() {
        final String leaderboardName = "TestLeaderboard";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboard.getScoreCorrection().setComment("Humba");
        TimePoint now = MillisecondsTimePoint.now();
        leaderboard.getScoreCorrection().setTimePointOfLastCorrectionsValidity(now);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        Leaderboard loadedLeaderboard = new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        assertEquals("Humba", loadedLeaderboard.getScoreCorrection().getComment());
        assertEquals(now, loadedLeaderboard.getScoreCorrection().getTimePointOfLastCorrectionsValidity());
    }
    
    @Test
    public void testStoreAndRetrieveLeaderboardWithDisplayNameSet() {
        final String leaderboardName = "TestLeaderboard";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor competitor = createCompetitor();
        TrackedRace raceWithOneCompetitor1 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        final String raceColumnName1 = "My First Race 1";
        leaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace */ false);
        final String displayName = "$$$ ... The Renamed Competitor ... $$$";
        leaderboard.setDisplayName(competitor, displayName);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(
                leaderboardName, /* regattaRegistry */null);
        // attach tracked race to leaderboard to ensure that competitor object is assigned properly
        loadedLeaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace, ignored */ false);
        Competitor loadedCompetitor = loadedLeaderboard.getCompetitorByName(competitor.getName());
        assertEquals(displayName, loadedLeaderboard.getDisplayName(loadedCompetitor));
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboard() {
        final String leaderboardName = "TestLeaderboard";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        Leaderboard loadedLeaderboard = new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        assertEquals(leaderboardName, loadedLeaderboard.getName());
        assertTrue(Arrays.equals(discardIndexResultsStartingWithHowManyRaces,
                ((ThresholdBasedResultDiscardingRule) loadedLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces()));
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithHighPointScoringScheme() {
        final String leaderboardName = "TestLeaderboard";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new HighPoint(), null);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        Leaderboard loadedLeaderboard = new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        assertSame(HighPoint.class, loadedLeaderboard.getScoringScheme().getClass());
        assertEquals(ScoringSchemeType.HIGH_POINT, loadedLeaderboard.getScoringScheme().getType());
    }
    
    @Test
    public void testStoreAndRetrieveLeaderboardWithCarryColumn() {
        final String leaderboardName = "TestLeaderboard";
        final String raceColumnName = "My First Race";
        final double carriedPointsForWolfgangHunger = 3.7;
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor competitor = createCompetitor();
        TrackedRace raceWithOneCompetitor = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        leaderboard.addRace(raceWithOneCompetitor, raceColumnName, /* medalRace */ false);
        leaderboard.setCarriedPoints(competitor, carriedPointsForWolfgangHunger);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        // attach tracked race to leaderboard to ensure that competitor object is assigned properly
        loadedLeaderboard.addRace(raceWithOneCompetitor, raceColumnName, /* medalRace, ignored */ false);
        assertEquals(leaderboardName, loadedLeaderboard.getName());
        assertTrue(Arrays.equals(discardIndexResultsStartingWithHowManyRaces,
                ((ThresholdBasedResultDiscardingRule) loadedLeaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces()));
        assertEquals(1, Util.size(loadedLeaderboard.getCompetitors()));
        assertEquals(competitor, loadedLeaderboard.getCompetitors().iterator().next());
        assertEquals(carriedPointsForWolfgangHunger, loadedLeaderboard.getCarriedPoints(competitor), 0.000000001);
    }

    @Test
    public void testStoreAndRetrieveLeaderboardWithScoreCorrections() {
        final String leaderboardName = "TestLeaderboard";
        final String raceColumnName1 = "My First Race 1";
        final String raceColumnName2 = "My First Race 2";
        final double correctedPoints = 2.75;
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor competitor = createCompetitor();
        TrackedRace raceWithOneCompetitor1 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        TrackedRace raceWithOneCompetitor2 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 2, /* started */ true);
        leaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace */ false);
        leaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace */ true);
        leaderboard.getScoreCorrection().correctScore(competitor, leaderboard.getRaceColumnByName(raceColumnName1), correctedPoints);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        // attach tracked race to leaderboard to ensure that competitor object is assigned properly
        RaceColumn loadedColumn1 = loadedLeaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace, ignored */ false);
        RaceColumn loadedColumn2 = loadedLeaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace, ignored */ false);
        assertEquals(leaderboardName, loadedLeaderboard.getName());
        assertTrue(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn1, MillisecondsTimePoint.now()));
        assertEquals(correctedPoints, (double) loadedLeaderboard.getScoreCorrection().getExplicitScoreCorrection(competitor, loadedColumn1), 0.00000001);
        assertFalse(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn2, MillisecondsTimePoint.now()));
    }

    @Test
    public void testStoreAndRetrieveLeaderboardWithScoreCorrectionsWithRaceColumnsWhoseNamesNeedEscaping() {
        final String leaderboardName = "TestLeaderboard";
        final String raceColumnName1 = "My.First.Race$1";
        final String raceColumnName2 = "My.First$Race$2";
        final double correctedPoints = 2.75;
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor competitor = createCompetitor();
        TrackedRace raceWithOneCompetitor1 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        TrackedRace raceWithOneCompetitor2 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 2, /* started */ true);
        leaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace */ false);
        leaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace */ true);
        leaderboard.getScoreCorrection().correctScore(competitor, leaderboard.getRaceColumnByName(raceColumnName1), correctedPoints);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        // attach tracked race to leaderboard to ensure that competitor object is assigned properly
        RaceColumn loadedColumn1 = loadedLeaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace, ignored */ false);
        RaceColumn loadedColumn2 = loadedLeaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace, ignored */ false);
        assertEquals(leaderboardName, loadedLeaderboard.getName());
        assertTrue(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn1, MillisecondsTimePoint.now()));
        assertEquals(correctedPoints, (double) loadedLeaderboard.getScoreCorrection().getExplicitScoreCorrection(competitor, loadedColumn1), 0.00000001);
        assertFalse(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn2, MillisecondsTimePoint.now()));
    }

    @Test
    public void testStoreAndRetrieveLeaderboardWithMaxPointsReason() {
        final String leaderboardName = "TestLeaderboard";
        final String raceColumnName1 = "My First Race 1";
        final String raceColumnName2 = "My First Race 2";
        final MaxPointsReason maxPointsReason = MaxPointsReason.DNF;
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor competitor = createCompetitor();
        TrackedRace raceWithOneCompetitor1 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        TrackedRace raceWithOneCompetitor2 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 2, /* started */ true);
        leaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace */ false);
        leaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace */ true);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitor, leaderboard.getRaceColumnByName(raceColumnName2), maxPointsReason);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        // attach tracked race to leaderboard to ensure that competitor object is assigned properly
        RaceColumn loadedColumn1 = loadedLeaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace, ignored */ false);
        RaceColumn loadedColumn2 = loadedLeaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace, ignored */ false);
        assertEquals(leaderboardName, loadedLeaderboard.getName());
        assertFalse(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn1, MillisecondsTimePoint.now()));
        assertTrue(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn2, MillisecondsTimePoint.now()));
        assertEquals(maxPointsReason, loadedLeaderboard.getScoreCorrection().getMaxPointsReason(competitor, loadedColumn2, MillisecondsTimePoint.now()));
    }

    @Test
    public void testStoreAndRetrieveLeaderboardWithMaxPointsReasonAndScoreCorrection() {
        final String leaderboardName = "TestLeaderboard";
        final String raceColumnName1 = "My First Race 1";
        final String raceColumnName2 = "My First Race 2";
        final double correctedPoints = 2.55;
        final MaxPointsReason maxPointsReason = MaxPointsReason.DNF;
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor competitor = createCompetitor();
        TrackedRace raceWithOneCompetitor1 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        TrackedRace raceWithOneCompetitor2 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 2, /* started */ true);
        leaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace */ false);
        leaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace */ true);
        leaderboard.getScoreCorrection().correctScore(competitor, leaderboard.getRaceColumnByName(raceColumnName1), correctedPoints);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitor, leaderboard.getRaceColumnByName(raceColumnName2), maxPointsReason);
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        // attach tracked race to leaderboard to ensure that competitor object is assigned properly
        RaceColumn loadedColumn1 = loadedLeaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace, ignored */ false);
        RaceColumn loadedColumn2 = loadedLeaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace, ignored */ false);
        assertEquals(leaderboardName, loadedLeaderboard.getName());
        assertTrue(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn1, MillisecondsTimePoint.now()));
        assertEquals(correctedPoints, (double) loadedLeaderboard.getScoreCorrection().getExplicitScoreCorrection(competitor, loadedColumn1), 0.000000001);
        assertTrue(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn2, MillisecondsTimePoint.now()));
        assertEquals(maxPointsReason, loadedLeaderboard.getScoreCorrection().getMaxPointsReason(competitor, loadedColumn2, MillisecondsTimePoint.now()));
    }

    /**
     * See bug 1707; the problem that this test case is trying to reproduce occurs when unlinking the last race from a leaderboard
     * that was contributing one or more competitors that have a score correction. At commit 31074ccf2d3e2440d6a6f680504f150c264992ff
     * the leaderboard is updated in the DB when a tracked race is removed (not explicitly unlinked). This is problematic for two reasons.
     * First, the inverse operation (re-loading the race and automatically linking it again) does not lead to an update; second,
     * the leaderboard loses all its score corrections in the DB because the score corrections, although still present in the
     * ScoreCorrectionsImpl object, are not written to the DB because the leaderboard's competitor list is computed from its
     * attached tracked races and therefore now is empty.
     */
    @Test
    public void testRemovingAndReAddingLastRaceWithScoreCorrections() {
        final String leaderboardName = "TestLeaderboard";
        final String raceColumnName1 = "My First Race 1";
        final String raceColumnName2 = "My First Race 2";
        final double correctedPoints = 2.55;
        final MaxPointsReason maxPointsReason = MaxPointsReason.DNF;
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        Competitor competitor = createCompetitor();
        TrackedRace raceWithOneCompetitor1 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        TrackedRace raceWithOneCompetitor2 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 2, /* started */ true);
        RaceColumn r1 = leaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace */ false);
        RaceColumn r2 = leaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace */ true);
        leaderboard.getScoreCorrection().correctScore(competitor, leaderboard.getRaceColumnByName(raceColumnName1), correctedPoints);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitor, leaderboard.getRaceColumnByName(raceColumnName2), maxPointsReason);
        // now release the tracked races; leaving no competitors in leaderboard.getCompetitors()
        r1.releaseTrackedRace(leaderboard.getFleet(null));
        r2.releaseTrackedRace(leaderboard.getFleet(null));
        new MongoObjectFactoryImpl(db).storeLeaderboard(leaderboard);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE).loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        // attach tracked race to leaderboard to ensure that competitor object is assigned properly
        RaceColumn loadedColumn1 = loadedLeaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace, ignored */ false);
        RaceColumn loadedColumn2 = loadedLeaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace, ignored */ false);
        assertEquals(leaderboardName, loadedLeaderboard.getName());
        assertTrue(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn1, MillisecondsTimePoint.now()));
        assertEquals(correctedPoints, (double) loadedLeaderboard.getScoreCorrection().getExplicitScoreCorrection(competitor, loadedColumn1), 0.000000001);
        assertTrue(loadedLeaderboard.getScoreCorrection().isScoreCorrected(competitor, loadedColumn2, MillisecondsTimePoint.now()));
        assertEquals(maxPointsReason, loadedLeaderboard.getScoreCorrection().getMaxPointsReason(competitor, loadedColumn2, MillisecondsTimePoint.now()));
    }

    /**
     * In conjunction with bug 1707 it turns out to be dangerous that the competitors for which score corrections were loaded are
     * only looked up in the leaderboard. Therefore, score corrections for competitors that exist already in the scope of the
     * competitor store but aren't yet attached to the leaderboard will not be updated to the leaderboard's score corrections object
     * and therefore won't be updated to the DB in case the leaderboard is stored before the tracked races are attached and thus
     * contribute the competitors, hence resolving the delayed score corrections.
     */
    @Test
    public void testStoringJustLoadedScoreCorrectionsWithNoTrackedRaceAttached() {
        final String leaderboardName = "TestLeaderboard";
        final String raceColumnName1 = "My First Race 1";
        final String raceColumnName2 = "My First Race 2";
        final double correctedPoints = 2.55;
        final MaxPointsReason maxPointsReason = MaxPointsReason.DNF;
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        final DomainFactory domainFactory = new DomainFactoryImpl((srlid)->null);
        // create the competitor through the competitor store/factory here so that the DomainObjectFactory finds it and
        // resolves the score corrections appropriate
        Competitor competitor = domainFactory.getOrCreateCompetitor(123, "$$$Dr. Wolfgang+Hunger$$$", Color.RED, "someone@nowhere.de", null, new TeamImpl("STG", Collections.singleton(
                new PersonImpl("$$$Dr. Wolfgang+Hunger$$$", new NationalityImpl("GER"),
                /* dateOfBirth */ null, "This is famous Dr. Wolfgang Hunger")), new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                        /* dateOfBirth */ null, "This is Rigo, the coach")), new BoatImpl("Dr. Wolfgang Hunger's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null),
                        /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        TrackedRace raceWithOneCompetitor1 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 1, /* started */ true);
        TrackedRace raceWithOneCompetitor2 = new MockedTrackedRaceWithFixedRank(competitor, /* rank */ 2, /* started */ true);
        leaderboard.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace */ false);
        leaderboard.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace */ true);
        leaderboard.getScoreCorrection().correctScore(competitor, leaderboard.getRaceColumnByName(raceColumnName1), correctedPoints);
        leaderboard.getScoreCorrection().setMaxPointsReason(competitor, leaderboard.getRaceColumnByName(raceColumnName2), maxPointsReason);
        final MongoObjectFactoryImpl mongoObjectFactory = new MongoObjectFactoryImpl(db);
        mongoObjectFactory.storeLeaderboard(leaderboard);
        final DomainObjectFactoryImpl domainObjectFactory = new DomainObjectFactoryImpl(db, domainFactory);
        FlexibleLeaderboard loadedLeaderboard = (FlexibleLeaderboard) domainObjectFactory.loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        // don't attach tracked race to leaderboard and immediately store again
        mongoObjectFactory.storeLeaderboard(loadedLeaderboard);
        FlexibleLeaderboard loadedLeaderboard2 = (FlexibleLeaderboard) domainObjectFactory.loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        RaceColumn loadedColumn1 = loadedLeaderboard2.addRace(raceWithOneCompetitor1, raceColumnName1, /* medalRace, ignored */ false);
        RaceColumn loadedColumn2 = loadedLeaderboard2.addRace(raceWithOneCompetitor2, raceColumnName2, /* medalRace, ignored */ false);
        assertEquals(leaderboardName, loadedLeaderboard2.getName());
        assertTrue(loadedLeaderboard2.getScoreCorrection().isScoreCorrected(competitor, loadedColumn1, MillisecondsTimePoint.now()));
        assertEquals(correctedPoints, (double) loadedLeaderboard2.getScoreCorrection().getExplicitScoreCorrection(competitor, loadedColumn1), 0.000000001);
        assertTrue(loadedLeaderboard2.getScoreCorrection().isScoreCorrected(competitor, loadedColumn2, MillisecondsTimePoint.now()));
        assertEquals(maxPointsReason, loadedLeaderboard2.getScoreCorrection().getMaxPointsReason(competitor, loadedColumn2, MillisecondsTimePoint.now()));
    }

    private Competitor createCompetitor() {
        Competitor competitor = new CompetitorImpl(123, "$$$Dr. Wolfgang+Hunger$$$", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                                new PersonImpl("$$$Dr. Wolfgang+Hunger$$$", new NationalityImpl("GER"),
                                /* dateOfBirth */ null, "This is famous Dr. Wolfgang Hunger")), new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                                        /* dateOfBirth */ null, "This is Rigo, the coach")), new BoatImpl("Dr. Wolfgang Hunger's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        return competitor;
    }

}
