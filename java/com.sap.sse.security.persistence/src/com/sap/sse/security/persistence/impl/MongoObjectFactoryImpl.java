package com.sap.sse.security.persistence.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.sap.sse.security.persistence.MongoObjectFactory;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private static final Logger logger = Logger.getLogger(MongoObjectFactoryImpl.class.getName());
    private final MongoCollection<Document> sessionCollection;
    private final MongoCollection<Document> corsFilterConfigurationsCollection;
    
    public MongoObjectFactoryImpl(MongoDatabase mongoDatabase) {
        sessionCollection = mongoDatabase.getCollection(CollectionNames.SESSIONS.name());
        sessionCollection.createIndex(new Document().
                append(FieldNames.CACHE_NAME.name(), 1).
                append(FieldNames.SESSION_ID.name(), 1), new IndexOptions().name("cachenameandsessionid").background(false));
        corsFilterConfigurationsCollection = mongoDatabase.getCollection(CollectionNames.CORS_FILTER_CONFIGURATIONS.name());
        corsFilterConfigurationsCollection.createIndex(new Document().
                append(FieldNames.CORS_FILTER_CONFIGURATION_SERVER_NAME.name(), 1), new IndexOptions().name("servername").background(false));
    }

    private Document getKey(String cacheName, Session session) {
        return getKey(cacheName, session.getId());
    }
    
    static Document getKey(String cacheName, Serializable sessionId) {
        return new Document().
                append(FieldNames.CACHE_NAME.name(), cacheName).
                append(FieldNames.SESSION_ID.name(), sessionId);
    }
    
    @Override
    public void storeSession(String cacheName, Session session) {
        final Document sessionAsDocument = getKey(cacheName, session).
                append(FieldNames.SESSION_HOST.name(), session.getHost()).
                append(FieldNames.SESSION_LAST_ACCESS_TIME.name(), session.getLastAccessTime()).
                append(FieldNames.SESSION_START_TIMESTAMP.name(), session.getStartTimestamp()).
                append(FieldNames.SESSION_TIMEOUT.name(), session.getTimeout());
        final List<Document> sessionAttributes = new ArrayList<>();
        for (Object attributeKey : session.getAttributeKeys()) {
            if (attributeKey instanceof String) {
                final Object attributeValue = session.getAttribute(attributeKey);
                if (attributeValue instanceof PrincipalCollection) {
                    sessionAttributes.add(new Document().
                            append(FieldNames.SESSION_ATTRIBUTE_NAME.name(), attributeKey).
                            append(FieldNames.SESSION_ATTRIBUTE_VALUE.name(), storePrincipalCollection((PrincipalCollection) attributeValue)));
                } else if (attributeValue instanceof String || attributeValue instanceof Number || attributeValue instanceof Boolean || attributeValue instanceof Character) {
                    sessionAttributes.add(new Document().
                            append(FieldNames.SESSION_ATTRIBUTE_NAME.name(), attributeKey).
                            append(FieldNames.SESSION_ATTRIBUTE_VALUE.name(), attributeValue));
                } else {
                    logger.fine("Ignoring session attribute "+attributeKey+" with value "+attributeValue+
                            "of type "+attributeValue.getClass().getName()+" because values of this type cannot be stored");
                }
            } else {
                logger.warning("Attribute key "+attributeKey+" of session "+session+" is not of type String but of type "+
                        attributeKey.getClass().getName()+" and cannot be stored");
            }
        }
        sessionAsDocument.append(FieldNames.SESSION_ATTRIBUTES.name(), sessionAttributes);
        final Document key = getKey(cacheName, session);
        sessionCollection.replaceOne(key, sessionAsDocument, new ReplaceOptions().upsert(true));
    }
    
    private List<Document> storePrincipalCollection(PrincipalCollection principalCollection) {
        final List<Document> result = new ArrayList<>();
        for (final String realmName : principalCollection.getRealmNames()) {
            final List<String> principalNames = new ArrayList<>();
            for (Object o : principalCollection.fromRealm(realmName)) {
                principalNames.add(o.toString());
            }
            final Document realmDocument = new Document().
                    append(FieldNames.SESSION_PRINCIPAL_REALM_NAME.name(), realmName).
                    append(FieldNames.SESSION_PRINCIPAL_REALM_VALUE.name(), principalNames);
            result.add(realmDocument);
        }
        return result;
    }

    @Override
    public void removeAllSessions(String cacheName) {
        sessionCollection.deleteMany(new Document(FieldNames.CACHE_NAME.name(), cacheName));
    }
    
    @Override
    public void removeSession(String cacheName, Session session) {
        sessionCollection.deleteOne(getKey(cacheName, session));
    }

    @Override
    public void storeCORSFilterConfigurationIsWildcard(String serverName) {
        final Document filter = new Document().append(FieldNames.CORS_FILTER_CONFIGURATION_SERVER_NAME.name(), serverName);
        final Document d = new Document()
                .append(FieldNames.CORS_FILTER_CONFIGURATION_SERVER_NAME.name(), serverName)
                .append(FieldNames.CORS_FILTER_CONFIGURATION_IS_WILDCARD.name(), true)
                .append(FieldNames.CORS_FILTER_CONFIGURATION_ALLOWED_ORIGINS.name(), Collections.emptyList());
        corsFilterConfigurationsCollection.replaceOne(filter, d, new ReplaceOptions().upsert(true));
    }

    @Override
    public void storeCORSFilterConfigurationAllowedOrigins(String serverName, String... allowedOrigins) {
        final Document filter = new Document().append(FieldNames.CORS_FILTER_CONFIGURATION_SERVER_NAME.name(), serverName);
        final Document d = new Document()
                .append(FieldNames.CORS_FILTER_CONFIGURATION_SERVER_NAME.name(), serverName)
                .append(FieldNames.CORS_FILTER_CONFIGURATION_IS_WILDCARD.name(), false)
                .append(FieldNames.CORS_FILTER_CONFIGURATION_ALLOWED_ORIGINS.name(), allowedOrigins == null ? Collections.emptyList() : Arrays.asList(allowedOrigins));
        corsFilterConfigurationsCollection.replaceOne(filter, d, new ReplaceOptions().upsert(true));
    }
}
