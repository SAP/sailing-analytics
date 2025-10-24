package com.sap.sailing.landscape;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.procedures.DeployProcessOnMultiServer;
import com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration;
import com.sap.sailing.landscape.procedures.SailingAnalyticsReplicaConfiguration;
import com.sap.sailing.landscape.procedures.SailingAnalyticsReplicaConfiguration.Builder;
import com.sap.sailing.landscape.procedures.SailingProcessConfigurationVariables;
import com.sap.sailing.landscape.procedures.StartMultiServer;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sailing.server.gateway.interfaces.SailingServer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.application.ApplicationReplicaSet;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.security.SecurityService;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;

public interface LandscapeService {
    /**
     * The timeout for a Master Data Import (MDI) to complete
     */
    Optional<Duration> MDI_TIMEOUT = Optional.of(Duration.ONE_HOUR.times(6));
    
    /**
     * time to wait between checks whether the master-data import has finished
     */
    Duration TIME_TO_WAIT_BETWEEN_MDI_COMPLETION_CHECKS = Duration.ONE_SECOND.times(15);
    
    String USER_PREFERENCE_FOR_SESSION_TOKEN = "___aws.session.token___";
   
    String SAILING_TARGET_GROUP_NAME_PREFIX = "S-";

    /**
     * For a combination of an AWS access key ID, the corresponding secret plus an MFA token code produces new session
     * credentials and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. Callers shall ensure that the current user has the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    void createMfaSessionCredentials(String awsAccessKey, String awsSecret, String mfaTokenCode);

    /**
     * For a combination of an AWS access key ID, the corresponding secret plus a session token produces the session
     * token and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. Callers shall ensure that the current user has the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    void createSessionCredentials(String awsKeyId, String awsKeySecret, String sessionToken);
    
    boolean hasValidSessionCredentials();

    AwsSessionCredentialsWithExpiry getSessionCredentials();

    /**
     * For the current user who has to have the {@code LANDSCAPE:MANAGE:AWS} permission, clears the preference in the
     * user's preference store which holds any session credentials created previously using
     * {@link #createMfaSessionCredentials(String, String, String)}.
     */
    void clearSessionCredentials();
    
