package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPointExtremeSailingSeriesOverall;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.impl.ScoreCorrectionImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithFixedRankAndManyCompetitors;
import com.sap.sailing.mongodb.MongoDBService;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.impl.RacingEventServiceImpl;

public class TestStoringAndRetrievingLeaderboardGroups extends AbstractMongoDBTest {
    private MongoObjectFactory mongoObjectFactory = null;
    private DomainObjectFactory domainObjectFactory = null;
    
    
    public TestStoringAndRetrievingLeaderboardGroups() throws UnknownHostException, MongoException {
        super();
    }

    @Before
    public void setUp() {
        mongoObjectFactory = new MongoObjectFactoryImpl(db);
        domainObjectFactory = new DomainObjectFactoryImpl(db);
    }
    
    @Test
    public void testStoringAndRetrievingLeaderboardGroupWithOverallLeaderboard() {
        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        
        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        
        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, false, leaderboards);
        final Leaderboard overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup,
                new HighPointExtremeSailingSeriesOverall(), new ResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup.setOverallLeaderboard(overallLeaderboard);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);
        
        final LeaderboardGroup loadedLeaderboardGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null,
                /* leaderboardRegistry */ null);
        assertNotNull(loadedLeaderboardGroup.getOverallLeaderboard());
        assertNotSame(leaderboardGroup.getOverallLeaderboard(), loadedLeaderboardGroup.getOverallLeaderboard());
        assertSame(ScoringSchemeType.HIGH_POINT_ESS_OVERALL, loadedLeaderboardGroup.getOverallLeaderboard().getScoringScheme().getType());
    }
    
    @Test
    public void testStoringAndRetrievingLeaderboardGroupWithSuppressedCompetitorsInOverallLeaderboard() {
        Competitor wolfgang = new CompetitorImpl(123, "$$$Dr. Wolfgang+Hunger$$$", new TeamImpl("STG", Collections.singleton(
                new PersonImpl("$$$Dr. Wolfgang+Hunger$$$", new NationalityImpl("GER"),
                /* dateOfBirth */ null, "This is famous Dr. Wolfgang Hunger")), new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                        /* dateOfBirth */ null, "This is Rigo, the coach")), new BoatImpl("Dr. Wolfgang Hunger's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null));
        Competitor hasso = new CompetitorImpl(123, "Hasso Plattner", new TeamImpl("STG", Collections.singleton(
                new PersonImpl("Hasso Plattner", new NationalityImpl("GER"),
                /* dateOfBirth */ null, "This is famous Dr. Hasso Plattner")), new PersonImpl("Lutz Patrunky", new NationalityImpl("GER"),
                        /* dateOfBirth */ null, "This is Patty, the coach")),
                        new BoatImpl("Dr. Hasso Plattner's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null));
        final String raceColumnName1 = "My First Race 1";
        MockedTrackedRaceWithFixedRankAndManyCompetitors raceWithTwoCompetitors = new MockedTrackedRaceWithFixedRankAndManyCompetitors(wolfgang, /* rank */ 1, /* started */ true);
        raceWithTwoCompetitors.addCompetitor(hasso);

        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        
        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard.addRace(raceWithTwoCompetitors, raceColumnName1, /* medalRace */ false, leaderboard.getFleet(null));
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        
        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, false, leaderboards);
        final Leaderboard overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup,
                new HighPointExtremeSailingSeriesOverall(), new ResultDiscardingRuleImpl(new int[0]));
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
        Competitor wolfgang = new CompetitorImpl(123, "$$$Dr. Wolfgang+Hunger$$$", new TeamImpl("STG", Collections.singleton(
                new PersonImpl("$$$Dr. Wolfgang+Hunger$$$", new NationalityImpl("GER"),
                /* dateOfBirth */ null, "This is famous Dr. Wolfgang Hunger")), new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                        /* dateOfBirth */ null, "This is Rigo, the coach")), new BoatImpl("Dr. Wolfgang Hunger's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null));
        Competitor hasso = new CompetitorImpl(123, "Hasso Plattner", new TeamImpl("STG", Collections.singleton(
                new PersonImpl("Hasso Plattner", new NationalityImpl("GER"),
                /* dateOfBirth */ null, "This is famous Dr. Hasso Plattner")), new PersonImpl("Lutz Patrunky", new NationalityImpl("GER"),
                        /* dateOfBirth */ null, "This is Patty, the coach")),
                        new BoatImpl("Dr. Hasso Plattner's boat", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null));
        final String raceColumnName1 = "My First Race 1";
        MockedTrackedRaceWithFixedRankAndManyCompetitors raceWithTwoCompetitors = new MockedTrackedRaceWithFixedRankAndManyCompetitors(wolfgang, /* rank */ 1, /* started */ true);
        raceWithTwoCompetitors.addCompetitor(hasso);

        final String[] leaderboardNames = {"Leaderboard 0", "Leaderboard 1", "Leaderboard 2", "Leaderboard 3"};
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        
        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard.addRace(raceWithTwoCompetitors, raceColumnName1, /* medalRace */ false, leaderboard.getFleet(null));
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        
        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, false, leaderboards);
        final Leaderboard overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup,
                new HighPointExtremeSailingSeriesOverall(), new ResultDiscardingRuleImpl(new int[0]));
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
        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards1.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards1.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards1.add(leaderboard);
        final LeaderboardGroup leaderboardGroup1 = new LeaderboardGroupImpl(groupName1, groupDescription1, false, leaderboards1);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup1);
        
        final String groupName2 = "Leaderboard Group 2";
        final String groupDescription2= "A leaderboard group 2";
        final ArrayList<Leaderboard> leaderboards2 = new ArrayList<>();
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards2.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards2.add(leaderboard);
        final LeaderboardGroup leaderboardGroup2 = new LeaderboardGroupImpl(groupName2, groupDescription2, false, leaderboards2);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup2);
        
        // the leaderboard named leaderboardNames[2] occurs in both groups
        RacingEventService racingEventService = new RacingEventServiceImpl(MongoDBService.INSTANCE); // expected to load leaderboard groups
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
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();

        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[2], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[3], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        
        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, false, leaderboards);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);
        
        final LeaderboardGroup loadedLeaderboardGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null,
                /* leaderboardRegistry */ null);

        Assert.assertEquals(groupName, loadedLeaderboardGroup.getName());
        Assert.assertEquals(groupDescription, loadedLeaderboardGroup.getDescription());
        
        int c = 0;
        for (Leaderboard board : leaderboardGroup.getLeaderboards()) {
            Assert.assertEquals(leaderboardNames[c], board.getName());
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
        
        Leaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[0], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        leaderboard = new FlexibleLeaderboardImpl(leaderboardNames[1], new ScoreCorrectionImpl(),
                new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        leaderboards.add(leaderboard);
        
        final LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl(groupName, groupDescription, false, leaderboards);
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);

        final String[] ungroupedLeaderboardNames = {"Ungrouped Leaderboard 0", "Ungrouped Leaderboard 1", "Ungrouped Leaderboard 2"};

        final FlexibleLeaderboard[] ungroupedLeaderboards = {
                new FlexibleLeaderboardImpl(ungroupedLeaderboardNames[0], new ScoreCorrectionImpl(),
                        new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint()),
                new FlexibleLeaderboardImpl(ungroupedLeaderboardNames[1], new ScoreCorrectionImpl(),
                        new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint()),
                new FlexibleLeaderboardImpl(ungroupedLeaderboardNames[2], new ScoreCorrectionImpl(),
                        new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint()) };
        mongoObjectFactory.storeLeaderboard(ungroupedLeaderboards[0]);
        mongoObjectFactory.storeLeaderboard(ungroupedLeaderboards[1]);
        mongoObjectFactory.storeLeaderboard(ungroupedLeaderboards[2]);
        
        Iterable<Leaderboard> loadedUngroupedLeaderboards = domainObjectFactory.getLeaderboardsNotInGroup(/* regattaRegistry */ null,
                /* leaderboardRegistry */ null);
        
        Assert.assertTrue(loadedUngroupedLeaderboards.iterator().hasNext());
        
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
            Assert.assertTrue(loadedBoardsContainsName);
        }
        
        Assert.assertTrue(c == ungroupedLeaderboards.length);
    }
    
    @Test
    public void testLeaderboardReferenceBreak() {
        //Set up
        final String leaderboardName = "Leaderboard 0";
        final String columnName = "Column";
        final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
        
        final String groupName = "Leaderboard Group";
        final String groupDescription = "A leaderboard group";
        final ArrayList<Leaderboard> leaderboards = new ArrayList<>();
        
        final FlexibleLeaderboard leaderboard = new FlexibleLeaderboardImpl(leaderboardName, new ScoreCorrectionImpl(), new ResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces), new LowPoint());
        final Fleet fleet = leaderboard.getFleet(null);
        final RaceColumn race = leaderboard.addRaceColumn(columnName, false, fleet);
        leaderboards.add(leaderboard);
        
        final LeaderboardGroup group = new LeaderboardGroupImpl(groupName, groupDescription, false, leaderboards);
        mongoObjectFactory.storeLeaderboardGroup(group);
        
        //Name change test
        final String newLeaderboardName = "Leaderboard ChangedName";
        mongoObjectFactory.renameLeaderboard(leaderboardName, newLeaderboardName);
        leaderboard.setName(newLeaderboardName);
        
        LeaderboardGroup loadedGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null, /* leaderboardRegistry */ null);
        String loadedLeaderboardName = loadedGroup.getLeaderboards().iterator().next().getName();
        Assert.assertEquals(newLeaderboardName, loadedLeaderboardName);
        
        //RaceIdentifier change test
        final String regattaName = "Event";
        final String raceName = "Race";
        leaderboard.getRaceColumnByName(columnName).setRaceIdentifier(fleet, new RegattaNameAndRaceName(regattaName, raceName));
        mongoObjectFactory.storeLeaderboard(leaderboard);
        
        //Check if the leaderboard updated correctly
        final Leaderboard loadedLeaderboard = domainObjectFactory.loadLeaderboard(leaderboard.getName(), /* regattaRegistry */ null);
        final RaceColumn loadedRaceColumnByName = loadedLeaderboard.getRaceColumnByName(columnName);
        Fleet loadedFleet = loadedRaceColumnByName.getFleetByName(fleet.getName());
        Assert.assertEquals(race.getRaceIdentifier(fleet), loadedRaceColumnByName.getRaceIdentifier(loadedFleet));
        
        // Check if the group received the changes
        loadedGroup = domainObjectFactory.loadLeaderboardGroup(groupName, /* regattaRegistry */ null, /* leaderboardRegistry */ null);
        final RaceColumn loadedRaceColumnFromGroupByName = loadedGroup.getLeaderboards().iterator().next().getRaceColumnByName(columnName);
        Fleet loadedGroupFleet = loadedRaceColumnFromGroupByName.getFleetByName(fleet.getName());
        RaceIdentifier loadedIdentifier = loadedRaceColumnFromGroupByName.getRaceIdentifier(loadedGroupFleet);
        Assert.assertEquals(race.getRaceIdentifier(fleet), loadedIdentifier);
    }

}
