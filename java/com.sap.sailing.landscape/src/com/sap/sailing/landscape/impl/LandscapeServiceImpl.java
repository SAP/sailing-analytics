package com.sap.sailing.landscape.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.procedures.CreateLaunchConfigurationAndAutoScalingGroup;
import com.sap.sailing.landscape.procedures.DeployProcessOnMultiServer;
import com.sap.sailing.landscape.procedures.SailingAnalyticsHostSupplier;
import com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration;
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
import com.sap.sse.landscape.InboundReplicationConfiguration;
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
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.ReverseProxy;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.common.shared.RedirectDTO;
import com.sap.sse.landscape.aws.impl.AwsApplicationReplicaSetImpl;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.impl.DNSCache;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.aws.orchestration.CopyAndCompareMongoDatabase;
import com.sap.sse.landscape.aws.orchestration.CreateDNSBasedLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.CreateDynamicLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.CreateLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.StartAwsHost;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.shared.util.Wait;
import com.sap.sse.util.ServiceTrackerFactory;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.sts.model.Credentials;

public class LandscapeServiceImpl implements LandscapeService {
    private static final Logger logger = Logger.getLogger(LandscapeServiceImpl.class.getName());

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
                        return new SailingAnalyticsProcessImpl<String>(port, host, serverDirectory, telnetPort, serverName,
                                expeditionUdpPort == null ? null : expeditionUdpPort.intValue(), getLandscape());
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
            Integer optionalMemoryTotalSizeFactorOrNull, Optional<Integer> minimumAutoScalingGroupSize,
            Optional<Integer> maximumAutoScalingGroupSize) throws Exception {
        final AwsLandscape<String> landscape = getLandscape();
        final AwsRegion region = new AwsRegion(regionId, landscape);
        final Release release = getRelease(releaseNameOrNullForLatestMaster);
        establishServerGroupAndTryToMakeCurrentUserItsOwnerAndMember(name);
        final com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration.Builder<?, String> masterConfigurationBuilder =
                createMasterConfigurationBuilder(name, masterReplicationBearerToken, optionalMemoryInMegabytesOrNull,
                        newSharedMasterInstance ? optionalMemoryTotalSizeFactorOrNull : null, region, release);
        final com.sap.sailing.landscape.procedures.StartSailingAnalyticsMasterHost.Builder<?, String> masterHostBuilder = StartSailingAnalyticsMasterHost.masterHostBuilder(masterConfigurationBuilder);
        masterHostBuilder
            .setInstanceType(InstanceType.valueOf(newSharedMasterInstance ? sharedInstanceType : dedicatedInstanceType))
            .setOptionalTimeout(WAIT_FOR_HOST_TIMEOUT)
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
        final String bearerTokenUsedByReplicas = Util.hasLength(replicaReplicationBearerToken) ? replicaReplicationBearerToken : getSecurityService().getOrCreateAccessToken(SessionUtils.getPrincipal().toString());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result =
            createLoadBalancingAndAutoScalingSetup(landscape, region, name, master, release, dedicatedInstanceType,
                dynamicLoadBalancerMapping, optionalKeyName, privateKeyEncryptionPassphrase, optionalDomainName,
                Optional.of(masterHostBuilder.getMachineImage()), bearerTokenUsedByReplicas,
                minimumAutoScalingGroupSize, maximumAutoScalingGroupSize);
        final Optional<SailingAnalyticsProcess<String>> unmanagedReplica = minimumAutoScalingGroupSize.map(minASGSize->{
            final List<SailingAnalyticsProcess<String>> unmanagedReplicas = new ArrayList<>();
            if (minASGSize == 0) {
                logger.info("No auto-scaling replica forced for replica set "+name+"; starting with an unmanaged replica on a shared instance");
                try {
                    unmanagedReplicas.add(launchUnmanagedReplica(result, region, optionalKeyName, privateKeyEncryptionPassphrase,
                            replicaReplicationBearerToken, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull,
                            Optional.of(InstanceType.valueOf(sharedInstanceType)),
                            /* optionalPreferredInstanceToDeployTo */ Optional.empty()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            return unmanagedReplicas.isEmpty() ? null : unmanagedReplicas.get(0);
        });
        // if an unmanaged replica process was launched, return a replica set that contains it; otherwise use the one we already have (without any replica)
        return unmanagedReplica.map(ur->getLandscape().getApplicationReplicaSet(region, name, master, Collections.singleton(ur))).orElse(result);
    }
    
    @Override
    public AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> deployApplicationToExistingHost(String replicaSetName,
            SailingAnalyticsHost<String> hostToDeployTo, String replicaInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String masterReplicationBearerToken, String replicaReplicationBearerToken,
            String optionalDomainName, Optional<Integer> optionalMinimumAutoScalingGroupSize, Optional<Integer> optionalMaximumAutoScalingGroupSize,
            Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Optional<InstanceType> optionalSharedInstanceTypeForNewReplicaHost, Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployUnmanagedReplicaTo) throws Exception {
        return deployApplicationToExistingHostInternal(hostToDeployTo.getRegion(),
                replicaSetName, hostToDeployTo,
                replicaInstanceType, dynamicLoadBalancerMapping, releaseNameOrNullForLatestMaster, optionalKeyName,
                privateKeyEncryptionPassphrase, masterReplicationBearerToken, replicaReplicationBearerToken,
                optionalDomainName, optionalMinimumAutoScalingGroupSize, optionalMaximumAutoScalingGroupSize,
                optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull, optionalSharedInstanceTypeForNewReplicaHost, optionalPreferredInstanceToDeployUnmanagedReplicaTo);
    }
    
    @Override
    public <AppConfigBuilderT extends SailingAnalyticsReplicaConfiguration.Builder<AppConfigBuilderT, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT>>
    SailingAnalyticsProcess<String> deployReplicaToExistingHost(final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            SailingAnalyticsHost<String> hostToDeployTo, String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull) throws Exception {
        logger.info("Deploying replica for application replica set "+replicaSet.getName()+" to host "+hostToDeployTo);
        return spinUpReplicaAndRegisterInPublicTargetGroup(hostToDeployTo.getRegion(), replicaSet, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase, replicaReplicationBearerToken,
                /* processLauncher: */ (AppConfigBuilderT replicaConfigurationBuilder)->{
                    // the process launcher uses the DeployProcessOnMultiServer procedure to launch the process based on the replica config 
                    DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT> replicaDeploymentProcessBuilder =
                            DeployProcessOnMultiServer.<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT> builder(replicaConfigurationBuilder, hostToDeployTo);
                    if (optionalKeyName != null) {
                        replicaDeploymentProcessBuilder.setKeyName(optionalKeyName);
                    }
                    replicaDeploymentProcessBuilder
                        .setOptionalTimeout(LandscapeService.WAIT_FOR_PROCESS_TIMEOUT)
                        .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase);
                    DeployProcessOnMultiServer<String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT> replicaDeploymentProcess;
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
     * The {@code replicaInstanceType} is used to configure the launch configuration used by the auto-scaling group
     * which is also created so that when dedicated replicas need to be provided during auto-scaling, their instance
     * type is known.
     * <p>
     * 
     * The "internal" method exists in order to declare a few type parameters which wouldn't be possible on the GWT RPC
     * interface method as some of these types are not seen by clients.
     * @param optionalMinimumAutoScalingGroupSize TODO
     * @param optionalMaximumAutoScalingGroupSize TODO
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
                    Integer optionalMemoryTotalSizeFactorOrNull, Optional<InstanceType> optionalInstanceType,
                    Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo) throws Exception {
        final AwsLandscape<String> landscape = getLandscape();
        final Release release = getRelease(releaseNameOrNullForLatestMaster);
        establishServerGroupAndTryToMakeCurrentUserItsOwnerAndMember(replicaSetName);
        final AppConfigBuilderT masterConfigurationBuilder = createMasterConfigurationBuilder(replicaSetName,
                masterReplicationBearerToken, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull,
                region, release);
        final DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT> multiServerAppDeployerBuilder =
                DeployProcessOnMultiServer.<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT> builder(
                        masterConfigurationBuilder, hostToDeployTo);
        if (optionalKeyName != null) {
            multiServerAppDeployerBuilder.setKeyName(optionalKeyName);
        }
        multiServerAppDeployerBuilder
                .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase)
                .setOptionalTimeout(LandscapeService.WAIT_FOR_HOST_TIMEOUT);
        final DeployProcessOnMultiServer<String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT> deployer = multiServerAppDeployerBuilder.build();
        deployer.run();
        final SailingAnalyticsProcess<String> master = deployer.getProcess();
        final String bearerTokenUsedByReplicas = Util.hasLength(replicaReplicationBearerToken) ? replicaReplicationBearerToken : getSecurityService().getOrCreateAccessToken(SessionUtils.getPrincipal().toString());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
            createLoadBalancingAndAutoScalingSetup(landscape, region, replicaSetName, master, release, replicaInstanceType, dynamicLoadBalancerMapping,
                optionalKeyName, privateKeyEncryptionPassphrase, optionalDomainName, /* use default AMI as replica machine image */ Optional.empty(),
                bearerTokenUsedByReplicas, optionalMinimumAutoScalingGroupSize, optionalMaximumAutoScalingGroupSize);
        final Iterable<SailingAnalyticsProcess<String>> replicas;
        if (optionalMinimumAutoScalingGroupSize.isPresent() && optionalMinimumAutoScalingGroupSize.get() == 0) {
            replicas = Collections.singleton(launchUnmanagedReplica(replicaSet, region, optionalKeyName,
                privateKeyEncryptionPassphrase, replicaReplicationBearerToken, optionalMemoryInMegabytesOrNull,
                optionalMemoryTotalSizeFactorOrNull, optionalInstanceType, optionalPreferredInstanceToDeployTo));
        } else {
            replicas = Collections.emptySet();
        }
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSetWithReplica =
                landscape.getApplicationReplicaSet(region, replicaSet.getServerName(), master, replicas);
        return replicaSetWithReplica;
    }

    /**
     * Starts a replica process for the given {@code replicaSet}. The memory configuration can optionally be defined. It
     * defaults to the usual large share of the physical RAM of the instance it is deployed on. The replica is launched
     * on the {@code optionalPreferredInstanceToDeployTo} host is specified and eligible, else on the "best" existing
     * eligible instance (based on aspects such as the processes already deployed on the instance). If no running
     * instance is found to be eligible (e.g., because none has the port required by the replica available, or all run
     * in the same availability zone as the replica set's master process), a new instance is launched.
     * <p>
     * 
     * If a new instance needs to be launched, the instance type can optionally be specified. It defaults to something
     * with large fast swap space and a reasonable amount of physical RAM that would allow somewhere between two and
     * four processes to share the physical RAM even during a live event.
     * 
     * @param optionalInstanceType
     *            defaults to {@link SharedLandscapeConstants#DEFAULT_SHARED_INSTANCE_TYPE_NAME}
     */
    private SailingAnalyticsProcess<String> launchUnmanagedReplica(
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            AwsRegion region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String replicaReplicationBearerToken, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Optional<InstanceType> optionalInstanceType,
            Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo) throws Exception {
        final EligibleInstanceForReplicaSetFindingStrategy strategyForFindingOrLaunchingInstanceForUnmangedReplica =
                new EligbleInstanceForReplicaSetFindingStrategyImpl(this, region, optionalKeyName,
                        privateKeyEncryptionPassphrase, /* master==false because we'd like to deploy a replica */ false,
                        /* mustBeDifferentAvailabilityZone */ true, optionalInstanceType,
                        optionalPreferredInstanceToDeployTo);
        final SailingAnalyticsProcess<String> replica = deployReplicaToExistingHost(
                replicaSet, strategyForFindingOrLaunchingInstanceForUnmangedReplica.getInstanceToDeployTo(replicaSet),
                optionalKeyName, privateKeyEncryptionPassphrase, replicaReplicationBearerToken, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull);
        return replica;
    }
    
    @Override
    public UUID archiveReplicaSet(String regionId, AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSetToArchive,
            String bearerTokenOrNullForApplicationReplicaSetToArchive,
            String bearerTokenOrNullForArchive,
            Duration durationToWaitBeforeCompareServers,
            int maxNumberOfCompareServerAttempts, boolean removeApplicationReplicaSet, MongoEndpoint moveDatabaseHere,
            String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception {
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> archiveReplicaSet = getLandscape()
                .getApplicationReplicaSetByTagValue(new AwsRegion(regionId, getLandscape()),
                        SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, SharedLandscapeConstants.ARCHIVE_SERVER_APPLICATION_HOST_TAG_VALUE,
                        new SailingAnalyticsHostSupplier<String>(), LandscapeService.WAIT_FOR_PROCESS_TIMEOUT,
                        Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption);
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
        logger.info("Importing master data from "+from+" to "+archive);
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
        if (mdiProgress != null && !mdiProgress.failed() && mdiProgress.getResult() != null) {
            logger.info("MDI from "+hostnameFromWhichToArchive+" info "+hostnameOfArchive+" succeeded. Waiting "+durationToWaitBeforeCompareServers+" before starting to compare content...");
            final CompareServersResult compareServersResult;
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
                    final AwsRegion region = new AwsRegion(regionId, getLandscape());
                    final ReverseProxy<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, RotatingFileBasedLog> centralReverseProxy =
                            getLandscape().getCentralReverseProxy(region);
                    // TODO bug5311: when refactoring this for general scope migration, moving to a dedicated replica set will not require this
                    // TODO bug5311: when refactoring this for general scope migration, moving into a cold storage server other than ARCHIVE will require ALBToReverseProxyRedirectMapper instead
                    logger.info("Adding reverse proxy rules for migrated content pointing to ARCHIVE");
                    defaultRedirect.accept(new ALBToReverseProxyArchiveRedirectMapper<>(
                            centralReverseProxy, hostnameFromWhichToArchive, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption));
                    if (removeApplicationReplicaSet) {
                        logger.info("Removing remote sailing server references to "+from+" from archive server "+archive);
                        try {
                            archive.removeRemoteServerReference(from);
                        } catch (Exception e) {
                            logger.log(Level.INFO, "Exception trying to remove remote server reference to "+from+
                                    "; probably such a reference didn't exist");
                        }
                        logger.info("Removing the application replica set archived ("+from+") was requested");
                        final SailingAnalyticsProcess<String> fromMaster = applicationReplicaSetToArchive.getMaster();
                        final Database fromDatabase = fromMaster.getDatabaseConfiguration(region, LandscapeService.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption);
                        removeApplicationReplicaSet(regionId, applicationReplicaSetToArchive, optionalKeyName, passphraseForPrivateKeyDecryption);
                        if (moveDatabaseHere != null) {
                            final Database toDatabase = moveDatabaseHere.getDatabase(fromDatabase.getName());
                            logger.info("Archiving the database content of "+fromDatabase.getConnectionURI()+" to "+toDatabase.getConnectionURI());
                            getCopyAndCompareMongoDatabaseBuilder(fromDatabase, toDatabase).run();
                        } else {
                            logger.info("No archiving of database content was requested. Leaving "+fromDatabase.getConnectionURI()+" untouched.");
                        }
                    } else {
                        logger.info("Removing remote sailing server references to events on "+from+" with IDs "+eventIDs+" from archive server "+archive);
                        archive.removeRemoteServerEventReferences(from, eventIDs);
                    }
                } else {
                    logger.severe("Even after "+maxNumberOfCompareServerAttempts+" attempts and waiting a total of "+
                            durationToWaitBeforeCompareServers.times(maxNumberOfCompareServerAttempts)+
                            " there were the following differences between exporting server "+hostnameFromWhichToArchive+
                            " and importing server "+hostnameOfArchive+":\nDifferences on importing side: "+compareServersResult.getADiffs()+
                            "\nDifferences on exporting side: "+compareServersResult.getBDiffs()+
                            "\nNot proceeding further. You need to resolve the issues manually.");
                }
            } else {
                logger.severe("Even after "+maxNumberOfCompareServerAttempts+" attempts and waiting a total of "+
                        durationToWaitBeforeCompareServers.times(maxNumberOfCompareServerAttempts)+
                        " the comparison of servers "+hostnameOfArchive+" and "+hostnameFromWhichToArchive+
                        " did not produce a result. Not proceeding. You have to resolve the issue manually.");
            }
        } else {
            logger.severe("The Master Data Import (MDI) from "+hostnameFromWhichToArchive+" into "+hostnameOfArchive+
                    " did not work"+(mdiProgress != null ? mdiProgress.getErrorMessage() : " (no result at all)"));
        }
        return idForProgressTracking;
    }

    private void terminateReplicasNotManagedByAutoScalingGroup(AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            String optionalKeyName, byte[] passphraseForPrivateKeyDecryption) throws InterruptedException, ExecutionException {
        // only terminate replica if not running on host created by auto-scaling group
        final AwsAutoScalingGroup autoScalingGroup = applicationReplicaSet.getAutoScalingGroup();
        for (final SailingAnalyticsProcess<String> replica : applicationReplicaSet.getReplicas()) {
            if (autoScalingGroup == null || !replica.getHost().isManagedByAutoScalingGroup(autoScalingGroup)) {
                logger.info("Found replica "+replica+" running on an instance not managed by auto-scaling group " +
                        (autoScalingGroup != null ? autoScalingGroup.getName() : "") + ". Stopping...");
                replica.stopAndTerminateIfLast(LandscapeService.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption);
            }
        }
    }
    
    @Override
    public void removeApplicationReplicaSet(String regionId,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet, String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception {
        final AwsRegion region = new AwsRegion(regionId, getLandscape());
        final AwsAutoScalingGroup autoScalingGroup = applicationReplicaSet.getAutoScalingGroup();
        final CompletableFuture<Void> autoScalingGroupRemoval;
        terminateReplicasNotManagedByAutoScalingGroup(applicationReplicaSet, optionalKeyName, passphraseForPrivateKeyDecryption);
        if (autoScalingGroup != null) {
            // remove the launch configuration used by the auto scaling group and the auto scaling group itself;
            // this will also terminate all replicas spun up by the auto-scaling group
            autoScalingGroupRemoval = getLandscape().removeAutoScalingGroupAndLaunchConfiguration(autoScalingGroup);
        } else {
            autoScalingGroupRemoval = new CompletableFuture<>();
            autoScalingGroupRemoval.complete(null);
        }
        // terminate the instances
        autoScalingGroupRemoval.thenAccept(v->
            applicationReplicaSet.getMaster().stopAndTerminateIfLast(LandscapeService.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption));
        // remove the load balancer rules
        getLandscape().deleteLoadBalancerListenerRules(region, Util.toArray(applicationReplicaSet.getLoadBalancerRules(), new Rule[0]));
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
    }

    private <BuilderT extends CopyAndCompareMongoDatabase.Builder<BuilderT, String>> CopyAndCompareMongoDatabase<String>
    getCopyAndCompareMongoDatabaseBuilder(Database fromDatabase, Database toDatabase) throws Exception {
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
            result = AwsLandscape.obtain(keyId, secret, sessionToken);
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
        final Credentials credentials = AwsLandscape.obtain(awsAccessKey, awsSecret).getMfaSessionCredentials(mfaTokenCode);
        final AwsSessionCredentialsWithExpiryImpl result = new AwsSessionCredentialsWithExpiryImpl(
                credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken(),
                TimePoint.of(credentials.expiration().toEpochMilli()));
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

    private void establishServerGroupAndTryToMakeCurrentUserItsOwnerAndMember(String serverName) {
        final String serverGroupName = serverName + ServerInfo.SERVER_GROUP_NAME_SUFFIX;
        final UserGroup existingServerGroup = getSecurityService().getUserGroupByName(serverGroupName);
        final UserGroup serverGroup;
        if (existingServerGroup == null) {
            final UUID serverGroupId = UUID.randomUUID();
            serverGroup = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredSecurityTypes.USER_GROUP,
                    new TypeRelativeObjectIdentifier(serverGroupId.toString()), /* securityDisplayName */ serverGroupName,
                    (Callable<UserGroup>)()->getSecurityService().createUserGroup(serverGroupId, serverGroupName));
        } else {
            serverGroup = existingServerGroup;
            final User currentUser = getSecurityService().getCurrentUser();
            if (!Util.contains(serverGroup.getUsers(), currentUser) && getSecurityService().hasCurrentUserUpdatePermission(serverGroup)) {
                getSecurityService().addUserToUserGroup(serverGroup, currentUser);
            }
        }
    }

    private <AppConfigBuilderT extends com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String>> AppConfigBuilderT createMasterConfigurationBuilder(
            String replicaSetName, String masterReplicationBearerToken, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, final AwsRegion region, final Release release) {
        final AppConfigBuilderT masterConfigurationBuilder = SailingAnalyticsMasterConfiguration.masterBuilder();
        final String bearerTokenUsedByMaster = Util.hasLength(masterReplicationBearerToken) ? masterReplicationBearerToken : getSecurityService().getOrCreateAccessToken(SessionUtils.getPrincipal().toString());
        masterConfigurationBuilder
            .setLandscape(getLandscape())
            .setServerName(replicaSetName)
            .setRelease(release)
            .setRegion(region)
            .setInboundReplicationConfiguration(InboundReplicationConfiguration.builder().setCredentials(new BearerTokenReplicationCredentials(bearerTokenUsedByMaster)).build());
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
            String replicaSetName, final int masterPort, final Release release,
            final String bearerTokenUsedByReplicas, final String masterHostname) {
        final AppConfigBuilderT replicaConfigurationBuilder = SailingAnalyticsReplicaConfiguration.replicaBuilder();
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
            final String bearerTokenUsedByReplicas, Optional<Integer> minimumNumberOfReplicas, Optional<Integer> maximumNumberOfReplicas)
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
        final String domainName = Optional.ofNullable(optionalDomainName).orElse(SharedLandscapeConstants.DEFAULT_DOMAIN_NAME);
        final String masterHostname = replicaSetName+"."+domainName;
        final CreateLoadBalancerMapping<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLoadBalancerMapping = createLoadBalancerMappingBuilder
            .setProcess(master)
            .setHostname(masterHostname)
            .setTargetGroupNamePrefix(SAILING_TARGET_GROUP_NAME_PREFIX)
            .setLandscape(landscape)
            .build();
        createLoadBalancerMapping.run();
        // construct a replica configuration which is used to produce the user data for the launch configuration used in an auto-scaling group
        final Builder<?, String> replicaConfigurationBuilder = createReplicaConfigurationBuilder(region, replicaSetName, master.getPort(), release, bearerTokenUsedByReplicas, masterHostname);
        // Now wait for master to become healthy before creating auto-scaling; otherwise it may happen that the replica tried to start
        // replication before the master is ready (see also bug 5527).
        master.waitUntilReady(LandscapeService.WAIT_FOR_HOST_TIMEOUT);
        final CreateLaunchConfigurationAndAutoScalingGroup.Builder<String, ?, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLaunchConfigurationAndAutoScalingGroupBuilder =
                CreateLaunchConfigurationAndAutoScalingGroup.builder(landscape, region, replicaSetName, createLoadBalancerMapping.getPublicTargetGroup());
        createLaunchConfigurationAndAutoScalingGroupBuilder
            .setInstanceType(InstanceType.valueOf(replicaInstanceType))
            .setTags(Tags.with(StartAwsHost.NAME_TAG_NAME, StartSailingAnalyticsHost.INSTANCE_NAME_DEFAULT_PREFIX+replicaSetName+" (Auto-Replica)")
                         .and(SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, replicaSetName))
            .setOptionalTimeout(LandscapeService.WAIT_FOR_HOST_TIMEOUT)
            .setReplicaConfiguration(replicaConfigurationBuilder.build()); // use the default scaling parameters (currently 1/30/30000)
        minimumNumberOfReplicas.ifPresent(minNumberOfReplicas->createLaunchConfigurationAndAutoScalingGroupBuilder.setMinReplicas(minNumberOfReplicas));
        maximumNumberOfReplicas.ifPresent(maxNumberOfReplicas->createLaunchConfigurationAndAutoScalingGroupBuilder.setMaxReplicas(maxNumberOfReplicas));
        if (replicaMachineImage.isPresent()) {
            createLaunchConfigurationAndAutoScalingGroupBuilder.setImage(replicaMachineImage.get());
        } else {
            // obtain the latest AMI for launching a Sailing Analytics replica host:
            createLaunchConfigurationAndAutoScalingGroupBuilder.setImage(
                    StartSailingAnalyticsReplicaHost.replicaHostBuilder(replicaConfigurationBuilder)
                        .setLandscape(getLandscape())
                        .getMachineImage());
        }
        if (optionalKeyName != null) {
            createLaunchConfigurationAndAutoScalingGroupBuilder.setKeyName(optionalKeyName);
        }
        createLaunchConfigurationAndAutoScalingGroupBuilder.build().run();
        final CompletableFuture<Iterable<ApplicationLoadBalancer<String>>> allLoadBalancersInRegion = landscape.getLoadBalancersAsync(region);
        final CompletableFuture<Map<TargetGroup<String>, Iterable<TargetHealthDescription>>> allTargetGroupsInRegion = landscape.getTargetGroupsAsync(region);
        final CompletableFuture<Map<Listener, Iterable<Rule>>> allLoadBalancerRulesInRegion = landscape.getLoadBalancerListenerRulesAsync(region, allLoadBalancersInRegion);
        final CompletableFuture<Iterable<AutoScalingGroup>> autoScalingGroups = landscape.getAutoScalingGroupsAsync(region);
        final CompletableFuture<Iterable<LaunchConfiguration>> launchConfigurations = landscape.getLaunchConfigurationsAsync(region);
        final DNSCache dnsCache = landscape.getNewDNSCache();
        final AwsApplicationReplicaSet<String,SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet =
                new AwsApplicationReplicaSetImpl<>(replicaSetName, masterHostname, master, /* no replicas yet */ Optional.empty(),
                        allLoadBalancersInRegion, allTargetGroupsInRegion, allLoadBalancerRulesInRegion, autoScalingGroups, launchConfigurations, dnsCache);
        return applicationReplicaSet;
    }

    @Override
    public AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> upgradeApplicationReplicaSet(AwsRegion region,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String releaseOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String replicaReplicationBearerToken)
            throws MalformedURLException, IOException, TimeoutException, Exception {
        final Release release = getRelease(releaseOrNullForLatestMaster);
        final String effectiveReplicaReplicationBearerToken = Util.hasLength(replicaReplicationBearerToken) ? replicaReplicationBearerToken :
            getSecurityService().getOrCreateAccessToken(SessionUtils.getPrincipal().toString());
        final int oldAutoScalingGroupMinSize;
        if (replicaSet.getAutoScalingGroup() != null) {
            oldAutoScalingGroupMinSize = replicaSet.getAutoScalingGroup().getAutoScalingGroup().minSize();
        } else {
            oldAutoScalingGroupMinSize = -1;
        }
        final Set<SailingAnalyticsProcess<String>> replicasToStopAfterUpgradingMaster = new HashSet<>();
        Util.addAll(replicaSet.getReplicas(), replicasToStopAfterUpgradingMaster);
        final SailingAnalyticsProcess<String> additionalReplicaStarted = ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(
                replicaSet, optionalKeyName, privateKeyEncryptionPassphrase, effectiveReplicaReplicationBearerToken);
        if (replicaSet.getAutoScalingGroup() != null) {
            getLandscape().updateReleaseInAutoScalingGroup(region, replicaSet.getAutoScalingGroup(), replicaSet.getName(), release);
        }
        final SailingAnalyticsProcess<String> master = replicaSet.getMaster();
        master.refreshToRelease(release, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        // wait for master to turn healthy:
        logger.info("Waiting for master "+master+" to get ready with new release "+release.getName());
        master.waitUntilReady(Optional.of(Duration.ONE_DAY)); // wait a little longer since master may need to re-load many races
        // register master again with master and public target group
        logger.info("Adding master "+master+" again to target groups "+
                replicaSet.getPublicTargetGroup()+" and "+replicaSet.getMasterTargetGroup());
        replicaSet.getPublicTargetGroup().addTarget(replicaSet.getMaster().getHost());
        replicaSet.getMasterTargetGroup().addTarget(replicaSet.getMaster().getHost());
        // if a replica was spun up (additionalReplicaStarted), remove from public target group and terminate:
        if (additionalReplicaStarted != null) {
            replicasToStopAfterUpgradingMaster.add(additionalReplicaStarted);
            if (replicaSet.getAutoScalingGroup() != null) {
                getLandscape().updateAutoScalingGroupMinSize(replicaSet.getAutoScalingGroup(), oldAutoScalingGroupMinSize);
            } // else, the replica was started explicitly, without an auto-scaling group; in any case, all replicas still
            // on the old release will now be stopped:
        }
        final Set<SailingAnalyticsProcess<String>> replicasUpdatedInPlace = new HashSet<>();
        // TODO bug5674: start up as many new replicas as there were old replicas without hooking up to target group, then replace atomically, shut down old replicas, wait for auto-scaling to provide new ones, then terminate explicit upgrade replicas
        for (final SailingAnalyticsProcess<String> replica : replicasToStopAfterUpgradingMaster) {
            // if managed by auto-scaling group or if it's an "unmanaged" / "explicit" replica and it's the one launched in order to have at least one, stop/terminate;
            replicaSet.getPublicTargetGroup().removeTarget(replica.getHost());
            if (replica.getHost().isManagedByAutoScalingGroup() ||
                    (additionalReplicaStarted != null && replica.getHost().getInstanceId().equals(additionalReplicaStarted.getHost().getInstanceId()))) {
                logger.info("Stopping (and terminating if last application process on host) replicas on old release: "+replicasToStopAfterUpgradingMaster);
                replica.stopAndTerminateIfLast(LandscapeService.WAIT_FOR_HOST_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
            } else { // otherwise, upgrade in place and add to target group again when ready
                logger.info("Refreshing unmanaged replica "+replica+" in place");
                replica.refreshToRelease(release, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                replica.waitUntilReady(LandscapeService.WAIT_FOR_PROCESS_TIMEOUT);
                replicaSet.getPublicTargetGroup().addTarget(replica.getHost());
                replicasUpdatedInPlace.add(replica);
            }
        }
        return getLandscape().getApplicationReplicaSet(region, replicaSet.getServerName(), master, replicasUpdatedInPlace);
    }

    @Override
    public Iterable<SailingAnalyticsHost<String>> getEligibleSharedHostsForReplicaSet(AwsRegion region,
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) {
        return Util.filter(getLandscape().getRunningHostsWithTagValue(region,
                SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG, SharedLandscapeConstants.MULTI_PROCESS_INSTANCE_TAG_VALUE,
                new SailingAnalyticsHostSupplier<String>()), h->{
                    try {
                        return replicaSet.isEligibleForDeployment(h, LandscapeService.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
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
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase,
            final String effectiveReplicaReplicationBearerToken)
            throws Exception, MalformedURLException, IOException, TimeoutException, InterruptedException,
            ExecutionException {
        final Set<SailingAnalyticsProcess<String>> replicasToStopReplicating = new HashSet<>();
        Util.addAll(replicaSet.getReplicas(), replicasToStopReplicating);
        final SailingAnalyticsProcess<String> additionalReplicaStarted;
        if (Util.isEmpty(replicaSet.getReplicas())) {
            logger.info("No replica found for replica set " + replicaSet.getName()
                    + "; spinning one up and waiting for it to become healthy");
            additionalReplicaStarted = launchReplicaAndWaitUntilHealthy(replicaSet, Optional.ofNullable(optionalKeyName),
                    privateKeyEncryptionPassphrase, effectiveReplicaReplicationBearerToken);
            replicasToStopReplicating.add(additionalReplicaStarted);
        } else {
            additionalReplicaStarted = null;
        }
        logger.info("Stopping replication for replica set "+replicaSet.getName());
        for (final SailingAnalyticsProcess<String> replica : replicasToStopReplicating) {
            logger.info("...asking replica "+replica+" to stop replication");
            replica.stopReplicatingFromMaster(effectiveReplicaReplicationBearerToken, LandscapeService.WAIT_FOR_PROCESS_TIMEOUT);
        }
        logger.info("Done stopping replication. Removing master "+replicaSet.getMaster()+" from target groups "+
                replicaSet.getPublicTargetGroup()+" and "+replicaSet.getMasterTargetGroup());
        replicaSet.getPublicTargetGroup().removeTarget(replicaSet.getMaster().getHost());
        replicaSet.getMasterTargetGroup().removeTarget(replicaSet.getMaster().getHost());
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
            spunUpReplica = spinUpReplicaAndRegisterInPublicTargetGroup(replicaSet, optionalKeyName,
                    privateKeyEncryptionPassphrase, replicationBearerToken);
        }
        return spunUpReplica;
    }

    private SailingAnalyticsProcess<String> spinUpReplicaAndRegisterInPublicTargetGroup(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicationBearerToken) throws Exception {
        final AwsRegion region = replicaSet.getMaster().getHost().getRegion();
        return spinUpReplicaAndRegisterInPublicTargetGroup(region, replicaSet, optionalKeyName, privateKeyEncryptionPassphrase, replicationBearerToken,
                /* processLauncher: */ replicaConfigurationBuilder->{
                    // the process launcher determines the master's instance type and launches a host of the same type using the replica configuration for the process config
                    final InstanceType masterInstanceType = getLandscape().getInstance(replicaSet.getMaster().getHost().getInstanceId(), region).instanceType();
                    final com.sap.sailing.landscape.procedures.StartSailingAnalyticsReplicaHost.Builder<?, String> replicaHostBuilder = StartSailingAnalyticsReplicaHost.replicaHostBuilder(replicaConfigurationBuilder);
                    replicaHostBuilder
                        .setInstanceType(masterInstanceType)
                        .setOptionalTimeout(LandscapeService.WAIT_FOR_HOST_TIMEOUT)
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
     * @return the process that the {@code processLauncher} started
     */
    private <AppConfigBuilderT extends SailingAnalyticsReplicaConfiguration.Builder<AppConfigBuilderT, String>>
    SailingAnalyticsProcess<String> spinUpReplicaAndRegisterInPublicTargetGroup(
            AwsRegion region,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicationBearerToken,
            Function<AppConfigBuilderT, SailingAnalyticsProcess<String>> processLauncher) throws Exception {
        final Release release = replicaSet.getVersion(LandscapeService.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
        final AppConfigBuilderT replicaConfigurationBuilder =
                createReplicaConfigurationBuilder(region, replicaSet.getServerName(), replicaSet.getMaster().getPort(), release, replicationBearerToken, replicaSet.getHostname());
        final SailingAnalyticsProcess<String> sailingAnalyticsProcess = processLauncher.apply(replicaConfigurationBuilder);
        registerReplicaInReplicaSetPublicTargetGroup(sailingAnalyticsProcess, replicaSet);
        return sailingAnalyticsProcess;
    }
    
    /**
     * Waits for the process to become ready (see {@link SailingAnalyticsProcess#waitUntilReady(Optional)}),
     * then registers its {@link SailingAnalyticsProcess#getHost() host} with the {@code replicaSet}'s
     * {@link AwsApplicationReplicaSet#getPublicTargetGroup() public target group}.
     */
    private void registerReplicaInReplicaSetPublicTargetGroup(
            final SailingAnalyticsProcess<String> sailingAnalyticsProcess,
            final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet) throws TimeoutException, Exception {
        sailingAnalyticsProcess.waitUntilReady(LandscapeService.WAIT_FOR_HOST_TIMEOUT);
        if (replicaSet.getPublicTargetGroup() != null) {
            replicaSet.getPublicTargetGroup().addTarget(sailingAnalyticsProcess.getHost());
        }
    }

    private SailingAnalyticsProcess<String> spinUpReplicaByIncreasingAutoScalingGroupMinSize(
            AwsAutoScalingGroup autoScalingGroup,
            SailingAnalyticsProcess<String> master)
            throws TimeoutException, Exception {
        if (autoScalingGroup.getAutoScalingGroup().minSize() < 1) {
            getLandscape().updateAutoScalingGroupMinSize(autoScalingGroup, 1);
        }
        return Wait.wait(()->hasHealthyReplica(master), healthyReplica->healthyReplica != null,
                /* retryOnException */ true,
                LandscapeService.WAIT_FOR_HOST_TIMEOUT, Duration.ONE_SECOND.times(5), Level.INFO,
                "Waiting for auto-scaling group to produce healthy replica");
    }

    /**
     * Returns one replica process that is healthy, or {@code null} if no such process was found
     */
    private SailingAnalyticsProcess<String> hasHealthyReplica(SailingAnalyticsProcess<String> master) throws Exception {
        final HostSupplier<String, SailingAnalyticsHost<String>> hostSupplier = new SailingAnalyticsHostSupplier<>();
        for (final SailingAnalyticsProcess<String> replica : master.getReplicas(LandscapeService.WAIT_FOR_HOST_TIMEOUT, hostSupplier, processFactoryFromHostAndServerDirectory)) {
            if (replica.isReady(LandscapeService.WAIT_FOR_HOST_TIMEOUT)) {
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
        return startMultiServerProcedure.getHost();
    }

    @Override
    public Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> updateImageForReplicaSets(AwsRegion region,
            Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> replicaSets,
            Optional<AmazonMachineImage<String>> optionalAmi) throws InterruptedException, ExecutionException {
        final Set<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> result = new HashSet<>();
        for (final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet : replicaSets) {
            if (replicaSet.getAutoScalingGroup() != null) {
                final AmazonMachineImage<String> ami = optionalAmi.orElseGet(
                        ()->getLandscape().getLatestImageWithType(region, SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING));
                logger.info("Upgrading AMI in auto-scaling group "+replicaSet.getAutoScalingGroup().getName()+" of replica set "+replicaSet.getName()+" to "+ami.getId());
                getLandscape().updateImageInAutoScalingGroup(region, replicaSet.getAutoScalingGroup(), replicaSet.getName(), ami);
                result.add(getLandscape().getApplicationReplicaSet(region, replicaSet.getServerName(), replicaSet.getMaster(), replicaSet.getReplicas()));
            } else {
                logger.info("No auto-scaling group found for replica set "+replicaSet.getName()+" to update AMI in");
            }
        }
        return result;
    }
}