    /**
     * Creates a new application replica set by launching a new instance that runs the master process, and if the
     * {@code minimumAutoScalingGroupSize} is 0, a replica process running on a shared instance. In this case an
     * eligible shared instance is looked for (based on the
     * {@link SharedLandscapeConstants#MULTI_PROCESS_INSTANCE_TAG_VALUE} value of the
     * {@link SharedLandscapeConstants#SAILING_ANALYTICS_APPLICATION_HOST_TAG} tag), and if not found, a new one is
     * launched. If the {@code minimumAutoScalingGroupSize} is greater than 0, no unmanaged replica is launched because
     * the auto-scaling group will launch one or more of them on dedicated instances.
     * 
     * @param name
     *            the name for the new replica set; defines the "server name" from which the server group name, the
     *            replica channel / exchange name and the database name will be derived.
     * @param newSharedMasterInstance
     *            if {@code true} the instance launched for the master process will be created such that it can be
     *            shared by other application processes as well. In particular, it's name will be set to
     *            {@link SharedLandscapeConstants#MULTI_PROCESS_INSTANCE_DEFAULT_NAME} and the
     *            {@link SharedLandscapeConstants#SAILING_ANALYTICS_APPLICATION_HOST_TAG} tag will be set to
     *            {@link SharedLandscapeConstants#MULTI_PROCESS_INSTANCE_TAG_VALUE}.
     * @param sharedInstanceType
     *            specifies the type of instance launched for hosting the new master process in case a shared masted
     *            instance is requested by setting {@code newSharedMasterInstance} to {@code true}, and for launching an
     *            instance for a first replica not managed by an auto-scaling group in case
     *            {@code minimumAutoScalingGroupSize} is 0. Make sure to use one that is good for sharing by multiple
     *            processes; e.g., use an instance type with plenty of fast swap space, such as
     *            {@link SharedLandscapeConstants#DEFAULT_SHARED_INSTANCE_TYPE_NAME}. The instance type is specified as
     *            a string that must match one of the {@link InstanceType} {@link Enum#name() literal names}. The
     *            parameter is ignored if neither a shared master nor a first shared replica are requested.
     * @param dedicatedInstanceType
     *            defines the instance type to use for the master (unless a shared master instance is requested by
     *            setting the {@code newSharedMasterInstance} parameter to {@code true}) and for the auto-scaling
     *            replicas. Must not be {@code null}.
     * @param dynamicLoadBalancerMapping
     *            If {@code true}, no DNS entry is created for the new application replica set. Instead, the
     *            fall-through logic in the landscape's default mapping for the {@code optionalDomainName} (defaulting
     *            to {@code sapsailing.com}) is used, assuming it points to a default Application Load Balancer (ALB) in
     *            our default region (see {@link SharedLandscapeConstants#REGION_WITH_DEFAULT_LOAD_BALANCER}).
     * @param optionalDomainName
     *            defaults to {@link SharedLandscapeConstants#DEFAULT_DOMAIN_NAME}.
     * @param optionalMemoryInMegabytesOrNull
     *            if not {@code null}, specifies the heap size to allocate for the master and replica processes each, in
     *            MB (1024*1024B); if provided, this takes precedence over anything specified in
     *            {@code optionalMemoryTotalSizeFactorOrNull}.
     * @param optionalMemoryTotalSizeFactorOrNull
     *            if not {@code null} and if {@code optionalMemoryInMegabytesOrNull} does not specify an absolute heap
     *            size, this parameter specifies how much memory to allocate for application processes on shared
     *            instances (which does not include dedicated auto-scaling replicas); the amount is specified as a
     *            fraction of the "physical" RAM (as seen by the operating system running on the instance) minus some
     *            space reserved for the operating system itself and for the Java VM. It can be thought of as an
     *            approximation for how many processes configured this way will fit into the instance's physical memory
     *            without the need for massive swapping activity. The parameter will be ignored for a new dedicated
     *            master instance where we assume that almost all physical RAM shall be made available to the process.
     * @param optionalIgtimiRiotPort
     *            if non-{@code null}, this will be used for the
     *            {@link SailingProcessConfigurationVariables#IGTIMI_RIOT_PORT} variable in both, primary/master and
     *            replica.
     * @param minimumAutoScalingGroupSize
     *            if {@code 0}, a replica process will be started on a shared host that must run in an availability zone
     *            different from the one on which the master process runs. If no such shared host exists that is
     *            eligible to receive a process deployment for the new replica set (e.g., based on available port
     *            restrictions), a new shared host is launched, using the same instance type specification provided for
     *            the master ({@code masterInstanceType}). Note that for the probably somewhat unusual combination of a
     *            non-shared master instance ({@code newSharedMasterInstance==false}) and a
     *            {@code minimumAutoScalingGroupSize} of 0, a "shared" host that needs to be launched for the first
     *            replica will inherit an instance type from the master's configuration that may not be suited too well
     *            for sharing. Eligibility considerations and precedence rules are then hoped to rank such instances to
     *            the bottom of the list based, e.g., on their available physical / swap memory ratio, so that they
     *            would hardly ever get chosen as a deployment target for other replica's shared instances.
     */
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createApplicationReplicaSet(
            String regionId, String name, boolean newSharedMasterInstance, String sharedInstanceType,
            String dedicatedInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String masterReplicationBearerToken, String replicaReplicationBearerToken, String optionalDomainName,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            Integer optionalIgtimiRiotPort, Optional<Integer> minimumAutoScalingGroupSize, Optional<Integer> maximumAutoScalingGroupSize)
            throws Exception;

    /**
     * Starts a first master process of a new replica set whose name is provided by the {@code replicaSetName}
     * parameter. The process is started on the host identified by the {@code hostToDeployTo} parameter. A set of
     * available ports is identified and chosen automatically. The target groups and load balancing set-up is created.
     * The {@code replicaInstanceType} is used to configure the launch template used by the auto-scaling group which is
     * also created so that when dedicated replicas need to be provided during auto-scaling, their instance type is
     * known. The choice of {@code dynamicLoadBalancerMapping} must only be set if the host to deploy to lives in the
     * default region; otherwise, the DNS wildcard record for the overall domain would be made point to a wrong region.
     * If set to {@code false}, a DNS entry will be created that points to the load balancer used for the new replica
     * set's routing rules.
     * <p>
     * 
     * @param optionalMinimumAutoScalingGroupSize
     *            defaults to 1; if 0, a replica process will be launched on an eligible shared instance in an
     *            availability zone different from that of the instance hosting the master process. Otherwise, at least
     *            one auto-scaling replica will ensure availability of the replica set.
     * @param optionalIgtimiRiotPort
     *            if non-{@code null}, this will be used to configure the
     *            {@link SailingProcessConfigurationVariables#IGTIMI_RIOT_PORT} variable for primary/master and replica
     *            processes
     */
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> deployApplicationToExistingHost(String replicaSetName,
            SailingAnalyticsHost<String> hostToDeployTo, String replicaInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String masterReplicationBearerToken, String replicaReplicationBearerToken,
            String optionalDomainName, Optional<Integer> optionalMinimumAutoScalingGroupSize, Optional<Integer> optionalMaximumAutoScalingGroupSize,
            Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort,
            Optional<InstanceType> optionalSharedInstanceTypeForNewReplicaHost,
            Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployUnmanagedReplicaTo) throws Exception;
    
