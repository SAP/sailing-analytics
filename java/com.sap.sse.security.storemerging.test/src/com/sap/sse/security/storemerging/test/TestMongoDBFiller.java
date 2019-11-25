package com.sap.sse.security.storemerging.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.client.MongoCollection;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.userstore.mongodb.impl.FieldNames;

public class TestMongoDBFiller {
    private static final String ODD_FILLER_TEST_COLLECTION_NAME = "___lafp987LFKJH7__";
    private MongoDBFiller filler = new MongoDBFiller();

    @Before
    @After
    public void dropCollection() {
        MongoDBService.INSTANCE.getDB().getCollection(ODD_FILLER_TEST_COLLECTION_NAME).drop();
    }
    
    @Test
    public void testUsersFiller() throws IOException {
        final MongoCollection<Document> testCollection = MongoDBService.INSTANCE.getDB().getCollection(ODD_FILLER_TEST_COLLECTION_NAME);
        filler.fill(testCollection, "/resources/USERS_target1.json");
        final Document singleResult = testCollection.find(new Document(FieldNames.User.NAME.name(), "uhl3")).first();
        assertEquals("axel.uhl@sap.com", singleResult.get(FieldNames.User.EMAIL.name()));
        final Document secondSingleResult = testCollection.find(new Document(FieldNames.User.NAME.name(), "axel")).first();
        assertEquals("axel.uhl@gmx.de", secondSingleResult.get(FieldNames.User.EMAIL.name()));
    }

    @Test
    public void testAccessControlStoreFiller() throws IOException {
        final MongoCollection<Document> testCollection = MongoDBService.INSTANCE.getDB().getCollection(ODD_FILLER_TEST_COLLECTION_NAME);
        filler.fill(testCollection, "/resources/ACCESS_CONTROL_STORE_target1.json");
        final Document singleResult = testCollection.find(new Document(FieldNames.Ownership.OBJECT_ID.name(), "ROLE_DEFINITION/244cb84c-2b8a-4557-b175-db963072cfbc")).first();
        assertNotNull(singleResult.get(FieldNames.AccessControlList.PERMISSION_MAP.name()));
    }
}
