package com.sap.sse.landscape.aws.orchestration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.AwsShard;
import com.sap.sse.landscape.aws.TargetGroup;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RuleCondition;

/**
 * This procedure appends {@code shardingKeys} to a shard, identified by {@link #shardName} from
 * the {@link #replicaSet}. This is done by adding rule conditions to the shard's rule set and after that just appending new
 * rules. This could lead to moving to replica set to another load balancer, because it may get full with new sharding
 * rules.
 * 
 * @author I569653
 *
 * @param <ShardingKey>
 * @param <MetricsT>
 * @param <ProcessT>
 */
public class AddShardingKeyToShard<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
        extends ShardProcedure<ShardingKey, MetricsT, ProcessT> {
    private static final Logger logger = Logger.getLogger(AddShardingKeyToShard.class.getName());

    public AddShardingKeyToShard(BuilderImpl<?, ShardingKey, MetricsT, ProcessT> builder) throws Exception {
        super(builder);
    }

    static class BuilderImpl<BuilderT extends Builder<BuilderT, AddShardingKeyToShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
            extends
            ShardProcedure.BuilderImpl<BuilderT, AddShardingKeyToShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT> {

        @Override
        public AddShardingKeyToShard<ShardingKey, MetricsT, ProcessT> build() throws Exception {
            assert shardingKeys != null;
            assert replicaSet != null;
            assert region != null;
            return new AddShardingKeyToShard<ShardingKey, MetricsT, ProcessT>(this);
        }
    }

    @Override
    public void run() throws Exception {
        AwsShard<ShardingKey> shard = null;
        for (final Entry<AwsShard<ShardingKey>, Iterable<ShardingKey>> entry : replicaSet.getShards().entrySet()) {
            if (entry.getKey().getName().equals(shardName)) {
                shard = entry.getKey();
                break;
            }
        }
        if (shard == null) {
            throw new Exception("Shard "+shardName+" not found in replica set "+replicaSet.getName());
        }
        logger.info("Appending " + Util.joinStrings(", ", shardingKeys) + " to " + shardName);
        // list for manipulation -> elements are allowed to be removed!!
        final List<ShardingKey> mutableShardingKeys = new LinkedList<>();
        mutableShardingKeys.addAll(shardingKeys);
        final TargetGroup<ShardingKey> targetgroup = shard.getTargetGroup();
        final ApplicationLoadBalancer<ShardingKey> loadBalancer = shard.getLoadBalancer();
        final Collection<TargetGroup<ShardingKey>> t = new ArrayList<>();
        t.add(targetgroup);
        // check if there is a rule with space left for one or more additional conditions:
        for (Rule r : shard.getRules()) {
            boolean updateRule = false;
            final ArrayList<ShardingKey> shardingKeys = new ArrayList<>();
            for (RuleCondition con : r.conditions()) {
                // if we find a 
                if (con.pathPatternConfig() != null) {
                    // eliminate PATH_UNUSED_BY_ANY_APPLICATION in case this proxy key was found;
                    // it usually indicates an empty shard; when now adding one or more conditions
                    // it can be replaced.
                    Util.addAll(
                            Util.filter(
                                    Util.map(con.values(), this::getShardingKeyFromPathCondition),
                                            shardingKey->!shardingKey.equals(SHARDING_KEY_UNUSED_BY_ANY_APPLICATION)),
                            shardingKeys);
                }
            }
            if (shardingKeys.isEmpty()) {
                // the rule probably only has PATH_UNUSED_BY_ANY_APPLICATION and was a proxy rule, probably at the end of the list; remove
                loadBalancer.deleteRules(r);
            } else { // update only non-empty rule because we assume it won't be at the end of the list
                while (shardingKeys.size() < ApplicationLoadBalancer.MAX_CONDITIONS_PER_RULE - NUMBER_OF_STANDARD_CONDITIONS_FOR_SHARDING_RULE
                        && !mutableShardingKeys.isEmpty()) {
                    shardingKeys.add(mutableShardingKeys.get(0));
                    mutableShardingKeys.remove(0);
                    updateRule = true;
                }
                final Collection<RuleCondition> ruleConditions = getShardingRuleConditions(loadBalancer, shardingKeys);
                // construct a rule only for transporting the conditions; no forwarding target is required for modifyRuleConditions
                Rule proxyRuleWithNewConditions = Rule.builder().ruleArn(r.ruleArn()).conditions(ruleConditions).build();
                if (updateRule) {
                    getLandscape().modifyRuleConditions(region, proxyRuleWithNewConditions);
                }
            }
        }
        if (!mutableShardingKeys.isEmpty()) {
            // check number of rules
            final Set<ShardingKey> keysCopy = new HashSet<>();
            keysCopy.addAll(shardingKeys);
            final ApplicationLoadBalancer<ShardingKey> alb;
            if (Util.size(loadBalancer.getRules()) + numberOfRequiredRules(Util.size(shardingKeys))
                    < ApplicationLoadBalancer.MAX_RULES_PER_LOADBALANCER) {
                // enough rules can be added to existing loadBalancer
                alb = loadBalancer;
            } else {
                // not enough rules; move replica set to a free / new load balancer
                alb = getFreeLoadBalancerAndMoveReplicaSet();
            }
            // set new rules
            addShardingRules(alb, keysCopy, targetgroup);
        }
    }

    public static <MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, BuilderT extends Builder<BuilderT, AddShardingKeyToShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT>, ShardingKey> Builder<BuilderT, AddShardingKeyToShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT> builder() {
        return new BuilderImpl<BuilderT, ShardingKey, MetricsT, ProcessT>();
    }
}