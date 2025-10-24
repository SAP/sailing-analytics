package com.sap.sse.security.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;

public class SimpleSecurityReplicationTest extends AbstractSecurityReplicationTest {
    private static final String ERNIE2_SESAME_STREET_COM = "ernie2@sesame-street.com";
    private static final String HTTP_ME_TO_BACK_COM = "http://me.to.back.com";
    private static final String ERNIE_S_COMPANY = "Ernie's Company";
    private static final String ERNIE_S_FULL_NAME = "Ernie's Full Name";
    private static final String BERT_MY_FRIEND = "BertMyFriend";
    private static final String ERNIE_SESAME_STREET_COM = "ernie@sesame-street.com";
    private static final String ERNIE = "Ernie";

    @Test
    public void testSimpleReplicationOfUserCreation() throws InterruptedException, UserManagementException, MailException, IllegalAccessException, UserGroupManagementException {
        assertNull(master.getUserByName(ERNIE));
        User user = master.createSimpleUser(ERNIE, ERNIE_SESAME_STREET_COM, BERT_MY_FRIEND, ERNIE_S_FULL_NAME, ERNIE_S_COMPANY, Locale.ENGLISH,
                HTTP_ME_TO_BACK_COM, null, /* clientIP */ null, /* enforce strong password */ false);
        assertNotNull(user);
        assertSame(user, master.getUserByName(ERNIE));
        assertTrue(master.checkPassword(ERNIE, BERT_MY_FRIEND));
        final String emailValidationSecret = user.getValidationSecret();
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        User replicatedErnie = replica.getUserByName(ERNIE);
        assertNotNull(replicatedErnie);
        assertEquals(ERNIE, replicatedErnie.getName());
        assertEquals(ERNIE_SESAME_STREET_COM, replicatedErnie.getEmail());
        assertTrue(replica.checkPassword(ERNIE, BERT_MY_FRIEND));
        assertEquals(emailValidationSecret, replicatedErnie.getValidationSecret());
        assertEquals(ERNIE_S_FULL_NAME, replicatedErnie.getFullName());
        assertEquals(ERNIE_S_COMPANY, replicatedErnie.getCompany());
        // check that incremental replication of access token handling works
        final String accessToken = master.createAccessToken(ERNIE);
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        assertEquals(ERNIE, replica.getUserByAccessToken(accessToken).getName());
    }

    @Test
    public void testSimpleReplicationOfUserEmailChange() throws InterruptedException, UserManagementException, MailException, IllegalAccessException, UserGroupManagementException {
        User user = master.createSimpleUser(ERNIE, ERNIE_SESAME_STREET_COM, BERT_MY_FRIEND, ERNIE_S_FULL_NAME, ERNIE_S_COMPANY, Locale.ENGLISH,
                HTTP_ME_TO_BACK_COM, master.getDefaultTenantForCurrentUser(), /* clientIP */ null, /* enforce strong password */ false);
        user.setFullName(ERNIE_S_FULL_NAME);
        user.setCompany(ERNIE_S_COMPANY);
        final String emailValidationSecretAfterCreation = user.getValidationSecret();
        master.updateSimpleUserEmail(ERNIE, ERNIE2_SESAME_STREET_COM, HTTP_ME_TO_BACK_COM);
        final String emailValidationSecretAfterChangingEmail = user.getValidationSecret();
        assertFalse(emailValidationSecretAfterChangingEmail.equals(emailValidationSecretAfterCreation));
        assertEquals(ERNIE2_SESAME_STREET_COM, user.getEmail());
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        User replicatedErnie = replica.getUserByName(ERNIE);
        assertNotNull(replicatedErnie);
        assertEquals(ERNIE, replicatedErnie.getName());
        assertEquals(ERNIE2_SESAME_STREET_COM, replicatedErnie.getEmail());
        assertEquals(emailValidationSecretAfterChangingEmail, replicatedErnie.getValidationSecret());
        assertEquals(ERNIE_S_FULL_NAME, replicatedErnie.getFullName());
        assertEquals(ERNIE_S_COMPANY, replicatedErnie.getCompany());
    }

