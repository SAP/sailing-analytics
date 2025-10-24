package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveEvent;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sse.common.Util;

public class RemoveLeaderboardTest {
    private static final String R1 = "R1";
    private RacingEventServiceImpl server;
    private static final String LEADERBOARD_NAME = "A Flexible Leaderboard";
    private static final String RACENAME1 = "racedef1";
    private final static String EVENTNAME = "TESTEVENT";
    private final static String BOATCLASSNAME = "HAPPYBOATCLASS";
    private final static String SERIES_NAME = "Opening";

    private Regatta regatta;
    private BoatClass boatClass;
    private RaceDefinition raceDef1;
    private DynamicTrackedRace trackedRace;
    private Fleet defaultFleet;
    private FlexibleLeaderboard leaderboard;

    @BeforeEach
    public void setUp() {
        server = new RacingEventServiceImpl();
        List<Event> allEvents = new ArrayList<>();
        Util.addAll(server.getAllEvents(), allEvents);
        for (final Event e : allEvents) {
            server.apply(new RemoveEvent(e.getId()));
        }
        Map<String, Leaderboard> allLeaderboards = new HashMap<>(server.getLeaderboards());
        for (final String leaderboardName : allLeaderboards.keySet()) {
            server.apply(new RemoveLeaderboard(leaderboardName));
        }
        Map<UUID, LeaderboardGroup> allLeaderboardGroups = new HashMap<>(server.getLeaderboardGroups());
        for (final LeaderboardGroup leaderboardGroup : allLeaderboardGroups.values()) {
            server.apply(new RemoveLeaderboardGroup(leaderboardGroup.getId()));
        }
        leaderboard = server.apply(
                new CreateFlexibleLeaderboard(LEADERBOARD_NAME, /* display name */ null, new int[0], new LowPoint(), /* default course area ID */ null));
        leaderboard.addRaceColumn(R1, /* medalRace */ false);
        boatClass = new BoatClassImpl(BOATCLASSNAME, /* typicallyStartsUpwind */ true);
        regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName(EVENTNAME, boatClass.getName()), boatClass, /* canBoatsOfCompetitorsChangePerRace*/ true,  CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */
                null, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), UUID.randomUUID(), null,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        TrackedRegatta trackedRegatta1 = server.getOrCreateTrackedRegatta(regatta);
        defaultFleet = new FleetImpl("Default");
        regatta.addSeries(new SeriesImpl(SERIES_NAME, /* is medal */ false, /* can fleets run in parallel */ true,
                Collections.singleton(defaultFleet), Collections.singleton(R1),
                /* regatta registry */ server));
        raceDef1 = new RaceDefinitionImpl(RACENAME1, new CourseImpl("Course1", new ArrayList<Waypoint>()), boatClass, new HashMap<>());
        regatta.addRace(raceDef1);
        trackedRace = trackedRegatta1.createTrackedRace(raceDef1, Collections.<Sideline> emptyList(),
                /* windStore */ EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l,
                /* millisecondsOverWhichToAverageWind */ 0l, /* millisecondsOverWhichToAverageSpeed */ 0l,
                /* raceDefinitionSetToUpdate */ null, /* useMarkPassingCalculator */ false,
                mock(RaceLogAndTrackedRaceResolver.class), Optional.empty(), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
    }
    
    @Test
    public void testRaceLogOfRaceColumnInSeriesOfRegattaIsAttached() {
        regatta.getRaceColumnByName(R1).setTrackedRace(defaultFleet, trackedRace);
        assertEquals(1, Util.size(trackedRace.getAttachedRaceLogs()));
        RaceLog raceLog = trackedRace.getAttachedRaceLogs().iterator().next();
        assertSame(regatta.getRaceColumnByName(R1).getRaceLog(defaultFleet), raceLog);
    }
    
    @Test
    public void testRaceLogOfRaceColumnInSeriesDetachedWhenRegattaIsRemoved() {
        regatta.getRaceColumnByName(R1).setTrackedRace(defaultFleet, trackedRace);
        assertEquals(1, Util.size(trackedRace.getAttachedRaceLogs()));
        server.apply(new RemoveRegatta(new RegattaName(regatta.getName())));
        assertTrue(Util.isEmpty(trackedRace.getAttachedRaceLogs()));
    }
    
    @Test
    public void testRaceLogOfRaceColumnInFlexibleLeaderboardIsAttached() {
        leaderboard.getRaceColumnByName(R1).setTrackedRace(leaderboard.getRaceColumnByName(R1).getFleets().iterator().next(), trackedRace);
        assertEquals(1, Util.size(trackedRace.getAttachedRaceLogs()));
        RaceLog raceLog = trackedRace.getAttachedRaceLogs().iterator().next();
        assertSame(leaderboard.getRaceColumnByName(R1).getRaceLog(leaderboard.getRaceColumnByName(R1).getFleets().iterator().next()), raceLog);
    }
    
    @Test
    public void testRemovingFlexibleLeaderboardDetachesAllRaceLogsFromTrackedRaces() {
        leaderboard.getRaceColumnByName(R1).setTrackedRace(leaderboard.getRaceColumnByName(R1).getFleets().iterator().next(), trackedRace);
        assertEquals(1, Util.size(trackedRace.getAttachedRaceLogs()));
        server.apply(new RemoveLeaderboard(LEADERBOARD_NAME));
        assertTrue(Util.isEmpty(trackedRace.getAttachedRaceLogs()));
    }
}
