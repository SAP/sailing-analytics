package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.persistence.MongoWindStore;
import com.sap.sailing.domain.persistence.MongoWindStoreFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.test.PositionAssert;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.CompetitorBravoFixTrackImpl;
import com.sap.sailing.server.operationaltransformation.AddDefaultRegatta;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.TrackRegatta;
import com.sap.sailing.server.operationaltransformation.UpdateStartOfTracking;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TrackedRaceContentsReplicationTest extends AbstractServerReplicationTest {
    private Competitor competitor;
    private DynamicTrackedRace trackedRace;
    private RegattaNameAndRaceName raceIdentifier;
    private DynamicTrackedRegatta trackedRegatta;
    
    @BeforeEach
    public void setUp() throws Exception, UnknownHostException, InterruptedException {
        super.setUp();
        final String boatClassName = "49er";
        final DomainFactory masterDomainFactory = testSetUp.getMaster().getBaseDomainFactory();
        BoatClass boatClass = masterDomainFactory.getOrCreateBoatClass(boatClassName, /* typicallyStartsUpwind */true);
        competitor = masterDomainFactory.getCompetitorAndBoatStore().getOrCreateCompetitor("GER 61", "Tina Lutz", "TL", Color.RED, "someone@nowhere.de", null, new TeamImpl("Tina Lutz + Susann Beucke",
                (List<PersonImpl>) Arrays.asList(new PersonImpl[] { new PersonImpl("Tina Lutz", masterDomainFactory.getOrCreateNationality("GER"), null, null),
                new PersonImpl("Tina Lutz", masterDomainFactory.getOrCreateNationality("GER"), null, null) }),
                new PersonImpl("Rigo de Mas", masterDomainFactory.getOrCreateNationality("NED"), null, null)),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat = masterDomainFactory.getCompetitorAndBoatStore().getOrCreateBoat("boat123", "boat123",
                masterDomainFactory.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true), "GER 61", null, /* storePersistently */ true);
        Map<Competitor, Boat> competitorAndBoats = new HashMap<>();
        competitorAndBoats.put(competitor, boat);
        final String baseEventName = "Test Event";
        AddDefaultRegatta addEventOperation = new AddDefaultRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, 
                /*startDate*/ null, /*endDate*/ null, UUID.randomUUID());
        Regatta regatta = master.apply(addEventOperation);
        final String raceName = "Test Race";
        final CourseImpl masterCourse = new CourseImpl("Test Course", new ArrayList<Waypoint>());
        RaceDefinition race = new RaceDefinitionImpl(raceName, masterCourse, boatClass, competitorAndBoats);
        AddRaceDefinition addRaceOperation = new AddRaceDefinition(new RegattaName(regatta.getName()), race);
        master.apply(addRaceOperation);
        masterCourse.addWaypoint(0, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark1"), /*passingInstruction*/ null));
        masterCourse.addWaypoint(1, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark2"), /*passingInstruction*/ null));
        masterCourse.addWaypoint(2, masterDomainFactory.createWaypoint(masterDomainFactory.getOrCreateMark("Mark3"), /*passingInstruction*/ null));
        masterCourse.removeWaypoint(1);
        raceIdentifier = new RegattaNameAndRaceName(regatta.getName(), raceName);
        trackedRegatta = master.apply(new TrackRegatta(raceIdentifier));
        trackedRace = (DynamicTrackedRace) master.apply(new CreateTrackedRace(raceIdentifier,
                MongoWindStoreFactory.INSTANCE.getMongoWindStore(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(),
                        PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory()), /* delayToLiveInMillis */ 5000,
                /* millisecondsOverWhichToAverageWind */ 10000, /* millisecondsOverWhichToAverageSpeed */10000, null));
        master.apply(new UpdateStartOfTracking(raceIdentifier, new MillisecondsTimePoint(0)));
        trackedRace.waitUntilLoadingFromWindStoreComplete();
    }
    
    protected Competitor getCompetitor() {
        return competitor;
    }

    protected DynamicTrackedRace getTrackedRace() {
        return trackedRace;
    }

    protected RegattaNameAndRaceName getRaceIdentifier() {
        return raceIdentifier;
    }

    protected DynamicTrackedRegatta getTrackedRegatta() {
        return trackedRegatta;
    }

    @Test
    public void testGPSFixReplication() throws InterruptedException {
        final GPSFixMovingImpl fix = new GPSFixMovingImpl(new DegreePosition(1, 2), new MillisecondsTimePoint(12345),
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ new DegreeBearingImpl(124));
        trackedRace.recordFix(competitor, fix);
        Thread.sleep(1000);
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        Competitor replicaCompetitor = replicaTrackedRace.getRace().getCompetitors().iterator().next();
        assertNotNull(replicaCompetitor);
        GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = replicaTrackedRace.getTrack(replicaCompetitor);
        competitorTrack.lockForRead();
        try {
            assertEquals(1, Util.size(competitorTrack.getRawFixes()));
            final GPSFixMoving replicatedFix = competitorTrack.getRawFixes().iterator().next();
            PositionAssert.assertGPSFixEquals(fix, replicatedFix, /* pos deg delta */ 0.0000001, /* bearing deg delta */ 0.01, /* knot speed delta */ 0.01);
            assertEquals(fix.getOptionalTrueHeading().getDegrees(), replicatedFix.getOptionalTrueHeading().getDegrees(), 0.01);
            assertNotSame(fix, replicatedFix);
        } finally {
            competitorTrack.unlockAfterRead();
        }
    }

    /**
     * See also bug4387: test that after replicating the TrackedRace the CompetitorBravoFixTrackImpl's gpsTrack object
     * points to the track from the TrackedRace to which the CompetitorBravoFixTrackImpl belongs.
     */
    @Test
    public void testBravoFixTrackReplication() throws InterruptedException {
        trackedRace.getOrCreateSensorTrack(competitor, BravoFixTrack.TRACK_NAME, () -> new CompetitorBravoFixTrackImpl(competitor, BravoFixTrack.TRACK_NAME, /* hasExtendedFixes */ false,
                trackedRace.getTrack(competitor)));
        Thread.sleep(1000);
        DynamicTrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        Competitor replicaCompetitor = replicaTrackedRace.getRace().getCompetitors().iterator().next();
        assertNotNull(replicaCompetitor);
        GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = replicaTrackedRace.getTrack(replicaCompetitor);
        CompetitorBravoFixTrackImpl competitorSensorTrack = replicaTrackedRace.getOrCreateSensorTrack(
                replicaCompetitor, BravoFixTrack.TRACK_NAME, () -> new CompetitorBravoFixTrackImpl(replicaCompetitor, BravoFixTrack.TRACK_NAME, /* hasExtendedFixes */ false,
                        replicaTrackedRace.getTrack(replicaCompetitor)));
        assertSame(competitorTrack, competitorSensorTrack.getGpsTrack());
    }

    @Test
    public void testMarkFixReplication() throws InterruptedException {
        final GPSFixMovingImpl fix = new GPSFixMovingImpl(new DegreePosition(2, 3), new MillisecondsTimePoint(3456),
                new KnotSpeedWithBearingImpl(13, new DegreeBearingImpl(234)), /* optionalTrueHeading */ null);
        final Mark masterMark = trackedRace.getRace().getCourse().getFirstWaypoint().getMarks().iterator().next();
        trackedRace.recordFix(masterMark, fix);
        Thread.sleep(1000);
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        Mark replicaMark = replicaTrackedRace.getRace().getCourse().getFirstWaypoint().getMarks().iterator().next();
        assertNotSame(replicaMark, masterMark);
        GPSFixTrack<Mark, GPSFix> replicaMarkTrack = replicaTrackedRace.getOrCreateTrack(replicaMark);
        replicaMarkTrack.lockForRead();
        try {
            assertEquals(1, Util.size(replicaMarkTrack.getRawFixes()));
            PositionAssert.assertGPSFixEquals(replicaMarkTrack.getRawFixes().iterator().next(), fix, /* pos deg delta */ 0.0000001);
            assertNotSame(fix, replicaMarkTrack.getRawFixes().iterator().next());
        } finally {
            replicaMarkTrack.unlockAfterRead();
        }
    }

    @Test
    public void testWindAdditionReplication() throws InterruptedException {
        TimePoint now = MillisecondsTimePoint.now();
        trackedRace.setStartOfTrackingReceived(now);
        final Wind wind = new WindImpl(new DegreePosition(2, 3), new MillisecondsTimePoint(now.asMillis()+1234),
                new KnotSpeedWithBearingImpl(13, new DegreeBearingImpl(234)));
        WindSource webWindSource = new WindSourceImpl(WindSourceType.WEB);
        trackedRace.recordWind(wind, webWindSource);
        Thread.sleep(1000);
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        WindTrack replicaWindTrack = replicaTrackedRace.getOrCreateWindTrack(replicaTrackedRace
                .getWindSources(WindSourceType.WEB).iterator().next());
        replicaWindTrack.lockForRead();
        try {
            assertEquals(1, Util.size(replicaWindTrack.getRawFixes()));
            Wind replicaWind = replicaWindTrack.getRawFixes().iterator().next();
            PositionAssert.assertWindEquals(wind, replicaWind, /* pos deg delta */ 0.0000001, /* bearing deg delta */ 0.02, /* knot speed delta */ 0.02);
            assertNotSame(wind, replicaWind);
        } finally {
            replicaWindTrack.unlockAfterRead();
        }
    }

    @Test
    public void testWindRemovalReplication() throws InterruptedException {
        final Wind wind = new WindImpl(new DegreePosition(2, 3), new MillisecondsTimePoint(3456),
                new KnotSpeedWithBearingImpl(13, new DegreeBearingImpl(234)));
        WindSource webWindSource = new WindSourceImpl(WindSourceType.WEB);
        trackedRace.recordWind(wind, webWindSource);
        Thread.sleep(500); // wind addition and wind removal are asynchronous operations that could otherwise pass each other
        trackedRace.removeWind(wind, webWindSource);
        final WindTrack windTrack = trackedRace.getOrCreateWindTrack(webWindSource);
        windTrack.lockForRead();
        try {
            assertEquals(0, Util.size(windTrack.getRawFixes()));
        } finally {
            windTrack.unlockAfterRead();
        }
        Thread.sleep(1000);
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        WindTrack replicaWindTrack = replicaTrackedRace.getOrCreateWindTrack(replicaTrackedRace
                .getWindSources(WindSourceType.WEB).iterator().next());
        replicaWindTrack.lockForRead();
        try {
            assertEquals(0, Util.size(replicaWindTrack.getRawFixes()));
        } finally {
            replicaWindTrack.unlockAfterRead();
        }
    }
    
    @Test
    public void testReplicationOfLoadingOfStoredWindTrack() throws UnknownHostException, MongoException, InterruptedException {
        TimePoint now = MillisecondsTimePoint.now();
        trackedRace.setStartOfTrackingReceived(now);
        MongoWindStore windStore = MongoWindStoreFactory.INSTANCE.getMongoWindStore(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory());
        WindSource webWindSource = new WindSourceImpl(WindSourceType.WEB);
        WindTrack windTrack = windStore.getWindTrack(trackedRegatta.getRegatta().getName(), trackedRace, webWindSource, /* millisecondsOverWhichToAverage */ 10000,
                /* delayForWindEstimationCacheInvalidation */ 10000);
        final Wind wind = new WindImpl(new DegreePosition(2, 3), new MillisecondsTimePoint(now.asMillis()+1234),
                new KnotSpeedWithBearingImpl(13, new DegreeBearingImpl(234)));
        windTrack.add(wind);
        Thread.sleep(1000); // give MongoDB time to read its own writes in a separate session
        WindTrack trackedRaceWebWindTrack = trackedRace.getOrCreateWindTrack(webWindSource);
        windTrack.lockForRead();
        trackedRaceWebWindTrack.lockForRead();
        try {
            assertEquals(Util.size(windTrack.getRawFixes()), Util.size(trackedRaceWebWindTrack.getRawFixes()));
            assertEquals(windTrack.getRawFixes().iterator().next(), trackedRaceWebWindTrack.getRawFixes().iterator().next());
            Thread.sleep(1000); // wait for replication to happen
            TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
            WindTrack replicaWindTrack = replicaTrackedRace.getOrCreateWindTrack(replicaTrackedRace
                    .getWindSources(WindSourceType.WEB).iterator().next());
            replicaWindTrack.lockForRead();
            try {
                assertEquals(Util.size(windTrack.getRawFixes()), Util.size(replicaWindTrack.getRawFixes()));
                assertEquals(windTrack.getRawFixes().iterator().next(), replicaWindTrack.getRawFixes().iterator().next());
            } finally {
                replicaWindTrack.unlockAfterRead();
            }
        } finally {
            trackedRaceWebWindTrack.unlockAfterRead();
            windTrack.unlockAfterRead();
        }
    }
}
