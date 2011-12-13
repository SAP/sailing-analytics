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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.EventImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedEvent;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.RacingEventService;

public class RaceTrackerStartStopTest {

    private static final String RACENAME3 = "racedef3";
    private static final String RACENAME2 = "racedef2";
    private static final String RACENAME1 = "racedef1";
    private final static String EVENTNAME = "TESTEVENT";
    private final static String BOATCLASSNAME = "HAPPYBOATCLASS";

    private RacingEventServiceImplMock racingEventService;
    private Event event;
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
        boatClass = new BoatClassImpl(BOATCLASSNAME);
        event = new EventImpl(EVENTNAME, boatClass);
        racingEventService.getEventsByName().put(EVENTNAME, event);
        TrackedEvent trackedEvent1 = racingEventService.getOrCreateTrackedEvent(event);
        racingEventService.getEventsByNameMap().put(EVENTNAME, event);
        raceTrackerSet = new HashSet<RaceTracker>();
        raceDef1 = new RaceDefinitionImpl(RACENAME1, new CourseImpl("Course1", new ArrayList<Waypoint>()), boatClass, new ArrayList<Competitor>());
        raceDef2 = new RaceDefinitionImpl(RACENAME2, new CourseImpl("Course2", new ArrayList<Waypoint>()), boatClass, new ArrayList<Competitor>());
        raceDef3 = new RaceDefinitionImpl(RACENAME3, new CourseImpl("Course3", new ArrayList<Waypoint>()), boatClass, new ArrayList<Competitor>());
        event.addRace(raceDef1);
        trackedEvent1.createTrackedRace(raceDef1, /* windStore */ EmptyWindStore.INSTANCE, /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null);
        event.addRace(raceDef2);
        trackedEvent1.createTrackedRace(raceDef2, /* windStore */ EmptyWindStore.INSTANCE, /* millisecondsOverWhichToAverageWind */ 0l,
                /* millisecondsOverWhichToAverageSpeed */ 0l, /* raceDefinitionSetToUpdate */ null);
        event.addRace(raceDef3);
        trackedEvent1.createTrackedRace(raceDef3, /* windStore */ EmptyWindStore.INSTANCE, /* millisecondsOverWhichToAverageWind */ 0l,
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
        raceTracker1 = new RaceTrackerMock(new Long(1), event, raceDefinitionSetRace1, true);
        raceTracker2 = new RaceTrackerMock(new Long(2), event, raceDefinitionSetRace2, true);
        raceTracker3 = new RaceTrackerMock(new Long(3), event, raceDefinitionSetRace3, true);
        raceTrackerSet.add(raceTracker1);
        raceTrackerSet.add(raceTracker2);
        raceTrackerSet.add(raceTracker3);
        racingEventService.getRaceTrackersByEventMap().put(event, raceTrackerSet);
        racingEventService.getRaceTrackersByIDMap().put(trackerID1, raceTracker1);
        racingEventService.getRaceTrackersByIDMap().put(trackerID2, raceTracker2);
        racingEventService.getRaceTrackersByIDMap().put(trackerID3, raceTracker3);
    }

    /**
     * This test method tests, if the {@link RacingEventService#stopTracking(Event, RaceDefinition) stopTracking} method works correctly.
     */
    @Test
    public void testStopTrackingRace() throws MalformedURLException, IOException, InterruptedException {
        Event event = racingEventService.getEventByName(EVENTNAME);
        TrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
        assertNotNull(event.getRaceByName(RACENAME2));
        assertNotNull(trackedEvent.getExistingTrackedRace(event.getRaceByName(RACENAME2)));
        racingEventService.stopTracking(event, raceDef2);
        // the raceDef2 should still be part of the event, and the corresponding tracked race should still be part
        // of the tracked event
        assertNotNull(event.getRaceByName(RACENAME2));
        boolean foundTrackedRaceForRaceDef2 = false;
        for (TrackedRace trackedRace : trackedEvent.getTrackedRaces()) {
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
        Iterator<RaceTracker> raceTrackerIter = racingEventService.getRaceTrackersByEventMap().get(event).iterator();
        while (raceTrackerIter.hasNext()) {
            RaceTracker currentTracker = raceTrackerIter.next();
            assertSame(raceTracker1, currentTracker);
        }
    }
    /**
     * This test methods checks if the {@link RacingEventService#removeRace(Event, RaceDefinition) removeRace} method works correctly
     */
    @Test
    public void testRemoveRace() throws MalformedURLException, IOException, InterruptedException {
        Event event = racingEventService.getEventByName(EVENTNAME);
        TrackedEvent trackedEvent = racingEventService.getTrackedEvent(event);
        assertNotNull(event.getRaceByName(RACENAME2));
        assertNotNull(trackedEvent.getExistingTrackedRace(event.getRaceByName(RACENAME2)));
        racingEventService.removeRace(event, raceDef2);
        // the raceDef2 should be removed from the event, and the corresponding tracked race should be removed
        // from the tracked event
        assertNull(event.getRaceByName(RACENAME2));
        boolean foundTrackedRaceForRaceDef2 = false;
        for (TrackedRace trackedRace : trackedEvent.getTrackedRaces()) {
            if (trackedRace.getRace().getName().equals(RACENAME2)) {
                foundTrackedRaceForRaceDef2 = true;
            }
        }
        assertFalse(foundTrackedRaceForRaceDef2);
        // The trackers map should still contain the raceTrackers
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(event).contains(raceTracker1));
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(event).contains(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(event).contains(raceTracker3));
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
     * This test methods checks if the {@link RacingEventService#removeRace(Event, RaceDefinition) removeRace} method works correctly if the
     * race to be stopped is the last race of a tracker
     */
    @Test
    public void testRemoveLastRaceOfTracker() throws MalformedURLException, IOException, InterruptedException {
        racingEventService.removeRace(event, raceDef1);
        racingEventService.removeRace(event, raceDef2);
        // The event map should still contain the raceTrackers except of raceTracker1 and raceTracker2
        assertFalse(racingEventService.getRaceTrackersByEventMap().get(event).contains(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByEventMap().get(event).contains(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByEventMap().get(event).contains(raceTracker3));
        // The RaceTrackerByID map should still contain raceTracker3, but not raceTracker1 and raceTracker2 anymore
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker1));
        assertFalse(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker2));
        assertTrue(racingEventService.getRaceTrackersByIDMap().containsValue(raceTracker3));
        // The raceTracker 3 should exist, and it should contain all race definitions still
        assertTrue(raceTracker3.getRaces().contains(raceDef3));
        assertEquals(3, raceTracker3.getRaces().size());
    }

}
