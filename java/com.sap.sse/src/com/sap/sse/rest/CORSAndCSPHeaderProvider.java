package com.sap.sse.rest;

import java.util.HashMap;
import java.util.Map;

import com.sap.sse.common.Util;
import com.sap.sse.impl.Activator;

/**
 * Servlet or Jersey response filters that implement this interface can use the {@link #getCORSAndCSPHeaders(String)} method
 * to obtain a map with CORS (Cross-Origin Resource Sharing) and CSP (Content Security Policy) HTTP response headers
 * based on the {@link Activator#getCORSFilterConfiguration() active filter configuration}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface CORSAndCSPHeaderProvider {
    /**
     * When the {@link CORSFilterConfiguration} is a {@link CORSFilterConfiguration#isWildcard() wildcard}, the
     * {@code Access-Control-Allow-Origin} header is set to "*". Otherwise, if the filter configuration
     * {@link CORSFilterConfiguration#contains(String) matches} {@code origin}, then the {@code origin} parameter's
     * value is used as the header value for {@code Access-Control-Allow-Origin}. In all other cases, that header field
     * remains unset. The {@code Content-Security-Policy} header is set if and only if the
     * {@code Access-Control-Allow-Origin} header is set; its value is
     */
    default Map<String, String> getCORSAndCSPHeaders(String origin) {
        final Map<String, String> responseCORSHeaders = new HashMap<>();
        final CORSFilterConfiguration corsFilterConfiguration = Activator.getInstance().getCORSFilterConfiguration();
        final String allowOrigin;
        if (corsFilterConfiguration.isWildcard()) {
            allowOrigin = "*";
        } else {
            final Iterable<String> origins = corsFilterConfiguration.getOrigins();
            responseCORSHeaders.put("Content-Security-Policy", "frame-ancestors 'self' "+
                    Util.joinStrings(" ", origins)+";");
            if (corsFilterConfiguration.contains(origin)) {
                allowOrigin = origin;
            } else {
                allowOrigin = null;
            }
        }
        if (allowOrigin != null) {
            responseCORSHeaders.put("Access-Control-Allow-Origin", allowOrigin);
            responseCORSHeaders.put("Vary", "Origin"); // don't cache this; it may vary depending on origin
        }
        responseCORSHeaders.put("Access-Control-Allow-Credentials", "true");
        responseCORSHeaders.put("Access-Control-Allow-Headers", "Authorization, Origin, X-Requested-With, Content-Type");
        responseCORSHeaders.put("Access-Control-Expose-Headers", "Location, Content-Disposition");
        responseCORSHeaders.put("Access-Control-Allow-Methods", "POST, PUT, GET, DELETE, HEAD, OPTIONS");
        return responseCORSHeaders;
    }
}
