package com.sap.sailing.domain.igtimiadapter.persistence.impl;

import com.sap.sailing.domain.igtimiadapter.persistence.DomainObjectFactory;
import com.sap.sailing.domain.igtimiadapter.persistence.MongoObjectFactory;
import com.sap.sailing.domain.igtimiadapter.persistence.PersistenceFactory;
import com.sap.sailing.mongodb.MongoDBConfiguration;
import com.sap.sailing.mongodb.MongoDBService;

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
