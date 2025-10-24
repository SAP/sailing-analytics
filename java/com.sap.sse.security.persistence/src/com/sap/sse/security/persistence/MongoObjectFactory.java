package com.sap.sse.security.persistence;

import org.apache.shiro.session.Session;

public interface MongoObjectFactory {

    void storeSession(String cacheName, Session session);

    void removeSession(String cacheName, Session session);

    void removeAllSessions(String cacheName);
    
    void storeCORSFilterConfigurationIsWildcard(String serverName);
    
    /**
     * Makes the CORS filter configuration for the application replica set identified by {@code serverName}
     * a non-wildcard configuration that accepts REST requests only from those origins listed in
     * {@code allowedOrigins}.
     * 
     * @param allowedOrigins {@code null} is allowed and handled like an empty array
     */
    void storeCORSFilterConfigurationAllowedOrigins(String serverName, String... allowedOrigins);
}