    /**
     * @return the reports on the master data import and content comparison; 
     */
    Triple<DataImportProgress, CompareServersResult, String> archiveReplicaSet(String regionId,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSetToArchive,
            String bearerTokenOrNullForApplicationReplicaSetToArchive, String bearerTokenOrNullForArchive,
            Duration durationToWaitBeforeCompareServers, int maxNumberOfCompareServerAttempts,
            boolean removeApplicationReplicaSet, MongoEndpoint moveDatabaseHere, String optionalKeyName,
            byte[] passphraseForPrivateKeyDecryption) throws Exception;
    
    /**
     * If the replica set is mapped through DNS, the DNS record is removed first, before any attempts are made to shut
     * down the processes and to remove the ALB rules and target groups. This way, this method can also be used at the
     * end of an archiving process where a reverse proxy entry has already been set to direct traffic for the replica
     * set to the archive from now on. It is then important that the DNS record is removed before starting to dismantle
     * the replica set because only then the traffic will be routed to the archive before the replica set's
     * infrastructure really becomes unresponsive.
     * 
     * @param moveDatabaseHere
     *            if not {@code null}, the application replica set master's database will be moved to this MongoDB
     *            endpoint
     * @return an error message string in case archiving the database was requested but failed for some reason;
     *         {@code null} otherwise
     */
    String removeApplicationReplicaSet(String regionId,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSetToRemove,
            MongoEndpoint moveDatabaseHere, String optionalKeyName, byte[] passphraseForPrivateKeyDecryption) throws Exception;

    Release getRelease(String releaseNameOrNullForLatestMaster);

    /**
     * @param optionalDomainName defaults to {@link SharedLandscapeConstants#DEFAULT_DOMAIN_NAME}.
     */
    String getFullyQualifiedHostname(String unqualifiedHostname, Optional<String> optionalDomainName);

    AwsLandscape<String> getLandscape();

    String getDefaultRedirectPath(Rule defaultRedirectRule);

    /**
     * Performs an in-place upgrade for the master service if the replica set has distinct public and master
     * target groups. If no replica exists, one is launched with the master's release, and the method waits
     * until the replica has reached its healthy state. The replica is then registered in the public target group.<p>
     * 
     * Then, the {@code ./refreshInstance.sh install-release <release>} command is sent to the master which will
     * download and unpack the new release but will not yet stop the master process. In parallel, the existing
     * default launch template version will be copied and updated with user data reflecting the new release to be used.
     * The existing auto-scaling group will then use the new default launch template version.<p>
     * 
     * Replication is then stopped for all existing replicas, then the master is de-registered from the master
     * target group and the public target group, effectively making the replica set "read-only." Then, the {@code ./stop}
     * command is issued which is expected to wait until all process resources have been released so that it's
     * appropriate to call {@code ./start} just after the {@code ./stop} call has returned, thus spinning up the
     * master process with the new release configuration.<p>
     * 
     * When the master process has reached its healthy state, it is registered with both target groups while all other
     * replicas are de-registered and then stopped. For replica processes being the last on their host, the host will
     * be terminated. It is up to an auto-scaling group or to the user to decide whether to launch new replicas again.
     * This won't happen automatically by this procedure.
     * TODO bug5674: before registering the master with the TGs, spin up as many new replicas as there are currently
     * replicas; wait until they are all ready, then register master and new replicas in TGs and de-register old replicas.
     * Then terminate old auto-scaling replicas and update any unmanaged replica in-place. When the number of auto-scaling
     * replicas has reached the desired size of the auto-scaling group, terminate the replicas created explicitly.<p>
     * 
     * Shards are updated by spinning up replicas for the temporary transition and changing the auto scaling config.
     * After that all shard replicas are getting shutdown and restarted with the new launch config.
     * It's expected that the replica set has its own auto scaling group if it has shards.
     */
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> upgradeApplicationReplicaSet(AwsRegion region,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String releaseOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String replicaReplicationBearerToken) throws InterruptedException, ExecutionException,
            MalformedURLException, IOException, TimeoutException, Exception;

