package com.sap.sailing.server.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoRaceLogStoreVisitor;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.AddDefaultRegatta;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.TrackRegatta;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;

public class RaceColumnReloadTest {

    private MongoRaceLogStoreVisitor mongoStoreVisitor;
    private RaceColumn raceColumn;
    private RaceLog raceLog;
    private DynamicTrackedRace trackedRace;
    private RaceLogWindFixEventImpl testWindEvent1, testWindEvent2;
    private Fleet defaultFleet;

    @BeforeEach
    public void setUp() {
        MongoDBService.INSTANCE.getDB().drop();
        final RacingEventServiceImpl service = new RacingEventServiceImpl();
        // FIXME use master DomainFactory; see bug 592
        final DomainFactory masterDomainFactory = service.getBaseDomainFactory();
        final MongoObjectFactory objectFactory = new MongoObjectFactoryImpl(MongoDBService.INSTANCE.getDB());
        final String leaderboardName = "Test Leaderboard", boatClassName = "49er", raceName = "Test Race";
        BoatClass boatClass = masterDomainFactory.getOrCreateBoatClass(boatClassName, /* typicallyStartsUpwind */true);
        PersonImpl sailor = new PersonImpl("Sailor", DomainFactory.INSTANCE.getOrCreateNationality("GER"), null, null);
        PersonImpl coach = new PersonImpl("Coach", DomainFactory.INSTANCE.getOrCreateNationality("NED"), null, null);
        Boat boat = new BoatImpl("61", "GER 61", boatClass, "GER 61");
        CompetitorWithBoat comp = masterDomainFactory.getOrCreateCompetitorWithBoat("GER 61", "Team", "T", Color.RED, "noone@nowhere.de",
                null, new TeamImpl("Team", Arrays.asList(sailor), coach),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, (DynamicBoat) boat, /* storePersistently */ true);
        service.apply(new CreateFlexibleLeaderboard(leaderboardName, "Test", new int[] { 1, 2 }, new LowPoint(), null));
        raceColumn = service.apply(new AddColumnToLeaderboard("R1", leaderboardName, false));
        Regatta regatta = service.apply(new AddDefaultRegatta(RegattaImpl.getDefaultName("Test Event", boatClassName),
                boatClassName, /* startDate */ null, /* endDate */ null, UUID.randomUUID()));
        final CourseImpl masterCourse = new CourseImpl("Test Course", new ArrayList<Waypoint>());
        final Map<Competitor, Boat> compAndBoats = new HashMap<>();
        compAndBoats.put(comp, comp.getBoat());
        final RaceDefinition race = new RaceDefinitionImpl(raceName, masterCourse, boatClass, compAndBoats);
        service.apply(new AddRaceDefinition(new RegattaName(regatta.getName()), race));
        masterCourse.addWaypoint(0, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark1"),
                /* passingInstruction */ null));
        final RegattaNameAndRaceName raceIdentifier = new RegattaNameAndRaceName(regatta.getName(), raceName);
        service.apply(new TrackRegatta(raceIdentifier));
        trackedRace = service.apply(new CreateTrackedRace(raceIdentifier, EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 5000, /* millisecondsOverWhichToAverageWind */ 10000,
                /* millisecondsOverWhichToAverageSpeed */10000, null));
        trackedRace.setStartOfTrackingReceived(MillisecondsTimePoint.now());
        defaultFleet = Util.get(raceColumn.getFleets(), 0);
        final RaceLogIdentifier raceLogIdentifier = raceColumn.getRaceLogIdentifier(defaultFleet);
        raceLog = raceColumn.getRaceLog(defaultFleet);
        trackedRace.attachRaceLog(raceLog);
        mongoStoreVisitor = new MongoRaceLogStoreVisitor(raceLogIdentifier, objectFactory);
        final TimePoint t1 = MillisecondsTimePoint.now(), t2 = MillisecondsTimePoint.now().plus(1000);
        final SpeedWithBearing s1 = new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(10));
        final SpeedWithBearing s2 = new KnotSpeedWithBearingImpl(2, new DegreeBearingImpl(20));
        final Position p1 = new DegreePosition(1, 1), p2 = new DegreePosition(2, 2);
        final AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
        testWindEvent1 = new RaceLogWindFixEventImpl(t1, author, 0, new WindImpl(p1, t1, s1), false);
        testWindEvent2 = new RaceLogWindFixEventImpl(t2, author, 0, new WindImpl(p2, t2, s2), false);
    }

    @Test
    public void testWindAddedOnlyViaDB() throws InterruptedException {
        final WindFixLoggingRaceLogVisitor raceLogVisitor = new WindFixLoggingRaceLogVisitor(raceLog);
        final WindFixLoggingRaceChangeListener trackedRaceListener = new WindFixLoggingRaceChangeListener(trackedRace);
        mongoStoreVisitor.visit(testWindEvent1);
        mongoStoreVisitor.visit(testWindEvent2);
        raceLogVisitor.assertWindFixCount(0);
        trackedRaceListener.assertWindFixCount(0);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLogVisitor.assertWindFixCount(2);
        trackedRaceListener.assertWindFixCount(2);
    }

    @Test
    public void testWindAddedAndDbWithDifferentAddedReloadWithTrackedRace() throws InterruptedException {
        final WindFixLoggingRaceLogVisitor raceLogVisitor = new WindFixLoggingRaceLogVisitor(raceLog);
        final WindFixLoggingRaceChangeListener trackedRaceListener = new WindFixLoggingRaceChangeListener(trackedRace);
        raceLog.add(testWindEvent1);
        raceLogVisitor.assertWindFixCount(1);
        trackedRaceListener.assertWindFixCount(1);
        mongoStoreVisitor.visit(testWindEvent2);
        raceLogVisitor.assertWindFixCount(1);
        trackedRaceListener.assertWindFixCount(1);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLogVisitor.assertWindFixCount(2);
        trackedRaceListener.assertWindFixCount(2);
    }

    @Test
    public void testAddedWind() throws InterruptedException {
        final WindFixLoggingRaceLogVisitor raceLogVisitor = new WindFixLoggingRaceLogVisitor(raceLog);
        raceLog.add(testWindEvent1);
        raceLogVisitor.assertWindFixCount(1);
    }

    @Test
    public void testAddedWindAndReloadAndTheSameWindMerged() throws InterruptedException {
        final WindFixLoggingRaceLogVisitor raceLogVisitor = new WindFixLoggingRaceLogVisitor(raceLog);
        raceLog.add(testWindEvent1);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLog.add(testWindEvent1);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLogVisitor.assertWindFixCount(1);
    }

    @Test
    public void testAddedWindAndReloadAndAddAnotherAndReloadAgain() throws InterruptedException {
        final WindFixLoggingRaceLogVisitor raceLogVisitor = new WindFixLoggingRaceLogVisitor(raceLog);
        raceLog.add(testWindEvent1);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLog.add(testWindEvent2);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLogVisitor.assertWindFixCount(2);
    }

    @Test
    public void testWindAddedAndDbWithSameAndReload() throws InterruptedException {
        final WindFixLoggingRaceLogVisitor raceLogVisitor = new WindFixLoggingRaceLogVisitor(raceLog);
        raceLog.add(testWindEvent1);
        raceLogVisitor.assertWindFixCount(1);
        mongoStoreVisitor.visit(testWindEvent1);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLogVisitor.assertWindFixCount(1);
    }

    @Test
    public void testWindAddedAndDbWithDifferentAndReload() throws InterruptedException {
        final WindFixLoggingRaceLogVisitor raceLogVisitor = new WindFixLoggingRaceLogVisitor(raceLog);
        raceLog.add(testWindEvent1);
        raceLogVisitor.assertWindFixCount(1);
        mongoStoreVisitor.visit(testWindEvent2);
        raceLogVisitor.assertWindFixCount(1);
        raceColumn.reloadRaceLog(defaultFleet);
        raceLogVisitor.assertWindFixCount(2);
    }

    private class WindFixLoggingRaceLogVisitor extends BaseRaceLogEventVisitor {
        private final Set<Wind> loggedWindFixes = ConcurrentHashMap.newKeySet();

        private WindFixLoggingRaceLogVisitor(RaceLog raceLog) {
            raceLog.addListener(this);
        }

        @Override
        public void visit(RaceLogWindFixEvent event) {
            this.loggedWindFixes.add(event.getWindFix());
        }

        private void assertWindFixCount(int expected) {
            Assertions.assertEquals(expected, loggedWindFixes.size());
        }
    }

    private class WindFixLoggingRaceChangeListener extends AbstractRaceChangeListener {

        private final Set<Wind> loggedWindFixes = ConcurrentHashMap.newKeySet();

        private WindFixLoggingRaceChangeListener(TrackedRace trackedRace) {
            trackedRace.addListener(this);
        }

        @Override
        public void windDataReceived(Wind wind, WindSource windSource) {
            if (WindSourceType.RACECOMMITTEE == windSource.getType()) {
                this.loggedWindFixes.add(wind);
            }
        }

        private void assertWindFixCount(int expected) {
            Assertions.assertEquals(expected, loggedWindFixes.size());
        }
    }
    
}
