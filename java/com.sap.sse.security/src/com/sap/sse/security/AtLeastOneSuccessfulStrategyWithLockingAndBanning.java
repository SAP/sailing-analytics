package com.sap.sse.security;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.realm.Realm;

import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.util.ServiceTrackerFactory;

public class AtLeastOneSuccessfulStrategyWithLockingAndBanning extends AtLeastOneSuccessfulStrategy {
    private static final Logger logger = Logger.getLogger(AtLeastOneSuccessfulStrategyWithLockingAndBanning.class.getName());
    
    private final Future<SecurityService> securityService;
    
    public AtLeastOneSuccessfulStrategyWithLockingAndBanning() {
        if (Activator.getContext() != null) {
            securityService = ServiceTrackerFactory.createServiceFuture(Activator.getContext(), SecurityService.class);
        } else {
            securityService = null;
        }
    }
    
    private SecurityService getSecurityService() {
        SecurityService result;
        try {
            result = securityService == null ? null : securityService.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, "Error retrieving security service", e);
            result = null;
        }
        return result;
    }

    @Override
    public AuthenticationInfo afterAttempt(Realm realm, AuthenticationToken token, AuthenticationInfo singleRealmInfo,
            AuthenticationInfo aggregateInfo, Throwable t) throws AuthenticationException {
        if (token != null && token.getPrincipal() != null && realm instanceof UsernamePasswordRealm) {
            final UsernamePasswordRealm upRealm = (UsernamePasswordRealm) realm;
            final String username = token.getPrincipal().toString();
            final User user = upRealm.getUserStore().getUserByName(username);
            if (user != null) {
                if (t != null) {
                    if (t instanceof IncorrectCredentialsException) {
                        logger.info("failed password authentication for user "+username);
                        final SecurityService mySecurityService = getSecurityService();
                        if (mySecurityService != null) {
                            final LockingAndBanning lockingAndBanning = mySecurityService.failedPasswordAuthentication(user);
                            if (lockingAndBanning != null) {
                                logger.info("User "+username+" locked for password authentication: "+lockingAndBanning);
                            }
                        } else {
                            logger.warning("Account locking due to failed password authentication for user "+username+" not possible; security service not found");
                        }
                    }
                } else {
                    // no exception, so the authentication must have been successful
                    final SecurityService mySecurityService = getSecurityService();
                    if (mySecurityService != null) {
                        mySecurityService.successfulPasswordAuthentication(user);
                    }
                }
            }
        } else if (token != null && realm instanceof BearerTokenRealm) {
            final BearerAuthenticationToken bearerToken = (BearerAuthenticationToken) token;
            if (singleRealmInfo == null || singleRealmInfo.getPrincipals().isEmpty()) {
                if (t != null && t instanceof LockedAccountException) {
                    logger.fine(()->"Bearer token authentication from client IP "+bearerToken.getClientIP()+" with user agent "+bearerToken.getUserAgent()+" currently locked");
                } else {
                    // authentication failed
                    logger.info("failed bearer token authentication for client IP "+bearerToken.getClientIP()+" with user agent "+bearerToken.getUserAgent());
                    final SecurityService mySecurityService = getSecurityService();
                    if (mySecurityService != null) {
                        final LockingAndBanning lockingAndBanning = mySecurityService.failedBearerTokenAuthentication(bearerToken.getClientIP());
                        if (lockingAndBanning != null) {
                            logger.info("Client IP "+bearerToken.getClientIP()+" locked for bearer token authentication: "+lockingAndBanning);
                        }
                    } else {
                        logger.warning("Client IP locking due to failed bearer token authentication for client IP "
                                +bearerToken.getClientIP()+" with user agent "+bearerToken.getUserAgent()
                                +" not possible; security service not found");
                    }
                }
            } else { // valid authentication info means authentication was successful
                final SecurityService mySecurityService = getSecurityService();
                if (mySecurityService != null) {
                    mySecurityService.successfulBearerTokenAuthentication(bearerToken.getClientIP());
                }
            }
        }
        return super.afterAttempt(realm, token, singleRealmInfo, aggregateInfo, t);
    }
}
