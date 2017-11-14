package com.sap.sailing.expeditionconnector.persistence.impl;

import com.sap.sailing.expeditionconnector.persistence.DomainObjectFactory;
import com.sap.sailing.expeditionconnector.persistence.MongoObjectFactory;
import com.sap.sailing.expeditionconnector.persistence.PersistenceFactory;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;

public class PersistenceFactoryImpl implements PersistenceFactory {

    @Override
    public DomainObjectFactory getDomainObjectFactory(MongoDBService mongoDbService) {
        return new DomainObjectFactoryImpl(mongoDbService.getDB());
    }

    @Override
    public MongoObjectFactory getMongoObjectFactory(MongoDBService mongoDbService) {
        return new MongoObjectFactoryImpl(mongoDbService.getDB());
    }

    @Override
    public DomainObjectFactory getDefaultDomainObjectFactory() {
        return getDomainObjectFactory(MongoDBConfiguration.getDefaultConfiguration().getService());
    }

    @Override
    public MongoObjectFactory getDefaultMongoObjectFactory() {
        return getMongoObjectFactory(MongoDBConfiguration.getDefaultConfiguration().getService());
    }

}
