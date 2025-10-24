package com.sap.sailing.domain.racelogtracking.test.impl;

import static com.sap.sse.common.Util.size;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogUseCompetitorsFromRaceLogEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.CollectionNames;
import com.sap.sailing.domain.persistence.racelog.tracking.MongoSensorFixStoreFactory;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.impl.fixtracker.RaceLogFixTrackerManager;
import com.sap.sailing.domain.racelogtracking.test.RaceLogTrackingTestHelper;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.util.WaitForTrackedRaceUtil;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;

@Timeout(value=3, unit=TimeUnit.MINUTES)
public class CreateAndTrackWithRaceLogTest extends RaceLogTrackingTestHelper {
    private RacingEventService service;
    private final static BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */ true);
    private final Fleet fleet = new FleetImpl("fleet");
    private final String columnName = "column";
    private RegattaLeaderboard leaderboard;
    private RaceLogTrackingAdapter adapter;
    private Regatta regatta;
    private SensorFixStore sensorFixStore;

    private long time = 0;

    @BeforeEach
    public void setup() throws UnknownHostException, MongoException {
        final ClientSession clientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
        final ClientSession metadataCollectionClientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
        final MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
        final MongoObjectFactory defaultMongoObjectFactory = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(serviceFinderFactory);
        defaultMongoObjectFactory.getDatabase().drop(clientSession);
        // ensure that the drop has gone through:
        defaultMongoObjectFactory.getDatabase().getCollection(CollectionNames.REGATTAS.name()).find(clientSession);
        defaultMongoObjectFactory.getDatabase().getCollection(CollectionNames.LEADERBOARDS.name()).find(clientSession);
        service = new RacingEventServiceImpl(/* clearPersistentCompetitorStore */ true,
                MongoSensorFixStoreFactory.INSTANCE.getMongoGPSFixStore(
                        defaultMongoObjectFactory,
                        PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), serviceFinderFactory,
                        ReadConcern.MAJORITY, WriteConcern.MAJORITY, clientSession, metadataCollectionClientSession),
                serviceFinderFactory, /* restoreTrackedRaces */ false);
        sensorFixStore = service.getSensorFixStore();
        author = service.getServerAuthor();
        Series series = new SeriesImpl("series", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, Collections.singletonList(fleet), Collections.emptySet(), service);
        regatta = service.createRegatta(RegattaImpl.getDefaultName("regatta", "Laser"), "Laser",
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */null, /* endDate */null, UUID.randomUUID(),
                Collections.<Series> singletonList(series), /* persistent */ true, new HighPoint(),
                service.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "Default", /* centerPosition */ null, /* radius */ null).getId(),
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        series.addRaceColumn(columnName, /* trackedRegattaRegistry */null);
        leaderboard = service.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "RegattaLeaderboard", new int[] {});
        adapter = RaceLogTrackingAdapterFactory.INSTANCE.getAdapter(DomainFactory.INSTANCE);
        DomainFactory.INSTANCE.getCompetitorAndBoatStore().clear();
    }
    
    @AfterEach
    public void tearDown() throws MalformedURLException, IOException, InterruptedException {
        service.removeRegatta(regatta);
    }

    @Test
    public void hasRaceLog() {
        assertNotNull(leaderboard.getRaceColumnByName(columnName).getRaceLog(fleet));
    }

    private TimePoint t() {
        return new MillisecondsTimePoint(time++);
    }

    private TimePoint t(long millis) {
        return new MillisecondsTimePoint(millis);
    }

    @Test
    public void cantAddBeforeDenoting() throws MalformedURLException, FileNotFoundException, URISyntaxException,
            Exception {
        RaceColumn column = leaderboard.getRaceColumnByName(columnName);
        Assertions.assertThrows(NotDenotedForRaceLogTrackingException.class, ()->trackAndGetRace(column));
    }

    private void testSize(Track<?> track, int expected) {
        track.lockForRead();
        try {
            assertEquals(expected, size(track.getRawFixes()));
        } finally {
            track.unlockAfterRead();
        }
    }

    private void addFixes0(DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(5), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5)), /* optionalTrueHeading */ null));
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(15), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5)), /* optionalTrueHeading */ null));
    }

    private void addFixes1(TrackedRace race, Competitor comp1, DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        // one fix should have been loaded from store
        testSize(race.getTrack(comp1), 1);
        // further fix arrives in race
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(7), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5)), /* optionalTrueHeading */ null));
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(14), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5)), /* optionalTrueHeading */ null)); // outside mapping range
        testSize(race.getTrack(comp1), 2);
    }

    private void addFixes2(TrackedRace race, Competitor comp1, DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        // add another mapping on the fly, other old fixes should be loaded
        testSize(race.getTrack(comp1), 4);
        // add another fix in new mapping range
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(18), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5)), /* optionalTrueHeading */ null));
        testSize(race.getTrack(comp1), 5);
    }

    private void addFixes3(TrackedRace race, Competitor comp1, DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        // stop tracking, then no more fixes arrive at race
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(8), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5)), /* optionalTrueHeading */ null));
        testSize(race.getTrack(comp1), 5);
    }

    @Test
    public void canDenote_Add_Track() throws MalformedURLException, FileNotFoundException, URISyntaxException,
            Exception {
        RaceColumn column = leaderboard.getRaceColumnByName(columnName);
        RegattaLog regattaLog = leaderboard.getRegattaLike().getRegattaLog();
        RaceLog raceLog = column.getRaceLog(fleet);
        // can denote racelog for tracking
        assertTrue(raceLog.isEmpty());
        adapter.denoteRaceForRaceLogTracking(service, leaderboard, column, fleet, "race");
        assertFalse(raceLog.isEmpty());
        // add a mapping and one fix in, one out of mapping
        Boat boat1 = new BoatImpl("id12345", "boat1", boatClass, /* sailID */ null);
        Competitor comp1 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp1", "comp1", "c", null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        DeviceIdentifier dev1 = new SmartphoneImeiIdentifierImpl("dev1");
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, 0, comp1, dev1, t(0), t(10)));
        addFixes0(dev1);
        raceLog.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(t(), author, t(), UUID.randomUUID(), 0));
        raceLog.add(new RaceLogRegisterCompetitorEventImpl(t(), author, 0, comp1, boat1));
        raceLog.add(new RaceLogStartOfTrackingEventImpl(t(0), author, /* passId */ 0));
        // start tracking
        TrackedRace race = trackAndGetRace(column);
        assertNotNull(race);
        RaceLogFixTrackerManager raceLogFixTrackerManager = new RaceLogFixTrackerManager((DynamicTrackedRace) race,
                sensorFixStore, null, /* removeOutliersFromCompetitorTracks */ false);
        raceLogFixTrackerManager.waitForTracker();
        race.waitForLoadingToFinish();
        addFixes1(race, comp1, dev1);
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, 0, comp1, dev1, t(11), t(20)));
        race.waitForLoadingToFinish();
        // add another mapping on the fly, other old fixes should be loaded
        addFixes2(race, comp1, dev1);
        // stop tracking, then no more fixes arrive at race
        service.getRaceTrackerById(raceLog.getId()).stop(false);
        raceLogFixTrackerManager.stop(/* preemptive */ false, /* willBeRemoved */ false);
        addFixes3(race, comp1, dev1);
    }

    private TrackedRace trackAndGetRace(RaceColumn column) throws NotDenotedForRaceLogTrackingException, Exception {
        final RaceHandle raceHandle = adapter.startTracking(service, leaderboard, column, fleet, /* trackWind */ false, /* correctWindDirectionByMagneticDeclination */ false,
                new DefaultRaceTrackingHandler());
        raceHandle.getRace();
        return WaitForTrackedRaceUtil.waitForTrackedRace(column, fleet, 10);
    }
    
    /**
     * See bug 4114: when the regatta is set to control the tracking times from start / finish times,
     * we don't want this to override start/end tracking times set explicitly in the race log unless
     * they have low priority. This could happen when the start time is adjusted manually or when
     * the race log is detached / attached again with a start time event in the race log.
     */
    @Test
    public void testStartOfTrackingTimes() throws NotDenotedForRaceLogTrackingException, Exception {
        regatta.setControlTrackingFromStartAndFinishTimes(true);
        RaceColumn column = leaderboard.getRaceColumnByName(columnName);
        raceLog = column.getRaceLog(fleet);
        adapter.denoteRaceForRaceLogTracking(service, leaderboard, column, fleet, "race");
        // start tracking
        TrackedRace race = trackAndGetRace(column);
        assertNotNull(race);
        final TimePoint startTimeInRaceLog = new MillisecondsTimePoint(123456);
        final TimePoint endTimeInRaceLog = new MillisecondsTimePoint(234567);
        setStartAndEndOfRaceInRaceLog(startTimeInRaceLog, endTimeInRaceLog);
        assertEquals(race.getStartOfRace().minus(TrackedRace.START_TRACKING_THIS_MUCH_BEFORE_RACE_START), race.getStartOfTracking());
        final TimePoint explicitStartOfTracking = new MillisecondsTimePoint(123000); // just a bit before start of race
        final TimePoint explicitEndOfTracking = new MillisecondsTimePoint(234600); // just a bit after end of race
        setStartAndEndOfTrackingInRaceLog(explicitStartOfTracking, explicitEndOfTracking);
        assertEquals(explicitStartOfTracking, race.getStartOfTracking());
        assertEquals(explicitEndOfTracking, race.getEndOfTracking());
        column.releaseTrackedRace(fleet);
        column.setTrackedRace(fleet, race);
        assertEquals(explicitStartOfTracking, race.getStartOfTracking());
        assertEquals(explicitEndOfTracking, race.getEndOfTracking());
        
        // now remove the tracked race and track again; again, the explicit tracking times in the race log must prevail
        service.removeRace(regatta, race.getRace());
        TrackedRace race2 = trackAndGetRace(column);
        assertEquals(explicitStartOfTracking, waitForStartOfTracking(race2));
        assertEquals(explicitEndOfTracking, race2.getEndOfTracking());
    }

    @Test
    public void useEventsInRegattaLog() throws NotDenotedForRaceLogTrackingException, Exception {
        RaceColumn column = leaderboard.getRaceColumnByName(columnName);
        RegattaLog regattaLog = leaderboard.getRegattaLike().getRegattaLog();
        raceLog = column.getRaceLog(fleet);
        adapter.denoteRaceForRaceLogTracking(service, leaderboard, column, fleet, "race");
        // add a mapping and one fix in, one out of mapping
        Boat boat1 = new BoatImpl("id12345", "boat1", boatClass, /* sailID */ null);
        CompetitorWithBoat comp1 = DomainFactory.INSTANCE.getOrCreateCompetitorWithBoat("comp1", "comp1", "c1", null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, (DynamicBoat) boat1, /* storePersistently */ true);
        DeviceIdentifier dev1 = new SmartphoneImeiIdentifierImpl("dev1");
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, UUID.randomUUID(), comp1, dev1,
                t(0), t(10)));
        addFixes0(dev1);
        regattaLog.add(new RegattaLogRegisterCompetitorEventImpl(t(), t(), author, UUID.randomUUID(), comp1));
        raceLog.add(new RaceLogStartOfTrackingEventImpl(t(0), author, /* passId */ 0));
        TrackedRace race = trackAndGetRace(column);
        assertNotNull(race);
        RaceLogFixTrackerManager raceLogFixTrackerManager = new RaceLogFixTrackerManager((DynamicTrackedRace) race,
                sensorFixStore, null, /* removeOutliersFromCompetitorTracks */ false);
        raceLogFixTrackerManager.waitForTracker();
        race.waitForLoadingToFinish();
        addFixes1(race, comp1, dev1);
        // add another mapping on the fly, other old fixes should be loaded
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, UUID.randomUUID(), comp1, dev1,
                t(11), t(20)));
        race.waitForLoadingToFinish();
        addFixes2(race, comp1, dev1);
        // stop tracking, then no more fixes arrive at race
        service.getRaceTrackerById(raceLog.getId()).stop(/* preemptive */ false);
        raceLogFixTrackerManager.stop(false, /* willBeRemoved */ false);
        addFixes3(race, comp1, dev1);
    }
    

    /**
     * Waits for startOfTracking being available for a given {@link TrackedRace}.
     */
    public static TimePoint waitForStartOfTracking(TrackedRace trackedRace) {
        final CompletableFuture<TimePoint> future = new CompletableFuture<>();

        final AbstractRaceChangeListener listener = new AbstractRaceChangeListener() {
            @Override
            public void startOfTrackingChanged(TimePoint oldStartOfTracking, TimePoint newStartOfTracking) {
                if (newStartOfTracking != null) {
                    future.complete(newStartOfTracking);
                    trackedRace.removeListener(this);
                }
            }
        };
        trackedRace.addListener(listener);
        final TimePoint startOfTracking = trackedRace.getStartOfTracking();
        if (startOfTracking != null) {
            trackedRace.removeListener(listener);
            return startOfTracking;
        }

        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            trackedRace.removeListener(listener);
            return null;
        }
    }
}
