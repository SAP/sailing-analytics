package com.sap.sailing.domain.persistence;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.persistence.impl.PersistenceFactoryImpl;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.mongodb.MongoDBService;

public interface PersistenceFactory {
    PersistenceFactory INSTANCE = new PersistenceFactoryImpl();
    
    DomainObjectFactory getDefaultDomainObjectFactory();
    DomainObjectFactory getDomainObjectFactory(MongoDBService mongoDBService, DomainFactory baseDomainFactory);
    DomainObjectFactory getMajorityDomainObjectFactory(MongoDBService mongoDBService, DomainFactory baseDomainFactory);
    DomainObjectFactory getDomainObjectFactory(MongoDBService mongoDBService, DomainFactory baseDomainFactory, TypeBasedServiceFinderFactory serviceFinderFactory);
    MongoObjectFactory getDefaultMongoObjectFactory();
    MongoObjectFactory getMongoObjectFactory(MongoDBService mongoDBService);
    MongoObjectFactory getMajorityMongoObjectFactory(MongoDBService mongoDBService);
    MongoObjectFactory getDefaultMongoObjectFactory(TypeBasedServiceFinderFactory serviceFinderFactory);
    MongoObjectFactory getMongoObjectFactory(MongoDBService mongoDBService, TypeBasedServiceFinderFactory serviceFinderFactory);
}
