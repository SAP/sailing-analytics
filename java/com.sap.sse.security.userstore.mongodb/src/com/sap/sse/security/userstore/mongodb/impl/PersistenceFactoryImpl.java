package com.sap.sse.security.userstore.mongodb.impl;

import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.userstore.mongodb.DomainObjectFactory;
import com.sap.sse.security.userstore.mongodb.MongoObjectFactory;
import com.sap.sse.security.userstore.mongodb.PersistenceFactory;
import com.sap.sse.security.userstore.mongodb.impl.sessionwrapper.MongoDatabaseWrapperWithClientSession;

public class PersistenceFactoryImpl implements PersistenceFactory {
    private final DomainObjectFactory defaultDomainObjectFactory;
    private final MongoObjectFactory defaultMongoObjectFactory;
    private final DomainObjectFactory defaultMajorityDomainObjectFactory;
    private final MongoObjectFactory defaultMajorityMongoObjectFactory;
    
    public PersistenceFactoryImpl() {
        this(MongoDBService.INSTANCE);
    }

    public PersistenceFactoryImpl(MongoDBService mongoDBService) {
        this(mongoDBService.startCausallyConsistentSession(), mongoDBService);
    }
    
    public PersistenceFactoryImpl(ClientSession clientSessionForMajorityFactories, MongoDBService mongoDBService) {
        this.defaultDomainObjectFactory = new DomainObjectFactoryImpl(mongoDBService.getDB());
        this.defaultMongoObjectFactory = new MongoObjectFactoryImpl(mongoDBService.getDB());
        this.defaultMajorityDomainObjectFactory = new DomainObjectFactoryImpl(
                new MongoDatabaseWrapperWithClientSession(
                        clientSessionForMajorityFactories,
                        mongoDBService.getDB().withReadConcern(ReadConcern.MAJORITY).withWriteConcern(WriteConcern.MAJORITY)));
        this.defaultMajorityMongoObjectFactory = new MongoObjectFactoryImpl(
                new MongoDatabaseWrapperWithClientSession(
                        clientSessionForMajorityFactories,
                        mongoDBService.getDB().withReadConcern(ReadConcern.MAJORITY).withWriteConcern(WriteConcern.MAJORITY)));
    }
    
    /**
     * A default domain object factory for test purposes only.
     */
    @Override
    public DomainObjectFactory getDefaultDomainObjectFactory() {
        return defaultDomainObjectFactory;
    }

    @Override
    public MongoObjectFactory getDefaultMongoObjectFactory() {
        return defaultMongoObjectFactory;
    }

    @Override
    public DomainObjectFactory getDefaultMajorityDomainObjectFactory() {
        return defaultMajorityDomainObjectFactory;
    }

    @Override
    public MongoObjectFactory getDefaultMajorityMongoObjectFactory() {
        return defaultMajorityMongoObjectFactory;
    }
}
