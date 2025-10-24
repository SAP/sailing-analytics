package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRevokeEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogUseCompetitorsFromRaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogUseCompetitorsFromRaceLogEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.leaderboard.impl.CompetitorProviderFromRaceColumnsAndRegattaLike;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests the behavior of the class {@link CompetitorProviderFromRaceColumnsAndRegattaLike}, paying particular
 * attention to its caching and cache invalidation logic. When competitors and their boats are added to regatta logs or race logs
 * or when a tracked race is attached to a column or a column is added or removed, its caches need to be
 * invalidated and re-calculated accordingly.
 *  
 * @author Axel Uhl (d043530)
 *
 */
public class CompetitorProviderCacheInvalidationTest extends AbstractLeaderboardTest {
    private CompetitorProviderFromRaceColumnsAndRegattaLike competitorProviderFlexibleLeaderboard;
    private CompetitorProviderFromRaceColumnsAndRegattaLike competitorProviderRegattaLeaderboard;
    private FlexibleLeaderboardImpl flexibleLeaderboard;
    private RegattaLeaderboardImpl regattaLeaderboard;
    private RegattaImpl regatta;
    final int NUMBER_OF_COMP_LISTS = 4;
    @SuppressWarnings("unchecked")
    private List<Competitor>[] compLists = (List<Competitor>[]) new List<?>[NUMBER_OF_COMP_LISTS];
    private Map<Competitor, Boat> boats = new HashMap<>();
    
    @BeforeEach
    public void setUp() {
        final CourseAreaImpl courseArea = new CourseAreaImpl(
                "Test Course Area", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null);
        flexibleLeaderboard = new FlexibleLeaderboardImpl("Test Flexible Leaderboard",
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(), courseArea);
        competitorProviderFlexibleLeaderboard = new CompetitorProviderFromRaceColumnsAndRegattaLike(flexibleLeaderboard);
        regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, "Test Regatta", new BoatClassImpl(BoatClassMasterdata._49ER),
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /* startDate */ null, /* endDate */null,
                /* trackedRegattaRegistry */null, new LowPoint(), UUID.randomUUID(),
                courseArea, /* registrationLinkSecret */ UUID.randomUUID().toString());
        regattaLeaderboard = new RegattaLeaderboardImpl(regatta, new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        regatta.addSeries(new SeriesImpl("Test Series", /* isMedal */false, /* isFleetsCanRunInParallel */ true, Arrays.asList(new FleetImpl("Yellow"),
                new FleetImpl("Blue")), Arrays.asList("R1", "R2", "R3"), /* trackedRegattaRegistry */null));
        for (int l = 0; l < NUMBER_OF_COMP_LISTS; l++) {
            compLists[l] = new ArrayList<Competitor>();
            for (int i = 0; i < 10; i++) {
                Competitor c = createCompetitor("" + l + "/" + i);
                compLists[l].add(c);
                boats.put(c, createBoat("" + l + "/" + i));
            }
        }
        competitorProviderRegattaLeaderboard = new CompetitorProviderFromRaceColumnsAndRegattaLike(regattaLeaderboard);
    }
    
