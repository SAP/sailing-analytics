package com.sap.sse.landscape.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.Random;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.mongodb.client.MongoDatabase;
import com.sap.sse.common.Duration;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.aws.orchestration.CopyAndCompareMongoDatabase;
import com.sap.sse.landscape.mongodb.MongoProcess;
import com.sap.sse.landscape.mongodb.impl.DatabaseImpl;
import com.sap.sse.landscape.mongodb.impl.MongoProcessImpl;
import com.sap.sse.landscape.mongodb.impl.MongoProcessInReplicaSetImpl;
import com.sap.sse.landscape.mongodb.impl.MongoReplicaSetImpl;

@Disabled("Needs a specific MongoDB replica set named 'rs0' in the environment")
public class MongoTests {
    private static final Optional<Duration> optionalTimeout = Optional.of(Duration.ONE_MINUTE.times(5));

    private Host localhost;
    private MongoProcessImpl mongoProcess;
    private MongoProcessInReplicaSetImpl mongoProcessInReplicaSet;
    private MongoReplicaSetImpl mongoReplicaSet;
    
    @BeforeEach
    public void setUp() throws UnknownHostException {
        localhost = Mockito.mock(Host.class);
        Mockito.when(localhost.getPrivateAddress()).thenReturn(InetAddress.getByName("127.0.0.1"));
        mongoProcess = new MongoProcessImpl(localhost);
        mongoReplicaSet = new MongoReplicaSetImpl("rs0");
        mongoProcessInReplicaSet = new MongoProcessInReplicaSetImpl(mongoReplicaSet, 10222, localhost);
        mongoReplicaSet.addReplica(mongoProcessInReplicaSet);
    }
    
    @Test
    public void testMongoReadiness() {
        assertTrue(mongoProcess.isReady(optionalTimeout));
    }
    
    @Test
    public void testMongoProcessInReplicaSetIsAvailable() {
        assertTrue(mongoProcessInReplicaSet.isReady(optionalTimeout));
    }
    
    @Test
    public void testMongoProcessInReplicaSetCanReportPriority() throws URISyntaxException {
        assertTrue(mongoProcessInReplicaSet.isInReplicaSet());
    }
    
    @Test
    public void testMd5() throws URISyntaxException {
        final String hash = mongoReplicaSet.getMD5Hash("local");
        assertNotNull(hash);
        assertTrue(!hash.isEmpty());
    }

    @Test
    public void testImport() throws URISyntaxException {
        final String dbName = "importtest"+new Random().nextInt();
        try { 
            final MongoDatabase exportFrom = mongoReplicaSet.getMongoDatabase(dbName);
            exportFrom.drop();
            mongoProcess.getMongoDatabase(dbName).drop();
            exportFrom.getCollection("C1").insertOne(new Document("a", "b"));
            exportFrom.getCollection("C2").insertOne(new Document("c", "d"));
            exportFrom.getCollection("C2").insertOne(new Document("e", "f"));
            exportFrom.createCollection("C3"); // force an empty C3 collection
            mongoProcess.importDatabase(exportFrom);
            final String hashExport = mongoReplicaSet.getMD5Hash(dbName);
            final String hashImport = mongoProcess.getMD5Hash(dbName);
            assertNotNull(hashExport);
            assertEquals(hashExport, hashImport);
        } finally {
            mongoProcess.getMongoDatabase(dbName).drop();
            mongoReplicaSet.getMongoDatabase(dbName).drop();
        }
    }
    
    @Test
    public void testDatabaseArchiving() throws Exception {
        final String dbName = "importtest"+new Random().nextInt();
        try { 
            final MongoDatabase exportFrom = mongoReplicaSet.getMongoDatabase(dbName);
            exportFrom.drop();
            mongoProcess.getMongoDatabase(dbName).drop();
            exportFrom.getCollection("C1").insertOne(new Document("a", "b"));
            exportFrom.getCollection("C2").insertOne(new Document("c", "d"));
            exportFrom.getCollection("C2").insertOne(new Document("e", "f"));
            exportFrom.createCollection("C3"); // force an empty C3 collection
            CopyAndCompareMongoDatabase.builder()
                .setSourceDatabase(new DatabaseImpl(mongoReplicaSet, dbName))
                .setTargetDatabase(new DatabaseImpl(mongoProcess, dbName))
                .build().run();
            // the "assertion" is that this terminates normally, without exception, which happens only if
            // the import was successful and showed no diff
        } finally {
            mongoProcess.getMongoDatabase(dbName).drop();
            mongoReplicaSet.getMongoDatabase(dbName).drop();
        }
    }

