package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.ManeuverLoss;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.mongodb.MongoDBConfiguration;

public class ManeuverRaceFingerprintConversionTest extends OnlineTracTracBasedTest {
    DynamicTrackedRaceImpl trackedRace1;
    DynamicTrackedRaceImpl trackedRace2;
    private final MongoDBConfiguration dbConfiguration;

    public ManeuverRaceFingerprintConversionTest() throws MalformedURLException, URISyntaxException {
        super();
        dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
    }

    private MongoClient newMongo() throws UnknownHostException, MongoException {
        return MongoClients.create(dbConfiguration.getMongoClientURI());
    }

    @Override
    protected String getExpectedEventName() {
        return "Academy Tracking 2011";
    }

    @BeforeEach
    public void setUp() throws Exception {
        newMongo().getDatabase(dbConfiguration.getDatabaseName()).drop();
        super.setUp("event_20110505_SailingTea", // Semifinale
                /* raceId */ "01ea3604-02ef-11e1-9efc-406186cbf87c", /* liveUri */ null, /* storedUri */ null,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE,
                        ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS, ReceiverType.SENSORDATA });
        trackedRace1 = getTrackedRace();
    }

    @Test
    public void testStoringToAndLoadingFromMongo() throws UnknownHostException, MongoException {
        final ManeuverRaceFingerprintFactory factory = ManeuverRaceFingerprintFactory.INSTANCE;
        final ManeuverRaceFingerprint fingerprint = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint.matches(trackedRace1));
        final MongoClient myFirstMongo = newMongo();
        final MongoDatabase firstDatabase = myFirstMongo.getDatabase(dbConfiguration.getDatabaseName());
        final RaceIdentifier raceIdentifier = trackedRace1.getRaceIdentifier();
        final Map<Competitor, List<Maneuver>> maneuvers = new HashMap<>();
        for (final Competitor competitor : getRace().getCompetitors()) {
            final List<Maneuver> maneuversForCompetitor = (List<Maneuver>) trackedRace1.getManeuvers(competitor, /* wait for latest */ true);
            maneuvers.put(competitor, maneuversForCompetitor);
        }
        new MongoObjectFactoryImpl(firstDatabase).storeManeuvers(raceIdentifier, fingerprint, trackedRace1.getRace().getCourse(), maneuvers);
        final DomainObjectFactory dF = PersistenceFactory.INSTANCE.getDomainObjectFactory(dbConfiguration.getService(), getDomainFactory().getBaseDomainFactory());
        final Map<RaceIdentifier, ManeuverRaceFingerprint> fingerprintHashMap = dF.loadFingerprintsForManeuverHashes();
        final ManeuverRaceFingerprint fingerprintAfterDB = fingerprintHashMap.get(trackedRace1.getRaceIdentifier());
        assertTrue(fingerprintAfterDB.matches(trackedRace1), "Original and de-serialized copy are equal");
        final Map<Competitor, List<Maneuver>> maneuversLoaded = dF.loadManeuvers(trackedRace1, trackedRace1.getRace().getCourse());
        assertEquals(maneuvers, maneuversLoaded); // this only checks the equality based on AbstractGPSFixImpl.equals, so lat/lng, cog/sog and time stamp
        for (final Competitor c : maneuversLoaded.keySet()) {
            final Iterator<Maneuver> maneuverIter = maneuvers.get(c).iterator();
            for (final Maneuver m : maneuversLoaded.get(c)) {
                final Maneuver maneuverDetected = maneuverIter.next();
                assertSame(maneuverDetected.getClass(), m.getClass());
                assertEqualManeuverLoss(maneuverDetected.getManeuverLoss(), m.getManeuverLoss());
                assertEquals(maneuverDetected.getAvgTurningRateInDegreesPerSecond(), m.getAvgTurningRateInDegreesPerSecond(), 0.000001);
                assertEquals(maneuverDetected.getDirectionChangeInDegrees(), m.getDirectionChangeInDegrees());
                assertEqualsManeuverCurveBoundaries(maneuverDetected.getManeuverBoundaries(), m.getManeuverBoundaries());
                assertEqualsManeuverCurveBoundaries(maneuverDetected.getManeuverCurveWithStableSpeedAndCourseBoundaries(), m.getManeuverCurveWithStableSpeedAndCourseBoundaries());
            }
        }
    }

    private void assertEqualsManeuverCurveBoundaries(ManeuverCurveBoundaries maneuverBoundaries1, ManeuverCurveBoundaries maneuverBoundaries2) {
        assertEquals(maneuverBoundaries1.getDirectionChangeInDegrees(), maneuverBoundaries2.getDirectionChangeInDegrees(), 0.000001);
        assertEquals(maneuverBoundaries1.getDuration().asSeconds(), maneuverBoundaries2.getDuration().asSeconds(), 0.000001);
        assertEquals(maneuverBoundaries1.getHighestSpeed().getKnots(), maneuverBoundaries2.getHighestSpeed().getKnots(), 0.000001);
        assertEquals(maneuverBoundaries1.getLowestSpeed().getKnots(), maneuverBoundaries2.getLowestSpeed().getKnots(), 0.000001);
        assertEquals(maneuverBoundaries1.getMiddleCourse().getDegrees(), maneuverBoundaries2.getMiddleCourse().getDegrees(), 0.000001);
        assertEqualSpeeds(maneuverBoundaries1.getSpeedWithBearingAfter(), maneuverBoundaries2.getSpeedWithBearingAfter());
        assertEqualSpeeds(maneuverBoundaries1.getSpeedWithBearingBefore(), maneuverBoundaries2.getSpeedWithBearingBefore());
        assertEquals(maneuverBoundaries1.getTimePointAfter(), maneuverBoundaries2.getTimePointAfter());
        assertEquals(maneuverBoundaries1.getTimePointBefore(), maneuverBoundaries2.getTimePointBefore());
    }

    private void assertEqualManeuverLoss(ManeuverLoss maneuverLoss1, ManeuverLoss maneuverLoss2) {
        if (maneuverLoss1 != null) {
            assertEquals(maneuverLoss1.getManeuverStartPosition(), maneuverLoss2.getManeuverStartPosition());
            assertEquals(maneuverLoss1.getManeuverEndPosition(), maneuverLoss2.getManeuverEndPosition());
            assertEquals(maneuverLoss1.getDistanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle().getMeters(),
                    maneuverLoss2.getDistanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle().getMeters(), 0.000001);
            assertEquals(maneuverLoss1.getDistanceSailedProjectedOnMiddleManeuverAngle().getMeters(),
                    maneuverLoss2.getDistanceSailedProjectedOnMiddleManeuverAngle().getMeters(), 0.000001);
            assertEquals(maneuverLoss1.getManeuverDuration().asSeconds(), maneuverLoss2.getManeuverDuration().asSeconds(), 0.000001);
            assertEquals(maneuverLoss1.getMiddleManeuverAngle().getDegrees(), maneuverLoss2.getMiddleManeuverAngle().getDegrees(), 0.000001);
            assertEquals(maneuverLoss1.getProjectedDistanceLost().getMeters(), maneuverLoss2.getProjectedDistanceLost().getMeters(), 0.000001);
            assertEquals(maneuverLoss1.getRatioBetweenDistanceSailedWithAndWithoutManeuver(), maneuverLoss2.getRatioBetweenDistanceSailedWithAndWithoutManeuver(), 0.000001);
            assertEqualSpeeds(maneuverLoss1.getSpeedWithBearingBefore(), maneuverLoss2.getSpeedWithBearingBefore());
        }
    }

    private void assertEqualSpeeds(SpeedWithBearing speedWithBearing1, SpeedWithBearing speedWithBearing2) {
        assertEquals(speedWithBearing1.getKnots(), speedWithBearing2.getKnots(), 0.000001);
        assertEquals(speedWithBearing1.getBearing().getDegrees(), speedWithBearing2.getBearing().getDegrees(), 0.000001);
    }
}
