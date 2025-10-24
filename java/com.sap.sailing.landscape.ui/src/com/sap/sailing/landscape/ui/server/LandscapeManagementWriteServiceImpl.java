package com.sap.sailing.landscape.ui.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.landscape.LandscapeService;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.SailingReleaseRepository;
import com.sap.sailing.landscape.common.RemoteServiceMappingConstants;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.impl.SailingAnalyticsHostImpl;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sailing.landscape.procedures.DeployProcessOnMultiServer;
import com.sap.sailing.landscape.procedures.SailingAnalyticsHostSupplier;
import com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration;
import com.sap.sailing.landscape.procedures.SailingAnalyticsProcessFactory;
import com.sap.sailing.landscape.procedures.UpgradeAmi;
import com.sap.sailing.landscape.ui.client.LandscapeManagementWriteService;
import com.sap.sailing.landscape.ui.impl.Activator;
import com.sap.sailing.landscape.ui.shared.AmazonMachineImageDTO;
import com.sap.sailing.landscape.ui.shared.AvailabilityZoneDTO;
import com.sap.sailing.landscape.ui.shared.AwsInstanceDTO;
import com.sap.sailing.landscape.ui.shared.AwsShardDTO;
import com.sap.sailing.landscape.ui.shared.CompareServersResultDTO;
import com.sap.sailing.landscape.ui.shared.LeaderboardNameDTO;
import com.sap.sailing.landscape.ui.shared.MongoEndpointDTO;
import com.sap.sailing.landscape.ui.shared.MongoProcessDTO;
import com.sap.sailing.landscape.ui.shared.MongoScalingInstructionsDTO;
import com.sap.sailing.landscape.ui.shared.ProcessDTO;
import com.sap.sailing.landscape.ui.shared.ReleaseDTO;
import com.sap.sailing.landscape.ui.shared.ReverseProxyDTO;
import com.sap.sailing.landscape.ui.shared.SSHKeyPairDTO;
import com.sap.sailing.landscape.ui.shared.SailingAnalyticsProcessDTO;
import com.sap.sailing.landscape.ui.shared.SailingApplicationReplicaSetDTO;
import com.sap.sailing.landscape.ui.shared.SerializationDummyDTO;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sailing.server.gateway.interfaces.SailingServer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.server.ResultCachingProxiedRemoteServiceServlet;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.AwsShard;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.LandscapeConstants;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.common.shared.PlainRedirectDTO;
import com.sap.sse.landscape.aws.common.shared.RedirectDTO;
import com.sap.sse.landscape.aws.impl.ApacheReverseProxy;
import com.sap.sse.landscape.aws.impl.AwsAvailabilityZoneImpl;
import com.sap.sse.landscape.aws.impl.AwsInstanceImpl;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.orchestration.CreateDNSBasedLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.CreateDynamicLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.CreateLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.StartMongoDBServer;
import com.sap.sse.landscape.common.shared.SecuredLandscapeTypes;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.landscape.mongodb.MongoProcess;
import com.sap.sse.landscape.mongodb.MongoProcessInReplicaSet;
import com.sap.sse.landscape.mongodb.MongoReplicaSet;
import com.sap.sse.landscape.ssh.SSHKeyPair;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.ui.server.SecurityDTOUtil;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sap.sse.util.ThreadPoolUtil;

import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TagDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealth;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

