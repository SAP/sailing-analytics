package com.sap.sailing.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

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
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.RacingEventService;

public class RaceTrackerStartStopTest {

    private static final String RACENAME3 = "racedef3";
    private static final String RACENAME2 = "racedef2";
    private static final String RACENAME1 = "racedef1";
    private final static String EVENTNAME = "TESTEVENT";
    private final static String BOATCLASSNAME = "HAPPYBOATCLASS";

    private RacingEventServiceImplMock racingEventService;
    private Regatta regatta;
    private BoatClass boatClass;
    private Set<RaceTracker> raceTrackerSet = new HashSet<RaceTracker>();

    private RaceDefinition raceDef1;
    private RaceDefinition raceDef2;
    private RaceDefinition raceDef3;

    private RaceTrackerMock raceTracker1;
    private RaceTrackerMock raceTracker2;
    private RaceTrackerMock raceTracker3;

    @Before
    public void setUp() {
        racingEventService = new RacingEventServiceImplMock();
        boatClass = new BoatClassImpl(BOATCLASSNAME, /* typicallyStartsUpwind */ true);
        regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EVENTNAME, boatClass, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), UUID.randomUUID(), null);
        racingEventService.getEventsByName().put(EVENTNAME, regatta);
        TrackedRegatta trackedRegatta1 = racingEventService.getOrCreateTrackedRegatta(regatta);
        racingEventService.getEventsByNameMap().put(EVENTNAME, regatta);
        raceTrackerSet = new HashSet<RaceTracker>();
        raceDef1 = new RaceDefinitionImpl(RACENAME1, new CourseImpl("Course1", new ArrayList<Waypoint>()), boatClass, new ArrayList<Competitor>());
        raceDef2 = new RaceDefinitionImpl(RACENAME2, new CourseImpl("Course2", new ArrayList<Waypoint>()), boatClass, new ArrayList<Competitor>());
        raceDef3 = new RaceDefinitionImpl(RACENAME3, new CourseImpl("Course3", new ArrayList<Waypoint>()), boatClass, new ArrayList<Competitor>());
        regatta.addRace(raceDef1);
        trackedRegatta1.createTrackedRace(raceDef1, Collections.<Sideline> emptyList(), /* windStore */ EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l, /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null);
        regatta.addRace(raceDef2);
        trackedRegatta1.createTrackedRace(raceDef2, Collections.<Sideline> emptyList(), /* windStore */ EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l, /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null);
        regatta.addRace(raceDef3);
        trackedRegatta1.createTrackedRace(raceDef3, Collections.<Sideline> emptyList(), /* windStore */ EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l, /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null);
        Set<RaceDefinition> raceDefinitionSetRace1 = new HashSet<RaceDefinition>();
        raceDefinitionSetRace1.add(raceDef1);
        Set<RaceDefinition> raceDefinitionSetRace2 = new HashSet<RaceDefinition>();
        raceDefinitionSetRace2.add(raceDef1);
        raceDefinitionSetRace2.add(raceDef2);
        Set<RaceDefinition> raceDefinitionSetRace3 = new HashSet<RaceDefinition>();
        raceDefinitionSetRace3.add(raceDef1);
        raceDefinitionSetRace3.add(raceDef2);
        raceDefinitionSetRace3.add(raceDef3);
        Long trackerID1 = new Long(1);
        Long trackerID2 = new Long(2);
        Long trackerID3 = new Long(3);
        raceTracker1 = new RaceTrackerMock(new Long(1), regatta, raceDefinitionSetRace1, true);
        raceTracker2 = new RaceTrackerMock(new Long(2), regatta, raceDefinitionSetRace2, true);
        raceTracker3 = new RaceTrackerMock(new Long(3), regatta, raceDefinitionSetRace3, true);
        raceTrackerSet.add(raceTracker1);
        raceTrackerSet.add(raceTracker2);
        raceTrackerSet.add(raceTracker3);
        racingEventService.getRaceTrackersByEventMap().put(regatta, raceTrackerSet);
        racingEventService.getRaceTrackersByIDMap().put(trackerID1, raceTracker1);
        racingEventService.getRaceTrackersByIDMap().put(trackerID2, raceTracker2);
        racingEventService.getRaceTrackersByIDMap().put(trackerID3, raceTracker3);
    }

    /**
     * This test method tests, if the {@link RacingEventService#stopTracking(Regatta, RaceDefinition) stopTracking} method works correctly.
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
        for (TrackedRace trackedRace : trackedRegatta.getTrackedRaces()) {
            if (trackedRace.getRace().getName().equals(RACENAME2)) {
                foundTrackedRaceForRaceDef2 = true;
            }
        }
        assertTrue(foundTrackedRaceForRaceDef2);
        // The raceTracker2 and raceTracker3 should currently not be in track mode. 
        assertTrue(raceTracker1.getIsTracking());
        assertFalse(raceTracker2.getIsTracking());
        assertFalse(raceTracker3.getIsTracking());
        // The RaceTrackersByID map should not contain the trackers raceTracker2 and raceTracker3 anymore
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker2));
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker3));
        // The RaceTrakcersByEvent map should contain a tracker with a set of RaceDefinitions, containing the
        // raceDefinition1
        assertEquals(1, racingEventService.getRaceTrackersByEventMap().size());
        Iterator<RaceTracker> raceTrackerIter = racingEventService.getRaceTrackersByEventMap().get(regatta).iterator();
        while (raceTrackerIter.hasNext()) {
            RaceTracker currentTracker = raceTrackerIter.next();
            assertSame(raceTracker1, currentTracker);
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
        for (TrackedRace trackedRace : trackedRegatta.getTrackedRaces()) {
            if (trackedRace.getRace().getName().equals(RACENAME2)) {
                foundTrackedRaceForRaceDef2 = true;
            }
        }
        assertFalse(foundTrackedRaceForRaceDef2);
        // The trackers map should still contain the raceTrackers
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(regatta).contains(raceTracker1));
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(regatta).contains(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(regatta).contains(raceTracker3));
        // The raceTrackerMap should still contain the raceTrackers. These raceTracker should not contain the raceDefinition raceDef2 anymore
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker1));
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker3));
        // The raceTracker should still exist; it shall still contain raceDef1 and raceDef2 because a tracker keeps tracking what it tracks...
        assertTrue(raceTracker1.getRaces().contains(raceDef1));
        assertEquals(1, raceTracker1.getRaces().size());
        assertTrue(raceTracker2.getRaces().contains(raceDef1));
        assertTrue(raceTracker2.getRaces().contains(raceDef2));
        assertEquals(2, raceTracker2.getRaces().size());
        assertTrue(raceTracker3.getRaces().contains(raceDef1));
        assertTrue(raceTracker3.getRaces().contains(raceDef2));
        assertTrue(raceTracker3.getRaces().contains(raceDef3));
        assertEquals(3, raceTracker3.getRaces().size());
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
        assertFalse(racingEventService.getRaceTrackersByEventMap().get(regatta).contains(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByEventMap().get(regatta).contains(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(regatta).contains(raceTracker3));
        // The RaceTrackerByID map should still contain raceTracker3, but not raceTracker1 and raceTracker2 anymore
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker3));
        // The raceTracker 3 should exist, and it should contain all race definitions still
        assertTrue(raceTracker3.getRaces().contains(raceDef3));
        assertEquals(3, raceTracker3.getRaces().size());
    }

}
