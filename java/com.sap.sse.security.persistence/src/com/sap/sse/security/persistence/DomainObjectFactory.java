package com.sap.sse.security.persistence;

import java.util.Map;
import java.util.Set;

import org.apache.shiro.session.Session;

import com.sap.sse.common.Util.Pair;

public interface DomainObjectFactory {
    Map<String, Set<Session>> loadSessionsByCacheName();
    
    /**
     * Loads the CORS filter configurations; as we cannot reference the {@code CORSFilterConfiguration} interface from
     * here without introducing a cyclic dependency, we return the configuration data in the form of a {@link Pair}
     * whose {@link Pair#getA() first} component is a boolean telling whether the filter uses the "wildcard" (*) to
     * allow REST requests from all possible origins, and the {@link Pair#getB() second} component lists the allowed
     * origins in case it's not a wildcard configuration. For wildcard configurations, the second component is ignored.
     */
    Map<String, Pair<Boolean, Set<String>>> loadCORSFilterConfigurationsForReplicaSetNames();
}
