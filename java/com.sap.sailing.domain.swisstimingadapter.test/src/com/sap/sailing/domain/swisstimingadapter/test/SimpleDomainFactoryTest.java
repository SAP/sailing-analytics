package com.sap.sailing.domain.swisstimingadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.Mark;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceType;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.impl.CourseImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.MarkImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.RaceImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.StartListImpl;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sse.common.Util;

import difflib.PatchFailedException;

public class SimpleDomainFactoryTest {
    @Test
    public void testExtractBoatClassFromSimpleRaceID() {
        String raceID = "SAW005901";
        RaceType raceType = DomainFactory.INSTANCE.getRaceTypeFromRaceID(raceID);
        assertEquals("470", raceType.getBoatClass().getName());
    }
    
    @Test
    public void testExtractBoatClassFromRaceIDWithEventID() {
        String raceID = "QINSWC12013_SAW005901";
        RaceType raceType = DomainFactory.INSTANCE.getRaceTypeFromRaceID(raceID);
        assertEquals("470", raceType.getBoatClass().getName());
    }
    
    /**
     * Tests that an unknown boat class encoded in a regular "SA" format race ID is returned as the unknown boat class
     * instead of null.
     */
    @Test
    public void testUnknownBoatClassInValidRaceID() {
        DomainFactory domainFactory = DomainFactory.INSTANCE;
        final String raceID = "SAX920103";
        RaceDefinition raceDefinition = domainFactory.createRaceDefinition(domainFactory.getOrCreateDefaultRegatta(
                EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, raceID,
                null /* boat class */, new RacingEventServiceImpl()), new Race() {
            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public String getRaceID() {
                return raceID;
            }

            @Override
            public BoatClass getBoatClass() {
                return null;
            }

            @Override
            public String getRaceName() {
                return "The famous SAX920103 race";
            }
        }, new StartList() {
            private static final long serialVersionUID = -4148295407963335012L;

            @Override
            public String getRaceID() {
                return raceID;
            }

            @Override
            public Iterable<Competitor> getCompetitors() {
                return Collections.emptyList();
            }
            
        }, new Course() {
            @Override
            public String getRaceID() {
                return raceID;
            }

            @Override
            public Iterable<Mark> getMarks() {
                return Collections.emptyList();
            }
        }, new DefaultRaceTrackingHandler());
        assertNotNull(raceDefinition.getBoatClass());
        assertEquals("UNKNOWN", raceDefinition.getBoatClass().getName());
    }
    
    @Test
    public void testCourseConfigForMark() throws PatchFailedException {
        DomainFactory domainFactory = DomainFactory.INSTANCE;
        Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, "TestEvent",
                /* boatClass */ null, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, new RacingEventServiceImpl(),
                com.sap.sailing.domain.base.DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                "123", null, /* registrationLinkSecret */ UUID.randomUUID().toString());
        Race race = new RaceImpl("1234", "R1", "Race 1234");
        Iterable<Competitor> competitors = Collections.emptyList();
        StartList startList = new StartListImpl("1234", competitors);
        Mark mark1 = new MarkImpl("M1", 0, Arrays.asList("D1", "D2"), /* markType */ null);
        Mark mark2 = new MarkImpl("M1", 0, Arrays.asList("D3", "D4"), /* markType */ null);
        List<Mark> marks = Arrays.asList(mark1, mark2);
        Course course = new CourseImpl("1234", marks);
        RaceDefinition raceDefinition = domainFactory.createRaceDefinition(regatta, race, startList, course,
                new DefaultRaceTrackingHandler());
        ArrayList<Waypoint> waypoints1 = new ArrayList<Waypoint>();
        for (Waypoint waypoint : raceDefinition.getCourse().getWaypoints()) {
            waypoints1.add(waypoint);
        }
        domainFactory.updateCourseWaypoints(raceDefinition.getCourse(), marks);
        ArrayList<Waypoint> waypoints2 = new ArrayList<Waypoint>();
        for (Waypoint waypoint : raceDefinition.getCourse().getWaypoints()) {
            waypoints2.add(waypoint);
        }
        assertEquals(waypoints1, waypoints2);
    }

    @Test
    public void testCourseConfigForGate() throws PatchFailedException {
        DomainFactory domainFactory = DomainFactory.INSTANCE;
        Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                "TestEvent", /* boatClass */ null, /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, new RacingEventServiceImpl(),
                com.sap.sailing.domain.base.DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                "123", null, /* registrationLinkSecret */ UUID.randomUUID().toString());
        Race race = new RaceImpl("1234", "R1", "Race 1234");
        Iterable<Competitor> competitors = Collections.emptyList();
        StartList startList = new StartListImpl("1234", competitors);
        Mark mark1 = new MarkImpl("M1", 0, Arrays.asList("D1", "D2"), /* markType */ null);
        Mark mark2 = new MarkImpl("M1", 0, Arrays.asList("D3", "D4"), /* markType */ null);
        List<Mark> marks = Arrays.asList(mark1, mark2);
        Course course = new CourseImpl("1234", marks);
        RaceDefinition raceDefinition = domainFactory.createRaceDefinition(regatta, race, startList, course,
                new DefaultRaceTrackingHandler());
        assertEquals(2, Util.size(raceDefinition.getCourse().getWaypoints()));
        ArrayList<Waypoint> waypoints1 = new ArrayList<Waypoint>();
        for (Waypoint waypoint : raceDefinition.getCourse().getWaypoints()) {
            waypoints1.add(waypoint);
        }
        domainFactory.updateCourseWaypoints(raceDefinition.getCourse(), marks);
        ArrayList<Waypoint> waypoints2 = new ArrayList<Waypoint>();
        for (Waypoint waypoint : raceDefinition.getCourse().getWaypoints()) {
            waypoints2.add(waypoint);
        }
        assertEquals(waypoints1, waypoints2);
    }
}
