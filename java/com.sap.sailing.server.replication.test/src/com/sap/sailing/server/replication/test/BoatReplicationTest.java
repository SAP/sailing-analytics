package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.operationaltransformation.AddRaceDefinition;
import com.sap.sailing.server.operationaltransformation.AllowBoatResetToDefaults;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.UpdateBoat;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests replication of boats in conjunction with the {@link CompetitorAndBoatStore} concepts, particularly the
 * possibility to allow for boat data to be updated, either explicitly or implicitly from a tracking provider
 * after marking the boat using
 * {@link CompetitorAndBoatStore#allowBoatResetToDefaults(com.sap.sailing.domain.base.Boat)}.
 * 
 * @author Frank Mittag
 * 
 */
public class BoatReplicationTest extends AbstractServerReplicationTest {
    /**
     * Add a tracked race to the master that includes a competitor; check that the boat was properly replicated to
     * the replica's {@link CompetitorAndBoatStore}. Afterwards, use the {@link UpdateBoat} operation on the master to
     * perform an explicit update; ensure that the update arrived on the replica. Then execute an
     * {@link AllowBoatResetToDefaults} operation on the master, afterwards update the boat on the master,
     * @throws URISyntaxException 
     */
    @Test
    public void testBoatAndBoatUpdateReplication() throws InterruptedException, URISyntaxException {
        String baseEventName = "My Test Event";
        String boatClassName = "Kielzugvogel";
        Integer regattaId = 12345;
        Iterable<Series> series = Collections.emptyList();
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, regattaId, series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        Iterable<Waypoint> emptyWaypointList = Collections.emptyList();
        final String boatName = "Kielboat Harry";
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("505", /* typicallyStartsUpwind */true);
        Competitor competitor = master.getBaseDomainFactory().getOrCreateCompetitor(
                123, "comp1", "c1", Color.RED, "someone@nowhere.de", null,
                new TeamImpl("STG", Collections.singleton(new PersonImpl("comp1", new NationalityImpl("GER"),
                /* dateOfBirth */null, "This is famous name")), new PersonImpl("Rigo van Maas",
                        new NationalityImpl("NED"), /* dateOfBirth */null, "This is Rigo, the coach")),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat = master.getBaseDomainFactory().getOrCreateBoat(UUID.randomUUID(), "Kielboat", boatClass, "GER 123", Color.RED, /* storePersistently */ true);
        Map<Competitor,Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(competitor, boat);
        final String raceName = "Test Race";
        RaceDefinition raceDefinition = new RaceDefinitionImpl(raceName, new CourseImpl("Empty Course", emptyWaypointList),
                masterRegatta.getBoatClass(), competitorsAndBoats);
        master.apply(new AddRaceDefinition(masterRegatta.getRegattaIdentifier(), raceDefinition));
        Thread.sleep(1000);
        Regatta replicatedRegatta = replica.getRegatta(new RegattaName(masterRegatta.getName()));
        assertNotNull(replicatedRegatta);
        assertTrue(replicatedRegatta.isPersistent());
        assertTrue(Util.isEmpty((replicatedRegatta.getSeries())));
        assertTrue(Util.isEmpty(replicatedRegatta.getCourseAreas()));        
        assertTrue(regattaId.equals(replicatedRegatta.getId()));
        RaceDefinition replicatedRace = replicatedRegatta.getRaceByName(raceName);
        assertNotNull(replicatedRace);
        Map.Entry<Competitor,Boat> replicatedCompetitorAndBoat = replicatedRace.getCompetitorsAndTheirBoats().entrySet().iterator().next();
        Competitor replicatedCompetitor = replicatedCompetitorAndBoat.getKey();
        Boat replicatedBoat = replicatedCompetitorAndBoat.getValue();
        assertNotSame(replicatedBoat, boat);
        assertEquals(boat.getId(), replicatedBoat.getId());
        assertEquals(boat.getName(), replicatedBoat.getName());
        assertEquals(boat.getBoatClass().getName(), replicatedBoat.getBoatClass().getName());
        assertEquals(boat.getSailID(), replicatedBoat.getSailID());
        assertEquals(boat.getColor(), replicatedBoat.getColor());
        // now update the boat on master using replicating operation
        final String newBoatName = "Der Vogel, der mit dem Kiel zieht";
        master.apply(new UpdateBoat(boat.getId().toString(), newBoatName, boat.getColor(), boat.getSailID()));
        Thread.sleep(1000);
        assertEquals(newBoatName, replicatedBoat.getName()); // expect in-place update of existing boat in replica
        // now allow for resetting to default through some event, such as receiving a GPS position
        master.apply(new AllowBoatResetToDefaults(Collections.singleton(boat.getId().toString())));
        // modify the boat on the master "from below" without an UpdateBoat operation, only locally:
        master.getBaseDomainFactory().getCompetitorAndBoatStore().updateBoat(boat.getId().toString(), boatName, boat.getColor(), boat.getSailID());
        final RegattaAndRaceIdentifier raceIdentifier = masterRegatta.getRaceIdentifier(raceDefinition);
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) master.apply(new CreateTrackedRace(raceIdentifier,
                EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 30000l, /* millisecondsOverWhichToAverageSpeed */ 30000l, null));
        trackedRace.getTrack(competitor).addGPSFix(new GPSFixMovingImpl(new DegreePosition(49.425, 8.293), MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(12.3, new DegreeBearingImpl(242.3)), /* optionalTrueHeading */ null));
        Thread.sleep(1000);
        TrackedRace replicatedTrackedRace = replica.getTrackedRace(raceIdentifier);
        assertNotNull(replicatedTrackedRace);
        Boat replicatedBoat2 = replicatedTrackedRace.getBoatOfCompetitor(replicatedCompetitor);
        assertNotNull(replicatedBoat2);
        assertEquals(boatName, replicatedBoat2.getName());
   }
    
    @Test
    public void testBoatCreationReplication() throws InterruptedException, URISyntaxException {
        final String boatName = "Kielzugvogel 123";
        BoatClass boatClass = new BoatClassImpl("Kielzugvogel", true);
        Boat boat = master.getBaseDomainFactory().getOrCreateBoat(123, boatName, boatClass, "GER 123", null, /* storePersistently */ true);
        Thread.sleep(1000);
        assertTrue(StreamSupport.stream(replica.getBaseDomainFactory().getCompetitorAndBoatStore().getBoats().spliterator(), /* parallel */ false).anyMatch(
                b-> b.getId().equals(boat.getId())));
    }
}