package com.sap.sse.security.persistence.test;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.persistence.DomainObjectFactory;
import com.sap.sse.security.persistence.MongoObjectFactory;
import com.sap.sse.security.persistence.PersistenceFactory;

public abstract class AbstractSecurityPersistenceTest {
    protected DomainObjectFactory dof;
    protected MongoObjectFactory mof;
    
    @BeforeEach
    public void setUp() {
        MongoDBService.INSTANCE.getDB().drop();
        dof = PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory();
        mof = PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory();
    }
}
