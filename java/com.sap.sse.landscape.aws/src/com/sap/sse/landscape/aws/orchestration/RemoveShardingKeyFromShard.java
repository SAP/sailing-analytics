package com.sap.sse.landscape.aws.orchestration;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.AwsShard;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RuleCondition;

public class RemoveShardingKeyFromShard<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
        extends ShardProcedure<ShardingKey, MetricsT, ProcessT> {
    public RemoveShardingKeyFromShard(BuilderImpl<?, ShardingKey, MetricsT, ProcessT> builder) throws Exception {
        super(builder);
    }

    static class BuilderImpl<BuilderT extends Builder<BuilderT, RemoveShardingKeyFromShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
            extends
            ShardProcedure.BuilderImpl<BuilderT, RemoveShardingKeyFromShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT> {

        @Override
        public RemoveShardingKeyFromShard<ShardingKey, MetricsT, ProcessT> build() throws Exception {
            assert shardingKeys != null;
            assert replicaSet != null;
            assert region != null;
            assert passphraseForPrivateKeyDecryption != null;
            return new RemoveShardingKeyFromShard<ShardingKey, MetricsT, ProcessT>(this);
        }
    }

    @Override
    public void run() throws Exception {
        AwsShard<ShardingKey> shard = null;
        for (Entry<AwsShard<ShardingKey>, Iterable<ShardingKey>> entry : replicaSet.getShards().entrySet()) {
            if (entry.getKey().getName().equals(shardName)) {
                shard = entry.getKey();
                break;
            }
        }
        if (shard == null) {
            throw new Exception("Shard not found!");
        }
        // remove conditions in rules where path is the sharding key
        Set<String> shardingKeysFromConditions = new HashSet<>();
        for (Rule r : shard.getRules()) {
            for (RuleCondition condition : r.conditions()) {
                if (condition.pathPatternConfig() != null) {
                    shardingKeysFromConditions.addAll(condition.values());
                }
            }
        }
        getLandscape().deleteLoadBalancerListenerRules(region, Util.toArray(shard.getRules(), new Rule[0]));
        shardingKeysFromConditions = Util.asSet(Util.filter(shardingKeysFromConditions, t -> {
            for (String s : shardingKeys) {
                if (s.equals(t)) {
                    return false;
                }

            }
            return true;
        }));
        // change ALB rules to new ones
        addShardingRules(shard.getLoadbalancer(), Util.asSet(shardingKeysFromConditions), shard.getTargetGroup());
    }

    public static <MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, 
        BuilderT extends Builder<BuilderT, RemoveShardingKeyFromShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT>, 
        ShardingKey> 
        Builder<BuilderT, RemoveShardingKeyFromShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT> builder() {
        return new BuilderImpl<BuilderT, ShardingKey, MetricsT, ProcessT>();
    }
}