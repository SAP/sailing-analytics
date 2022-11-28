package com.sap.sse.landscape.application;

import com.sap.sse.common.Named;

/**
 * Part of a {@link Scope}. A {@link Shard} cannot be moved in isolation and can hence not move across {@link Scope}s.
 * Sharding can be used to optionally split {@link ApplicationProcess}es in groups, each of which being responsible
 * primarily only for a subset of the {@link Shard}s available in the {@link ApplicationReplicaSet}. While we assume
 * that all {@link ApplicationProcess}es in an {@link ApplicationReplicaSet} <em>can</em> handle requests for all
 * {@link Shard}s managed by the replica set, it may be beneficial performance-wise to have individual processes focus
 * only on a subset of {@link Shard}s. This may, e.g., result in better cache utilization and hence less CPU consumption
 * on the hosts running those processes.
 * <p>
 * 
 * There may be restrictions for the {@link Named#getName() name} that a shard can have, for example it could be
 * possible that shard names with quotes or brackets in them are not permitted; the name is expected to be human-readable
 * and meaningful.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface Shard<ShardingKey> extends Named {
    /**
     * @return the keys handled by this shard
     * 
     */
    Iterable<ShardingKey> getKeys();
    
    
    
}
