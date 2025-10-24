package com.sap.sse.security.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.mail.MailException;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.testsupport.AbstractServerWithSingleServiceReplicationTest;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.PersistenceFactory;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class SecurityServiceInitialLoadTest extends AbstractServerWithSingleServiceReplicationTest<SecurityService, SecurityServiceImpl> {
    private final static String username = "abc";
    private final static String email = "e@mail.com";
    private final static String password = "password";
    private final static String fullName = "Full Name";
    private final static String company = "Company";
    private static String accessToken = "Company";
    
    public SecurityServiceInitialLoadTest() {
        super(new AbstractSecurityReplicationTest.SecurityServerReplicationTestSetUp() {
            @Override
            protected SecurityServiceImpl createNewMaster(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock)
                    throws MalformedURLException, IOException, InterruptedException, MailException, UserStoreManagementException {
                final UserStore userStore = new UserStoreImpl(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(),
                        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), "TestDefaultTenant");
                userStore.ensureDefaultRolesExist();
                userStore.loadAndMigrateUsers();
                final AccessControlStore accessControlStore = new AccessControlStoreImpl(
                        PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(),
                        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), userStore);
                final SecurityServiceImpl newMaster = new SecurityServiceImpl(null, /* corsFilterConfigurationTracker */ null, userStore,
                        accessControlStore, SecuredSecurityTypes::getAllInstances, SSESubscriptionPlan::getAllInstances);
                newMaster.createSimpleUser(username, email, password, fullName, company,
                        /* validationBaseURL */ Locale.ENGLISH, null, null, /* clientIP */ null,
                        /* enforce strong password */ false);
                accessToken = newMaster.createAccessToken(username);
                return newMaster;
            }
        });
    }

    @Test
    public void simpleMasterTest() {
        assertEquals(username, master.getUserByName(username).getName());
        assertEquals(username, master.getUserByAccessToken(accessToken).getName());
    }
    
    @Test
    public void simpleReplicaTest() {
        assertEquals(username, replica.getUserByName(username).getName());
        assertEquals(username, replica.getUserByAccessToken(accessToken).getName());
    }
}
