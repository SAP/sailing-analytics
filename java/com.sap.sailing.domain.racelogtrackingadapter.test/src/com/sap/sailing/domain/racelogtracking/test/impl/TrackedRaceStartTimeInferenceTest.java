package com.sap.sailing.domain.racelogtracking.test.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelogtracking.test.AbstractGPSFixStoreTest;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TrackedRaceStartTimeInferenceTest extends AbstractGPSFixStoreTest {
    private final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");

    /**
     * tests the precedence order described in {@link TrackedRaceImpl#updateStartAndEndOfTracking(boolean)}
     */
    @Test
    public void testStartTimeInferencePrecedenceOrder() throws TransformationException,
            NoCorrespondingServiceRegisteredException, InterruptedException {
        Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", "c2", null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat2 = DomainFactory.INSTANCE.getOrCreateBoat("boat2", "boat2", boatClass, "USA 123", null, /* storePersistently */ true);
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat);
        competitorsAndBoats.put(comp2, boat2);
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author, new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author, new MillisecondsTimePoint(1), 0, mark2));
        Course course = new CourseImpl("course", Arrays.asList(new Waypoint[] { new WaypointImpl(mark),
                new WaypointImpl(mark2) }));
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);

        TrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
        regatta.getRegatta().setControlTrackingFromStartAndFinishTimes(true);
        final DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(regatta, race,
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, 0, 0, 0,
                /* useMarkPassingCalculator */ false,
                OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        
        MillisecondsTimePoint startOfRaceInRaceLog = new MillisecondsTimePoint(10000);
        MillisecondsTimePoint endOfRaceInRaceLog = new MillisecondsTimePoint(20000);
        setStartAndEndOfRaceInRaceLog(startOfRaceInRaceLog, endOfRaceInRaceLog);
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        
        // test inference via race start/end time in racelog
        TimePoint expectedStartOfTracking = startOfRaceInRaceLog.minus(TrackedRace.START_TRACKING_THIS_MUCH_BEFORE_RACE_START);
        TimePoint expectedEndOfTracking = endOfRaceInRaceLog.plus(TrackedRace.STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH);
        assertEquals(expectedStartOfTracking, trackedRace.getStartOfTracking());
        assertEquals(expectedEndOfTracking, trackedRace.getEndOfTracking());
        
        // test inference via manually set start/end of tracking
        final MillisecondsTimePoint manualStartOfTracking = new MillisecondsTimePoint(30000);
        final MillisecondsTimePoint manualEndOfTracking = new MillisecondsTimePoint(40000);
        setManualTrackingTimesOnTrackedRace(trackedRace, manualStartOfTracking, manualEndOfTracking);
        assertEquals(manualStartOfTracking, trackedRace.getStartOfTracking());
        assertEquals(manualEndOfTracking, trackedRace.getEndOfTracking());
        
        // shouldn't change when setting time in raceLog again
        setStartAndEndOfRaceInRaceLog(new MillisecondsTimePoint(100000), new MillisecondsTimePoint(200000));
        assertEquals(manualStartOfTracking, trackedRace.getStartOfTracking());
        assertEquals(manualEndOfTracking, trackedRace.getEndOfTracking());
        
        // test inference via start/end of tracking in RaceLog
        MillisecondsTimePoint startOfTrackingInRacelog = new MillisecondsTimePoint(50000);
        MillisecondsTimePoint endOfTrackingInRacelog = new MillisecondsTimePoint(60000);
        setStartAndEndOfTrackingInRaceLog(startOfTrackingInRacelog, endOfTrackingInRacelog);
        assertEquals(startOfTrackingInRacelog, trackedRace.getStartOfTracking());
        assertEquals(endOfTrackingInRacelog, trackedRace.getEndOfTracking());
        
        // shouldn't change when setting time in raceLog again
        setStartAndEndOfRaceInRaceLog(new MillisecondsTimePoint(300000), new MillisecondsTimePoint(400000));
        assertEquals(startOfTrackingInRacelog, trackedRace.getStartOfTracking());
        assertEquals(endOfTrackingInRacelog, trackedRace.getEndOfTracking());
        
        // shouldn't change when setting start/end of tracking
        setManualTrackingTimesOnTrackedRace(trackedRace, new MillisecondsTimePoint(500000), new MillisecondsTimePoint(600000));
        assertEquals(startOfTrackingInRacelog, trackedRace.getStartOfTracking());
        assertEquals(endOfTrackingInRacelog, trackedRace.getEndOfTracking());
        
        // bug 4114: now detach the race log and attach it again; this is expected to set the
        // start of race from the race log which now is no longer expected to update the race log
        // with new start/end of tracking times when there are already such explicit events:
        trackedRace.detachRaceLog(raceLog.getId());
        trackedRace.attachRaceLog(raceLog);
        assertEquals(startOfTrackingInRacelog, trackedRace.getStartOfTracking());
        assertEquals(endOfTrackingInRacelog, trackedRace.getEndOfTracking());
    }    
    
    /**
     * tests notification of first start time update through race log; see bug 3660
     */
    @Test
    public void testStartTimeChangeNotificationForFirstUpdateThroughRaceLog() throws TransformationException,
            NoCorrespondingServiceRegisteredException, InterruptedException {
        Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", "c2", null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat2 = DomainFactory.INSTANCE.getOrCreateBoat("boat2", "boat2", boatClass, "USA 123", null, /* storePersistently */ true);
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat);
        competitorsAndBoats.put(comp2, boat2);
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author, new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author, new MillisecondsTimePoint(1), 0, mark2));
        Course course = new CourseImpl("course", Arrays.asList(new Waypoint[] { new WaypointImpl(mark),
                new WaypointImpl(mark2) }));
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);
        TrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
        final DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(regatta, race,
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, 0, 0, 0,
                /* useMarkPassingCalculator */ false,
                OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        trackedRace.attachRaceLog(raceLog);
        final TimePoint[] oldAndNewStartTimeNotifiedByRace = new TimePoint[2];
        trackedRace.addListener(new AbstractRaceChangeListener() {
            @Override
            public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
                oldAndNewStartTimeNotifiedByRace[0] = oldStartOfRace;
                oldAndNewStartTimeNotifiedByRace[1] = newStartOfRace;
            }
        });
        assertNull(trackedRace.getStartOfRace());
        final TimePoint newStartOfRace = MillisecondsTimePoint.now();
        raceLog.add(new RaceLogStartTimeEventImpl(newStartOfRace, author, 0, newStartOfRace, /* courseAreaId */ null));
        assertNull(oldAndNewStartTimeNotifiedByRace[0]);
        assertEquals(newStartOfRace, oldAndNewStartTimeNotifiedByRace[1]);
        assertEquals(newStartOfRace, trackedRace.getStartOfRace());
    }    
    
    /**
     * tests notification of first start time update through race log; see bug 3660
     */
    @Test
    public void testStartTimeChangeNotificationForFirstUpdateThroughStartMarkPassing() throws TransformationException,
            NoCorrespondingServiceRegisteredException, InterruptedException {
        Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", "c2", null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat2 = DomainFactory.INSTANCE.getOrCreateBoat("boat2", "boat2", boatClass, "USA 123", null, /* storePersistently */ true);
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat);
        competitorsAndBoats.put(comp2, boat2);
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author, new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author, new MillisecondsTimePoint(1), 0, mark2));
        Course course = new CourseImpl("course", Arrays.asList(new Waypoint[] { new WaypointImpl(mark),
                new WaypointImpl(mark2) }));
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);
        TrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
        regatta.getRegatta().setControlTrackingFromStartAndFinishTimes(true);
        final DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(regatta, race,
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, 0, 0, 0,
                /* useMarkPassingCalculator */ false,
                OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        final TimePoint[] oldAndNewStartTimeNotifiedByRace = new TimePoint[2];
        trackedRace.addListener(new AbstractRaceChangeListener() {
            @Override
            public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
                oldAndNewStartTimeNotifiedByRace[0] = oldStartOfRace;
                oldAndNewStartTimeNotifiedByRace[1] = newStartOfRace;
            }
        });
        assertNull(trackedRace.getStartOfRace());
        final TimePoint newStartOfRace = MillisecondsTimePoint.now();
        trackedRace.updateMarkPassings(comp, Arrays.asList(new MarkPassingImpl(newStartOfRace, Util.get(course.getWaypoints(),  0), comp),
                new MarkPassingImpl(newStartOfRace.plus(Duration.ONE_MINUTE), Util.get(course.getWaypoints(),  1), comp)));
        assertNull(oldAndNewStartTimeNotifiedByRace[0]);
        assertEquals(newStartOfRace, oldAndNewStartTimeNotifiedByRace[1]);
        assertEquals(newStartOfRace, trackedRace.getStartOfRace());
    }    
    

    
    
    /**
     * tests the precedence order described in {@link TrackedRaceImpl#updateStartAndEndOfTracking(boolean)}
     */
    @Test
    public void testStartAndEndTrackingTimeInferencePrecedenceOrderTriggersListener() throws TransformationException,
            NoCorrespondingServiceRegisteredException, InterruptedException {
        Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", "c2", null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat2 = DomainFactory.INSTANCE.getOrCreateBoat("boat2", "boat2", boatClass, "USA 123", null, /* storePersistently */ true);
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat);
        competitorsAndBoats.put(comp2, boat2);
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author, new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author, new MillisecondsTimePoint(1), 0, mark2));
        Course course = new CourseImpl("course", Arrays.asList(new Waypoint[] { new WaypointImpl(mark),
                new WaypointImpl(mark2) }));
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);
        TrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true,  CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
        regatta.getRegatta().setControlTrackingFromStartAndFinishTimes(true);
        assertTrue(regatta.getRegatta().useStartTimeInference());
        final DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(regatta, race,
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, 0, 0, 0,
                /* useMarkPassingCalculator */ false,
                OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        final TimePoint[] newStartAndEndOfTrackingNotifiedByRace = new TimePoint[2];
        trackedRace.addListener(new AbstractRaceChangeListener() {
            @Override
            public void startOfTrackingChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
                newStartAndEndOfTrackingNotifiedByRace[0] = newStartOfRace;
            }
            @Override
            public void endOfTrackingChanged(TimePoint oldEndOfTracking, TimePoint newEndOfTracking) {
                newStartAndEndOfTrackingNotifiedByRace[1] = newEndOfTracking;
            }
        });
        assertNull(trackedRace.getStartOfTracking());
        assertNull(trackedRace.getEndOfTracking());
        // test inference from implicit startOfRace change through start mark passing update
        newStartAndEndOfTrackingNotifiedByRace[0] = null;
        newStartAndEndOfTrackingNotifiedByRace[1] = null;
        final TimePoint startMarkPassingTimePoint = MillisecondsTimePoint.now();
        trackedRace.updateMarkPassings(comp, Collections.singleton(new MarkPassingImpl(startMarkPassingTimePoint, trackedRace.getRace().getCourse().getFirstWaypoint(), comp)));
        assertNotNull(newStartAndEndOfTrackingNotifiedByRace[0]);
        assertTrue(trackedRace.getStartOfTracking().before(startMarkPassingTimePoint));
        final MillisecondsTimePoint startOfRaceInRaceLog = new MillisecondsTimePoint(123456);
        assertTrue(trackedRace.getStartOfTracking().after(startOfRaceInRaceLog));
        // test inference from finished time change by new blue flag down event
        newStartAndEndOfTrackingNotifiedByRace[0] = null;
        newStartAndEndOfTrackingNotifiedByRace[1] = null;
        final TimePoint finishedTimePoint = MillisecondsTimePoint.now();
        raceLog.add(new RaceLogRaceStatusEventImpl(finishedTimePoint, finishedTimePoint, author, UUID.randomUUID(), 0, RaceLogRaceStatus.FINISHED));
        assertNotNull(newStartAndEndOfTrackingNotifiedByRace[1]);
        assertTrue(trackedRace.getEndOfTracking().after(finishedTimePoint));
        // verify that setting a start and finished time through the race log adjusts the start/end of tracking times
        newStartAndEndOfTrackingNotifiedByRace[0] = null;
        newStartAndEndOfTrackingNotifiedByRace[1] = null;
        final MillisecondsTimePoint endOfRaceInRaceLog = new MillisecondsTimePoint(234567);
        setStartAndEndOfRaceInRaceLog(startOfRaceInRaceLog, endOfRaceInRaceLog);
        assertNotNull(trackedRace.getStartOfTracking());
        assertNotNull(trackedRace.getEndOfTracking());
        assertTrue(trackedRace.getStartOfTracking().before(startOfRaceInRaceLog));
        assertTrue(trackedRace.getEndOfTracking().after(endOfRaceInRaceLog));
        newStartAndEndOfTrackingNotifiedByRace[0] = null;
        newStartAndEndOfTrackingNotifiedByRace[1] = null;
        final MillisecondsTimePoint manualStartOfTracking = new MillisecondsTimePoint(1111);
        final MillisecondsTimePoint manualEndOfTracking = new MillisecondsTimePoint(2222);
        setManualTrackingTimesOnTrackedRace(trackedRace, manualStartOfTracking, manualEndOfTracking); // wrong values; race log should take precedence when available
        // assert that the event listener was triggered
        assertEquals(manualStartOfTracking, newStartAndEndOfTrackingNotifiedByRace[0]);
        assertEquals(manualEndOfTracking, newStartAndEndOfTrackingNotifiedByRace[1]);
        // test values set immediately
        assertEquals(manualStartOfTracking, trackedRace.getStartOfTracking());
        assertEquals(manualEndOfTracking, trackedRace.getEndOfTracking());
        newStartAndEndOfTrackingNotifiedByRace[0] = null;
        newStartAndEndOfTrackingNotifiedByRace[1] = null;
        final MillisecondsTimePoint newStartOfTrackingInRaceLog = new MillisecondsTimePoint(10000);
        final MillisecondsTimePoint newEndOfTrackingInRaceLog = new MillisecondsTimePoint(20000);
        setStartAndEndOfTrackingInRaceLog(newStartOfTrackingInRaceLog, newEndOfTrackingInRaceLog); // correct values, taking precedence
        // assert that the event listener was triggered
        assertEquals(newStartOfTrackingInRaceLog, newStartAndEndOfTrackingNotifiedByRace[0]);
        assertEquals(newEndOfTrackingInRaceLog, newStartAndEndOfTrackingNotifiedByRace[1]);
        // test inference via racelog
        assertEquals(newStartOfTrackingInRaceLog, trackedRace.getStartOfTracking());
        assertEquals(newEndOfTrackingInRaceLog, trackedRace.getEndOfTracking());
        // shouldn't change anymore when setting explicitly because race log takes precedence
        newStartAndEndOfTrackingNotifiedByRace[0] = null;
        newStartAndEndOfTrackingNotifiedByRace[1] = null;
        setManualTrackingTimesOnTrackedRace(trackedRace, manualStartOfTracking, manualEndOfTracking);
        assertNull(newStartAndEndOfTrackingNotifiedByRace[0]);
        assertNull(newStartAndEndOfTrackingNotifiedByRace[1]);
        assertEquals(newStartOfTrackingInRaceLog, trackedRace.getStartOfTracking());
        assertEquals(newEndOfTrackingInRaceLog, trackedRace.getEndOfTracking());
        // test inference when setting null in RaceLog; RaceLog should still take precedence with its null values
        newStartAndEndOfTrackingNotifiedByRace[0] = MillisecondsTimePoint.now();
        newStartAndEndOfTrackingNotifiedByRace[1] = MillisecondsTimePoint.now();
        raceLog.add(new RaceLogStartOfTrackingEventImpl(null, author, 0));
        raceLog.add(new RaceLogEndOfTrackingEventImpl(null, author, 0));
        assertNull(trackedRace.getStartOfTracking());
        assertNull(trackedRace.getEndOfTracking());
        assertNull(newStartAndEndOfTrackingNotifiedByRace[0]);
        assertNull(newStartAndEndOfTrackingNotifiedByRace[1]);
    }    
    
    /**
     * tests notification of first start time update through race log; see bug 3660
     */
    @Test
    public void testStartAndEndOfTrackingTimeChangeNotificationForFirstUpdateThroughRaceLog() throws TransformationException,
            NoCorrespondingServiceRegisteredException, InterruptedException {
        Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", "c2", null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat2 = DomainFactory.INSTANCE.getOrCreateBoat("boat2", "boat2", boatClass, "USA 123", null, /* storePersistently */ true);
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat);
        competitorsAndBoats.put(comp2, boat2);
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author, new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author, new MillisecondsTimePoint(1), 0, mark2));
        Course course = new CourseImpl("course", Arrays.asList(new Waypoint[] { new WaypointImpl(mark),
                new WaypointImpl(mark2) }));
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);
        TrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
        final DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(regatta, race,
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, 0, 0, 0,
                /* useMarkPassingCalculator */ false,
                OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        trackedRace.attachRaceLog(raceLog);
        final TimePoint[] oldAndNewStartTimeNotifiedByRace = new TimePoint[2];
        trackedRace.addListener(new AbstractRaceChangeListener() {
            @Override
            public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
                oldAndNewStartTimeNotifiedByRace[0] = oldStartOfRace;
                oldAndNewStartTimeNotifiedByRace[1] = newStartOfRace;
            }
        });
        assertNull(trackedRace.getStartOfRace());
        final TimePoint newStartOfRace = MillisecondsTimePoint.now();
        raceLog.add(new RaceLogStartTimeEventImpl(newStartOfRace, author, 0, newStartOfRace, /* courseAreaId */ null));
        assertNull(oldAndNewStartTimeNotifiedByRace[0]);
        assertEquals(newStartOfRace, oldAndNewStartTimeNotifiedByRace[1]);
        assertEquals(newStartOfRace, trackedRace.getStartOfRace());
    }    
}
