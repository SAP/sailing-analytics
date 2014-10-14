package com.sap.sse.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.web.filter.authc.PassThruAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;

/**
 * Forwards a set of query parameters that are useful for the login entry point, in particular <code>gwt.codesvr</code> which
 * is important for GWT hosted mode debugging, and <code>locale</code> for i18n support.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class CustomFilter extends PassThruAuthenticationFilter {
    private final static Iterable<String> queryParametersToForwardToLogin = Arrays.asList("gwt.codesvr", "locale");
    
    @Override
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
        String loginUrl = getLoginUrl();
        Map<String, String> queryParams = new HashMap<>();
        for (String queryParameterToForwardToLogin : queryParametersToForwardToLogin) {
            String value = request.getParameterMap().get(queryParameterToForwardToLogin) == null ? null : request.getParameterMap().get(queryParameterToForwardToLogin)[0];
            if (value != null) {
                queryParams.put(queryParameterToForwardToLogin, value);
            }
        }
        WebUtils.issueRedirect(request, response, loginUrl, queryParams, /* contextRelative */ false);
    }
}
