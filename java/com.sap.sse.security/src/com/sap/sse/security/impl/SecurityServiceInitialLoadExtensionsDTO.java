package com.sap.sse.security.impl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.sap.sse.common.TimedLock;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.Replicable;
import com.sap.sse.security.SecurityService;

/**
 * Starting with the CORS filter configurations, this and future extensions of the {@link SecurityService}'s
 * initial load format can be added to this DTO type. The benefit, compared to adding new content with
 * explicit {@link ObjectOutputStream#writeObject(Object)} and {@link ObjectInputStream#readObject()} calls,
 * is that there is a certain built-in backward compatibility when a replica with a new version and additional
 * fields expected in the initial load tries to replicate from an older version where those fields don't exist
 * yet in this type. Then, those additional fields will come out as {@code null} on the replica, and the replica
 * at least has a chance to continue with a useful initialization of those data structures instead of having
 * the replication of {@link SecurityService} and all subsequent {@link Replicable}s fail due to incompatible
 * formats of object output and input streams.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SecurityServiceInitialLoadExtensionsDTO implements Serializable {
    private static final long serialVersionUID = -6033028595976152484L;

    private final ConcurrentMap<String, Pair<Boolean, Set<String>>> corsFilterConfigurationsByReplicaSetName;
    
    private final ConcurrentMap<String, TimedLock> clientIPBasedTimedLocksForBearerTokenAuthentication;
    
    private final ConcurrentMap<String, TimedLock> clientIPBasedTimedLocksForUserCreation;
    
    public SecurityServiceInitialLoadExtensionsDTO(
            ConcurrentMap<String, Pair<Boolean, Set<String>>> corsFilterConfigurationsByReplicaSetName,
            ConcurrentMap<String, TimedLock> clientIPBasedTimedLockForBearerTokenAuthentication,
            ConcurrentMap<String, TimedLock> clientIPBasedTimedLocksForUserCreation) {
        super();
        this.corsFilterConfigurationsByReplicaSetName = corsFilterConfigurationsByReplicaSetName;
        this.clientIPBasedTimedLocksForBearerTokenAuthentication = clientIPBasedTimedLockForBearerTokenAuthentication;
        this.clientIPBasedTimedLocksForUserCreation = clientIPBasedTimedLocksForUserCreation;
    }

    
    ConcurrentMap<String, Pair<Boolean, Set<String>>> getCorsFilterConfigurationsByReplicaSetName() {
        return corsFilterConfigurationsByReplicaSetName;
    }

    ConcurrentMap<String, TimedLock> getClientIPBasedTimedLocksForBearerTokenAuthentication() {
        return clientIPBasedTimedLocksForBearerTokenAuthentication;
    }

    ConcurrentMap<String, TimedLock> getClientIPBasedTimedLocksForUserCreation() {
        return clientIPBasedTimedLocksForUserCreation;
    }
}
