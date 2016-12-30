package com.sap.sailing.domain.racelogtracking.test.impl;

import static com.sap.sse.common.Util.size;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogUseCompetitorsFromRaceLogEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelog.tracking.test.mock.SmartphoneImeiIdentifier;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.racelogtracking.impl.fixtracker.RaceLogFixTrackerManager;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.Track;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class CreateAndTrackWithRaceLogTest {
    private RacingEventService service;

    private final Fleet fleet = new FleetImpl("fleet");
    private final String columnName = "column";
    private RegattaLeaderboard leaderboard;
    private RaceLogTrackingAdapter adapter;
    private Regatta regatta;
    private AbstractLogEventAuthor author;
    private SensorFixStore sensorFixStore;

    private long time = 0;

    @Rule
    public Timeout CreateAndTrackWithRaceLogTestTimeout = new Timeout(3 * 60 * 1000);

    @Before
    public void setup() {
        service = new RacingEventServiceImpl(true, new MockSmartphoneImeiServiceFinderFactory());
        sensorFixStore = service.getSensorFixStore();
        service.getMongoObjectFactory().getDatabase().dropDatabase();
        author = service.getServerAuthor();
        Series series = new SeriesImpl("series", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, Collections.singletonList(fleet), Collections.emptySet(),
                service);
        regatta = service.createRegatta(RegattaImpl.getDefaultName("regatta", "Laser"), "Laser",
                /* canBoatsOfCompetitorsChangePerRace */ true, /* startDate */null, /* endDate */null,
                UUID.randomUUID(), Collections.<Series> singletonList(series), false,
                new HighPoint(), UUID.randomUUID(), /*buoyZoneRadiusInHullLengths*/2.0, /* useStartTimeInference */true, /* controlTrackingFromStartAndFinishTimes */ false, OneDesignRankingMetric::new);
        series.addRaceColumn(columnName, /* trackedRegattaRegistry */null);
        leaderboard = service.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "RegattaLeaderboard", new int[] {});
        adapter = RaceLogTrackingAdapterFactory.INSTANCE.getAdapter(DomainFactory.INSTANCE);
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

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void cantAddBeforeDenoting() throws MalformedURLException, FileNotFoundException, URISyntaxException,
            Exception {
        RaceColumn column = leaderboard.getRaceColumnByName(columnName);
        exception.expect(NotDenotedForRaceLogTrackingException.class);
        adapter.startTracking(service, leaderboard, column, fleet);
    }

    private void testSize(Track<?> track, int expected) {
        track.lockForRead();
        assertEquals(expected, size(track.getRawFixes()));
        track.unlockAfterRead();
    }

    private void addFixes0(DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(5), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5))));
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(15), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5))));
    }

    private void addFixes1(TrackedRace race, Competitor comp1, DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        // one fix should have been loaded from store
        testSize(race.getTrack(comp1), 1);

        // further fix arrives in race
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(7), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5))));
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(14), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5)))); // outside mapping range
        testSize(race.getTrack(comp1), 2);
    }

    private void addFixes2(TrackedRace race, Competitor comp1, DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        // add another mapping on the fly, other old fixes should be loaded
        testSize(race.getTrack(comp1), 4);

        // add another fix in new mapping range
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(18), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5))));
        testSize(race.getTrack(comp1), 5);
    }

    private void addFixes3(TrackedRace race, Competitor comp1, DeviceIdentifier dev1) throws TransformationException,
            NoCorrespondingServiceRegisteredException {
        // stop tracking, then no more fixes arrive at race
        sensorFixStore.storeFix(dev1, new GPSFixMovingImpl(new DegreePosition(0, 0), t(8), new KnotSpeedWithBearingImpl(
                10, new DegreeBearingImpl(5))));
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
        Competitor comp1 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp1", "comp1", "c", null, null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DeviceIdentifier dev1 = new SmartphoneImeiIdentifier("dev1");
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, 0, comp1, dev1, t(0), t(10)));
        addFixes0(dev1);
        raceLog.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(t(), author, t(), UUID.randomUUID(), 0));
        raceLog.add(new RaceLogRegisterCompetitorEventImpl(t(), author, 0, comp1));
        raceLog.add(new RaceLogStartOfTrackingEventImpl(t(0), author, /* passId */ 0));
        // start tracking
        adapter.startTracking(service, leaderboard, column, fleet);
        

        // now there is a tracked race
        TrackedRace race = column.getTrackedRace(fleet);
        assertNotNull(race);
        
        RaceLogFixTrackerManager raceLogFixTrackerManager = new RaceLogFixTrackerManager((DynamicTrackedRace) race, sensorFixStore, null);

        race.waitForLoadingToFinish();
        addFixes1(race, comp1, dev1);
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, 0, comp1, dev1, t(11), t(20)));

        race.waitForLoadingToFinish();
        // add another mapping on the fly, other old fixes should be loaded
        addFixes2(race, comp1, dev1);

        // stop tracking, then no more fixes arrive at race
        service.getRaceTrackerById(raceLog.getId()).stop(false);
        raceLogFixTrackerManager.stop(false);
        addFixes3(race, comp1, dev1);
    }

    @Test
    public void useEventsInRegattaLog() throws NotDenotedForRaceLogTrackingException, Exception {
        RaceColumn column = leaderboard.getRaceColumnByName(columnName);
        RegattaLog regattaLog = leaderboard.getRegattaLike().getRegattaLog();
        RaceLog raceLog = column.getRaceLog(fleet);

        adapter.denoteRaceForRaceLogTracking(service, leaderboard, column, fleet, "race");

        // add a mapping and one fix in, one out of mapping
        Competitor comp1 = DomainFactory.INSTANCE.getOrCreateCompetitor("comp1", "comp1", "c1", null, null, null, null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        DeviceIdentifier dev1 = new SmartphoneImeiIdentifier("dev1");
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, UUID.randomUUID(), comp1, dev1,
                t(0), t(10)));
        addFixes0(dev1);
        regattaLog.add(new RegattaLogRegisterCompetitorEventImpl(t(), t(), author, UUID.randomUUID(), comp1));
        raceLog.add(new RaceLogStartOfTrackingEventImpl(t(0), author, /* passId */ 0));

        // start tracking
        adapter.startTracking(service, leaderboard, column, fleet);

        // now there is a trackedrace
        TrackedRace race = column.getTrackedRace(fleet);
        assertNotNull(race);
        
        RaceLogFixTrackerManager raceLogFixTrackerManager = new RaceLogFixTrackerManager((DynamicTrackedRace) race, sensorFixStore, null);

        race.waitForLoadingToFinish();
        addFixes1(race, comp1, dev1);

        // add another mapping on the fly, other old fixes should be loaded
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(t(), t(), author, UUID.randomUUID(), comp1, dev1,
                t(11), t(20)));
        race.waitForLoadingToFinish();
        addFixes2(race, comp1, dev1);

        // stop tracking, then no more fixes arrive at race
        service.getRaceTrackerById(raceLog.getId()).stop(/* preemptive */ false);
        raceLogFixTrackerManager.stop(false);
        addFixes3(race, comp1, dev1);
    }
}
