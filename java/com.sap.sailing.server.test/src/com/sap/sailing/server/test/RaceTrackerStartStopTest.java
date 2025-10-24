package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.testsupport.RacingEventServiceImplMock;
import com.sap.sse.common.Util;

public class RaceTrackerStartStopTest {

    private static final String RACENAME3 = "racedef3";
    private static final String RACENAME2 = "racedef2";
    private static final String RACENAME1 = "racedef1";
    private final static String EVENTNAME = "TESTEVENT";
    private final static String BOATCLASSNAME = "HAPPYBOATCLASS";

    private RacingEventServiceImplMock racingEventService;
    private Regatta regatta;
    private BoatClass boatClass;
    private Set<RaceTracker> raceTrackerSet = Collections.newSetFromMap(new ConcurrentHashMap<RaceTracker, Boolean>());

    private RaceDefinition raceDef1;
    private RaceDefinition raceDef2;
    private RaceDefinition raceDef3;

    private RaceTrackerMock raceTracker1;
    private RaceTrackerMock raceTracker2;
    private RaceTrackerMock raceTracker3;

    @BeforeEach
    public void setUp() {
        racingEventService = new RacingEventServiceImplMock(){};
        boatClass = new BoatClassImpl(BOATCLASSNAME, /* typicallyStartsUpwind */ true);
        regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName(EVENTNAME, boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */
                null, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), UUID.randomUUID(), null,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        racingEventService.getRegattasByName().put(EVENTNAME, regatta);
        TrackedRegatta trackedRegatta1 = racingEventService.getOrCreateTrackedRegatta(regatta);
        racingEventService.getRegattasByName().put(EVENTNAME, regatta);
        raceTrackerSet = Collections.newSetFromMap(new ConcurrentHashMap<RaceTracker, Boolean>());
        raceDef1 = new RaceDefinitionImpl(RACENAME1, new CourseImpl("Course1", new ArrayList<Waypoint>()), boatClass, Collections.<Competitor, Boat>emptyMap());
        raceDef2 = new RaceDefinitionImpl(RACENAME2, new CourseImpl("Course2", new ArrayList<Waypoint>()), boatClass, Collections.<Competitor, Boat>emptyMap());
        raceDef3 = new RaceDefinitionImpl(RACENAME3, new CourseImpl("Course3", new ArrayList<Waypoint>()), boatClass, Collections.<Competitor, Boat>emptyMap());
        regatta.addRace(raceDef1);
        trackedRegatta1.createTrackedRace(raceDef1, Collections.<Sideline> emptyList(),
                /* windStore */ EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l,
                /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null, /*useMarkPassingCalculator*/ false, mock(RaceLogAndTrackedRaceResolver.class),
                Optional.empty(), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        regatta.addRace(raceDef2);
        trackedRegatta1.createTrackedRace(raceDef2, Collections.<Sideline> emptyList(),
                /* windStore */ EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l,
                /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null, /*useMarkPassingCalculator*/ false, mock(RaceLogAndTrackedRaceResolver.class),
                Optional.empty(), null, /* markPassingRaceFingerprintRegistry */ null);
        regatta.addRace(raceDef3);
        trackedRegatta1.createTrackedRace(raceDef3, Collections.<Sideline> emptyList(),
                /* windStore */ EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l,
                /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null, /*useMarkPassingCalculator*/ false, mock(RaceLogAndTrackedRaceResolver.class),
                Optional.empty(), null, /* markPassingRaceFingerprintRegistry */ null);
        Long trackerID1 = Long.valueOf(1);
        Long trackerID2 = Long.valueOf(2);
        Long trackerID3 = Long.valueOf(3);
        raceTracker1 = new RaceTrackerMock(Long.valueOf(1), regatta, raceDef1, true);
        raceTracker2 = new RaceTrackerMock(Long.valueOf(2), regatta, raceDef2, true);
        raceTracker3 = new RaceTrackerMock(Long.valueOf(3), regatta, raceDef3, true);
        raceTrackerSet.add(raceTracker1);
        raceTrackerSet.add(raceTracker2);
        raceTrackerSet.add(raceTracker3);
        racingEventService.getRaceTrackersByRegattaMap().put(regatta, raceTrackerSet);
        racingEventService.getRaceTrackersByIDMap().put(trackerID1, raceTracker1);
        racingEventService.getRaceTrackersByIDMap().put(trackerID2, raceTracker2);
        racingEventService.getRaceTrackersByIDMap().put(trackerID3, raceTracker3);
    }

    /**
     * This test method tests if the {@link RacingEventService#stopTracking(Regatta, RaceDefinition) stopTracking} method works correctly.
     */
    @Test
    public void testStopTrackingRace() throws MalformedURLException, IOException, InterruptedException {
        Regatta regatta = racingEventService.getRegattaByName(EVENTNAME);
        TrackedRegatta trackedRegatta = racingEventService.getTrackedRegatta(regatta);
        assertNotNull(regatta.getRaceByName(RACENAME2));
        assertNotNull(trackedRegatta.getExistingTrackedRace(regatta.getRaceByName(RACENAME2)));
        racingEventService.stopTracking(regatta, raceDef2);
        // the raceDef2 should still be part of the event, and the corresponding tracked race should still be part
        // of the tracked event
        assertNotNull(regatta.getRaceByName(RACENAME2));
        boolean foundTrackedRaceForRaceDef2 = false;
        trackedRegatta.lockTrackedRacesForRead();
        try {
            for (TrackedRace trackedRace : trackedRegatta.getTrackedRaces()) {
                if (trackedRace.getRace().getName().equals(RACENAME2)) {
                    foundTrackedRaceForRaceDef2 = true;
                }
            }
        } finally {
            trackedRegatta.unlockTrackedRacesAfterRead();
        }
        assertTrue(foundTrackedRaceForRaceDef2);
        // The raceTracker2 should no longer be in track mode: 
        assertTrue(raceTracker1.getIsTracking());
        assertFalse(raceTracker2.getIsTracking());
        assertTrue(raceTracker3.getIsTracking());
        // The RaceTrackersByID map should not contain the trackers raceTracker2 and raceTracker3 anymore
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker3));
        // The RaceTrackersByEvent map should contain a tracker with a set of RaceDefinitions, containing the
        // raceDefinition1
        assertEquals(1, racingEventService.getRaceTrackersByRegattaMap().size());
        Iterator<RaceTracker> raceTrackerIter = racingEventService.getRaceTrackersByRegattaMap().get(regatta).iterator();
        while (raceTrackerIter.hasNext()) {
            RaceTracker currentTracker = raceTrackerIter.next();
            assertTrue(Util.contains(Arrays.asList(raceTracker1, raceTracker3), currentTracker));
        }
    }
    
    /**
     * This test methods checks if the {@link RacingEventService#removeRace(Regatta, RaceDefinition) removeRace} method works correctly
     */
    @Test
    public void testRemoveRace() throws MalformedURLException, IOException, InterruptedException {
        Regatta regatta = racingEventService.getRegattaByName(EVENTNAME);
        TrackedRegatta trackedRegatta = racingEventService.getTrackedRegatta(regatta);
        assertNotNull(regatta.getRaceByName(RACENAME2));
        assertNotNull(trackedRegatta.getExistingTrackedRace(regatta.getRaceByName(RACENAME2)));
        racingEventService.removeRace(regatta, raceDef2);
        // the raceDef2 should be removed from the event, and the corresponding tracked race should be removed
        // from the tracked event
        assertNull(regatta.getRaceByName(RACENAME2));
        boolean foundTrackedRaceForRaceDef2 = false;
        trackedRegatta.lockTrackedRacesForRead();
        try {
            for (TrackedRace trackedRace : trackedRegatta.getTrackedRaces()) {
                if (trackedRace.getRace().getName().equals(RACENAME2)) {
                    foundTrackedRaceForRaceDef2 = true;
                }
            }
        } finally {
            trackedRegatta.unlockTrackedRacesAfterRead();
        }
        assertFalse(foundTrackedRaceForRaceDef2);
        // The trackers map should still contain the raceTrackers, except for raceTracker2 which should
        // have been removed because the race named RACENAME2 was removed, hopefully together with its tracker...
        assertTrue(racingEventService.getRaceTrackersByRegattaMap().get(regatta).contains(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByRegattaMap().get(regatta).contains(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByRegattaMap().get(regatta).contains(raceTracker3));
        // The raceTrackerMap should still contain the raceTrackers, except the one for race 2 which was removed.
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker3));
        // The raceTracker should still exist; it shall still contain raceDef1 and raceDef2 because a tracker keeps tracking what it tracks...
        assertSame(raceTracker1.getRace(), raceDef1);
        assertSame(raceTracker2.getRace(), raceDef2);
        assertSame(raceTracker3.getRace(), raceDef3);
    }
    
    /**
     * This test methods checks if the {@link RacingEventService#removeRace(Regatta, RaceDefinition) removeRace} method works correctly if the
     * race to be stopped is the last race of a tracker
     */
    @Test
    public void testRemoveLastRaceOfTracker() throws MalformedURLException, IOException, InterruptedException {
        racingEventService.removeRace(regatta, raceDef1);
        racingEventService.removeRace(regatta, raceDef2);
        // The event map should still contain the raceTrackers except of raceTracker1 and raceTracker2
        assertFalse(racingEventService.getRaceTrackersByRegattaMap().get(regatta).contains(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByRegattaMap().get(regatta).contains(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByRegattaMap().get(regatta).contains(raceTracker3));
        // The RaceTrackerByID map should still contain raceTracker3, but not raceTracker1 and raceTracker2 anymore
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker3));
        // The raceTracker 3 should exist, and it should contain all race definitions still
        assertSame(raceTracker3.getRace(), raceDef3);
    }

}
