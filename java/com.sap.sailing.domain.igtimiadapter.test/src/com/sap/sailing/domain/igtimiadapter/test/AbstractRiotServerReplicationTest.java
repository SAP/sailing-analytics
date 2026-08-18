package com.sap.sailing.domain.igtimiadapter.test;

import com.sap.sailing.domain.igtimiadapter.persistence.PersistenceFactory;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sailing.domain.igtimiadapter.server.riot.impl.RiotServerImpl;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp;
import com.sap.sse.replication.testsupport.AbstractServerWithSingleServiceReplicationTest;
import com.sap.sse.security.SecurityService;

public abstract class AbstractRiotServerReplicationTest extends AbstractServerWithSingleServiceReplicationTest<RiotServer, RiotServerImpl> {
    public AbstractRiotServerReplicationTest() {
        super(new RiotServerReplicationTestSetUp());
    }
    
    public static class RiotServerReplicationTestSetUp extends AbstractServerReplicationTestSetUp<RiotServer, RiotServerImpl> {
        private MongoDBService mongoDBService;

        @Override
        protected void persistenceSetUp(boolean dropDB) {
            mongoDBService = MongoDBService.INSTANCE;
            if (dropDB) {
                mongoDBService.getDB().drop();
            }
        }

        @Override
        protected RiotServerImpl createNewMaster(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock) throws Exception {
            return new RiotServerImpl(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* context */ null);
        }

        @Override
        protected RiotServerImpl createNewReplica(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock)
                throws Exception {
            return new RiotServerImpl(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), /* context */ null);
        }
    }
}