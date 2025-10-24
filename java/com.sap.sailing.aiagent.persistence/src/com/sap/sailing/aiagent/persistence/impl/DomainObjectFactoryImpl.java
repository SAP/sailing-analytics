package com.sap.sailing.aiagent.persistence.impl;

import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.aiagent.persistence.DomainObjectFactory;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.CredentialsParser;
import com.sap.sse.common.Util;

public class DomainObjectFactoryImpl implements DomainObjectFactory {
    private final MongoCollection<Document> eventsToCommentCollection;
    private final MongoCollection<Document> credentialsCollection;
    
    public DomainObjectFactoryImpl(MongoDatabase db) {
        eventsToCommentCollection = MongoObjectFactoryImpl.getOrCreateEventsToCommentCollection(db);
        credentialsCollection = MongoObjectFactoryImpl.getOrCreateCredentialsCollection(db);
    }

    @Override
    public Iterable<UUID> getEventsToComment(ClientSession clientSessionOrNull) {
        final FindIterable<Document> documents;
        if (clientSessionOrNull == null) {
            documents = eventsToCommentCollection.find();
        } else {
            documents = eventsToCommentCollection.find(clientSessionOrNull);
        }
        return Util.map(documents, d->UUID.fromString(d.getString(FieldNames.AIAGENT_EVENTS_TO_COMMENT_EVENT_ID.name())));
    }

    @Override
    public Credentials getCredentials(ClientSession clientSessionOrNull) {
        final FindIterable<Document> documents;
        if (clientSessionOrNull == null) {
            documents = credentialsCollection.find();
        } else {
            documents = credentialsCollection.find(clientSessionOrNull);
        }
        final Iterable<Credentials> credentials = Util.map(documents, d->CredentialsParser.create().parseFromEncoded(
                        d.getString(FieldNames.AIAGENT_CREDENTIALS_ENCODED_AS_STRING.name()),
                        d.getString(FieldNames.AIAGENT_CRETENTIALS_SALT.name())));
        return Util.first(credentials);
    }
}
