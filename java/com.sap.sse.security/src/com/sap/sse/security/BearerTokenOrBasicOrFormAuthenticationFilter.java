package com.sap.sse.security;


import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

import com.sap.sse.security.impl.FormAuthenticationFilterWithPublicCreateToken;
import com.sap.sse.security.jaxrs.api.SecurityResource;
import com.sap.sse.util.HttpRequestUtils;

/**
 * Looks for an "Authorization: Bearer &lt;token&gt;" HTTP header. If found, tries to authenticate a user
 * with the bearer token presented. If instead an "Authorization: Basic ..." header field is found, HTTP
 * basic authentication is attempted using a regular username/password token. If that isn't found, looks
 * for username/password fields in the POST parameters, like the {@link FormAuthenticationFilter} does.<p>
 * 
 * This filter can be used for securing REST requests, in conjunction with the {@link SecurityResource}'s
 * {@link SecurityResource#accessToken(String, String)} through which a bearer token can be obtained using
 * basic authentication.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class BearerTokenOrBasicOrFormAuthenticationFilter extends BasicHttpAuthenticationFilter {
    private static final String BEARER = "Bearer";
    
    /**
     * Used to delegate requests to in case no bearer token and no basic authentication is found
     * in the "Authorization:" header field.
     */
    private final FormAuthenticationFilterWithPublicCreateToken formAuthenticationFilter;
    
    public BearerTokenOrBasicOrFormAuthenticationFilter() {
        this(/* application name */ "SAP");
    }
    
    protected BearerTokenOrBasicOrFormAuthenticationFilter(String applicationName) {
        formAuthenticationFilter = new FormAuthenticationFilterWithPublicCreateToken();
        setApplicationName(applicationName);
    }
    
    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        String authorizationHeader = getAuthzHeader(request);
        if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
            String[] authTokens = authorizationHeader.split(" ");
            if (authTokens[0].equalsIgnoreCase(BEARER)) {
                return authTokens.length < 2 ? null : new BearerAuthenticationToken(authTokens[1],
                        request instanceof HttpServletRequest ? HttpRequestUtils.getClientIP((HttpServletRequest) request) : null,
                        request instanceof HttpServletRequest ? HttpRequestUtils.getUserAgent((HttpServletRequest) request) : null);
            } else if (authTokens[0].equalsIgnoreCase(HttpServletRequest.BASIC_AUTH)) {
                return super.createToken(request, response);
            }
        }
        return formAuthenticationFilter.createToken(request, response);
    }

    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        final String authzScheme = BEARER.toLowerCase(Locale.ENGLISH);
        final String authzHeader = getAuthzHeader(request);
        return (authzHeader != null && authzHeader.toLowerCase(Locale.ENGLISH).startsWith(authzScheme)) ||
                super.isLoginAttempt(request, response) || formAuthenticationFilter.isLoginSubmission(request, response);
    }
}
