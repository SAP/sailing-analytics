package com.sap.sse.gwt.dispatch.shared.caching;

/**
 * Cache interface used by the caching dispatch to identify which command can be cached on client side. Each command
 * must provide an instance key used to store the results in the cache. The instance key is appended to an class
 * identifying key.
 */
public interface IsClientCacheable {

    /**
     * The builder used to identify instance specific results in the cache.
     */
    void cacheInstanceKey(StringBuilder key);
}
