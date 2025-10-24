package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintFactory;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.mongodb.MongoDBConfiguration;

public class MarkPassingRaceFingerprintConversionTest extends OnlineTracTracBasedTest {
    DynamicTrackedRaceImpl trackedRace1;
    DynamicTrackedRaceImpl trackedRace2;
    private final MongoDBConfiguration dbConfiguration;

    public MarkPassingRaceFingerprintConversionTest() throws MalformedURLException, URISyntaxException {
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
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint.matches(trackedRace1));
        MongoClient myFirstMongo = newMongo();
        MongoDatabase firstDatabase = myFirstMongo.getDatabase(dbConfiguration.getDatabaseName());
        RaceIdentifier raceIdentifier = trackedRace1.getRaceIdentifier();
        final Map<Competitor, Map<Waypoint, MarkPassing>> markPassings = trackedRace1.getMarkPassings(/* waitForLatestUpdates */ true);
        new MongoObjectFactoryImpl(firstDatabase).storeMarkPassings(raceIdentifier, fingerprint, markPassings, trackedRace1.getRace().getCourse());
        DomainObjectFactory dF = PersistenceFactory.INSTANCE.getDomainObjectFactory(dbConfiguration.getService(), getDomainFactory().getBaseDomainFactory());
        Map<RaceIdentifier, MarkPassingRaceFingerprint> fingerprintHashMap = dF.loadFingerprintsForMarkPassingHashes();
        MarkPassingRaceFingerprint fingerprintAfterDB = fingerprintHashMap.get(trackedRace1.getRaceIdentifier());
        assertTrue(fingerprintAfterDB.matches(trackedRace1), "Original and de-serialized copy are equal");
        final Map<Competitor, Map<Waypoint, MarkPassing>> markPassingsLoaded = dF.loadMarkPassings(trackedRace1.getRaceIdentifier(), trackedRace1.getRace().getCourse());
        assertEquals(markPassings, markPassingsLoaded);
    }
}