    @Test
    public void testDatabaseArchivingWithPreviousContentInTargetWithoutDropping() throws Exception {
        final String dbName = "importtest"+new Random().nextInt();
        final MongoDatabase exportTo = mongoProcess.getMongoDatabase(dbName);
        try { 
            final MongoDatabase exportFrom = mongoReplicaSet.getMongoDatabase(dbName);
            exportFrom.drop();
            exportTo.drop();
            exportTo.getCollection("C4").insertOne(new Document("x", "y")); // an extra record that shall spoil the comparison after archiving without dropping
            exportFrom.getCollection("C1").insertOne(new Document("a", "b"));
            exportFrom.getCollection("C2").insertOne(new Document("c", "d"));
            exportFrom.getCollection("C2").insertOne(new Document("e", "f"));
            exportFrom.createCollection("C3"); // force an empty C3 collection
            try {
                CopyAndCompareMongoDatabase.builder()
                    .setSourceDatabase(new DatabaseImpl(mongoReplicaSet, dbName))
                    .setTargetDatabase(new DatabaseImpl(mongoProcess, dbName))
                    .build().run();
                fail("Expected the DB comparison to fail with an IllegalStateException due to different hashes");
            } catch (IllegalStateException e) {
                assertTrue(e.getMessage().contains("hashes are different"));
            }
        } finally {
            exportTo.drop();
            mongoReplicaSet.getMongoDatabase(dbName).drop();
        }
    }

    @Test
    public void testDatabaseArchivingWithPreviousContentInTargetWithDropping() throws Exception {
        final String dbName = "importtest"+new Random().nextInt();
        final MongoDatabase exportTo = mongoProcess.getMongoDatabase(dbName);
        try { 
            final MongoDatabase exportFrom = mongoReplicaSet.getMongoDatabase(dbName);
            exportFrom.drop();
            exportTo.drop();
            exportTo.getCollection("C1").insertOne(new Document("x", "y")); // an already existing collection that without dropping would cause a MongoDB command exception
            exportTo.getCollection("C4").insertOne(new Document("x", "y")); // an extra record that shall spoil the comparison after archiving without dropping
            exportFrom.getCollection("C1").insertOne(new Document("a", "b"));
            exportFrom.getCollection("C2").insertOne(new Document("c", "d"));
            exportFrom.getCollection("C2").insertOne(new Document("e", "f"));
            exportFrom.createCollection("C3"); // force an empty C3 collection
            CopyAndCompareMongoDatabase.builder()
                .dropTargetFirst(true)
                .setSourceDatabase(new DatabaseImpl(mongoReplicaSet, dbName))
                .setTargetDatabase(new DatabaseImpl(mongoProcess, dbName))
                .build().run();
            // the "assertion" is that this terminates normally, without exception, which happens only if
            // the import was successful and showed no diff
        } finally {
            exportTo.drop();
            mongoReplicaSet.getMongoDatabase(dbName).drop();
        }
    }

    /**
     * An SSH tunnel locally bound to port 10202 to dbserver.internal.sapsailing.com:10202 is required for
     * this test.
     */
    @Test
    public void testImportSCL2018StPetersburgThroughTunnel() throws URISyntaxException {
        final String dbName = "scl2018-sanktpetersburg";
        try { 
            final MongoProcess archive = new MongoProcessImpl(localhost, 10202);
            final MongoDatabase exportFrom = archive.getMongoDatabase(dbName);
            mongoProcess.getMongoDatabase(dbName).drop();
            mongoProcess.importDatabase(exportFrom);
            final String hashExport = archive.getMD5Hash(dbName);
            final String hashImport = mongoProcess.getMD5Hash(dbName);
            assertNotNull(hashExport);
            assertEquals(hashExport, hashImport);
        } finally {
            mongoProcess.getMongoDatabase(dbName).drop();
        }
    }
}
