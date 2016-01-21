package com.sap.sse.gwt.dispatch.client.caching;

/**
 * Cache interface used by the caching dispatch to identify which command can be cached on client side.
 * 
 * @author pgtaboada
 *
 */
public interface IsClientCacheable {

    /**
     * The instance specific key used to identify instance specific results in the cache.
     * 
     * @return
     */
    void cacheInstanceKey(StringBuilder key);
}
