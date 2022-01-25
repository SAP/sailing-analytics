package com.sap.sse.landscape.aws.impl;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.DefaultProcessConfigurationVariables;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.MachineImage;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.SecurityGroup;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.ApplicationReplicaSet;
import com.sap.sse.landscape.application.Scope;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsApplicationProcess;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsAutoScalingGroup;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.AwsLandscapeState;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.ReverseProxyCluster;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.aws.persistence.DomainObjectFactory;
import com.sap.sse.landscape.aws.persistence.MongoObjectFactory;
import com.sap.sse.landscape.aws.persistence.PersistenceFactory;
import com.sap.sse.landscape.common.shared.MongoDBConstants;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.landscape.mongodb.MongoReplicaSet;
import com.sap.sse.landscape.mongodb.impl.DatabaseImpl;
import com.sap.sse.landscape.mongodb.impl.MongoProcessImpl;
import com.sap.sse.landscape.mongodb.impl.MongoProcessInReplicaSetImpl;
import com.sap.sse.landscape.mongodb.impl.MongoReplicaSetImpl;
import com.sap.sse.landscape.rabbitmq.RabbitMQEndpoint;
import com.sap.sse.landscape.ssh.SSHKeyPair;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.util.ThreadPoolUtil;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.acm.AcmAsyncClient;
import software.amazon.awssdk.services.acm.model.CertificateDetail;
import software.amazon.awssdk.services.acm.model.CertificateStatus;
import software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.autoscaling.model.MetricType;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSnapshotsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest.Builder;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2AsyncClient;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ActionTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Certificate;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetHealthResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerState;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyTargetGroupAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RedirectActionStatusCodeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RulePriorityPair;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SetRulePrioritiesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SubnetMapping;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupTuple;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealth;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetTypeEnum;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.MFADevice;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.Change;
import software.amazon.awssdk.services.route53.model.ChangeAction;
import software.amazon.awssdk.services.route53.model.ChangeBatch;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsRequest;
import software.amazon.awssdk.services.route53.model.ChangeResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.GetChangeRequest;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;
import software.amazon.awssdk.services.route53.model.TestDnsAnswerResponse;
import software.amazon.awssdk.services.route53.paginators.ListResourceRecordSetsIterable;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;

public class AwsLandscapeImpl<ShardingKey> implements AwsLandscape<ShardingKey> {
    private static final String AUTO_SCALING_GROUP_NAME_SUFFIX = "-auto-replicas";
    private static final String DEFAULT_TARGET_GROUP_PREFIX = "D";
    private static final Logger logger = Logger.getLogger(AwsLandscapeImpl.class.getName());
    private static final long DEFAULT_DNS_TTL_MILLIS = 60l;
    private static final String DEFAULT_CERTIFICATE_DOMAIN = "*.sapsailing.com";
    // TODO <config> the "Java Application with Reverse Proxy" security group in eu-west-2 for experimenting; we need this security group per region
    private static final String DEFAULT_APPLICATION_SERVER_SECURITY_GROUP_ID_EU_WEST_1 = "sg-eaf31e85";
    private static final String DEFAULT_APPLICATION_SERVER_SECURITY_GROUP_ID_EU_WEST_2 = "sg-0b2afd48960251280";
    private static final String DEFAULT_MONGODB_SECURITY_GROUP_ID_EU_WEST_1 = "sg-0a9bc2fb61f10a342";
    private static final String DEFAULT_MONGODB_SECURITY_GROUP_ID_EU_WEST_2 = "sg-02649c35a73ee0ae5";
    private static final String DEFAULT_NON_DNS_MAPPED_ALB_NAME = "DefDyn";
    private final String accessKeyId;
    private final String secretAccessKey;
    private final Optional<String> sessionToken;
    private final AwsRegion globalRegion;
    private final AwsLandscapeState landscapeState;
    
    public AwsLandscapeImpl(AwsLandscapeState awsLandscapeState) {
        this(awsLandscapeState,
             System.getProperty(ACCESS_KEY_ID_SYSTEM_PROPERTY_NAME), System.getProperty(SECRET_ACCESS_KEY_SYSTEM_PROPERTY_NAME));
    }
    
    public AwsLandscapeImpl(AwsLandscapeState awsLandscapeState, String accessKeyId, String secretAccessKey) {
        this(awsLandscapeState, accessKeyId, secretAccessKey, /* no session token */ null);
    }
    
    public AwsLandscapeImpl(AwsLandscapeState awsLandscapeState, String accessKeyId, String secretAccessKey, String sessionToken) {
        this(accessKeyId, secretAccessKey, sessionToken,
                // by using MongoDBService.INSTANCE the default test configuration will be used if nothing else is configured
                PersistenceFactory.INSTANCE.getDomainObjectFactory(MongoDBService.INSTANCE), PersistenceFactory.INSTANCE.getMongoObjectFactory(MongoDBService.INSTANCE), awsLandscapeState);
    }
    
