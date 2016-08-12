package com.sap.sailing.server.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.AddDefaultRegatta;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.ConnectTrackedRaceToLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.DisconnectLeaderboardColumnFromTrackedRace;
import com.sap.sailing.server.operationaltransformation.TrackRegatta;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class WindByRaceLogTest {
    
    private DynamicTrackedRace trackedRace;
    private RegattaNameAndRaceName raceIdentifier;
    
    private RaceColumn raceColumn;
    private Fleet defaultFleet;
    
    private RacingEventService service;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    @Before
    public void setup() throws Exception {
        this.service = new RacingEventServiceImpl();
        
        final String boatClassName = "49er";
        // FIXME use master DomainFactory; see bug 592
        final DomainFactory masterDomainFactory = service.getBaseDomainFactory();
        BoatClass boatClass = masterDomainFactory.getOrCreateBoatClass(boatClassName, /* typicallyStartsUpwind */true);
        Competitor competitor = createCompetitor(masterDomainFactory);
        int[] discardThreshold = {1, 2};
        CreateFlexibleLeaderboard createLeaderboardOperation = new CreateFlexibleLeaderboard("Test Leaderboard", "Test", discardThreshold, new LowPoint(), null);
        service.apply(createLeaderboardOperation);
        AddColumnToLeaderboard leaderboardColumnOperation = new AddColumnToLeaderboard("R1", "Test Leaderboard", false);
        raceColumn = service.apply(leaderboardColumnOperation);
        
        final String baseRegattaName = "Test Event";
        AddDefaultRegatta addRegattaOperation = new AddDefaultRegatta(RegattaImpl.getDefaultName(baseRegattaName, boatClassName), boatClassName, 
                /*startDate*/ null, /*endDate*/ null, UUID.randomUUID());
        Regatta regatta = service.apply(addRegattaOperation);
        final String raceName = "Test Race";
        final CourseImpl masterCourse = new CourseImpl("Test Course", new ArrayList<Waypoint>());
        RaceDefinition race = new RaceDefinitionImpl(raceName, masterCourse, boatClass, Collections.singletonList(competitor));
        AddRaceDefinition addRaceOperation = new AddRaceDefinition(new RegattaName(regatta.getName()), race);
        service.apply(addRaceOperation);
        masterCourse.addWaypoint(0, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark1"), /*passingInstruction*/ null));
        raceIdentifier = new RegattaNameAndRaceName(regatta.getName(), raceName);
        service.apply(new TrackRegatta(raceIdentifier));
        trackedRace = (DynamicTrackedRace) service.apply(new CreateTrackedRace(raceIdentifier, EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 5000,
                /* millisecondsOverWhichToAverageWind */ 10000, /* millisecondsOverWhichToAverageSpeed */10000));
        trackedRace.setStartOfTrackingReceived(MillisecondsTimePoint.now());
        defaultFleet = Util.get(raceColumn.getFleets(), 0);
    }

    private Competitor createCompetitor(final DomainFactory masterDomainFactory) {
        return masterDomainFactory.getOrCreateCompetitor("GER 61", "Sailor", "S", Color.RED, "noone@nowhere.de", null, new TeamImpl("Sailor",
                (List<PersonImpl>) Arrays.asList(new PersonImpl[] { new PersonImpl("Sailor 1", DomainFactory.INSTANCE.getOrCreateNationality("GER"), null, null)}),
                new PersonImpl("Sailor 2", DomainFactory.INSTANCE.getOrCreateNationality("NED"), null, null)),
                new BoatImpl("123", "GER 61", DomainFactory.INSTANCE.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true), "GER 61"), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null);
    }
    
    private void attachTrackedRaceToRaceColumn() {
        ConnectTrackedRaceToLeaderboardColumn connectColumn = new ConnectTrackedRaceToLeaderboardColumn("Test Leaderboard", "R1", 
                defaultFleet.getName(), raceIdentifier);
        service.apply(connectColumn);
    }
    
    private void detachTrackedRaceFromRaceColumn() {
        DisconnectLeaderboardColumnFromTrackedRace disconnectColumn = new DisconnectLeaderboardColumnFromTrackedRace("Test Leaderboard", "R1", defaultFleet.getName());
        service.apply(disconnectColumn);
    }
    
    @Test
    public void testAttachingRaceLogContainingWindFixes() {
        RaceLog raceLog = raceColumn.getRaceLog(defaultFleet);
        TimePoint timeMinus3 = MillisecondsTimePoint.now().minus(20);
        TimePoint timeMinus2 = MillisecondsTimePoint.now().minus(10);
        
        Wind wind1 = new WindImpl(new DegreePosition(50, 4), timeMinus3,
                new KnotSpeedWithBearingImpl(15, new DegreeBearingImpl(234)));
        Wind wind2 = new WindImpl(new DegreePosition(49, 3), timeMinus2,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)));
        RaceLogWindFixEvent windEvent1 = new RaceLogWindFixEventImpl(timeMinus3, author, 0, wind1, /* isMagnetic */ false);
        RaceLogWindFixEvent windEvent2 = new RaceLogWindFixEventImpl(timeMinus2, author, 0, wind2, /* isMagnetic */ false);
        raceLog.add(windEvent1);
        raceLog.add(windEvent2);
        
        attachTrackedRaceToRaceColumn();
        
        WindSource source = new WindSourceImpl(WindSourceType.RACECOMMITTEE);
        assertTrue(Util.contains(trackedRace.getWindSources(), source));
        
        WindTrack windTrack = trackedRace.getOrCreateWindTrack(source);
        
        try {
            windTrack.lockForRead();
            assertTrue(Util.contains(windTrack.getFixes(), wind1));
            assertTrue(Util.contains(windTrack.getFixes(), wind2));
        } finally {
            windTrack.unlockAfterRead();
        }
    }
    
    @Test
    public void testAttachingRaceLogAndAddWindFixes() {
        RaceLog raceLog = raceColumn.getRaceLog(defaultFleet);
        TimePoint timeMinus3 = MillisecondsTimePoint.now().minus(20);
        
        attachTrackedRaceToRaceColumn();
        
        Wind wind1 = new WindImpl(new DegreePosition(50, 4), timeMinus3,
                new KnotSpeedWithBearingImpl(15, new DegreeBearingImpl(234)));
        RaceLogWindFixEvent windEvent1 = new RaceLogWindFixEventImpl(timeMinus3, author, 0, wind1, /* isMagnetic */ false);
        raceLog.add(windEvent1);
        
        WindSource source = new WindSourceImpl(WindSourceType.RACECOMMITTEE);
        assertTrue(Util.contains(trackedRace.getWindSources(), source));
        
        WindTrack windTrack = trackedRace.getOrCreateWindTrack(source);
        
        try {
            windTrack.lockForRead();
            assertTrue(Util.contains(windTrack.getFixes(), wind1));
        } finally {
            windTrack.unlockAfterRead();
        }
    }
    
    @Test
    public void testDetachingRaceLogWithWindFixes() {
        RaceLog raceLog = raceColumn.getRaceLog(defaultFleet);
        TimePoint timeMinus3 = MillisecondsTimePoint.now().minus(20);
        TimePoint timeMinus2 = MillisecondsTimePoint.now().minus(10);
        TimePoint timeMinus1 = MillisecondsTimePoint.now().minus(5);
        
        Wind wind1 = new WindImpl(new DegreePosition(50, 4), timeMinus3,
                new KnotSpeedWithBearingImpl(15, new DegreeBearingImpl(234)));
        Wind wind2 = new WindImpl(new DegreePosition(49, 3), timeMinus2,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)));
        Wind wind3 = new WindImpl(new DegreePosition(48, 1), timeMinus2,
                new KnotSpeedWithBearingImpl(18, new DegreeBearingImpl(270)));
        RaceLogWindFixEvent windEvent1 = new RaceLogWindFixEventImpl(timeMinus3, author, 0, wind1, /* isMagnetic */ false);
        RaceLogWindFixEvent windEvent2 = new RaceLogWindFixEventImpl(timeMinus2, author, 0, wind2, /* isMagnetic */ false);
        RaceLogWindFixEvent windEvent3 = new RaceLogWindFixEventImpl(timeMinus1, author, 0, wind3, /* isMagnetic */ false);
        raceLog.add(windEvent1);
        raceLog.add(windEvent2);
        
        attachTrackedRaceToRaceColumn();
        raceLog.add(windEvent3);
        detachTrackedRaceFromRaceColumn();
        
        WindSource source = new WindSourceImpl(WindSourceType.RACECOMMITTEE);
        assertTrue(Util.contains(trackedRace.getWindSources(), source));
        
        WindTrack windTrack = trackedRace.getOrCreateWindTrack(source);
        
        try {
            windTrack.lockForRead();
            assertFalse(Util.contains(windTrack.getFixes(), wind1));
            assertFalse(Util.contains(windTrack.getFixes(), wind2));
            assertFalse(Util.contains(windTrack.getFixes(), wind3));
        } finally {
            windTrack.unlockAfterRead();
        }
    }

}
