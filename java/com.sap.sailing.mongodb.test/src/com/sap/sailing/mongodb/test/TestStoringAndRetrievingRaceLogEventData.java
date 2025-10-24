package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.UnknownHostException;

import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

public class TestStoringAndRetrievingRaceLogEventData extends AbstractMongoDBTest {
    private static final String RACELOG_TEST_COLLECTION = "racelog_test_collection";
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    public TestStoringAndRetrievingRaceLogEventData() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    @Override
    public void dropTestDB() throws UnknownHostException, MongoException, InterruptedException {
        super.dropTestDB();
        db.getCollection(RACELOG_TEST_COLLECTION).drop();
    }

    @Test
    public void testDBConnection() throws UnknownHostException, MongoException {
        MongoCollection<Document> coll = db.getCollection(RACELOG_TEST_COLLECTION);
        assertNotNull(coll);
        Document doc = new Document();
        doc.put("upperflag", "AP");
        doc.put("lowerflag", "ALPHA");
        coll.insertOne(doc);
    }

    @Test
    public void testDBRead() throws UnknownHostException, MongoException, InterruptedException {
        {
            MongoCollection<Document> coll = db.getCollection(RACELOG_TEST_COLLECTION);
            assertNotNull(coll);
            Document doc = new Document();
            doc.put("upperflag", "AP");
            doc.put("lowerflag", "ALPHA");
            coll.insertOne(doc);
        }

        {
            MongoCollection<Document> coll = db.getCollection(RACELOG_TEST_COLLECTION);
            assertNotNull(coll);
            Document object = coll.find().first();
            assertEquals("AP", object.get("upperflag"));
            assertEquals("ALPHA", object.get("lowerflag"));
        }
    }

    @Test
    public void storeRaceLogFlagEvent() throws UnknownHostException, MongoException, InterruptedException, JsonDeserializationException, ParseException {
        TimePoint now = MillisecondsTimePoint.now();
        RaceLogFlagEvent rcEvent = new RaceLogFlagEventImpl(now, author, 0, Flags.AP, Flags.ALPHA, true);
        {
            Document rcEventForMongo = ((MongoObjectFactoryImpl) PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory())
                    .storeRaceLogFlagEvent(rcEvent);
            MongoCollection<Document> coll = db.getCollection(RACELOG_TEST_COLLECTION);
            coll.insertOne(rcEventForMongo);
        }

        {
            MongoCollection<Document> coll = db.getCollection(RACELOG_TEST_COLLECTION);
            assertNotNull(coll);
            Document object = coll.find().first();
            RaceLogFlagEvent readRcEvent = (RaceLogFlagEvent) ((DomainObjectFactoryImpl) PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory()).loadRaceLogEvent(object).getA();
            assertEquals(rcEvent.getLogicalTimePoint(), readRcEvent.getLogicalTimePoint());
            assertEquals(rcEvent.getId(), readRcEvent.getId());
            assertEquals(rcEvent.getPassId(), readRcEvent.getPassId());
            assertEquals(rcEvent.getUpperFlag(), readRcEvent.getUpperFlag());
            assertEquals(rcEvent.getLowerFlag(), readRcEvent.getLowerFlag());
        }
    }
}
