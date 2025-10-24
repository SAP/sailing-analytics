package com.sap.sailing.landscape.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.shiro.authz.AuthorizationException;
import org.json.simple.parser.ParseException;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.landscape.ALBToReverseProxyArchiveRedirectMapper;
import com.sap.sailing.landscape.AwsSessionCredentialsWithExpiry;
import com.sap.sailing.landscape.EligibleInstanceForReplicaSetFindingStrategy;
import com.sap.sailing.landscape.LandscapeService;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.SailingReleaseRepository;
import com.sap.sailing.landscape.common.RemoteServiceMappingConstants;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.procedures.CreateLaunchTemplateAndAutoScalingGroup;
import com.sap.sailing.landscape.procedures.DeployProcessOnMultiServer;
import com.sap.sailing.landscape.procedures.SailingAnalyticsHostSupplier;
import com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration;
import com.sap.sailing.landscape.procedures.SailingAnalyticsProcessFactory;
import com.sap.sailing.landscape.procedures.SailingAnalyticsReplicaConfiguration;
import com.sap.sailing.landscape.procedures.SailingAnalyticsReplicaConfiguration.Builder;
import com.sap.sailing.landscape.procedures.SailingProcessConfigurationVariables;
import com.sap.sailing.landscape.procedures.StartMultiServer;
import com.sap.sailing.landscape.procedures.StartSailingAnalyticsHost;
import com.sap.sailing.landscape.procedures.StartSailingAnalyticsMasterHost;
import com.sap.sailing.landscape.procedures.StartSailingAnalyticsReplicaHost;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sailing.server.gateway.interfaces.MasterDataImportResult;
import com.sap.sailing.server.gateway.interfaces.SailingServer;
import com.sap.sailing.server.gateway.interfaces.SailingServerFactory;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;
import com.sap.sse.landscape.DefaultProcessConfigurationVariables;
import com.sap.sse.landscape.InboundReplicationConfiguration;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.application.ProcessFactory;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsAutoScalingGroup;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.AwsShard;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.ReverseProxy;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.common.shared.RedirectDTO;
import com.sap.sse.landscape.aws.impl.AwsApplicationReplicaSetImpl;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.impl.DNSCache;
import com.sap.sse.landscape.aws.orchestration.AddShardingKeyToShard;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.aws.orchestration.CopyAndCompareMongoDatabase;
import com.sap.sse.landscape.aws.orchestration.CreateDNSBasedLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.CreateDynamicLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.CreateLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.CreateShard;
import com.sap.sse.landscape.aws.orchestration.RemoveShardingKeyFromShard;
import com.sap.sse.landscape.aws.orchestration.ShardProcedure;
import com.sap.sse.landscape.aws.orchestration.StartAwsHost;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.ServerAdminRole;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.util.RemoteServerUtil;
import com.sap.sse.shared.util.Wait;
import com.sap.sse.util.JvmUtils;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sap.sse.util.ThreadPoolUtil;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;
import software.amazon.awssdk.services.sts.model.Credentials;

public class LandscapeServiceImpl implements LandscapeService {
    private static final Logger logger = Logger.getLogger(LandscapeServiceImpl.class.getName());
    
    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/SailingLandscape_StringMessages";

    private static final String TEMPORARY_UPGRADE_REPLICA_NAME_SUFFIX = " (Upgrade Replica)";

    private static final String UPGRADE_REPLICA_TAG_KEY = "upgradeReplica";

    private final FullyInitializedReplicableTracker<SecurityService> securityServiceTracker;

    private final ProcessFactory<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, SailingAnalyticsHost<String>> processFactoryFromHostAndServerDirectory;
    
    private final ServiceTracker<SailingServerFactory, SailingServerFactory> sailingServerFactoryTracker;
    
