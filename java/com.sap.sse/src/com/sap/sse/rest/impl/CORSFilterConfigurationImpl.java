package com.sap.sse.rest.impl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.sse.common.http.HttpHeaderUtil;
import com.sap.sse.rest.CORSFilterConfiguration;

public class CORSFilterConfigurationImpl implements CORSFilterConfiguration {
    private boolean wildcard;
    
    /**
     * A concurrent hash set, backed by a {@link ConcurrentHashMap}. All contained strings must be all lowercase.
     */
    private final Set<String> allowedOrigins;
    
    public CORSFilterConfigurationImpl() {
        this.allowedOrigins = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    @Override
    public boolean isWildcard() {
        return wildcard;
    }

    @Override
    public void setWildcard() {
        wildcard = true;
    }

    @Override
    public boolean contains(String origin) {
        return origin != null && allowedOrigins.contains(origin.toLowerCase());
    }

    @Override
    public void setOrigins(Iterable<String> allowedOrigins) throws IllegalArgumentException {
        for (final String allowedOrigin : allowedOrigins) {
            if (!HttpHeaderUtil.isValidOriginHeaderValue(allowedOrigin)) {
                throw new IllegalArgumentException("\""+allowedOrigin+"\" is not a valid CORS origin");
            }
        }
        this.wildcard = false;
        this.allowedOrigins.clear();
        for (final String allowedOrigin : allowedOrigins) {
            this.allowedOrigins.add(allowedOrigin.toLowerCase());
        }
    }

    @Override
    public Iterable<String> getOrigins() {
        return Collections.unmodifiableSet(allowedOrigins);
    }

    @Override
    public String toString() {
        return "Access-Control-Allow-Origin: "+(isWildcard()?"*":allowedOrigins);
    }
}
