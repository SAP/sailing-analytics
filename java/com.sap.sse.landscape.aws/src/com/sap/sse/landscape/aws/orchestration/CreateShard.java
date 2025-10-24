package com.sap.sse.landscape.aws.orchestration;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Duration;
import com.sap.sse.common.HttpRequestHeaderConstants;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.AwsAutoScalingGroup;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.common.shared.ShardTargetGroupName;
import com.sap.sse.shared.util.Wait;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ActionTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ForwardActionConfig;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RuleCondition;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupTuple;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealth;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthStateEnum;

/**
 * This class is for creating shards out of the {@code shardingKeys}. This class creates a target group, and an
 * autoscaling group and inserts rules into the {@code replicaSet}'s load balancer. If the load balancer does not have
 * enough rules left in it'S HTTPS-listener, the whole replica set gets moved to another load balancer.
 * 
 * @author I569653
 *
 * @param <ShardingKey>
 * @param <MetricsT>
 * @param <ProcessT>
 */
public class CreateShard<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
        extends ShardProcedure<ShardingKey, MetricsT, ProcessT> {
    private static int DEFAULT_INSTANCE_STARTUP_TIME = 180;
    private static final Logger logger = Logger.getLogger(ShardProcedure.class.getName());
    private final String targetGroupNamePrefix;

    public CreateShard(BuilderImpl<?, ShardingKey, MetricsT, ProcessT> builder) throws Exception {
        super(builder);
        this.targetGroupNamePrefix = builder.getTargetGroupNamePrefix();
    }
    
    public static interface Builder<
        BuilderT extends Builder<BuilderT, T, ShardingKey, MetricsT, ProcessT>,
        T extends CreateShard<ShardingKey, MetricsT, ProcessT>, 
        ShardingKey, 
        MetricsT extends ApplicationProcessMetrics, 
        ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    extends ShardProcedure.Builder<BuilderT, T, ShardingKey, MetricsT, ProcessT> {
        BuilderT setTargetGroupNamePrefix(String targetGroupNamePrefix);
    }
    
    static class BuilderImpl<BuilderT extends Builder<BuilderT, CreateShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT>,
                            ShardingKey,
                            MetricsT extends ApplicationProcessMetrics,
                            ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    extends ShardProcedure.BuilderImpl<BuilderT, CreateShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT>
    implements Builder<BuilderT, CreateShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT> {
        private String targetGroupNamePrefix = "";
        
        String getTargetGroupNamePrefix() {
            return targetGroupNamePrefix;
        }

        public BuilderT setTargetGroupNamePrefix(String targetGroupNamePrefix) {
            if (!ShardTargetGroupName.isValidTargetGroupNamePrefix(targetGroupNamePrefix)) {
                throw new IllegalArgumentException("Not a valid target group name prefix: "+targetGroupNamePrefix);
            }
            this.targetGroupNamePrefix = targetGroupNamePrefix;
            return self();
        }

        @Override
        public CreateShard<ShardingKey, MetricsT, ProcessT> build() throws Exception {
            assert shardingKeys != null;
            assert replicaSet != null;
            assert region != null;
            return new CreateShard<ShardingKey, MetricsT, ProcessT>(this);
        }
    }

    @Override
    public void run() throws Exception {
        final ShardTargetGroupName name;
        if (shardName == null) {
            throw new Exception("Shardname is null, please enter a name");
        } else {
            name = replicaSet.getNewShardName(shardName, targetGroupNamePrefix);
        }
        if (!isTargetGroupNameUnique(name.getTargetGroupName())) {
            throw new Exception(
                    "targetgroup name with this shardname is not unique. You may change the last or first two chars");
        }
        final ApplicationLoadBalancer<ShardingKey> loadBalancer = getFreeLoadBalancerAndMoveReplicaSet();
        logger.info(
                "Creating Targer group for Shard " + name + ". Inheriting from Replicaset: " + replicaSet.getName());
        final TargetGroup<ShardingKey> targetGroup = getLandscape().createTargetGroupWithoutLoadbalancer(region,
                name.getTargetGroupName(), replicaSet.getMaster().getPort(), loadBalancer.getVpcId());
        getLandscape().addTargetGroupTag(targetGroup.getTargetGroupArn(), ShardTargetGroupName.TAG_KEY, name.getName(), region);
        final AwsAutoScalingGroup autoScalingGroup = replicaSet.getAutoScalingGroup();
        logger.info("Creating Auto-Scaling Group for Shard " + shardName + ". Inheriting from Auto-Scaling Group: "
                + autoScalingGroup.getName());
        final int minAutoscalingSize = getMinShardingStartInstanceNumber(autoScalingGroup);
        final String newAutoscalingGroupName = getLandscape().createAutoScalingGroupFromExisting(autoScalingGroup, shardName, targetGroup, minAutoscalingSize, Optional.empty());
        // create one rule to path unused by any application for linking ALB to target group.
        if (loadBalancer != null) {
            final Iterable<Rule> rules = loadBalancer.getRules();
            if (Util.size(rules) < ApplicationLoadBalancer.MAX_RULES_PER_LOADBALANCER
                    - numberOfRequiredRules(Util.size(shardingKeys))) {
                final int rulePrio = getHighestAvailableIndex(rules);
                if (rulePrio > 0) {
                    Rule newRule = Rule.builder().priority("" + rulePrio)
                            .conditions(
                                    loadBalancer.createHostHeaderRuleCondition(replicaSet.getHostname()),
                                    RuleCondition.builder().field("http-header")
                                            .httpHeaderConfig(hhcb -> hhcb
                                                    .httpHeaderName(HttpRequestHeaderConstants.HEADER_KEY_FORWARD_TO)
                                                    .values(HttpRequestHeaderConstants.HEADER_FORWARD_TO_REPLICA
                                                            .getB()))
                                            .build(),
                                    RuleCondition.builder().field("path-pattern")
                                            .pathPatternConfig(ppc -> ppc.values(getPathConditionForShardingKey(SHARDING_KEY_UNUSED_BY_ANY_APPLICATION))).build())
                            .actions(Action.builder()
                                    .forwardConfig(ForwardActionConfig.builder()
                                            .targetGroups(TargetGroupTuple.builder()
                                                    .targetGroupArn(targetGroup.getTargetGroupArn()).build())
                                            .build())
                                    .type(ActionTypeEnum.FORWARD).build())
                            .build();
                    final Iterable<Rule> newRuleSet = loadBalancer.addRules(newRule);
                    getLandscape().putScalingPolicy(DEFAULT_INSTANCE_STARTUP_TIME, getLandscape().getAutoScalingGroupName(shardName), targetGroup,
                            AwsAutoScalingGroup.DEFAULT_MAX_REQUESTS_PER_TARGET, region);
                    // wait until instances are running
                    Wait.wait(()->{
                        boolean ret = true;
                        final Map<AwsInstance<ShardingKey>, TargetHealth> healths = getLandscape()
                                .getTargetHealthDescriptions(targetGroup);
                        if (healths.isEmpty() || healths.size() < minAutoscalingSize) {
                            ret = false; // if there are not enough instances in the target group yet
                        } else {
                            for (Map.Entry<AwsInstance<ShardingKey>, TargetHealth> instance : healths.entrySet()) {
                                if (instance.getValue().state() != TargetHealthStateEnum.HEALTHY) {
                                    ret = false; // if an instance is unhealthy
                                    break;
                                }
                            }
                        }
                        return ret;
                    }, Landscape.WAIT_FOR_HOST_TIMEOUT, Duration.ONE_SECOND.times(30), Level.INFO, "Instances not yet healty");
                    // remove dummy-rule
                    for (Rule r : newRuleSet) {
                        loadBalancer.deleteRules(r);
                    }
                    final Set<ShardingKey> shardingKeysToUse;
                    if (shardingKeys.isEmpty()) {
                        shardingKeysToUse = Collections.singleton(SHARDING_KEY_UNUSED_BY_ANY_APPLICATION);
                    } else {
                        shardingKeysToUse = shardingKeys;
                    }
                    // change ALB rules to new ones
                    addShardingRules(loadBalancer, shardingKeysToUse, targetGroup);
                    // restore default minCapacity of shard's autoscalinggroup
                    getLandscape().resetShardMinAutoscalingGroupSize(newAutoscalingGroupName, region);

                } else {
                    throw new Exception("Unexpected Error - No prio left?");
                }
            } else {
                throw new Exception("Unexpected Error - Loadbalancer was null!");
            }
        }
    }

    protected int getMinShardingStartInstanceNumber(AwsAutoScalingGroup autoscalingGroupParent) {
        final int currentMinSize = autoscalingGroupParent.getAutoScalingGroup().instances().size();
        return (currentMinSize < ShardProcedure.DEFAULT_MINIMUM_AUTO_SCALING_GROUP_SIZE)
                ? ShardProcedure.DEFAULT_MINIMUM_AUTO_SCALING_GROUP_SIZE
                : currentMinSize; // ensure that the minsize is at least 2
    }

    public static <MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, BuilderT extends Builder<BuilderT, CreateShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT>, ShardingKey> Builder<BuilderT, CreateShard<ShardingKey, MetricsT, ProcessT>, ShardingKey, MetricsT, ProcessT> builder() {
        return new BuilderImpl<BuilderT, ShardingKey, MetricsT, ProcessT>();
    }
}