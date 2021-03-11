package com.sap.sse.landscape.mongodb.impl;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.session.ClientSession;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoEndpoint;

public abstract class MongoEndpointImpl implements MongoEndpoint {
    private static final Logger logger = Logger.getLogger(MongoEndpointImpl.class.getName());
    
    @Override
    public Iterable<MongoDatabase> getMongoDatabases() throws URISyntaxException {
        final MongoClient client = getClient();
        return Util.map(client.listDatabaseNames(), dbName->client.getDatabase(dbName));
    }

    @Override
    public MongoDatabase getMongoDatabase(String dbName) throws URISyntaxException {
        return getClient().getDatabase(dbName);
    }

    @Override
    public MongoDatabase importDatabase(MongoDatabase from) throws URISyntaxException {
        final int BATCH_SIZE = 100;
        final MongoDatabase targetDatabase = getMongoDatabase(from.getName());
        logger.info("Importing database "+from+" into "+targetDatabase);
        for (final String collectionName : from.listCollectionNames()) {
            final MongoCollection<Document> sourceCollection = from.getCollection(collectionName);
            final MongoCollection<Document> targetCollection = targetDatabase.getCollection(collectionName);
            logger.info("...importing "+sourceCollection.count()+" documents from collection "+collectionName+" from "+from+" into "+this);
            List<Document> documentsToInsert = new ArrayList<>(BATCH_SIZE);
            int i=0;
            for (final Document document : sourceCollection.find()) {
                documentsToInsert.add(document);
                i++;
                if (i>=BATCH_SIZE) {
                    targetCollection.insertMany(documentsToInsert);
                    i = 0;
                    documentsToInsert = new ArrayList<>(BATCH_SIZE);
                }
            }
            if (i>0) {
                targetCollection.insertMany(documentsToInsert);
            }
        }
        return targetDatabase;
    }

    @Override
    public boolean isInReplicaSet() throws URISyntaxException {
        return getClient().getReplicaSetStatus() != null;
    }
    
    @Override
    public MongoClientURI getMongoClientURI(Optional<Database> optionalDb) throws URISyntaxException {
        return new MongoClientURI(getURI(optionalDb).toString());
    }
    
    @Override
    public MongoClientURI getMongoClientURI(Optional<Database> optionalDb, Optional<Duration> timeoutEmptyMeaningForever) throws URISyntaxException {
        return new MongoClientURI(getURI(optionalDb, timeoutEmptyMeaningForever).toString());
    }
    
    @Override
    public MongoClient getClient() throws URISyntaxException {
        return new MongoClient(getMongoClientURI(Optional.empty()));
    }
    
    @Override
    public MongoClient getClient(Optional<Duration> timeoutEmptyMeaningForever) throws URISyntaxException {
        return new MongoClient(getMongoClientURI(Optional.empty(), timeoutEmptyMeaningForever));
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
