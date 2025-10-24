package com.sap.sse.security;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SaltedAuthenticationInfo;

import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.interfaces.SimpleSaltedAuthenticationInfo;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.util.ServiceTrackerFactory;

/**
 * A realm that authenticates users by an access token which has previously been obtained through
 * {@link SecurityService#createAccessToken(String)}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class BearerTokenRealm extends AbstractCompositeAuthorizingRealm {
    private static final Logger logger = Logger.getLogger(BearerTokenRealm.class.getName());

    private final Future<SecurityService> securityService;

    public BearerTokenRealm() {
        super();
        securityService = Activator.getContext() == null ? null : ServiceTrackerFactory.createServiceFuture(Activator.getContext(), SecurityService.class);
        setAuthenticationTokenClass(BearerAuthenticationToken.class);
    }
    
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        final AuthenticationInfo result;
        BearerAuthenticationToken accessToken = (BearerAuthenticationToken) token;
        try {
            final SecurityService mySecurityService = securityService == null ? null : securityService.get();
            if (mySecurityService != null) {
                if (mySecurityService.isClientIPLockedForBearerTokenAuthentication(accessToken.getClientIP())) {
                    throw new LockedAccountException("Authentication for client IP "+accessToken.getClientIP()
                        +" with user agent "+accessToken.getUserAgent()
                        +" is currently locked");
                }
            } else {
                logger.warning("Cannot check whether client IP/User-Agent is locked for bearer token-based authentication; security service not found");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, "Error retrieving security service", e);
        }
        final User user = getUserStore().getUserByAccessToken(accessToken.getCredentials());
        if (user == null) {
            result = null;
        } else {
            // return salted credentials
            SaltedAuthenticationInfo sai = new SimpleSaltedAuthenticationInfo(user.getName(), accessToken.getCredentials(), /* salt */ null);
            result = sai;
        }
        return result;
    }
}