    /**
     * Asserts that a regatta leaderboard provides the regatta log-registered competitors even if there
     * is not a single race column in the regatta yet.
     */
    @Test
    public void testCompetitorsFromRegattaLogForEmptyRegatta() {
        for (final Series seriesToRemove : regatta.getSeries()) {
            regatta.removeSeries(seriesToRemove);
        }
        RegattaLog regattaLog = regattaLeaderboard.getRegatta().getRegattaLog();
        final Map<Competitor, RegattaLogRegisterCompetitorEvent> competitorOnRegattaLogRegistrationEvents = new HashMap<>();
        final LogEventAuthorImpl author = new LogEventAuthorImpl("Me", 0);
        for (Competitor c : compLists[0]) {
            final RegattaLogRegisterCompetitorEvent registerCompetitorEvent = new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author, UUID.randomUUID(), c);
            regattaLog.add(registerCompetitorEvent);
            competitorOnRegattaLogRegistrationEvents.put(c, registerCompetitorEvent);
        }
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actual2 = new HashSet<>();
        Util.addAll(regatta.getAllCompetitors(), actual2);
        assertEquals(expected, actual2);
    }
    
    @Test
    public void testSimpleCompetitorListOnOneRaceInFlexibleLeaderboard() {
        TrackedRace trackedRace = new MockedTrackedRaceWithStartTimeAndRanks(MillisecondsTimePoint.now(), compLists[0]);
        flexibleLeaderboard.addRace(trackedRace, "R1", /* medalRace */ false);
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRace = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R1"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace);
        assertEquals(expected, actualForRace);
    }

    @Test
    public void testSimpleCompetitorListOnOneRaceLogInFlexibleLeaderboard() {
        flexibleLeaderboard.addRaceColumn("R1", /* medalRace */ false);
        RaceLog raceLog = flexibleLeaderboard.getRacelog("R1", LeaderboardNameConstants.DEFAULT_FLEET_NAME);
        final int passId = 1;
        final LogEventAuthorImpl author = new LogEventAuthorImpl("Me", 0);        
        raceLog.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(MillisecondsTimePoint.now(), author, MillisecondsTimePoint.now(), UUID.randomUUID(), passId));
        final Map<Competitor, RaceLogRegisterCompetitorEvent> competitorOnRaceLogRegistrationEvents = new HashMap<>();
        for (Competitor c : compLists[0]) {
            final RaceLogRegisterCompetitorEvent registerCompetitorEvent = new RaceLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), author, passId, c, boats.get(c));
            raceLog.add(registerCompetitorEvent);
            competitorOnRaceLogRegistrationEvents.put(c, registerCompetitorEvent);
        }
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRace = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R1"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace);
        assertEquals(expected, actualForRace);
        // we're now revoking a competitor registration on the race log and assert
        // that the competitor is gone from the list:
        final Competitor competitorToRevokeFromRaceLog = compLists[0].get(2);
        raceLog.add(new RaceLogRevokeEventImpl(author, passId, competitorOnRaceLogRegistrationEvents.get(competitorToRevokeFromRaceLog), "Test revoking"));
        actualForRace = new HashSet<>(); // try another time; cache should of course yield an equal result (although
        // we're not asserting here that the result actually comes from the cache)
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R1"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace);
        expected.remove(competitorToRevokeFromRaceLog);
        assertEquals(expected.size(), actualForRace.size());
        assertEquals(expected, actualForRace);
        flexibleLeaderboard.removeRaceColumn("R1"); // test that changes to the race columns invalidate the cache
        flexibleLeaderboard.addRaceColumn("R1", /* medalRace */ false);
        actualForRace = new HashSet<>(); // try another time; cache should of course yield an equal result (although
        // we're not asserting here that the result actually comes from the cache)
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R1"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace);
        assertTrue(actualForRace.isEmpty());
    }

    @Test
    public void testSimpleCompetitorListOnRegattaLogInFlexibleLeaderboard() {
        flexibleLeaderboard.addRaceColumn("R1", /* medalRace */ false);
        RegattaLog regattaLog = flexibleLeaderboard.getRegattaLog();
        for (Competitor c : compLists[0]) {
            regattaLog.add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), new LogEventAuthorImpl("Me", 0), UUID.randomUUID(), c));
        }
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRace = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R1"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace);
        assertEquals(expected, actualForRace);
    }

    @Test
    public void testSimpleCompetitorListOnOneRaceLogAndRegattaLogInFlexibleLeaderboard() throws NotRevokableException {
        flexibleLeaderboard.addRaceColumn("R1", /* medalRace */ false);
        RegattaLog regattaLog = flexibleLeaderboard.getRegattaLog();
        for (Competitor c : compLists[0]) {
            regattaLog.add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), new LogEventAuthorImpl("Me", 0), UUID.randomUUID(), c));
        }
        RaceLog raceLog = flexibleLeaderboard.getRacelog("R1", LeaderboardNameConstants.DEFAULT_FLEET_NAME);
        final LogEventAuthorImpl author = new LogEventAuthorImpl("Me", 0);
        int passId = 1;
        final RaceLogUseCompetitorsFromRaceLogEvent usesCompetitorsFromRaceLogEvent = new RaceLogUseCompetitorsFromRaceLogEventImpl(MillisecondsTimePoint.now(), author, MillisecondsTimePoint.now(), UUID.randomUUID(), passId);
        raceLog.add(usesCompetitorsFromRaceLogEvent);
        for (Competitor c : compLists[1]) {
            raceLog.add(new RaceLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), new LogEventAuthorImpl("Me", 0), 1, c, boats.get(c)));
        }
        Set<Competitor> allCompetitors = new HashSet<>();
        allCompetitors.addAll(compLists[0]);
        allCompetitors.addAll(compLists[1]);
        // expected are only the competitors in the RaceLog, because only one RaceColumn is
        // registered, which has the competitors registered in the RaceLog.
        assertRegattaAndRaceCompetitors(allCompetitors, new HashSet<>(compLists[1]));
        // Now we revoke that the race log provides the competitors for R1; the competitors should then
        // snap back to those competitors taken from the regatta log for both, the rentire leaderboard
        // as well as for the race column
        raceLog.revokeEvent(author, usesCompetitorsFromRaceLogEvent);
        assertRegattaAndRaceCompetitors(new HashSet<>(compLists[0]), new HashSet<>(compLists[0]));
        // And now re-introduce per-race competitors and validate again that the cache adjusts properly:
        raceLog.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(MillisecondsTimePoint.now(), author, MillisecondsTimePoint.now(), UUID.randomUUID(), passId));
        assertRegattaAndRaceCompetitors(allCompetitors, new HashSet<>(compLists[1]));
    }

    private void assertRegattaAndRaceCompetitors(Set<Competitor> expectedAllCompetitors, Set<Competitor> expectedForRace) {
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(), actual);
        assertEquals(expectedAllCompetitors, actual);
        Set<Competitor> actualForRace = new HashSet<>();
        
        // For the race only the competitors registered on RaceLog as RaceLogUseCompetitorsFromRaceLogEvent is present
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R1"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace);
        assertEquals(expectedForRace, actualForRace);
    }

    @Test
    public void testTwoCompetitorListsOnTwoRacesInFlexibleLeaderboard() {
        TrackedRace trackedRace1 = new MockedTrackedRaceWithStartTimeAndRanks(MillisecondsTimePoint.now(), compLists[0]);
        flexibleLeaderboard.addRace(trackedRace1, "R1", /* medalRace */ false);
        TrackedRace trackedRace2 = new MockedTrackedRaceWithStartTimeAndRanks(MillisecondsTimePoint.now(), compLists[1]);
        flexibleLeaderboard.addRace(trackedRace2, "R2", /* medalRace */ false);
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        expected.addAll(compLists[1]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRace1 = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R1"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace1);
        Set<Competitor> actualForRace2 = new HashSet<>();
        Util.addAll(competitorProviderFlexibleLeaderboard.getAllCompetitors(
                flexibleLeaderboard.getRaceColumnByName("R2"),
                flexibleLeaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), actualForRace2);
        assertEquals(new HashSet<>(compLists[0]), actualForRace1);
        assertEquals(new HashSet<>(compLists[1]), actualForRace2);
    }

    @Test
    public void testSimpleCompetitorListOnOneRaceInRegattaLeaderboard() {
        TrackedRace trackedRace = new MockedTrackedRaceWithStartTimeAndRanks(MillisecondsTimePoint.now(), compLists[0]);
        regattaLeaderboard.getRaceColumnByName("R1").setTrackedRace(regattaLeaderboard.getRaceColumnByName("R1").getFleetByName("Yellow"), trackedRace);
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRaceYellow = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        assertEquals(expected, actualForRaceYellow);
        Set<Competitor> actualForRaceBlue = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Blue")), actualForRaceBlue);
        assertEquals(Collections.emptySet(), actualForRaceBlue);
    }

    @Test
    public void testTwoCompetitorListsOnTwoRacesInRegattaLeaderboard() {
        TrackedRace trackedRace1 = new MockedTrackedRaceWithStartTimeAndRanks(MillisecondsTimePoint.now(), compLists[0]);
        regattaLeaderboard.getRaceColumnByName("R1").setTrackedRace(regattaLeaderboard.getRaceColumnByName("R1").getFleetByName("Yellow"), trackedRace1);
        TrackedRace trackedRace2 = new MockedTrackedRaceWithStartTimeAndRanks(MillisecondsTimePoint.now(), compLists[1]);
        regattaLeaderboard.getRaceColumnByName("R1").setTrackedRace(regattaLeaderboard.getRaceColumnByName("R1").getFleetByName("Blue"), trackedRace2);
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        expected.addAll(compLists[1]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRaceYellow = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        assertEquals(new HashSet<>(compLists[0]), actualForRaceYellow);
        Set<Competitor> actualForRaceBlue = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Blue")), actualForRaceBlue);
        assertEquals(new HashSet<>(compLists[1]), actualForRaceBlue);
    }

    @Test
    public void testSimpleCompetitorListOnOneRaceLogInRegattaLeaderboard() {
        RaceLog raceLog = regattaLeaderboard.getRacelog("R1", "Yellow");
        final LogEventAuthorImpl author = new LogEventAuthorImpl("Me", 0);
        int passId = 1;
        raceLog.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(MillisecondsTimePoint.now(), author, MillisecondsTimePoint.now(), UUID.randomUUID(), passId));
        for (Competitor c : compLists[0]) {
            raceLog.add(new RaceLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), new LogEventAuthorImpl("Me", 0), 1, c, boats.get(c)));
        }
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRaceYellow = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        assertEquals(new HashSet<>(compLists[0]), actualForRaceYellow);
    }

    @Test
    public void testSimpleCompetitorListOnRegattaLogInRegattaLeaderboard() throws NotRevokableException {
        RegattaLog regattaLog = regattaLeaderboard.getRegatta().getRegattaLog();
        final Map<Competitor, RegattaLogRegisterCompetitorEvent> competitorOnRegattaLogRegistrationEvents = new HashMap<>();
        final LogEventAuthorImpl author = new LogEventAuthorImpl("Me", 0);
        for (Competitor c : compLists[0]) {
            final RegattaLogRegisterCompetitorEvent registerCompetitorEvent = new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author, UUID.randomUUID(), c);
            regattaLog.add(registerCompetitorEvent);
            competitorOnRegattaLogRegistrationEvents.put(c, registerCompetitorEvent);
        }
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualForRaceYellow = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        assertEquals(new HashSet<>(compLists[0]), actualForRaceYellow);
        Set<Competitor> actualForRaceBlue = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Blue")), actualForRaceBlue);
        assertEquals(new HashSet<>(compLists[0]), actualForRaceBlue);
        // now revoke one registration again from the regatta log
        final Competitor competitorToRevokeFromRegattaLog = compLists[0].get(2);
        regattaLog.revokeEvent(author, competitorOnRegattaLogRegistrationEvents.get(competitorToRevokeFromRegattaLog));
        actualForRaceYellow = new HashSet<>(); // try another time; cache should of course yield an equal result (although
        // we're not asserting here that the result actually comes from the cache)
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        expected.remove(competitorToRevokeFromRegattaLog);
        assertEquals(expected.size(), actualForRaceYellow.size());
        assertEquals(expected, actualForRaceYellow);
    }

    @Test
    public void testSimpleCompetitorListOnOneRaceLogAndRegattaLogInRegattaLeaderboard() {
        final RegattaLog regattaLog = regattaLeaderboard.getRegatta().getRegattaLog();
        final LogEventAuthorImpl author = new LogEventAuthorImpl("Me", 0);
        for (Competitor c : compLists[0]) {
            regattaLog.add(new RegattaLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author, UUID.randomUUID(), c));
        }
        final RaceLog raceLog = regattaLeaderboard.getRacelog("R1", "Yellow");
        final int passId = 1;
        raceLog.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(MillisecondsTimePoint.now(), author, MillisecondsTimePoint.now(), UUID.randomUUID(), passId));
        final Map<Competitor, RaceLogRegisterCompetitorEvent> competitorOnRaceLogRegistrationEvents = new HashMap<>();
        for (Competitor c : compLists[passId]) {
            final RaceLogRegisterCompetitorEvent registerCompetitorEvent = new RaceLogRegisterCompetitorEventImpl(MillisecondsTimePoint.now(), author, passId, c, boats.get(c));
            raceLog.add(registerCompetitorEvent);
            competitorOnRaceLogRegistrationEvents.put(c, registerCompetitorEvent);
        }
        regattaLeaderboard.setSuppressed(compLists[0].get(compLists[0].size()-passId), /* suppressed */ true);
        Set<Competitor> expected = new HashSet<>(compLists[0]);
        expected.addAll(compLists[passId]);
        Set<Competitor> expectedWithoutSuppressed = new HashSet<>(expected);
        expectedWithoutSuppressed.remove(compLists[0].get(compLists[0].size()-passId));
        Set<Competitor> actual = new HashSet<>();
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(), actual);
        assertEquals(expected, actual);
        Set<Competitor> actualWithoutSuppressed = new HashSet<>();
        Util.addAll(regattaLeaderboard.getCompetitors(), actualWithoutSuppressed);
        assertEquals(expectedWithoutSuppressed, actualWithoutSuppressed);
        Set<Competitor> actualForRaceYellow = new HashSet<>();
        Set<Competitor> expectedFoRaceYellow = new HashSet<>(compLists[passId]);
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        assertEquals(expectedFoRaceYellow, actualForRaceYellow);
        actualForRaceYellow = new HashSet<>(); // try another time; cache should of course yield an equal result (although
        // we're not asserting here that the result actually comes from the cache)
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        assertEquals(expectedFoRaceYellow, actualForRaceYellow);
        // But now it gets interesting: we're revoking a competitor registration on the race log and assert
        // that the competitor is gone from the list:
        final Competitor competitorToRevokeFromRaceLog = compLists[passId].get(2);
        raceLog.add(new RaceLogRevokeEventImpl(author, passId, competitorOnRaceLogRegistrationEvents.get(competitorToRevokeFromRaceLog), "Test revoking"));
        actualForRaceYellow = new HashSet<>(); // try another time; cache should of course yield an equal result (although
        // we're not asserting here that the result actually comes from the cache)
        Util.addAll(competitorProviderRegattaLeaderboard.getAllCompetitors(
                regattaLeaderboard.getRaceColumnByName("R1"), regattaLeaderboard.getFleet("Yellow")), actualForRaceYellow);
        expectedFoRaceYellow.remove(competitorToRevokeFromRaceLog);
        assertEquals(expectedFoRaceYellow.size(), actualForRaceYellow.size());
        assertEquals(expectedFoRaceYellow, actualForRaceYellow);
    }

}
