package com.sap.sailing.domain.racelogtracking.test.impl;

import static com.sap.sse.common.Util.size;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.MappingEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogCloseOpenEndedDeviceMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorBravoMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRevokeEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
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
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.sensordata.BravoSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.MongoSensorFixStoreImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelog.tracking.SensorFixMapper;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelogsensortracking.SensorFixMapperFactory;
import com.sap.sailing.domain.racelogtracking.impl.BravoDataFixMapper;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.impl.fixtracker.FixLoaderAndTracker;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.SensorFixTrackImpl;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class SensorFixStoreAndLoadTest {
    private static final long START_OF_TRACKING = 100;
    private static final long MID_OF_TRACKING = 200;
    private static final long END_OF_TRACKING = 300;
    private static final long FIX_TIMESTAMP = 110;
    private static final long FIX_TIMESTAMP2 = 120;
    private static final long FIX_TIMESTAMP3 = 210;
    private static final long AFTER_LAST_FIX = FIX_TIMESTAMP3 + 1;
    private static final Distance FIX_RIDE_HEIGHT = new MeterDistance(1337.0);
    private static final Distance FIX_RIDE_HEIGHT2 = new MeterDistance(1338.0);
    private static final Distance FIX_RIDE_HEIGHT3 = new MeterDistance(1336.0);
    private static final double FIX_TEST_VALUE = 12.0;
    protected final MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
    protected final DeviceIdentifier device = new SmartphoneImeiIdentifierImpl("a");
    protected final DeviceIdentifier deviceTest = new SmartphoneImeiIdentifierImpl("b");
    protected RaceLog raceLog;
    protected RegattaLog regattaLog;
    protected SensorFixStore store;
    protected final Competitor comp = DomainFactory.INSTANCE.getOrCreateCompetitor("comp", "comp", null, null, null, null,
            null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
    protected final Competitor comp2 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp2", "comp2", null, null, null, null,
            null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
    private final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");
    protected final Boat boat1 = DomainFactory.INSTANCE.getOrCreateBoat("Boat1", "Boat1", boatClass, "GER 1", null, /* storePersistently */ true);
    protected final Boat boat2 = DomainFactory.INSTANCE.getOrCreateBoat("Boat2", "Boat2", boatClass, "GER 2", null, /* storePersistently */ true);
    private final Competitor compNotPartOfRace = DomainFactory.INSTANCE.getOrCreateCompetitor("comp3", "comp3", null, null, null, null,
            null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
    protected final Mark mark = DomainFactory.INSTANCE.getOrCreateMark("mark");
    protected final Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");

    protected final AbstractLogEventAuthor author = new LogEventAuthorImpl("author", 0);
    private DynamicTrackedRace trackedRace;

    @BeforeEach
    public void setUp() throws UnknownHostException, MongoException {
        dropPersistedData();
        raceLog = new RaceLogImpl("racelog");
        raceLog.add(new RaceLogStartOfTrackingEventImpl(new MillisecondsTimePoint(START_OF_TRACKING), author, 0));
        raceLog.add(new RaceLogEndOfTrackingEventImpl(new MillisecondsTimePoint(END_OF_TRACKING), author, 0));
        regattaLog = new RegattaLogImpl("regattalog");
        store = new MongoSensorFixStoreImpl(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), serviceFinderFactory, ReadConcern.MAJORITY,
                WriteConcern.MAJORITY, /* clientSession */ null, /* metadataCollectionClientSession */ null);
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(1), author,
                new MillisecondsTimePoint(1), 0, mark));
        regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(2), author,
                new MillisecondsTimePoint(1), 0, mark2));
        Course course = new CourseImpl("course",
                Arrays.asList(new Waypoint[] { new WaypointImpl(mark), new WaypointImpl(mark2) }));
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat1);
        competitorsAndBoats.put(comp2, boat2);
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);
        DynamicTrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
        regatta.getRegatta().setControlTrackingFromStartAndFinishTimes(true);
        trackedRace = new DynamicTrackedRaceImpl(regatta, race, Collections.<Sideline> emptyList(),
                EmptyWindStore.INSTANCE, 0, 0, 0, /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
    }

    private void dropPersistedData() {
        MongoDatabase db = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().getDatabase();
        db.getCollection(CollectionNames.GPS_FIXES.name()).drop();
        db.getCollection(CollectionNames.GPS_FIXES_METADATA.name()).drop();
        db.getCollection(CollectionNames.REGATTA_LOGS.name()).drop();
        db.getCollection(CollectionNames.RACE_LOGS.name()).drop();
    }

    @AfterEach
    public void after() {
        dropPersistedData();
    }

    @Test
    public void testLoadAlreadyAddedFixes() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 2);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testAddFixesWhileTracking() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        addBravoFixes();
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 2);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testNoFixesAreLoadedIfNoStoredFixIsInTimeRange() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(AFTER_LAST_FIX), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 0);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testFixesNotInMappedTimeRangeAreIgnoredWhileTracking() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(AFTER_LAST_FIX), new MillisecondsTimePoint(END_OF_TRACKING)));
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        addBravoFixes();
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 0);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testCompetitorWithoutMappingHasNoTrack() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        assertNull(trackedRace.getSensorTrack(comp2, BravoFixTrack.TRACK_NAME));
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testDeviceIsMappedToDifferentCompetitorsInDifferentTimeRanges() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp2,
                device, new MillisecondsTimePoint(MID_OF_TRACKING + 1), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 2);
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp2, BravoFixTrack.TRACK_NAME), 1);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testMultipleMappingsForOneDeviceAndCompetitor() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(MID_OF_TRACKING + 1), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 3);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testNothingLoadedForRevokedMapping() throws InterruptedException {
        RegattaLogDeviceCompetitorBravoMappingEventImpl mappingEvent = new RegattaLogDeviceCompetitorBravoMappingEventImpl(
                new MillisecondsTimePoint(3), author, comp, device, new MillisecondsTimePoint(START_OF_TRACKING),
                new MillisecondsTimePoint(MID_OF_TRACKING));
        regattaLog.add(mappingEvent);
        regattaLog.add(new RegattaLogRevokeEventImpl(author, mappingEvent, "Test purposes"));
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        addBravoFixes();
        trackedRace.waitForLoadingToFinish();
        assertNull(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME));
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }
    
    @Test
    public void testFixesAreLoadedIfThereIsOneRevokedAndOneNonRevokedMapping() throws InterruptedException {
        // revoked mapping timestamp 100-300
        RegattaLogDeviceCompetitorBravoMappingEventImpl mappingEvent = new RegattaLogDeviceCompetitorBravoMappingEventImpl(
                new MillisecondsTimePoint(3), author, comp, device, new MillisecondsTimePoint(START_OF_TRACKING),
                new MillisecondsTimePoint(END_OF_TRACKING));
        regattaLog.add(mappingEvent);
        regattaLog.add(new RegattaLogRevokeEventImpl(author, mappingEvent, "Test purposes"));
        // non-revoked mapping timestamp 100-200
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        addBravoFixes();
        trackedRace.waitForLoadingToFinish();
        // Only Fixes from 100 to 200 may be included
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 2);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testLoadFixesWhenClosingEventIsRevoked() throws InterruptedException {
        UUID mappingEventId = UUID.randomUUID();
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(MillisecondsTimePoint.now(),
                new MillisecondsTimePoint(3), author, mappingEventId, comp, device, new MillisecondsTimePoint(START_OF_TRACKING),
                null));
        RegattaLogCloseOpenEndedDeviceMappingEventImpl closeEvent = new RegattaLogCloseOpenEndedDeviceMappingEventImpl(
                new MillisecondsTimePoint(4), author, mappingEventId, new MillisecondsTimePoint(MID_OF_TRACKING));
        regattaLog.add(closeEvent);
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        BravoFixTrack<Competitor> bravoFixTrack = trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME);
        testNumberOfRawFixes(bravoFixTrack, 2);
        regattaLog.add(new RegattaLogRevokeEventImpl(author, closeEvent, "Test purposes"));
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(bravoFixTrack, 3);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    public void testBravoFixIsCorrectlyWrapped() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        BravoFixTrack<Competitor> bravoFixTrack = trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME);
        assertEquals(FIX_RIDE_HEIGHT,
                bravoFixTrack.getFirstFixAtOrAfter(new MillisecondsTimePoint(FIX_TIMESTAMP)).getRideHeight());
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }
    
    @Test
    public void testMultipleFixTypesAreLoadedInSeparateTracks() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorTestMappingEventImpl(new MillisecondsTimePoint(1), author, comp,
                deviceTest, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        addTestFixes();
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 2);
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, TestFixTrackImpl.TRACK_NAME), 1);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }
    
    @Test
    public void testMultipleFixTypesAreLoadedInSeparateTracksWhileTracking() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorTestMappingEventImpl(new MillisecondsTimePoint(1), author, comp,
                deviceTest, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        addTestFixes();
        addBravoFixes();
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, TestFixTrackImpl.TRACK_NAME), 1);
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 2);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }
    
    @Test
    public void testMultipleFixTypesAreMappedCorrectly() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorTestMappingEventImpl(new MillisecondsTimePoint(1), author, comp,
                deviceTest, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        addTestFixes();
        addBravoFixes();
        trackedRace.waitForLoadingToFinish();
        BravoFixTrack<Competitor> bravoFixTrack = trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME);
        assertEquals(FIX_RIDE_HEIGHT,
                bravoFixTrack.getFirstFixAtOrAfter(new MillisecondsTimePoint(FIX_TIMESTAMP)).getRideHeight());
        assertEquals(FIX_RIDE_HEIGHT2,
                bravoFixTrack.getFirstFixAtOrAfter(new MillisecondsTimePoint(FIX_TIMESTAMP2)).getRideHeight());
        assertEquals(FIX_RIDE_HEIGHT3,
                bravoFixTrack.getFirstFixAtOrAfter(new MillisecondsTimePoint(FIX_TIMESTAMP3)).getRideHeight());
        TestFixTrackImpl<Competitor> testFixTrack = trackedRace.getSensorTrack(comp, TestFixTrackImpl.TRACK_NAME);
        assertEquals(FIX_TEST_VALUE,
                testFixTrack.getFirstFixAtOrAfter(new MillisecondsTimePoint(FIX_TIMESTAMP)).getTestValue(), 0.0000001);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    private void addBravoFixes() {
        store.storeFix(device, createBravoDoubleVectorFixWithRideHeight(FIX_TIMESTAMP, FIX_RIDE_HEIGHT.getMeters()));
        store.storeFix(device, createBravoDoubleVectorFixWithRideHeight(FIX_TIMESTAMP2, FIX_RIDE_HEIGHT2.getMeters()));
        store.storeFix(device, createBravoDoubleVectorFixWithRideHeight(FIX_TIMESTAMP3, FIX_RIDE_HEIGHT3.getMeters()));
    }

    private void addMoreBravoFixes() {
        store.storeFix(device, createBravoDoubleVectorFixWithRideHeight(FIX_TIMESTAMP + 1, FIX_RIDE_HEIGHT.getMeters()));
        store.storeFix(device, createBravoDoubleVectorFixWithRideHeight(FIX_TIMESTAMP2 + 1, FIX_RIDE_HEIGHT2.getMeters()));
        store.storeFix(device, createBravoDoubleVectorFixWithRideHeight(FIX_TIMESTAMP3 + 1, FIX_RIDE_HEIGHT3.getMeters()));
    }

    private FixLoaderAndTracker createFixLoaderAndTracker() {
        return new FixLoaderAndTracker(trackedRace, store, new SensorFixMapperFactory() {
            private TestDataFixMapper testDataFixMapper = new TestDataFixMapper();
            private BravoDataFixMapper bravoDataFixMapper = new BravoDataFixMapper();

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public <FixT extends Timed, TrackT extends DynamicTrack<FixT>> SensorFixMapper<FixT, TrackT, Competitor> createCompetitorMapper(
                    Class<? extends RegattaLogDeviceMappingEvent<?>> eventType) {
                if(bravoDataFixMapper.isResponsibleFor(eventType)) {
                    return (SensorFixMapper) bravoDataFixMapper;
                }
                if(testDataFixMapper.isResponsibleFor(eventType)) {
                    return (SensorFixMapper) testDataFixMapper;
                }
                throw new IllegalArgumentException("Unknown event type");
            }
        }, /* removeOutliersFromCompetitorTracks */ false);
    }

    protected void testNumberOfRawFixes(Track<?> track, long expected) {
        if (expected == 0) {
            if (track != null) {
                track.lockForRead();
                try {
                    assertTrue(size(track.getRawFixes()) == 0);
                } finally {
                    track.unlockAfterRead();
                }
            }
        } else {
            track.lockForRead();
            try {
                assertEquals(expected, size(track.getRawFixes()));
            } finally {
                track.unlockAfterRead();
            }
        }
    }

    private DoubleVectorFix createBravoDoubleVectorFixWithRideHeight(long timestamp, double rideHeight) {
        Double[] fixData = new Double[BravoSensorDataMetadata.getTrackColumnCount()];
        // fill the port/starboard columns as well because their minimum defines the true ride height
        fixData[BravoSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex()] = rideHeight;
        fixData[BravoSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex()] = rideHeight;
        return new DoubleVectorFixImpl(new MillisecondsTimePoint(timestamp), fixData);
    }
    
    private void addTestFixes() {
        store.storeFix(deviceTest, createTestDoubleVectorFixWithTestValue(FIX_TIMESTAMP, FIX_TEST_VALUE));
    }
    
    private DoubleVectorFix createTestDoubleVectorFixWithTestValue(long timestamp, double testValue) {
        Double[] fixData = new Double[TestFixImpl.COLUMNS.size()];
        fixData[TestFixImpl.TEST_COLUMN_INDEX] = testValue;
        return new DoubleVectorFixImpl(new MillisecondsTimePoint(timestamp), fixData);
    }
    
    private static class TestFixImpl implements SensorFix {
        private static final long serialVersionUID = 2033254212013220L;
        
        public static final String TEST_COLUMN = "testColumn";
        public static final int TEST_COLUMN_INDEX = 1;
        
        public static final List<String> COLUMNS = Collections.unmodifiableList(Arrays.asList("blub", TEST_COLUMN));
        
        private final DoubleVectorFix fix;

        public TestFixImpl(DoubleVectorFix fix) {
            this.fix = fix;
        }

        @Override
        public double get(String valueName) {
            return fix.get(COLUMNS.indexOf(valueName));
        }

        @Override
        public TimePoint getTimePoint() {
            return fix.getTimePoint();
        }

        public double getTestValue() {
            return fix.get(TEST_COLUMN_INDEX);
        }

        @Override
        public Double[] get() {
            return Arrays.copyOf(fix.get(), fix.get().length);
        }

    }
    
    public class TestFixTrackImpl<ItemType extends WithID & Serializable> extends SensorFixTrackImpl<ItemType, TestFixImpl> {
        private static final long serialVersionUID = 5517848726454386L;
        
        public static final String TRACK_NAME = "TestFixTrack";
        
        public TestFixTrackImpl(ItemType trackedItem, String trackName) {
            super(trackedItem, trackName, TRACK_NAME + " for " + trackedItem);
        }
        
        public Double getTextValue(TimePoint timePoint) {
            TestFixImpl fixAfter = getFirstFixAtOrAfter(timePoint);
            if (fixAfter != null && fixAfter.getTimePoint().compareTo(timePoint) == 0) {
                // exact match of timepoint -> no interpolation necessary
                return fixAfter.getTestValue();
            }
            TestFixImpl fixBefore = getLastFixAtOrBefore(timePoint);
            if (fixBefore != null && fixBefore.getTimePoint().compareTo(timePoint) == 0) {
                // exact match of timepoint -> no interpolation necessary
                return fixBefore.getTestValue();
            }
            if (fixAfter == null || fixBefore == null) {
                // the fix is out of the TimeRange where we have fixes
                return null;
            }
            return fixBefore.getTestValue();
        }
    }
    
    public class TestDataFixMapper implements SensorFixMapper<TestFixImpl, DynamicSensorFixTrack<Competitor, TestFixImpl>, Competitor> {

        @Override
        public DynamicSensorFixTrack<Competitor, TestFixImpl> getTrack(DynamicTrackedRace race, Competitor key) {
            return race.getOrCreateSensorTrack(key, TestFixTrackImpl.TRACK_NAME, 
                    () -> new TestFixTrackImpl<Competitor>(key, TestFixTrackImpl.TRACK_NAME));
        }
        
        @Override
        public TestFixImpl map(DoubleVectorFix fix) {
            return new TestFixImpl(fix);
        }
        
        @Override
        public boolean isResponsibleFor(Class<? extends RegattaLogDeviceMappingEvent<?>> eventType) {
            return RegattaLogDeviceCompetitorTestMappingEventImpl.class.isAssignableFrom(eventType);
        }
    }
    
    public class RegattaLogDeviceCompetitorTestMappingEventImpl extends RegattaLogDeviceMappingEventImpl<Competitor>
        implements RegattaLogDeviceCompetitorSensorDataMappingEvent {
        private static final long serialVersionUID = -14940305448048753L;
        
        
        public RegattaLogDeviceCompetitorTestMappingEventImpl(TimePoint createdAt, TimePoint logicalTimePoint,
                AbstractLogEventAuthor author, Serializable pId, Competitor mappedTo, DeviceIdentifier device,
                TimePoint from, TimePoint to) {
            super(createdAt, logicalTimePoint, author, pId, mappedTo, device, from, to);
        }
        
        public RegattaLogDeviceCompetitorTestMappingEventImpl(TimePoint logicalTimePoint, AbstractLogEventAuthor author,
                Competitor mappedTo, DeviceIdentifier device, TimePoint from, TimePoint to) {
            super(logicalTimePoint, author, mappedTo, device, from, to);
        }
        
        @Override
        public void accept(RegattaLogEventVisitor visitor) {
            visitor.visit(this);
        }
        
        @Override
        public void accept(MappingEventVisitor visitor) {
            visitor.visit(this);
        }
    }
    
    @Test
    public void testThatMappingsOutsideOfTheTrackedIntervalDontCauseLoadingToFail() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(10), new MillisecondsTimePoint(20)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp2,
                deviceTest, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(MID_OF_TRACKING)));
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp2,
                deviceTest, new MillisecondsTimePoint(10), new MillisecondsTimePoint(20)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        assertNotNull(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME));
        assertNotNull(trackedRace.getSensorTrack(comp2, BravoFixTrack.TRACK_NAME));
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }
    
    /** Regression test for bug 4052 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4052 */
    @Test
    public void testThatNoSensorFixesAreAddedToTrackOfCompetitorWhoIsntPartOfTheRace() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, compNotPartOfRace,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        assertNull(trackedRace.getSensorTrack(compNotPartOfRace, BravoFixTrack.TRACK_NAME));
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    /** Test for changes of bug 4044 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4044 */
    public void testThatNoSensorFixesAreLoadedAsLongAsStartOfTrackingIsNull() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        // raceLog is intentionally not attached
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        // No fixes are loaded because startOfTracking isn't set through the raceLog yet
        assertNull(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME));
        // Loading of fixes is triggered by setting startOfTracking
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(START_OF_TRACKING));
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 3);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    /** Test for changes of bug 4044 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4044 */
    public void testThatNoSensorFixesAreRecordedAsWhenStartOfTrackingIsNull() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        // raceLog is intentionally not attached
        trackedRace.attachRegattaLog(regattaLog);
        addBravoFixes();
        assertNull(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME));
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }

    @Test
    /** Test for changes of bug 4044 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4044 */
    public void testThatNoMoreSensorFixesAreLoadedWhenStartOfTrackingChangesToNull() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(START_OF_TRACKING));
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 3);
        raceLog.add(new RaceLogStartOfTrackingEventImpl(null, author, 0));
        assertNull(trackedRace.getStartOfTracking());
        addMoreBravoFixes();
        trackedRace.waitForLoadingToFinish();
        // only the initial 3 fixes are available
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 3);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> gpsFixTrack = trackedRace.getTrack(comp);
        final DegreePosition pos1 = new DegreePosition(0, 0);
        final DegreeBearingImpl course = new DegreeBearingImpl(0);
        final KnotSpeedWithBearingImpl speed = new KnotSpeedWithBearingImpl(10, course);
        final MillisecondsTimePoint timePoint = new MillisecondsTimePoint(FIX_TIMESTAMP);
        final MillisecondsTimePoint timePoint2 = new MillisecondsTimePoint(FIX_TIMESTAMP2);
        final MillisecondsTimePoint timePoint3 = new MillisecondsTimePoint(FIX_TIMESTAMP3);
        final TimePoint timePointBetween2And3 = new MillisecondsTimePoint((FIX_TIMESTAMP2+FIX_TIMESTAMP3)/2);
        final GPSFixMoving fix1 = new GPSFixMovingImpl(pos1, timePoint, speed, /* optionalTrueHeading */ null);
        final Position pos2 = pos1.translateGreatCircle(course, speed.travel(timePoint.until(timePoint2)));
        final GPSFixMoving fix2 = new GPSFixMovingImpl(pos2, timePoint2, speed, /* optionalTrueHeading */ null);
        final Position pos3 = pos2.translateGreatCircle(course, speed.travel(timePoint2.until(timePoint3)));
        final GPSFixMoving fix3 = new GPSFixMovingImpl(pos3, timePoint3, speed, /* optionalTrueHeading */ null);
        gpsFixTrack.add(fix1);
        gpsFixTrack.add(fix2);
        gpsFixTrack.add(fix3);
        final BravoFixTrack<Competitor> bravoFixTrack = trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME);
        final Distance foilingDistance = bravoFixTrack.getDistanceSpentFoiling(timePoint, timePoint3);
        final Distance distanceTraveled = gpsFixTrack.getDistanceTraveled(timePoint, timePoint3);
        assertEquals(distanceTraveled.getMeters(), foilingDistance.getMeters(), 0.001);
        final Duration foilingDuration = bravoFixTrack.getTimeSpentFoiling(timePoint, timePoint3);
        assertEquals(timePoint.until(timePoint3).asMillis(), foilingDuration.asMillis());
        
        // now insert a GPS fix which leads to an extended distance traveled as it is not exactly on the previous track
        final DegreeBearingImpl temporaryCourse = new DegreeBearingImpl(90);
        final GPSFixMoving fixBetween2And3 = new GPSFixMovingImpl(
                pos2.translateGreatCircle(temporaryCourse, speed.add(new KnotSpeedWithBearingImpl(5, temporaryCourse)).travel(timePoint2.until(timePointBetween2And3))),
                timePointBetween2And3, speed, /* optionalTrueHeading */ null);
        gpsFixTrack.add(fixBetween2And3);
        final Distance distanceTraveledDifferently = gpsFixTrack.getDistanceTraveled(timePoint, timePoint3);
        assertTrue(distanceTraveledDifferently.compareTo(distanceTraveled) > 0);
        final Distance foilingDistanceDifferent = bravoFixTrack.getDistanceSpentFoiling(timePoint, timePoint3);
        assertEquals(distanceTraveledDifferently.getMeters(), foilingDistanceDifferent.getMeters(), 0.001);
        
        // now overwrite fix3 by a new one with increased speed, therefore extended distance:
        final SpeedWithBearing doubledSpeed = speed.add(speed);
        final GPSFixMoving fix3Faster = new GPSFixMovingImpl(
                pos2.translateGreatCircle(course, doubledSpeed.travel(timePoint2.until(timePoint3))),
                timePoint3, doubledSpeed, /* optionalTrueHeading */ null);
        gpsFixTrack.add(fix3Faster, /* replace */ true);
        final Distance distanceTraveledFaster = gpsFixTrack.getDistanceTraveled(timePoint, timePoint3);
        assertTrue(distanceTraveledFaster.compareTo(distanceTraveled) > 0);
        final Distance foilingDistanceFaster = bravoFixTrack.getDistanceSpentFoiling(timePoint, timePoint3);
        assertEquals(distanceTraveledFaster.getMeters(), foilingDistanceFaster.getMeters(), 0.001);
    }
    
    @Test
    /** Test for changes of bug 4044 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4044 */
    public void testThatNoMoreSensorFixesAreLoadedWhenStartOfTrackingChangesFromNullBySettingStartTime() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        // creating a new RaceLog instance to ensure that startOfTracking insn't set initially
        raceLog = new RaceLogImpl("racelog");
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 0);
        raceLog.add(new RaceLogStartTimeEventImpl(new MillisecondsTimePoint(START_OF_TRACKING), author, 0, 
                new MillisecondsTimePoint(START_OF_TRACKING), /* courseAreaId */ null));
        assertNotNull(trackedRace.getStartOfTracking());
        trackedRace.waitForLoadingToFinish();
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 3);
        raceLog.add(new RaceLogStartOfTrackingEventImpl(null, author, 0));
        addMoreBravoFixes();
        trackedRace.waitForLoadingToFinish();
        // only the initial 3 fixes are available
        testNumberOfRawFixes(trackedRace.getSensorTrack(comp, BravoFixTrack.TRACK_NAME), 3);
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }
    
    @Timeout(value=10_000, unit=TimeUnit.SECONDS)
    @Test
    /** Test for regression introduced while working on bug 4125 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4125 */
    public void testPreemptiveStopDoesNotBlockThread() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        CyclicBarrier cb = new CyclicBarrier(2);
        // This listener enforces the race to stay in loading state until a preemptive stop is triggered below
        trackedRace.addListener(new AbstractRaceChangeListener() {
            @Override
            public void competitorSensorTrackAdded(DynamicSensorFixTrack<Competitor, ?> track) {
                try {
                    cb.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        final FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        // This thread solves the loading state through the CyclicBarrier
        new Thread() {
            public void run() {
                try {
                    while (!fixLoaderAndTracker.isStopRequested()) {
                        sleep(100);
                    }
                    cb.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }.start();
        // When the bug is triggered, this call would hang until the test timeout is reached
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
    }
    
    @Test
    /** Test for bug 4125 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4125 */
    public void testThatLoadingStateIsTriggeredOnInitialLoad() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        StatusTransitionListener statusTransitionListener = new StatusTransitionListener();
        trackedRace.addListener(statusTransitionListener);
        final FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
        statusTransitionListener.assertTransitions(TrackedRaceStatusEnum.PREPARED, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.FINISHED);
    }

    @Test
    /** Test for bug 4125 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4125 */
    public void testThatLoadingStateIsTriggeredWhenAddingMapping() throws InterruptedException {
        addBravoFixes();
        StatusTransitionListener statusTransitionListener = new StatusTransitionListener();
        trackedRace.addListener(statusTransitionListener);
        final FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRegattaLog(regattaLog);
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        trackedRace.waitForLoadingToFinish();
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
        statusTransitionListener.assertTransitions(TrackedRaceStatusEnum.PREPARED, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.FINISHED);
    }

    @Test
    /** Test for bug 4125 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4125 */
    public void testThatLoadingStateIsTriggeredWhenStartOfTrackingIsSet() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        StatusTransitionListener statusTransitionListener = new StatusTransitionListener();
        trackedRace.addListener(statusTransitionListener);
        final FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        // raceLog is intentionally not attached
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(START_OF_TRACKING));
        trackedRace.waitForLoadingToFinish();
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
        statusTransitionListener.assertTransitions(TrackedRaceStatusEnum.PREPARED, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.FINISHED);
    }
    
    @Test
    /** Test for bug 4125 - https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4125 */
    public void testThatLoadingStateIsTriggeredWhenStartOfTrackingChanges() throws InterruptedException {
        regattaLog.add(new RegattaLogDeviceCompetitorBravoMappingEventImpl(new MillisecondsTimePoint(3), author, comp,
                device, new MillisecondsTimePoint(START_OF_TRACKING), new MillisecondsTimePoint(END_OF_TRACKING)));
        addBravoFixes();
        StatusTransitionListener statusTransitionListener = new StatusTransitionListener();
        trackedRace.addListener(statusTransitionListener);
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(END_OF_TRACKING));
        final FixLoaderAndTracker fixLoaderAndTracker = createFixLoaderAndTracker();
        // raceLog is intentionally not attached
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.waitForLoadingToFinish();
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(START_OF_TRACKING));
        trackedRace.waitForLoadingToFinish();
        fixLoaderAndTracker.stop(true, /* willBeRemoved */ false);
        statusTransitionListener.assertTransitions(TrackedRaceStatusEnum.PREPARED, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.LOADING, TrackedRaceStatusEnum.TRACKING, TrackedRaceStatusEnum.FINISHED);
    }

    private class StatusTransitionListener extends AbstractRaceChangeListener {
        List<Pair<TrackedRaceStatusEnum, TrackedRaceStatusEnum>> transitions = new ArrayList<>();
        
        @Override
        public void statusChanged(TrackedRaceStatus newStatus, TrackedRaceStatus oldStatus) {
            TrackedRaceStatusEnum oldStatusEnum = oldStatus.getStatus();
            TrackedRaceStatusEnum newStatusEnum = newStatus.getStatus();
            if (oldStatusEnum != newStatusEnum) {
                transitions.add(new Pair<>(oldStatusEnum, newStatusEnum));
            }
        }
        
        public void assertTransitions(TrackedRaceStatusEnum... expectedStates) {
            TrackedRaceStatusEnum oldValue = null;
            Iterator<Pair<TrackedRaceStatusEnum, TrackedRaceStatusEnum>> recordedTransitionsIterator = transitions.iterator();
            for (TrackedRaceStatusEnum newStatusEnum : expectedStates) {
                if (oldValue != null) {
                    Pair<TrackedRaceStatusEnum, TrackedRaceStatusEnum> recordedTransition = recordedTransitionsIterator.next();
                    assertEquals(oldValue, recordedTransition.getA());
                    assertEquals(newStatusEnum, recordedTransition.getB());
                }
                oldValue = newStatusEnum;
            }
        }
    }
}
