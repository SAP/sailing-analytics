package com.sap.sailing.aiagent.persistence.impl;


import java.util.UUID;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.aiagent.persistence.MongoObjectFactory;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.CredentialsParser;
import com.sap.sse.common.Util.Pair;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private final MongoCollection<Document> eventsToCommentCollection;
    private final MongoCollection<Document> credentialsCollection;

    public MongoObjectFactoryImpl(MongoDatabase db) {
        this.eventsToCommentCollection = getOrCreateEventsToCommentCollection(db);
        this.credentialsCollection = getOrCreateCredentialsCollection(db);
    }
    
    static MongoCollection<Document> getOrCreateEventsToCommentCollection(MongoDatabase database) {
        final MongoCollection<Document> eventsToCommentCollection = database.getCollection(CollectionNames.AIAGENT_EVENTS_TO_COMMENT.name());
        return eventsToCommentCollection;
    }

    static MongoCollection<Document> getOrCreateCredentialsCollection(MongoDatabase database) {
        final MongoCollection<Document> credentialsCollection = database.getCollection(CollectionNames.AIAGENT_CREDENTIALS.name());
        return credentialsCollection;
    }

    @Override
    public void clear(ClientSession clientSessionOrNull) {
        if (clientSessionOrNull == null) {
            eventsToCommentCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).drop();
        } else {
            eventsToCommentCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).drop(clientSessionOrNull);
        }
    }
    
    @Override
    public void addEventToComment(UUID eventId, ClientSession clientSessionOrNull) {
        final Document document = new Document(FieldNames.AIAGENT_EVENTS_TO_COMMENT_EVENT_ID.name(), eventId.toString());
        if (clientSessionOrNull == null) {
            eventsToCommentCollection.insertOne(document);
        } else {
            eventsToCommentCollection.insertOne(clientSessionOrNull, document);
        }
    }

    @Override
    public void removeEventToComment(UUID eventId, ClientSession clientSessionOrNull) {
        final Document filter = new Document(FieldNames.AIAGENT_EVENTS_TO_COMMENT_EVENT_ID.name(), eventId.toString());
        if (clientSessionOrNull == null) {
            eventsToCommentCollection.deleteOne(filter);
        } else {
            eventsToCommentCollection.deleteOne(clientSessionOrNull, filter);
        }
    }

    @Override
    public void updateCredentials(Credentials credentials, ClientSession clientSessionOrNull) {
        if (clientSessionOrNull == null) {
            credentialsCollection.drop();
        } else {
            credentialsCollection.drop(clientSessionOrNull);
        }
        if (credentials != null) {
            final Pair<String, String> credentialsAsEncodedStringAndSalt = CredentialsParser.create().getAsEncodedString(credentials);
            final Document credentialsDocument = new Document()
                    .append(FieldNames.AIAGENT_CREDENTIALS_ENCODED_AS_STRING.name(), credentialsAsEncodedStringAndSalt.getA())
                    .append(FieldNames.AIAGENT_CRETENTIALS_SALT.name(), credentialsAsEncodedStringAndSalt.getB());
            if (clientSessionOrNull == null) {
                credentialsCollection.insertOne(credentialsDocument);
            } else {
                credentialsCollection.insertOne(clientSessionOrNull, credentialsDocument);
            }
        }
    }
}
