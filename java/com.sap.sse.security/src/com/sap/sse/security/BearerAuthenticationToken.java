package com.sap.sse.security;

import java.util.logging.Logger;

import org.apache.shiro.authc.AuthenticationToken;

import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.shared.impl.User;

/**
 * An access token as issued by {@link SecurityService#createAccessToken(String)}. The user name which is returned
 * by {@link #getPrincipal()} is obtained by looking up the access token using the {@link SecurityService}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class BearerAuthenticationToken implements AuthenticationToken {
    private static final long serialVersionUID = 8528031991813216585L;
    private static final Logger logger = Logger.getLogger(BearerAuthenticationToken.class.getName());

    private final String token;
    private final String clientIP;
    private final String userAgent;
    
    public BearerAuthenticationToken(String token, String clientIP, String userAgent) {
        super();
        this.token = token;
        this.clientIP = clientIP;
        this.userAgent = userAgent;
    }

    @Override
    public Object getPrincipal() {
        SecurityService securityService = Activator.getSecurityService();
        User user = securityService.getUserByAccessToken(token);
        if (user == null) {
            logger.fine("Invalid bearer token did not authenticate an existing user");
        }
        return user == null ? null : user.getName();
    }

    @Override
    public String getCredentials() {
        return token;
    }

    public String getClientIP() {
        return clientIP;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
