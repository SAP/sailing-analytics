package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
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
import com.sap.sailing.server.operationaltransformation.AllowCompetitorResetToDefaults;
import com.sap.sailing.server.operationaltransformation.CreateTrackedRace;
import com.sap.sailing.server.operationaltransformation.UpdateCompetitor;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests replication of competitors in conjunction with the {@link CompetitorAndBoatStore} concepts, particularly the
 * possibility to allow for competitor data to be updated, either explicitly or implicitly from a tracking provider
 * after marking the competitor using
 * {@link CompetitorAndBoatStore#allowCompetitorResetToDefaults(com.sap.sailing.domain.base.Competitor)}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class CompetitorReplicationTest extends AbstractServerReplicationTest {
    /**
     * Add a tracked race to the master that includes a competitor; check that the competitor was properly replicated to
     * the replica's {@link CompetitorAndBoatStore}. Afterwards, use the {@link UpdateCompetitor} operation on the master to
     * perform an explicit update; ensure that the update arrived on the replica. Then execute an
     * {@link AllowCompetitorResetToDefaults} operation on the master, afterwards update the competitor on the master,
     * then force some competitor-related event to be sent to the replica, such as a GPS fix update. This will serialize
     * the competitor with its modified state over to the replica where a
     * {@link DomainFactory#getOrCreateCompetitor(java.io.Serializable, String, com.sap.sailing.domain.base.impl.DynamicTeam, com.sap.sailing.domain.base.impl.DynamicBoat)}
     * will be triggered. This should also update the competitor on the client.
     * @throws URISyntaxException 
     */
    @Test
    public void testSimpleSpecificRegattaReplication() throws InterruptedException, URISyntaxException {
        String baseEventName = "My Test Event";
        String boatClassName = "Kielzugvogel";
        Integer regattaId = 12345;
        URI flagImageURI = new URI("http://www.sapsailing.com");
        Iterable<Series> series = Collections.emptyList();
        Regatta masterRegatta = master.createRegatta(RegattaImpl.getDefaultName(baseEventName, boatClassName), boatClassName, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, regattaId, series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        Iterable<Waypoint> emptyWaypointList = Collections.emptyList();
        final String competitorName = "Der mit dem Kiel zieht";
        final String competitorShortName = "DK";
        final BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */true);
        Competitor competitor = master.getBaseDomainFactory().getOrCreateCompetitor(
                123, competitorName, competitorShortName, Color.RED, "someone@nowhere.de", flagImageURI,
                new TeamImpl("STG", Collections.singleton(new PersonImpl(competitorName, new NationalityImpl("GER"),
                /* dateOfBirth */null, "This is famous " + competitorName)), new PersonImpl("Rigo van Maas",
                        new NationalityImpl("NED"),
                        /* dateOfBirth */null, "This is Rigo, the coach")),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
        Boat boat = new BoatImpl(competitor.getId(), competitorName + "'s boat", boatClass, /* sailID */ null);
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
        assertNotSame(replicatedCompetitor, competitor);
        assertEquals(competitor.getId(), replicatedCompetitor.getId());
        assertEquals(competitor.getName(), replicatedCompetitor.getName());
        assertEquals(competitor.getShortName(), replicatedCompetitor.getShortName());
        assertEquals(competitor.getColor(), replicatedCompetitor.getColor());
        assertEquals(competitor.getFlagImage(), replicatedCompetitor.getFlagImage());
        assertEquals(boat.getSailID(), replicatedBoat.getSailID());
        assertEquals(competitor.getTeam().getNationality(), replicatedCompetitor.getTeam().getNationality());
        
        // now update competitor on master using replicating operation
        final String newCompetitorName = "Der Vogel, der mit dem Kiel zieht";
        final String newCompetitorShortName = "VK";
        master.apply(new UpdateCompetitor(competitor.getId().toString(), newCompetitorName, newCompetitorShortName, competitor.getColor(), competitor.getEmail(), competitor.getTeam().getNationality(),
                competitor.getTeam().getImage(), competitor.getFlagImage(),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null));
        Thread.sleep(1000);
        assertEquals(newCompetitorName, replicatedCompetitor.getName()); // expect in-place update of existing competitor in replica
        assertEquals(newCompetitorShortName, replicatedCompetitor.getShortName());
        
        // now allow for resetting to default through some event, such as receiving a GPS position
        master.apply(new AllowCompetitorResetToDefaults(Collections.singleton(competitor.getId().toString())));
        // modify the competitor on the master "from below" without an UpdateCompetitor operation, only locally:
        master.getBaseDomainFactory().getCompetitorAndBoatStore().updateCompetitor(competitor.getId().toString(), competitorName, competitorShortName, Color.RED, competitor.getEmail(),
                competitor.getTeam().getNationality(), competitor.getTeam().getImage(), competitor.getFlagImage(), 
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        final RegattaAndRaceIdentifier raceIdentifier = masterRegatta.getRaceIdentifier(raceDefinition);
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) master.apply(new CreateTrackedRace(raceIdentifier,
                EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 3000,
                /* millisecondsOverWhichToAverageWind */ 30000l, /* millisecondsOverWhichToAverageSpeed */ 30000l, null));
        trackedRace.getTrack(competitor).addGPSFix(new GPSFixMovingImpl(new DegreePosition(49.425, 8.293), MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(12.3, new DegreeBearingImpl(242.3)), /* optionalTrueHeading */ null));
        Thread.sleep(1000);
        TrackedRace replicatedTrackedRace = replica.getTrackedRace(raceIdentifier);
        assertNotNull(replicatedTrackedRace);
        assertNotNull(replicatedTrackedRace.getTrack(replicatedCompetitor).getFirstRawFix());
        assertEquals(competitorName, replicatedCompetitor.getName());
        assertEquals(competitorShortName, replicatedCompetitor.getShortName());
    }
    
    @Test
    public void testCompetitorCreationReplication() throws InterruptedException, URISyntaxException {
        final String competitorName = "Der mit dem Kiel zieht";
        final String shortCcompetitorName = "Kiel";
        URI flagImageURI = new URI("http://www.sapsailing.com");
        Competitor competitor = master.getBaseDomainFactory().getOrCreateCompetitor(
            123, competitorName, shortCcompetitorName, Color.RED, "someone@nowhere.de", flagImageURI,
            new TeamImpl("STG", Collections.singleton(new PersonImpl(competitorName, new NationalityImpl("GER"),
                    /* dateOfBirth */null, "This is famous " + competitorName)), new PersonImpl("Rigo van Maas",
                    new NationalityImpl("NED"), /* dateOfBirth */null, "This is Rigo, the coach")),
                    /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
        Thread.sleep(1000);
        Competitor[] replicaCompetitor = new Competitor[1];
        StreamSupport.stream(replica.getBaseDomainFactory().getCompetitorAndBoatStore().getAllCompetitors().spliterator(), /* parallel */ false).filter(
                c->c.getId().equals(competitor.getId())).forEach(c->replicaCompetitor[0]=c);
        assertNotNull(replicaCompetitor[0]);
        assertEquals(competitor.getClass().getName(), replicaCompetitor[0].getClass().getName());
    }

    @Test
    public void testCompetitorWithBoatCreationReplication() throws InterruptedException, URISyntaxException {
        final String competitorName = "Der mit dem Kiel zieht";
        final String shortCcompetitorName = "Kiel";
        URI flagImageURI = new URI("http://www.sapsailing.com");
        DynamicBoat boat = master.getBaseDomainFactory().getOrCreateBoat(234, "The Boat", master.getBaseDomainFactory().getOrCreateBoatClass("49er"), "234", null, /* storePersistently */ true);
        CompetitorWithBoat competitor = master.getBaseDomainFactory().getOrCreateCompetitorWithBoat(
            123, competitorName, shortCcompetitorName, Color.RED, "someone@nowhere.de", flagImageURI,
            new TeamImpl("STG", Collections.singleton(new PersonImpl(competitorName, new NationalityImpl("GER"),
                    /* dateOfBirth */null, "This is famous " + competitorName)), new PersonImpl("Rigo van Maas",
                    new NationalityImpl("NED"), /* dateOfBirth */null, "This is Rigo, the coach")),
                    /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, boat, /* storePersistently */ true);
        Thread.sleep(1000);
        Competitor[] replicaCompetitor = new Competitor[1];
        StreamSupport.stream(replica.getBaseDomainFactory().getCompetitorAndBoatStore().getAllCompetitors().spliterator(), /* parallel */ false).filter(
                c->c.getId().equals(competitor.getId())).forEach(c->replicaCompetitor[0]=c);
        assertNotNull(replicaCompetitor[0]);
        assertEquals(competitor.getClass().getName(), replicaCompetitor[0].getClass().getName());
    }
}