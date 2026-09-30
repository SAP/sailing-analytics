package com.sap.sse.landscape.aws.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.Duration;
import com.sap.sse.common.HttpRequestHeaderConstants;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.DefaultProcessConfigurationVariables;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.ProcessFactory;
import com.sap.sse.landscape.application.impl.ApplicationProcessImpl;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.AwsApplicationProcess;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.MongoUriParser;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.replication.ReplicationStatus;
import com.sap.sse.util.IPAddressUtil;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.ActionTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthStateEnum;

public abstract class AwsApplicationProcessImpl<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
extends ApplicationProcessImpl<ShardingKey, MetricsT, ProcessT>
implements AwsApplicationProcess<ShardingKey, MetricsT, ProcessT> {
    private final AwsLandscape<ShardingKey> landscape;
    private Database databaseConfiguration;
    
    public AwsApplicationProcessImpl(int port, Host host, String serverDirectory, AwsLandscape<ShardingKey> landscape) {
        super(port, host, serverDirectory);
        this.landscape = landscape;
    }

    public AwsApplicationProcessImpl(int port, Host host, String serverDirectory, Integer telnetPort,
            String serverName, AwsLandscape<ShardingKey> landscape) {
        super(port, host, serverDirectory, telnetPort, serverName);
        this.landscape = landscape;
    }
    
    protected AwsLandscape<ShardingKey> getLandscape() {
        return landscape;
    }
    
    @Override
    public AwsInstance<ShardingKey> getHost() {
        @SuppressWarnings("unchecked")
        final AwsInstance<ShardingKey> host = (AwsInstance<ShardingKey>) super.getHost();
        return host;
    }

    @Override
    public Database getDatabaseConfiguration(Region region, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        if (databaseConfiguration == null) {
            databaseConfiguration = new MongoUriParser<ShardingKey>(landscape, region).parseMongoUri(getEnvShValueFor(DefaultProcessConfigurationVariables.MONGODB_URI, optionalTimeout,
                optionalKeyName, privateKeyEncryptionPassphrase));
        }
        return databaseConfiguration;
    }

    @Override
    public <HostT extends AwsInstance<ShardingKey>> ProcessT getMaster(Optional<Duration> optionalTimeout, HostSupplier<ShardingKey, HostT> hostSupplier,
            ProcessFactory<ShardingKey, MetricsT, ProcessT, HostT> processFactory) throws Exception {
        final JSONObject replicationStatus = getReplicationStatus(optionalTimeout);
        final JSONArray replicables = (JSONArray) replicationStatus.get(ReplicationStatus.JSON_FIELD_NAME_REPLICABLES);
        for (final Object replicableObject : replicables) {
            final JSONObject replicable = (JSONObject) replicableObject;
            final JSONObject replicatedFrom = (JSONObject) replicable.get(ReplicationStatus.JSON_FIELD_NAME_REPLICABLE_REPLICATEDFROM);
            if (replicatedFrom != null) {
                final String masterAddress = (String) replicatedFrom.get(ReplicationStatus.JSON_FIELD_NAME_HOSTNAME);
                final Integer port = replicatedFrom.get(ReplicationStatus.JSON_FIELD_NAME_PORT) == null ? null : ((Number) replicatedFrom.getOrDefault(ReplicationStatus.JSON_FIELD_NAME_PORT, 8888)).intValue();
                Pair<HostT, Integer> hostAndOptionalTargetPort = getHostAndOptionalTargetPortFromIpAddress(hostSupplier, masterAddress);
                if (hostAndOptionalTargetPort != null) {
                    return processFactory.createProcess(hostAndOptionalTargetPort.getA(),
                            // if a separate target port was returned by the lookup via IP address, use it because then
                            // the "port" we have here is only targeting a load balancer, not the host itself:
                            hostAndOptionalTargetPort.getB() != null ? hostAndOptionalTargetPort.getB() : port,
                            /* serverDirectory to be discovered otherwise */ null,
                            /* telnetPort can be obtained from environment on demand */ null,
                            /* serverName to be discovered otherwise */ null, Collections.emptyMap());
                }
            }
        }
        return null;
    }

    /**
     * If {@code ipAddressOrHostname} refers to a DNS entry instead of an IP address, a lookup in our own DNS registry
     * is performed, checking if the hostname matches a CNAME record that in turn points to a load balancer. See
     * {@link AwsLandscape#getDNSMappedLoadBalancerFor(Region, String)}. If so, the load balancer rules are scanned for
     * those having a hostname filter matching the hostname provided in {@code ipAddressOrHostname} and checks to which
     * "-m" master target group the rule refers. The corresponding target group is then looked up, and the first target
     * ready will be returned, or {@code null} if the target group does not contain any target that is ready.
     * <p>
     * 
     * In case {@code ipAddressOrHostname} represents an IP address this method assumes to find the
     * {@code ipAddressOrHostname} in this host's {@link Host#getRegion() region}. This method then looks for instances
     * that have {@code ipAddressOrHostname} as their public or private IP address. If found, the respective host is
     * returned, otherwise {@code null} is returned.
     * <p>
     */
    private <HostT extends AwsInstance<ShardingKey>> Pair<HostT, Integer> getHostAndOptionalTargetPortFromIpAddress(HostSupplier<ShardingKey, HostT> hostSupplier, final String ipAddressOrHostname) {
        HostT host;
        Integer targetPort;
        final ApplicationLoadBalancer<ShardingKey> alb = IPAddressUtil.isIPAddressLiteral(ipAddressOrHostname) ? null : landscape.getDNSMappedLoadBalancerFor(ipAddressOrHostname);
        if (alb != null) {
            logger.info("Found a hostname mapped to a load balancer; trying to find master through target group...");
            host = null;
            targetPort = null;
            for (final Rule rule : alb.getRules()) {
                if (rule.conditions().stream().filter(
                        r->r.hostHeaderConfig() != null
                        && r.hostHeaderConfig().values().contains(ipAddressOrHostname)).findAny().isPresent()
                && rule.conditions().stream().filter(
                        r->r.httpHeaderConfig() != null
                        && r.httpHeaderConfig().httpHeaderName().equals(HttpRequestHeaderConstants.HEADER_KEY_FORWARD_TO)
                        && r.httpHeaderConfig().values().contains(HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER.getB())).findAny().isPresent()) {
                    logger.info("Found rule matching hostname "+ipAddressOrHostname+" for master");
                    Optional<String> targetGroupArn = rule.actions().stream().filter(
                                                            a -> a.type() == ActionTypeEnum.FORWARD).map(
                                                                    act -> act.forwardConfig().targetGroups().stream().findAny().map(
                                                                            tgt -> tgt.targetGroupArn()).orElse(null)).findAny();
                    final Optional<TargetGroup<ShardingKey>> targetGroup = targetGroupArn
                            .map(tgArn->landscape.getAwsTargetGroupByArn(alb.getRegion(), tgArn))
                            .map(awsTg->landscape.getTargetGroup(alb.getRegion(), awsTg.targetGroupName()));
                    host = targetGroup
                            .map(tg->tg.getRegisteredTargets())
                            .map(tah->tah.keySet().stream().filter(t->tah.get(t).state() == TargetHealthStateEnum.HEALTHY).findAny().orElse(null))
                            .map(awsInstance->hostSupplier.supply(awsInstance.getInstanceId(), awsInstance.getAvailabilityZone(), awsInstance.getPrivateAddress(), awsInstance.getLaunchTimePoint(), landscape))
                            .orElse(null);
                    targetPort = targetGroup.map(TargetGroup::getPort).orElse(null);
                    if (host != null) {
                        logger.info("Identified master in target group for hostname "+ipAddressOrHostname+": "+host+":"+targetPort);
                        break;
                    }
                }
            }
        } else {
            targetPort = null;
            try {
                host = landscape.getHostByPublicIpAddress(getHost().getRegion(), ipAddressOrHostname, hostSupplier);
            } catch (Exception e) {
                logger.info("Unable to find master by public IP "+ipAddressOrHostname+" ("+e.getMessage()+"); trying to look up master assuming "+ipAddressOrHostname+" is the private IP");
                try {
                    host = landscape.getHostByPrivateDnsNameOrIpAddress(getHost().getRegion(), ipAddressOrHostname, hostSupplier);
                } catch (Exception f) {
                    logger.info("Unable to find master by private IP "+ipAddressOrHostname+" ("+f.getMessage()+") either. Returning null.");
                    host = null;
                }
            }
        }
        return host == null ? null : new Pair<>(host, targetPort);
    }

    @Override
    public <HostT extends AwsInstance<ShardingKey>> Iterable<ProcessT> getReplicas(Optional<Duration> optionalTimeout, HostSupplier<ShardingKey, HostT> hostSupplier,
            ProcessFactory<ShardingKey, MetricsT, ProcessT, HostT> processFactory) throws TimeoutException, Exception {
        final Set<ProcessT> result = new HashSet<>();
        final JSONObject replicationStatus = getReplicationStatus(optionalTimeout);
        final JSONArray replicables = (JSONArray) replicationStatus.get(ReplicationStatus.JSON_FIELD_NAME_REPLICABLES);
        for (final Object replicableObject : replicables) {
            final JSONObject replicable = (JSONObject) replicableObject;
            final JSONArray replicatedBy = (JSONArray) replicable.get(ReplicationStatus.JSON_FIELD_NAME_REPLICABLE_REPLICATEDBY);
            if (replicatedBy != null) {
                for (final Object replicaObject : replicatedBy) {
                    final JSONObject replica = (JSONObject) replicaObject;
                    final String replicaAddress = (String) replica.get(ReplicationStatus.JSON_FIELD_NAME_ADDRESS);
                    final Integer port = replica.get(ReplicationStatus.JSON_FIELD_NAME_PORT) == null ? null : ((Number) replica.getOrDefault(ReplicationStatus.JSON_FIELD_NAME_PORT, 8888)).intValue();
                    Pair<HostT, Integer> hostAndOptionalTargetPort = getHostAndOptionalTargetPortFromIpAddress(hostSupplier, replicaAddress);
                    if (hostAndOptionalTargetPort != null) {
                        // adding to the set relies on ApplicationProcess deciding equality based on host and port
                        result.add(processFactory.createProcess(hostAndOptionalTargetPort.getA(),
                                // if a separate target port was returned by the lookup via IP address, use it because then
                                // the "port" we have here is only targeting a load balancer, not the host itself:
                                hostAndOptionalTargetPort.getB() != null ? hostAndOptionalTargetPort.getB() : port,
                                /* serverDirectory to be discovered otherwise */ null,
                                /* telnetPort can be obtained from environment on demand */ null,
                                /* serverName to be discovered otherwise */ null, Collections.emptyMap()));
                    }
                }
            }
        }
        return result;
    }
}
