package com.sap.sse.security.replication.test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.sap.sse.common.mail.MailException;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp;
import com.sap.sse.replication.testsupport.AbstractServerWithSingleServiceReplicationTest;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public abstract class AbstractSecurityReplicationTest extends AbstractServerWithSingleServiceReplicationTest<SecurityService, SecurityServiceImpl> {
    public AbstractSecurityReplicationTest() {
        super(new SecurityServerReplicationTestSetUp());
    }
    
    public static class SecurityServerReplicationTestSetUp extends AbstractServerReplicationTestSetUp<SecurityService, SecurityServiceImpl> {
        private MongoDBService mongoDBService;

        @Override
        protected void persistenceSetUp(boolean dropDB) {
            mongoDBService = MongoDBService.INSTANCE;
            if (dropDB) {
                mongoDBService.getDB().drop();
            }
        }

        @Override
        protected SecurityServiceImpl createNewMaster(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock) throws MalformedURLException, IOException, InterruptedException,
                MailException, UserStoreManagementException {
            final UserStoreImpl userStore = new UserStoreImpl("TestDefaultTenant");
            userStore.ensureDefaultRolesExist();
            userStore.loadAndMigrateUsers();
            final AccessControlStore accessControlStore = new AccessControlStoreImpl(userStore);
            SecurityServiceImpl result = new SecurityServiceImpl(/* mailServiceTracker */ null, /* corsFilterConfigurationTracker */ null, userStore,
                    accessControlStore, SecuredSecurityTypes::getAllInstances, SSESubscriptionPlan::getAllInstances);
            return result;
        }

        @Override
        protected SecurityServiceImpl createNewReplica(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock)
                throws UserStoreManagementException, MalformedURLException, IOException, InterruptedException {
            final UserStoreImpl userStore = new UserStoreImpl("TestDefaultTenant");
            userStore.ensureDefaultRolesExist();
            userStore.loadAndMigrateUsers();
            final AccessControlStore accessControlStore = new AccessControlStoreImpl(userStore);
            return new SecurityServiceImpl(/* mailServiceTracker */ null, /* corsFilterConfigurationTracker */ null, userStore,
                    accessControlStore, SecuredSecurityTypes::getAllInstances, SSESubscriptionPlan::getAllInstances);
        }
    }
}