    public LandscapeServiceImpl(BundleContext context) {
        securityServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, SecurityService.class);
        sailingServerFactoryTracker = ServiceTrackerFactory.createAndOpen(context, SailingServerFactory.class);
        processFactoryFromHostAndServerDirectory =
                (host, port, serverDirectory, telnetPort, serverName, additionalProperties)->{
                    try {
                        final Number expeditionUdpPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.EXPEDITION_PORT.name());
                        final Number igtimiRiotPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT.name());
                        return new SailingAnalyticsProcessImpl<String>(port, host, serverDirectory, telnetPort, serverName,
                                expeditionUdpPort == null ? null : expeditionUdpPort.intValue(),
                                igtimiRiotPort == null ? null : igtimiRiotPort.intValue(), getLandscape());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
    }
    
    @Override
    public AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createApplicationReplicaSet(
            String regionId, String name, boolean newSharedMasterInstance, String sharedInstanceType,
            String dedicatedInstanceType, boolean dynamicLoadBalancerMapping, String releaseNameOrNullForLatestMaster,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String masterReplicationBearerToken,
            String replicaReplicationBearerToken, String optionalDomainName, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort,
            Optional<Integer> minimumAutoScalingGroupSize, Optional<Integer> maximumAutoScalingGroupSize) throws Exception {
        final AwsLandscape<String> landscape = getLandscape();
        final String hostname = getHostname(name, optionalDomainName);
        final Iterable<ResourceRecordSet> existingDNSRulesForHostname = landscape.getResourceRecordSets(hostname);
        // Failing early in case DNS record already exists (see also bug 5826):
        if (existingDNSRulesForHostname != null && !Util.isEmpty(existingDNSRulesForHostname)) {
            throw new IllegalArgumentException("DNS record for "+hostname+" already exists");
        }
        final AwsRegion region = new AwsRegion(regionId, landscape);
        final Release release = getRelease(releaseNameOrNullForLatestMaster);
        final com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration.Builder<?, String> masterConfigurationBuilder =
                createMasterConfigurationBuilder(name, masterReplicationBearerToken, optionalMemoryInMegabytesOrNull,
                        newSharedMasterInstance ? optionalMemoryTotalSizeFactorOrNull : null, optionalIgtimiRiotPort, region, release);
        final String bearerTokenUsedByReplicas = getEffectiveBearerToken(replicaReplicationBearerToken);
        final InboundReplicationConfiguration inboundMasterReplicationConfiguration = masterConfigurationBuilder.getInboundReplicationConfiguration().get();
        establishServerGroupAndTryToMakeCurrentUserItsOwnerAndMember(name, bearerTokenUsedByReplicas,
                inboundMasterReplicationConfiguration.getMasterHostname(), inboundMasterReplicationConfiguration.getMasterHttpPort());
        final com.sap.sailing.landscape.procedures.StartSailingAnalyticsMasterHost.Builder<?, String> masterHostBuilder = StartSailingAnalyticsMasterHost.masterHostBuilder(masterConfigurationBuilder);
        masterHostBuilder
            .setInstanceType(InstanceType.valueOf(newSharedMasterInstance ? sharedInstanceType : dedicatedInstanceType))
            .setOptionalTimeout(Landscape.WAIT_FOR_HOST_TIMEOUT)
            .setLandscape(landscape)
            .setRegion(region)
            .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase);
        if (optionalKeyName != null) {
            masterHostBuilder.setKeyName(optionalKeyName);
        }
        if (newSharedMasterInstance) {
            masterHostBuilder
                .setInstanceName(SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_DEFAULT_NAME)
                .setTags(Tags.with(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE));
        }
        final StartSailingAnalyticsMasterHost<String> masterHostStartProcedure = masterHostBuilder.build();
        masterHostStartProcedure.run();
        final SailingAnalyticsProcess<String> master = masterHostStartProcedure.getSailingAnalyticsProcess();
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result =
            createLoadBalancingAndAutoScalingSetup(landscape, region, name, master, release, dedicatedInstanceType,
                dynamicLoadBalancerMapping, optionalKeyName, privateKeyEncryptionPassphrase, optionalDomainName,
                Optional.of(masterHostBuilder.getMachineImage()), bearerTokenUsedByReplicas,
                minimumAutoScalingGroupSize, maximumAutoScalingGroupSize, optionalIgtimiRiotPort);
        final Optional<SailingAnalyticsProcess<String>> unmanagedReplica = minimumAutoScalingGroupSize.map(minASGSize->{
            final List<SailingAnalyticsProcess<String>> unmanagedReplicas = new ArrayList<>();
            if (minASGSize == 0) {
                logger.info("No auto-scaling replica forced for replica set "+name+"; starting with an unmanaged replica on a shared instance");
                try {
                    unmanagedReplicas.add(launchUnmanagedReplica(result, region, optionalKeyName, privateKeyEncryptionPassphrase,
                            bearerTokenUsedByReplicas, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull,
                            optionalIgtimiRiotPort,
                            Optional.of(InstanceType.valueOf(sharedInstanceType)), /* optionalPreferredInstanceToDeployTo */ Optional.empty()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            return unmanagedReplicas.isEmpty() ? null : unmanagedReplicas.get(0);
        });
        // if an unmanaged replica process was launched, return a replica set that contains it; otherwise use the one we already have (without any replica)
        return unmanagedReplica.map(ur->{
                try {
                    return getLandscape().getApplicationReplicaSet(region, name, master, Collections.singleton(ur), Landscape.WAIT_FOR_PROCESS_TIMEOUT,
                            Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            
        }).orElse(result);
    }
    
    @Override
    public AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> deployApplicationToExistingHost(String replicaSetName,
            SailingAnalyticsHost<String> hostToDeployTo, String replicaInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String masterReplicationBearerToken, String replicaReplicationBearerToken,
            String optionalDomainName, Optional<Integer> optionalMinimumAutoScalingGroupSize, Optional<Integer> optionalMaximumAutoScalingGroupSize,
            Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort,
            Optional<InstanceType> optionalSharedInstanceTypeForNewReplicaHost, Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployUnmanagedReplicaTo) throws Exception {
        return deployApplicationToExistingHostInternal(hostToDeployTo.getRegion(),
                replicaSetName, hostToDeployTo,
                replicaInstanceType, dynamicLoadBalancerMapping, releaseNameOrNullForLatestMaster, optionalKeyName,
                privateKeyEncryptionPassphrase, masterReplicationBearerToken, replicaReplicationBearerToken,
                optionalDomainName, optionalMinimumAutoScalingGroupSize, optionalMaximumAutoScalingGroupSize,
                optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull, optionalIgtimiRiotPort,
                optionalSharedInstanceTypeForNewReplicaHost, optionalPreferredInstanceToDeployUnmanagedReplicaTo);
    }
    
    @Override
    public <AppConfigBuilderT extends SailingAnalyticsReplicaConfiguration.Builder<AppConfigBuilderT, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT>>
    SailingAnalyticsProcess<String> deployReplicaToExistingHost(final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            SailingAnalyticsHost<String> hostToDeployTo, String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort) throws Exception {
        logger.info("Deploying replica for application replica set "+replicaSet.getName()+" to host "+hostToDeployTo);
        return spinUpReplicaAndRegisterInPublicTargetGroup(hostToDeployTo.getRegion(), replicaSet, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase, replicaReplicationBearerToken,
                optionalIgtimiRiotPort, /* processLauncher: */ (AppConfigBuilderT replicaConfigurationBuilder)->{
                    if (optionalMemoryInMegabytesOrNull != null) {
                        replicaConfigurationBuilder.setMemoryInMegabytes(optionalMemoryInMegabytesOrNull);
                    } else if (optionalMemoryTotalSizeFactorOrNull != null) {
                        replicaConfigurationBuilder.setMemoryTotalSizeFactor(optionalMemoryTotalSizeFactorOrNull);
                    }
                    try {
                        Integer igtimiRiotPort = replicaSet.getMaster().getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                        if (igtimiRiotPort != null) {
                            replicaConfigurationBuilder.setIgtimiRiotPort(igtimiRiotPort);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    // the process launcher uses the DeployProcessOnMultiServer procedure to launch the process based on the replica config 
                    final DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT> replicaDeploymentProcessBuilder =
                            DeployProcessOnMultiServer.<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT> builder(replicaConfigurationBuilder, hostToDeployTo);
                    if (optionalKeyName != null) {
                        replicaDeploymentProcessBuilder.setKeyName(optionalKeyName);
                    }
                    replicaDeploymentProcessBuilder
                        .setOptionalTimeout(Landscape.WAIT_FOR_PROCESS_TIMEOUT)
                        .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase);
                    final DeployProcessOnMultiServer<String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT> replicaDeploymentProcess;
                    try {
                        replicaDeploymentProcess = replicaDeploymentProcessBuilder.build();
                        replicaDeploymentProcess.run();
                        return replicaDeploymentProcess.getProcess();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    
    /**
     * Starts a first master process of a new replica set whose name is provided by the {@code replicaSetName}
     * parameter. The process is started on the host identified by the {@code hostToDeployTo} parameter. A set of
     * available ports is identified and chosen automatically. The target groups and load balancing set-up is created.
     * The {@code replicaInstanceType} is used to configure the launch template used by the auto-scaling group which is
     * also created so that when dedicated replicas need to be provided during auto-scaling, their instance type is
     * known.
     * <p>
     * 
     * The "internal" method exists in order to declare a few type parameters which wouldn't be possible on the GWT RPC
     * interface method as some of these types are not seen by clients.
     * 
     * @param optionalMinimumAutoScalingGroupSize
     *            defaults to 1; if 0, a replica process will be launched on an eligible shared instance in an
     *            availability zone different from that of the instance hosting the master process. Otherwise, at least
     *            one auto-scaling replica will ensure availability of the replica set.
     * @param optionalIgtimiRiotPort
     *            if non-{@code null}, this will be used to configure the
     *            {@link SailingProcessConfigurationVariables#IGTIMI_RIOT_PORT} variable for the process
     * @param optionalInstanceType
     *            if a new instance must be launched because no eligible one is found, this parameter can be used to
     *            specify its instance type. It defaults to {@link InstanceType#I3_2_XLARGE} which is reasonably suited
     *            for a multi-process set-up.
     * @param optionalPreferredInstanceToDeployTo
     *            If {@link Optional#isPresent() present}, specifies a preferred host for the answer given by
     *            {@link #getInstanceToDeployTo(AwsApplicationReplicaSet)}. However, if the instance turns out not to be
     *            eligible by the rules regarding general
     *            {@link AwsApplicationReplicaSet#isEligibleForDeployment(ApplicationProcessHost, Optional, Optional, byte[])
     *            eligibility} (based mainly on port and directory available as well as not being managed by an
     *            auto-scaling group) and additional rules regarding availability zone "anti-affinity" as defined here
     *            (see {@link #master}), the default rules for selecting or launching an eligible instance apply.
     */
    private <AppConfigBuilderT extends SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String>,
        MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT>>
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> deployApplicationToExistingHostInternal(
                    AwsRegion region, String replicaSetName, SailingAnalyticsHost<String> hostToDeployTo,
                    String replicaInstanceType, boolean dynamicLoadBalancerMapping,
                    String releaseNameOrNullForLatestMaster, String optionalKeyName,
                    byte[] privateKeyEncryptionPassphrase, String masterReplicationBearerToken,
                    String replicaReplicationBearerToken, String optionalDomainName,
                    Optional<Integer> optionalMinimumAutoScalingGroupSize,
                    Optional<Integer> optionalMaximumAutoScalingGroupSize, Integer optionalMemoryInMegabytesOrNull,
                    Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort,
                    Optional<InstanceType> optionalInstanceType, Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo) throws Exception {
        final AwsLandscape<String> landscape = getLandscape();
        final String hostname = getHostname(replicaSetName, optionalDomainName);
        final Iterable<ResourceRecordSet> existingDNSRulesForHostname = landscape.getResourceRecordSets(hostname);
        // Failing early in case DNS record already exists (see also bug 5826):
        if (existingDNSRulesForHostname != null && !Util.isEmpty(existingDNSRulesForHostname)) {
            throw new IllegalArgumentException("DNS record for "+hostname+" already exists");
        }
        final Release release = getRelease(releaseNameOrNullForLatestMaster);
        final AppConfigBuilderT masterConfigurationBuilder = createMasterConfigurationBuilder(replicaSetName,
                masterReplicationBearerToken, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull,
                optionalIgtimiRiotPort, region, release);
        final InboundReplicationConfiguration inboundMasterReplicationConfiguration = masterConfigurationBuilder.getInboundReplicationConfiguration().get();
        final String bearerTokenUsedByReplicas = getEffectiveBearerToken(replicaReplicationBearerToken);
        establishServerGroupAndTryToMakeCurrentUserItsOwnerAndMember(replicaSetName, bearerTokenUsedByReplicas,
                inboundMasterReplicationConfiguration.getMasterHostname(), inboundMasterReplicationConfiguration.getMasterHttpPort());
        final SailingAnalyticsProcess<String> master = deployProcessToSharedInstance(hostToDeployTo,
                masterConfigurationBuilder, optionalKeyName, privateKeyEncryptionPassphrase);
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
            createLoadBalancingAndAutoScalingSetup(landscape, region, replicaSetName, master, release, replicaInstanceType, dynamicLoadBalancerMapping,
                optionalKeyName, privateKeyEncryptionPassphrase, optionalDomainName, /* use default AMI as replica machine image */ Optional.empty(),
                bearerTokenUsedByReplicas, optionalMinimumAutoScalingGroupSize, optionalMaximumAutoScalingGroupSize, optionalIgtimiRiotPort);
        final Iterable<SailingAnalyticsProcess<String>> replicas;
        if (optionalMinimumAutoScalingGroupSize.isPresent() && optionalMinimumAutoScalingGroupSize.get() == 0) {
            replicas = Collections.singleton(launchUnmanagedReplica(replicaSet, region, optionalKeyName,
                privateKeyEncryptionPassphrase, bearerTokenUsedByReplicas, optionalMemoryInMegabytesOrNull,
                optionalMemoryTotalSizeFactorOrNull, optionalIgtimiRiotPort, optionalInstanceType, optionalPreferredInstanceToDeployTo));
        } else {
            replicas = Collections.emptySet();
        }
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSetWithReplica =
                landscape.getApplicationReplicaSet(region, replicaSet.getServerName(), master, replicas,
                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        return replicaSetWithReplica;
    }

    private <AppConfigBuilderT extends SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT>>
    SailingAnalyticsProcess<String> deployProcessToSharedInstance(
            SailingAnalyticsHost<String> hostToDeployTo, final AppConfigBuilderT applicationConfigurationBuilder,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT> multiServerAppDeployerBuilder =
                DeployProcessOnMultiServer.<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT> builder(
                        applicationConfigurationBuilder, hostToDeployTo);
        if (optionalKeyName != null) {
            multiServerAppDeployerBuilder.setKeyName(optionalKeyName);
        }
        multiServerAppDeployerBuilder
                .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase)
                .setOptionalTimeout(Landscape.WAIT_FOR_HOST_TIMEOUT);
        final DeployProcessOnMultiServer<String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT> deployer = multiServerAppDeployerBuilder.build();
        deployer.run();
        final SailingAnalyticsProcess<String> master = deployer.getProcess();
        return master;
    }

    /**
     * Starts a replica process for the given {@code replicaSet}. The memory configuration can optionally be defined. It
     * defaults to the usual large share of the physical RAM of the instance it is deployed on. The replica is launched
     * on the {@code optionalPreferredInstanceToDeployTo} host if specified and eligible, else on the "best" existing
     * eligible instance (based on aspects such as the processes already deployed on the instance). If no running
     * instance is found to be eligible (e.g., because none has the port required by the replica available, or all run
     * in the same availability zone as the replica set's master process), a new instance is launched.
     * <p>
     * 
     * If a new instance needs to be launched, the instance type can optionally be specified. It defaults to something
     * with large fast swap space and a reasonable amount of physical RAM that would allow somewhere between two and
     * four processes to share the physical RAM even during a live event.
     * 
     * @param optionalIgtimiRiotPort
     *            if non-{@code null}, configures the {@link SailingProcessConfigurationVariables#IGTIMI_RIOT_PORT}
     *            variable of the replica
     * @param optionalInstanceType
     *            defaults to {@link SharedLandscapeConstants#DEFAULT_SHARED_INSTANCE_TYPE_NAME}
     */
    private SailingAnalyticsProcess<String> launchUnmanagedReplica(
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            AwsRegion region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String replicaReplicationBearerToken, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort,
            Optional<InstanceType> optionalInstanceType, Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo) throws Exception {
        final EligibleInstanceForReplicaSetFindingStrategy strategyForFindingOrLaunchingInstanceForUnmangedReplica =
                new EligibleInstanceForReplicaSetFindingStrategyImpl(this, region, optionalKeyName,
                        privateKeyEncryptionPassphrase, /* master==false because we'd like to deploy a replica */ false,
                        /* mustBeDifferentAvailabilityZone */ true, optionalInstanceType,
                        optionalPreferredInstanceToDeployTo);
        final SailingAnalyticsProcess<String> replica = deployReplicaToExistingHost(
                replicaSet, strategyForFindingOrLaunchingInstanceForUnmangedReplica.getInstanceToDeployTo(replicaSet),
                optionalKeyName, privateKeyEncryptionPassphrase, replicaReplicationBearerToken,
                optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull,
                optionalIgtimiRiotPort);
        return replica;
    }
    
    @Override
    public Util.Triple<DataImportProgress, CompareServersResult, String> archiveReplicaSet(String regionId,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSetToArchive,
            String bearerTokenOrNullForApplicationReplicaSetToArchive,
            String bearerTokenOrNullForArchive,
            Duration durationToWaitBeforeCompareServers,
            int maxNumberOfCompareServerAttempts, boolean removeApplicationReplicaSet, MongoEndpoint moveDatabaseHere,
            String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception {
        if (removeApplicationReplicaSet && applicationReplicaSetToArchive.isLocalReplicaSet()) {
            throw new IllegalArgumentException("A replica set cannot archive itself if it is going to be removed. Current replica set: "+ServerInfo.getName());
        }
        final AwsRegion region = new AwsRegion(regionId, getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> archiveReplicaSet = getApplicationReplicaSet(
                region, SharedLandscapeConstants.ARCHIVE_SERVER_APPLICATION_REPLICA_SET_NAME,
                Landscape.WAIT_FOR_PROCESS_TIMEOUT.get().asMillis(), optionalKeyName,
                passphraseForPrivateKeyDecryption);
        if (archiveReplicaSet == null) {
            final String msg = "Couldn't find archive replica set tagged as "+SharedLandscapeConstants.ARCHIVE_SERVER_APPLICATION_REPLICA_SET_NAME;
            logger.severe(msg);
            throw new IllegalArgumentException(msg);
        }
        logger.info("Found ARCHIVE replica set " + archiveReplicaSet + " with master " + archiveReplicaSet.getMaster());
        final UUID idForProgressTracking = UUID.randomUUID();
        final RedirectDTO defaultRedirect = RedirectDTO.from(getDefaultRedirectPath(applicationReplicaSetToArchive.getDefaultRedirectRule()));
        final String hostnameFromWhichToArchive = applicationReplicaSetToArchive.getHostname();
        final String hostnameOfArchive = archiveReplicaSet.getHostname();
        final SailingServerFactory sailingServerFactory = sailingServerFactoryTracker.getService();
        if (sailingServerFactory == null) {
            throw new IllegalStateException("Couldn't find SailingServerFactory");
        }
        final SailingServer from = sailingServerFactory.getSailingServer(new URL("https", hostnameFromWhichToArchive, "/"), bearerTokenOrNullForApplicationReplicaSetToArchive);
        final SailingServer archive = sailingServerFactory.getSailingServer(new URL("https", hostnameOfArchive, "/"), bearerTokenOrNullForArchive);
        final SailingServer failoverArchive = sailingServerFactory.getSailingServer(new URL("https", SharedLandscapeConstants.ARCHIVE_FAILOVER_ADDRESS, "/"), bearerTokenOrNullForArchive);
        logger.info("Importing master data from "+from+" to "+archive);
        sendMailToReplicaSetOwner(archiveReplicaSet, "StartingToArchiveReplicaSetIntoSubject", "StartingToArchiveReplicaSetIntoBody", Optional.of(ServerActions.CAN_IMPORT_MASTERDATA));
        sendMailToReplicaSetOwner(applicationReplicaSetToArchive, "StartingToArchiveReplicaSetSubject", "StartingToArchiveReplicaSetBody", Optional.of(ServerActions.CAN_EXPORT_MASTERDATA));
        // Note: if from.getLeaderboardGroupIds() returns an empty set, "all" leaderboards will be imported by the MDI which again is the empty set.
        // In this case, no comparison is required; in fact it wouldn't even work because passing an empty set to the archive into which the import
        // was done would implicitly compare all leaderboard groups, resulting in the entire archive server content being the "diff."
        final MasterDataImportResult mdiResult = archive.importMasterData(from, from.getLeaderboardGroupIds(), /* override */ true, /* compress */ true,
                /* import wind */ true, /* import device configurations */ false, /* import tracked races and start tracking */ true, Optional.of(idForProgressTracking));
        if (mdiResult == null) {
            logger.severe("Couldn't find any result for the master data import. Aborting.");
            throw new IllegalStateException("Couldn't find any result for the master data import. Aborting archiving of replica set "+from);
        }
        final DataImportProgress mdiProgress = waitForMDICompletionOrError(archive, idForProgressTracking, /* log message */ "MDI from "+hostnameFromWhichToArchive+" into "+hostnameOfArchive);
        final CompareServersResult compareServersResult;
        final String mongoDbArchivingErrorMessage;
        if (mdiProgress != null && !mdiProgress.failed() && mdiProgress.getResult() != null) {
            logger.info("MDI from "+hostnameFromWhichToArchive+" info "+hostnameOfArchive+" succeeded. Waiting "+durationToWaitBeforeCompareServers+" before starting to compare content...");
            if (Util.isEmpty(from.getLeaderboardGroupIds())) {
                logger.info("Empty set of leaderboard groups imported. Not making any comparison.");
                compareServersResult = null;
            } else {
                Thread.sleep(durationToWaitBeforeCompareServers.asMillis());
                logger.info("Comparing contents now...");
                compareServersResult = Wait.wait(()->from.compareServers(Optional.empty(), archive, Optional.of(from.getLeaderboardGroupIds())),
                        csr->!csr.hasDiffs(), /* retryOnException */ true, Optional.of(durationToWaitBeforeCompareServers.times(maxNumberOfCompareServerAttempts)),
                        durationToWaitBeforeCompareServers, Level.INFO, "Comparing leaderboard groups with IDs "+Util.joinStrings(", ", from.getLeaderboardGroupIds())+
                        " between importing server "+hostnameOfArchive+" and exporting server "+hostnameFromWhichToArchive);
            }
            if (Util.isEmpty(from.getLeaderboardGroupIds()) || compareServersResult != null) {
                if (Util.isEmpty(from.getLeaderboardGroupIds()) || !compareServersResult.hasDiffs()) {
                    logger.info("No differences found during comparing server contents. Moving on...");
                    final Set<UUID> eventIDs = new HashSet<>();
                    for (final Iterable<UUID> eids : Util.map(mdiResult.getLeaderboardGroupsImported(), lgWithEventIds->lgWithEventIds.getEventIds())) {
                        Util.addAll(eids, eventIDs);
                    }
                    final ReverseProxy<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, RotatingFileBasedLog> reverseProxyCluster =
                            getLandscape().getCentralReverseProxy(region);
                    // TODO bug5311: when refactoring this for general scope migration, moving to a dedicated replica set will not require this
                    // TODO bug5311: when refactoring this for general scope migration, moving into a cold storage server other than ARCHIVE will require ALBToReverseProxyRedirectMapper instead
                    logger.info("Adding reverse proxy rules for migrated content pointing to ARCHIVE");
                    defaultRedirect.accept(new ALBToReverseProxyArchiveRedirectMapper<>(
                            reverseProxyCluster, hostnameFromWhichToArchive, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption));
                    if (removeApplicationReplicaSet) {
                        logger.info("Removing remote sailing server references to "+from+" from archive server "+archive);
                        try {
                            archive.removeRemoteServerReference(from);
                            failoverArchive.removeRemoteServerReference(from);
                        } catch (Exception e) {
                            logger.log(Level.INFO, "Exception trying to remove remote server reference to "+from+
                                    "; probably such a reference didn't exist");
                        }
                        logger.info("Removing the application replica set archived ("+from+") was requested");
                        mongoDbArchivingErrorMessage = removeApplicationReplicaSet(regionId, applicationReplicaSetToArchive, moveDatabaseHere, optionalKeyName, passphraseForPrivateKeyDecryption);
                    } else {
                        mongoDbArchivingErrorMessage = null;
                        logger.info("Removing remote sailing server references to events on "+from+" with IDs "+eventIDs+" from archive server "+archive);
                        archive.removeRemoteServerEventReferences(from, eventIDs);
                    }
                } else {
                    mongoDbArchivingErrorMessage = null;
                    logger.severe("Even after "+maxNumberOfCompareServerAttempts+" attempts and waiting a total of "+
                            durationToWaitBeforeCompareServers.times(maxNumberOfCompareServerAttempts)+
                            " there were the following differences between exporting server "+hostnameFromWhichToArchive+
                            " and importing server "+hostnameOfArchive+":\nDifferences on importing side: "+compareServersResult.getADiffs()+
                            "\nDifferences on exporting side: "+compareServersResult.getBDiffs()+
                            "\nNot proceeding further. You need to resolve the issues manually.");
                }
            } else {
                mongoDbArchivingErrorMessage = null;
                logger.severe("Even after "+maxNumberOfCompareServerAttempts+" attempts and waiting a total of "+
                        durationToWaitBeforeCompareServers.times(maxNumberOfCompareServerAttempts)+
                        " the comparison of servers "+hostnameOfArchive+" and "+hostnameFromWhichToArchive+
                        " did not produce a result. Not proceeding. You have to resolve the issue manually.");
            }
        } else {
            logger.severe("The Master Data Import (MDI) from "+hostnameFromWhichToArchive+" into "+hostnameOfArchive+
                    " did not work"+(mdiProgress != null ? mdiProgress.getErrorMessage() : " (no result at all)"));
            compareServersResult = null;
            mongoDbArchivingErrorMessage = null;
        }
        sendMailToReplicaSetOwner(archiveReplicaSet, "FinishedToArchiveReplicaSetIntoSubject", "FinishedToArchiveReplicaSetIntoBody", Optional.of(ServerActions.CAN_IMPORT_MASTERDATA));
        sendMailToReplicaSetOwner(applicationReplicaSetToArchive, "FinishedToArchiveReplicaSetSubject", "FinishedToArchiveReplicaSetBody", Optional.of(ServerActions.CAN_EXPORT_MASTERDATA));
        return new Util.Triple<>(mdiProgress, compareServersResult, mongoDbArchivingErrorMessage);
    }

    private void terminateReplicasNotManagedByAutoScalingGroup(AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            String optionalKeyName, byte[] passphraseForPrivateKeyDecryption) throws InterruptedException, ExecutionException {
        // only terminate replica if not running on host created by auto-scaling group
        final AwsAutoScalingGroup autoScalingGroup = applicationReplicaSet.getAutoScalingGroup();
        for (final SailingAnalyticsProcess<String> replica : applicationReplicaSet.getReplicas()) {
            if (autoScalingGroup == null || !replica.getHost().isManagedByAutoScalingGroup(Collections.singleton(autoScalingGroup))) {
                logger.info("Found replica "+replica+" running on an instance not managed by auto-scaling group " +
                        (autoScalingGroup != null ? autoScalingGroup.getName() : "") + ". Stopping...");
                replica.stopAndTerminateIfLast(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption);
            }
        }
    }
    
    @Override
    public String removeApplicationReplicaSet(String regionId,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            MongoEndpoint moveDatabaseHere, String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception {
        if (applicationReplicaSet.isLocalReplicaSet()) {
            throw new IllegalArgumentException("A replica set cannot remove itself. Current replica set: "+ServerInfo.getName());
        }
        final AwsRegion region = new AwsRegion(regionId, getLandscape());
        final AwsAutoScalingGroup autoScalingGroup = applicationReplicaSet.getAutoScalingGroup();
        final CompletableFuture<Void> autoScalingGroupRemoval;
        // Remove all shards
        for (AwsShard<String> shard : applicationReplicaSet.getShards().keySet()) {
            applicationReplicaSet.removeShard(shard, getLandscape());
        }
        // Before stopping all replicas, make sure the master is contained in the public target group.
        // Sometimes, operators may manually remove it to keep the master from receiving too much pressure.
        // In this case, stopping all replicas would make the replica set become unavailable for public read access:
        logger.info("Adding the master "+applicationReplicaSet.getMaster().getHost().getId()+" to public target group "+
                applicationReplicaSet.getMasterTargetGroup().getName()+" if it isn't already contained");
        getLandscape().addTargetsToTargetGroup(applicationReplicaSet.getPublicTargetGroup(), Collections.singleton(applicationReplicaSet.getMaster().getHost()));
        // Only then can we start terminating the replica processes:
        terminateReplicasNotManagedByAutoScalingGroup(applicationReplicaSet, optionalKeyName, passphraseForPrivateKeyDecryption);
        if (autoScalingGroup != null) {
            // remove the launch template used by the auto scaling group and the auto scaling group itself;
            // this will also terminate all replicas spun up by the auto-scaling group
            autoScalingGroupRemoval = getLandscape().removeAutoScalingGroupAndLaunchTemplate(autoScalingGroup);
            Wait.wait(()->isAllAutoScalingReplicasShutDown(applicationReplicaSet, autoScalingGroup),
                    Landscape.WAIT_FOR_HOST_TIMEOUT, Duration.ONE_SECOND.times(10),
                    Level.INFO, "Waiting for auto-scaling replicas to shut down");
        } else {
            autoScalingGroupRemoval = new CompletableFuture<>();
            autoScalingGroupRemoval.complete(null);
        }
        // important to obtain this before shutting down master because then master won't reply to the MONGODB_URI request anymore
        final Database fromDatabase = applicationReplicaSet.getMaster().getDatabaseConfiguration(region,
                Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName),
                passphraseForPrivateKeyDecryption);
        autoScalingGroupRemoval.thenAccept(v->{
            // remove the load balancer rules
            try {
                getLandscape().deleteLoadBalancerListenerRules(region, Util.toArray(applicationReplicaSet.getLoadBalancerRules(), new Rule[0]));
                applicationReplicaSet.getMaster().stopAndTerminateIfLast(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        // remove the target groups
        getLandscape().deleteTargetGroup(applicationReplicaSet.getMasterTargetGroup());
        getLandscape().deleteTargetGroup(applicationReplicaSet.getPublicTargetGroup());
        final String loadBalancerDNSName = applicationReplicaSet.getLoadBalancer().getDNSName();
        final Iterable<Rule> currentLoadBalancerRuleSet = applicationReplicaSet.getLoadBalancer().getRules();
        if (applicationReplicaSet.getResourceRecordSet() != null) {
            // remove the load balancer if it is a DNS-mapped one and there are no rules left other than the default rule
            if (applicationReplicaSet.getResourceRecordSet().resourceRecords().stream().filter(rr->
                    AwsLandscape.removeTrailingDotFromHostname(rr.value()).equals(loadBalancerDNSName)).findAny().isPresent() &&
                (Util.isEmpty(currentLoadBalancerRuleSet) ||
                    (Util.size(currentLoadBalancerRuleSet) == 1 && currentLoadBalancerRuleSet.iterator().next().isDefault()))) {
                logger.info("No more rules "+(!Util.isEmpty(currentLoadBalancerRuleSet) ? "except default rule " : "")+
                        "left in load balancer "+applicationReplicaSet.getLoadBalancer().getName()+" which was DNS-mapped; deleting.");
                applicationReplicaSet.getLoadBalancer().delete();
            } else {
                logger.info("Keeping load balancer "+loadBalancerDNSName+" because it is not DNS-mapped or still has rules.");
            }
            // remove the DNS record if this replica set was a DNS-mapped one
            logger.info("Removing DNS CNAME record "+applicationReplicaSet.getResourceRecordSet());
            getLandscape().removeDNSRecord(applicationReplicaSet.getHostedZoneId(), applicationReplicaSet.getHostname(), RRType.CNAME, loadBalancerDNSName);
        } else {
            logger.info("Keeping load balancer "+loadBalancerDNSName+" because it is not DNS-mapped.");
        }
        final String mongoDBArchivingErrorMessage;
        if (moveDatabaseHere != null) {
            if (moveDatabaseHere.equals(fromDatabase.getEndpoint())) {
                mongoDBArchivingErrorMessage = "Requested to move database "+fromDatabase+" to its own endpoint "+moveDatabaseHere+
                        ". Not executing because this would kill the database.";
                logger.warning(mongoDBArchivingErrorMessage);
            } else {
                final Database toDatabase = moveDatabaseHere.getDatabase(fromDatabase.getName());
                logger.info("Archiving the database content of "+fromDatabase.getConnectionURI()+" to "+toDatabase.getConnectionURI());
                final CopyAndCompareMongoDatabase<String> copyAndCompareMongoDatabaseProcedure = getCopyAndCompareMongoDatabaseProcedure(fromDatabase, toDatabase);
                copyAndCompareMongoDatabaseProcedure.run();
                mongoDBArchivingErrorMessage = copyAndCompareMongoDatabaseProcedure.getMongoDbArchivingErrorMessage();
            }
        } else {
            mongoDBArchivingErrorMessage = null;
            logger.info("No archiving of database content was requested. Leaving "+fromDatabase.getConnectionURI()+" untouched.");
        }
        getSecurityService().deleteAllDataForRemovedObject(SecuredSecurityTypes.SERVER.getQualifiedObjectIdentifier(
                new TypeRelativeObjectIdentifier(applicationReplicaSet.getServerName())));
        return mongoDBArchivingErrorMessage;
    }

    private boolean isAllAutoScalingReplicasShutDown(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            AwsAutoScalingGroup autoScalingGroup) throws Exception {
        final Iterable<SailingAnalyticsProcess<String>> replicas = applicationReplicaSet.getMaster().getReplicas(Landscape.WAIT_FOR_PROCESS_TIMEOUT,
                new SailingAnalyticsHostSupplier<String>(), processFactoryFromHostAndServerDirectory);
        for (final SailingAnalyticsProcess<String> replica : replicas) {
            if (replica.getHost().isManagedByAutoScalingGroup(Collections.singleton(autoScalingGroup)) && replica.isAlive(Landscape.WAIT_FOR_PROCESS_TIMEOUT)) {
                logger.info("Replica "+replica+" is managed by auto-scaling group "+autoScalingGroup.getName()+" and is still alive.");
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a procedure that drops the {@link #toDatabase} first; then copies {@code fromDatabase} to {@code toDatabase} and starts comparing
     * the result with the original. If the comparison fails, the {@link CopyAndCompareMongoDatabase#run()} method will throw an
     * {@link IllegalStateException}. If the comparison succeeds, the {@code fromDatabase} will then be deleted.
     */
    private <BuilderT extends CopyAndCompareMongoDatabase.Builder<BuilderT, String>> CopyAndCompareMongoDatabase<String>
    getCopyAndCompareMongoDatabaseProcedure(Database fromDatabase, Database toDatabase) throws Exception {
        BuilderT builder = CopyAndCompareMongoDatabase.<BuilderT, String>builder()
                .dropTargetFirst(true)
                .dropSourceAfterSuccessfulCopy(true)
                .setSourceDatabase(fromDatabase)
                .setTargetDatabase(toDatabase)
                .setAdditionalDatabasesToDelete(Collections.singleton(fromDatabase.getWithDifferentName(
                        fromDatabase.getName()+SailingAnalyticsReplicaConfiguration.Builder.DEFAULT_REPLICA_DATABASE_NAME_SUFFIX)));
        builder
            .setLandscape(getLandscape());
        return builder.build();
    }

    private DataImportProgress waitForMDICompletionOrError(SailingServer archive,
            UUID idForProgressTracking, String logMessage) throws Exception {
        return Wait.wait(()->archive.getMasterDataImportProgress(idForProgressTracking), progress->progress.failed() || progress.getResult() != null,
                /* retryOnException */ false, LandscapeService.MDI_TIMEOUT, LandscapeService.TIME_TO_WAIT_BETWEEN_MDI_COMPLETION_CHECKS,
                Level.INFO, logMessage);
    }

    private SecurityService getSecurityService() {
        try {
            return securityServiceTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String getDefaultRedirectPath(Rule defaultRedirectRule) {
        final String result;
        if (defaultRedirectRule == null) {
            result = null;
        } else {
            result = defaultRedirectRule.actions().stream().map(action->RedirectDTO.toString(action.redirectConfig().path(),
                        Optional.ofNullable(action.redirectConfig().query()))).findAny().orElse(null);
        }
        return result;
    }

    @Override
    public AwsLandscape<String> getLandscape() {
        final String keyId;
        final String secret;
        final String sessionToken;
        final AwsSessionCredentialsWithExpiry sessionCredentials = getSessionCredentials();
        final AwsLandscape<String> result;
        if (sessionCredentials != null) {
            keyId = sessionCredentials.getAccessKeyId();
            secret = sessionCredentials.getSecretAccessKey();
            sessionToken = sessionCredentials.getSessionToken();
            result = AwsLandscape.obtain(keyId, secret, sessionToken, RemoteServiceMappingConstants.pathPrefixForShardingKey);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * For the logged-in user checks the LANDSCAPE:MANAGE:AWS permission, and if present, tries to obtain the user preference
     * named like {@link #USER_PREFERENCE_FOR_SESSION_TOKEN}. If found and not yet expired, they are returned. Otherwise,
     * {@code null} is returned, indicating to the caller that new session credentials shall be obtained which shall then be
     * stored to the user preference again for future reference.
     */
    @Override
    public AwsSessionCredentialsWithExpiry getSessionCredentials() {
        final AwsSessionCredentialsWithExpiry result;
        final AwsSessionCredentialsFromUserPreference credentialsPreferences = getSecurityService().getPreferenceObject(
                getSecurityService().getCurrentUser().getName(), USER_PREFERENCE_FOR_SESSION_TOKEN);
        if (credentialsPreferences != null) {
            final AwsSessionCredentialsWithExpiry credentials = credentialsPreferences.getAwsSessionCredentialsWithExpiry();
            if (credentials.getExpiration().before(TimePoint.now())) {
                result = null;
            } else {
                result = credentials;
            }
        } else {
            result = null;
        }
        return result;
    }
    
    
    @Override
    public void createMfaSessionCredentials(String awsAccessKey, String awsSecret, String mfaTokenCode) {
        final Credentials credentials = AwsLandscape.obtain(awsAccessKey, awsSecret, RemoteServiceMappingConstants.pathPrefixForShardingKey).getMfaSessionCredentials(mfaTokenCode);
        final AwsSessionCredentialsWithExpiryImpl result = new AwsSessionCredentialsWithExpiryImpl(
                credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken(),
                TimePoint.of(credentials.expiration().toEpochMilli()));
        final AwsSessionCredentialsFromUserPreference credentialsPreferences = new AwsSessionCredentialsFromUserPreference(result);
        getSecurityService().setPreferenceObject(
                getSecurityService().getCurrentUser().getName(), LandscapeService.USER_PREFERENCE_FOR_SESSION_TOKEN, credentialsPreferences);
    }

    @Override
    public void createSessionCredentials(String awsKeyId, String awsKeySecret, String sessionToken) {
        final AwsSessionCredentialsWithExpiryImpl result = new AwsSessionCredentialsWithExpiryImpl(
                awsKeyId, awsKeySecret, sessionToken,
                // for the expiry, guess that the creation of the session token passed here didn't happen more than five minutes ago
                TimePoint.now().plus(Duration.ONE_HOUR.times(12).minus(Duration.ONE_MINUTE.times(5))));
        final AwsSessionCredentialsFromUserPreference credentialsPreferences = new AwsSessionCredentialsFromUserPreference(result);
        getSecurityService().setPreferenceObject(
                getSecurityService().getCurrentUser().getName(), LandscapeService.USER_PREFERENCE_FOR_SESSION_TOKEN, credentialsPreferences);
    }

    @Override
    public boolean hasValidSessionCredentials() {
        return getSessionCredentials() != null;
    }

    @Override
    public void clearSessionCredentials() {
        getSecurityService().unsetPreference(getSecurityService().getCurrentUser().getName(), LandscapeService.USER_PREFERENCE_FOR_SESSION_TOKEN);
    }

    @Override
    public String getFullyQualifiedHostname(String unqualifiedHostname, Optional<String> optionalDomainName) {
        final String domainName = optionalDomainName.orElse(SharedLandscapeConstants.DEFAULT_DOMAIN_NAME);
        final String fullyQualifiedHostname = unqualifiedHostname+"."+domainName;
        return fullyQualifiedHostname;
    }

    @Override
    public Release getRelease(String releaseNameOrNullForLatestMaster) {
        return releaseNameOrNullForLatestMaster==null
                ? SailingReleaseRepository.INSTANCE.getLatestMasterRelease()
                : SailingReleaseRepository.INSTANCE.getRelease(releaseNameOrNullForLatestMaster);
    }

    private void establishServerGroupAndTryToMakeCurrentUserItsOwnerAndMember(String serverName,
            String bearerTokenUsedByReplicas, String securityServiceHostname,
            Integer securityServicePort)
            throws MalformedURLException, ClientProtocolException, IOException, ParseException, IllegalAccessException {
        final String serverGroupName = serverName + ServerInfo.SERVER_GROUP_NAME_SUFFIX;
        final SailingServerFactory sailingServerFactory = sailingServerFactoryTracker.getService();
        final SailingServer securityServiceServer = sailingServerFactory.getSailingServer(
                RemoteServerUtil.getBaseServerUrl(securityServiceHostname, securityServicePort==null?443:securityServicePort), bearerTokenUsedByReplicas);
        final UUID userGroupId = securityServiceServer.getUserGroupIdByName(serverGroupName);
        final UUID groupId;
        if (userGroupId != null) {
            groupId = userGroupId;
            final TypeRelativeObjectIdentifier serverGroupTypeRelativeObjectId = new TypeRelativeObjectIdentifier(userGroupId.toString());
            final Iterable<Pair<WildcardPermission, Boolean>> permissions = securityServiceServer.hasPermissions(Arrays.asList(
                    SecuredSecurityTypes.USER_GROUP.getPermissionForTypeRelativeIdentifier(DefaultActions.CREATE, serverGroupTypeRelativeObjectId),
                    SecuredSecurityTypes.USER_GROUP.getPermissionForTypeRelativeIdentifier(DefaultActions.READ, serverGroupTypeRelativeObjectId),
                    SecuredSecurityTypes.USER_GROUP.getPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE, serverGroupTypeRelativeObjectId),
                    SecuredSecurityTypes.USER_GROUP.getPermissionForTypeRelativeIdentifier(DefaultActions.DELETE, serverGroupTypeRelativeObjectId),
                    SecuredSecurityTypes.SERVER.getPermissionForTypeRelativeIdentifier(DefaultActions.CREATE, serverGroupTypeRelativeObjectId),
                    SecuredSecurityTypes.SERVER.getPermissionForTypeRelativeIdentifier(DefaultActions.READ, serverGroupTypeRelativeObjectId),
                    SecuredSecurityTypes.SERVER.getPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE, serverGroupTypeRelativeObjectId),
                    SecuredSecurityTypes.SERVER.getPermissionForTypeRelativeIdentifier(DefaultActions.DELETE, serverGroupTypeRelativeObjectId)));
            for (final Pair<WildcardPermission, Boolean> permission : permissions) {
                if (!permission.getB()) {
                    final String msg = "Subject "+securityServiceServer.getUsername()+" on server "+securityServiceHostname+
                            " is not allowed "+permission.getA()+". Not allowing to create application replica set for "+serverName;
                    logger.warning(msg);
                    throw new AuthorizationException(msg);
                }
            }
            // Now we know the user is permitted to create/read/update/delete the user group and the server object in the remote
            // security realm that the application replica set's master process will use if it existed already. Add the user to the group
            securityServiceServer.addCurrentUserToGroup(userGroupId);
        } else {
            groupId = securityServiceServer.createUserGroupAndAddCurrentUser(serverGroupName);
            try {
                securityServiceServer.addRoleToUser(ServerAdminRole.getInstance().getId(), securityServiceServer.getUsername(),
                        /* qualified for server group: */ groupId, null, /* transitive */ true);
            } catch (Exception e) {
                // this didn't work, but it's not the end of the world if we cannot grant the requesting user the
                // event_manager:{group-name} role; the user may end up not having SERVER:CREATE_OBJECT...
                logger.warning("Couldn't grant role "+ServerAdminRole.getInstance().getName()+" to user "+securityServiceServer.getUsername()+": "+e.getMessage());
            }
            try {
                // try to set the group owner of the new group to the group itself, allowing all users with role user:{group-name} to
                // change / edit it.
                securityServiceServer.setGroupAndUserOwner(SecuredSecurityTypes.USER_GROUP, new TypeRelativeObjectIdentifier(groupId.toString()),
                        Optional.empty() /* displayName */, Optional.of(groupId), Optional.empty() /* leave user owner unchanged */);
            } catch (Exception e) {
                // this didn't work, but it's not the end of the world if we cannot update the new group's ownership,
                // although it's a bit surprising because the user identified by the bearerToken should be the group's
                // owner...
                logger.warning("Couldn't update user group ownership of user group "+serverGroupName+": "+e.getMessage());
            }
        }
        ensureGroupMembersCanReadGroup(securityServiceServer, groupId);
    }

    private void ensureGroupMembersCanReadGroup(SailingServer securityServiceServer, UUID groupId) throws ClientProtocolException, IOException, ParseException {
        // cleanly copy result as it may be unmodifiable
        final Map<UUID, Set<String>> acls = new HashMap<>(securityServiceServer.getAccessControlLists(SecuredSecurityTypes.USER_GROUP, new TypeRelativeObjectIdentifier(groupId.toString())));
        final Set<String> actionsForGroup = acls.computeIfAbsent(groupId, gid->new HashSet<>());
        if (!actionsForGroup.contains(DefaultActions.READ.name()) && !actionsForGroup.contains("!"+DefaultActions.READ.name())) {
            actionsForGroup.add(DefaultActions.READ.name());
            securityServiceServer.setAccessControlLists(SecuredSecurityTypes.USER_GROUP, new TypeRelativeObjectIdentifier(groupId.toString()), acls);
        }
    }

    private <AppConfigBuilderT extends com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String>> AppConfigBuilderT createMasterConfigurationBuilder(
            String replicaSetName, String optionalMasterReplicationBearerTokenOrNull, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort, final AwsRegion region, final Release release) {
        final AppConfigBuilderT masterConfigurationBuilder = SailingAnalyticsMasterConfiguration.masterBuilder();
        final String bearerTokenUsedByMaster = getEffectiveBearerToken(optionalMasterReplicationBearerTokenOrNull);
        final User currentUser = getSecurityService().getCurrentUser();
        if (currentUser != null && currentUser.isEmailValidated() && currentUser.getEmail() != null) {
            masterConfigurationBuilder.setCommaSeparatedEmailAddressesToNotifyOfStartup(currentUser.getEmail());
        }
        masterConfigurationBuilder
            .setLandscape(getLandscape())
            .setServerName(replicaSetName)
            .setRelease(release)
            .setRegion(region)
            // TODO bug5684: probably this is the place to add the REPLICATE_MASTER_SERVLET_HOST/REPLICATE_MASTER_EXCHANGE_NAME variables to point to a default security service?
            .setInboundReplicationConfiguration(InboundReplicationConfiguration.builder().setCredentials(new BearerTokenReplicationCredentials(bearerTokenUsedByMaster)).build());
        if (optionalIgtimiRiotPort != null) {
            masterConfigurationBuilder.setIgtimiRiotPort(optionalIgtimiRiotPort);
        }
        applyMemoryConfigurationToApplicationConfigurationBuilder(masterConfigurationBuilder, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull);
        return masterConfigurationBuilder;
    }

    /**
     * No specific memory configuration is made here; replicas are mostly launched on a dedicated host and hence can
     * grab as much memory as they can get on that host. Callers that want to deploy to a shared host should use
     * {@link Builder#setMemoryInMegabytes(int)} and/or {@link Builder#setMemoryTotalSizeFactor(int)} to be more
     * specific about the memory configuration.
     */
    private <AppConfigBuilderT extends SailingAnalyticsReplicaConfiguration.Builder<AppConfigBuilderT, String>>
    AppConfigBuilderT createReplicaConfigurationBuilder(final AwsRegion region,
            String replicaSetName, final int masterPort, Integer optionalIgtimiRiotPort,
            final Release release, final String bearerTokenUsedByReplicas, final String masterHostname) {
        final AppConfigBuilderT replicaConfigurationBuilder = SailingAnalyticsReplicaConfiguration.replicaBuilder();
        final User currentUser = getSecurityService().getCurrentUser();
        if (currentUser != null && currentUser.isEmailValidated() && currentUser.getEmail() != null) {
            replicaConfigurationBuilder.setCommaSeparatedEmailAddressesToNotifyOfStartup(currentUser.getEmail());
        }
        // no specific memory configuration is made here; replicas are mostly launched on a dedicated host and hence can
        // grab as much memory as they can get on that host
        replicaConfigurationBuilder
            .setLandscape(getLandscape())
            .setRegion(region)
            .setServerName(replicaSetName)
            .setRelease(release)
            .setPort(masterPort) // replicas need to run on the same port for target group "interoperability"
            .setInboundReplicationConfiguration(InboundReplicationConfiguration.builder()
                    .setMasterHostname(masterHostname) // don't set the master port; the replica set talks to "itself" through the load balancer using HTTPS
                    .setCredentials(new BearerTokenReplicationCredentials(bearerTokenUsedByReplicas))
                    .build());
        if (optionalIgtimiRiotPort != null) {
            replicaConfigurationBuilder.setIgtimiRiotPort(optionalIgtimiRiotPort);
        }
        return replicaConfigurationBuilder;
    }
    
    private void applyMemoryConfigurationToApplicationConfigurationBuilder(
            final AwsApplicationConfiguration.Builder<?, ?, ?, ?, ?> applicationConfigurationBuilder,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull) {
        if (optionalMemoryInMegabytesOrNull != null) {
            applicationConfigurationBuilder.setMemoryInMegabytes(optionalMemoryInMegabytesOrNull);
        } else if (optionalMemoryTotalSizeFactorOrNull != null) {
            applicationConfigurationBuilder.setMemoryTotalSizeFactor(optionalMemoryTotalSizeFactorOrNull);
        }
    }

    private AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLoadBalancingAndAutoScalingSetup(
            final AwsLandscape<String> landscape, final AwsRegion region, String replicaSetName,
            final SailingAnalyticsProcess<String> master, final Release release, String replicaInstanceType,
            boolean dynamicLoadBalancerMapping, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String optionalDomainName,
            final Optional<AmazonMachineImage<String>> replicaMachineImage,
            final String bearerTokenUsedByReplicas, Optional<Integer> minimumNumberOfReplicas, Optional<Integer> maximumNumberOfReplicas,
            Integer optionalIgtimiRiotPort)
            throws Exception, JSchException, IOException, InterruptedException, SftpException, TimeoutException {
        logger.info("Creating load balancing and auto-scaling set-up for replica set "+replicaSetName);
        if (dynamicLoadBalancerMapping && !region.getId().equals(SharedLandscapeConstants.REGION_WITH_DEFAULT_LOAD_BALANCER)) {
            // see bug5669:
            throw new IllegalArgumentException("You must not request dynamic load balancer mapping in regions other than "+
                    SharedLandscapeConstants.REGION_WITH_DEFAULT_LOAD_BALANCER+
                    "; you tried it for region "+region.getId());
        }
        final CreateLoadBalancerMapping.Builder<?, ?, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLoadBalancerMappingBuilder =
                dynamicLoadBalancerMapping ? CreateDynamicLoadBalancerMapping.builder() : CreateDNSBasedLoadBalancerMapping.builder();
        final String masterHostname = getHostname(replicaSetName, optionalDomainName);
        final CreateLoadBalancerMapping<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLoadBalancerMapping = createLoadBalancerMappingBuilder
            .setProcess(master)
            .setHostname(masterHostname)
            .setTargetGroupNamePrefix(SAILING_TARGET_GROUP_NAME_PREFIX)
            .setSecurityGroupForVpc(landscape.getDefaultSecurityGroupForApplicationHosts(region))
            .setLandscape(landscape)
            .build();
        createLoadBalancerMapping.run();
        // construct a replica configuration which is used to produce the user data for the launch template used in an auto-scaling group
        final Builder<?, String> replicaConfigurationBuilder = createReplicaConfigurationBuilder(region, replicaSetName, master.getPort(), optionalIgtimiRiotPort, release, bearerTokenUsedByReplicas, masterHostname);
        // Now wait for master to become healthy before creating auto-scaling; otherwise it may happen that the replica tried to start
        // replication before the master is ready (see also bug 5527).
        master.waitUntilReady(Landscape.WAIT_FOR_HOST_TIMEOUT);
        final CreateLaunchTemplateAndAutoScalingGroup.Builder<String, ?, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLaunchTemplateAndAutoScalingGroupBuilder =
                CreateLaunchTemplateAndAutoScalingGroup.builder(landscape, region, replicaSetName, createLoadBalancerMapping.getPublicTargetGroup());
        createLaunchTemplateAndAutoScalingGroupBuilder
            .setInstanceType(InstanceType.valueOf(replicaInstanceType))
            .setTags(Tags.with(StartAwsHost.NAME_TAG_NAME, StartSailingAnalyticsHost.INSTANCE_NAME_DEFAULT_PREFIX+replicaSetName+" (Auto-Replica)")
                         .and(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, replicaSetName))
            .setOptionalTimeout(Landscape.WAIT_FOR_HOST_TIMEOUT)
            .setReplicaConfiguration(replicaConfigurationBuilder.build()); // use the default scaling parameters (currently 1/30/30000)
        minimumNumberOfReplicas.ifPresent(minNumberOfReplicas->createLaunchTemplateAndAutoScalingGroupBuilder.setMinReplicas(minNumberOfReplicas));
        maximumNumberOfReplicas.ifPresent(maxNumberOfReplicas->createLaunchTemplateAndAutoScalingGroupBuilder.setMaxReplicas(maxNumberOfReplicas));
        if (replicaMachineImage.isPresent()) {
            createLaunchTemplateAndAutoScalingGroupBuilder.setImage(replicaMachineImage.get());
        } else {
            // obtain the latest AMI for launching a Sailing Analytics replica host:
            createLaunchTemplateAndAutoScalingGroupBuilder.setImage(
                    StartSailingAnalyticsReplicaHost.replicaHostBuilder(replicaConfigurationBuilder)
                        .setLandscape(getLandscape())
                        .setRegion(region)
                        .getMachineImage());
        }
        if (optionalKeyName != null) {
            createLaunchTemplateAndAutoScalingGroupBuilder.setKeyName(optionalKeyName);
        }
        createLaunchTemplateAndAutoScalingGroupBuilder.build().run();
        final CompletableFuture<Iterable<ApplicationLoadBalancer<String>>> allLoadBalancersInRegion = landscape.getLoadBalancersAsync(region);
        final CompletableFuture<Map<TargetGroup<String>, Iterable<TargetHealthDescription>>> allTargetGroupsInRegion = landscape.getTargetGroupsAsync(region);
        final CompletableFuture<Map<Listener, Iterable<Rule>>> allLoadBalancerRulesInRegion = landscape.getLoadBalancerListenerRulesAsync(region, allLoadBalancersInRegion);
        final CompletableFuture<Iterable<AutoScalingGroup>> autoScalingGroups = landscape.getAutoScalingGroupsAsync(region);
        final CompletableFuture<Iterable<LaunchTemplate>> launchTemplates = landscape.getLaunchTemplatesAsync(region);
        final CompletableFuture<Iterable<LaunchTemplateVersion>> launchTemplateDefaultVersions = landscape.getLaunchTemplateDefaultVersionsAsync(region);
        final DNSCache dnsCache = landscape.getNewDNSCache();
        final AwsApplicationReplicaSet<String,SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet =
                new AwsApplicationReplicaSetImpl<>(replicaSetName, masterHostname, master, /* no replicas yet */ Optional.empty(),
                        allLoadBalancersInRegion, allTargetGroupsInRegion, allLoadBalancerRulesInRegion,
                        autoScalingGroups, launchTemplates, launchTemplateDefaultVersions, dnsCache, RemoteServiceMappingConstants.pathPrefixForShardingKey,
                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        return applicationReplicaSet;
    }

    @Override
    public String getHostname(String replicaSetName, String optionalDomainName) {
        final String domainName = Optional.ofNullable(optionalDomainName).orElse(SharedLandscapeConstants.DEFAULT_DOMAIN_NAME);
        final String masterHostname = replicaSetName+"."+domainName;
        return masterHostname;
    }

    @Override
    public AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> upgradeApplicationReplicaSet(AwsRegion region,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String releaseOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String replicaReplicationBearerToken)
            throws MalformedURLException, IOException, TimeoutException, Exception {
        if (replicaSet.isLocalReplicaSet()) {
            throw new IllegalArgumentException(
                    "A replica set cannot upgrade itself. Current replica set: " + ServerInfo.getName());
        }
        if (!replicaSet.getShards().isEmpty() && replicaSet.getAutoScalingGroup() == null) {
            throw new IllegalStateException(
                    "A replica set is expected to have an auto scaling group if it has shards");
        }
        final Release release = getRelease(releaseOrNullForLatestMaster);
        final String effectiveReplicaReplicationBearerToken = getEffectiveBearerToken(replicaReplicationBearerToken);
        final int oldAutoScalingGroupMinSize;
        final AwsAutoScalingGroup autoScalingGroup = replicaSet.getAutoScalingGroup();
        final Collection<AwsAutoScalingGroup> affectedAutoScalingGroups = new ArrayList<>();
        final int autoScalingReplicaCount;
        replicaSet.getShards().keySet().forEach(t -> affectedAutoScalingGroups.add(t.getAutoScalingGroup()));
        if (autoScalingGroup != null) {
            affectedAutoScalingGroups.add(autoScalingGroup);
            oldAutoScalingGroupMinSize = autoScalingGroup.getAutoScalingGroup().minSize();
            int tempAutoScalingReplicaCount = 0;
            for (AwsAutoScalingGroup asg : affectedAutoScalingGroups) {
                tempAutoScalingReplicaCount = tempAutoScalingReplicaCount + asg.getAutoScalingGroup().desiredCapacity();
            }
            autoScalingReplicaCount = tempAutoScalingReplicaCount;
        } else {
            oldAutoScalingGroupMinSize = -1;
            autoScalingReplicaCount = -1;
        }
        final Set<SailingAnalyticsProcess<String>> replicasToStopAfterUpgradingMaster = new HashSet<>();
        Util.addAll(replicaSet.getReplicas(), replicasToStopAfterUpgradingMaster);
        final SailingAnalyticsProcess<String> additionalReplicaStarted = ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(
                replicaSet, optionalKeyName, privateKeyEncryptionPassphrase, effectiveReplicaReplicationBearerToken);
        if (replicaSet.getAutoScalingGroup() != null) {
            getLandscape().updateReleaseInAutoScalingGroups(region, replicaSet.getAutoScalingGroup().getLaunchTemplate(),
                    affectedAutoScalingGroups, replicaSet.getName(), release);
        }
        final SailingAnalyticsProcess<String> master = replicaSet.getMaster();
        master.refreshToRelease(release, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        // wait for master to turn healthy:
        logger.info("Waiting for master "+master+" to get ready with new release "+release.getName());
        master.waitUntilReady(Optional.of(Duration.ONE_DAY)); // wait a little longer since master may need to re-load many races
        logger.info("Launching upgrade replicas based on current set of replicas for replica set "+replicaSet.getName());
        final Iterable<SailingAnalyticsProcess<String>> temporaryUpgradeReplicas = launchUpgradeReplicasAndWaitUntilReady(
                replicaSet, release, effectiveReplicaReplicationBearerToken, Optional.ofNullable(optionalKeyName),
                privateKeyEncryptionPassphrase);
        final List<SailingAnalyticsProcess<String>> temporaryUpgradeReplicasMutable = Util.asList(temporaryUpgradeReplicas);
        // register master again with master and public target group
        logger.info("Adding master " + master + " and dedicated upgraded temporary replicas to target groups "
                + replicaSet.getPublicTargetGroup() + " and " + replicaSet.getMasterTargetGroup());
        replicaSet.getPublicTargetGroup().addTarget(master.getHost());
        replicaSet.getMasterTargetGroup().addTarget(master.getHost());
        replicaSet.getOtherTargetGroups().forEach(tg->tg.addTarget(master.getHost()));
        sendMailAboutMasterAvailable(replicaSet);
        // the following map stores the assignment of the temporary upgrade replicas to the public / shard target groups
        // for later removal as the final auto-scaling replicas start to replace them:
        final Map<TargetGroup<String>, Iterable<AwsInstance<String>>> tempUpgradeReplicasByTargetGroup = new HashMap<>();
        // add as many temporary upgrade replicas to each shard's target group as the target group has targets now:
        for (final AwsShard<String> shard : replicaSet.getShards().keySet()) {
            final TargetGroup<String> shardTargetGroup = shard.getTargetGroup();
            final Collection<AwsInstance<String>> hosts = new ArrayList<>();
            final int numberOfTargets = shardTargetGroup.getRegisteredTargets().size();
            for (int i = 0; i < numberOfTargets; i++) {
                final SailingAnalyticsProcess<String> tempUpgradeReplicaProcess = temporaryUpgradeReplicasMutable.remove(0);
                shardTargetGroup.addTarget(tempUpgradeReplicaProcess.getHost());
                hosts.add(tempUpgradeReplicaProcess.getHost());
            }
            tempUpgradeReplicasByTargetGroup.put(shardTargetGroup, hosts);
        }
        // add all other temporary upgrade replicas not used for shards to the public target group:
        replicaSet.getPublicTargetGroup().addTargets(Util.map(temporaryUpgradeReplicasMutable,
                temporaryUpgradeReplica -> temporaryUpgradeReplica.getHost()));
        replicaSet.getOtherTargetGroups().forEach(tg->tg.addTargets(Util.map(temporaryUpgradeReplicasMutable,
                temporaryUpgradeReplica -> temporaryUpgradeReplica.getHost())));
        tempUpgradeReplicasByTargetGroup.put(replicaSet.getPublicTargetGroup(), Util.map(temporaryUpgradeReplicasMutable, temporaryUpgradeReplica->temporaryUpgradeReplica.getHost()));
        replicaSet.getOtherTargetGroups().forEach(tg->tempUpgradeReplicasByTargetGroup.put(tg, Util.map(temporaryUpgradeReplicasMutable, temporaryUpgradeReplica->temporaryUpgradeReplica.getHost())));
        // if a replica was spun up (additionalReplicaStarted), remove from public target group and terminate:
        if (additionalReplicaStarted != null) {
            replicasToStopAfterUpgradingMaster.add(additionalReplicaStarted);
            if (replicaSet.getAutoScalingGroup() != null) {
                // run updating the auto-scaling group in the background; it's more time-critical now
                // to remove the old replicas from the target group
                for (AwsAutoScalingGroup asg : affectedAutoScalingGroups) {
                    ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor()
                            .execute(ThreadPoolUtil.INSTANCE.associateWithSubjectIfAny(() -> {
                                getLandscape().updateAutoScalingGroupMinSize(asg, oldAutoScalingGroupMinSize);
                            }));
                }
            } // else, the replica was started explicitly, without an auto-scaling group; in any case, all replicas still
            // on the old release will now be stopped:
        }
        final Set<SailingAnalyticsProcess<String>> newUpgradedUnmanagedReplicas = new HashSet<>();
        logger.info("Removing old replicas from public target group "+replicaSet.getPublicTargetGroup());
        replicaSet.getPublicTargetGroup().removeTargets(Util.map(replicasToStopAfterUpgradingMaster, replica->replica.getHost()));
        replicaSet.getOtherTargetGroups().forEach(tg->tg.removeTargets(Util.map(replicasToStopAfterUpgradingMaster, replica->replica.getHost())));
        for (final SailingAnalyticsProcess<String> replica : replicasToStopAfterUpgradingMaster) {
            // if managed by auto-scaling group or if it's an "unmanaged" / "explicit" replica and it's the one launched in order to have at least one, stop/terminate;
            final boolean managedByAutoScalingGroup = replica.getHost().isManagedByAutoScalingGroup(affectedAutoScalingGroups);
            if (managedByAutoScalingGroup ||
                    (additionalReplicaStarted != null && replica.getHost().getInstanceId().equals(additionalReplicaStarted.getHost().getInstanceId()))) {
                logger.info("Stopping (and terminating if last application process on host) replicas on old release: "+replicasToStopAfterUpgradingMaster);
                replica.stopAndTerminateIfLast(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
            } else { // otherwise, upgrade in place and add to target group again when ready
                logger.info("Refreshing unmanaged replica "+replica+" in place");
                replica.refreshToRelease(release, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                replica.waitUntilReady(Optional.of(Duration.ONE_DAY));
                replicaSet.getPublicTargetGroup().addTarget(replica.getHost());
                replicaSet.getOtherTargetGroups().forEach(tg->tg.addTarget(replica.getHost()));
                newUpgradedUnmanagedReplicas.add(replica);
            }
        }
        final Iterable<SailingAnalyticsProcess<String>> newUpgradedReplicas;
        if (autoScalingGroup != null && autoScalingReplicaCount > 0) {
            logger.info("Now waiting until " + autoScalingReplicaCount
                    + " auto-scaling replicas are ready before taking down dedicated temporary upgrade replicas");
            // Note: the wait call returns *all* of the master's replicas, not only the auto-scaling ones; it's the predicate that
            // filters them down to the auto-scaling ones, counts them and compares them to the required count
            newUpgradedReplicas = Wait.wait(() -> master.getReplicas(Landscape.WAIT_FOR_PROCESS_TIMEOUT,
                    new SailingAnalyticsHostSupplier<String>(), new SailingAnalyticsProcessFactory(this::getLandscape)),
                    replicas -> {
                        int size = Util.size(Util.filter(replicas,
                                replica -> replica.getHost().isManagedByAutoScalingGroup(affectedAutoScalingGroups)));
                        logger.info("Until now there are " + size + " auto-scaling replicas healthy");
                        return size >= autoScalingReplicaCount;
                    }, /* retryOnException */ true, Optional.of(Duration.ONE_DAY),
                    /* duration between attempts */ Duration.ONE_SECOND.times(30), Level.INFO,
                    "Waiting for " + autoScalingReplicaCount + " auto-scaling replicas to become ready");
        } else {
            logger.info("No auto-scaling group or auto-scaling group did not have managed instances; using only the upgraded unmanaged replicas "+
                    newUpgradedUnmanagedReplicas);
            newUpgradedReplicas = newUpgradedUnmanagedReplicas;
        }
        logger.info("Removing targets for all temporary upgrade replicas " + temporaryUpgradeReplicas);
        for (Entry<TargetGroup<String>, Iterable<AwsInstance<String>>> entry : tempUpgradeReplicasByTargetGroup.entrySet()) {
            entry.getKey().removeTargets(entry.getValue());
        }
        logger.info("Stopping/terminating all temporary upgrade replicas "+temporaryUpgradeReplicas);
        for (final SailingAnalyticsProcess<String> upgradeReplica : temporaryUpgradeReplicas) {
            upgradeReplica.stopAndTerminateIfLast(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        }
        return getLandscape().getApplicationReplicaSet(region, replicaSet.getServerName(), master,
                // don't use those temporary upgrade replicas that just got terminated:
                Util.filter(newUpgradedReplicas, newUpgradedReplica->!Util.contains(temporaryUpgradeReplicas, newUpgradedReplica)),
                Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
    }

    /**
     * Launches as many temporary upgrade replicas as are currently available in
     * {@link AwsApplicationReplicaSet#getReplicas() the replica set's replicas}, in the same availability zones as the
     * existing ones, and with the same instance types as the existing ones, then waits for them to become ready. The
     * replicas launched here are <em>not</em> added to any target group by this method. The differences compared to the
     * original replicas will be:
     * <ul>
     * <li>The new replicas will use the {@code release} specified which is expected to be the new release to which the
     * master already has been switched.</li>
     * <li>The new replicas will be named and tagged such that they can be recognized by an operator as dedicated
     * upgrade replicas</li>
     * <li>Instead of using the replica set's {@link AwsApplicationReplicaSet#getHostname() hostname} to identify the
     * master process to replicate from, the replicas produced by this method use the master's IP address and port
     * so that replication works also while the master is not registered with the replica set's master target group.</li>
     * </ul>
     * <p>
     * 
     * The replicas will be launched with their master configuration not based on the hostname but on the master's IP
     * address, therefore not depending on the master being registered in the "-m" target group. With this, old replicas
     * on the old version can remain in place with no other version showing up in either target group until all upgrade
     * replicas have become ready and can then be used to populate the public target group, together with the master
     * which is additionally registered in the "-m" target group then. This "switching" process happens in probably less
     * than a second and should reduce the time span during which processes running different versions may be
     * encountered to a minimum.
     * 
     * @return the replica processes launched
     */
    private <AppConfigBuilderT extends SailingAnalyticsReplicaConfiguration.Builder<AppConfigBuilderT, String>>
    Iterable<SailingAnalyticsProcess<String>> launchUpgradeReplicasAndWaitUntilReady(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            Release release, String replicationBearerToken, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final Set<SailingAnalyticsProcess<String>> result = new HashSet<>();
        final SailingAnalyticsProcess<String> master = replicaSet.getMaster();
        final Integer optionalIgtimiRiotPort = master.getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
        final AwsRegion region = master.getHost().getRegion();
        for (final SailingAnalyticsProcess<String> replica : replicaSet.getReplicas()) {
            final InstanceType instanceType = replica.getHost().getInstance().instanceType();
            // Determine the replica node's current heap size configuration ("MEMORY" variable) and
            // use it to set the upgrade replica's explicit memory size:
            final String memoryVariable = replica.getEnvShValueFor(DefaultProcessConfigurationVariables.MEMORY, Landscape.WAIT_FOR_PROCESS_TIMEOUT,
                    optionalKeyName, privateKeyEncryptionPassphrase);
            final Optional<Integer> megabytesFromJvmSize = JvmUtils.getMegabytesFromJvmSize(memoryVariable);
            logger.info("Determined replica set "+replicaSet.getName()+"'s replica memory size for replica "+replica+
                    " as "+memoryVariable+" which equals "+megabytesFromJvmSize+
                    "MB. Using for upgrade replica configuration with instance type "+instanceType+".");
            final AppConfigBuilderT replicaConfigurationBuilder =
                    createReplicaConfigurationBuilder(region, replicaSet.getServerName(), master.getPort(),
                            optionalIgtimiRiotPort, release, replicationBearerToken, replicaSet.getHostname());
            // regarding the dedicated temporary upgrade replica's memory configuration we can assume that either the
            // old replica was running on a dedicated instance and therefore had a memory configuration that uses the
            // instance's available RAM, so will fit into the new dedicated temporary upgrade instance; or the
            // old replica was on a shared instance; in this case we'll over-provision, but it won't be long.
            replicaConfigurationBuilder
                .setInboundReplicationConfiguration(InboundReplicationConfiguration.builder()
                    .setMasterHostname(master.getHost().getHostname())
                    .setMasterHttpPort(master.getPort())
                    .setCredentials(new BearerTokenReplicationCredentials(replicationBearerToken))
                    .build());
            megabytesFromJvmSize.ifPresent(
                    memoryInMegabytes->replicaConfigurationBuilder.setMemoryInMegabytes(memoryInMegabytes));
            final com.sap.sailing.landscape.procedures.StartSailingAnalyticsReplicaHost.Builder<?, String> replicaHostBuilder =
                    StartSailingAnalyticsReplicaHost.replicaHostBuilder(replicaConfigurationBuilder);
            replicaHostBuilder
                .setInstanceName(StartSailingAnalyticsHost.INSTANCE_NAME_DEFAULT_PREFIX+replicaSet.getName()+TEMPORARY_UPGRADE_REPLICA_NAME_SUFFIX)
                .setInstanceType(instanceType)
                .setOptionalTimeout(Landscape.WAIT_FOR_HOST_TIMEOUT)
                .setLandscape(getLandscape())
                .setRegion(region)
                .setTags(Tags.with(UPGRADE_REPLICA_TAG_KEY, replicaSet.getName()));
            optionalKeyName.ifPresent(replicaHostBuilder::setKeyName);
            final StartSailingAnalyticsReplicaHost<String> replicaHostStartProcedure = replicaHostBuilder.build();
            logger.info("Launching dedicated replica host of type "+instanceType+" for replica "+replica);
            replicaHostStartProcedure.run();
            Wait.wait(()->replicaHostStartProcedure.getSailingAnalyticsProcess().getHost().getInstance(), instance->instance != null, /* retryOnException */ true,
                    Landscape.WAIT_FOR_HOST_TIMEOUT, /* sleepBetweenAttempts */ Duration.ONE_SECOND.times(10), Level.WARNING,
                    "Waiting for replica instance with ID "+replicaHostStartProcedure.getHost().getInstanceId());
            result.add(replicaHostStartProcedure.getSailingAnalyticsProcess());
        }
        for (final SailingAnalyticsProcess<String> resultReplica : result) {
            logger.info("Waiting for replica "+resultReplica+" to become ready");
            resultReplica.waitUntilReady(Optional.of(Duration.ONE_DAY)); // the my or archive server will take that long...
        }
        return result;
    }

    @Override
    public Iterable<SailingAnalyticsHost<String>> getEligibleSharedHostsForReplicaSet(AwsRegion region,
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) {
        return Util.filter(getLandscape().getRunningHostsWithTagValue(region,
                SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE,
                new SailingAnalyticsHostSupplier<String>()), h->{
                    try {
                        return isEligibleForDeployment(h, replicaSet.getServerName(), replicaSet.getPort(),
                                replicaSet.getMaster().getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase),
                                Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    
    @Override
    public AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> getApplicationReplicaSet(
            final AwsRegion region, String replicaSetName, Long optionalTimeoutInMilliseconds, String optionalKeyName,
            byte[] passphraseForPrivateKeyDecryption) throws Exception {
        return Util.first(Util.filter(
                getLandscape().getApplicationReplicaSetsByTag(region,
                    SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, new SailingAnalyticsHostSupplier<String>(),
                    Optional.ofNullable(optionalTimeoutInMilliseconds).map(Duration::ofMillis), Optional.ofNullable(optionalKeyName),
                    passphraseForPrivateKeyDecryption),
                rs->rs.getName().equals(replicaSetName)));
    }
    
    /**
     * @return a new replica that was started in case no running replica was found in the {@code replicaSet}, otherwise
     *         {@code null}.
     */
    @Override
    public SailingAnalyticsProcess<String> ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            final String effectiveReplicaReplicationBearerToken) throws Exception, MalformedURLException, IOException,
            TimeoutException, InterruptedException, ExecutionException {
        final Set<SailingAnalyticsProcess<String>> replicasToStopReplicating = new HashSet<>();
        Util.addAll(replicaSet.getReplicas(), replicasToStopReplicating);
        final SailingAnalyticsProcess<String> additionalReplicaStarted;
        if (Util.isEmpty(Util.filter(new HashSet<>(replicasToStopReplicating), replica ->
            // exclude replicas belonging to a shard
            !replica.getHost().isManagedByAutoScalingGroup(replicaSet.getShards().keySet().stream().map(
                    shard->shard.getAutoScalingGroup())::iterator)))) {
            logger.info("No replica that doesn't belong to any shard was found for replica set " + replicaSet.getName()
                    + "; spinning one up and waiting for it to become healthy");
            additionalReplicaStarted = launchReplicaAndWaitUntilHealthy(replicaSet,
                    Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase,
                    effectiveReplicaReplicationBearerToken);
            replicasToStopReplicating.add(additionalReplicaStarted);
        } else {
            additionalReplicaStarted = null;
        }
        logger.info("Stopping replication for replica set "+replicaSet.getName());
        for (final SailingAnalyticsProcess<String> replica : replicasToStopReplicating) {
            logger.info("...asking replica "+replica+" to stop replication");
            try {
                replica.stopReplicatingFromMaster(effectiveReplicaReplicationBearerToken, Landscape.WAIT_FOR_PROCESS_TIMEOUT);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Telling replica "+replica+" to stop replicating from master didn't work; assuming it's dead; continuing...", e);
            }
        }
        logger.info("Done stopping replication. Removing master "+replicaSet.getMaster()+" from target groups "+
                replicaSet.getPublicTargetGroup()+" and "+replicaSet.getMasterTargetGroup());
        replicaSet.getPublicTargetGroup().removeTarget(replicaSet.getMaster().getHost());
        replicaSet.getMasterTargetGroup().removeTarget(replicaSet.getMaster().getHost());
        replicaSet.getOtherTargetGroups().forEach(tg->tg.removeTarget(replicaSet.getMaster().getHost()));
        sendMailAboutMasterUnavailable(replicaSet);
        return additionalReplicaStarted;
    }

    /**
     * For the {@code replicaSet}, find out how a replica can be spun up.
     * <ul>
     * <li>If there is an
     * {@link AwsApplicationReplicaSet#getAutoScalingGroup() auto-scaling group} in place, ensure that
     * its {@link AutoScalingGroup#minSize() minimum size} is at least one, then wait for a replica
     * to show up and become healthy.</li>
     * <li>Without an auto-scaling group, configure and run a {@link StartSailingAnalyticsReplicaHost} procedure
     * and wait for its {@link StartSailingAnalyticsReplicaHost#getHost()} to become healthy, then
     * {@link TargetGroup#addTarget(AwsInstance) add} the replica to the public target group.</li>
     * </ul>
     * 
     * @return the replica launched
     */
    private SailingAnalyticsProcess<String> launchReplicaAndWaitUntilHealthy(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicationBearerToken)
            throws Exception {
        final SailingAnalyticsProcess<String> spunUpReplica;
        if (replicaSet.getAutoScalingGroup() != null) {
            spunUpReplica = spinUpReplicaByIncreasingAutoScalingGroupMinSize(replicaSet.getAutoScalingGroup(), replicaSet.getMaster());
        } else {
            final Integer optionalIgtimiRiotPort = replicaSet.getMaster().getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
            spunUpReplica = spinUpReplicaAndRegisterInPublicTargetGroup(replicaSet, optionalKeyName,
                    privateKeyEncryptionPassphrase, replicationBearerToken, optionalIgtimiRiotPort);
        }
        // bug6083 comment #6: Note that the following won't happen when the auto-scaling group automatically reacts to scaling events
        replicaSet.getOtherTargetGroups().forEach(tg->tg.addTarget(spunUpReplica.getHost()));
        return spunUpReplica;
    }

    private SailingAnalyticsProcess<String> spinUpReplicaAndRegisterInPublicTargetGroup(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicationBearerToken, Integer optionalIgtimiRiotPort) throws Exception {
        final AwsRegion region = replicaSet.getMaster().getHost().getRegion();
        return spinUpReplicaAndRegisterInPublicTargetGroup(region, replicaSet, optionalKeyName, privateKeyEncryptionPassphrase, replicationBearerToken,
                optionalIgtimiRiotPort, /* processLauncher: */ replicaConfigurationBuilder->{
                    // the process launcher determines the master's instance type and launches a host of the same type using the replica configuration for the process config
                    final InstanceType masterInstanceType = replicaSet.getMaster().getHost().getInstance().instanceType();
                    final com.sap.sailing.landscape.procedures.StartSailingAnalyticsReplicaHost.Builder<?, String> replicaHostBuilder = StartSailingAnalyticsReplicaHost.replicaHostBuilder(replicaConfigurationBuilder);
                    replicaHostBuilder
                        .setInstanceType(masterInstanceType)
                        .setOptionalTimeout(Landscape.WAIT_FOR_HOST_TIMEOUT)
                        .setLandscape(getLandscape())
                        .setRegion(region)
                        .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase);
                    optionalKeyName.ifPresent(keyName->replicaHostBuilder.setKeyName(keyName));
                    try {
                        final StartSailingAnalyticsReplicaHost<String> replicaHostStartProcedure = replicaHostBuilder.build();
                        replicaHostStartProcedure.run();
                        return replicaHostStartProcedure.getSailingAnalyticsProcess();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    
    /**
     * Launches a replica process for a given replica set and adds it to the replica set's public target group. The way
     * the replica process is launched is provided as a {@link Function} in the ({@code processLauncher} parameter). The
     * function may, e.g., use the replica process configuration to launch a new host which automatically deploys a
     * replica process according to this configuration, or the function may for example use the replica process
     * configuration to parameterize a process deployment to an existing host (see the
     * {@link DeployProcessOnMultiServer} procedure).
     * 
     * @param optionalIgtimiRiotPort
     *            if non-{@code null}, used to configure the
     *            {@link SailingProcessConfigurationVariables#IGTIMI_RIOT_PORT} variable of the replica launched
     * 
     * @return the process that the {@code processLauncher} started
     */
    private <AppConfigBuilderT extends SailingAnalyticsReplicaConfiguration.Builder<AppConfigBuilderT, String>>
    SailingAnalyticsProcess<String> spinUpReplicaAndRegisterInPublicTargetGroup(
            AwsRegion region,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicationBearerToken,
            Integer optionalIgtimiRiotPort, Function<AppConfigBuilderT, SailingAnalyticsProcess<String>> processLauncher) throws Exception {
        final Release release = replicaSet.getVersion(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
        final AppConfigBuilderT replicaConfigurationBuilder =
                createReplicaConfigurationBuilder(region, replicaSet.getServerName(), replicaSet.getMaster().getPort(),
                        optionalIgtimiRiotPort, release, replicationBearerToken, replicaSet.getHostname());
        final SailingAnalyticsProcess<String> sailingAnalyticsProcess = processLauncher.apply(replicaConfigurationBuilder);
        waitUntilHealthyAndThenRegisterReplicaInPublicTargetGroup(sailingAnalyticsProcess, replicaSet);
        return sailingAnalyticsProcess;
    }
    
    /**
     * Waits for the process to become ready (see {@link SailingAnalyticsProcess#waitUntilReady(Optional)}),
     * then registers its {@link SailingAnalyticsProcess#getHost() host} with the {@code replicaSet}'s
     * {@link AwsApplicationReplicaSet#getPublicTargetGroup() public target group}.
     */
    private void waitUntilHealthyAndThenRegisterReplicaInPublicTargetGroup(
            final SailingAnalyticsProcess<String> sailingAnalyticsProcess,
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws TimeoutException, Exception {
        sailingAnalyticsProcess.waitUntilReady(Landscape.WAIT_FOR_HOST_TIMEOUT);
        if (replicaSet.getPublicTargetGroup() != null) {
            replicaSet.getPublicTargetGroup().addTarget(sailingAnalyticsProcess.getHost());
        }
        replicaSet.getOtherTargetGroups().forEach(tg->tg.addTarget(sailingAnalyticsProcess.getHost()));
    }

    private SailingAnalyticsProcess<String> spinUpReplicaByIncreasingAutoScalingGroupMinSize(
            AwsAutoScalingGroup autoScalingGroup,
            SailingAnalyticsProcess<String> master)
            throws TimeoutException, Exception {
        if (autoScalingGroup.getAutoScalingGroup().minSize() < 1) {
            getLandscape().updateAutoScalingGroupMinSize(autoScalingGroup, 1);
        }
        return Wait.wait(()->hasHealthyAutoScalingReplica(master, autoScalingGroup), healthyReplica->healthyReplica != null,
                /* retryOnException */ true,
                Landscape.WAIT_FOR_HOST_TIMEOUT, Duration.ONE_SECOND.times(5), Level.INFO,
                "Waiting for auto-scaling group "+autoScalingGroup.getName()+" to produce healthy replica");
    }

    /**
     * Returns one replica process managed by {@code autoScalingGroup} that is healthy, or {@code null} if no such process was found
     */
    private SailingAnalyticsProcess<String> hasHealthyAutoScalingReplica(SailingAnalyticsProcess<String> master, AwsAutoScalingGroup autoScalingGroup) throws Exception {
        final HostSupplier<String, SailingAnalyticsHost<String>> hostSupplier = new SailingAnalyticsHostSupplier<>();
        for (final SailingAnalyticsProcess<String> replica : master.getReplicas(Landscape.WAIT_FOR_HOST_TIMEOUT, hostSupplier, processFactoryFromHostAndServerDirectory)) {
            if (replica.getHost().isManagedByAutoScalingGroup(Collections.singleton(autoScalingGroup)) && replica.isReady(Landscape.WAIT_FOR_HOST_TIMEOUT)) {
                return replica;
            }
        }
        return null;
    }

    @Override
    public <BuilderT extends StartMultiServer.Builder<BuilderT, String>> SailingAnalyticsHost<String> createEmptyMultiServer(AwsRegion region, Optional<InstanceType> instanceType,
            Optional<AwsAvailabilityZone> availabilityZone, Optional<String> name, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final StartMultiServer.Builder<BuilderT, String> startMultiServerProcedureBuilder = StartMultiServer.<BuilderT, String>builder();
        startMultiServerProcedureBuilder
            .setLandscape(getLandscape())
            .setRegion(region);
        instanceType.ifPresent(it->startMultiServerProcedureBuilder.setInstanceType(it));
        availabilityZone.ifPresent(az->startMultiServerProcedureBuilder.setAvailabilityZone(az));
        optionalKeyName.ifPresent(keyName->startMultiServerProcedureBuilder.setKeyName(keyName));
        if (privateKeyEncryptionPassphrase != null) {
            startMultiServerProcedureBuilder.setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase);
        }
        name.ifPresent(nameTag->startMultiServerProcedureBuilder.setInstanceName(nameTag));
        final StartMultiServer<String> startMultiServerProcedure = startMultiServerProcedureBuilder.build();
        startMultiServerProcedure.run();
        return Wait.wait(() -> startMultiServerProcedure.getHost(), multiServer -> {
            try {
                return multiServer.isReady(optionalKeyName, privateKeyEncryptionPassphrase);
            } catch (Exception e) {
                return false;
            }
        }, true, Landscape.WAIT_FOR_HOST_TIMEOUT, /* sleepBetweenAttempts */ Duration.ONE_SECOND.times(10), Level.INFO, "Waiting until host "+startMultiServerProcedure.getHost().getId()+" is ready");
    }

    @Override
    public Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> updateImageForReplicaSets(AwsRegion region,
            Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> replicaSets,
            Optional<AmazonMachineImage<String>> optionalAmi, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws InterruptedException, ExecutionException, TimeoutException {
        final Set<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> result = new HashSet<>();
        for (final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet : replicaSets) {
            if (replicaSet.getAutoScalingGroup() != null) {
                final AmazonMachineImage<String> ami = optionalAmi.orElseGet(
                        ()->getLandscape().getLatestImageWithType(region, SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING));
                logger.info("Upgrading AMI in auto-scaling groups "+Util.join(", ", replicaSet.getAllAutoScalingGroups())+" of replica set "+replicaSet.getName()+" to "+ami.getId());
                getLandscape().updateImageInAutoScalingGroups(region, replicaSet.getAllAutoScalingGroups(), replicaSet.getName(), ami);
                result.add(getLandscape().getApplicationReplicaSet(region, replicaSet.getServerName(), replicaSet.getMaster(), replicaSet.getReplicas(),
                        optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase));
            } else {
                logger.info("No auto-scaling group found for replica set "+replicaSet.getName()+" to update AMI in");
            }
        }
        return result;
    }

    @Override
    public <AppConfigBuilderT extends Builder<AppConfigBuilderT, String>,
            MultiServerDeployerBuilderT extends com.sap.sailing.landscape.procedures.DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT>>
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> useDedicatedAutoScalingReplicasInsteadOfShared(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result;
        if (replicaSet.getAutoScalingGroup() != null) {
            final Integer minSize = replicaSet.getAutoScalingGroup().getAutoScalingGroup().minSize();
            if (minSize != null && minSize > 0) {
                logger.info("Replica set "+replicaSet+" already has its auto-scaling group minimum size set to a non-zero value: "+minSize);
                result = replicaSet;
            } else {
                final SailingAnalyticsProcess<String> replica = spinUpReplicaByIncreasingAutoScalingGroupMinSize(replicaSet.getAutoScalingGroup(), replicaSet.getMaster());
                // bug6083 comment #6: this registration with other target groups won't happen when another scaling event triggers the launch
                replicaSet.getOtherTargetGroups().forEach(tg->tg.addTarget(replica.getHost()));
                assert replica.isReady(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
                for (final SailingAnalyticsProcess<String> nonAutoScalingReplica : replicaSet.getReplicas()) {
                    if (!nonAutoScalingReplica.getHost().isManagedByAutoScalingGroup()) {
                        logger.info("Found replica "+nonAutoScalingReplica+" to be not managed by auto-scaling group "+replicaSet.getAutoScalingGroup().getName()+
                                ". Removing it from Target Group and stopping it...");
                        replicaSet.getPublicTargetGroup().removeTarget(nonAutoScalingReplica.getHost());
                        nonAutoScalingReplica.stopAndTerminateIfLast(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                    }
                }
                result = getLandscape().getApplicationReplicaSet(replicaSet.getMaster().getHost().getRegion(), replicaSet.getServerName(), replicaSet.getMaster(),
                        Collections.singleton(replica), Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
            }
        } else {
            logger.warning("No auto-scaling group found for replica set "+replicaSet+"; not terminating any replicas.");
            result = null;
        }
        return result;
    }


    @Override
    public <AppConfigBuilderT extends Builder<AppConfigBuilderT, String>,
            MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT>>
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> useSingleSharedInsteadOfDedicatedAutoScalingReplica(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String replicaReplicationBearerToken, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Optional<InstanceType> optionalInstanceType) throws Exception {
        final AwsAutoScalingGroup autoScalingGroup = replicaSet.getAutoScalingGroup();
        final Set<SailingAnalyticsProcess<String>> nonAutoScalingReplica = new HashSet<>();
        for (final SailingAnalyticsProcess<String> replica : replicaSet.getReplicas()) {
            if (autoScalingGroup == null || !replica.getHost().isManagedByAutoScalingGroup(Collections.singleton(autoScalingGroup))) {
                logger.info("Found replica "+replica+" in replica set "+replicaSet.getName()+
                        " which is not managed by auto-scaling group");
                nonAutoScalingReplica.add(replica);
            }
        }
        if (nonAutoScalingReplica.isEmpty()) {
            logger.info("No replica found for replica set "+replicaSet.getName()+
                    " that is not managed by auto-scaling group "+(autoScalingGroup==null?"null":autoScalingGroup.getName())+
                    ". Launching one on an eligible shared instance.");
            final Integer optionalIgtimiRiotPort = replicaSet.getMaster().getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
            nonAutoScalingReplica.add(launchUnmanagedReplica(replicaSet, replicaSet.getMaster().getHost().getRegion(), optionalKeyName,
                    privateKeyEncryptionPassphrase, getEffectiveBearerToken(replicaReplicationBearerToken), optionalMemoryInMegabytesOrNull,
                    optionalMemoryTotalSizeFactorOrNull,
                    optionalIgtimiRiotPort,
                    Optional.of(optionalInstanceType.orElseGet(()->replicaSet.getMaster().getHost().getInstance().instanceType())), /* optionalPreferredInstanceToDeployTo */ Optional.empty()));
        }
        if (autoScalingGroup != null) {
            logger.info("Scaling down auto-scaling group for replica set "+replicaSet.getName()+" from minimum size "+
                    replicaSet.getAutoScalingGroup().getAutoScalingGroup().minSize()+" to 0");
            getLandscape().updateAutoScalingGroupMinSize(replicaSet.getAutoScalingGroup(), 0);
        } else {
            logger.info("No auto-scaling group found for replica set "+replicaSet.getName()+"; nothing to scale down.");
        }
        return getLandscape().getApplicationReplicaSet(replicaSet.getMaster().getHost().getRegion(), replicaSet.getServerName(),
                replicaSet.getMaster(), nonAutoScalingReplica, Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
    }
    
    @Override
    public <AppConfigBuilderT extends SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT>>
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> moveMasterToOtherInstance(
                    final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
                    final boolean useSharedInstance, Optional<InstanceType> optionalInstanceType,
                    Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo, String optionalKeyName,
                    byte[] privateKeyEncryptionPassphrase, String optionalMasterReplicationBearerTokenOrNull, final String optionalReplicaReplicationBearerTokenOrNull, Integer optionalMemoryInMegabytesOrNull,
                    Integer optionalMemoryTotalSizeFactorOrNull)
                    throws MalformedURLException,
                    IOException, TimeoutException, InterruptedException, ExecutionException, Exception {
        final Integer igtimiRiotPort = replicaSet.getMaster().getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        if (replicaSet.isLocalReplicaSet()) {
            throw new IllegalArgumentException("A replica set cannot move its own master process. Current replica set: "+ServerInfo.getName());
        }
        final SailingAnalyticsProcess<String> newTemporaryReplica = ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(replicaSet,
                optionalKeyName, privateKeyEncryptionPassphrase,
                getEffectiveBearerToken(optionalReplicaReplicationBearerTokenOrNull));
        // important to obtain the release before stopping master:
        final Release release = replicaSet.getVersion(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        final AwsRegion region = replicaSet.getMaster().getHost().getRegion();
        SailingAnalyticsHost<String> hostToDeployTo = null;
        if (useSharedInstance) {
            // determine new shared host before stopping old master; we want to *move* and not end up on the same instance again
            hostToDeployTo = new EligibleInstanceForReplicaSetFindingStrategyImpl(this,
                    region, optionalKeyName, privateKeyEncryptionPassphrase,
                    /* master */ true, /* mustBeDifferentAvailabilityZone */ true, optionalInstanceType,
                    optionalPreferredInstanceToDeployTo).getInstanceToDeployTo(replicaSet);
        }
        logger.info("Stopping master "+replicaSet.getMaster());
        replicaSet.getMaster().stopAndTerminateIfLast(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        final AppConfigBuilderT masterConfigurationBuilder = createMasterConfigurationBuilder(replicaSet.getName(),
                optionalMasterReplicationBearerTokenOrNull, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull,
                igtimiRiotPort, region, release);
        masterConfigurationBuilder.setPort(replicaSet.getPort()); // master must run on same port as the rest of the replica set
        final SailingAnalyticsProcess<String> newMaster;
        if (useSharedInstance) {
            assert hostToDeployTo != null;
            logger.info("Launching new master on shared instance "+hostToDeployTo);
            newMaster = deployProcessToSharedInstance(hostToDeployTo, masterConfigurationBuilder, optionalKeyName, privateKeyEncryptionPassphrase);
        } else {
            assert hostToDeployTo == null;
            final com.sap.sailing.landscape.procedures.StartSailingAnalyticsMasterHost.Builder<?, String> masterHostBuilder = StartSailingAnalyticsMasterHost.masterHostBuilder(masterConfigurationBuilder);
            masterHostBuilder
                .setInstanceType(optionalInstanceType.orElse(InstanceType.valueOf(SharedLandscapeConstants.DEFAULT_DEDICATED_INSTANCE_TYPE_NAME)))
                .setOptionalTimeout(Landscape.WAIT_FOR_HOST_TIMEOUT)
                .setLandscape(getLandscape())
                .setRegion(region)
                .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase);
            if (optionalKeyName != null) {
                masterHostBuilder.setKeyName(optionalKeyName);
            }
            final StartSailingAnalyticsMasterHost<String> masterHostStartProcedure = masterHostBuilder.build();
            masterHostStartProcedure.run();
            hostToDeployTo = masterHostStartProcedure.getHost();
            logger.info("Launched dedicated instance for master: "+hostToDeployTo);
            newMaster = masterHostStartProcedure.getSailingAnalyticsProcess();
        }
        newMaster.waitUntilReady(Landscape.WAIT_FOR_HOST_TIMEOUT);
        logger.info("Adding new master "+newMaster+" to target groups");
        replicaSet.getPublicTargetGroup().addTarget(hostToDeployTo);
        replicaSet.getMasterTargetGroup().addTarget(hostToDeployTo);
        final SailingAnalyticsHost<String> finalHostToDeployTo = hostToDeployTo;
        replicaSet.getOtherTargetGroups().forEach(tg->tg.addTarget(finalHostToDeployTo));
        sendMailAboutMasterAvailable(replicaSet);
        if (newTemporaryReplica != null) {
            newTemporaryReplica.stopAndTerminateIfLast(Landscape.WAIT_FOR_HOST_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        }
        // here, although we're waiting for a process to become healthy, that process may need to fetch a large
        // initial load before reaching the healthy state, so we'll use the longer HOST timeout here:
        replicaSet.restartAllReplicas(Landscape.WAIT_FOR_HOST_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        return getLandscape().getApplicationReplicaSet(region, replicaSet.getServerName(), newMaster, replicaSet.getReplicas(),
                Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
    }

    private void sendMailAboutMasterUnavailable(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws MailException {
        sendMailToReplicaSetOwner(replicaSet, "MasterUnavailableMailSubject", "MasterUnavailableMailBody", Optional.of(ServerActions.CONFIGURE_LOCAL_SERVER));
    }

    private void sendMailAboutMasterAvailable(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws MailException {
        sendMailToReplicaSetOwner(replicaSet, "MasterAvailableMailSubject", "MasterAvailableMailBody", Optional.of(ServerActions.CONFIGURE_LOCAL_SERVER));
    }
    
    /**
     * @param subjectMessageKey
     *            must have a single placeholder argument representing the name of the replica set
     * @param bodyMessageKey
     *            must have a single placeholder argument representing the name of the replica set
     * @param alsoSendToAllUsersWithThisPermissionOnReplicaSet
     *            when not empty, all users that have permission to this {@link SecuredSecurityTypes#SERVER SERVER}
     *            action on the {@code replicaSet} will receive the e-mail in addition to the server owner. No user
     *            will receive the e-mail twice.
     */
    private void sendMailToReplicaSetOwner(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            final String subjectMessageKey, final String bodyMessageKey, Optional<Action> alsoSendToAllUsersWithThisPermissionOnReplicaSet) throws MailException {
        final Iterable<User> usersToSendMailTo = getSecurityService().getUsersToInformAboutReplicaSet(replicaSet.getServerName(), alsoSendToAllUsersWithThisPermissionOnReplicaSet);
        final ResourceBundleStringMessages stringMessages = new ResourceBundleStringMessagesImpl(STRING_MESSAGES_BASE_NAME, getClass().getClassLoader(), StandardCharsets.UTF_8.name());
        for (final User user : usersToSendMailTo) {
            final String subject = stringMessages.get(user.getLocaleOrDefault(), subjectMessageKey, replicaSet.getServerName());
            final String body = stringMessages.get(user.getLocaleOrDefault(), bodyMessageKey, replicaSet.getServerName());
            if (user.isEmailValidated()) {
                getSecurityService().sendMail(user.getName(),
                        subject,
                        body);
            } else {
                logger.warning("Not sending e-mail with subject "+subject+" to user "+user.getName()+
                        " with e-mail address "+user.getEmail()+" because e-mail address has not been validated");
            }
        }
    }

    /**
     * If a non-{@code null}, non-{@link String#isEmpty() empty} bearer token is provided by the
     * {@code optionalBearerTokenOnNull} parameter, it is returned unchanged; otherwise, the bearer token as obtained
     * for the current session's principal is returned. See also {@link SecurityService#getOrCreateAccessToken(String)}.
     */
    @Override
    public String getEffectiveBearerToken(String optionalBearerTokenOrNull) {
        return Util.hasLength(optionalBearerTokenOrNull) ? optionalBearerTokenOrNull :
            getSecurityService().getOrCreateAccessToken(SessionUtils.getPrincipal().toString());
    }
    
    @Override
    public AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> changeAutoScalingReplicasInstanceType(
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            InstanceType instanceType, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result;
        final Iterable<AwsAutoScalingGroup> autoScalingGroups = replicaSet.getAllAutoScalingGroups();
        if (!Util.isEmpty(autoScalingGroups)) {
            // Don't trust the replicas passed in by the client; it may be stale. Instead, obtain a new set of replicas from the master:
            final Iterable<SailingAnalyticsProcess<String>> oldReplicas = replicaSet.getMaster().getReplicas(
                    Landscape.WAIT_FOR_PROCESS_TIMEOUT, new SailingAnalyticsHostSupplier<String>(),
                    processFactoryFromHostAndServerDirectory);
            Iterable<SailingAnalyticsProcess<String>> newSetOfAllReplicas = oldReplicas;
            final Set<SailingAnalyticsProcess<String>> terminatedReplicas = new HashSet<>();
            getLandscape().updateInstanceTypeInAutoScalingGroup(replicaSet.getMaster().getHost().getRegion(), autoScalingGroups, replicaSet.getName(), instanceType);
            for (final AwsAutoScalingGroup autoScalingGroup : autoScalingGroups) {
                logger.info("Rolling upgrade of instances for auto-scaling group "+autoScalingGroup.getName()+" to new instance type"+instanceType);
                final int oldMinSize = autoScalingGroup.getAutoScalingGroup().minSize();
                final int newMinSize = autoScalingGroup.getAutoScalingGroup().desiredCapacity() + 1;
                getLandscape().updateAutoScalingGroupMinSize(autoScalingGroup, newMinSize);
                for (final SailingAnalyticsProcess<String> replica : oldReplicas) {
                    final SailingAnalyticsHost<String> replicaHost = replica.getHost();
                    if (replicaHost.isManagedByAutoScalingGroup(Collections.singleton(autoScalingGroup))) {
                        logger.info("Replica "+replica+" is managed by auto-scaling group "+autoScalingGroup.getName());
                        newSetOfAllReplicas = waitUntilAtLeastSoManyAutoScalingReplicasAreReady(replicaSet, autoScalingGroup, newMinSize);
                        getLandscape().terminate(replicaHost);
                        terminatedReplicas.add(replica);
                    }
                }
                getLandscape().updateAutoScalingGroupMinSize(autoScalingGroup, oldMinSize);
            }
            // determine new replicas, without the terminated ones...
            final Iterable<SailingAnalyticsProcess<String>> newReplicasWithoutTerminated =  
                    Util.filter(newSetOfAllReplicas, r->!Util.contains(terminatedReplicas, r));
            // ...and add them to any other target groups the instances of the replica set have to be added to
            newReplicasWithoutTerminated.forEach(replica->{
                try {
                    replicaSet.getOtherTargetGroups().forEach(tg->tg.addTarget(replica.getHost()));
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            result = getLandscape().getApplicationReplicaSet(replicaSet.getMaster().getHost().getRegion(),
                    replicaSet.getServerName(), replicaSet.getMaster(), newReplicasWithoutTerminated,
                    optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        } else {
            logger.info("Replica set "+replicaSet.getName()+
                    " does not have an auto-scaling group configured, so no changes can be made to its launch template.");
            result = replicaSet;
        }
        return result;
    }

    private Iterable<SailingAnalyticsProcess<String>> waitUntilAtLeastSoManyAutoScalingReplicasAreReady(
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            AwsAutoScalingGroup autoScalingGroup, final int newMinSize) throws Exception {
        final SailingAnalyticsProcess<String> master = replicaSet.getMaster();
        assert autoScalingGroup != null;
        final Set<SailingAnalyticsProcess<String>> replicas = new HashSet<>(); 
        if (Wait.wait(()->{
            int readyAutoScalingReplicas = 0;
            try {
                replicas.clear();
                Util.addAll(master.getReplicas(
                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, new SailingAnalyticsHostSupplier<String>(),
                        processFactoryFromHostAndServerDirectory), replicas);
                for (final SailingAnalyticsProcess<String> replica : replicas) {
                    if (replica.getHost().isManagedByAutoScalingGroup(Collections.singleton(autoScalingGroup))) {
                        if (replica.waitUntilReady(Landscape.WAIT_FOR_PROCESS_TIMEOUT)) {
                            readyAutoScalingReplicas++;
                            logger.info("Replica "+replica+" is ready; found "+readyAutoScalingReplicas+"/"+newMinSize+" so far");
                            if (readyAutoScalingReplicas >= newMinSize) {
                                break;
                            }
                        } else {
                            logger.info("Replica "+replica+" NOT ready; still found only "+readyAutoScalingReplicas+"/"+newMinSize+" so far");
                        }
                    }
                }
            } catch (TimeoutException timeoutException) {
                logger.info("Timeout looking for replicas: "+timeoutException);
            }
            return readyAutoScalingReplicas >= newMinSize;
        }, Landscape.WAIT_FOR_HOST_TIMEOUT, /* duration between attempts */ Duration.ONE_SECOND.times(30),
                    Level.INFO, "Waiting until at least "+newMinSize+" auto-scaling replicas for replica set "+replicaSet.getName()+" are ready")) {
            return replicas;
        } else {
            throw new TimeoutException("Could determine set of ready auto-scaling replicas of replica set "+
                    replicaSet.getName()+" within timeout period "+Landscape.WAIT_FOR_HOST_TIMEOUT);
        }
    }

    /**
     * Checks whether the {@code host} is eligible for deploying a process of an application replica set named as
     * defined by {@code serverName}, listening on the {@link SailingAnalyticsProcess#getPort() application port}
     * specified by {@code port}.
     */
    @Override
    public <ShardingKey> boolean isEligibleForDeployment(SailingAnalyticsHost<ShardingKey> host, String serverName, int port, Integer optionalIgtimiRiotPort,
            Optional<Duration> optionalTimeout, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        boolean result;
        if (host.isManagedByAutoScalingGroup()) {
            result = false;
        } else {
            result = true;
            final Iterable<SailingAnalyticsProcess<ShardingKey>> applicationProcesses = host.getApplicationProcesses(optionalTimeout,
                    Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
            for (final SailingAnalyticsProcess<ShardingKey> applicationProcess : applicationProcesses) {
                if (applicationProcess.getPort() == port || applicationProcess.getServerName(optionalTimeout, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase).equals(serverName)
                        // also, if an Igtimi Riot port is specified, it must not be used as some other process's Riot port:
                        || (optionalIgtimiRiotPort != null && Util.equalsWithNull(optionalIgtimiRiotPort, applicationProcess.getIgtimiRiotPort(optionalTimeout, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase)))) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
    @Override
    public SailingServer getSailingServer(String hostname, String username, String password, Optional<Integer> port)
            throws MalformedURLException {
        final SailingServerFactory fac = sailingServerFactoryTracker.getService();
        return fac.getSailingServer(RemoteServerUtil.getBaseServerUrl(hostname,
                port.isPresent() ? port.get() : /* defaults to HTTPS */ 443), username, password);
    }

    @Override
    public SailingServer getSailingServer(String hostname, String bearerToken, Optional<Integer> port)
            throws MalformedURLException {
        final SailingServerFactory fac = sailingServerFactoryTracker.getService();
        return fac.getSailingServer(RemoteServerUtil.getBaseServerUrl(hostname,
                port.orElse(443 /* defaults to HTTPS */)), bearerToken);
    }

    private <BuilderT extends CreateShard.Builder<BuilderT, CreateShard<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> com.sap.sse.landscape.aws.orchestration.CreateShard.Builder<BuilderT, CreateShard<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createShardBuilder() {
        return CreateShard.<SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, BuilderT, String> builder();
    }

    private <BuilderT extends ShardProcedure.Builder<BuilderT, AddShardingKeyToShard<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> com.sap.sse.landscape.aws.orchestration.ShardProcedure.Builder<BuilderT, AddShardingKeyToShard<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> appendShardingKeyToShardBuilder() {
        return AddShardingKeyToShard
                .<SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, BuilderT, String> builder();
    }

    private <BuilderT extends ShardProcedure.Builder<BuilderT, RemoveShardingKeyFromShard<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> com.sap.sse.landscape.aws.orchestration.ShardProcedure.Builder<BuilderT, RemoveShardingKeyFromShard<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> removeShardingKeyFromShardBuilder() {
        return RemoveShardingKeyFromShard
                .<SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, BuilderT, String> builder();
    }

    @Override
    public void removeShardingKeysFromShard(Iterable<String> selectedleaderboards,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            AwsRegion region, String shardName, String bearerToken)
            throws Exception {
        final SailingServer server = getSailingServer(applicationReplicaSet.getHostname(), bearerToken,
                /* HTTPS port */ Optional.of(443));
        Set<String> shardingKeys = new HashSet<>();
        for (String leaderboardName : selectedleaderboards) {
            shardingKeys.add(server.getLeaderboardShardingKey(leaderboardName));
        }
        removeShardingKeyFromShardBuilder()
            .setLandscape(getLandscape())
            .setRegion(region)
            .setPathPrefixForShardingKey(RemoteServiceMappingConstants.pathPrefixForShardingKey)
            .setShardingKeys(shardingKeys)
            .setReplicaSet(applicationReplicaSet)
            .setShardName(shardName)
            .build()
            .run();
    }

    @Override
    public void appendShardingKeysToShard(Iterable<String> selectedLeaderboards,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            AwsRegion region, String shardName, String bearerToken)
            throws Exception {
        final SailingServer server = getSailingServer(applicationReplicaSet.getHostname(), bearerToken,
                /* HTTPS port */ Optional.of(443));
        final Set<String> shardingkeys = new HashSet<String>();
        for (String s : selectedLeaderboards) {
            shardingkeys.add(server.getLeaderboardShardingKey(s));
        }
        appendShardingKeyToShardBuilder()
            .setLandscape(getLandscape())
            .setRegion(region)
            .setPathPrefixForShardingKey(RemoteServiceMappingConstants.pathPrefixForShardingKey)
            .setShardingKeys(shardingkeys)
            .setReplicaSet(applicationReplicaSet)
            .setShardName(shardName)
            .build()
            .run();
    }

    @Override
    public void removeShard(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            String shardTargetGroupArn) throws Exception {
        for (Entry<AwsShard<String>, Iterable<String>> entry : applicationReplicaSet.getShards().entrySet()) {
            if (shardTargetGroupArn.equals(entry.getKey().getTargetGroup().getTargetGroupArn())) {
                applicationReplicaSet.removeShard(entry.getKey(), getLandscape());
            }
        }
    }

    @Override
    public void addShard(Iterable<String> selectedLeaderboardNames,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            AwsRegion region, String bearerToken, String shardName)
            throws Exception {
        final SailingServer server = getSailingServer(applicationReplicaSet.getHostname(), bearerToken,
                Optional.of(443));
        final Set<String> shardingkeys = new HashSet<String>();
        for (final String s : selectedLeaderboardNames) {
            shardingkeys.add(server.getLeaderboardShardingKey(s));
        }
        createShardBuilder()
            .setLandscape(getLandscape())
            .setTargetGroupNamePrefix(LandscapeService.SAILING_TARGET_GROUP_NAME_PREFIX)
            .setShardingKeys(shardingkeys)
            .setReplicaSet(applicationReplicaSet)
            .setRegion(region)
            .setPathPrefixForShardingKey(RemoteServiceMappingConstants.pathPrefixForShardingKey)
            .setShardName(shardName)
            .build()
            .run();
    }
    
    @Override
    public Triple<SailingAnalyticsHost<String>, Map<String, SailingAnalyticsProcess<String>>, Map<String, SailingAnalyticsProcess<String>>>
    moveAllApplicationProcessesAwayFrom(SailingAnalyticsHost<String> host,
            Optional<InstanceType> optionalInstanceTypeForNewInstance,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        if (!host.getInstance().tags().stream().anyMatch(tag->
                tag.key().equals(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG) &&
                tag.value().equals(SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE))) {
            throw new IllegalArgumentException("Host "+host+" is not tagged as multiserver. The host is expected to have value "+
                    SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE+" for tag "+
                    SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG);
        }
        logger.info("Moving all application processes from shared host "+host+" to a newly started shared host.");
        final AwsRegion region = host.getRegion();
        final HostSupplier<String, SailingAnalyticsHost<String>> hostSupplier = new SailingAnalyticsHostSupplier<>();
        final SailingAnalyticsHost<String> targetHost = createEmptyMultiServer(region,
                Optional.of(optionalInstanceTypeForNewInstance.orElse(host.getInstanceType())),
                Optional.of(host.getAvailabilityZone()),
                Optional.of(SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_DEFAULT_NAME),
                Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        final Map<String, SailingAnalyticsProcess<String>> masterProcessesMoved = new HashMap<>();
        final Map<String, SailingAnalyticsProcess<String>> replicaProcessesMoved = new HashMap<>();
        for (final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet : getLandscape()
                .getApplicationReplicaSetsByTag(region, SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG,
                        hostSupplier, Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName),
                        privateKeyEncryptionPassphrase)) {
            if (replicaSet.getMaster().getHost().getId().equals(host.getId())) {
                // We're moving a master process:
                logger.info("Found master process "+replicaSet.getMaster()+" on host "+host+" to move to "+targetHost);
                masterProcessesMoved.put(replicaSet.getName(), replicaSet.getMaster());
                // Try to obtain the replicas' replication bearer token from a replica; if no replica is found in the
                // application replica set, use the master's replication token as a default
                final String replicaReplicationBearerToken = Util.stream(replicaSet.getReplicas()).findAny()
                        .map(replica -> {
                            try {
                                return replica.getEnvShValueFor(
                                        DefaultProcessConfigurationVariables.REPLICATE_MASTER_BEARER_TOKEN,
                                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName),
                                        privateKeyEncryptionPassphrase);
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Error trying to obtain environment variable value for "+
                                        DefaultProcessConfigurationVariables.REPLICATE_MASTER_BEARER_TOKEN+" from host "+replica, e);
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse(replicaSet.getMaster().getEnvShValueFor(
                                DefaultProcessConfigurationVariables.REPLICATE_MASTER_BEARER_TOKEN,
                                Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName),
                                privateKeyEncryptionPassphrase));
                final Integer totalMemorySizeFactor = getTotalMemorySizeFactor(optionalKeyName, privateKeyEncryptionPassphrase, replicaSet.getMaster());
                moveMasterToOtherInstance(replicaSet, /* useSharedInstance */ true,
                        /* optionalInstanceType */ Optional.empty(), Optional.of(targetHost), optionalKeyName,
                        privateKeyEncryptionPassphrase,
                        /* optionalMasterReplicationBearerTokenOrNull */ replicaSet.getMaster().getEnvShValueFor(
                                DefaultProcessConfigurationVariables.REPLICATE_MASTER_BEARER_TOKEN,
                                Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName),
                                privateKeyEncryptionPassphrase),
                        replicaReplicationBearerToken,
                        totalMemorySizeFactor == null ? getMemoryInMegabytes(optionalKeyName, privateKeyEncryptionPassphrase, replicaSet.getMaster()) : null,
                        totalMemorySizeFactor); 
                logger.info("Done moving master of "+replicaSet.getName()+" from "+host+" to "+targetHost);
            } else {
                final SailingAnalyticsProcess<String> replica;
                if ((replica = Util.stream(replicaSet.getReplicas()).filter(r->r.getHost().getId().equals(host.getId())).findFirst().orElse(null)) != null) {
                    // We're moving a replica process:
                    logger.info("Found replica process "+replica+" on host "+host+" to move to "+targetHost);
                    final String replicaReplicationBearerToken = replica.getEnvShValueFor(
                            DefaultProcessConfigurationVariables.REPLICATE_MASTER_BEARER_TOKEN,
                            Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName),
                            privateKeyEncryptionPassphrase);
                    replicaProcessesMoved.put(replicaSet.getName(), replica);
                    final Integer totalMemorySizeFactor = getTotalMemorySizeFactor(optionalKeyName, privateKeyEncryptionPassphrase, replica);
                    final Integer optionalIgtimiRiotPort = replicaSet.getMaster().getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                    final SailingAnalyticsProcess<String> newReplica = deployReplicaToExistingHost(replicaSet, targetHost, optionalKeyName, privateKeyEncryptionPassphrase,
                            replicaReplicationBearerToken,
                            totalMemorySizeFactor == null ? getMemoryInMegabytes(optionalKeyName, privateKeyEncryptionPassphrase, replica) : null,
                            totalMemorySizeFactor, optionalIgtimiRiotPort);
                    if (newReplica != null && newReplica.isReady(Landscape.WAIT_FOR_PROCESS_TIMEOUT)) {
                        logger.info("New replica " + newReplica + " deployed successfully to " + targetHost
                                + "; removing old replica " + replica
                                + " from public target group of application replica set " + replicaSet.getName());
                        replicaSet.getPublicTargetGroup().removeTarget(replica.getHost());
                        replicaSet.getOtherTargetGroups().forEach(tg->tg.removeTarget(replica.getHost()));
                        logger.info("Stopping old replica "+replica);
                        replica.stopAndTerminateIfLast(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                    }
                }
            }
        }
        return new Triple<>(targetHost, masterProcessesMoved, replicaProcessesMoved);
    }

    private Integer getTotalMemorySizeFactor(String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            final SailingAnalyticsProcess<String> process)
            throws Exception {
        final String totalMemorySizeFactorAsString = process.getEnvShValueFor(DefaultProcessConfigurationVariables.TOTAL_MEMORY_SIZE_FACTOR, Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        final Integer totalMemorySizeFactor = Util.hasLength(totalMemorySizeFactorAsString) ? Integer.valueOf(totalMemorySizeFactorAsString) : null;
        return totalMemorySizeFactor;
    }

    private Integer getMemoryInMegabytes(String optionalKeyName, byte[] privateKeyEncryptionPassphrase, final SailingAnalyticsProcess<String> process)
            throws Exception {
        final String memoryInMegabytesAsString = process.getEnvShValueFor(DefaultProcessConfigurationVariables.MEMORY, Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        final Integer memoryInMegabytes = JvmUtils.getMegabytesFromJvmSize(memoryInMegabytesAsString).orElse(null);
        return memoryInMegabytes;
    }
}
