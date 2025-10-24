package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPointExtremeSailingSeriesOverall;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.media.MediaDBFactory;
import com.sap.sailing.domain.racelog.tracking.EmptySensorFixStore;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRankAndManyCompetitors;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrection;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.testsupport.SecurityServiceMockFactory;

public class TestStoringAndRetrievingLeaderboardGroups extends AbstractMongoDBTest {
    private MongoObjectFactory mongoObjectFactory = null;
    private DomainObjectFactory domainObjectFactory = null;


    public TestStoringAndRetrievingLeaderboardGroups() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    public void setUp() {
        DomainFactory.INSTANCE.getCompetitorAndBoatStore().clearCompetitors();
        mongoObjectFactory = new MongoObjectFactoryImpl(db);
        domainObjectFactory = new DomainObjectFactoryImpl(db, DomainFactory.INSTANCE);
    }

    @Test
    public void testStoringAndRetrievingLeaderboardGroupWithOverallLeaderboard() {
        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };

        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);

        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, /* displayName */ null, false, leaderboards);
        final Leaderboard overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup,
                new HighPointExtremeSailingSeriesOverall(), new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup.setOverallLeaderboard(overallLeaderboard);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);

        final LeaderboardGroup loadedLeaderboardGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null,
                /* leaderboardRegistry */ null);
        assertNotNull(loadedLeaderboardGroup.getOverallLeaderboard());
        assertEquals(leaderboardGroup.getId(), loadedLeaderboardGroup.getId());
        assertNotSame(leaderboardGroup.getOverallLeaderboard(), loadedLeaderboardGroup.getOverallLeaderboard());
        assertSame(ScoringSchemeType.HIGH_POINT_ESS_OVERALL, loadedLeaderboardGroup.getOverallLeaderboard().getScoringScheme().getType());
    }

    @Test
    public void testStoringAndRetrievingLeaderboardGroupWithOverallLeaderboardWithScoreCorrection() throws NoWindException {
        
        final SecurityService securityService = SecurityServiceMockFactory.mockSecurityService();
        final RacingEventService racingEventService  = new RacingEventServiceImpl() {
            @Override
            public SecurityService getSecurityService() {
                return securityService;
            }
        };
        Competitor wolfgangWithoutBoat = new CompetitorImpl(123, "$$$Dr. Wolfgang+Hunger$$$", "KYC", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                                        new PersonImpl("$$$Dr. Wolfgang+Hunger$$$", new NationalityImpl("GER"),
                                                /* dateOfBirth */ null, "This is famous Dr. Wolfgang Hunger")), new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                                                        /* dateOfBirth */ null, "This is Rigo, the coach")), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DynamicBoat wolfgangsBoat = new BoatImpl("123", "Dr. Wolfgang Hunger's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null);
        CompetitorWithBoat wolfgang =  new CompetitorWithBoatImpl(wolfgangWithoutBoat,wolfgangsBoat);
        Competitor hassoWithoutBoat = new CompetitorImpl(234, "Hasso Plattner", "KYC", Color.RED, null, null,
                                new TeamImpl("STG", Collections.singleton(
                                        new PersonImpl("Hasso Plattner", new NationalityImpl("GER"),
                                                /* dateOfBirth */ null, "This is famous Dr. Hasso Plattner")), new PersonImpl("Lutz Patrunky", new NationalityImpl("GER"),
                                                        /* dateOfBirth */ null, "This is Patty, the coach")), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DynamicBoat hassosBoat = new BoatImpl("456", "Dr. Hasso Plattner's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null);
        CompetitorWithBoat hasso = new CompetitorWithBoatImpl(hassoWithoutBoat,hassosBoat);
        final String raceColumnName1 = "My First Race 1";
        MockedTrackedRaceWithFixedRankAndManyCompetitors raceWithTwoCompetitors = new MockedTrackedRaceWithFixedRankAndManyCompetitors(
                wolfgang, /* rank */ 1, /* started */ true);
        raceWithTwoCompetitors.addCompetitorWithBoat(hasso);

        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };

        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";

        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        racingEventService.addLeaderboard(leaderboard);
        leaderboard.addRace(raceWithTwoCompetitors, raceColumnName1, /* medalRace */false);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        racingEventService.addLeaderboard(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        racingEventService.addLeaderboard(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        racingEventService.addLeaderboard(leaderboard);

        LeaderboardGroup leaderboardGroup = racingEventService.addLeaderboardGroup(UUID.randomUUID(), groupName, groupDescription,
                "The LG Display Name", false,
                Arrays.asList(leaderboardNames), new int[0], ScoringSchemeType.HIGH_POINT_ESS_OVERALL);
        final Leaderboard overallLeaderboard = leaderboardGroup.getOverallLeaderboard();
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);
        racingEventService.apply(new UpdateLeaderboardScoreCorrection(overallLeaderboard.getName(), "Leaderboard 3",
                wolfgang.getId().toString(), 99.9, MillisecondsTimePoint.now()));

        final LeaderboardGroup loadedLeaderboardGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null,
                /* leaderboardRegistry */ null);
        loadedLeaderboardGroup.getLeaderboards().iterator().next().getRaceColumnByName(raceColumnName1).setTrackedRace(
                loadedLeaderboardGroup.getLeaderboards().iterator().next().getFleet(null), raceWithTwoCompetitors);
        Leaderboard loadedOverallLeaderboard = loadedLeaderboardGroup.getOverallLeaderboard();
        final RaceColumn leaderboard3Column = loadedOverallLeaderboard.getRaceColumnByName("Leaderboard 3");
        assertTrue(loadedOverallLeaderboard.getScoreCorrection().hasCorrectionFor(leaderboard3Column));
        final Double netPoints = loadedOverallLeaderboard.getNetPoints(wolfgang, leaderboard3Column,
                MillisecondsTimePoint.now());
        assertNotNull(netPoints);
        assertEquals(99.9, netPoints, 0.00000000001);
    }

    @Test
    public void testStoringAndRetrievingLeaderboardGroupWithSuppressedCompetitorsInOverallLeaderboard() {
        Competitor wolfgangWithoutBoat = new CompetitorImpl(123, "$$$Dr. Wolfgang+Hunger$$$", "KYC", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                                        new PersonImpl("$$$Dr. Wolfgang+Hunger$$$", new NationalityImpl("GER"),
                                                /* dateOfBirth */ null, "This is famous Dr. Wolfgang Hunger")), new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                                                        /* dateOfBirth */ null, "This is Rigo, the coach")), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DynamicBoat wolfgangsBoat = new BoatImpl("123", "Dr. Wolfgang Hunger's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null); 
        CompetitorWithBoat wolfgang =  new CompetitorWithBoatImpl(wolfgangWithoutBoat,wolfgangsBoat);
        Competitor hassoWithoutBaot = new CompetitorImpl(234, "Hasso Plattner", "KYC", Color.RED, null, null, 
                                new TeamImpl("STG", Collections.singleton(
                                        new PersonImpl("Hasso Plattner", new NationalityImpl("GER"),
                                                /* dateOfBirth */ null, "This is famous Dr. Hasso Plattner")), new PersonImpl("Lutz Patrunky", new NationalityImpl("GER"),
                                                        /* dateOfBirth */ null, "This is Patty, the coach")), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DynamicBoat hassosBoat = new BoatImpl("456", "Dr. Hasso Plattner's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null); 
        CompetitorWithBoat hasso = new CompetitorWithBoatImpl(hassoWithoutBaot,hassosBoat);
        final String raceColumnName1 = "My First Race 1";
        MockedTrackedRaceWithFixedRankAndManyCompetitors raceWithTwoCompetitors = new MockedTrackedRaceWithFixedRankAndManyCompetitors(
                wolfgang, /* rank */ 1, /* started */ true);
        raceWithTwoCompetitors.addCompetitorWithBoat(hasso);

        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };

        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard.addRace(raceWithTwoCompetitors, raceColumnName1, /* medalRace */false);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);

        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, /* displayName */ null, false, leaderboards);
        final Leaderboard overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup,
                new HighPointExtremeSailingSeriesOverall(), new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup.setOverallLeaderboard(overallLeaderboard);
        overallLeaderboard.setSuppressed(wolfgang, true);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);

        final LeaderboardGroup loadedLeaderboardGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null,
                /* leaderboardRegistry */ null);
        // ensure that with no tracked races associated, suppressed competitors are not yet resolved:
        assertFalse(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), wolfgang));
        assertFalse(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), hasso));
        // now add tracked race to one of the group's leaderboards:
        loadedLeaderboardGroup.getLeaderboards().iterator().next().getRaceColumnByName(raceColumnName1).setTrackedRace(
                loadedLeaderboardGroup.getLeaderboards().iterator().next().getFleet(null), raceWithTwoCompetitors);
        // ensure that with no tracked races associated, suppressed competitors are not yet resolved:
        assertTrue(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), wolfgang));
        assertFalse(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), hasso));
    }

    @Test
    public void testStoringAndRetrievingLeaderboardGroupWithTwoSuppressedCompetitorsInOverallLeaderboard() {
        Competitor wolfgangWithoutBoat = new CompetitorImpl(123, "$$$Dr. Wolfgang+Hunger$$$", "KYC", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                                        new PersonImpl("$$$Dr. Wolfgang+Hunger$$$", new NationalityImpl("GER"),
                                                /* dateOfBirth */ null, "This is famous Dr. Wolfgang Hunger")), new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                                                        /* dateOfBirth */ null, "This is Rigo, the coach")), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DynamicBoat wolfgangsBoat = new BoatImpl("123", "Dr. Wolfgang Hunger's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null); 
        CompetitorWithBoat wolfgang =  new CompetitorWithBoatImpl(wolfgangWithoutBoat,wolfgangsBoat);
        Competitor hassoWithoutBaot = new CompetitorImpl(234, "Hasso Plattner", "KYC", Color.RED, null,null, 
                                new TeamImpl("STG", Collections.singleton(
                                        new PersonImpl("Hasso Plattner", new NationalityImpl("GER"),
                                                /* dateOfBirth */ null, "This is famous Dr. Hasso Plattner")), new PersonImpl("Lutz Patrunky", new NationalityImpl("GER"),
                                                        /* dateOfBirth */ null, "This is Patty, the coach")), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DynamicBoat hassosBoat = new BoatImpl("456", "Dr. Hasso Plattner's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null);
        CompetitorWithBoat hasso = new CompetitorWithBoatImpl(hassoWithoutBaot,hassosBoat);
        final String raceColumnName1 = "My First Race 1";
        MockedTrackedRaceWithFixedRankAndManyCompetitors raceWithTwoCompetitors = new MockedTrackedRaceWithFixedRankAndManyCompetitors(
                wolfgang, /* rank */ 1, /* started */ true);
        raceWithTwoCompetitors.addCompetitorWithBoat(hasso);

        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };

        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard.addRace(raceWithTwoCompetitors, raceColumnName1, /* medalRace */false);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);

        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, /* displayName */ null, false, leaderboards);
        final Leaderboard overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup,
                new HighPointExtremeSailingSeriesOverall(), new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup.setOverallLeaderboard(overallLeaderboard);
        overallLeaderboard.setSuppressed(wolfgang, true);
        overallLeaderboard.setSuppressed(hasso, true);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);

        final LeaderboardGroup loadedLeaderboardGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null,
                /* leaderboardRegistry */ null);
        // ensure that with no tracked races associated, suppressed competitors are not yet resolved:
        assertFalse(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), wolfgang));
        assertFalse(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), hasso));
        // now add tracked race to one of the group's leaderboards:
        loadedLeaderboardGroup.getLeaderboards().iterator().next().getRaceColumnByName(raceColumnName1).setTrackedRace(
                loadedLeaderboardGroup.getLeaderboards().iterator().next().getFleet(null), raceWithTwoCompetitors);
        // ensure that with no tracked races associated, suppressed competitors are not yet resolved:
        assertTrue(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), wolfgang));
        assertTrue(Util.contains(loadedLeaderboardGroup.getOverallLeaderboard().getSuppressedCompetitors(), hasso));
    }

    /**
     * Bug 908: asserting that after loading two leaderboard groups referencing the same leaderbard the leaderboard is
     * loaded only once
     */
    @Test
    public void testStoringAndRetrievingTwoLeaderboardGroupsReferencingTheSameLeaderboard() {
        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };

        final String groupName1 = "Leaderboard Group 1";
        final String groupDescription1 = "A leaderboard group 1";
        final ArrayList<Leaderboard> leaderboards1 = new ArrayList<>();
        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards1.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards1.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards1.add(leaderboard);
        final LeaderboardGroup leaderboardGroup1 = new LeaderboardGroupImpl(groupName1, groupDescription1, /* displayName */ null, false, leaderboards1);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup1);

        final String groupName2 = "Leaderboard Group 2";
        final String groupDescription2= "A leaderboard group 2";
        final ArrayList<Leaderboard> leaderboards2 = new ArrayList<>();
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards2.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards2.add(leaderboard);
        final LeaderboardGroup leaderboardGroup2 = new LeaderboardGroupImpl(groupName2, groupDescription2, /* displayName */ null, false, leaderboards2);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup2);

        // the leaderboard named leaderboardNames[2] occurs in both groups
        RacingEventService racingEventService = new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(MongoDBService.INSTANCE, DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                .getMongoObjectFactory(MongoDBService.INSTANCE), MediaDBFactory.INSTANCE.getMediaDB(MongoDBService.INSTANCE), EmptyWindStore.INSTANCE, EmptySensorFixStore.INSTANCE, /* restoreTrackedRaces */ false); // expected to load leaderboard groups
        final LeaderboardGroup loadedLeaderboardGroup1 = racingEventService.getLeaderboardGroupByName(groupName1);
        final LeaderboardGroup loadedLeaderboardGroup2 = racingEventService.getLeaderboardGroupByName(groupName2);

        assertEquals(groupName1, loadedLeaderboardGroup1.getName());
        assertEquals(groupDescription1, loadedLeaderboardGroup1.getDescription());
        assertEquals(groupName2, loadedLeaderboardGroup2.getName());
        assertEquals(groupDescription2, loadedLeaderboardGroup2.getDescription());
        assertSame(Util.get(loadedLeaderboardGroup1.getLeaderboards(), 2), Util.get(loadedLeaderboardGroup2.getLeaderboards(), 0));
    }

    @Test
    public void testStoringAndRetrievingSimpleLeaderboardGroup() {
        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };

        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final String groupDisplayName = "Some short name";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);

        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, groupDisplayName, false, leaderboards);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);

        final LeaderboardGroup loadedLeaderboardGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null,
                /* leaderboardRegistry */ null);

        Assertions.assertEquals(groupName, loadedLeaderboardGroup.getName());
        Assertions.assertEquals(groupDescription, loadedLeaderboardGroup.getDescription());
        Assertions.assertEquals(groupDisplayName, loadedLeaderboardGroup.getDisplayName());

        int c = 0;
        for (Leaderboard board : leaderboardGroup.getLeaderboards()) {
            Assertions.assertEquals(leaderboardNames[c], board.getName());
            c++;
        }
    }

    @Test
    public void testGetLeaderboardsNotInGroup() {
        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();
        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);
        leaderboards.add(leaderboard);
        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, /* displayName */ null, false, leaderboards);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);
        final String[] ungroupedLeaderboardNames = {"Ungrouped Leaderboard 0", "Ungrouped Leaderboard 1", "Ungrouped Leaderboard 2"};
        final FlexibleLeaderboard[] ungroupedLeaderboards = {
                new FlexibleLeaderboardImpl(ungroupedLeaderboardNames[0], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                        new LowPoint(), null),
                        new FlexibleLeaderboardImpl(ungroupedLeaderboardNames[1], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                                new LowPoint(), null),
                                new FlexibleLeaderboardImpl(ungroupedLeaderboardNames[2], new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                                        new LowPoint(), null) };
        mongoObjectFactory.storeLeaderboard(ungroupedLeaderboards[0]);
        mongoObjectFactory.storeLeaderboard(ungroupedLeaderboards[1]);
        mongoObjectFactory.storeLeaderboard(ungroupedLeaderboards[2]);
        Iterable<Leaderboard> loadedUngroupedLeaderboards = domainObjectFactory.getLeaderboardsNotInGroup(/* regattaRegistry */ null,
                /* leaderboardRegistry */ null);
        Assertions.assertTrue(loadedUngroupedLeaderboards.iterator().hasNext());
        int c = 0;
        for (int i = 0; i < ungroupedLeaderboardNames.length; i++) {
            boolean loadedBoardsContainsName = false;
            for (Leaderboard board : loadedUngroupedLeaderboards) {
                if (ungroupedLeaderboardNames[i].equals(board.getName())) {
                    loadedBoardsContainsName = true;
                    c++;
                    break;
                }
            }
            Assertions.assertTrue(loadedBoardsContainsName);
        }
        Assertions.assertTrue(c == ungroupedLeaderboards.length);
    }

    @Test
    public void testLeaderboardReferenceBreak() {
        // Set up
        final String leaderboardName = "Leaderboard 0";
        final String columnName = "Column";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();
        final FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint(), null);
        final Fleet fleet = leaderboard.getFleet(null);
        final RaceColumn race = leaderboard.addRaceColumn(columnName, false);
        leaderboards.add(leaderboard);
        final LeaderboardGroup group = new LeaderboardGroupImpl(groupName, groupDescription, /* displayName */ null, false, leaderboards);
        mongoObjectFactory.storeLeaderboardGroup(group);
        LeaderboardGroup loadedGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null, /* leaderboardRegistry */ null);
        // RaceIdentifier change test
        final String regattaName = "Event";
        final String raceName = "Race";
        leaderboard.getRaceColumnByName(columnName).setRaceIdentifier(fleet, new RegattaNameAndRaceName(regattaName, raceName));
        mongoObjectFactory.storeLeaderboard(leaderboard);
        // Check if the leaderboard updated correctly
        final Leaderboard loadedLeaderboard = domainObjectFactory.loadLeaderboard(leaderboard.getName(), /* regattaRegistry */ null, /* leaderboardRegistry */ null);
        final RaceColumn loadedRaceColumnByName = loadedLeaderboard.getRaceColumnByName(columnName);
        Fleet loadedFleet = loadedRaceColumnByName.getFleetByName(fleet.getName());
        Assertions.assertEquals(race.getRaceIdentifier(fleet), loadedRaceColumnByName.getRaceIdentifier(loadedFleet));
        // Check if the group received the changes
        loadedGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null, /* leaderboardRegistry */ null);
        final RaceColumn loadedRaceColumnFromGroupByName = loadedGroup.getLeaderboards().iterator().next().getRaceColumnByName(columnName);
        Fleet loadedGroupFleet = loadedRaceColumnFromGroupByName.getFleetByName(fleet.getName());
        RaceIdentifier loadedIdentifier = loadedRaceColumnFromGroupByName.getRaceIdentifier(loadedGroupFleet);
        Assertions.assertEquals(race.getRaceIdentifier(fleet), loadedIdentifier);
    }
}