public class LandscapeManagementWriteServiceImpl extends ResultCachingProxiedRemoteServiceServlet
        implements LandscapeManagementWriteService {
    private static final long serialVersionUID = -3332717645383784425L;
    private static final Logger logger = Logger.getLogger(LandscapeManagementWriteServiceImpl.class.getName());
    
    private static final Optional<Duration> IMAGE_UPGRADE_TIMEOUT = Optional.of(Duration.ONE_MINUTE.times(10));
    
    private final FullyInitializedReplicableTracker<SecurityService> securityServiceTracker;
    
    private final ServiceTracker<LandscapeService, LandscapeService> landscapeServiceTracker;

    public <ShardingKey, MetricsT extends ApplicationProcessMetrics,
    ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>> LandscapeManagementWriteServiceImpl() {
        BundleContext context = Activator.getContext();
        securityServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, SecurityService.class);
        landscapeServiceTracker = ServiceTrackerFactory.createAndOpen(context, LandscapeService.class);
    }
    
    protected SecurityService getSecurityService() {
        try {
            return securityServiceTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected LandscapeService getLandscapeService() {
        try {
            return landscapeServiceTracker.waitForService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * For a combination of an AWS access key ID, the corresponding secret plus an MFA token code produces new session
     * credentials and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. The current user must have the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    @Override
    public void createMfaSessionCredentials(String awsAccessKey, String awsSecret, String mfaTokenCode) {
        checkLandscapeManageAwsPermission();
        getLandscapeService().createMfaSessionCredentials(awsAccessKey, awsSecret, mfaTokenCode);
    }
    
    /**
     * For a combination of an AWS access key ID, the corresponding secret plus an MFA token code produces new session
     * credentials and stores them in the user's preference store from where they can be obtained again using
     * {@link #getSessionCredentials()}. Any session credentials previously stored in the current user's preference store
     * will be overwritten by this. The current user must have the {@code LANDSCAPE:MANAGE:AWS} permission.
     */
    @Override
    public void createSessionCredentials(String awsAccessKey, String awsSecret, String awsSessionToken) {
        checkLandscapeManageAwsPermission();
        getLandscapeService().createSessionCredentials(awsAccessKey, awsSecret, awsSessionToken);
    }
    
    /**
     * For the current user who has to have the {@code LANDSCAPE:MANAGE:AWS} permission, clears the preference in the
     * user's preference store which holds any session credentials created previously using
     * {@link #createMfaSessionCredentials(String, String, String)}.
     */
    @Override
    public void clearSessionCredentials() {
        checkLandscapeManageAwsPermission();
        getLandscapeService().clearSessionCredentials();
    }

    @Override
    public boolean hasValidSessionCredentials() {
        return getLandscapeService().hasValidSessionCredentials();
    }

    private void checkLandscapeManageAwsPermission() {
        SecurityUtils.getSubject().checkPermission(SecuredLandscapeTypes.LANDSCAPE.getStringPermissionForTypeRelativeIdentifier(SecuredLandscapeTypes.LandscapeActions.MANAGE,
                new TypeRelativeObjectIdentifier("AWS")));
    }
    
    @Override
    public ArrayList<String> getRegions() {
        checkLandscapeManageAwsPermission();
        final ArrayList<String> result = new ArrayList<>();
        Util.addAll(Util.map(AwsLandscape.obtain(RemoteServiceMappingConstants.pathPrefixForShardingKey).getRegions(), r->r.getId()), result);
        return result;
    }

    private static final Set<InstanceType> INSTANCE_TYPES_BANNED_FROM_INSTANCE_BASE_NLB_TARGET_GROUPS_AS_SET =
            new HashSet<>(Arrays.asList(LandscapeConstants.INSTANCE_TYPES_BANNED_FROM_INSTANCE_BASED_NLB_TARGET_GROUPS)); 

    @Override
    public ArrayList<String> getInstanceTypeNames(boolean canBeDeployedInNlbInstanceBasedTargetGroup) {
        final ArrayList<String> result = new ArrayList<>();
        Util.addAll(
            Util.map(
                // if deployment to NLB instance-based target group is to be possible, remove those
                // instance types that are banned from those sorts of target groups
                Util.filter(Arrays.asList(InstanceType.values()), instanceType->
                        !canBeDeployedInNlbInstanceBasedTargetGroup ||
                        !INSTANCE_TYPES_BANNED_FROM_INSTANCE_BASE_NLB_TARGET_GROUPS_AS_SET.contains(instanceType)),
                instanceType->instanceType.name()), result);
        return result;
    }
    
    @Override
    public ArrayList<MongoEndpointDTO> getMongoEndpoints(String region) throws MalformedURLException, IOException, URISyntaxException {
        checkLandscapeManageAwsPermission();
        final ArrayList<MongoEndpointDTO> result = new ArrayList<>();
        for (final MongoEndpoint mongoEndpoint : getLandscape().getMongoEndpoints(new AwsRegion(region, getLandscape()))) {
            final MongoEndpointDTO dto;
            if (mongoEndpoint.isReplicaSet()) {
                final MongoReplicaSet replicaSet = mongoEndpoint.asMongoReplicaSet();
                final List<MongoProcessDTO> hostnamesAndPorts = new ArrayList<>();
                for (final MongoProcessInReplicaSet process : replicaSet.getInstances()) {
                    hostnamesAndPorts.add(convertToMongoProcessDTO(process, replicaSet.getName()));
                }
                dto = new MongoEndpointDTO(replicaSet.getName(), hostnamesAndPorts);
            } else {
                final MongoProcess mongoProcess = mongoEndpoint.asMongoProcess();
                dto = new MongoEndpointDTO(/* no replica set */ null, Collections.singleton(convertToMongoProcessDTO(mongoProcess, /* replicaSetName */ null)));
            }
            result.add(dto);
        }
        return result;
    }
    
    @Override
    public ArrayList<AvailabilityZoneDTO> describeAvailabilityZones(String region) {
        final ArrayList<AvailabilityZoneDTO> availabilityZones = new ArrayList<>();
        getLandscape()
                .getAvailabilityZones(new AwsRegion(region, getLandscape()),
                        Optional.of(getLandscape().getDefaultSecurityGroupForApplicationLoadBalancer(
                                new AwsRegion(region, getLandscape())).getVpcId()))
                .forEach(az -> availabilityZones.add(new AvailabilityZoneDTO(az.getName(), region, az.getId())));
        return availabilityZones;
    }
    
    @Override
    public ArrayList<ReverseProxyDTO> getReverseProxies(String region) throws Exception  {
        checkLandscapeManageAwsPermission();
        final AwsLandscape<String> landscape = getLandscape();
        TargetGroup<String> targetGroupInQuestion = null;
        for (TargetGroup<String> targetGroup : landscape.getTargetGroups(new AwsRegion(region, landscape))) {
            for (TagDescription description : targetGroup.getTagDescriptions()) { // There should only be 1 tag
                                                                                  // description.
                if (!description.tags().isEmpty()) {
                    for (Tag tag : description.tags()) {
                        if (tag.key().equals(LandscapeConstants.ALL_REVERSE_PROXIES)
                                && targetGroup.getLoadBalancerArn() != null
                                && !targetGroup.getLoadBalancerArn().contains(LandscapeConstants.NLB_ARN_CONTAINS)) {
                            targetGroupInQuestion = targetGroup;
                        }
                    }
                }
            }
        }
        Map<AwsInstance<String>, TargetHealth> healths = null;
        if (targetGroupInQuestion != null) {
            healths = landscape.getTargetHealthDescriptions(targetGroupInQuestion);
        }
        final ArrayList<ReverseProxyDTO> results = new ArrayList<>();
        for (AwsInstance<String> instance : landscape.getReverseProxyCluster(new AwsRegion(region, landscape))
                .getHosts()) {
            boolean isDisposable = landscape.getTag(instance, LandscapeConstants.DISPOSABLE_PROXY).isPresent() ? true : false;
            ReverseProxyDTO dto = convertToReverseProxyDTO(region, healths, instance, isDisposable);
            results.add(dto);
        }
        return results;
    }

    private ReverseProxyDTO convertToReverseProxyDTO(String region, Map<AwsInstance<String>, TargetHealth> healths,
            AwsInstance<String> instance, boolean isDisposable) {
        return new ReverseProxyDTO(instance.getInstanceId(),
                instance.getPrivateAddress().getHostAddress(), instance.getPublicAddress().getHostAddress(),
                region, instance.getLaunchTimePoint(), instance.isSharedHost(),
                instance.getNameTag(), instance.getImageId(), extractHealth(healths, instance),
                isDisposable, new AvailabilityZoneDTO(instance.getAvailabilityZone().getName(), instance.getRegion().getId(), instance.getAvailabilityZone().getId()));
    }
    
    @Override
    public void rotateHttpdLogs(ReverseProxyDTO instanceDTO, String region, String optionalKeyName,
            byte[] passphraseForPrivateKeyDecryption) throws Exception {
        checkLandscapeManageAwsPermission();
        AwsInstance<String> awsInstance = getLandscape().getHostByInstanceId(new AwsRegion(region, getLandscape()),
                instanceDTO.getInstanceId(), AwsInstanceImpl::new);
        new ApacheReverseProxy<>(getLandscape(), awsInstance).rotateLogs(Optional.ofNullable(optionalKeyName),
                passphraseForPrivateKeyDecryption);
    }
    
    private String extractHealth(Map<AwsInstance<String>, TargetHealth> healths, AwsInstance<String> instance) {
        final String NO_HEALTH_VALUE_FOUND_MSG = "No health value found";
        final String health_message;
        if (healths == null) {
            health_message = NO_HEALTH_VALUE_FOUND_MSG;
        } else {
            TargetHealth health = healths.get(instance);
            if (health != null) {
                health_message = health.state().toString();
            } else {
                health_message = NO_HEALTH_VALUE_FOUND_MSG;
            }
        }
        return health_message;
    }
    
    @Override
    public void removeReverseProxy(ReverseProxyDTO proxy, String region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        checkLandscapeManageAwsPermission();
        AwsRegion awsRegion = new AwsRegion(region, getLandscape());
        AwsInstance<String> awsInstance = getLandscape().getHostByInstanceId(awsRegion, proxy.getInstanceId(),
                AwsInstanceImpl::new);
        getLandscape().getReverseProxyCluster(awsRegion).removeHost(awsInstance, Optional.of(optionalKeyName), privateKeyEncryptionPassphrase);
    }
    
    @Override
    public void addReverseProxy(String instanceName, String instanceType,  String region, String launchKey, AvailabilityZoneDTO availabilityZoneDTO) {
        try {
            getLandscape().getReverseProxyCluster(new AwsRegion(region, getLandscape())).createHost(instanceName,
                    InstanceType.valueOf(instanceType),
                    new AwsAvailabilityZoneImpl(availabilityZoneDTO.getAzId(), availabilityZoneDTO.getAzName(), new AwsRegion(region, getLandscape())), launchKey);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        
    }
    
    private MongoEndpoint getMongoEndpoint(MongoEndpointDTO mongoEndpointDTO) {
        final MongoEndpoint result;
        if (mongoEndpointDTO == null) {
            result = null;
        } else {
            final HostSupplier<String, SailingAnalyticsHost<String>> hostSupplier = new SailingAnalyticsHostSupplier<>();
            final Set<Pair<AwsInstance<String>, Integer>> nodes = new HashSet<>();
            for (final MongoProcessDTO node : mongoEndpointDTO.getHostnamesAndPorts()) {
                nodes.add(new Pair<>(getLandscape().getHostByInstanceId(new AwsRegion(node.getHost().getRegion(), getLandscape()), node.getHost().getInstanceId(), hostSupplier), node.getPort()));
            }
            if (mongoEndpointDTO.getReplicaSetName() == null) {
                // single node:
                final Pair<AwsInstance<String>, Integer> hostAndPort = nodes.iterator().next();
                result = getLandscape().getDatabaseConfigurationForSingleNode(hostAndPort.getA(), hostAndPort.getB());
            } else {
                // replica set
                result = getLandscape().getDatabaseConfigurationForReplicaSet(mongoEndpointDTO.getReplicaSetName(), nodes);
            }
        }
        return result;
    }
    
    private MongoProcessDTO convertToMongoProcessDTO(MongoProcess mongoProcess, String replicaSetName) throws MalformedURLException, IOException, URISyntaxException {
        return new MongoProcessDTO(convertToAwsInstanceDTO(mongoProcess.getHost()), mongoProcess.getPort(), mongoProcess.getHostname(Landscape.WAIT_FOR_PROCESS_TIMEOUT),
                replicaSetName, mongoProcess.getURI(/* no specific DB */ Optional.empty(), Landscape.WAIT_FOR_PROCESS_TIMEOUT).toString());
    }

    private AwsInstanceDTO convertToAwsInstanceDTO(Host host) {
        return new AwsInstanceDTO(host.getId().toString(), host.getPrivateAddress().getHostAddress(), host.getPublicAddress() == null ? null : host.getPublicAddress().getHostAddress(),
                host.getRegion().getId(),
                host.getLaunchTimePoint(), host.isSharedHost(), new AvailabilityZoneDTO(host.getAvailabilityZone().getName(), host.getRegion().getId(), host.getAvailabilityZone().getId()));
    }
    
    @Override
    public ArrayList<SailingApplicationReplicaSetDTO<String>> getApplicationReplicaSets(String regionId,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final ArrayList<SailingApplicationReplicaSetDTO<String>> result = new ArrayList<>();
        final AwsRegion region = new AwsRegion(regionId, getLandscape());
        final HostSupplier<String, SailingAnalyticsHost<String>> hostSupplier = new SailingAnalyticsHostSupplier<>();
        final Map<Future<SailingApplicationReplicaSetDTO<String>>, AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> resultFutures = new HashMap<>();
        final ScheduledExecutorService backgroundThreadPool = ThreadPoolUtil.INSTANCE.createBackgroundTaskThreadPoolExecutor("Constructing SailingApplicationReplicaSetDTOs "+UUID.randomUUID());
        for (final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationServerReplicaSet :
            getLandscape().getApplicationReplicaSetsByTag(region, SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG,
                hostSupplier, Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase)) {
            resultFutures.put(backgroundThreadPool.submit(()->
                convertToSailingApplicationReplicaSetDTO(applicationServerReplicaSet, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase)), applicationServerReplicaSet);
        }
        Util.addAll(Util.filter(Util.map(resultFutures.keySet(), future->{
            try {
                return future.get(Landscape.WAIT_FOR_PROCESS_TIMEOUT.get().asMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.log(Level.WARNING, "Problem waiting for a replica set "+resultFutures.get(future)+"; ignoring that replica set", e);
                return null;
            }
        }), r->r!=null), result);
        backgroundThreadPool.shutdown();
        return result;
    }

    private SailingApplicationReplicaSetDTO<String> convertToSailingApplicationReplicaSetDTO(
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationServerReplicaSet,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return new SailingApplicationReplicaSetDTO<>(applicationServerReplicaSet.getName(),
                convertToSailingAnalyticsProcessDTO(applicationServerReplicaSet.getMaster(), optionalKeyName, privateKeyEncryptionPassphrase),
                Util.map(applicationServerReplicaSet.getReplicas(), r->{
                    try {
                        return convertToSailingAnalyticsProcessDTO(r, optionalKeyName, privateKeyEncryptionPassphrase);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }),
                applicationServerReplicaSet.getVersion(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase).getName(),
                applicationServerReplicaSet.getHostname(), getLandscapeService().getDefaultRedirectPath(applicationServerReplicaSet.getDefaultRedirectRule()),
                applicationServerReplicaSet.getAutoScalingGroup() == null ? null :
                    applicationServerReplicaSet.getAutoScalingGroup().getLaunchTemplateDefaultVersion() == null ? null :
                        applicationServerReplicaSet.getAutoScalingGroup().getLaunchTemplateDefaultVersion().launchTemplateData().imageId());
    }
    
    private SailingAnalyticsProcessDTO convertToSailingAnalyticsProcessDTO(SailingAnalyticsProcess<String> sailingAnalyticsProcess,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return new SailingAnalyticsProcessDTO(convertToAwsInstanceDTO(sailingAnalyticsProcess.getHost()),
                sailingAnalyticsProcess.getPort(), sailingAnalyticsProcess.getHostname(),
                sailingAnalyticsProcess.getRelease(SailingReleaseRepository.INSTANCE, Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase).getName(),
                sailingAnalyticsProcess.getTelnetPortToOSGiConsole(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase),
                sailingAnalyticsProcess.getServerName(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase),
                sailingAnalyticsProcess.getServerDirectory(Landscape.WAIT_FOR_PROCESS_TIMEOUT),
                sailingAnalyticsProcess.getExpeditionUdpPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase),
                sailingAnalyticsProcess.getIgtimiRiotPort(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase),
                sailingAnalyticsProcess.getStartTimePoint(Landscape.WAIT_FOR_PROCESS_TIMEOUT));
    }

    private AwsLandscape<String> getLandscape() {
        checkLandscapeManageAwsPermission();
        return getLandscapeService().getLandscape();
    }
    
    @Override
    public Boolean verifyPassphrase(String regionId, SSHKeyPairDTO key, String privateKeyEncryptionPassphrase) {
        final JSch jsch = new JSch();
        final boolean res;
        if (key == null) {
            res = false;
        } else {
            final SSHKeyPair keypair = getLandscape().getSSHKeyPair(new AwsRegion(regionId, getLandscape()),
                    key.getName());
            if (keypair == null) {
                res = false;
            } else {
                res = keypair.checkPassphrase(jsch, privateKeyEncryptionPassphrase.getBytes());
            }
        }
        return res;
    }

    @Override
    public MongoEndpointDTO getMongoEndpoint(String region, String replicaSetName) throws MalformedURLException, IOException, URISyntaxException {
        return getMongoEndpoints(region).stream().filter(mep->Util.equalsWithNull(mep.getReplicaSetName(), replicaSetName)).findAny().orElse(null);
    }
    
    @Override
    public SSHKeyPairDTO generateSshKeyPair(String regionId, String keyName, String privateKeyEncryptionPassphrase) {
        final Subject subject = SecurityUtils.getSubject();
        final SSHKeyPair dummyKeyPairForSecurityCheck = new SSHKeyPair(regionId, subject.getPrincipal().toString(), 
                TimePoint.now(), keyName, /* publicKey */ null, /* encryptedPrivateKey */ null);
        final SSHKeyPair keyPair = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(dummyKeyPairForSecurityCheck.getPermissionType(),
                dummyKeyPairForSecurityCheck.getIdentifier().getTypeRelativeObjectIdentifier(), keyName,
                        ()->{
                            return getLandscape()
                                    .createKeyPair(new AwsRegion(regionId, getLandscape()), keyName, privateKeyEncryptionPassphrase.getBytes());
                });
        return convertToSSHKeyPairDTO(keyPair);
    }
   
    @Override
    public SSHKeyPairDTO addSshKeyPair(String regionId, String keyName,
            String publicKey, String encryptedPrivateKey) throws JSchException {
        final Subject subject = SecurityUtils.getSubject();
        final SSHKeyPair dummyKeyPairForSecurityCheck = new SSHKeyPair(regionId, subject.getPrincipal().toString(), 
                TimePoint.now(), keyName, /* publicKey */ null, /* encryptedPrivateKey */ null);
        final SSHKeyPair keyPair = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(dummyKeyPairForSecurityCheck.getPermissionType(),
                dummyKeyPairForSecurityCheck.getIdentifier().getTypeRelativeObjectIdentifier(), keyName,
                        ()->{
                            return getLandscape()
                                    .importKeyPair(new AwsRegion(regionId, getLandscape()), publicKey.getBytes(), encryptedPrivateKey.getBytes(), keyName);
                });
        return convertToSSHKeyPairDTO(keyPair);
    }
   
    private SSHKeyPairDTO convertToSSHKeyPairDTO(SSHKeyPair keyPair) {
        final SSHKeyPairDTO result = new SSHKeyPairDTO(keyPair.getRegionId(), keyPair.getName(), keyPair.getCreatorName(), keyPair.getCreationTime());
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), result);
        return result;
    }

    @Override
    public ArrayList<SSHKeyPairDTO> getSshKeys(String regionId) {
        final ArrayList<SSHKeyPairDTO> result = new ArrayList<>();
        final AwsLandscape<String> landscape = getLandscape();
        final AwsRegion region = new AwsRegion(regionId, landscape);
        for (final KeyPairInfo keyPairInfo : landscape.getAllKeyPairInfos(region)) {
            final SSHKeyPair key = landscape.getSSHKeyPair(region, keyPairInfo.keyName());
            if (key != null && SecurityUtils.getSubject().isPermitted(key.getIdentifier().getStringPermission(DefaultActions.READ))) {
                final SSHKeyPairDTO sshKeyPairDTO = new SSHKeyPairDTO(key.getRegionId(), key.getName(), key.getCreatorName(), key.getCreationTime());
                SecurityDTOUtil.addSecurityInformation(getSecurityService(), sshKeyPairDTO);
                result.add(sshKeyPairDTO);
            }
        }
        return result;
    }

    @Override
    public void removeSshKey(SSHKeyPairDTO keyPair) {
        getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(keyPair,
            ()->getLandscape().deleteKeyPair(new AwsRegion(keyPair.getRegionId(), getLandscape()), keyPair.getName()));
    }
    
    @Override
    public byte[] getEncryptedSshPrivateKey(String regionId, String keyName) throws JSchException {
        final AwsLandscape<String> landscape = AwsLandscape.obtain(RemoteServiceMappingConstants.pathPrefixForShardingKey);
        final SSHKeyPair keyPair = landscape.getSSHKeyPair(new AwsRegion(regionId, landscape), keyName);
        getSecurityService().checkCurrentUserReadPermission(keyPair);
        return keyPair.getEncryptedPrivateKey();
    }

    @Override
    public byte[] getSshPublicKey(String regionId, String keyName) throws JSchException {
        final AwsLandscape<String> landscape = AwsLandscape.obtain(RemoteServiceMappingConstants.pathPrefixForShardingKey);
        final SSHKeyPair keyPair = landscape.getSSHKeyPair(new AwsRegion(regionId, landscape), keyName);
        getSecurityService().checkCurrentUserReadPermission(keyPair);
        return keyPair.getPublicKey();
    }

    @Override
    public ArrayList<AmazonMachineImageDTO> getAmazonMachineImages(String region) {
        checkLandscapeManageAwsPermission();
        final ArrayList<AmazonMachineImageDTO> result = new ArrayList<>();
        final AwsRegion awsRegion = new AwsRegion(region, getLandscape());
        final AwsLandscape<String> landscape = getLandscape();
        for (final String imageType : landscape.getMachineImageTypes(awsRegion)) {
            for (final AmazonMachineImage<String> machineImage : landscape.getAllImagesWithType(awsRegion, imageType)) {
                final AmazonMachineImageDTO dto = new AmazonMachineImageDTO(machineImage.getId(),
                        machineImage.getRegion().getId(), machineImage.getName(), imageType, machineImage.getState().name(),
                        machineImage.getCreatedAt());
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    public void removeAmazonMachineImage(String region, String machineImageId) {
        checkLandscapeManageAwsPermission();
        final AwsLandscape<String> landscape = getLandscape();
        final AmazonMachineImage<String> ami = landscape.getImage(new AwsRegion(region, landscape), machineImageId);
        ami.delete();
    }

    @Override
    public AmazonMachineImageDTO upgradeAmazonMachineImage(String region, String machineImageId) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsLandscape<String> landscape = getLandscape();
        final AwsRegion awsRegion = new AwsRegion(region, landscape);
        final AmazonMachineImage<String> ami = landscape.getImage(awsRegion, machineImageId);
        final UpgradeAmi.Builder<?, String, SailingAnalyticsProcess<String>> upgradeAmiBuilder = UpgradeAmi.builder();
        upgradeAmiBuilder
            .setLandscape(landscape)
            .setRegion(awsRegion)
            .setMachineImage(ami)
            .setOptionalTimeout(IMAGE_UPGRADE_TIMEOUT);
        final UpgradeAmi<String> upgradeAmi = upgradeAmiBuilder.build();
        upgradeAmi.run();
        final AmazonMachineImage<String> resultingAmi = upgradeAmi.getUpgradedAmi();
        return new AmazonMachineImageDTO(resultingAmi.getId(), resultingAmi.getRegion().getId(), resultingAmi.getName(),
               resultingAmi.getType(), resultingAmi.getState().name(), resultingAmi.getCreatedAt());
    }

    @Override
    public void scaleMongo(String regionId, MongoScalingInstructionsDTO mongoScalingInstructions, String keyName) throws Exception {
        final int WAIT_TIME_FOR_REPLICA_SET_TO_APPLY_CONFIG_CHANCE_IN_MILLIS = 5000;
        checkLandscapeManageAwsPermission();
        final AwsLandscape<String> landscape = getLandscape();
        for (final Iterator<MongoProcessDTO> i=mongoScalingInstructions.getHostnamesAndPortsToShutDown().iterator(); i.hasNext(); ) {
            final ProcessDTO processToShutdown = i.next();
            logger.info("Shutting down MongoDB instance "+processToShutdown.getHost().getInstanceId()+" on behalf of user "+SessionUtils.getPrincipal());
            final AwsRegion region = new AwsRegion(processToShutdown.getHost().getRegion(), landscape);
            final AwsInstance<String> instance = new AwsInstanceImpl<>(processToShutdown.getHost().getInstanceId(),
                    new AwsAvailabilityZoneImpl(processToShutdown.getHost().getAvailabilityZoneId(),
                            processToShutdown.getHost().getAvailabilityZoneName(), region), 
                            InetAddress.getByName(processToShutdown.getHost().getPrivateIpAddress()),
                            processToShutdown.getHost().getLaunchTimePoint(), landscape);
            instance.terminate();
            if (i.hasNext()) {
                Thread.sleep(WAIT_TIME_FOR_REPLICA_SET_TO_APPLY_CONFIG_CHANCE_IN_MILLIS); // give the primary a chance to apply the configuration change before asking for the next configuration change
            }
        }
        if (mongoScalingInstructions.getReplicaSetName() == null) {
            throw new IllegalArgumentException("Can only scale MongoDB Replica Sets, not standalone instances");
        }
        final AwsRegion region = new AwsRegion(regionId, landscape);
        for (int i=0; i<mongoScalingInstructions.getLaunchParameters().getNumberOfInstances(); i++) {
            logger.info("Launching new MongoDB instance of type "+mongoScalingInstructions.getLaunchParameters().getInstanceType()+" on behalf of user "+SessionUtils.getPrincipal());
            final StartMongoDBServer.Builder<?, String, MongoProcessInReplicaSet> startMongoProcessBuilder = StartMongoDBServer.builder();
            final StartMongoDBServer<String, MongoProcessInReplicaSet> startMongoDBServer = startMongoProcessBuilder
                .setLandscape(landscape)
                .setInstanceType(InstanceType.valueOf(mongoScalingInstructions.getLaunchParameters().getInstanceType()))
                .setKeyName(keyName)
                .setRegion(region)
                .setReplicaSetName(mongoScalingInstructions.getReplicaSetName())
                .setReplicaSetPrimary(mongoScalingInstructions.getLaunchParameters().getReplicaSetPrimary())
                .setReplicaSetPriority(mongoScalingInstructions.getLaunchParameters().getReplicaSetPriority())
                .setReplicaSetVotes(mongoScalingInstructions.getLaunchParameters().getReplicaSetVotes())
                .build();
            startMongoDBServer.run();
            if (i<mongoScalingInstructions.getLaunchParameters().getNumberOfInstances()-1) {
                Thread.sleep(WAIT_TIME_FOR_REPLICA_SET_TO_APPLY_CONFIG_CHANCE_IN_MILLIS); // give the primary a chance to apply the configuration change before asking for the next configuration change
            }
        }
    }
    
    @Override
    public SailingApplicationReplicaSetDTO<String> createApplicationReplicaSet(String regionId, String name, boolean sharedMasterInstance,
            String sharedInstanceType, String dedicatedInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String masterReplicationBearerToken,
            String replicaReplicationBearerToken, String optionalDomainName, Integer minimumAutoScalingGroupSizeOrNull,
            Integer maximumAutoScalingGroupSizeOrNull, Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            Integer optionalIgtimiRiotPort)
            throws Exception {
        checkLandscapeManageAwsPermission();
        final Release release = getLandscapeService().getRelease(releaseNameOrNullForLatestMaster);
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result = getLandscapeService()
                .createApplicationReplicaSet(regionId, name, sharedMasterInstance, sharedInstanceType,
                        dedicatedInstanceType, dynamicLoadBalancerMapping, release.getName(), optionalKeyName,
                        privateKeyEncryptionPassphrase, masterReplicationBearerToken, replicaReplicationBearerToken,
                        optionalDomainName, optionalMemoryInMegabytesOrNull,
                        optionalMemoryTotalSizeFactorOrNull,
                        optionalIgtimiRiotPort, Optional.ofNullable(minimumAutoScalingGroupSizeOrNull), Optional.ofNullable(maximumAutoScalingGroupSizeOrNull));
        return new SailingApplicationReplicaSetDTO<String>(result.getName(), convertToSailingAnalyticsProcessDTO(result
                .getMaster(), Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase),
                Util.map(result.getReplicas(), r->{
                    try {
                        return convertToSailingAnalyticsProcessDTO(r, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                }),
                release.getName(),
                getLandscapeService().getFullyQualifiedHostname(name, Optional.ofNullable(optionalDomainName)),
                getLandscapeService().getDefaultRedirectPath(result.getDefaultRedirectRule()),
                result.getAutoScalingGroup()==null?null:result.getAutoScalingGroup().getLaunchTemplateDefaultVersion().launchTemplateData().imageId());
    }

    @Override
    public SailingApplicationReplicaSetDTO<String> deployApplicationToExistingHost(String replicaSetName,
            AwsInstanceDTO hostToDeployTo, String replicaInstanceType, boolean dynamicLoadBalancerMapping,
            String releaseNameOrNullForLatestMaster, String optionalKeyName, byte[] privateKeyEncryptionPassphrase,
            String masterReplicationBearerToken, String replicaReplicationBearerToken, String optionalDomainName,
            Integer optionalMinimumAutoScalingGroupSizeOrNull, Integer optionalMaximumAutoScalingGroupSizeOrNull,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            Integer optionalIgtimiRiotPort, AwsInstanceDTO optionalPreferredInstanceToDeployUnmanagedReplicaTo) throws Exception {
        return deployApplicationToExistingHostInternal(replicaSetName, hostToDeployTo, replicaInstanceType,
                dynamicLoadBalancerMapping, releaseNameOrNullForLatestMaster, optionalKeyName,
                privateKeyEncryptionPassphrase, masterReplicationBearerToken, replicaReplicationBearerToken,
                optionalDomainName, optionalMinimumAutoScalingGroupSizeOrNull,
                optionalMaximumAutoScalingGroupSizeOrNull, optionalMemoryInMegabytesOrNull,
                optionalMemoryTotalSizeFactorOrNull, optionalIgtimiRiotPort, optionalPreferredInstanceToDeployUnmanagedReplicaTo);
    }
    
    /**
     * Starts a first master process of a new replica set whose name is provided by the {@code replicaSetName}
     * parameter. The process is started on the host identified by the {@code hostToDeployTo} parameter. A set of
     * available ports is identified and chosen automatically. The target groups and load balancing set-up is created.
     * The {@code replicaInstanceType} is used to configure the launch template used by the auto-scaling group
     * which is also created so that when dedicated replicas need to be provided during auto-scaling, their instance
     * type is known.
     * <p>
     * 
     * The "internal" method exists in order to declare a few type parameters which wouldn't be possible on the GWT RPC
     * interface method as some of these types are not seen by clients.
     * @param optionalMinimumAutoScalingGroupSize
     *            defaults to 1; if 0, a replica process will be launched on an eligible shared instance in an
     *            availability zone different from that of the instance hosting the master process. Otherwise,
     *            at least one auto-scaling replica will ensure availability of the replica set.
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
        MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String,
        SailingAnalyticsHost<String>,
        SailingAnalyticsMasterConfiguration<String>, AppConfigBuilderT>>
    SailingApplicationReplicaSetDTO<String> deployApplicationToExistingHostInternal(
            String replicaSetName, AwsInstanceDTO hostToDeployToDTO, String replicaInstanceType,
            boolean dynamicLoadBalancerMapping, String releaseNameOrNullForLatestMaster, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase,
            String masterReplicationBearerToken, String replicaReplicationBearerToken, String optionalDomainName, Integer optionalMinimumAutoScalingGroupSizeOrNull,
            Integer optionalMaximumAutoScalingGroupSizeOrNull,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull, Integer optionalIgtimiRiotPort, AwsInstanceDTO optionalPreferredInstanceToDeployUnmanagedReplicaTo)
            throws Exception {
        checkLandscapeManageAwsPermission();
        final Release release = getLandscapeService().getRelease(releaseNameOrNullForLatestMaster);
        final SailingAnalyticsHost<String> hostToDeployTo = getHostFromInstanceDTO(hostToDeployToDTO);
        final SailingAnalyticsHost<String> hostToDeployReplicaTo = optionalPreferredInstanceToDeployUnmanagedReplicaTo == null
                ? null : getHostFromInstanceDTO(optionalPreferredInstanceToDeployUnmanagedReplicaTo);
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> result =
                getLandscapeService().deployApplicationToExistingHost(replicaSetName, hostToDeployTo,
                    replicaInstanceType, dynamicLoadBalancerMapping, release.getName(),
                    optionalKeyName, privateKeyEncryptionPassphrase, masterReplicationBearerToken,
                    replicaReplicationBearerToken, optionalDomainName, Optional.ofNullable(optionalMinimumAutoScalingGroupSizeOrNull),
                    Optional.ofNullable(optionalMaximumAutoScalingGroupSizeOrNull), optionalMemoryInMegabytesOrNull,
                    optionalMemoryTotalSizeFactorOrNull, optionalIgtimiRiotPort, Optional.of(hostToDeployTo.getInstance().instanceType()), Optional.ofNullable(hostToDeployReplicaTo));
        return new SailingApplicationReplicaSetDTO<String>(result.getName(),
                convertToSailingAnalyticsProcessDTO(result.getMaster(), Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase),
                Util.map(result.getReplicas(), r->{
                    try {
                        return convertToSailingAnalyticsProcessDTO(r, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }), release.getName(),
                getLandscapeService().getFullyQualifiedHostname(replicaSetName, Optional.ofNullable(optionalDomainName)),
                getLandscapeService().getDefaultRedirectPath(result.getDefaultRedirectRule()),
                result.getAutoScalingGroup()==null?null:result.getAutoScalingGroup().getLaunchTemplateDefaultVersion().launchTemplateData().imageId());
    }

    @Override
    public void defineDefaultRedirect(String regionId, String hostname, RedirectDTO redirect,
            String keyName, String passphraseForPrivateKeyDecryption) {
        final ApplicationLoadBalancer<String> loadBalancer = getLandscape().getLoadBalancerByHostname(hostname);
        loadBalancer.setDefaultRedirect(hostname, redirect.getPath(), redirect.getQuery());
    }

    @Override
    public SerializationDummyDTO serializationDummy(ProcessDTO mongoProcessDTO, AwsInstanceDTO awsInstanceDTO, AwsShardDTO shardDTO,
            SailingApplicationReplicaSetDTO<String> sailingApplicationReplicationSetDTO, LeaderboardNameDTO leaderboard) {
        return null;
    }
    
    @Override
    public Triple<DataImportProgress, CompareServersResultDTO, String> archiveReplicaSet(String regionId, SailingApplicationReplicaSetDTO<String> applicationReplicaSetToArchive,
            String bearerTokenOrNullForApplicationReplicaSetToArchive,
            String bearerTokenOrNullForArchive,
            Duration durationToWaitBeforeCompareServers,
            int maxNumberOfCompareServerAttempts, boolean removeApplicationReplicaSet, MongoEndpointDTO moveDatabaseHere,
            String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception {
        checkLandscapeManageAwsPermission();
        if (removeApplicationReplicaSet) {
            getSecurityService().checkCurrentUserDeletePermission(SecuredSecurityTypes.SERVER.getQualifiedObjectIdentifier(
                    new TypeRelativeObjectIdentifier(applicationReplicaSetToArchive.getReplicaSetName())));
        }
        final Triple<DataImportProgress, CompareServersResult, String> result = getLandscapeService().archiveReplicaSet(regionId,
                convertFromApplicationReplicaSetDTO(new AwsRegion(regionId, getLandscape()), applicationReplicaSetToArchive, optionalKeyName, passphraseForPrivateKeyDecryption),
                bearerTokenOrNullForApplicationReplicaSetToArchive, bearerTokenOrNullForArchive,
                durationToWaitBeforeCompareServers, maxNumberOfCompareServerAttempts, removeApplicationReplicaSet,
                getMongoEndpoint(moveDatabaseHere), optionalKeyName, passphraseForPrivateKeyDecryption);
        final String mongoDBArchivingFailureReason = result.getC();
        final CompareServersResultDTO compareServersResultDTO = createCompareServersResultDTO(result);
        return new Triple<>(result.getA(), compareServersResultDTO, mongoDBArchivingFailureReason);
    }

    private CompareServersResultDTO createCompareServersResultDTO(
            final Triple<DataImportProgress, CompareServersResult, String> compareServersResult) {
        return compareServersResult == null ?
                null :
                new CompareServersResultDTO(compareServersResult.getB()==null?null:compareServersResult.getB().getServerA(),
                                            compareServersResult.getB()==null?null:compareServersResult.getB().getServerB(),
                                            compareServersResult.getB()==null?null:compareServersResult.getB().getADiffs().toString(),
                                            compareServersResult.getB()==null?null:compareServersResult.getB().getBDiffs().toString());
    }
    
    @Override
    public String removeApplicationReplicaSet(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToRemove, MongoEndpointDTO moveDatabaseHere,
            String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception {
        checkLandscapeManageAwsPermission();
        getSecurityService().checkCurrentUserDeletePermission(SecuredSecurityTypes.SERVER.getQualifiedObjectIdentifier(
                new TypeRelativeObjectIdentifier(applicationReplicaSetToRemove.getReplicaSetName())));
        final String mongoDbArchivingErrorMessage = getLandscapeService().removeApplicationReplicaSet(regionId, convertFromApplicationReplicaSetDTO(
                new AwsRegion(regionId, getLandscape()), applicationReplicaSetToRemove, optionalKeyName, passphraseForPrivateKeyDecryption), getMongoEndpoint(moveDatabaseHere),
                optionalKeyName, passphraseForPrivateKeyDecryption);
        return mongoDbArchivingErrorMessage;
    }

    private AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> convertFromApplicationReplicaSetDTO(
            final AwsRegion region, SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, String optionalKeyName, byte[] passphraseForPrivateKeyDecryption)
            throws Exception {
        final SailingAnalyticsProcess<String> master = getSailingAnalyticsProcessFromDTO(applicationReplicaSetDTO.getMaster());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet =
                getLandscape().getApplicationReplicaSet(region, applicationReplicaSetDTO.getReplicaSetName(),
                    master, master.getReplicas(Landscape.WAIT_FOR_PROCESS_TIMEOUT, new SailingAnalyticsHostSupplier<String>(),
                            new SailingAnalyticsProcessFactory(this::getLandscape)), Landscape.WAIT_FOR_PROCESS_TIMEOUT,
                    Optional.ofNullable(optionalKeyName), passphraseForPrivateKeyDecryption);
        return applicationReplicaSet;
    }

    private SailingAnalyticsProcess<String> getSailingAnalyticsProcessFromDTO(SailingAnalyticsProcessDTO processDTO) throws UnknownHostException {
        return new SailingAnalyticsProcessImpl<String>(processDTO.getPort(),
                getHostFromInstanceDTO(processDTO.getHost()), processDTO.getServerDirectory(),
                processDTO.getExpeditionUdpPort(), processDTO.getIgtimiRiotPort(), getLandscape());
    }

    private SailingAnalyticsHost<String> getHostFromInstanceDTO(AwsInstanceDTO hostDTO) throws UnknownHostException {
        return new SailingAnalyticsHostImpl<String, SailingAnalyticsHost<String>>(hostDTO.getInstanceId(),
                new AwsAvailabilityZoneImpl(AvailabilityZone.builder().regionName(hostDTO.getRegion()).zoneId(hostDTO.getAvailabilityZoneId()).build(), getLandscape()),
                InetAddress.getByName(hostDTO.getPrivateIpAddress()), hostDTO.getLaunchTimePoint(), getLandscape(),
                new SailingAnalyticsProcessFactory(this::getLandscape));
    }
    
    @Override
    public SailingApplicationReplicaSetDTO<String> createDefaultLoadBalancerMappings(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToCreateLoadBalancerMappingFor,
            boolean useDynamicLoadBalancer, String optionalDomainName, boolean forceDNSUpdate) throws Exception {
        checkLandscapeManageAwsPermission();
        logger.info("Creating default load balancer mappings in region "+regionId+" for application replica set "+
                applicationReplicaSetToCreateLoadBalancerMappingFor.getName()+" on behalf of "+SecurityUtils.getSubject().getPrincipal());
        final SailingAnalyticsProcess<String> master = getSailingAnalyticsProcessFromDTO(
                applicationReplicaSetToCreateLoadBalancerMappingFor.getMaster());
        final CreateLoadBalancerMapping.Builder<?, ?, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLoadBalancerMappingBuilder;
        if (useDynamicLoadBalancer) {
            createLoadBalancerMappingBuilder = CreateDynamicLoadBalancerMapping.builder();
        } else {
            com.sap.sse.landscape.aws.orchestration.CreateDNSBasedLoadBalancerMapping.Builder<?, ?, String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> withDNSBuilder =
                    CreateDNSBasedLoadBalancerMapping.builder();
            withDNSBuilder.forceDNSUpdate(forceDNSUpdate);
            createLoadBalancerMappingBuilder = withDNSBuilder;
        }
        final String domainName = Optional.ofNullable(optionalDomainName).orElse(SharedLandscapeConstants.DEFAULT_DOMAIN_NAME);
        final String masterHostname = applicationReplicaSetToCreateLoadBalancerMappingFor.getHostname() == null
                ? (applicationReplicaSetToCreateLoadBalancerMappingFor.getName()+"."+domainName).toLowerCase()
                : (applicationReplicaSetToCreateLoadBalancerMappingFor.getHostname()).toLowerCase();
        final CreateLoadBalancerMapping<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createLoadBalancerMapping = createLoadBalancerMappingBuilder
            .setProcess(master)
            .setHostname(masterHostname)
            .setTargetGroupNamePrefix(LandscapeService.SAILING_TARGET_GROUP_NAME_PREFIX)
            .setSecurityGroupForVpc(getLandscape().getDefaultSecurityGroupForApplicationHosts(new AwsRegion(regionId, getLandscape())))
            .setLandscape(getLandscape())
            .build();
        createLoadBalancerMapping.run();
        final PlainRedirectDTO defaultRedirect = new PlainRedirectDTO();
        return new SailingApplicationReplicaSetDTO<String>(
                applicationReplicaSetToCreateLoadBalancerMappingFor.getName(),
                applicationReplicaSetToCreateLoadBalancerMappingFor.getMaster(),
                applicationReplicaSetToCreateLoadBalancerMappingFor.getReplicas(),
                applicationReplicaSetToCreateLoadBalancerMappingFor.getVersion(),
                applicationReplicaSetToCreateLoadBalancerMappingFor.getHostname(),
                RedirectDTO.toString(defaultRedirect.getPath(), defaultRedirect.getQuery()),
                applicationReplicaSetToCreateLoadBalancerMappingFor.getAutoScalingGroupAmiId());
    }

    @Override
    public Boolean ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSet,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion region = new AwsRegion(regionId, getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
                convertFromApplicationReplicaSetDTO(region, applicationReplicaSet, optionalKeyName, privateKeyEncryptionPassphrase);
        final String effectiveReplicaReplicationBearerToken = getLandscapeService().getEffectiveBearerToken(replicaReplicationBearerToken);
        final SailingAnalyticsProcess<String> additionalReplicaStarted = getLandscapeService()
                .ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(replicaSet,
                        optionalKeyName, privateKeyEncryptionPassphrase, effectiveReplicaReplicationBearerToken);
        return additionalReplicaStarted != null;
    }

    /**
     * Upgrades the {@code applicationReplicaSetToUpgrade} to the release specified or the latest master build if no
     * release is specified. See
     * {@link LandscapeService#upgradeApplicationReplicaSet(AwsRegion, AwsApplicationReplicaSet, String, String, byte[], String)}
     * for details.
     */
    @Override
    public SailingApplicationReplicaSetDTO<String> upgradeApplicationReplicaSet(String regionId,
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetToUpgrade, String releaseOrNullForLatestMaster,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion region = new AwsRegion(regionId, getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
                convertFromApplicationReplicaSetDTO(region, applicationReplicaSetToUpgrade, optionalKeyName, privateKeyEncryptionPassphrase);
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> upgradedReplicaSet =
                getLandscapeService().upgradeApplicationReplicaSet(region, replicaSet,
                    releaseOrNullForLatestMaster, optionalKeyName, privateKeyEncryptionPassphrase,
                    replicaReplicationBearerToken);
        final SailingAnalyticsProcess<String> oldMaster = replicaSet.getMaster();
        final Release release = upgradedReplicaSet.getVersion(Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        return new SailingApplicationReplicaSetDTO<String>(applicationReplicaSetToUpgrade.getName(),
                convertToSailingAnalyticsProcessDTO(oldMaster, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase),
                Util.map(upgradedReplicaSet.getReplicas(), r->{
                    try {
                        return convertToSailingAnalyticsProcessDTO(r, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }),
                release.getName(), applicationReplicaSetToUpgrade.getHostname(),
                applicationReplicaSetToUpgrade.getDefaultRedirectPath(), applicationReplicaSetToUpgrade.getAutoScalingGroupAmiId());
    }

    @Override
    public ArrayList<ReleaseDTO> getReleases() {
        return Util.mapToArrayList(SailingReleaseRepository.INSTANCE, r->new ReleaseDTO(r.getName(), r.getBaseName(), r.getCreationDate()));
    }
    
    @Override
    public ArrayList<SailingApplicationReplicaSetDTO<String>> updateImageForReplicaSets(
            String regionId, ArrayList<SailingApplicationReplicaSetDTO<String>> applicationReplicaSetsToUpdate,
            AmazonMachineImageDTO amiDTOOrNullForLatest,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion region = new AwsRegion(regionId, getLandscape());
        final AmazonMachineImage<String> ami = amiDTOOrNullForLatest==null?null:getLandscape().getImage(region, amiDTOOrNullForLatest.getId());
        final Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> replicaSets =
                Util.map(applicationReplicaSetsToUpdate, rsDTO->{
                    try {
                        return convertFromApplicationReplicaSetDTO(region, rsDTO, optionalKeyName, privateKeyEncryptionPassphrase);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        final Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> updatedReplicaSets =
                getLandscapeService().updateImageForReplicaSets(region, replicaSets, Optional.ofNullable(ami),
                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        final ArrayList<SailingApplicationReplicaSetDTO<String>> result = new ArrayList<>();
        for (final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> updatedReplicaSet : updatedReplicaSets) {
            result.add(convertToSailingApplicationReplicaSetDTO(updatedReplicaSet, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase));
        }
        return result;
    }
    
    @Override
    public SailingApplicationReplicaSetDTO<String> useDedicatedAutoScalingReplicasInsteadOfShared(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion region = new AwsRegion(applicationReplicaSetDTO.getMaster().getHost().getRegion(), getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
                convertFromApplicationReplicaSetDTO(region, applicationReplicaSetDTO, optionalKeyName, privateKeyEncryptionPassphrase);
        return convertToSailingApplicationReplicaSetDTO(
                getLandscapeService().useDedicatedAutoScalingReplicasInsteadOfShared(replicaSet, optionalKeyName, privateKeyEncryptionPassphrase),
                Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
    }
    
    @Override
    public SailingApplicationReplicaSetDTO<String> useSingleSharedInsteadOfDedicatedAutoScalingReplica(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, String optionalKeyName,
            byte[] privateKeyEncryptionPassphrase, String replicaReplicationBearerToken,
            Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
            String optionalSharedReplicaInstanceType) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion region = new AwsRegion(applicationReplicaSetDTO.getMaster().getHost().getRegion(), getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
                convertFromApplicationReplicaSetDTO(region, applicationReplicaSetDTO, optionalKeyName, privateKeyEncryptionPassphrase);
        return convertToSailingApplicationReplicaSetDTO(
                getLandscapeService().useSingleSharedInsteadOfDedicatedAutoScalingReplica(replicaSet, optionalKeyName,
                        privateKeyEncryptionPassphrase, replicaReplicationBearerToken, optionalMemoryInMegabytesOrNull,
                        optionalMemoryTotalSizeFactorOrNull,
                        optionalSharedReplicaInstanceType==null?Optional.empty():Optional.of(InstanceType.valueOf(optionalSharedReplicaInstanceType))),
                Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
    }    
    
    @Override
    public SailingApplicationReplicaSetDTO<String> moveMasterToOtherInstance(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, boolean useSharedInstance,
            String optionalInstanceTypeOrNull,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase, String optionalMasterReplicationBearerTokenOrNull,
            String optionalReplicaReplicationBearerTokenOrNull, Integer optionalMemoryInMegabytesOrNull,
            Integer optionalMemoryTotalSizeFactorOrNull) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion region = new AwsRegion(applicationReplicaSetDTO.getMaster().getHost().getRegion(), getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
                convertFromApplicationReplicaSetDTO(region, applicationReplicaSetDTO, optionalKeyName, privateKeyEncryptionPassphrase);
        return convertToSailingApplicationReplicaSetDTO(
                getLandscapeService().moveMasterToOtherInstance(replicaSet, useSharedInstance,
                        optionalInstanceTypeOrNull==null?Optional.empty():Optional.of(InstanceType.valueOf(optionalInstanceTypeOrNull)),
                        /* optionalPreferredInstanceToDeployTo */ Optional.empty(), optionalKeyName,
                        privateKeyEncryptionPassphrase, optionalMasterReplicationBearerTokenOrNull, optionalReplicaReplicationBearerTokenOrNull,
                        optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull), Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
    }

    @Override
    public SailingApplicationReplicaSetDTO<String> changeAutoScalingReplicasInstanceType(
            SailingApplicationReplicaSetDTO<String> applicationReplicaSetDTO, String instanceTypeName,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion region = new AwsRegion(applicationReplicaSetDTO.getMaster().getHost().getRegion(), getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet =
                convertFromApplicationReplicaSetDTO(region, applicationReplicaSetDTO, optionalKeyName, privateKeyEncryptionPassphrase);
        return convertToSailingApplicationReplicaSetDTO(
                getLandscapeService().changeAutoScalingReplicasInstanceType(replicaSet, InstanceType.valueOf(instanceTypeName),
                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase),
                Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
    }

    @Override
    public ArrayList<LeaderboardNameDTO> getLeaderboardNames(SailingApplicationReplicaSetDTO<String> replicaSet,
            String bearerToken) throws Exception {
        final SailingServer server = getLandscapeService().getSailingServer(replicaSet.getHostname(), bearerToken,
                /* HTTPS Port */ Optional.of(443));
        return new ArrayList<>(Util.asList(Util.map(server.getLeaderboardNames(), LeaderboardNameDTO::new)));
    }
    
    @Override
    public void addShard(String shardName, ArrayList<LeaderboardNameDTO> selectedLeaderBoardNames,
            SailingApplicationReplicaSetDTO<String> replicaSetDTO, String bearerToken, String region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion awsRegion = new AwsRegion(replicaSetDTO.getMaster().getHost().getRegion(), getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> awsReplicaSet = convertFromApplicationReplicaSetDTO(
                awsRegion, replicaSetDTO, optionalKeyName, privateKeyEncryptionPassphrase);
        getLandscapeService().addShard(Util.map(selectedLeaderBoardNames, t -> t.getName()), awsReplicaSet, awsRegion,
                bearerToken, shardName);
    }

    @Override
    public Map<AwsShardDTO, Iterable<String>> getShards(SailingApplicationReplicaSetDTO<String> replicaSetDTO,
            String region, String bearerToken, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion awsRegion = new AwsRegion(replicaSetDTO.getMaster().getHost().getRegion(), getLandscape());
        final Map<AwsShardDTO, Iterable<String>> shardingKeysForShards = new HashMap<>();
        final SailingServer server = getLandscapeService().getSailingServer(replicaSetDTO.getHostname(), bearerToken, Optional.empty());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationServerReplicaSet = convertFromApplicationReplicaSetDTO(
                awsRegion, replicaSetDTO, optionalKeyName, privateKeyEncryptionPassphrase);
        final Map<String, String> leaderboardNamesByShardingKeys = new HashMap<>();
        for (String leaderboard : server.getLeaderboardNames()) {
            leaderboardNamesByShardingKeys.put(server.getLeaderboardShardingKey(leaderboard), leaderboard);
        }
        for (Entry<AwsShard<String>, Iterable<String>> entry : applicationServerReplicaSet.getShards().entrySet()) {
            shardingKeysForShards.put(createAwsShardDTO(entry.getKey(), applicationServerReplicaSet.getName(), server, leaderboardNamesByShardingKeys),
                    entry.getValue());
        }
        final Map<AwsShardDTO, Iterable<String>> res = new HashMap<>();
        for (Entry<AwsShardDTO, Iterable<String>> entry : shardingKeysForShards.entrySet()) {
            res.put(entry.getKey(), entry.getValue());
        }
        return res;
    }

    @Override
    public void removeShard(AwsShardDTO shard, SailingApplicationReplicaSetDTO<String> replicaSetDTO, String region, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion awsRegion = new AwsRegion(replicaSetDTO.getMaster().getHost().getRegion(), getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationServerReplicaSet = convertFromApplicationReplicaSetDTO(
                awsRegion, replicaSetDTO, optionalKeyName, privateKeyEncryptionPassphrase);
        getLandscapeService().removeShard(applicationServerReplicaSet, shard.getTargetgroupArn());
    }

    @Override
    public void appendShardingKeysToShard(Iterable<LeaderboardNameDTO> sharindKeysToAppend, String region,
            String shardName, SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion awsRegion = new AwsRegion(replicaSet.getMaster().getHost().getRegion(), getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> rs = convertFromApplicationReplicaSetDTO(
                awsRegion, replicaSet, optionalKeyName, privateKeyEncryptionPassphrase);
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> applicationReplicaSet = getLandscape()
                .getApplicationReplicaSet(awsRegion, rs.getServerName(), rs.getMaster(), rs.getReplicas(),
                        Landscape.WAIT_FOR_PROCESS_TIMEOUT, Optional.ofNullable(optionalKeyName), privateKeyEncryptionPassphrase);
        getLandscapeService().appendShardingKeysToShard(Util.map(sharindKeysToAppend, t -> t.getName()),
                applicationReplicaSet, awsRegion, shardName, bearerToken);
    }

    @Override
    public void removeShardingKeysFromShard(Iterable<LeaderboardNameDTO> selectedShardingKeys, String region,
            String shardName, SailingApplicationReplicaSetDTO<String> replicaSet, String bearerToken, String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final AwsRegion awsRegion = new AwsRegion(region, getLandscape());
        final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> rs = convertFromApplicationReplicaSetDTO(
                awsRegion, replicaSet, optionalKeyName, privateKeyEncryptionPassphrase);
        getLandscapeService().removeShardingKeysFromShard(Util.asList(Util.map(selectedShardingKeys, t -> t.getName())),
                rs, awsRegion, shardName, bearerToken);
    }

    private AwsShardDTO createAwsShardDTO(AwsShard<String> shard, String replicaSetName, SailingServer server,
            Map<String, String> leaderboardNamesByShardingKeys) throws Exception {
        return new AwsShardDTO(Util.filter(Util.map(shard.getKeys(),
                shardingKey -> leaderboardNamesByShardingKeys.get(shardingKey)), leaderboardName->leaderboardName!=null),
                shard.getTargetGroup().getTargetGroupArn(), shard.getTargetGroup().getName(),
                shard.getAutoScalingGroup().getAutoScalingGroup().autoScalingGroupARN(),
                shard.getTargetGroup().getLoadBalancerArn(), shard.getAutoScalingGroup().getName(),
                shard.getName() == null ? "" : shard.getName(), replicaSetName);
    }
    
    @Override
    public void moveAllApplicationProcessesAwayFrom(AwsInstanceDTO host, String optionalInstanceTypeForNewInstance,
            String optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        checkLandscapeManageAwsPermission();
        final SailingAnalyticsHost<String> sailingAnalyticsHost = getHostFromInstanceDTO(host);
        getLandscapeService().moveAllApplicationProcessesAwayFrom(sailingAnalyticsHost,
                Optional.ofNullable(optionalInstanceTypeForNewInstance == null ? null
                        : InstanceType.valueOf(optionalInstanceTypeForNewInstance)),
                optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    @Override
    public boolean hasDNSResourceRecordsForReplicaSet(String replicaSetName, String optionalDomainName) {
        final AwsLandscape<String> landscape = getLandscape();
        final LandscapeService landscapeService = getLandscapeService();
        final String hostname = landscapeService.getHostname(replicaSetName, optionalDomainName);
        final Iterable<ResourceRecordSet> existingDNSRulesForHostname = landscape.getResourceRecordSets(hostname);
        // Failing early in case DNS record already exists (see also bug 5826):
        return existingDNSRulesForHostname != null && !Util.isEmpty(existingDNSRulesForHostname);
    }
}