    public AwsLandscapeImpl(String accessKeyId, String secretAccessKey,
            String sessionToken, DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory, AwsLandscapeState landscapeState) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = Optional.ofNullable(sessionToken);
        this.globalRegion = new AwsRegion(Region.AWS_GLOBAL, this);
        this.landscapeState = landscapeState;
    }
    
    private static byte[] getPrivateKeyBytes(KeyPair unencryptedKeyPair) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        unencryptedKeyPair.writePrivateKey(bos);
        return bos.toByteArray();
    }

    @Override
    public SSHKeyPair addSSHKeyPair(com.sap.sse.landscape.Region region, String creator, String keyName, KeyPair keyPairWithDecryptedPrivateKey) throws JSchException {
        assert !keyPairWithDecryptedPrivateKey.isEncrypted();
        final SSHKeyPair result = new SSHKeyPair(region.getId(), creator, TimePoint.now(), keyName, keyPairWithDecryptedPrivateKey.getPublicKeyBlob(),
                getPrivateKeyBytes(keyPairWithDecryptedPrivateKey));
        landscapeState.addSSHKeyPair(result);
        return result;
    }
    
    @Override
    public SSHKeyPair createKeyPair(com.sap.sse.landscape.Region region, String keyName, byte[] privateKeyEncryptionPassphrase) throws JSchException {
        final CreateKeyPairResponse keyPairResponse = getEc2Client(getRegion(region))
                .createKeyPair(CreateKeyPairRequest.builder().keyName(keyName).build());
        final String keyMaterial = keyPairResponse.keyMaterial();
        Object principal;
        try {
            principal = SessionUtils.getPrincipal();
        } catch (Exception e) {
            logger.severe("Problem determining current user: "+e.getMessage());
            principal = null;
        }
        final byte[] privKey = keyMaterial.getBytes();
        final KeyPair keyPair = KeyPair.load(new JSch(), privKey, /* pubkey */ null); // private key is unencrypted so far; public key can be obtained:
        final String creatorName = principal==null?"":principal.toString();
        final ByteArrayOutputStream publicKeyBytes = new ByteArrayOutputStream();
        final TimePoint now = TimePoint.now();
        keyPair.writePublicKey(publicKeyBytes, "public key "+keyName+" generated by user "+creatorName+" at "+now);
        final SSHKeyPair result = new SSHKeyPair(region.getId(), creatorName,
                now, keyPairResponse.keyName(), publicKeyBytes.toByteArray(), privKey,
                privateKeyEncryptionPassphrase);
        landscapeState.addSSHKeyPair(result);
        return result;
    }

    private <B extends AwsClientBuilder<B, C>, C> C getClient(B clientBuilder, Region region) {
        return clientBuilder.credentialsProvider(this::getCredentials).region(region).build();
    }
    
    private Ec2Client getEc2Client(Region region) {
        return getClient(Ec2Client.builder(), region);
    }
    
    private AcmAsyncClient getAcmAsyncClient(Region region) {
        return getClient(AcmAsyncClient.builder(), region);
    }
    
    private ElasticLoadBalancingV2Client getLoadBalancingClient(Region region) {
        return getClient(ElasticLoadBalancingV2Client.builder(), region);
    }
    
    private ElasticLoadBalancingV2AsyncClient getLoadBalancingAsyncClient(Region region) {
        return getClient(ElasticLoadBalancingV2AsyncClient.builder(), region);
    }
    
    private AutoScalingClient getAutoScalingClient(Region region) {
        return getClient(AutoScalingClient.builder(), region);
    }
    
    private AutoScalingAsyncClient getAutoScalingAsyncClient(Region region) {
        return getClient(AutoScalingAsyncClient.builder(), region);
    }
    
    /**
     * For legacy reasons our primary region (eu-west-1) uses a special bucket name for ALB log storage.
     */
    private String getS3BucketForAlbLogs(com.sap.sse.landscape.Region region) {
        final String result;
        if (region.getId().equals(Region.EU_WEST_1.id())) {
            result = "sapsailing-access-logs";
        } else {
            result = "sapsailing-access-logs-"+region.getId();
        }
        return result;
    }
    
    @Override
    public ApplicationLoadBalancer<ShardingKey> createLoadBalancer(String name, com.sap.sse.landscape.Region region) throws InterruptedException, ExecutionException {
        Region awsRegion = getRegion(region);
        final ElasticLoadBalancingV2Client client = getLoadBalancingClient(awsRegion);
        final Iterable<AwsAvailabilityZone> availabilityZones = getAvailabilityZones(region);
        final SubnetMapping[] subnetMappings = Util.toArray(Util.map(getSubnetsForAvailabilityZones(awsRegion, availabilityZones),
                subnet->SubnetMapping.builder().subnetId(subnet.subnetId()).build()), new SubnetMapping[0]);
        final CreateLoadBalancerResponse response = client
                .createLoadBalancer(CreateLoadBalancerRequest.builder()
                        .name(name)
                        .subnetMappings(subnetMappings)
                        .securityGroups(getDefaultSecurityGroupForApplicationLoadBalancer(region).getId())
                        .build());
        client.modifyLoadBalancerAttributes(b->b.loadBalancerArn(response.loadBalancers().iterator().next().loadBalancerArn()).
                attributes(
                        LoadBalancerAttribute.builder().key("access_logs.s3.enabled").value("true").build(),
                        LoadBalancerAttribute.builder().key("access_logs.s3.bucket").value(getS3BucketForAlbLogs(region)).build(),
                        LoadBalancerAttribute.builder().key("idle_timeout.timeout_seconds").value("4000").build()).build());
        final ApplicationLoadBalancer<ShardingKey> result = new ApplicationLoadBalancerImpl<>(region, response.loadBalancers().iterator().next(), this);
        createLoadBalancerHttpListener(result);
        createLoadBalancerHttpsListener(result);
        return result;
    }
    
    private <MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    Listener createLoadBalancerHttpListener(ApplicationLoadBalancer<ShardingKey> alb) {
        return getLoadBalancingClient(getRegion(alb.getRegion()))
                        .createListener(l -> {
                            l.loadBalancerArn(alb.getArn()).protocol(ProtocolEnum.HTTP)
                                        .port(80)
                                        .defaultActions(Action.builder()
                                                .type(ActionTypeEnum.REDIRECT)
                                                .redirectConfig(rcb->rcb
                                                        .protocol(ProtocolEnum.HTTPS.name())
                                                        .port("443")
                                                        .host("#{host}")
                                                        .path("/#{path}")
                                                        .query("#{query}")
                                                        .statusCode(RedirectActionStatusCodeEnum.HTTP_301))
                                                .build());
                        }).listeners().iterator().next();
    }
    
    private CompletableFuture<String> getDefaultCertificateArn(com.sap.sse.landscape.Region region, String domainName) {
        final AcmAsyncClient acmClient = getAcmAsyncClient(getRegion(region));
        return acmClient.listCertificates(b->b
                .certificateStatuses(CertificateStatus.ISSUED)).thenCompose(response->{
                    final List<CompletableFuture<CertificateDetail>> certificateDetails = new ArrayList<>();
                    response.certificateSummaryList().stream().filter(certificateSummary->Util.equalsWithNull(certificateSummary.domainName(), domainName))
                        .forEach(certSummaryForDomain->certificateDetails.add(
                                acmClient.describeCertificate(b->b.certificateArn(certSummaryForDomain.certificateArn()))
                                    .thenApply(detailResponse->detailResponse.certificate())));
                    final CompletableFuture<Void> waitForCertificateDetails = CompletableFuture.allOf(certificateDetails.toArray(new CompletableFuture<?>[0]));
                    final CompletableFuture<String> result = waitForCertificateDetails
                        .thenApply(v->certificateDetails.stream().map(cf->{
                            try {
                                return cf.get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException();
                            }
                        }).sorted(/* newest first */ (cd1, cd2)->cd2.notAfter().compareTo(cd1.notAfter()))
                                .findFirst().map(cd->cd.certificateArn()).get());
                    return result;
                });
    }

    private <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    Listener createLoadBalancerHttpsListener(ApplicationLoadBalancer<ShardingKey> alb) throws InterruptedException, ExecutionException {
        final CompletableFuture<String> defaultCertificateArnFuture = getDefaultCertificateArn(alb.getRegion(), DEFAULT_CERTIFICATE_DOMAIN);
        final int httpPort = 80;
        final int httpsPort = 443;
        final ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> reverseProxy = getCentralReverseProxy(alb.getRegion());
        final TargetGroup<ShardingKey> defaultTargetGroup = createTargetGroup(alb.getRegion(), DEFAULT_TARGET_GROUP_PREFIX+alb.getName()+"-"+ProtocolEnum.HTTP.name(),
                httpPort, reverseProxy.getHealthCheckPath(), /* healthCheckPort */ httpPort, alb.getArn());
        defaultTargetGroup.addTargets(reverseProxy.getHosts());
        final String defaultCertificateArn = defaultCertificateArnFuture.get();
        return getLoadBalancingClient(getRegion(alb.getRegion()))
                        .createListener(l -> {
                            l.loadBalancerArn(alb.getArn()).protocol(ProtocolEnum.HTTPS)
                                        .port(httpsPort)
                                        .defaultActions(Action.builder()
                                                .targetGroupArn(defaultTargetGroup.getTargetGroupArn())
                                                .type(ActionTypeEnum.FORWARD)
                                                .forwardConfig(f -> f.targetGroups(TargetGroupTuple.builder()
                                                        .targetGroupArn(defaultTargetGroup.getTargetGroupArn()).build())
                                                        .build())
                                                .build());
                            l.certificates(Certificate.builder().certificateArn(defaultCertificateArn).build());
                        }).listeners().iterator().next();
    }

    @Override
    public void deleteLoadBalancer(ApplicationLoadBalancer<ShardingKey> alb) {
        getLoadBalancingClient(getRegion(alb.getRegion())).deleteLoadBalancer(DeleteLoadBalancerRequest.builder().loadBalancerArn(alb.getArn()).build());
    }

    @Override
    public Iterable<TargetGroup<ShardingKey>> getTargetGroupsByLoadBalancerArn(com.sap.sse.landscape.Region region, String loadBalancerArn) {
        return Util.map(getLoadBalancingClient(getRegion(region)).describeTargetGroupsPaginator(tg->tg.loadBalancerArn(loadBalancerArn)).targetGroups(),
                tg->new AwsTargetGroupImpl<>(this, region, tg.targetGroupName(), tg.targetGroupArn(), loadBalancerArn,
                        tg.protocol(), tg.port(), tg.healthCheckProtocol(), getHealthCheckPort(tg), tg.healthCheckPath()));
    }

    @Override
    public Iterable<Listener> getListeners(ApplicationLoadBalancer<ShardingKey> alb) {
        final ElasticLoadBalancingV2Client client = getLoadBalancingClient(getRegion(alb.getRegion()));
        return client.describeListeners(b->b.loadBalancerArn(alb.getArn())).listeners();
    }

    @Override
    public LoadBalancerState getApplicationLoadBalancerStatus(ApplicationLoadBalancer<ShardingKey> alb) {
        final ElasticLoadBalancingV2Client client = getLoadBalancingClient(getRegion(alb.getRegion()));
        final DescribeLoadBalancersResponse response = client.describeLoadBalancers(b->b.loadBalancerArns(alb.getArn()));
        return response.loadBalancers().iterator().next().state();
    }

    @Override
    public Iterable<Rule> getLoadBalancerListenerRules(Listener loadBalancerListener, com.sap.sse.landscape.Region region) {
        return getLoadBalancingClient(getRegion(region)).describeRules(b->b.listenerArn(loadBalancerListener.listenerArn())).rules();
    }

    @Override
    public Iterable<Rule> createLoadBalancerListenerRules(com.sap.sse.landscape.Region region,
            Listener loadBalancerListenerToAddRuleTo, Rule... rulesToAdd) {
        final List<Rule> result = new ArrayList<>();
        for (final Rule rule : rulesToAdd) {
            result.add(getLoadBalancingClient(getRegion(region)).createRule(b -> b
                    .listenerArn(loadBalancerListenerToAddRuleTo.listenerArn())
                    .conditions(rule.conditions())
                    .priority(Integer.valueOf(rule.priority()))
                    .actions(rule.actions())).rules().iterator().next());
        }
        return result;
    }
    
    @Override
    public void deleteLoadBalancerListenerRules(com.sap.sse.landscape.Region region, Rule... rulesToDelete) {
        logger.info("Removing load balancer rules "+rulesToDelete+" in region "+region);
        for (final Rule rule : rulesToDelete) {
            getLoadBalancingClient(getRegion(region)).deleteRule(b -> b.ruleArn(rule.ruleArn()));
        }
    }
    
    @Override
    public void updateLoadBalancerListenerRule(com.sap.sse.landscape.Region region, Rule ruleToUpdate) {
        getLoadBalancingClient(getRegion(region)).modifyRule(b->b
                .ruleArn(ruleToUpdate.ruleArn())
                .actions(ruleToUpdate.actions())
                .conditions(ruleToUpdate.conditions()));
    }
    
    @Override
    public void updateLoadBalancerListenerRulePriorities(com.sap.sse.landscape.Region region, Collection<RulePriorityPair> newRulePriorities) {
        getLoadBalancingClient(getRegion(region)).setRulePriorities(SetRulePrioritiesRequest.builder().rulePriorities(newRulePriorities).build());
    }
    
    @Override
    public void deleteLoadBalancerListener(com.sap.sse.landscape.Region region, Listener listener) {
        getLoadBalancingClient(getRegion(region)).deleteListener(b->b.listenerArn(listener.listenerArn()));
    }

    /**
     * Grabs all subnets that are default subnet for any of the availability zones specified
     */
    private Iterable<Subnet> getSubnetsForAvailabilityZones(Region region, Iterable<AwsAvailabilityZone> azs) {
        return Util.filter(getEc2Client(region).describeSubnets().subnets(), subnet -> subnet.defaultForAz()
                && Util.contains(Util.map(azs, az -> az.getId()), subnet.availabilityZoneId()));
    }

    @Override
    public AwsAvailabilityZone getAvailabilityZoneByName(com.sap.sse.landscape.Region region, String availabilityZoneName) {
        final software.amazon.awssdk.services.ec2.model.AvailabilityZone awsAz = getEc2Client(getRegion(region))
                .describeAvailabilityZones(
                        DescribeAvailabilityZonesRequest.builder().zoneNames(availabilityZoneName).build())
                .availabilityZones().iterator().next();
        return new AwsAvailabilityZoneImpl(awsAz, this);
    }
    
    @Override
    public Iterable<ApplicationLoadBalancer<ShardingKey>> getLoadBalancers(com.sap.sse.landscape.Region region) {
        final List<LoadBalancer> loadBalancers = getLoadBalancingClient(getRegion(region))
                .describeLoadBalancers(DescribeLoadBalancersRequest.builder().build())
                .loadBalancers();
        return Util.map(loadBalancers, lb->new ApplicationLoadBalancerImpl<>(region, lb, this));
        
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> getLoadBalancerByName(String loadBalancerNameLowercase, com.sap.sse.landscape.Region region) {
        try {
            final DescribeLoadBalancersResponse response = getLoadBalancingClient(getRegion(region)).describeLoadBalancers();
            return response.hasLoadBalancers() ?
                    response.loadBalancers().stream()
                        .filter(lb->Util.equalsWithNull(loadBalancerNameLowercase, lb.loadBalancerName(), /* ignore case */ true))
                        .findFirst().map(lb->new ApplicationLoadBalancerImpl<>(region, lb, this)).orElse(null)
                    : null;
        } catch (LoadBalancerNotFoundException e) {
            return null;
        }
    }
    
    @Override
    public ApplicationLoadBalancer<ShardingKey> getLoadBalancer(String loadBalancerArn, com.sap.sse.landscape.Region region) {
        final LoadBalancer loadBalancer = getLoadBalancingClient(getRegion(region))
                .describeLoadBalancers(DescribeLoadBalancersRequest.builder().loadBalancerArns(loadBalancerArn).build())
                .loadBalancers().iterator().next();
        return new ApplicationLoadBalancerImpl<>(region, loadBalancer, this);
    }
    
    @Override
    public Instance getInstance(String instanceId, com.sap.sse.landscape.Region region) {
        return getEc2Client(getRegion(region))
                .describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceId).build()).reservations()
                .iterator().next().instances().iterator().next();
    }

    @Override
    public Instance getInstanceByPublicIpAddress(com.sap.sse.landscape.Region region, String publicIpAddress) {
        try {
            final InetAddress inetAddress = InetAddress.getByName(publicIpAddress);
            return getEc2Client(getRegion(region))
                    .describeInstances(b->b.filters(Filter.builder().name("ip-address").values(inetAddress.getHostAddress()).build())).reservations()
                    .iterator().next().instances().iterator().next();
        } catch (UnknownHostException e) {
            logger.warning("IP address for "+publicIpAddress+" not found");
            return null;
        }
    }
    
    @Override
    public <HostT extends AwsInstance<ShardingKey>> HostT getHostByPublicIpAddress(com.sap.sse.landscape.Region region, String publicIpAddress, HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getHost(region, getInstanceByPublicIpAddress(region, publicIpAddress), hostSupplier);
    }

    @Override
    public Instance getInstanceByPrivateIpAddress(com.sap.sse.landscape.Region region, String privateIpAddress) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(privateIpAddress);
            return getEc2Client(getRegion(region))
                    .describeInstances(b->b.filters(Filter.builder().name("private-ip-address").values(inetAddress.getHostAddress()).build())).reservations()
                    .iterator().next().instances().iterator().next();
        } catch (UnknownHostException | NoSuchElementException e) {
            logger.warning("IP address for "+privateIpAddress+" not found");
            return null;
        }
    }

    @Override
    public <HostT extends AwsInstance<ShardingKey>> HostT getHostByPrivateIpAddress(com.sap.sse.landscape.Region region, String publicIpAddress, HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getHost(region, getInstanceByPrivateIpAddress(region, publicIpAddress), hostSupplier);
    }

    private Route53Client getRoute53Client() {
        return getClient(Route53Client.builder(), getRegion(globalRegion));
    }
    
    private Route53AsyncClient getRoute53AsyncClient() {
        return getClient(Route53AsyncClient.builder(), getRegion(globalRegion));
    }
    
    @Override
    public ChangeInfo setDNSRecordToHost(String hostedZoneId, String hostname, Host host, boolean force) {
        final String ipAddressAsString = host.getPublicAddress().getHostAddress();
        return setDNSRecordToValue(hostedZoneId, hostname, ipAddressAsString, /* force */ false);
    }
    
    @Override
    public ChangeInfo setDNSRecordToApplicationLoadBalancer(String hostedZoneId, String hostname,
            ApplicationLoadBalancer<ShardingKey> alb, boolean force) {
        final String dnsName = alb.getDNSName();
        return setDNSRecord(hostedZoneId, hostname, RRType.CNAME, dnsName, force);
    }

    @Override
    public ChangeInfo setDNSRecordToValue(String hostedZoneId, String hostname, String value, boolean force) {
        return setDNSRecord(hostedZoneId, hostname, RRType.A, value, force);
    }

    @Override
    public String getDNSHostedZoneId(String hostedZoneName) {
        return getRoute53Client().listHostedZonesByName(b->b.dnsName(hostedZoneName)).hostedZones().iterator().next().id().replaceFirst("^\\/hostedzone\\/", "");
    }

    private ChangeInfo setDNSRecord(String hostedZoneId, String hostname, RRType type, String value, boolean force) {
        final Route53Client route53Client = getRoute53Client();
        final ListResourceRecordSetsIterable existingResourceRecordSets = route53Client.listResourceRecordSetsPaginator(b->b.hostedZoneId(hostedZoneId).startRecordName(hostname));
        final Set<String> oldValues = new HashSet<>();
        boolean foundEqualValueForHostname = false;
        if (!force) {
            outer: for (final ListResourceRecordSetsResponse rrrs : existingResourceRecordSets) {
                for (final ResourceRecordSet rrs : rrrs.resourceRecordSets()) {
                    if (AwsLandscape.removeTrailingDotFromHostname(rrs.name()).toLowerCase().equals(hostname.toLowerCase())) {
                        for (final ResourceRecord rr : rrs.resourceRecords()) {
                            if (rr.value().equals(value)) {
                                foundEqualValueForHostname = true;
                                break outer;
                            } else {
                                oldValues.add(rr.value());
                            }
                        }
                    }
                }
            }
            if (!foundEqualValueForHostname && !oldValues.isEmpty()) {
                throw new IllegalStateException("A resource record set named "+hostname+" already exists in hosted zone "+hostedZoneId+
                        ", its values "+oldValues+" do not contain the desired value "+value+" and the \"force\" option was not set");
            }
        }
        final ChangeResourceRecordSetsResponse response = route53Client
                .changeResourceRecordSets(
                        ChangeResourceRecordSetsRequest.builder().hostedZoneId(hostedZoneId)
                                .changeBatch(ChangeBatch.builder().changes(Change.builder().action(ChangeAction.UPSERT)
                                        .resourceRecordSet(ResourceRecordSet.builder().name(hostname.toLowerCase()).type(type).ttl(DEFAULT_DNS_TTL_MILLIS)
                                                .resourceRecords(ResourceRecord.builder().value(value).build()).build())
                                        .build()).build())
                                .build());
        return response.changeInfo();
    }

    @Override
    public ChangeInfo removeDNSRecord(String hostedZoneId, String hostname, String value) {
        return removeDNSRecord(hostedZoneId, hostname, RRType.A, value);
    }

    @Override
    public ChangeInfo removeDNSRecord(String hostedZoneId, String hostname, RRType type, String value) {
        return getRoute53Client().changeResourceRecordSets(ChangeResourceRecordSetsRequest.builder().hostedZoneId(hostedZoneId)
                .changeBatch(ChangeBatch.builder().changes(Change.builder().action(ChangeAction.DELETE)
                        .resourceRecordSet(ResourceRecordSet.builder().name(hostname).type(type).ttl(DEFAULT_DNS_TTL_MILLIS)
                                .resourceRecords(ResourceRecord.builder().value(value).build()).build()).build()).build()).build()).
                changeInfo();
    }

    @Override
    public ChangeInfo getUpdatedChangeInfo(ChangeInfo changeInfo) {
        return getRoute53Client().getChange(GetChangeRequest.builder().id(changeInfo.id()).build()).changeInfo();
    }

    @Override
    public AmazonMachineImage<ShardingKey> getImage(com.sap.sse.landscape.Region region, String imageId) {
        final DescribeImagesResponse response = getEc2Client(getRegion(region))
                .describeImages(DescribeImagesRequest.builder().imageIds(imageId).build());
        return new AmazonMachineImageImpl<>(response.images().iterator().next(), region, this);
    }
    
    @Override
    public AmazonMachineImage<ShardingKey> createImage(AwsInstance<ShardingKey> instance, String imageName, Optional<Tags> tags) {
        logger.info("Creating Amazon Machine Image (AMI) named "+imageName+" for instance "+instance.getInstanceId());
        final Ec2Client client = getEc2Client(getRegion(instance.getRegion()));
        final String imageId = client.createImage(b->b
                .instanceId(instance.getInstanceId())
                .name(imageName)).imageId();
        final CreateTagsRequest.Builder createTagsRequestBuilder = CreateTagsRequest.builder().resources(imageId);
        // Apply the tags if present
        tags.ifPresent(t->t.forEach(tag->createTagsRequestBuilder.tags(Tag.builder().key(tag.getKey()).value(tag.getValue()).build())));
        client.createTags(createTagsRequestBuilder.build());
        return getImage(instance.getRegion(), imageId);
    }

    @Override
    public void deleteImage(com.sap.sse.landscape.Region region, String imageId) {
        getEc2Client(getRegion(region)).deregisterImage(b->b.imageId(imageId));
    }

    @Override
    public AmazonMachineImage<ShardingKey> getLatestImageWithTag(com.sap.sse.landscape.Region region, String tagName, String tagValue) {
        final DescribeImagesResponse response = getEc2Client(getRegion(region))
                .describeImages(DescribeImagesRequest.builder().filters(Filter.builder().name("tag:"+tagName).values(tagValue).build()).build());
        return new AmazonMachineImageImpl<>(response.images().stream().max(getMachineImageCreationDateComparator()).get(), region, this);
    }
    
    @Override
    public Iterable<AmazonMachineImage<ShardingKey>> getAllImagesWithTag(com.sap.sse.landscape.Region region,
            String tagName, String tagValue) {
        final DescribeImagesResponse response = getEc2Client(getRegion(region))
                .describeImages(DescribeImagesRequest.builder().filters(Filter.builder().name("tag:"+tagName).values(tagValue).build()).build());
        return Util.map(response.images(), image->new AmazonMachineImageImpl<>(image, region, this));
    }

    @Override
    public Iterable<String> getMachineImageTypes(com.sap.sse.landscape.Region region) {
        final DescribeImagesResponse response = getEc2Client(getRegion(region))
                .describeImages(DescribeImagesRequest.builder().filters(
                        Filter.builder().name("tag-key").values(IMAGE_TYPE_TAG_NAME).build()).build());
        final Set<String> result = new HashSet<>();
        Util.addAll(Util.map(response.images(), image->image.tags().stream().filter(t->t.key().equals(IMAGE_TYPE_TAG_NAME)).findAny().get().value()), result);
        return result;
    }

    @Override
    public void setSnapshotName(com.sap.sse.landscape.Region region, String snapshotId, String snapshotName) {
        getEc2Client(getRegion(region)).createTags(b->b
                .resources(snapshotId)
                .tags(Tag.builder().key("Name").value(snapshotName).build()));
    }

    @Override
    public void deleteSnapshot(com.sap.sse.landscape.Region region, String snapshotId) {
        getEc2Client(getRegion(region)).deleteSnapshot(b->b.snapshotId(snapshotId));
    }

    @Override
    public <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> getHostsWithTagValue(com.sap.sse.landscape.Region region,
            String tagName, String tagValue, HostSupplier<ShardingKey, HostT> hostSupplier) {
        final Filter filter = getHostsWithTagValueFilter(Filter.builder(), tagName, tagValue).build();
        return getHostsWithFilters(region, hostSupplier, filter);
    }

    private Filter.Builder getHostsWithTagValueFilter(Filter.Builder builder, String tagName, String tagValue) {
        return builder.name("tag:"+tagName).values(tagValue);
    }

    @Override
    public <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> getRunningHostsWithTagValue(com.sap.sse.landscape.Region region,
            String tagName, String tagValue, HostSupplier<ShardingKey, HostT> hostSupplier) {
        final Filter tagFilter = getHostsWithTagValueFilter(Filter.builder(), tagName, tagValue).build();
        final Filter runningFilter = getRunningHostsFilter();
        return getHostsWithFilters(region, hostSupplier, tagFilter, runningFilter);
    }
    
    private Filter getRunningHostsFilter() {
        return getRunningHostsFilter(Filter.builder()).build();
    }

    private Filter.Builder getRunningHostsFilter(Filter.Builder builder) {
        return builder.name("instance-state-name").values("running");
    }

    private <HostT extends AwsInstance<ShardingKey>>
    Iterable<HostT> getHostsWithFilters(com.sap.sse.landscape.Region region, HostSupplier<ShardingKey, HostT> hostSupplier, Filter... filters) {
        final List<HostT> result = new ArrayList<>();
        final DescribeInstancesResponse instanceResponse = getEc2Client(getRegion(region)).describeInstances(b->b.filters(filters));
        for (final Reservation r : instanceResponse.reservations()) {
            for (final Instance i : r.instances()) {
                result.add(getHost(region, i, hostSupplier));
            }
        }
        return result;
    }

    private <HostT extends AwsInstance<ShardingKey>>
    HostT getHost(com.sap.sse.landscape.Region region, final Instance instance, HostSupplier<ShardingKey, HostT> hostSupplier) {
        try {
            return hostSupplier.supply(instance.instanceId(),
                    getAvailabilityZoneByName(region, instance.placement().availabilityZone()),
                    InetAddress.getByName(instance.privateIpAddress()), TimePoint.of(instance.launchTime().toEpochMilli()), this);
        } catch (UnknownHostException e) {
            logger.warning("This shouldn't have occurred. "+instance.privateIpAddress()+" was expected to be parsable by InetAddress.getByName(...) but it wasn't.");
            throw new RuntimeException(e);
        }
    }

    private <HostT extends AwsInstance<ShardingKey>>
    HostT getHost(com.sap.sse.landscape.Region region, final String instanceId, HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getHost(region, getInstance(instanceId, region), hostSupplier);
    }
    
    @Override
    public <HostT extends AwsInstance<ShardingKey>>
    HostT getHostByInstanceId(com.sap.sse.landscape.Region region, final String instanceId, HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getHost(region, instanceId,hostSupplier);
    }
    
    @Override
    public <HostT extends AwsInstance<ShardingKey>>
    Iterable<HostT> getRunningHostsWithTag(com.sap.sse.landscape.Region region, String tagName, HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getHostsWithFilters(region, hostSupplier, getFilterForHostWithTag(Filter.builder(), tagName), getRunningHostsFilter());
    }
    
    @Override
    public <HostT extends AwsInstance<ShardingKey>>
    Iterable<HostT> getHostsWithTag(com.sap.sse.landscape.Region region, String tagName, HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getHostsWithFilters(region, hostSupplier, getFilterForHostWithTag(Filter.builder(), tagName));
    }
    
    private Filter getFilterForHostWithTag(Filter.Builder builder, String tagName) {
        return builder.name("tag-key").values(tagName).build();
    }

    private Comparator<? super Image> getMachineImageCreationDateComparator() {
        return (ami1, ami2)->{
            return ami1.creationDate().compareTo(ami2.creationDate());
        };
    }

    /**
     * If a {@link #sessionToken} was provided to this landscape, use it to create {@link AwsSessionCredentials}; otherwise
     * an {@link AwsBasicCredentials} object will be produced from the {@link #accessKeyId} and the {@link #secretAccessKey}.
     * @return
     */
    private AwsCredentials getCredentials() {
        return sessionToken.map(nonEmptySessionToken->(AwsCredentials) AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken.get()))
                .orElse(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }
    
    @Override
    public Credentials getMfaSessionCredentials(String nonEmptyMfaTokenCode) {
        final AwsBasicCredentials basicCredentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        final List<MFADevice> mfaDevices = IamClient.builder().region(Region.AWS_GLOBAL).credentialsProvider(()->basicCredentials).build()
            .listMFADevices().mfaDevices();
        logger.info("Found the following MFA devices: "+Util.joinStrings(", ", Util.map(mfaDevices, d->d.serialNumber())));
        final String serialNumberOfMfaDevice = mfaDevices.iterator().next().serialNumber();
        logger.info("Found MFA device "+serialNumberOfMfaDevice+"; using MFA token code "+nonEmptyMfaTokenCode);
        final Credentials result = StsClient.builder().region(Region.AWS_GLOBAL).credentialsProvider(()->basicCredentials).build()
            .getSessionToken(b->b.tokenCode(nonEmptyMfaTokenCode).serialNumber(serialNumberOfMfaDevice)).credentials();
        logger.info("Produced valid MFA session credentials for access key ID "+result.accessKeyId());
        return result;
    }
    
    @Override
    public KeyPairInfo getKeyPairInfo(com.sap.sse.landscape.Region region, String keyName) {
        return getEc2Client(getRegion(region))
                .describeKeyPairs(DescribeKeyPairsRequest.builder().keyNames(keyName).build()).keyPairs().iterator()
                .next();
    }

    @Override
    public Iterable<KeyPairInfo> getAllKeyPairInfos(com.sap.sse.landscape.Region region) {
        return getEc2Client(getRegion(region))
                .describeKeyPairs(DescribeKeyPairsRequest.builder().build()).keyPairs();
    }

    @Override
    public void deleteKeyPair(com.sap.sse.landscape.Region region, String keyName) {
        getEc2Client(getRegion(region)).deleteKeyPair(DeleteKeyPairRequest.builder().keyName(keyName).build());
        landscapeState.deleteKeyPair(region.getId(), keyName);
    }

    @Override
    public SSHKeyPair importKeyPair(com.sap.sse.landscape.Region region, byte[] publicKey, byte[] encryptedPrivateKey, String keyName) throws JSchException {
        if (!KeyPair.load(new JSch(), encryptedPrivateKey, publicKey).isEncrypted()) {
            throw new IllegalArgumentException("Expected an encrypted private key");
        }
        try {
            getEc2Client(getRegion(region)).importKeyPair(ImportKeyPairRequest.builder().keyName(keyName)
                    .publicKeyMaterial(SdkBytes.fromByteArray(publicKey)).build());
        } catch (Exception e) {
            // this didn't work; if it didn't work because a key by that name already exists, let's still try to import the
            // key into this Landscape, only making this Landscape aware of the key pair for which the public key had been
            // uploaded to AWS earlier.
            if (e.getMessage().contains("The keypair '"+keyName+"' already exists")) {
                logger.info("A key named " + keyName + " already exists in the AWS region " + region.getId()
                        + ". No problem; trying to import into this landscape.");
            } else {
                logger.info("Error trying to import a public key into the landscape: "+e.getMessage());
                throw e;
            }
        }
        Object principal;
        try {
            principal = SessionUtils.getPrincipal();
        } catch (Exception e) {
            logger.severe("Couldn't find current user; continuing anonymously");
            principal = null;
        }
        final SSHKeyPair keyPair = new SSHKeyPair(region.getId(), principal==null?"":principal.toString(),
                TimePoint.now(), keyName, publicKey, encryptedPrivateKey);
        landscapeState.addSSHKeyPair(keyPair);
        return keyPair;
    }

    @Override
    public SSHKeyPair getSSHKeyPair(com.sap.sse.landscape.Region region, String keyName) {
        return landscapeState.getSSHKeyPair(region.getId(), keyName);
    }
    
    @Override
    public Iterable<SSHKeyPair> getSSHKeyPairs() {
        return landscapeState.getSSHKeyPairs();
    }

    @Override
    public byte[] getDecryptedPrivateKey(SSHKeyPair keyPair, byte[] privateKeyEncryptionPassphrase) throws JSchException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        keyPair.getKeyPair(new JSch(), privateKeyEncryptionPassphrase).writePrivateKey(bos);
        return bos.toByteArray();
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    Map<Scope<ShardingKey>, ApplicationReplicaSet<ShardingKey, MetricsT, ProcessT>> getScopes() {
        // TODO Implement Landscape<ShardingKey,MetricsT>.getScopes(...)
        return null;
    }
    
    @Override
    public <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> launchHosts(HostSupplier<ShardingKey, HostT> hostSupplier,
            int numberOfHostsToLaunch, MachineImage fromImage,
            InstanceType instanceType, AwsAvailabilityZone az, String keyName, Iterable<SecurityGroup> securityGroups, Optional<Tags> tags, String... userData) {
        if (!fromImage.getRegion().equals(az.getRegion())) {
            throw new IllegalArgumentException("Trying to launch an instance in region "+az.getRegion()+
                    " with image "+fromImage+" that lives in region "+fromImage.getRegion()+" which is different."+
                    " Consider copying the image to that region.");
        }
        final Builder runInstancesRequestBuilder = RunInstancesRequest.builder()
            .additionalInfo("Test " + getClass().getName())
            .imageId(fromImage.getId().toString())
            .minCount(numberOfHostsToLaunch)
            .maxCount(numberOfHostsToLaunch)
            .instanceType(instanceType).keyName(keyName)
            .placement(Placement.builder().availabilityZone(az.getName()).build())
            .securityGroupIds(Util.mapToArrayList(securityGroups, sg->sg.getId()));
        if (userData != null) {
            runInstancesRequestBuilder.userData(Base64.getEncoder().encodeToString(String.join("\n", userData).getBytes()));
        }
        tags.ifPresent(theTags->{
            final Collection<Tag> awsTags = getAwsTags(theTags);
            runInstancesRequestBuilder.tagSpecifications(TagSpecification.builder().resourceType(ResourceType.INSTANCE).tags(awsTags).build());
        });
        final RunInstancesRequest launchRequest = runInstancesRequestBuilder.build();
        logger.info("Launching instance(s): "+launchRequest);
        final RunInstancesResponse response = getEc2Client(getRegion(az.getRegion())).runInstances(launchRequest);
        final List<HostT> result = new ArrayList<>();
        for (final Instance instance : response.instances()) {
            try {
                result.add(hostSupplier.supply(instance.instanceId(), az,
                        InetAddress.getByName(instance.privateIpAddress()), TimePoint.of(instance.launchTime().toEpochMilli()),
                        this));
            } catch (UnknownHostException e) {
                logger.warning("This shouldn't have occurred. "+instance.privateIpAddress()+" was expected to be parsable by InetAddress.getByName(...) but it wasn't.");
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private Collection<Tag> getAwsTags(Tags tags) {
        final List<Tag> awsTags = new ArrayList<>();
        for (final Entry<String, String> tag : tags) {
            awsTags.add(Tag.builder().key(tag.getKey()).value(tag.getValue()).build());
        }
        return awsTags;
    }

    @Override
    public void terminate(AwsInstance<ShardingKey> host) {
        logger.info("Terminating instance "+host);
        getEc2Client(getRegion(host.getAvailabilityZone().getRegion())).terminateInstances(
                TerminateInstancesRequest.builder().instanceIds(host.getInstanceId()).build());
    }

    private Region getRegion(com.sap.sse.landscape.Region region) {
        return Region.of(region.getId());
    }
    
    /**
     * The health check port is provided as a {@link String} and can assume the value {@code "traffic-port"} in which
     * case the numerical port is that returned by {@link software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup#port()}.
     */
    public static Integer getHealthCheckPort(software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup targetGroup) {
        return targetGroup.healthCheckPort() == null
                ? null
                : targetGroup.healthCheckPort().equals("traffic-port")
                  ? targetGroup.port()
                  : Integer.valueOf(targetGroup.healthCheckPort());
    }

    @Override
    public Iterable<AwsAvailabilityZone> getAvailabilityZones(com.sap.sse.landscape.Region awsRegion) {
        return Util.map(getEc2Client(getRegion(awsRegion)).describeAvailabilityZones().availabilityZones(),
                az->new AwsAvailabilityZoneImpl(az, this));
    }

    @Override
    public TargetGroup<ShardingKey> getTargetGroup(com.sap.sse.landscape.Region region, String targetGroupName, String loadBalancerArn) {
        final ElasticLoadBalancingV2Client loadBalancingClient = getLoadBalancingClient(getRegion(region));
        final DescribeTargetGroupsResponse targetGroupResponse = loadBalancingClient.describeTargetGroups(b->b.names(targetGroupName));
        final software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup targetGroup = targetGroupResponse.targetGroups().iterator().next();
        return targetGroupResponse.hasTargetGroups()
                ? new AwsTargetGroupImpl<>(this, region, targetGroupName, targetGroup.targetGroupArn(),
                        loadBalancerArn == null ? Util.first(targetGroup.loadBalancerArns()) : loadBalancerArn,
                        targetGroup.protocol(), targetGroup.port(),
                        targetGroup.healthCheckProtocol(), getHealthCheckPort(targetGroup), targetGroup.healthCheckPath())
                : null;
    }

    @Override
    public TargetGroup<ShardingKey> getTargetGroup(com.sap.sse.landscape.Region region, String targetGroupName) {
        return getTargetGroup(region, targetGroupName, /* discover load balancer ARN from target group */ null);
    }

    @Override
    public TargetGroup<ShardingKey> createTargetGroup(com.sap.sse.landscape.Region region, String targetGroupName, int port,
            String healthCheckPath, int healthCheckPort, String loadBalancerArn) {
        final ElasticLoadBalancingV2Client loadBalancingClient = getLoadBalancingClient(getRegion(region));
        final software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup targetGroup =
                loadBalancingClient.createTargetGroup(CreateTargetGroupRequest.builder()
                .name(targetGroupName)
                .healthyThresholdCount(2)
                .unhealthyThresholdCount(2)
                .healthCheckTimeoutSeconds(4)
                .healthCheckEnabled(true)
                .healthCheckIntervalSeconds(5)
                .healthCheckPath(healthCheckPath)
                .healthCheckPort(""+healthCheckPort)
                .healthCheckProtocol(guessProtocolFromPort(healthCheckPort))
                .port(port)
                .vpcId(getVpcId(region))
                .protocol(guessProtocolFromPort(port))
                .targetType(TargetTypeEnum.INSTANCE)
                .build()).targetGroups().iterator().next();
        final String targetGroupArn = targetGroup.targetGroupArn();
        loadBalancingClient.modifyTargetGroupAttributes(ModifyTargetGroupAttributesRequest.builder()
                .targetGroupArn(targetGroupArn)
                .attributes(TargetGroupAttribute.builder().key("stickiness.enabled").value("true").build(),
                            TargetGroupAttribute.builder().key("load_balancing.algorithm.type").value("least_outstanding_requests")
                            .build()).build());
        return new AwsTargetGroupImpl<>(this, region, targetGroupName, targetGroupArn, loadBalancerArn,
                targetGroup.protocol(), port, targetGroup.healthCheckProtocol(), healthCheckPort, healthCheckPath);
    }

    private ProtocolEnum guessProtocolFromPort(int healthCheckPort) {
        return healthCheckPort == 443 ? ProtocolEnum.HTTPS : ProtocolEnum.HTTP;
    }
    
    private String getVpcId(com.sap.sse.landscape.Region region) {
        Vpc vpc = getEc2Client(getRegion(region)).describeVpcs().vpcs().stream().filter(myVpc->myVpc.isDefault()).findAny().
                orElseThrow(()->new IllegalStateException("No default VPC found in region "+region));
        return vpc.vpcId();
    }

    @Override
    public software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroup(com.sap.sse.landscape.Region region, String targetGroupName) {
        return getLoadBalancingClient(getRegion(region)).describeTargetGroups(DescribeTargetGroupsRequest.builder()
                        .names(targetGroupName).build()).targetGroups().iterator().next();
    }
    
    @Override
    public software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroupByArn(com.sap.sse.landscape.Region region, String targetGroupArn) {
        return getLoadBalancingClient(getRegion(region)).describeTargetGroups(DescribeTargetGroupsRequest.builder()
                        .targetGroupArns(targetGroupArn).build()).targetGroups().iterator().next();
    }
    
    @Override
    public <SK> void deleteTargetGroup(TargetGroup<SK> targetGroup) {
        logger.info("Deleting target group "+targetGroup);
        getLoadBalancingClient(getRegion(targetGroup.getRegion())).deleteTargetGroup(DeleteTargetGroupRequest.builder().targetGroupArn(
                targetGroup.getTargetGroupArn()).build());
    }
    
    @Override
    public Map<AwsInstance<ShardingKey>, TargetHealth> getTargetHealthDescriptions(TargetGroup<ShardingKey> targetGroup) {
        final Map<AwsInstance<ShardingKey>, TargetHealth> result = new HashMap<>();
        final Region region = getRegion(targetGroup.getRegion());
        getLoadBalancingClient(region)
                .describeTargetHealth(DescribeTargetHealthRequest.builder().targetGroupArn(targetGroup.getTargetGroupArn()).build())
                .targetHealthDescriptions().forEach(
                    targetHealthDescription->result.put(
                            getHost(targetGroup.getRegion(), targetHealthDescription.target().id(), AwsInstanceImpl::new), targetHealthDescription.targetHealth()));
        return result;
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> getCentralReverseProxy(com.sap.sse.landscape.Region region) {
        ApacheReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> reverseProxyCluster = new ApacheReverseProxyCluster<>(this);
        for (final AwsInstance<ShardingKey> reverseProxyHost : getRunningHostsWithTag(region, CENTRAL_REVERSE_PROXY_TAG_NAME, AwsInstanceImpl::new)) {
            reverseProxyCluster.addHost(reverseProxyHost);
        }
        return reverseProxyCluster;
    }

    @Override
    public SecurityGroup getSecurityGroup(String securityGroupId, com.sap.sse.landscape.Region region) {
        return ()->getEc2Client(getRegion(region)).describeSecurityGroups(sg->sg.groupIds(securityGroupId)).securityGroups().iterator().next().groupId();
    }

    @Override
    public void addTargetsToTargetGroup(
            TargetGroup<ShardingKey> targetGroup,
            Iterable<AwsInstance<ShardingKey>> targets) {
        getLoadBalancingClient(getRegion(targetGroup.getRegion())).registerTargets(getRegisterTargetsRequestBuilderConsumer(targetGroup, targets));
    }

    private TargetDescription[] getTargetDescriptions(Iterable<AwsInstance<ShardingKey>> targets) {
        return Util.toArray(Util.map(targets, t->TargetDescription.builder().id(t.getInstanceId()).build()), new TargetDescription[0]);
    }

    @Override
    public void removeTargetsFromTargetGroup(
            TargetGroup<ShardingKey> targetGroup,
            Iterable<AwsInstance<ShardingKey>> targets) {
        getLoadBalancingClient(getRegion(targetGroup.getRegion())).deregisterTargets(getDeregisterTargetRequestBuilderConsumers(targetGroup, targets));
    }

    private Consumer<software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest.Builder> getRegisterTargetsRequestBuilderConsumer(
            TargetGroup<ShardingKey> targetGroup, Iterable<AwsInstance<ShardingKey>> targets) {
        final TargetDescription[] targetDescriptions = getTargetDescriptions(targets);
        return b->b
                .targetGroupArn(targetGroup.getTargetGroupArn())
                .targets(targetDescriptions);
    }

    private Consumer<software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsRequest.Builder> getDeregisterTargetRequestBuilderConsumers(
            TargetGroup<ShardingKey> targetGroup, Iterable<AwsInstance<ShardingKey>> targets) {
        final TargetDescription[] targetDescriptions = getTargetDescriptions(targets);
        return b->b
                .targetGroupArn(targetGroup.getTargetGroupArn())
                .targets(targetDescriptions);
    }

    @Override
    public LoadBalancer getAwsLoadBalancer(String loadBalancerArn, com.sap.sse.landscape.Region region) {
        return getLoadBalancingClient(getRegion(region))
                .describeLoadBalancers(lb -> lb.loadBalancerArns(loadBalancerArn)).loadBalancers().iterator().next();
    }

    @Override
    public SecurityGroup getDefaultSecurityGroupForApplicationHosts(com.sap.sse.landscape.Region region) {
        final SecurityGroup result;
        // TODO find a better way, e.g., by tagging, to identify the security group per region to use for application hosts
        if (region.getId().equals(Region.EU_WEST_1.id())) {
            result = getSecurityGroup(DEFAULT_APPLICATION_SERVER_SECURITY_GROUP_ID_EU_WEST_1, region);
        } else if (region.getId().equals(Region.EU_WEST_2.id())) {
            result = getSecurityGroup(DEFAULT_APPLICATION_SERVER_SECURITY_GROUP_ID_EU_WEST_2, region);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public SecurityGroup getDefaultSecurityGroupForCentralReverseProxy(com.sap.sse.landscape.Region region) {
        return getDefaultSecurityGroupForApplicationHosts(region);
    }

    @Override
    public SecurityGroup getDefaultSecurityGroupForApplicationLoadBalancer(com.sap.sse.landscape.Region region) {
        return getDefaultSecurityGroupForApplicationHosts(region);
    }

    @Override
    public SecurityGroup getDefaultSecurityGroupForMongoDBHosts(com.sap.sse.landscape.Region region) {
        final SecurityGroup result;
        // TODO find a better way, e.g., by tagging, to identify the security group per region to use for MongoDB hosts
        if (region.getId().equals(Region.EU_WEST_1.id())) {
            result = getSecurityGroup(DEFAULT_MONGODB_SECURITY_GROUP_ID_EU_WEST_1, region);
        } else if (region.getId().equals(Region.EU_WEST_2.id())) {
            result = getSecurityGroup(DEFAULT_MONGODB_SECURITY_GROUP_ID_EU_WEST_2, region);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> getNonDNSMappedLoadBalancer(
            com.sap.sse.landscape.Region region, String wildcardDomain) {
        return getLoadBalancerByName(getNonDNSMappedLoadBalancerName(wildcardDomain), region);
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> createNonDNSMappedLoadBalancer(
            com.sap.sse.landscape.Region region, String wildcardDomain) throws InterruptedException, ExecutionException {
        return createLoadBalancer(getNonDNSMappedLoadBalancerName(wildcardDomain), region);
    }

    private String getNonDNSMappedLoadBalancerName(String wildcardDomain) {
        return DEFAULT_NON_DNS_MAPPED_ALB_NAME + wildcardDomain.replaceAll("\\.", "-");
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> getDNSMappedLoadBalancerFor(
            com.sap.sse.landscape.Region region, String hostname) {
        final DescribeLoadBalancersResponse response = getLoadBalancingClient(getRegion(region)).describeLoadBalancers();
        for (final LoadBalancer lb : response.loadBalancers()) {
            final ApplicationLoadBalancer<ShardingKey> alb = new ApplicationLoadBalancerImpl<>(region, lb, this);
            for (final Rule rule : alb.getRules()) {
                if (rule.conditions().stream().filter(r->r.hostHeaderConfig().values().contains(hostname)).findAny().isPresent()) {
                    return alb;
                }
            }
        }
        return null;
    }
    
    @Override
    public MongoEndpoint getDatabaseConfigurationForDefaultReplicaSet(com.sap.sse.landscape.Region region) {
        return getDatabaseConfigurationForReplicaSet(region, MONGO_DEFAULT_REPLICA_SET_NAME);
    }
    
    private int getMongoPort(String[] replicaSetNameAndOptionalPort) {
        final int result;
        if (replicaSetNameAndOptionalPort.length < 2) {
            result = MongoDBConstants.DEFAULT_PORT;
        } else {
            result = Integer.valueOf(replicaSetNameAndOptionalPort[1].trim());
        }
        return result;
    }

    @Override
    public Optional<String> getTag(AwsInstance<ShardingKey> host, String tagName) {
        final DescribeTagsResponse tagResponse = getEc2Client(getRegion(host.getRegion())).describeTags(b->b.filters(
                Filter.builder()
                    .name("resource-id").values(host.getInstanceId()).build(),
                Filter.builder()
                    .name("key").values(tagName).build()));
        return tagResponse.tags().stream().map(t->t.value()).findAny();
    }

    @Override
    public Tags getTagForMongoProcess(Tags tagsToAddTo, String replicaSetName, int port) {
        return tagsToAddTo.and(MONGO_REPLICA_SETS_TAG_NAME,
                (replicaSetName==null?"":replicaSetName)+MONGO_REPLICA_SET_NAME_AND_PORT_SEPARATOR+port);
    }

    @Override
    public MongoReplicaSet getDatabaseConfigurationForReplicaSet(com.sap.sse.landscape.Region region, String mongoReplicaSetName) {
        final Set<Pair<AwsInstance<ShardingKey>, Integer>> nodes = new HashSet<>();
        for (final AwsInstance<ShardingKey> host : getMongoDBHosts(region)) {
            for (final Pair<String, Integer> replicaSetNameAndPort : getMongoEndpointSpecificationsAsReplicaSetNameAndPort(host)) {
                if (replicaSetNameAndPort.getA().equals(mongoReplicaSetName)) {
                    nodes.add(new Pair<>(host, replicaSetNameAndPort.getB()));
                }
            }
        }
        return getDatabaseConfigurationForReplicaSet(mongoReplicaSetName, nodes);
    }
    
    @Override
    public MongoReplicaSet getDatabaseConfigurationForReplicaSet(String mongoReplicaSetName, Iterable<Pair<AwsInstance<ShardingKey>, Integer>> hostsAndPortsOfNodes) {
        final MongoReplicaSet result = new MongoReplicaSetImpl(mongoReplicaSetName);
        for (final Pair<AwsInstance<ShardingKey>, Integer> hostAndPort : hostsAndPortsOfNodes) {
            result.addReplica(new MongoProcessInReplicaSetImpl(result, hostAndPort.getB(), hostAndPort.getA()));
        }
        return result;
    }
    
    @Override
    public MongoProcessImpl getDatabaseConfigurationForSingleNode(AwsInstance<ShardingKey> host, int port) {
        return new MongoProcessImpl(host, port);
    }

    private Iterable<AwsInstance<ShardingKey>> getMongoDBHosts(com.sap.sse.landscape.Region region) {
        return getRunningHostsWithTag(region, MONGO_REPLICA_SETS_TAG_NAME, AwsInstanceImpl::new);
    }

    /**
     * @param host
     *            assumed to be a host that has the {@link #MONGO_REPLICA_SETS_TAG_NAME} tag set
     * @return the replica set name / port number pairs extracted from the tag value
     */
    private Iterable<Pair<String, Integer>> getMongoEndpointSpecificationsAsReplicaSetNameAndPort(final AwsInstance<ShardingKey> host) {
        final List<Pair<String, Integer>> result = new ArrayList<>();
        getTag(host, MONGO_REPLICA_SETS_TAG_NAME).ifPresent(tagValue->{
            for (final String replicaNameWithOptionalPortColonSeparated : tagValue.split(",")) {
                final String[] splitByColon = replicaNameWithOptionalPortColonSeparated.split(MONGO_REPLICA_SET_NAME_AND_PORT_SEPARATOR);
                final int port = getMongoPort(splitByColon);
                result.add(new Pair<>(splitByColon[0].trim(), port));
            }});
        return result;
    }

    @Override
    public Iterable<MongoEndpoint> getMongoEndpoints(com.sap.sse.landscape.Region region) {
        final Set<MongoEndpoint> result = new HashSet<>();
        final Set<String> replicaSetsCreated = new HashSet<>();
        for (final AwsInstance<ShardingKey> mongoDBHost : getMongoDBHosts(region)) {
            for (final Pair<String, Integer> replicaSetNameAndPort : getMongoEndpointSpecificationsAsReplicaSetNameAndPort(mongoDBHost)) {
                if (replicaSetNameAndPort.getA() != null && !replicaSetNameAndPort.getA().isEmpty()) { // non-empty replica set name
                    if (!replicaSetsCreated.contains(replicaSetNameAndPort.getA())) {
                        replicaSetsCreated.add(replicaSetNameAndPort.getA());
                        result.add(getDatabaseConfigurationForReplicaSet(region, replicaSetNameAndPort.getA()));
                    }
                } else {
                    // single instance:
                    result.add(new MongoProcessImpl(mongoDBHost, replicaSetNameAndPort.getB()));
                }
            }
        }
        return result;
    }

    @Override
    public RabbitMQEndpoint getDefaultRabbitConfiguration(AwsRegion region) {
        final RabbitMQEndpoint result;
        final Iterable<AwsInstance<ShardingKey>> rabbitMQHostsInRegion = getRunningHostsWithTag(region, RABBITMQ_TAG_NAME, AwsInstanceImpl::new);
        if (rabbitMQHostsInRegion.iterator().hasNext()) {
            final AwsInstance<ShardingKey> anyRabbitMQHost = rabbitMQHostsInRegion.iterator().next();
            result = new RabbitMQEndpoint() {
                @Override
                public int getPort() {
                    return getTag(anyRabbitMQHost, RABBITMQ_TAG_NAME)
                            .map(t -> t.trim().isEmpty() ? RabbitMQEndpoint.DEFAULT_PORT : Integer.valueOf(t.trim()))
                            .orElse(RabbitMQEndpoint.DEFAULT_PORT);
                }

                @Override
                public String getNodeName() {
                    return anyRabbitMQHost.getPrivateAddress().getHostAddress();
                }
            };
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Database getDatabase(com.sap.sse.landscape.Region region, String databaseName) {
        return new DatabaseImpl(getDatabaseConfigurationForDefaultReplicaSet(region), databaseName);
    }

    @Override
    public RabbitMQEndpoint getMessagingConfigurationForDefaultCluster(com.sap.sse.landscape.Region region) {
        final RabbitMQEndpoint result;
        if (region.getId().equals(Region.EU_WEST_1.id())) {
            result = ()->"rabbit.internal.sapsailing.com";
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>,
    HostT extends ApplicationProcessHost<ShardingKey, MetricsT, ProcessT>>
    Iterable<HostT> getApplicationProcessHostsByTag(com.sap.sse.landscape.Region region, String tagName,
            HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getRunningHostsWithTag(region, tagName, hostSupplier);
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>,
    HostT extends ApplicationProcessHost<ShardingKey, MetricsT, ProcessT>>
    AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> getApplicationReplicaSetByTagValue(
            com.sap.sse.landscape.Region region, String tagName, String tagValue, HostSupplier<ShardingKey, HostT> hostSupplier,
            Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return Util.first(getApplicationReplicaSets(region, ()->getRunningHostsWithTagValue(region, tagName, tagValue, hostSupplier),
                optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase));
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>,
    HostT extends ApplicationProcessHost<ShardingKey, MetricsT, ProcessT>>
    Iterable<AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT>> getApplicationReplicaSetsByTag(
            com.sap.sse.landscape.Region region, String tagName, HostSupplier<ShardingKey, HostT> hostSupplier,
            Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return getApplicationReplicaSets(region, ()->getRunningHostsWithTag(region, tagName, hostSupplier),
                optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    private <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>,
    HostT extends ApplicationProcessHost<ShardingKey, MetricsT, ProcessT>>
    Iterable<AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT>> getApplicationReplicaSets(
            com.sap.sse.landscape.Region region, Supplier<Iterable<HostT>> hostsSupplier,
            Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> allLoadBalancersInRegion = getLoadBalancersAsync(region);
        final CompletableFuture<Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>>> allTargetGroupsInRegion = getTargetGroupsAsync(region);
        final CompletableFuture<Map<Listener, Iterable<Rule>>> allLoadBalancerRulesInRegion = getLoadBalancerListenerRulesAsync(region, allLoadBalancersInRegion);
        final CompletableFuture<Iterable<AutoScalingGroup>> allAutoScalingGroups = getAutoScalingGroupsAsync(region);
        final CompletableFuture<Iterable<LaunchConfiguration>> allLaunchConfigurations = getLaunchConfigurationsAsync(region);
        final Iterable<HostT> hosts = hostsSupplier.get();
        final Map<String, ProcessT> mastersByServerName = new HashMap<>();
        final Map<String, Set<ProcessT>> replicasByServerName = new HashMap<>();
        final ConcurrentLinkedQueue<Future<?>> tasksToWaitFor = new ConcurrentLinkedQueue<>();
        final ScheduledExecutorService backgroundExecutor = ThreadPoolUtil.INSTANCE.createBackgroundTaskThreadPoolExecutor("Application Process Discovery "+UUID.randomUUID());
        for (final ApplicationProcessHost<ShardingKey, MetricsT, ProcessT> host : hosts) {
            tasksToWaitFor.add(backgroundExecutor.submit(()->{
                try {
                    for (final ProcessT applicationProcess : host.getApplicationProcesses(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase)) {
                        tasksToWaitFor.add(backgroundExecutor.submit(()->{
                            String serverName;
                            try {
                                serverName = applicationProcess.getServerName(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
                                final String masterServerName = applicationProcess.getMasterServerName(optionalTimeout);
                                if (masterServerName != null && Util.equalsWithNull(masterServerName, serverName)) {
                                    // then applicationProcess is a replica in the serverName cluster:
                                    synchronized (replicasByServerName) {
                                        Util.addToValueSet(replicasByServerName, serverName, applicationProcess);
                                    }
                                } else {
                                    // check if it's a new or else a newer master:
                                    synchronized (mastersByServerName) {
                                        if (!mastersByServerName.containsKey(serverName)
                                        || Comparator.<TimePoint>nullsLast(Comparator.naturalOrder()).compare(
                                                mastersByServerName.get(serverName).getStartTimePoint(optionalTimeout),
                                                applicationProcess.getStartTimePoint(optionalTimeout)) < 0) {
                                            if (mastersByServerName.containsKey(serverName)) {
                                                logger.warning("Replacing master "+mastersByServerName.get(serverName)+" with newer master "+applicationProcess);
                                            }
                                            mastersByServerName.put(serverName, applicationProcess);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        // wait for all application processes on all hosts to be discovered
        Future<?> taskToWaitFor;
        while ((taskToWaitFor=tasksToWaitFor.poll()) != null) {
            try {
                waitForFuture(taskToWaitFor, optionalTimeout);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Problem waiting for "+taskToWaitFor, e);
            }
        }
        backgroundExecutor.shutdown();
        final Set<AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT>> result = new HashSet<>();
        final DNSCache dnsCache = getNewDNSCache();
        for (final Entry<String, ProcessT> serverNameAndMaster : mastersByServerName.entrySet()) {
            final String serverName = serverNameAndMaster.getKey();
            final ProcessT master = serverNameAndMaster.getValue();
            final Set<ProcessT> replicas = replicasByServerName.get(serverName);
            final AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> replicaSet = getApplicationReplicaSet(
                    serverName, master, replicas, allLoadBalancersInRegion, allTargetGroupsInRegion,
                    allLoadBalancerRulesInRegion, allAutoScalingGroups, allLaunchConfigurations, dnsCache);
            result.add(replicaSet);
        }
        return result;
    }
    
    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> getApplicationReplicaSet(com.sap.sse.landscape.Region region,
            final String serverName, final ProcessT master, final Iterable<ProcessT> replicas) {
        final CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> allLoadBalancersInRegion = getLoadBalancersAsync(region);
        final CompletableFuture<Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>>> allTargetGroupsInRegion = getTargetGroupsAsync(region);
        final CompletableFuture<Map<Listener, Iterable<Rule>>> allLoadBalancerRulesInRegion = getLoadBalancerListenerRulesAsync(region, allLoadBalancersInRegion);
        final CompletableFuture<Iterable<AutoScalingGroup>> autoScalingGroups = getAutoScalingGroupsAsync(region);
        final CompletableFuture<Iterable<LaunchConfiguration>> launchConfigurations = getLaunchConfigurationsAsync(region);
        final DNSCache dnsCache = getNewDNSCache();
        return getApplicationReplicaSet(serverName, master, replicas, allLoadBalancersInRegion, allTargetGroupsInRegion,
                allLoadBalancerRulesInRegion, autoScalingGroups, launchConfigurations, dnsCache);
    }

    private <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> getApplicationReplicaSet(
            final String serverName, final ProcessT master, final Iterable<ProcessT> replicas,
            final CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> allLoadBalancersInRegion,
            final CompletableFuture<Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>>> allTargetGroupsInRegion,
            final CompletableFuture<Map<Listener, Iterable<Rule>>> allLoadBalancerRulesInRegion,
            final CompletableFuture<Iterable<AutoScalingGroup>> allAutoScalingGroups,
            CompletableFuture<Iterable<LaunchConfiguration>> allLaunchConfigurations, final DNSCache dnsCache) {
        final AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> replicaSet = new AwsApplicationReplicaSetImpl<ShardingKey, MetricsT, ProcessT>(
                serverName, master, Optional.ofNullable(replicas), allLoadBalancersInRegion, allTargetGroupsInRegion,
                allLoadBalancerRulesInRegion, this, allAutoScalingGroups, allLaunchConfigurations, dnsCache);
        return replicaSet;
    }
    
    private <T> void waitForFuture(Future<T> future, Optional<Duration> optionalTimeout)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (optionalTimeout.isPresent()) {
            future.get(optionalTimeout.get().asMillis(), TimeUnit.MILLISECONDS);
        } else {
            future.get();
        }
    }
    
    /**
     * This assumes the format {LoadBalancerName}-{[0-9]*}.{RegionId}.elb.amazonaws.com. The region is then parsed from
     * the name. If the pattern is not matched, {@code null} is returned; otherwise, the load balancer name and its
     * region are returned as a pair.
     */
    private Pair<String, AwsRegion> getLoadBalancerNameAndRegionFromLoadBalancerDNSName(String loadBalancerDNSName) {
        final Pattern pattern = Pattern.compile("^([^.]*)-([^-.]*)\\.([^.]*)\\.elb\\.amazonaws\\.com\\.$");
        final Matcher matcher = pattern.matcher(loadBalancerDNSName);
        final Pair<String, AwsRegion> result;
        if (matcher.matches()) {
            result = new Pair<>(matcher.group(1), new AwsRegion(matcher.group(3), this));
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> getLoadBalancerByHostname(String hostname) {
        final TestDnsAnswerResponse dnsAnswer = getRoute53Client().testDNSAnswer(b->b
                .hostedZoneId(getDNSHostedZoneId(AwsLandscape.getHostedZoneName(hostname)))
                .recordType(RRType.CNAME)
                .recordName(hostname));
        final ApplicationLoadBalancer<ShardingKey> result;
        if (dnsAnswer.hasRecordData()) {
            final Pair<String, AwsRegion> nameAndRegion = getLoadBalancerNameAndRegionFromLoadBalancerDNSName(dnsAnswer.recordData().iterator().next());
            result = getLoadBalancerByName(nameAndRegion.getA(), nameAndRegion.getB());
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public CompletableFuture<Iterable<ResourceRecordSet>> getResourceRecordSetsAsync(String hostname) {
        final Route53AsyncClient route53Client = getRoute53AsyncClient();
        final String hostedZoneId = getDNSHostedZoneId(AwsLandscape.getHostedZoneName(hostname));
        return route53Client.listResourceRecordSets(b->b.hostedZoneId(hostedZoneId).startRecordName(hostname)).handle((response, e)->
            Util.filter(response.resourceRecordSets(), resourceRecordSet->AwsLandscape.removeTrailingDotFromHostname(resourceRecordSet.name()).equals(hostname)));
    }
    
    @Override
    public CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> getLoadBalancersAsync(com.sap.sse.landscape.Region region) {
        return getLoadBalancingAsyncClient(getRegion(region)).describeLoadBalancers().handleAsync((response, exception)->
            Util.map(response.loadBalancers(), lb->new ApplicationLoadBalancerImpl<ShardingKey>(region, lb, this)));
    }
    
    @Override
    public CompletableFuture<Listener> getHttpsListenerAsync(com.sap.sse.landscape.Region region, ApplicationLoadBalancer<ShardingKey> loadBalancer) {
        return getLoadBalancingAsyncClient(getRegion(region)).describeListeners(b->b.loadBalancerArn(loadBalancer.getArn())).handleAsync(
                (response, exception)->Util.first(Util.filter(response.listeners(), l->l.protocol() == ProtocolEnum.HTTPS)));
    }
    
    @Override
    public CompletableFuture<Map<Listener, Iterable<Rule>>> getLoadBalancerListenerRulesAsync(com.sap.sse.landscape.Region region,
            CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> allLoadBalancersInRegion) {
        return allLoadBalancersInRegion.thenCompose(loadBalancers->getListenerToRulesMap(region, loadBalancers));
    }
    
    @Override
    public CompletableFuture<Iterable<AutoScalingGroup>> getAutoScalingGroupsAsync(com.sap.sse.landscape.Region region) {
        final Set<AutoScalingGroup> result = new HashSet<>();
        return getAutoScalingAsyncClient(getRegion(region)).describeAutoScalingGroupsPaginator().subscribe(response->
            result.addAll(response.autoScalingGroups())).handle((v, e)->Collections.unmodifiableCollection(result));
    }
    
    @Override
    public void updateAutoScalingGroupMinSize(AwsAutoScalingGroup autoScalingGroup, int minSize) {
        getAutoScalingClient(getRegion(autoScalingGroup.getRegion())).updateAutoScalingGroup(b->b
                .autoScalingGroupName(autoScalingGroup.getAutoScalingGroup().autoScalingGroupName())
                .minSize(minSize));
    }
    
    @Override
    public CompletableFuture<Iterable<LaunchConfiguration>> getLaunchConfigurationsAsync(com.sap.sse.landscape.Region region) {
        final Set<LaunchConfiguration> result = new HashSet<>();
        return getAutoScalingAsyncClient(getRegion(region)).describeLaunchConfigurationsPaginator().subscribe(response->
            result.addAll(response.launchConfigurations())).handle((v, e)->Collections.unmodifiableCollection(result));
    }
    
    private CompletableFuture<Map<Listener, Iterable<Rule>>> getListenerToRulesMap(com.sap.sse.landscape.Region region, Iterable<ApplicationLoadBalancer<ShardingKey>> loadBalancers) {
        final ElasticLoadBalancingV2AsyncClient loadBalancingClient = getLoadBalancingAsyncClient(getRegion(region));
        final Set<CompletableFuture<Pair<Listener, Iterable<Rule>>>> mapEntryFutures = new HashSet<>();
        for (final ApplicationLoadBalancer<ShardingKey> loadBalancer : loadBalancers) {
            final CompletableFuture<Listener> listenerFuture = getHttpsListenerAsync(region, loadBalancer);
            final CompletableFuture<Pair<Listener, Iterable<Rule>>> mapEntryFuture =
                    listenerFuture.thenCompose(listener->{
                        final CompletableFuture<Pair<Listener, Iterable<Rule>>> result;
                        if (listener==null) {
                            result = new CompletableFuture<>();
                            result.complete(null);
                        } else {
                            result = loadBalancingClient.describeRules(
                                b->b.listenerArn(listener.listenerArn())).handle(
                                    (describeRulesResponse, e)->{
                                        final Pair<Listener, Iterable<Rule>> rulesResult;
                                        if (e != null) {
                                            logger.log(Level.WARNING, "Problem trying to get load balancer listener rules for "
                                                    + loadBalancer.getName() + ". Trying synchronously...", e);
                                            try {
                                                Thread.sleep(3000);
                                            } catch (InterruptedException e1) {
                                                logger.log(Level.WARNING, "Strange; sleeping was interrupted", e1);
                                            } // wait a bit; could have been a rate limit exceeding issue 
                                            rulesResult = new Pair<Listener, Iterable<Rule>>(listener,
                                                    getLoadBalancingClient(getRegion(region)).describeRules(b->b
                                                            .listenerArn(listener.listenerArn())).rules());
                                        } else {
                                            rulesResult = new Pair<Listener, Iterable<Rule>>(listener, describeRulesResponse.rules());
                                        }
                                        return rulesResult;
                                    });
                        }
                        return result;
                    });
            mapEntryFutures.add(mapEntryFuture);
        }
        return CompletableFuture.allOf(mapEntryFutures.toArray(new CompletableFuture<?>[0])).handle((v, e)->{
            final Map<Listener, Iterable<Rule>> result = new HashMap<>();
            for (final CompletableFuture<Pair<Listener, Iterable<Rule>>> mapEntryFuture : mapEntryFutures) {
                try {
                    if (mapEntryFuture.get() != null) {
                        result.put(mapEntryFuture.get().getA(), mapEntryFuture.get().getB());
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
            }
            return result;
        });
    }

    /**
     * This request will happen once per target group in a region; those may be many, and we'd like to avoid rate limit exceedings,
     * so we'll throttle our requests a bit here; usually, 100 requests per seconds and region with a "bucket refill" of 20/s applies
     * as a limit, so if we throttle down to 20/s we should be fine for most cases.
     */
    private final static Object sequencer = new Object();
    @Override
    public CompletableFuture<Iterable<TargetHealthDescription>> getTargetHealthDescriptionsAsync(com.sap.sse.landscape.Region region, TargetGroup<ShardingKey> targetGroup) {
        synchronized (sequencer) { // ensure across instances to throttle access accordingly
            try {
                Thread.sleep(1000/20);
            } catch (InterruptedException e1) {
                logger.log(Level.WARNING, "Interrupted", e1);
            }
            final CompletableFuture<DescribeTargetHealthResponse> describeTargetHealthResponse = getLoadBalancingAsyncClient(
                    getRegion(region)).describeTargetHealth(b->b.targetGroupArn(targetGroup.getTargetGroupArn()));
            return describeTargetHealthResponse.handleAsync((targetHealthResponse, e)->{
                final Iterable<TargetHealthDescription> result;
                if (e != null) {
                    logger.log(Level.WARNING, "Exception trying to obtain health status of target group "+targetGroup, e);
                    result = Collections.emptySet();
                } else {
                    result = targetHealthResponse.targetHealthDescriptions();
                }
                return result;
            });
        }
    }
    
    @Override
    public CompletableFuture<Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>>> getTargetGroupsAsync(com.sap.sse.landscape.Region region) {
        final Set<DescribeTargetGroupsResponse> responses = new HashSet<>();
        return getLoadBalancingAsyncClient(getRegion(region)).describeTargetGroupsPaginator().subscribe(response->responses.add(response)).thenCompose(someVoid->{
                // now we have all responses
            final Map<TargetGroup<ShardingKey>, CompletableFuture<Iterable<TargetHealthDescription>>> futures = new HashMap<>();
            for (final DescribeTargetGroupsResponse response : responses) {
                for (final software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup tg : response.targetGroups()) {
                    final TargetGroup<ShardingKey> targetGroup = new AwsTargetGroupImpl<ShardingKey>(this, region,
                            tg.targetGroupName(), tg.targetGroupArn(), Util.first(tg.loadBalancerArns()),
                            tg.protocol(), tg.port(), tg.healthCheckProtocol(), getHealthCheckPort(tg),
                            tg.healthCheckPath());
                    futures.put(targetGroup, getTargetHealthDescriptionsAsync(region, targetGroup));
                }
            }
            return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture<?>[0])).handle((v, e)->{
                final Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>> result = new HashMap<>();
                for (final Entry<TargetGroup<ShardingKey>, CompletableFuture<Iterable<TargetHealthDescription>>> future : futures.entrySet()) {
                    try {
                        result.put(future.getKey(), future.getValue().get());
                    } catch (InterruptedException | ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return result;
            });
        });
    }
    
    @Override
    public AwsRegion getDefaultRegion() {
        return new AwsRegion(Region.EU_WEST_2, this); // TODO actually, EU_WEST_1 (Ireland) is our default region, but as long as this is under development, EU_WEST_2 gives us an isolated test environment
    }

    @Override
    public Iterable<com.sap.sse.landscape.Region> getRegions() {
        return Util.map(Region.regions(), r->new AwsRegion(r, this));
    }
    
    @Override
    public void updateReleaseInAutoScalingGroup(com.sap.sse.landscape.Region region, AwsAutoScalingGroup autoScalingGroup, String replicaSetName, Release release) {
        logger.info("Adjusting release for auto-scaling group "+autoScalingGroup.getName()+" to "+release);
        final AutoScalingClient autoScalingClient = getAutoScalingClient(getRegion(region));
        final String releaseName = release.getName();
        final LaunchConfiguration oldLaunchConfiguration = autoScalingGroup.getLaunchConfiguration();
        final String oldUserData = new String(Base64.getDecoder().decode(oldLaunchConfiguration.userData().getBytes()));
        final String newUserData = oldUserData.replaceFirst(
                "(?m)^"+DefaultProcessConfigurationVariables.INSTALL_FROM_RELEASE.name()+"=(.*)$",
                DefaultProcessConfigurationVariables.INSTALL_FROM_RELEASE.name()+"=\""+release.getName()+"\"");
        final String newLaunchConfigurationName = getLaunchConfigurationName(replicaSetName, releaseName);
        logger.info("Creating new launch configuration "+newLaunchConfigurationName);
        autoScalingClient.createLaunchConfiguration(b->b
                .associatePublicIpAddress(oldLaunchConfiguration.associatePublicIpAddress())
                .blockDeviceMappings(oldLaunchConfiguration.blockDeviceMappings())
                .classicLinkVPCId(oldLaunchConfiguration.classicLinkVPCId())
                .classicLinkVPCSecurityGroups(oldLaunchConfiguration.classicLinkVPCSecurityGroups())
                .ebsOptimized(oldLaunchConfiguration.ebsOptimized())
                .iamInstanceProfile(oldLaunchConfiguration.iamInstanceProfile())
                .imageId(oldLaunchConfiguration.imageId())
                .instanceMonitoring(oldLaunchConfiguration.instanceMonitoring())
                .instanceType(oldLaunchConfiguration.instanceType())
                .keyName(oldLaunchConfiguration.keyName())
                .launchConfigurationName(newLaunchConfigurationName)
                .placementTenancy(oldLaunchConfiguration.placementTenancy())
                .securityGroups(oldLaunchConfiguration.securityGroups())
                .spotPrice(oldLaunchConfiguration.spotPrice())
                .userData(Base64.getEncoder().encodeToString(newUserData.getBytes())));
        logger.info("Telling auto-scaling group "+autoScalingGroup.getName()+" to use new launch configuration "+newLaunchConfigurationName);
        autoScalingClient.updateAutoScalingGroup(b->b
                .autoScalingGroupName(autoScalingGroup.getAutoScalingGroup().autoScalingGroupName())
                .launchConfigurationName(newLaunchConfigurationName));
        logger.info("Removing old launch configuration "+oldLaunchConfiguration.launchConfigurationName());
        autoScalingClient.deleteLaunchConfiguration(b->b.launchConfigurationName(oldLaunchConfiguration.launchConfigurationName()));
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    void createLaunchConfigurationAndAutoScalingGroup(
            com.sap.sse.landscape.Region region, String replicaSetName, Optional<Tags> tags,
            TargetGroup<ShardingKey> publicTargetGroup, String keyName, InstanceType instanceType,
            String imageId, AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> replicaConfiguration,
            int minReplicas, int maxReplicas, int maxRequestsPerTarget) {
        logger.info("Creating launch configuration for replica set "+replicaSetName);
        final AutoScalingClient autoScalingClient = getAutoScalingClient(getRegion(region));
        final String releaseName = replicaConfiguration.getRelease().map(r->r.getName()).orElse("UnknownRelease");
        final String launchConfigurationName = getLaunchConfigurationName(replicaSetName, releaseName);
        final String autoScalingGroupName = getAutoScalingGroupName(replicaSetName);
        final Iterable<AwsAvailabilityZone> availabilityZones = getAvailabilityZones(region);
        final int instanceWarmupTimeInSeconds = (int) Duration.ONE_MINUTE.times(3).asSeconds();
        autoScalingClient.createLaunchConfiguration(b->b
                .launchConfigurationName(launchConfigurationName)
                .keyName(keyName)
                .imageId(imageId)
                .instanceMonitoring(i->i.enabled(true))
                .securityGroups(getDefaultSecurityGroupForApplicationHosts(region).getId())
                .userData(Base64.getEncoder().encodeToString(replicaConfiguration.getAsEnvironmentVariableAssignments().getBytes()))
                .instanceType(instanceType.toString()));
        logger.info("Creating auto-scaling group for replica set "+replicaSetName);
        autoScalingClient.createAutoScalingGroup(b->{
            b
                .minSize(minReplicas)
                .maxSize(maxReplicas)
                .healthCheckGracePeriod(instanceWarmupTimeInSeconds)
                .autoScalingGroupName(autoScalingGroupName)
                .availabilityZones(Util.toArray(Util.map(availabilityZones, az->az.getName()), new String[3]))
                .targetGroupARNs(publicTargetGroup.getTargetGroupArn())
                .launchConfigurationName(launchConfigurationName);
            tags.ifPresent(t->{
                final List<software.amazon.awssdk.services.autoscaling.model.Tag> awsTags = new ArrayList<>();
                for (final Entry<String, String> tag : t) {
                    awsTags.add(software.amazon.awssdk.services.autoscaling.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build());
                }
                b.tags(awsTags);
            });
        });
        autoScalingClient.putScalingPolicy(b->b
                .autoScalingGroupName(autoScalingGroupName)
                .estimatedInstanceWarmup(instanceWarmupTimeInSeconds)
                .policyType("TargetTrackingScaling")
                .policyName("KeepRequestsPerTargetAt"+maxRequestsPerTarget)
                .targetTrackingConfiguration(t->t
                        .predefinedMetricSpecification(p->p
                                .resourceLabel("app/"+publicTargetGroup.getLoadBalancer().getName()+"/"+publicTargetGroup.getLoadBalancer().getId()+
                                        "/targetgroup/"+publicTargetGroup.getName()+"/"+publicTargetGroup.getId())
                                .predefinedMetricType(MetricType.ALB_REQUEST_COUNT_PER_TARGET))
                        .targetValue((double) maxRequestsPerTarget)));
    }

    private String getLaunchConfigurationName(String replicaSetName, final String releaseName) {
        return replicaSetName + "-" + releaseName;
    }

    private String getAutoScalingGroupName(String replicaSetName) {
        return replicaSetName+AUTO_SCALING_GROUP_NAME_SUFFIX;
    }

    @Override
    public Snapshot getSnapshot(AwsRegion region, String snapshotId) {
        final DescribeSnapshotsResponse describeSnapshotResponse = getEc2Client(getRegion(region)).describeSnapshots(b->b.filters(Filter.builder().name("snapshot-id").values(snapshotId).build()));
        return describeSnapshotResponse.hasSnapshots() ? describeSnapshotResponse.snapshots().iterator().next() : null;
    }

    @Override
    public DNSCache getNewDNSCache() {
        return new DNSCache(getRoute53AsyncClient());
    }

    @Override
    public CompletableFuture<Void> removeAutoScalingGroupAndLaunchConfiguration(AwsAutoScalingGroup autoScalingGroup) {
        final String launchConfigurationName = autoScalingGroup.getAutoScalingGroup().launchConfigurationName();
        final AutoScalingAsyncClient autoScalingAsyncClient = getAutoScalingAsyncClient(getRegion(autoScalingGroup.getRegion()));
        logger.info("Removing auto-scaling group "+autoScalingGroup.getAutoScalingGroup().autoScalingGroupName());
        return autoScalingAsyncClient.deleteAutoScalingGroup(b->b.forceDelete(true).autoScalingGroupName(autoScalingGroup.getAutoScalingGroup().autoScalingGroupName()))
            .thenAccept(response->{
                logger.info("Removing launch configuration "+launchConfigurationName);
                autoScalingAsyncClient.deleteLaunchConfiguration(b->b.launchConfigurationName(launchConfigurationName));
            });
    }
}
