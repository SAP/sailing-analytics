package com.sap.sailing.domain.racelogtracking.test.impl;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
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
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelog.tracking.test.mock.SmartphoneImeiIdentifier;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sailing.domain.racelogtracking.impl.fixtracker.FixLoaderAndTracker;
import com.sap.sailing.domain.racelogtracking.test.AbstractGPSFixStoreTest;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Timed;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

public class TrackedRaceLoadsFixesTest extends AbstractGPSFixStoreTest {
    private final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");

    @Rule
    public Timeout TrackedRaceLoadsFixesTestTimeout = new Timeout(3 * 60 * 1000);

    @Test
    public void doesRaceLoadOnlyBetweenStartAndEndOfTracking() throws TransformationException,
            NoCorrespondingServiceRegisteredException, InterruptedException {
        Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", null, null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        DeviceIdentifier markDevice = new SmartphoneImeiIdentifier("imei2");
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author, new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author, new MillisecondsTimePoint(1), 0, mark2));
        Course course = new CourseImpl("course", Arrays.asList(new Waypoint[] { new WaypointImpl(mark),
                new WaypointImpl(mark2) }));
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, Arrays.asList(new Competitor[] { comp, comp2 }));
        map(comp, device, 0, 10000);
        map(mark, markDevice, 0, 10000);

        store.storeFix(device, createFix(100, 10, 20, 30, 40)); // before
        store.storeFix(device, createFix(1100, 10, 20, 30, 40)); // in
        store.storeFix(device, createFix(2100, 10, 20, 30, 40)); // after
        store.storeFix(markDevice, createFix(100, 10, 20, 30, 40));
        store.storeFix(markDevice, createFix(1100, 10, 20, 30, 40));
        store.storeFix(markDevice, createFix(2100, 10, 20, 30, 40));

        DynamicTrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass, /* startDate */
                null, /* endDate */null, null, null, "a", null));
        final DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(regatta, race,
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, 0, 0, 0,
                /* useMarkPassingCalculator */ false,
                OneDesignRankingMetric::new, mock(RaceLogResolver.class));
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(1000));
        trackedRace.setEndOfTrackingReceived(new MillisecondsTimePoint(2000));
        new FixLoaderAndTracker(trackedRace, store, null);

        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();

        testNumberOfRawFixes(trackedRace.getTrack(comp), 1);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        // now extend the tracking interval of the tracked race and assert that the additional fixes are loaded
        trackedRace.setEndOfTrackingReceived(new MillisecondsTimePoint(2500), /* wait for fixes to load */ true);
        testNumberOfRawFixes(trackedRace.getTrack(comp), 2);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(0), /* wait for fixes to load */ true);
        testNumberOfRawFixes(trackedRace.getTrack(comp), 3);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 3);
    }
    
    @Test
    public void areFixesStoredInDb() throws TransformationException, NoCorrespondingServiceRegisteredException,
            InterruptedException {
        Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", null, null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author, new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author, new MillisecondsTimePoint(1), 0, mark2));
        DeviceIdentifier device2 = new SmartphoneImeiIdentifier("imei2");
        DeviceIdentifier device3 = new SmartphoneImeiIdentifier("imei3");
        Course course = new CourseImpl("course", Arrays.asList(new Waypoint[] { new WaypointImpl(mark),
                new WaypointImpl(mark2) }));
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, Arrays.asList(comp, comp2));

        map(comp, device, 0, 20000);
        map(comp2, device2, 0, 600);
        // reuse device for two marks
        map(mark, device3, 0, 600);
        map(mark2, device3, 0, 600);

        store.storeFix(device, createFix(100, 10, 20, 30, 40));
        store.storeFix(device, createFix(200, 10, 20, 30, 40));
        store.storeFix(device2, createFix(100, 10, 20, 30, 40));
        store.storeFix(device3, createFix(100, 10, 20, 30, 40));
        store.storeFix(device3, createFix(100, 10, 20, 30, 40));

        for (int i = 0; i < 10000; i++) {
            store.storeFix(device, createFix(i + 1000, 10, 20, 30, 40));
        }

        DynamicTrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass,
                /* startDate */ null, /* endDate */null, null, null, "a", null));
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(regatta, race,
                Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE, 0, 0, 0,
                /*useMarkPassingCalculator*/ false, OneDesignRankingMetric::new, mock(RaceLogResolver.class));

        new FixLoaderAndTracker(trackedRace, store, null);
        
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();

        testNumberOfRawFixes(trackedRace.getTrack(comp), 10002);
        testNumberOfRawFixes(trackedRace.getTrack(comp2), 1);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark2), 1);
    }
    
    /** Bug 4008 */
    @Test
    public void testFixesForMarkAreLoadedIfMappingDoesNotIntersectWithTrackingInterval()
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixesForMarks(mark2 -> {
            map(mark, device, 0, 100);
            store.storeFix(device, createFix(10, 10, 20, 30, 40));
            store.storeFix(device, createFix(20, 10, 20, 30, 40));
        }, (trackedRace, mark2) -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 2);
        });
    }
    
    @Test
    public void testFixesForTwoMarksAreLoadedIfMappingsDoNotIntersectWithTrackingInterval()
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixesForMarks(mark2 -> {
            map(mark, device, 0, 100);
            store.storeFix(device, createFix(50, 10, 20, 30, 40));
            
            map(mark2, device, 200, 300);
            store.storeFix(device, createFix(250, 10, 20, 30, 40));
        }, (trackedRace, mark2) -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark2), 1);
        });
    }
    
    @Test
    public void testFixesForMarkAreLoadedIfMappingDoesIntersectWithTrackingIntervalFixesWithinIntersectionOnly()
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixesForMarks(mark2 -> {
            map(mark, device, 0, 200);
            store.storeFix(device, createFix(50, 10, 20, 30, 40));
            store.storeFix(device, createFix(150, 10, 20, 30, 40));
            store.storeFix(device, createFix(250, 10, 20, 30, 40));
            
            map(mark, device, 350, 500);
            store.storeFix(device, createFix(400, 10, 20, 30, 40));
            store.storeFix(device, createFix(450, 10, 20, 30, 40));
        }, (trackedRace, mark2) -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 1);
        });
    }
    
    @Test
    public void testFixesForMarkAreLoadedIfMappingDoesIntersectWithTrackingIntervalAllFixesBecauseOfNoFixesWithIntersection() 
        throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        testFixesForMarks(mark2 -> {
            map(mark, device, 0, 200);
            store.storeFix(device, createFix(50, 10, 20, 30, 40));
            store.storeFix(device, createFix(150, 10, 20, 30, 40));
            
            map(mark, device, 350, 500);
            store.storeFix(device, createFix(400, 10, 20, 30, 40));
            store.storeFix(device, createFix(450, 10, 20, 30, 40));
        }, (trackedRace, mark2) -> {
            testNumberOfRawFixes(trackedRace.getOrCreateTrack(mark), 4);
        });
    }
    
    private void testFixesForMarks(Consumer<Mark> mappingAndFixes, BiConsumer<DynamicTrackedRace, Mark> tests)
            throws TransformationException, NoCorrespondingServiceRegisteredException, InterruptedException {
        Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
        defineMarks(mark, mark2);
        setStartAndEndOfTracking(400, 500);
        
        Course course = createCourse("course", mark, mark2);
        RaceDefinition raceDefinition = new RaceDefinitionImpl("race", course, boatClass, Arrays.asList(comp));
        
        mappingAndFixes.accept(mark2);
        
        DynamicTrackedRace trackedRace = createDynamikTrackedRace(boatClass, raceDefinition);
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        
        new FixLoaderAndTracker(trackedRace, store, null);
        trackedRace.waitForLoadingToFinish();

        tests.accept(trackedRace, mark2);
    }
    
    @Test
    public void metadataStoredInDb() throws TransformationException, NoCorrespondingServiceRegisteredException {
        assertEquals(0, store.getNumberOfFixes(device));
        assertEquals(null, store.getTimeRangeCoveredByFixes(device));

        map(comp, device, 0, 600);

        store.storeFix(device, createFix(100, 10, 20, 30, 40));
        store.storeFix(device, createFix(200, 10, 20, 30, 40));

        assertEquals(2, store.getNumberOfFixes(device));
        assertEquals(TimeRangeImpl.create(100, 200), store.getTimeRangeCoveredByFixes(device));
    }
    
    @Test
    public void testFindLatestFixForMapping() throws TransformationException, NoCorrespondingServiceRegisteredException {
        store.storeFix(device, createFix(100, 10, 20, 30, 40));
        store.storeFix(device, createFix(1100, 10, 20, 30, 40));
        store.storeFix(device, createFix(2100, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes = store.getLastFix(Collections.singleton(device));
        assertEquals(1, lastFixes.size());
        Timed lastFix = lastFixes.get(device);
        assertEquals(2100, lastFix.getTimePoint().asMillis());
        store.storeFix(device, createFix(2000, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes2 = store.getLastFix(Collections.singleton(device));
        assertEquals(1, lastFixes2.size());
        Timed lastFix2 = lastFixes2.get(device);
        assertEquals(2100, lastFix2.getTimePoint().asMillis());
        store.storeFix(device, createFix(2200, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes3 = store.getLastFix(Collections.singleton(device));
        assertEquals(1, lastFixes3.size());
        Timed lastFix3 = lastFixes3.get(device);
        assertEquals(2200, lastFix3.getTimePoint().asMillis());
        final DeviceIdentifier device2 = new SmartphoneImeiIdentifier("b");
        store.storeFix(device2, createFix(1200, 10, 20, 30, 40));
        store.storeFix(device2, createFix(1100, 10, 20, 30, 40));
        final Map<DeviceIdentifier, Timed> lastFixes4 = store.getLastFix(Arrays.asList(device, device2));
        assertEquals(2, lastFixes4.size());
        Timed lastFix4 = lastFixes4.get(device);
        assertEquals(2200, lastFix4.getTimePoint().asMillis());
        Timed lastFixDevice2 = lastFixes4.get(device2);
        assertEquals(1200, lastFixDevice2.getTimePoint().asMillis());
    }
}
