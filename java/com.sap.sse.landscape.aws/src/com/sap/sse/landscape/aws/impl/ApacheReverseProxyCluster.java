package com.sap.sse.landscape.aws.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sap.sse.landscape.aws.LandscapeConstants;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.concurrent.ConsumerWithException;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.Log;
import com.sap.sse.landscape.MachineImage;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.SecurityGroup;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.Scope;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.ReverseProxyCluster;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.orchestration.StartAwsHost;
import com.sap.sse.shared.util.Wait;

import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TagDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealth;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthStateEnum;

public class ApacheReverseProxyCluster<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, LogT extends Log>
        extends AbstractApacheReverseProxy<ShardingKey, MetricsT, ProcessT>
        implements ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> {
    private static final Logger logger = Logger.getLogger(ApacheReverseProxyCluster.class.getName());
    private Set<AwsInstance<ShardingKey>> hosts;

    public ApacheReverseProxyCluster(AwsLandscape<ShardingKey> landscape) {
        super(landscape);
        this.hosts = new HashSet<>();
    }

    @Override
    public Iterable<AwsInstance<ShardingKey>> getHosts() {
        return Collections.unmodifiableCollection(hosts);
    }

    /**
     * Gets an iterable converting the hosts to ApacheReverseProxies. Assumes the hosts "list/map" is up to date.
     */
    private Iterable<ApacheReverseProxy<ShardingKey, MetricsT, ProcessT>> getReverseProxies() {
        return Util.map(hosts, host -> new ApacheReverseProxy<>(getLandscape(), host));
    }

    @Override
    public void addHost(AwsInstance<ShardingKey> host) {
        hosts.add(host);
    }

    @Override
    public AwsInstance<ShardingKey> createHost(String name, InstanceType instanceType, AwsAvailabilityZone az,
            String keyName) throws TimeoutException, Exception {
        final AwsInstance<ShardingKey> host = getLandscape().launchHost(
                (instanceId, availabilityZone, privateIpAddress, launchTimePoint,
                        landscape) -> new AwsInstanceImpl<ShardingKey>(instanceId, availabilityZone, privateIpAddress,
                                launchTimePoint, landscape),
                getAmiId(az.getRegion()), instanceType, az, keyName, getSecurityGroups(az.getRegion()),
                Optional.of(Tags.with(StartAwsHost.NAME_TAG_NAME, name).and(LandscapeConstants.DISPOSABLE_PROXY, "")
                        .and(LandscapeConstants.REVERSE_PROXY_TAG_NAME, "")),
                "");
        addHost(host);
        Wait.wait(() -> host.getInstance().state().name().equals(InstanceStateName.RUNNING), (result) -> result, true,
                Optional.of(Duration.ofSeconds(60 * 7)), Duration.ofSeconds(30), Level.INFO,
                "Is instance in the running state check");
        for (TargetGroup<ShardingKey> targetGroup : getLandscape().getTargetGroups(az.getRegion())) {
            targetGroup.getTagDescriptions().forEach(description -> description.tags().forEach(tag -> {
                if (tag.key().equals(LandscapeConstants.ALL_REVERSE_PROXIES)) {
                    final ApplicationLoadBalancer<ShardingKey> loadBalancer = targetGroup.getLoadBalancer();
                    if (loadBalancer != null && loadBalancer.getArn().contains(LandscapeConstants.NLB_ARN_CONTAINS)) {
                        getLandscape().addIpTargetToTargetGroup(targetGroup, Collections.singleton(host));
                        logger.info("Added " + host.getPrivateAddress().getHostAddress() + " to NLB target group"
                                + targetGroup.getTargetGroupArn());
                    } else if (loadBalancer != null) {
                        targetGroup.addTarget(host);
                        logger.info(
                                "Added " + host.getInstanceId() + " to target group" + targetGroup.getTargetGroupArn());
                    }
                }
            }));
        }
        return host;
    }

    @Override
    public void removeHost(AwsInstance<ShardingKey> host, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        assert Util.contains(getHosts(), host);
        if (Util.size(getHosts()) == 1) {
            throw new IllegalStateException(
                    "Trying to remove the last hosts of reverse proxy " + this + ". Use terminate() instead");
        }
        AwsInstance<ShardingKey> instanceFromHost = new AwsInstanceImpl<ShardingKey>(host.getInstanceId(),
                host.getAvailabilityZone(), host.getPrivateAddress(Landscape.WAIT_FOR_PROCESS_TIMEOUT),
                host.getLaunchTimePoint(), getLandscape());
        final List<TargetGroup<ShardingKey>> targetGroupsHostResidesIn = new ArrayList<>();
        for (TargetGroup<ShardingKey> targetGroup : getLandscape()
                .getTargetGroups(host.getAvailabilityZone().getRegion())) {
            final String loadBalancerArn = targetGroup.getLoadBalancerArn();
            if (loadBalancerArn != null) {
                final Iterator<TagDescription> tagDescriptions = targetGroup.getTagDescriptions().iterator();
                while (tagDescriptions.hasNext()) {
                    final TagDescription tagDescription = tagDescriptions.next();
                    if (tagDescription.hasTags()) {
                        tagDescription.tags().forEach(tag -> {
                            if (tag.key().equals(LandscapeConstants.ALL_REVERSE_PROXIES) && targetGroup.getRegisteredTargets().containsKey(instanceFromHost)) {
                                targetGroupsHostResidesIn.add(targetGroup);
                                if (loadBalancerArn.contains(LandscapeConstants.NLB_ARN_CONTAINS)) {
                                    getLandscape().removeIpTargetFromTargetGroup(targetGroup, Collections.singleton(instanceFromHost));
                                } else {
                                    targetGroup.removeTarget(instanceFromHost);
                                }
                            }
                        });
                    }
                }
            }
        }
        Wait.wait(() -> {
            for (TargetGroup<ShardingKey> tg : targetGroupsHostResidesIn) {
                final TargetHealth targetHealth = tg.getRegisteredTargets().get(instanceFromHost);
                if (targetHealth != null && targetHealth.state().equals(TargetHealthStateEnum.DRAINING)) {
                    return false;
                }
            }
            return true;
        }, Optional.of(Duration.ofSeconds(60 * 10)), Duration.ofSeconds(20), Level.INFO, "Waiting for target to drain"); // Default is 5 minute draining time, as of writing.
        ApacheReverseProxy<ShardingKey, MetricsT, ProcessT> proxy = new ApacheReverseProxy<>(getLandscape(), host);
        proxy.rotateLogs(optionalKeyName, privateKeyEncryptionPassphrase);
        getLandscape().terminate(host); // This assumes that the host is running only the reverse proxy process.
    }

    /**
     * Gets the security groups in the region that a reverse proxy instance should or does have.
     */
    private Iterable<SecurityGroup> getSecurityGroups(Region region) {
        return getLandscape().getDefaultSecurityGroupsForReverseProxy(region);
    }

    /**
     * Gets the latest image in the current region with the correct tag for creating a reverse proxy.
     */
    private MachineImage getAmiId(Region region) {
        return getLandscape().getLatestImageWithType(region, LandscapeConstants.IMAGE_TYPE_REVERSE_PROXY);
    }

    @Override
    public void terminate() {
        Set<AwsInstance<ShardingKey>> hosts = new HashSet<>();
        Util.addAll(getHosts(), hosts);
        for (final AwsInstance<ShardingKey> host : hosts) {
            getLandscape().terminate(host);
        }
    }

    /**
     * Chooses any one instance in the cluster, in the region, to apply the redirect to. Upon a push, a Git hook is
     * triggered to propagate the changes to the others in the cluster.
     * 
     * @param redirectSetter
     *            The ConsumerWithException to apply the necessary redirect to the proxy.
     */
    private void setRedirect(ConsumerWithException<ApacheReverseProxy<ShardingKey, MetricsT, ProcessT>> redirectSetter)
            throws Exception {
        if (getReverseProxies().iterator().hasNext()) {
            final ApacheReverseProxy<ShardingKey, MetricsT, ProcessT> proxy = getReverseProxies().iterator().next();
            redirectSetter.accept(proxy);
        }
    }

    @Override
    public void setPlainRedirect(String hostname, ProcessT applicationProcess, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.setPlainRedirect(hostname, applicationProcess, optionalKeyName,
                privateKeyEncryptionPassphrase));
    }

    @Override
    public void setHomeRedirect(String hostname, ProcessT applicationProcess, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.setHomeRedirect(hostname, applicationProcess, optionalKeyName,
                privateKeyEncryptionPassphrase));
    }

    @Override
    public void setEventRedirect(String hostname, ProcessT applicationProcess, UUID eventId,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.setEventRedirect(hostname, applicationProcess, eventId, optionalKeyName,
                privateKeyEncryptionPassphrase));
    }

    @Override
    public void setEventSeriesRedirect(String hostname, ProcessT applicationProcess, UUID leaderboardGroupId,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.setEventSeriesRedirect(hostname, applicationProcess, leaderboardGroupId,
                optionalKeyName, privateKeyEncryptionPassphrase));
    }

    @Override
    public void setEventArchiveRedirect(String hostname, UUID eventId, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.setEventArchiveRedirect(hostname, eventId, optionalKeyName,
                privateKeyEncryptionPassphrase));
    }

    @Override
    public void setEventSeriesArchiveRedirect(String hostname, UUID leaderboardGroupId,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.setEventSeriesArchiveRedirect(hostname, leaderboardGroupId, optionalKeyName,
                privateKeyEncryptionPassphrase));
    }

    @Override
    public void setHomeArchiveRedirect(String hostname, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.setHomeArchiveRedirect(hostname, optionalKeyName, privateKeyEncryptionPassphrase));
    }

    @Override
    public void setScopeRedirect(Scope<ShardingKey> scope, ProcessT applicationProcess) throws Exception {
        setRedirect(proxy -> proxy.setScopeRedirect(scope, applicationProcess));
    }

    @Override
    public void removeRedirect(String hostname, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        setRedirect(proxy -> proxy.removeRedirect(hostname, optionalKeyName, privateKeyEncryptionPassphrase));
    }

    @Override
    public void removeRedirect(Scope<ShardingKey> scope, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(proxy -> proxy.removeRedirect(scope, optionalKeyName, privateKeyEncryptionPassphrase));
    }
}
