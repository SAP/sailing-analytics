package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.persistence.MongoWindStoreFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.test.PositionAssert;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddDefaultRegatta;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.TrackRegatta;
import com.sap.sailing.server.operationaltransformation.UpdateStartOfTracking;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp.ReplicationServiceTestImpl;

/**
 * Runs the same tests as {@link TrackedRaceContentsReplicationTest}, but with a non-empty {@link SensorFixStore} that
 * has special serialization requirements.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TrackedRaceWithSensorFixStoreContentsReplicationTest extends AbstractServerReplicationTest {
    private Competitor competitor;
    private DynamicTrackedRace trackedRace;
    private RegattaNameAndRaceName raceIdentifier;
    private DynamicTrackedRegatta trackedRegatta;
    
    @BeforeEach
    public void setUp() throws Exception, UnknownHostException, InterruptedException {
        Pair<ReplicationServiceTestImpl<RacingEventService>, ReplicationMasterDescriptor> replicationDescriptors = super.basicSetUp(
                /* dropDB */true, /* master=null means create a new one */null,
                /* replica=null means create a new one */null);
        final String boatClassName = "49er";
        final DomainFactory masterDomainFactory = testSetUp.getMaster().getBaseDomainFactory();
        BoatClass boatClass = masterDomainFactory.getOrCreateBoatClass(boatClassName, /* typicallyStartsUpwind */true);
        BoatClass boatClass470 = DomainFactory.INSTANCE.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true);
        competitor = masterDomainFactory.getCompetitorAndBoatStore().getOrCreateCompetitor("GER 61", "Tina Lutz", "TL", Color.RED, "someone@nowhere.de", null, new TeamImpl("Tina Lutz + Susann Beucke",
                (List<PersonImpl>) Arrays.asList(new PersonImpl[] { new PersonImpl("Tina Lutz", DomainFactory.INSTANCE.getOrCreateNationality("GER"), null, null),
                new PersonImpl("Tina Lutz", DomainFactory.INSTANCE.getOrCreateNationality("GER"), null, null) }),
                new PersonImpl("Rigo de Mas", DomainFactory.INSTANCE.getOrCreateNationality("NED"), null, null)),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat = masterDomainFactory.getCompetitorAndBoatStore().getOrCreateBoat("boat", "GER 61", boatClass470, "GER 61", null, /* storePersistently */ true);
        final String baseEventName = "Test Event";
        AddDefaultRegatta addEventOperation = new AddDefaultRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, 
                /*startDate*/ null, /*endDate*/ null, UUID.randomUUID());
        Regatta regatta = master.apply(addEventOperation);
        final String raceName = "Test Race";
        final CourseImpl masterCourse = new CourseImpl("Test Course", new ArrayList<Waypoint>());
        final Map<Competitor,Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(competitor, boat);
        RaceDefinition race = new RaceDefinitionImpl(raceName, masterCourse, boatClass, competitorsAndBoats);
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
        // set up the tracked race on the master with a non-empty GPS fix store before starting replication; this shall
        // test serialization with a non-empty GPS fix store set on the tracked race. See also bug 2986.
        replicationDescriptors.getA().startToReplicateFrom(replicationDescriptors.getB());
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

    /**
     * See bugs 3249 and 3006. The hypothesis for these two bugs is that the TrackedRace, when replicated through the initial load,
     * loses its listener relationship with the CourseImpl object because the course's listener collection is transient.
     */
    @Test
    public void testReplicatedTrackedRaceIsRegisteredAsCourseListener() throws InterruptedException {
        final String HUMBA = "Humba";
        final TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        final Waypoint newMasterWaypoint = new WaypointImpl(new MarkImpl(HUMBA));
        trackedRace.getRace().getCourse().addWaypoint(/* zeroBasedPosition */ 0, newMasterWaypoint);
        assertEquals(newMasterWaypoint, trackedRace.getRace().getCourse().getWaypoints().iterator().next());
        assertEquals(newMasterWaypoint, trackedRace.getRace().getCourse().getLegs().iterator().next().getFrom());
        assertEquals(newMasterWaypoint, trackedRace.getTrackedLegs().iterator().next().getLeg().getFrom());
        
        Thread.sleep(1000); // wait until course change has been replicated
        
        assertEquals(HUMBA, replicaTrackedRace.getRace().getCourse().getWaypoints().iterator().next().getName());
        assertEquals(HUMBA, replicaTrackedRace.getRace().getCourse().getLegs().iterator().next().getFrom().getName());
        assertEquals(Util.size(trackedRace.getTrackedLegs()), Util.size(replicaTrackedRace.getTrackedLegs()));
    }
    
    @Test
    public void testGPSFixReplication() throws InterruptedException {
        final GPSFixMovingImpl fix = new GPSFixMovingImpl(new DegreePosition(1, 2), new MillisecondsTimePoint(12345),
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        trackedRace.recordFix(competitor, fix);
        Thread.sleep(1000);
        final TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        final Competitor replicaCompetitor = replicaTrackedRace.getRace().getCompetitors().iterator().next();
        assertNotNull(replicaCompetitor);
        GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = replicaTrackedRace.getTrack(replicaCompetitor);
        competitorTrack.lockForRead();
        try {
            assertEquals(1, Util.size(competitorTrack.getRawFixes()));
            PositionAssert.assertGPSFixEquals(fix, competitorTrack.getRawFixes().iterator().next(), /* pos deg delta */ 0.0000001, /* bearing deg delta */ 0.01, /* knot speed delta */ 0.01);
            assertNotSame(fix, competitorTrack.getRawFixes().iterator().next());
        } finally {
            competitorTrack.unlockAfterRead();
        }
    }

    @Test
    public void testTrackedRaceHasValidRaceLogResolverAfterDeserialization() {
        TrackedRace replicaTrackedRace = replica.getTrackedRace(raceIdentifier);
        assertNotNull(replicaTrackedRace.getRaceLogResolver());
    }
}
