package com.sap.sse.landscape.mongodb.impl;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.RawBsonDocument;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterConnectionMode;
import com.mongodb.session.ClientSession;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.mongodb.MongoDBService;

public abstract class MongoEndpointImpl implements MongoEndpoint {
    private static final Logger logger = Logger.getLogger(MongoEndpointImpl.class.getName());
    private static final int IMPORT_BATCH_SIZE = 100;
    /**
     * Limit retained BSON bytes as well as document count. A larger document is inserted in a batch of its own.
     */
    private static final int IMPORT_BATCH_BYTES = 8 * 1024 * 1024;
    
    @Override
    public abstract boolean equals(Object o);
    
    @Override
    public abstract int hashCode();
    
    @Override
    public Iterable<MongoDatabase> getMongoDatabases() throws URISyntaxException {
        final MongoClient client = getClient();
        return Util.map(client.listDatabaseNames(), dbName->client.getDatabase(dbName));
    }

    @Override
    public MongoDatabase getMongoDatabase(String dbName) throws URISyntaxException {
        return getClient().getDatabase(dbName);
    }

    /**
     * Copies collections sequentially using raw BSON and byte- and count-limited write batches. Retained document data
     * is bounded independently of collection size by the write batch, the current document, and the driver's read
     * batch and encoding buffers. A document larger than the write-batch byte budget is copied on its own.
     */
    @Override
    public MongoDatabase importDatabase(final MongoDatabase from) throws URISyntaxException {
        final MongoDatabase targetDatabase = getMongoDatabase(from.getName());
        logger.info("Importing database "+from.getName()+" into "+targetDatabase.getName()+" on "+this);
        try (final MongoCursor<String> collectionNames = from.listCollectionNames().iterator()) {
            while (collectionNames.hasNext()) {
                importCollection(from, targetDatabase, collectionNames.next());
            }
        }
        return targetDatabase;
    }

    private void importCollection(final MongoDatabase from, final MongoDatabase targetDatabase, final String collectionName) {
        // Keep documents in their compact wire representation instead of expanding nested arrays and documents on the heap.
        final MongoCollection<RawBsonDocument> sourceCollection = from.getCollection(collectionName, RawBsonDocument.class);
        targetDatabase.createCollection(collectionName); // retain empty collections for equal database hashes
        final MongoCollection<RawBsonDocument> targetCollection = targetDatabase.getCollection(collectionName, RawBsonDocument.class);
        logger.info("...importing "+sourceCollection.estimatedDocumentCount()+" documents from collection "+collectionName+" from "+from+" into "+this);
        final List<RawBsonDocument> documentsToInsert = new ArrayList<>(IMPORT_BATCH_SIZE);
        int batchBytes = 0;
        try (final MongoCursor<RawBsonDocument> documents = sourceCollection.find().batchSize(IMPORT_BATCH_SIZE).iterator()) {
            while (documents.hasNext()) {
                final RawBsonDocument document = documents.next();
                final int documentBytes = document.getByteBuffer().remaining();
                if (!documentsToInsert.isEmpty() && (documentsToInsert.size() >= IMPORT_BATCH_SIZE
                        || batchBytes + documentBytes > IMPORT_BATCH_BYTES)) {
                    targetCollection.insertMany(documentsToInsert);
                    documentsToInsert.clear();
                    batchBytes = 0;
                }
                documentsToInsert.add(document);
                batchBytes += documentBytes;
            }
            if (!documentsToInsert.isEmpty()) {
                targetCollection.insertMany(documentsToInsert);
            }
        }
    }

    @Override
    public boolean isInReplicaSet() throws URISyntaxException {
        return getClient().getClusterDescription().getConnectionMode() != ClusterConnectionMode.SINGLE;
    }
    
    @Override
    public ConnectionString getConnectionString(Optional<Database> optionalDb) throws URISyntaxException {
        return new ConnectionString(getURI(optionalDb).toString());
    }
    
    @Override
    public ConnectionString getConnectionString(Optional<Database> optionalDb, Optional<Duration> timeoutEmptyMeaningForever) throws URISyntaxException {
        return new ConnectionString(getURI(optionalDb, timeoutEmptyMeaningForever).toString());
    }
    
    @Override
    public MongoClient getClient() throws URISyntaxException {
        return MongoDBService.INSTANCE.getMongo(getConnectionString(Optional.empty()));
    }
    
    @Override
    public MongoClient getClient(Optional<Duration> timeoutEmptyMeaningForever) throws URISyntaxException {
        return MongoDBService.INSTANCE.getMongo(getConnectionString(Optional.empty(), timeoutEmptyMeaningForever));
    }
    
    @Override
    public ClientSession getClientSession() throws URISyntaxException {
        return getClient().startSession(ClientSessionOptions.builder().causallyConsistent(true).build());
    }
    
    @Override
    public String getMD5Hash(MongoDatabase database) throws URISyntaxException {
        return database.runCommand(new Document("dbHash", 1)).get("md5").toString();
    }

    @Override
    public String toString() {
        String uri;
        try {
            uri = getURI(Optional.empty()).toString();
        } catch (URISyntaxException e) {
            uri = "<Error determining URI: "+e.getMessage()+">";
        }
        return "MongoEndpointImpl ["+uri+"]";
    }
}
