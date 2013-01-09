package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.test.AbstractTracTracLiveTest;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.mongodb.MongoDBConfiguration;
import com.sap.sailing.server.impl.RacingEventServiceImpl;

public class TestStoringAndRetrievingWindTracksTest extends AbstractTracTracLiveTest {

    private Mongo mongo;
    private DB db;
    
    private final MongoDBConfiguration dbConfiguration;

    public TestStoringAndRetrievingWindTracksTest() throws URISyntaxException, MalformedURLException {
        super();
        dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
    }
    
    private Mongo newMongo() throws UnknownHostException, MongoException {
        return new Mongo(System.getProperty("mongo.host", "127.0.0.1"),
                dbConfiguration.getPort());
    }
    
    @Before
    public void dropTestDB() throws UnknownHostException, MongoException {
        mongo = newMongo();
        assertNotNull(mongo);
        mongo.dropDatabase(dbConfiguration.getDatabaseName());
        db = mongo.getDB(dbConfiguration.getDatabaseName());
        assertNotNull(db);
    }
    
    @Test
    public void testStoreAFewWindEntries() throws UnknownHostException, MongoException, InterruptedException {
        DomainFactory domainFactory = DomainFactory.INSTANCE;
        Regatta domainEvent = domainFactory.getOrCreateDefaultRegatta(getTracTracEvent(), /* trackedRegattaRegistry */ null);
        DynamicTrackedRegatta trackedRegatta = new RacingEventServiceImpl().getOrCreateTrackedRegatta(domainEvent);
        Iterable<Receiver> typeControllers = domainFactory.getUpdateReceivers(trackedRegatta, getTracTracEvent(),
                EmptyWindStore.INSTANCE, /* startOfTracking */null, /* endOfTracking */null, /* delayToLiveInMillis */
                0l, /* simulator */ null, new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race) {
                    }
                }, /* trackedRegattaRegistry */ null, ReceiverType.RACECOURSE);
        addListenersForStoredDataAndStartController(typeControllers);
        RaceDefinition race = domainFactory.getAndWaitForRaceDefinition(getTracTracEvent().getRaceList().iterator().next());
        DynamicTrackedRace trackedRace = trackedRegatta.createTrackedRace(race, EmptyWindStore.INSTANCE, 
                    /* delayToLiveInMillis */ 0l, /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 10000, new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race) {
                    }
                });
        WindSource windSource = new WindSourceImpl(WindSourceType.WEB);
        Mongo myFirstMongo = newMongo();
        DB firstDatabase = myFirstMongo.getDB(dbConfiguration.getDatabaseName());
        new MongoObjectFactoryImpl(firstDatabase).addWindTrackDumper(trackedRegatta, trackedRace, windSource);
        WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
        Position pos = new DegreePosition(54, 9);
        TimePoint timePoint = MillisecondsTimePoint.now();
        for (double bearingDeg = 123.4; bearingDeg<140; bearingDeg += 1.1) {
            windTrack.add(new WindImpl(pos, timePoint, new KnotSpeedWithBearingImpl(10., new DegreeBearingImpl(bearingDeg))));
            timePoint = new MillisecondsTimePoint(timePoint.asMillis()+1);
        }
        Thread.sleep(2000); // give MongoDB some time to make written data available to other connections
        
        Mongo mySecondMongo = newMongo();
        DB secondDatabase = mySecondMongo.getDB(dbConfiguration.getDatabaseName());
        WindTrack result = new DomainObjectFactoryImpl(secondDatabase).loadWindTrack(domainEvent, race, windSource, /* millisecondsOverWhichToAverage */
                30000);
        double myBearingDeg = 123.4;
        result.lockForRead();
        try {
            for (Wind wind : result.getRawFixes()) {
                assertEquals(pos, wind.getPosition());
                assertEquals(10., wind.getKnots(), 0.000000000001);
                assertEquals(myBearingDeg, wind.getBearing().getDegrees(), 0.000000001);
                myBearingDeg += 1.1;
            }
        } finally {
            result.unlockAfterRead();
        }
        assertTrue("Expected myBeaaringDeg to be >= 139.999999999 but was "+myBearingDeg, myBearingDeg >= 139.999999999);
    }
}
