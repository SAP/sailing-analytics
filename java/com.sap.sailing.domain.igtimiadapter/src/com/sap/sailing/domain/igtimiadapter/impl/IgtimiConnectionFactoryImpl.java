package com.sap.sailing.domain.igtimiadapter.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;

public class IgtimiConnectionFactoryImpl implements IgtimiConnectionFactory {
    private final URL baseUrl;
    
    /**
     * Used when {@code null} is passed to {@link #getOrCreateConnection(String)} as a bearer token. If this field is
     * {@code null}, too, requests to the REST API will be made through the connections returned from
     * {@link #getOrCreateConnection(String)} without an authenticated user.
     */
    private final String defaultBearerToken;
    
    /**
     * Remembers connections created by this factory, through weak references, so when no client uses the
     * connection anymore it can be garbage-collected. Keys are the bearer tokens used when creating the
     * connection. The {@code null} key is escaped using the empty string.
     */
    private final ConcurrentMap<String, IgtimiConnection> connectionPool;

    /**
     * @param baseUrl
     *            base URL of the service where Igtimi wind data can be requested from; example:
     *            {@code https://wind.sapsailing.com}; trailing slashes will be removed implicitly here.
     * @param defaultBearerToken
     *            Used when {@code null} is passed to {@link #getOrCreateConnection(String)} as a bearer token. If this field
     *            is {@code null}, too, requests to the REST API will be made through the connections returned from
     *            {@link #getOrCreateConnection(String)} without an authenticated user.
     */
    public IgtimiConnectionFactoryImpl(URL baseUrl, String defaultBearerToken) throws MalformedURLException {
        this.baseUrl = (baseUrl.toString().endsWith("/") ? new URL(baseUrl.toString().substring(0, baseUrl.toString().length()-1)) : baseUrl);
        this.defaultBearerToken = defaultBearerToken;
        this.connectionPool = new ConcurrentHashMap<>();
    }

    @Override
    public URL getBaseUrl() {
        return baseUrl;
    }

    @Override
    public boolean hasCredentials() {
        return defaultBearerToken != null;
    }
    
    @Override
    public synchronized IgtimiConnection getOrCreateConnection(String bearerToken) {
        final String poolKey = bearerToken == null ? "" : bearerToken;
        final IgtimiConnection pooledConnection = connectionPool.get(poolKey);
        final IgtimiConnection result;
        if (pooledConnection == null) {
            result = new IgtimiConnectionImpl(baseUrl, bearerToken);
            connectionPool.put(poolKey, result);
        } else {
            result = pooledConnection;
        }
        return result;
    }
    
    @Override
    public IgtimiConnection getOrCreateConnection() {
        return getOrCreateConnection(defaultBearerToken);
    }
}
