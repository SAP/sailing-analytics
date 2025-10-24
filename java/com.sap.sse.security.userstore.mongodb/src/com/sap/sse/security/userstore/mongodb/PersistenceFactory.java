package com.sap.sse.security.userstore.mongodb;

import com.mongodb.client.ClientSession;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.userstore.mongodb.impl.PersistenceFactoryImpl;

public interface PersistenceFactory {
    PersistenceFactory INSTANCE = new PersistenceFactoryImpl();
    
    static PersistenceFactory create(MongoDBService mongoDBService) {
        return new PersistenceFactoryImpl(mongoDBService);
    }
    
    static PersistenceFactory create(ClientSession clientSessionForMajorityFactories, MongoDBService mongoDBService) {
        return new PersistenceFactoryImpl(clientSessionForMajorityFactories, mongoDBService);
    }
    
    DomainObjectFactory getDefaultDomainObjectFactory();
    MongoObjectFactory getDefaultMongoObjectFactory();

    /**
     * @return a domain object factory for reading objects from the DB, with "majority" read concern as well as a
     *         causally consistent MongoDB client session, reading our own writes when written through the Mongo object
     *         factory obtained by {@link #getDefaultMajorityMongoObjectFactory()}.
     */
    DomainObjectFactory getDefaultMajorityDomainObjectFactory();

    /**
     * @return a Mongo object factory for writing objects from the DB, with "majority" write concern as well as a
     *         causally consistent MongoDB client session, allowing for reading our own writes when read through the
     *         domain object factory obtained by {@link #getDefaultMajorityDomainObjectFactory()}.
     */
    MongoObjectFactory getDefaultMajorityMongoObjectFactory();
}