    /**
     * @return a new replica that was started in case no running replica was found in the {@code replicaSet}, otherwise
     *         {@code null}.
     */
    SailingAnalyticsProcess<String> ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String effectiveReplicaReplicationBearerToken) throws Exception, MalformedURLException, IOException,
            TimeoutException, InterruptedException, ExecutionException;

    /**
     * For an existing replica set deploys a new replica onto an existing host. The host may be shared by multiple
     * application processes. As a precondition, the host must be
     * {@link AwsApplicationReplicaSet#isEligibleForDeployment(com.sap.sse.landscape.aws.ApplicationProcessHost, Optional, Optional, byte[])
     * eligible} for deploying a process of the replica set to it. In particular, the directory as derived from the
     * replica set name and the HTTP port must not be used by any other application already deployed on that host.
     * The replica process is registered with the {@code replicaSet}'s public target group.
     * @param optionalIgtimiRiotPort TODO
     */
    <AppConfigBuilderT extends Builder<AppConfigBuilderT, String>,
     MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT>>
    SailingAnalyticsProcess<String> deployReplicaToExistingHost(
                    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
                    SailingAnalyticsHost<String> hostToDeployTo, String optionalKeyName,
                    byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
                    Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort)
                    throws Exception;

    /**
     * In the {@code region} specified, searches through all hosts tagged with the
     * {@link SharedLandscapeConstants#SAILING_ANALYTICS_APPLICATION_HOST_TAG} tag set to
     * {@link SharedLandscapeConstants#MULTI_PROCESS_INSTANCE_TAG_VALUE} for hosts that are
     * {@link AwsApplicationReplicaSet#isEligibleForDeployment(com.sap.sse.landscape.aws.ApplicationProcessHost, Optional, Optional, byte[])
     * eligible} for receiving a deployment of a process that belongs to the {@code replicaSet}. This could be a master
     * or a replica; both will require the same set of resources that the eligibility check is looking for: the HTTP
     * port and the server directory must be available on the host.
     * 
     * @return the hosts eligible for receiving a process deployment for the {@code replicaSet}.
     */
    Iterable<SailingAnalyticsHost<String>> getEligibleSharedHostsForReplicaSet(AwsRegion region,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase);

    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> getApplicationReplicaSet(
            AwsRegion region, String replicaSetName, Long optionalTimeoutInMilliseconds, String optionalKeyName,
            byte[] passphraseForPrivateKeyDecryption) throws Exception;
    
    /**
     * Creates a new empty multi-server instance. The region must be specified; instance type and availability zone
     * may be specified, and so may the server name used for the "Name" tag.
     */
    <BuilderT extends StartMultiServer.Builder<BuilderT, String>>
    SailingAnalyticsHost<String> createEmptyMultiServer(AwsRegion region, Optional<InstanceType> instanceType,
            Optional<AwsAvailabilityZone> availabilityZone, Optional<String> name, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * Updates the AMI to use in the launch template version of those of the {@code replicaSets} that have an auto-scaling group.
     * Any running replica will not be affected by this. Only new replicas will be launched based on the AMI specified.
     * @param replicaSets
     *            those without an auto-scaling group won't be affected
     * @param optionalAmi
     *            defaults to the latest image of type {@link SharedLandscapeConstants#IMAGE_TYPE_TAG_VALUE_SAILING}
     * @param optionalTimeout TODO
     * @param optionalKeyName TODO
     * @param privateKeyEncryptionPassphrase TODO
     * 
     * @return those replica sets that were updated according to this request; those from {@code replicaSets} not part
     *         of this result have not had their AMI upgraded, probably because we didn't find an auto-scaling group and
     *         hence no launch template version to update
     */
    Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> updateImageForReplicaSets(AwsRegion region,
            Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> replicaSets,
            Optional<AmazonMachineImage<String>> optionalAmi, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * For an existing replica set with an {@link AwsApplicationReplicaSet#getAutoScalingGroup() auto-scaling group}
     * ensures that the auto-scaling group has a minimum size of 1 and waits until an auto-scaling replica is available.
     * Then, all replicas not managed by the auto-scaling group are stopped one by one.
     * <p>
     * 
     * If the replica set does not have an auto-scaling group assigned, no action is taken.
     * 
     * @return the updated replica set
     */
    <AppConfigBuilderT extends Builder<AppConfigBuilderT, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT>>
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> useDedicatedAutoScalingReplicasInsteadOfShared(
                    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
                    String optionalKeyName, byte[] privateKeyEncryptionPassphrase)
                    throws Exception;

    /**
     * For an existing replica set ensures that there is a replica running and ready that is not an auto-scaling replica
     * on a dedicated instance. If no such replica process is found, one is launched on a shared instance in an
     * availability zone different from the one hosting the replica set's master instance. Once the non-auto-scaling
     * replica is ready, the auto-scaling group's {@link AutoScalingGroup#minSize() minimum size} is reduced to 0 so
     * that with no excess workload the auto-scaling group will terminate all auto-scaling group-managed instances.<p>
     * 
     * Should a new shared instance be required for a new replica, its instance type is obtained from the one hosting
     * the replica set's master process, silently assuming that it may already be on a shared set-up.
     */
    <AppConfigBuilderT extends Builder<AppConfigBuilderT, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsReplicaConfiguration<String>, AppConfigBuilderT>>
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> useSingleSharedInsteadOfDedicatedAutoScalingReplica(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            Optional<InstanceType> optionalInstanceType) throws Exception;

    /**
     * If a non-{@code null}, non-{@link String#isEmpty() empty} bearer token is provided by the
     * {@code optionalBearerTokenOnNull} parameter, it is returned unchanged; otherwise, the bearer token as obtained
     * for the current session's principal is returned. See also {@link SecurityService#getOrCreateAccessToken(String)}.
     */
    String getEffectiveBearerToken(String replicaReplicationBearerToken);

    /**
     * Moves a replica set's master process to another instance. For this, all {@link ApplicationReplicaSet#getReplicas() replicas}
     * will stop replicating the current master and the current master is de-registered from the two target groups.
     * Then, if necessary, a new (shared or dedicated) instance is launched if no existing eligible shared instance is found
     * in case {@code useSharedInstance} is {@code true}. The new master process is deployed and launched on the instance
     * and if ready will be registered with the two target groups again. Then, all replicas are re-started in place one
     * after the other, always keeping the master and n-1 replicas available for reading if n was the original number of
     * replicas in the set at the time the method was called.
     * 
     * @param useSharedInstance
     *            if {@code true}, a shared instance that is eligible to host the {@code replicaSet}'s master is
     *            determined (shared-instance replicas running in different AZ or auto-scaling replica exists), and if
     *            not found, a new shared instance is launched using the {@code optionalInstanceType}, defaulting to
     *            {@link SharedLandscapeConstants#DEFAULT_SHARED_INSTANCE_TYPE_NAME}. If {@code useSharedInstance} is
     *            {@code false}, a new dedicated instance is launched, using {@code optionalInstanceType}, defaulting to
     *            {@link SharedLandscapeConstants#DEFAULT_DEDICATED_INSTANCE_TYPE_NAME}.
     * @param optionalInstanceType
     *            used to control the type of instance to launch; depending on {@code useSharedInstance}, this may be
     *            used to launch a dedicated or a shared instance. The default for the two cases are
     *            {@link SharedLandscapeConstants#DEFAULT_DEDICATED_INSTANCE_TYPE_NAME} and
     *            {@link SharedLandscapeConstants#DEFAULT_SHARED_INSTANCE_TYPE_NAME}, respectively.
     * @param optionalPreferredInstanceToDeployTo
     *            can be used if {@code useSharedInstance} is {@code true} to specify a preferred shared instance to
     *            deploy the new master process to. The instance will be checked for eligibility first, including
     *            checking the AZ, and if not eligible the method behaves as if the instance had not been specified.
     */
    <AppConfigBuilderT extends SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String, SailingAnalyticsHost<String>, SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT>>
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> moveMasterToOtherInstance(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            boolean useSharedInstance, Optional<InstanceType> instanceType,
            Optional<SailingAnalyticsHost<String>> optionalPreferredInstanceToDeployTo, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, String optionalMasterReplicationBearerTokenOrNull,
            String optionalReplicaReplicationBearerTokenOrNull, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull) throws MalformedURLException, IOException, TimeoutException,
            InterruptedException, ExecutionException, Exception;

    /**
     * If the {@code replicaSet} provided has one or more auto-scaling groups, their default launch template version is
     * adjusted such that it matches the {@code optionalInstanceType}. The existing replicas managed currently by the
     * auto-scaling group are replaced one by one with new instances with the new configuration. This happens by setting
     * the auto-scaling group's new minimum size to the current number of instances managed by the auto-scaling group
     * plus one, then waiting for the new instance to become available, and then terminating one of the old auto-scaling
     * group managed replicas, again waiting for the one next new replica to become ready, and so on, until the last old
     * auto-scaling replica has been stopped/terminated. Then, the auto-scaling group's minimum size is reset to what it
     * was when this method was called.
     */
    AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> changeAutoScalingReplicasInstanceType(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            InstanceType instanceType, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    <ShardingKey> boolean isEligibleForDeployment(SailingAnalyticsHost<ShardingKey> host, String serverName, int port, Integer optionalIgtimiRiotPort,
            Optional<Duration> waitForProcessTimeout, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    SailingServer getSailingServer(String hostname, String username, String password, Optional<Integer> port)
            throws MalformedURLException;

    SailingServer getSailingServer(String hostname, String bearertoken, Optional<Integer> port)
            throws MalformedURLException;
    
    void removeShardingKeysFromShard(Iterable<String> selectedleaderboards, 
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            AwsRegion region,String shardName, String bearertoken) throws Exception;
    
    public void appendShardingKeysToShard(Iterable<String> selectedLeaderboards,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet,
            AwsRegion region, String shardName, String bearertoken) throws Exception;
    
    void removeShard(AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet, String shardTargetGroupArn) throws Exception;
    
    void addShard(Iterable<String> selectedLeaderboardNames, 
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet, 
            AwsRegion region, String bearertoken, String shardName) throws Exception;

    /**
     * Removes all application processes {@link SailingAnalyticsHost#getApplicationProcesses(Optional, Optional, byte[])
     * found running} on {@code host} and deploys them to another host in {@code host}'s availability zone. The default
     * configuration for primaries and replicas is used based on the application replica set the processes belong to,
     * except for the memory configuration which is copied from the processes running on {@code host}. This will mean
     * that any hand-crafted special configuration will get lost during the process. So don't apply this operation to
     * hosts running non-standard application processes with non-default configurations.
     * <p>
     * 
     * For those processes that are the primary ("master") instance of their replica set this method ensures that there
     * is at least one healthy replica available before moving the primary instance to the new host. When moving a
     * primary process, it is first removed from the "-m" target group, the new process is launched on the new host, and
     * when it is healthy it is added to the "-m" target group.
     * <p>
     * 
     * Moving a replica is easier: the replica can be launched on the new host first, added to the public target group
     * when healthy, and then the replica on the old {@code host} can be terminated and removed.
     * 
     * @param host
     *            must be a "multi-instance" host intended for sharing; this must be indicated by the tag value
     *            {@link SharedLandscapeConstants#MULTI_PROCESS_INSTANCE_TAG_VALUE "___multi___"} on the
     *            {@code sailing-analytics-server} tag of the instance. Otherwise, the method will throw an
     *            {@link IllegalStateException}.
     * @param optionalInstanceTypeForNewInstance
     *            if not specified, the new multi-instance launched will use the same instance type as the one from
     *            where the processes are moved away ({@code host})
     * @return a triple of which the {@link Triple#getA() first} element is the new host to which the processes have
     *         been moved, the {@link Triple#getB() second} element is the set of master processes moved, and the
     *         {@link Triple#getC() third} element is the set of replica processes moved; the master and replica process
     *         maps are keyed by the names of the application replica sets to which the processes belong.
     */
    Triple<SailingAnalyticsHost<String>, Map<String, SailingAnalyticsProcess<String>>, Map<String, SailingAnalyticsProcess<String>>>
    moveAllApplicationProcessesAwayFrom(SailingAnalyticsHost<String> host,
            Optional<InstanceType> optionalInstanceTypeForNewInstance,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    String getHostname(String replicaSetName, String optionalDomainName);
}
