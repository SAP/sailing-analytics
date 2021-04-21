package com.sap.sse.security.replication.test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.sap.sse.common.mail.MailException;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp;
import com.sap.sse.replication.testsupport.AbstractServerWithSingleServiceReplicationTest;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
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
        protected SecurityServiceImpl createNewMaster() throws MalformedURLException, IOException, InterruptedException,
                MailException, UserStoreManagementException {
            final UserStoreImpl userStore = new UserStoreImpl("TestDefaultTenant");
            userStore.ensureDefaultRolesExist();
            userStore.loadAndMigrateUsers();
            final AccessControlStore accessControlStore = new AccessControlStoreImpl(userStore);
            SecurityServiceImpl result = new SecurityServiceImpl(null, userStore, accessControlStore,
                    SecuredSecurityTypes::getAllInstances);
            return result;
        }

        @Override
        protected SecurityServiceImpl createNewReplica()
                throws UserStoreManagementException, MalformedURLException, IOException, InterruptedException {
            final UserStoreImpl userStore = new UserStoreImpl("TestDefaultTenant");
            userStore.ensureDefaultRolesExist();
            userStore.loadAndMigrateUsers();
            final AccessControlStore accessControlStore = new AccessControlStoreImpl(userStore);
            return new SecurityServiceImpl(null, userStore, accessControlStore, SecuredSecurityTypes::getAllInstances);
        }
    }
}
