package com.sap.sailing.aiagent.persistence;

import com.sap.sailing.aiagent.persistence.impl.PersistenceFactoryImpl;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;

public interface PersistenceFactory {
    PersistenceFactory INSTANCE = new PersistenceFactoryImpl();

    /**
     * Obtains the domain object factory using the default persistence settings from {@link MongoDBConfiguration#getDefaultConfiguration()}.
     */
    DomainObjectFactory getDefaultDomainObjectFactory();
    
    DomainObjectFactory getDomainObjectFactory(MongoDBService mongoDbService);

    /**
     * Obtains the Mongo object factory using the default persistence settings from {@link MongoDBConfiguration#getDefaultConfiguration()}.
     */
    MongoObjectFactory getDefaultMongoObjectFactory();
    
    MongoObjectFactory getMongoObjectFactory(MongoDBService mongoDbService);
}