    @Test
    public void testSimpleReplicationOfUserPasswordChange() throws InterruptedException, UserManagementException, MailException, IllegalAccessException, UserGroupManagementException {
        final String newPassword = "ErnieAndBert";
        master.createSimpleUser(ERNIE, ERNIE_SESAME_STREET_COM, BERT_MY_FRIEND,
                /* fullName */ null, /* company */ null, Locale.ENGLISH, HTTP_ME_TO_BACK_COM,
                null, /* clientIP */ null, /* enforce strong password */ false);
        master.updateSimpleUserPassword(ERNIE, newPassword);
        assertTrue(master.checkPassword(ERNIE, newPassword));
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        User replicatedErnie = replica.getUserByName(ERNIE);
        assertNotNull(replicatedErnie);
        assertEquals(ERNIE, replicatedErnie.getName());
        assertFalse(replica.checkPassword(ERNIE, BERT_MY_FRIEND));
        // checking with incorrect password locks user for some time; wait long enough before retrying with correct password
        final TimePoint lockedUntil = replicatedErnie.getLockingAndBanning().getLockedUntil();
        Thread.sleep(Math.max(0, TimePoint.now().until(lockedUntil).asMillis()+10));
        assertTrue(replica.checkPassword(ERNIE, newPassword));
    }

    @Test
    public void testReplicationOfPasswordReset() throws InterruptedException, UserManagementException, MailException, IllegalAccessException, UserGroupManagementException {
        final String validationBaseURL = "http://me.to.back.com/validateemail";
        final String passwordResetBaseURL = "http://me.to.back.com/passwordreset";
        User user = master.createSimpleUser(ERNIE, ERNIE_SESAME_STREET_COM, BERT_MY_FRIEND,
                /* fullName */ null, /* company */ null, Locale.ENGLISH, validationBaseURL,
                null, /* clientIP */ null, /* enforce strong password */ false);
        master.validateEmail(ERNIE, user.getValidationSecret());
        assertTrue(user.isEmailValidated());
        master.resetPassword(ERNIE, passwordResetBaseURL);
        String passwordResetSecret = user.getPasswordResetSecret();
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        User replicatedErnie = replica.getUserByName(ERNIE);
        assertNotNull(replicatedErnie);
        assertTrue(replicatedErnie.isEmailValidated());
        assertEquals(passwordResetSecret, replicatedErnie.getPasswordResetSecret());
    }
    
    @Test
    public void testReplicationOfLegacyRole() throws InterruptedException, UserManagementException, MailException, IllegalAccessException, UserGroupManagementException {
        final UUID roleDefinitionUUID = UUID.randomUUID();
        final String roleDefinitionName = "roleDefinition";
        final WildcardPermission permission = new WildcardPermission("EVENT:READ:*");
        final Set<WildcardPermission> permissions = Collections.singleton(permission);
        master.createSimpleUser(ERNIE, ERNIE_SESAME_STREET_COM, BERT_MY_FRIEND,
                /* fullName */ null, /* company */ null, Locale.ENGLISH, null,
                null, /* clientIP */ null, /* enforce strong password */ false);
        final RoleDefinition roleDefinition = master.createRoleDefinition(roleDefinitionUUID, roleDefinitionName);
        roleDefinition.setPermissions(permissions);
        master.updateRoleDefinition(roleDefinition);
        final Role role = new Role(roleDefinition, null);
        master.addRoleForUser(ERNIE, role);
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        // If no transitivity indication was given, the roles transitivity should default to true
        assert(excecutePermissionCheckUnderUserSubject(() -> replica.hasCurrentUserMetaPermission(permission, null)));
    }
    
    @Test
    public void testReplicationONonTransitiveRole() throws InterruptedException, UserManagementException, MailException, IllegalAccessException, UserGroupManagementException {
        final UUID roleDefinitionUUID = UUID.randomUUID();
        final String roleDefinitionName = "roleDefinition";
        final WildcardPermission permission = new WildcardPermission("EVENT:READ:*");
        final Set<WildcardPermission> permissions = Collections.singleton(permission);
        master.createSimpleUser(ERNIE, ERNIE_SESAME_STREET_COM, BERT_MY_FRIEND,
                /* fullName */ null, /* company */ null, Locale.ENGLISH, null,
                null, /* clientIP */ null, /* enforce strong password */ false);
        final RoleDefinition roleDefinition = master.createRoleDefinition(roleDefinitionUUID, roleDefinitionName);
        roleDefinition.setPermissions(permissions);
        master.updateRoleDefinition(roleDefinition);
        final Role role = new Role(roleDefinition, false);
        master.addRoleForUser(ERNIE, role);
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        // If no transitivity indication was given, the roles transitivity should default to true
        assertFalse(excecutePermissionCheckUnderUserSubject(() -> replica.hasCurrentUserMetaPermission(permission, null)));
    }
    
    private boolean excecutePermissionCheckUnderUserSubject(Callable<Boolean> callable) {
        PrincipalCollection principals = new SimplePrincipalCollection(Collections.singleton(ERNIE), "MyRealm");
        Subject subject = new Subject.Builder().principals(principals).authenticated(true).buildSubject();
        return subject.execute(callable);
    }

}
