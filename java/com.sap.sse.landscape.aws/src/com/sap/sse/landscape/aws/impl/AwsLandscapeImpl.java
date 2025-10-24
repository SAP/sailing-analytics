package com.sap.sse.landscape.aws.impl;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
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
import java.util.stream.Collectors;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.NamedWithIDImpl;
import com.sap.sse.landscape.DefaultProcessConfigurationVariables;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.MachineImage;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.SecurityGroup;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
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
import com.sap.sse.landscape.aws.LandscapeConstants;
import com.sap.sse.landscape.aws.ReverseProxyCluster;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.TargetGroup;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.aws.orchestration.ShardProcedure;
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
import software.amazon.awssdk.services.autoscaling.model.DeleteAutoScalingGroupResponse;
import software.amazon.awssdk.services.autoscaling.model.EnableMetricsCollectionRequest;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.autoscaling.model.MetricType;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairResponse;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionRequest;
import software.amazon.awssdk.services.ec2.model.CreateLaunchTemplateVersionResponse;
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
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;
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
import software.amazon.awssdk.services.elasticloadbalancingv2.model.IpAddressType;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerNotFoundException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerState;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyRuleResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyTargetGroupAttributesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RedirectActionStatusCodeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RulePriorityPair;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SetRulePrioritiesRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.SubnetMapping;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TagDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupAttribute;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroupNotFoundException;
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
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListResourceRecordSetsResponse;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecord;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;
import software.amazon.awssdk.services.route53.model.TestDnsAnswerResponse;
import software.amazon.awssdk.services.route53.paginators.ListResourceRecordSetsIterable;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.wafv2.Wafv2Client;
import software.amazon.awssdk.services.wafv2.model.ListWebAcLsResponse;
import software.amazon.awssdk.services.wafv2.model.Scope;

public class AwsLandscapeImpl<ShardingKey> implements AwsLandscape<ShardingKey> {
    private static final String SSL_SECURITY_POLICY = "ELBSecurityPolicy-FS-1-2-Res-2019-08";
    private static final String AUTO_SCALING_GROUP_NAME_SUFFIX = "-auto-replicas";
    private static final String DEFAULT_TARGET_GROUP_PREFIX = "D";
    private static final Logger logger = Logger.getLogger(AwsLandscapeImpl.class.getName());
    public static final long DEFAULT_DNS_TTL_SECONDS = 60l;
    private static final String DEFAULT_CERTIFICATE_DOMAIN = "*.sapsailing.com";
    private static final String DEFAULT_NON_DNS_MAPPED_ALB_NAME = "DefDyn";
    private static final String SAILING_APP_SECURITY_GROUP_NAME = "Sailing Analytics App";
    private final String accessKeyId;
    private final String secretAccessKey;
    private final Optional<String> sessionToken;
    private final AwsRegion globalRegion;
    private final AwsLandscapeState landscapeState;
    private final String pathPrefixForShardingKey;
    
    public AwsLandscapeImpl(AwsLandscapeState awsLandscapeState, String pathPrefixForShardingKey) {
        this(awsLandscapeState,
             System.getProperty(ACCESS_KEY_ID_SYSTEM_PROPERTY_NAME),
             System.getProperty(SECRET_ACCESS_KEY_SYSTEM_PROPERTY_NAME), pathPrefixForShardingKey);
    }
    
    public AwsLandscapeImpl(AwsLandscapeState awsLandscapeState, String accessKeyId, String secretAccessKey, String pathPrefixForShardingKey) {
        this(awsLandscapeState, accessKeyId, secretAccessKey, /* no session token */ null, pathPrefixForShardingKey);
    }
    
    public AwsLandscapeImpl(AwsLandscapeState awsLandscapeState, String accessKeyId, String secretAccessKey, String sessionToken, String pathPrefixForShardingKey) {
        this(accessKeyId, secretAccessKey, sessionToken,
                // by using MongoDBService.INSTANCE the default test configuration will be used if nothing else is configured
                PersistenceFactory.INSTANCE.getDomainObjectFactory(MongoDBService.INSTANCE), PersistenceFactory.INSTANCE.getMongoObjectFactory(MongoDBService.INSTANCE), awsLandscapeState, pathPrefixForShardingKey);
    }
    
    public AwsLandscapeImpl(String accessKeyId, String secretAccessKey,
            String sessionToken, DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory, AwsLandscapeState landscapeState, String pathPrefixForShardingKey) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = Optional.ofNullable(sessionToken);
        this.globalRegion = new AwsRegion(Region.AWS_GLOBAL, this);
        this.landscapeState = landscapeState;
        this.pathPrefixForShardingKey = pathPrefixForShardingKey;
    }
    
    /**
     * @param unencryptedKeyPair
     *            will not work for keys of type ED25519 because for those types the {@code getPrivateKey} method of
     *            {@link KeyPair} is not implemented and throws an {@link UnsupportedOperationException} instead.
     */
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
    
    private Ec2AsyncClient getEc2AsyncClient(Region region) {
        return getClient(Ec2AsyncClient.builder(), region);
    }
    
    private AcmAsyncClient getAcmAsyncClient(Region region) {
        return getClient(AcmAsyncClient.builder(), region);
    }
    
    private ElasticLoadBalancingV2Client getLoadBalancingClient(Region region) {
        return getClient(ElasticLoadBalancingV2Client.builder(), region);
    }
    
    private Wafv2Client getWafClient(Region region) {
        return getClient(Wafv2Client.builder(), region);
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
    public ApplicationLoadBalancer<ShardingKey> createLoadBalancer(String name, com.sap.sse.landscape.Region region,
            SecurityGroup securityGroupForVpc) throws InterruptedException, ExecutionException {
        Region awsRegion = getRegion(region);
        final ElasticLoadBalancingV2Client client = getLoadBalancingClient(awsRegion);
        final Iterable<AwsAvailabilityZone> availabilityZones = getAvailabilityZones(region);
        final SubnetMapping[] subnetMappings = Util.toArray(Util.map(getSubnetsForAvailabilityZones(awsRegion, availabilityZones, securityGroupForVpc),
                subnet->SubnetMapping.builder().subnetId(subnet.subnetId()).build()), new SubnetMapping[0]);
        final CreateLoadBalancerResponse response = client
                .createLoadBalancer(CreateLoadBalancerRequest.builder()
                        .name(name)
                        .ipAddressType(IpAddressType.DUALSTACK) // IPv4 and IPv6
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
        getWafACLsByTagAndAssociateWithALB(LandscapeConstants.WEB_ACL_PURPOSE_TAG, LandscapeConstants.WEB_ACL_GEOBLOCKING_PURPOSE, result.getArn(), awsRegion);
        return result;
    }
    
    private void getWafACLsByTagAndAssociateWithALB(String tagKey, String tagValue, String albArn, Region region) {
        logger.info("Trying to find WAF ACLs with tag "+tagKey+"="+tagValue+" to associate with ALB "+albArn+" in region "+region.id());
        final Wafv2Client wafClient = getWafClient(region);
        // Step 1: list all REGIONAL Web ACLs
        final ListWebAcLsResponse listResp = wafClient.listWebACLs(b->b.scope(Scope.REGIONAL));
        listResp.webACLs().stream().filter(aclSummary ->
            // Step 2: filter the ACLs down to those with the right tag
            wafClient.listTagsForResource(b->b.resourceARN(aclSummary.arn())).tagInfoForResource().tagList().stream()
                    .anyMatch(tag -> tag.key().equals(tagKey) && tag.value().equals(tagValue)))
        .forEach(aclSummary -> {
            // Step 3: associate the ALB with those Web ACLs
            logger.info("Associating WAF ACL "+aclSummary.arn()+" with ALB "+albArn);
            wafClient.associateWebACL(b->b
                        .webACLArn(aclSummary.arn())
                        .resourceArn(albArn));
        });
    }
    
    private Subnet getSubnetForAvailabilityZoneInSameVpcAsSecurityGroup(AwsAvailabilityZone az, SecurityGroup securityGroup, Region region) {
        final Ec2Client ec2Client = getEc2Client(region);
        final String vpcId = ec2Client.describeSecurityGroups(b->b.groupIds(securityGroup.getId())).securityGroups().iterator().next().vpcId();
        return ec2Client
                .describeSubnets(b -> b.filters(Filter.builder().name("vpc-id").values(vpcId).build(),
                        Filter.builder().name("availability-zone-id").values(az.getId()).build()))
                .subnets().stream().filter(subnet -> !subnet.tags().stream().map(tag -> tag.key())
                        .collect(Collectors.toList()).contains(LandscapeConstants.NO_INSTANCE_DEPLOYMENT))
                .iterator().next();
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
        final ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> reverseProxy = getReverseProxyCluster(alb.getRegion());
        final HashMap<String, String> tagKeyandValue = new HashMap<>();
        tagKeyandValue.put(LandscapeConstants.ALL_REVERSE_PROXIES, "");
        final TargetGroup<ShardingKey> defaultTargetGroup = createTargetGroup(alb.getRegion(), DEFAULT_TARGET_GROUP_PREFIX + alb.getName() + "-" + ProtocolEnum.HTTP.name(),
                httpPort, reverseProxy.getHealthCheckPath(), /* healthCheckPort */ httpPort, alb.getArn(), alb.getVpcId(), tagKeyandValue);
        setTargetGroupHealthCheckPath(defaultTargetGroup, reverseProxy.getTargetGroupHealthCheckPath(defaultTargetGroup.getTargetGroupArn()));
        defaultTargetGroup.addTargets(reverseProxy.getHosts());
        final String defaultCertificateArn = defaultCertificateArnFuture.get();
        return getLoadBalancingClient(getRegion(alb.getRegion()))
                        .createListener(l -> {
                            l.loadBalancerArn(alb.getArn()).protocol(ProtocolEnum.HTTPS)
                                        .port(httpsPort)
                                        .sslPolicy(SSL_SECURITY_POLICY)
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
                tg->createTargetGroup(this, region, tg.targetGroupName(), tg.targetGroupArn(), loadBalancerArn,
                        tg.protocol(), tg.port(), tg.healthCheckProtocol(), getHealthCheckPort(tg), tg.healthCheckPath()));
    }
    
    private TargetGroup<ShardingKey> createTargetGroup(AwsLandscape<ShardingKey> landscape,
            com.sap.sse.landscape.Region region, String targetGroupName, String targetGroupArn, String loadBalancerArn,
            ProtocolEnum protocol, Integer port, ProtocolEnum healthCheckProtocol, Integer healthCheckPort,
            String healthCheckPath) {
        return new AwsTargetGroupImpl<ShardingKey>(this, region, targetGroupName, targetGroupArn, loadBalancerArn,
                protocol, port, healthCheckProtocol, healthCheckPort, healthCheckPath);
    }
    
    @Override
    public Iterable<TargetGroup<ShardingKey>> getTargetGroups(com.sap.sse.landscape.Region region) {
        return Util.map(getLoadBalancingClient(getRegion(region)).describeTargetGroupsPaginator().targetGroups(),
                tg -> createTargetGroup(this, region, tg.targetGroupName(), tg.targetGroupArn(),
                        tg.loadBalancerArns().isEmpty() ? null : tg.loadBalancerArns().get(0), tg.protocol(), tg.port(),
                        tg.healthCheckProtocol(), getHealthCheckPort(tg), tg.healthCheckPath()));
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
    public Iterable<Rule> modifyRuleConditions(com.sap.sse.landscape.Region region, Rule rule) {
        ModifyRuleResponse res = getLoadBalancingClient(getRegion(region))
                .modifyRule(t -> t.conditions(rule.conditions()).ruleArn(rule.ruleArn()).build());
        return res.rules();
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
        logger.info("Removing load balancer rules "+Util.joinStrings(", ", Arrays.asList(rulesToDelete))+" in region "+region);
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
    public void updateLoadBalancerListenerRulePriorities(com.sap.sse.landscape.Region region, Iterable<RulePriorityPair> newRulePriorities) {
        getLoadBalancingClient(getRegion(region)).setRulePriorities(SetRulePrioritiesRequest.builder().rulePriorities(Util.asList(newRulePriorities)).build());
    }
    
    @Override
    public void deleteLoadBalancerListener(com.sap.sse.landscape.Region region, Listener listener) {
        getLoadBalancingClient(getRegion(region)).deleteListener(b->b.listenerArn(listener.listenerArn()));
    }

    /**
     * Grabs all subnets that are default subnet for any of the availability zones specified
     * <p>
     * 
     * @param securityGroupForVpc
     *            if provided, the security group's VPC association will be used to constrain the subnets for the AZs to
     *            that VPC; if {@code null}, the default subnet for each respective AZ is used
     */
    private Iterable<Subnet> getSubnetsForAvailabilityZones(Region region, Iterable<AwsAvailabilityZone> azs, SecurityGroup securityGroupForVpc) {
        final Ec2Client ec2Client = getEc2Client(region);
        final Optional<String> securityGroupVpcId = Optional.ofNullable(securityGroupForVpc).map(
                sg->ec2Client.describeSecurityGroups(b->b.groupIds(sg.getId())).securityGroups().get(0).vpcId()); // Maps the security group to its vpc id.
        return Util.filter(ec2Client.describeSubnets().subnets(),
                subnet -> securityGroupVpcId.map(vpcId -> vpcId.equals(subnet.vpcId())).orElse(subnet.defaultForAz())
                        && Util.contains(Util.map(azs, az -> az.getId()), subnet.availabilityZoneId())
                        && subnet.tags().stream().map(tag -> tag.key())
                                .filter(key -> key.equals(LandscapeConstants.NO_INSTANCE_DEPLOYMENT)).count() == 0);
        // Checks whether the subnet vpcId matches the vpcId of the security group. In the cases they are not equal, or the security group vpc is not found, instead it checks that the subnet is the default for the AZ.
        // AND, it checks whether the subnet is in any of the AZs passed in the parameter.
        // AND it checks that the subnet is actually usable for deployment (eg. subnets may be used exclusively for lambda NAT gateways).

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
    public <HostT extends AwsInstance<ShardingKey>> HostT getHostByPrivateIpAddress(com.sap.sse.landscape.Region region, String privateIpAddress, HostSupplier<ShardingKey, HostT> hostSupplier) {
        return getHost(region, getInstanceByPrivateIpAddress(region, privateIpAddress), hostSupplier);
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
                                        .resourceRecordSet(ResourceRecordSet.builder().name(hostname.toLowerCase()).type(type).ttl(DEFAULT_DNS_TTL_SECONDS)
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
                        // TODO using the DEFAULT_DNS_TTL_MILLIS is a bit unclean here; if the record has been modified manually or the default has changed, removal will fail
                        .resourceRecordSet(ResourceRecordSet.builder().name(hostname).type(type).ttl(DEFAULT_DNS_TTL_SECONDS)
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
                .describeImages(DescribeImagesRequest.builder().filters(
                        Filter.builder().name("tag:"+tagName).values(tagValue).build(),
                        Filter.builder().name("state").values("available").build()).build()); // only use available images, see bug 5679
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
     */
    @Override
    public AwsCredentials getCredentials() {
        return sessionToken.map(nonEmptySessionToken->(AwsCredentials) AwsSessionCredentials.create(accessKeyId, secretAccessKey, nonEmptySessionToken))
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
            if (e.getMessage().contains("The keypair ") && e.getMessage().contains("already exists")) {
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
    public <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> launchHosts(HostSupplier<ShardingKey, HostT> hostSupplier,
            int numberOfHostsToLaunch, MachineImage fromImage,
            InstanceType instanceType, AwsAvailabilityZone az, String keyName, Iterable<SecurityGroup> securityGroups, Optional<Tags> tags, String... userData) {
        if (!fromImage.getRegion().equals(az.getRegion())) {
            throw new IllegalArgumentException("Trying to launch an instance in region "+az.getRegion()+
                    " with image "+fromImage+" that lives in region "+fromImage.getRegion()+" which is different."+
                    " Consider copying the image to that region.");
        }
        final Ec2Client ec2Client = getEc2Client(getRegion(az.getRegion()));
        final Builder runInstancesRequestBuilder = RunInstancesRequest.builder()
            .additionalInfo("Test " + getClass().getName())
            .imageId(fromImage.getId().toString())
            .minCount(numberOfHostsToLaunch)
            .maxCount(numberOfHostsToLaunch)
            .instanceType(instanceType).keyName(keyName)
            .subnetId(getSubnetForAvailabilityZoneInSameVpcAsSecurityGroup(az, securityGroups.iterator().next(), getRegion(az.getRegion())).subnetId())
            .placement(Placement.builder().availabilityZone(az.getName()).build())
            .securityGroupIds(Util.mapToArrayList(securityGroups, SecurityGroup::getId));
        if (userData != null) {
            runInstancesRequestBuilder.userData(Base64.getEncoder().encodeToString(String.join("\n", userData).getBytes()));
        }
        tags.ifPresent(theTags->{
            final Collection<Tag> awsTags = getAwsTags(theTags);
            runInstancesRequestBuilder.tagSpecifications(TagSpecification.builder().resourceType(ResourceType.INSTANCE).tags(awsTags).build());
        });
        final RunInstancesRequest launchRequest = runInstancesRequestBuilder.build();
        logger.info("Launching instance(s): "+launchRequest);
        final RunInstancesResponse response = ec2Client.runInstances(launchRequest);
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
        return getAvailabilityZones(awsRegion, /* VPC ID filter */ Optional.empty());
    }

    @Override
    public Iterable<AwsAvailabilityZone> getAvailabilityZones(com.sap.sse.landscape.Region awsRegion,
            Optional<String> vpcId) {
        final Ec2Client ec2Client = getEc2Client(getRegion(awsRegion));
        // filter for the VPC ID if present; otherwise list all subnets in the region
        return ec2Client
                .describeSubnets(b -> vpcId
                        .ifPresent(theVpcId -> b.filters(Filter.builder().name("vpc-id").values(theVpcId).build())))
                .subnets().stream().map(subnet -> getAvailabilityZoneByName(awsRegion, subnet.availabilityZone()))
                .distinct().collect(Collectors.toList());
    }
    
    @Override
    public TargetGroup<ShardingKey> getTargetGroup(com.sap.sse.landscape.Region region, String targetGroupName, String loadBalancerArn) {
        final ElasticLoadBalancingV2Client loadBalancingClient = getLoadBalancingClient(getRegion(region));
        final DescribeTargetGroupsResponse targetGroupResponse = loadBalancingClient.describeTargetGroups(b->b.names(targetGroupName));
        final software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup targetGroup = targetGroupResponse.targetGroups().iterator().next();
        return targetGroupResponse.hasTargetGroups()
                ? createTargetGroup(this, region, targetGroupName, targetGroup.targetGroupArn(),
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
            String healthCheckPath, int healthCheckPort, String loadBalancerArn, String vpcId) {
        return createTargetGroup(region, targetGroupName, port, healthCheckPath, healthCheckPort, loadBalancerArn,
                vpcId, Collections.emptyMap());
    }
    
    /**
     * Overloaded method to allow tag-keys and values to be passed.
     */
    private TargetGroup<ShardingKey> createTargetGroup(com.sap.sse.landscape.Region region, String targetGroupName, int port,
            String healthCheckPath, int healthCheckPort, String loadBalancerArn, String vpcId, Map<String,String> tagKeyAndValues) {
        software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag tags[] = tagKeyAndValues.entrySet().stream()
                .map(entry -> software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag.builder()
                        .key(entry.getKey()).value(entry.getValue()).build())
                .toArray(x -> new software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag[x]);
        final ElasticLoadBalancingV2Client loadBalancingClient = getLoadBalancingClient(getRegion(region));
        software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupRequest.Builder targetGroupRequestBuilder = CreateTargetGroupRequest
                .builder().name(targetGroupName).healthyThresholdCount(2).unhealthyThresholdCount(2)
                .healthCheckTimeoutSeconds(4).healthCheckEnabled(true).healthCheckIntervalSeconds(10)
                .healthCheckPath(healthCheckPath).healthCheckPort("" + healthCheckPort)
                .healthCheckProtocol(guessProtocolFromPort(healthCheckPort)).port(port)
                .vpcId(vpcId == null ? getVpcId(region) : vpcId).protocol(guessProtocolFromPort(port))
                .targetType(TargetTypeEnum.INSTANCE);
        if (tags.length > 0) {
            targetGroupRequestBuilder.tags(tags);
        }
        final software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup targetGroup = loadBalancingClient
                .createTargetGroup(targetGroupRequestBuilder.build()).targetGroups().iterator().next();
        final String targetGroupArn = targetGroup.targetGroupArn();
        int numberOfRetries = 3;
        final Duration TIME_BETWEEN_RETRIES = Duration.ONE_SECOND;
        boolean success = false;
        while (!success && numberOfRetries-- > 0) { 
            try {
                loadBalancingClient.modifyTargetGroupAttributes(ModifyTargetGroupAttributesRequest.builder()
                        .targetGroupArn(targetGroupArn)
                        .attributes(TargetGroupAttribute.builder().key("stickiness.enabled").value("true").build(),
                                    TargetGroupAttribute.builder().key("load_balancing.algorithm.type").value("least_outstanding_requests")
                                    .build()).build());
                success = true;
            } catch (TargetGroupNotFoundException e) {
                // See also https://github.com/hashicorp/terraform-provider-aws/issues/16860; try again
                logger.log(Level.WARNING, "Couldn't find target group with ARN "+targetGroupArn+" that was just created successfully." +
                        (numberOfRetries > 0 ? " Trying again..." : ""), e);
                if (numberOfRetries > 0) {
                    try {
                        Thread.sleep(TIME_BETWEEN_RETRIES.asMillis());
                    } catch (InterruptedException e1) {
                        logger.warning("Sleep got interrupted. Well, then we'll retry a bit sooner...");
                    }
                }
            }
        }
        return createTargetGroup(this, region, targetGroupName, targetGroupArn, loadBalancerArn,
                targetGroup.protocol(), port, targetGroup.healthCheckProtocol(), healthCheckPort, healthCheckPath);
    }

    private ProtocolEnum guessProtocolFromPort(int healthCheckPort) {
        return healthCheckPort == 443 ? ProtocolEnum.HTTPS : ProtocolEnum.HTTP;
    }
    
    /**
     * Finds the ID of the "default" VPC in the {@code region} specified. If there is no such default VPC in that {@code region},
     * an {@link IllegalStateException} will be thrown.
     */
    private String getVpcId(com.sap.sse.landscape.Region region) {
        Vpc vpc = getEc2Client(getRegion(region)).describeVpcs().vpcs().stream().filter(myVpc->myVpc.isDefault()).findAny().
                orElseThrow(()->new IllegalStateException("No default VPC found in region "+region));
        return vpc.vpcId();
    }

    @Override
    public software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroup(com.sap.sse.landscape.Region region, String targetGroupName) {
        return Util.first(getLoadBalancingClient(getRegion(region)).describeTargetGroups(DescribeTargetGroupsRequest.builder()
                        .names(targetGroupName).build()).targetGroups());
    }
    
    @Override
    public software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroupByArn(com.sap.sse.landscape.Region region, String targetGroupArn) {
        return Util.first(getLoadBalancingClient(getRegion(region)).describeTargetGroups(DescribeTargetGroupsRequest.builder()
                        .targetGroupArns(targetGroupArn).build()).targetGroups());
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
                .describeTargetHealth(
                        DescribeTargetHealthRequest.builder().targetGroupArn(targetGroup.getTargetGroupArn()).build())
                .targetHealthDescriptions().forEach(targetHealthDescription -> {
                    if (targetHealthDescription.target().id().matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
                        AwsInstance<ShardingKey> awsInstance = getHostByPrivateIpAddress(targetGroup.getRegion(), targetHealthDescription.target().id().trim(),
                                AwsInstanceImpl::new);
                        result.put(awsInstance, targetHealthDescription.targetHealth());
                    } else {
                        result.put(getHost(targetGroup.getRegion(), targetHealthDescription.target().id(),
                                AwsInstanceImpl::new), targetHealthDescription.targetHealth());
                    }
                });
        return result;
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> getReverseProxyCluster(com.sap.sse.landscape.Region region) {
        ApacheReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> reverseProxyCluster = new ApacheReverseProxyCluster<>(this);
        for (final AwsInstance<ShardingKey> reverseProxyHost : getRunningHostsWithTag(region, LandscapeConstants.REVERSE_PROXY_TAG_NAME, AwsInstanceImpl::new)) {
            reverseProxyCluster.addHost(reverseProxyHost);
        }
        return reverseProxyCluster;
    }
    
    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> getCentralReverseProxy(com.sap.sse.landscape.Region region) {
        ApacheReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> reverseProxyCluster = new ApacheReverseProxyCluster<>(this);
        for (final AwsInstance<ShardingKey> reverseProxyHost : getRunningHostsWithTag(region, LandscapeConstants.CENTRAL_REVERSE_PROXY_TAG_NAME, AwsInstanceImpl::new)) {
            reverseProxyCluster.addHost(reverseProxyHost);
        }
        Iterator<AwsInstance<ShardingKey>>  iterator = reverseProxyCluster.getHosts().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            if (iterator.hasNext()) {
                throw new IllegalStateException("There should only be one central reverse proxy");
            }
        }
        return reverseProxyCluster;
    }

    @Override
    public SecurityGroup getSecurityGroup(String securityGroupId, com.sap.sse.landscape.Region region) {
        final software.amazon.awssdk.services.ec2.model.SecurityGroup securityGroup =
                getEc2Client(getRegion(region)).describeSecurityGroups(sg->sg.groupIds(securityGroupId)).securityGroups().iterator().next();
        return new SecurityGroup() {
            @Override
            public String getId() {
                return securityGroup.groupId();
            }

            @Override
            public String getVpcId() {
                return securityGroup.vpcId();
            }
        };
    }

    @Override
    public Optional<SecurityGroup> getSecurityGroupByName(String securityGroupName, com.sap.sse.landscape.Region region) {
        final List<software.amazon.awssdk.services.ec2.model.SecurityGroup> securityGroups = getEc2Client(getRegion(region)).describeSecurityGroups(
                sg->sg.filters(Filter.builder().name("tag:Name").values(securityGroupName).build())).securityGroups();
        return securityGroups.stream().findFirst().map(sg->new SecurityGroup() {
            @Override
            public String getId() {
                return sg.groupId();
            }

            @Override
            public String getVpcId() {
                return sg.vpcId();
            }
        });
    }
    
    /**
     * Adjusts the health check path of an existing target group. Note that this will make the {@link TargetGroup} object passed as
     * the {@code targetGroup} parameter inconsistent in its {@link TargetGroup#getHealthCheckPath()} field which does not reflect the
     * new value set here.
     */
    private void setTargetGroupHealthCheckPath(TargetGroup<ShardingKey> targetGroup, String path) {
        getLoadBalancingClient(getRegion(targetGroup.getRegion())).modifyTargetGroup(ModifyTargetGroupRequest.builder()
                .targetGroupArn(targetGroup.getTargetGroupArn()).healthCheckPath(path).build());
    }
    
    @Override
    public void addTargetsToTargetGroup(
            TargetGroup<ShardingKey> targetGroup,
            Iterable<AwsInstance<ShardingKey>> targets) {
        getLoadBalancingClient(getRegion(targetGroup.getRegion())).registerTargets(getRegisterTargetsRequestBuilderConsumer(targetGroup, targets));
    }
    
    @Override
    public void addIpTargetToTargetGroup(TargetGroup<ShardingKey> targetGroup, Iterable<AwsInstance<ShardingKey>> hosts) {
        final TargetDescription[] descriptions = Util.toArray(Util.map(hosts, t->TargetDescription.builder().id(t.getPrivateAddress().getHostAddress()).port(80).build()), new TargetDescription[0]);
        getLoadBalancingClient(getRegion(targetGroup.getRegion())).registerTargets(t->t.targetGroupArn(targetGroup.getTargetGroupArn()).targets(descriptions));
    }
    
    @Override
    public void removeIpTargetFromTargetGroup(TargetGroup<ShardingKey> targetGroup, Iterable<AwsInstance<ShardingKey>> hosts) {
        final TargetDescription[] descriptions = Util.toArray(Util.map(hosts, t->TargetDescription.builder().id(t.getPrivateAddress().getHostAddress()).port(80).build()), new TargetDescription[0]);
        getLoadBalancingClient(getRegion(targetGroup.getRegion())).deregisterTargets(builder -> builder.targetGroupArn(targetGroup.getTargetGroupArn()).targets(descriptions));
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
        return getSecurityGroupByName(SAILING_APP_SECURITY_GROUP_NAME, region).orElseGet(()->{
            final List<SecurityGroup> securityGroups = new ArrayList<>();
            securityGroups.addAll(getSecurityGroupByTag(LandscapeConstants.SAILING_APPLICATION_SG_TAG, region));
            return securityGroups.isEmpty() ? null : securityGroups.get(0);
        });
    }

    @Override
    public Iterable<SecurityGroup> getDefaultSecurityGroupsForReverseProxy(com.sap.sse.landscape.Region region) {
        return getSecurityGroupByTag(LandscapeConstants.REVERSE_PROXY_SG_TAG, region);
    }

    public List<SecurityGroup> getSecurityGroupByTag(String tag, com.sap.sse.landscape.Region region) {
        final List<software.amazon.awssdk.services.ec2.model.SecurityGroup> securityGroups = getEc2Client(
                getRegion(region)).describeSecurityGroups(sg -> sg.filters(Filter.builder().name("tag-key").values(tag).build()))
                        .securityGroups();
        return securityGroups.stream().map(sg -> new SecurityGroup() {
            @Override
            public String getVpcId() {
                return sg.vpcId();
            }

            @Override
            public String getId() {
                return sg.groupId();
            }
        }).collect(Collectors.toList());
    }
    
    @Override
    public SecurityGroup getDefaultSecurityGroupForApplicationLoadBalancer(com.sap.sse.landscape.Region region) {
        return getDefaultSecurityGroupForApplicationHosts(region);
    }

    @Override
    public Iterable<SecurityGroup> getDefaultSecurityGroupsForMongoDBHosts(com.sap.sse.landscape.Region region) {
        return getSecurityGroupByTag(LandscapeConstants.MONGO_SG_TAG, region);
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> getNonDNSMappedLoadBalancer(
            com.sap.sse.landscape.Region region, String wildcardDomain) {
        return getLoadBalancerByName(getNonDNSMappedLoadBalancerName(wildcardDomain), region);
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> createNonDNSMappedLoadBalancer(
            com.sap.sse.landscape.Region region, String wildcardDomain, SecurityGroup securityGroupForVpc) throws InterruptedException, ExecutionException {
        return createLoadBalancer(getNonDNSMappedLoadBalancerName(wildcardDomain), region, securityGroupForVpc);
    }

    private String getNonDNSMappedLoadBalancerName(String wildcardDomain) {
        return DEFAULT_NON_DNS_MAPPED_ALB_NAME + wildcardDomain.replaceAll("\\.", "-");
    }
    
    /**
     * @param hostname example: {@code NLB-sapsailing-dot-com-f937a5b33246d221.elb.eu-west-1.amazonaws.com} or
     * {@code DNSMapped-0-1286577811.eu-west-1.elb.amazonaws.com}. The ".elb." part may occur before or after
     * the region identifier.
     */
    private boolean isLoadBalancerDNSName(String hostname) {
        return getLoadBalancerDescriptorForDNSName(hostname) != null;
    }

    private static class LoadBalancerDescriptor extends NamedWithIDImpl {
        private static final long serialVersionUID = 5813730385448660098L;
        private final Region region;

        public LoadBalancerDescriptor(String name, String id, Region region) {
            super(name, id);
            this.region = region;
        }

        public Region getRegion() {
            return region;
        }
    }
    
    private LoadBalancerDescriptor getLoadBalancerDescriptorForDNSName(String hostname) {
        final String loadBalancerNameAndIdPattern = "([^.]*)-([^.-]*)";
        final String amazonAwsComSuffixPattern = "amazonaws\\.com";
        final String elbInfixPattern = "\\.elb\\.";
        final String regionIdPattern = "([^.]*)";
        final Pattern p1 = Pattern.compile("^"+loadBalancerNameAndIdPattern+elbInfixPattern+regionIdPattern+"\\."+amazonAwsComSuffixPattern+"$");
        final Pattern p2 = Pattern.compile("^"+loadBalancerNameAndIdPattern+"\\."+regionIdPattern+elbInfixPattern+amazonAwsComSuffixPattern+"$");
        final Matcher m1 = p1.matcher(hostname);
        final Matcher result;
        if (m1.matches()) {
            result = m1;
        } else {
            final Matcher m2 = p2.matcher(hostname);
            if (m2.matches()) {
                result = m2;
            } else {
                result = null;
            }
        }
        return result == null ? null : new LoadBalancerDescriptor(result.group(1), result.group(2), Region.of(result.group(3)));
    }

    @Override
    public ApplicationLoadBalancer<ShardingKey> getDNSMappedLoadBalancerFor(String hostname) {
        return Util.stream(getResourceRecordSets(hostname))
                    .filter(rrs->rrs.type() == RRType.CNAME)
                    .flatMap(rrs->rrs.resourceRecords().stream())
                    .filter(rr->isLoadBalancerDNSName(rr.value()))
                    .map(rr->getLoadBalancerDescriptorForDNSName(rr.value()))
                    .findAny()
                    .map(lbDesc->getLoadBalancerByName(lbDesc.getName(), new AwsRegion(lbDesc.getRegion(), this)))
                    .orElse(null);
    }
    
    @Override
    public ApplicationLoadBalancer<ShardingKey> getDNSMappedLoadBalancerFor(com.sap.sse.landscape.Region region, String hostname) {
        final DescribeLoadBalancersResponse response = getLoadBalancingClient(getRegion(region)).describeLoadBalancers();
        for (final LoadBalancer lb : response.loadBalancers()) {
            final ApplicationLoadBalancer<ShardingKey> alb = new ApplicationLoadBalancerImpl<>(region, lb, this);
            for (final Rule rule : alb.getRules()) {
                if (rule.conditions().stream().filter(
                        r->r.hostHeaderConfig() != null && r.hostHeaderConfig().values().contains(hostname))
                        .findAny().isPresent()) {
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

    public Iterable<TagDescription> getTargetGroupTags(String arn, com.sap.sse.landscape.Region region) {
        final software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTagsResponse tagResponse = getLoadBalancingClient(
                getRegion(region)).describeTags(t -> t.resourceArns(arn));
        return tagResponse.tagDescriptions();
    }
    
    @Override
    public Tags addTargetGroupTag(String arn, String key, String value, com.sap.sse.landscape.Region region) {
        Collection<software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag> tags = new ArrayList<>(); 
        tags.add(software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag.builder().key(key).value(value).build());
        getLoadBalancingClient(getRegion(region)).addTags(t -> t.resourceArns(arn).tags(tags));
        return new TagsImpl(key,value);
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
    public RabbitMQEndpoint getDefaultRabbitConfiguration(com.sap.sse.landscape.Region region) {
        final RabbitMQEndpoint defaultRabbitMQInDefaultRegion = ()->SharedLandscapeConstants.RABBIT_IN_DEFAULT_REGION_HOSTNAME; // using default port RabbitMQEndpoint.DEFAULT_PORT
        final RabbitMQEndpoint result;
        if (region.getId().equals(Region.EU_WEST_1.id())) {
            result = defaultRabbitMQInDefaultRegion; 
        } else {
            final Iterable<AwsInstance<ShardingKey>> rabbitMQHostsInRegion = getRunningHostsWithTag(
                    region, SharedLandscapeConstants.RABBITMQ_TAG_NAME, AwsInstanceImpl::new);
            if (rabbitMQHostsInRegion.iterator().hasNext()) {
                final AwsInstance<ShardingKey> anyRabbitMQHost = rabbitMQHostsInRegion.iterator().next();
                result = new RabbitMQEndpoint() {
                    @Override
                    public int getPort() {
                        return getTag(anyRabbitMQHost, SharedLandscapeConstants.RABBITMQ_TAG_NAME)
                                .map(t -> t.trim().isEmpty() ? RabbitMQEndpoint.DEFAULT_PORT : Integer.valueOf(t.trim()))
                                .orElse(RabbitMQEndpoint.DEFAULT_PORT);
                    }
    
                    @Override
                    public String getNodeName() {
                        return anyRabbitMQHost.getHostname();
                    }
                };
            } else {
                result = defaultRabbitMQInDefaultRegion; // no instance with tag found; hope for VPC peering and use RabbitMQ hostname from default region
            }
        }
        return result;
    }

    @Override
    public Database getDatabase(com.sap.sse.landscape.Region region, String databaseName) {
        return new DatabaseImpl(getDatabaseConfigurationForDefaultReplicaSet(region), databaseName);
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
        final CompletableFuture<Iterable<LaunchTemplate>> allLaunchTemplates = getLaunchTemplatesAsync(region);
        final CompletableFuture<Iterable<LaunchTemplateVersion>> allLaunchTemplateDefaultVersions = getLaunchTemplateDefaultVersionsAsync(region);
        final Iterable<HostT> hosts = hostsSupplier.get();
        final Map<String, ProcessT> mastersByServerName = new HashMap<>();
        final Map<String, Set<ProcessT>> replicasByServerName = new HashMap<>();
        final ConcurrentLinkedQueue<Pair<Future<?>, String>> tasksToWaitForAndLogStringForTimeout = new ConcurrentLinkedQueue<>();
        final ScheduledExecutorService backgroundExecutor = ThreadPoolUtil.INSTANCE.createBackgroundTaskThreadPoolExecutor("Application Process Discovery "+UUID.randomUUID());
        for (final ApplicationProcessHost<ShardingKey, MetricsT, ProcessT> host : hosts) {
            tasksToWaitForAndLogStringForTimeout.add(new Pair<>(backgroundExecutor.submit(()->{
                try {
                    for (final ProcessT applicationProcess : host.getApplicationProcesses(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase)) {
                        tasksToWaitForAndLogStringForTimeout.add(new Pair<>(backgroundExecutor.submit(()->{
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
                        }), "Host: "+host.toString()+", process "+applicationProcess.toString()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }), "Host: "+host.toString()));
        }
        // wait for all application processes on all hosts to be discovered
        Pair<Future<?>, String> taskToWaitForAndTimeoutString;
        while ((taskToWaitForAndTimeoutString=tasksToWaitForAndLogStringForTimeout.poll()) != null) {
            final Future<?> taskToWaitFor = taskToWaitForAndTimeoutString.getA();
            try {
                waitForFuture(taskToWaitFor, optionalTimeout);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Problem waiting for "+taskToWaitFor+" for "+taskToWaitForAndTimeoutString.getB(), e);
            }
        }
        backgroundExecutor.shutdown();
        final Set<AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT>> result = new HashSet<>();
        final DNSCache dnsCache = getNewDNSCache();
        synchronized (mastersByServerName) {
            for (final Entry<String, ProcessT> serverNameAndMaster : mastersByServerName.entrySet()) {
                final String serverName = serverNameAndMaster.getKey();
                final ProcessT master = serverNameAndMaster.getValue();
                final Set<ProcessT> replicas = replicasByServerName.get(serverName);
                final AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> replicaSet = getApplicationReplicaSet(
                        serverName, master, replicas, allLoadBalancersInRegion, allTargetGroupsInRegion,
                        allLoadBalancerRulesInRegion, allAutoScalingGroups, allLaunchTemplates, allLaunchTemplateDefaultVersions, dnsCache, 
                        optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
                result.add(replicaSet);
            }
        }
        return result;
    }
    
    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> getApplicationReplicaSet(com.sap.sse.landscape.Region region,
            final String serverName, final ProcessT master, final Iterable<ProcessT> replicas, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws InterruptedException, ExecutionException, TimeoutException {
        final CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> allLoadBalancersInRegion = getLoadBalancersAsync(region);
        final CompletableFuture<Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>>> allTargetGroupsInRegion = getTargetGroupsAsync(region);
        final CompletableFuture<Map<Listener, Iterable<Rule>>> allLoadBalancerRulesInRegion = getLoadBalancerListenerRulesAsync(region, allLoadBalancersInRegion);
        final CompletableFuture<Iterable<AutoScalingGroup>> autoScalingGroups = getAutoScalingGroupsAsync(region);
        final CompletableFuture<Iterable<LaunchTemplate>> launchTemplates = getLaunchTemplatesAsync(region);
        final CompletableFuture<Iterable<LaunchTemplateVersion>> launchTemplateDefaultVersions = getLaunchTemplateDefaultVersionsAsync(region);
        final DNSCache dnsCache = getNewDNSCache();
        return getApplicationReplicaSet(serverName, master, replicas, allLoadBalancersInRegion, allTargetGroupsInRegion,
                allLoadBalancerRulesInRegion, autoScalingGroups, launchTemplates, launchTemplateDefaultVersions, dnsCache,
                optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }

    private <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> getApplicationReplicaSet(
            final String serverName, final ProcessT master, final Iterable<ProcessT> replicas,
            final CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> allLoadBalancersInRegion,
            final CompletableFuture<Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>>> allTargetGroupsInRegion,
            final CompletableFuture<Map<Listener, Iterable<Rule>>> allLoadBalancerRulesInRegion,
            final CompletableFuture<Iterable<AutoScalingGroup>> allAutoScalingGroups,
            final CompletableFuture<Iterable<LaunchTemplate>> allLaunchTemplates, CompletableFuture<Iterable<LaunchTemplateVersion>> allLaunchTemplateDefaultVersions,
            final DNSCache dnsCache, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws InterruptedException, ExecutionException, TimeoutException {
        final AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> replicaSet = new AwsApplicationReplicaSetImpl<ShardingKey, MetricsT, ProcessT>(
                serverName, master, Optional.ofNullable(replicas), allLoadBalancersInRegion, allTargetGroupsInRegion,
                allLoadBalancerRulesInRegion, this, allAutoScalingGroups, allLaunchTemplates, allLaunchTemplateDefaultVersions, dnsCache, pathPrefixForShardingKey,
                optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
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
    
    /**
     * Stores {@link ResourceRecordSet}s keyed by the private IP address(es) (as {@link String}) to which they map.
     * The values in this map are pairs whose {@link Pair#getA() first} component is the resource record set, the
     * {@link Pair#getB() second} is the time point at which the resource record was cached.<p>
     * 
     * When a lookup finds an entry that, based on its {@link ResourceRecordSet#ttl() TTL} has already expired,
     * the entry is removed from the cache, and a new lookup is triggered. If a lookup cannot find an entry,
     * the {@code null} result is cached with the minimum TTL of 60s.<p>
     * 
     * Lookups using the {@link #findHostnamesForIP(String)} method must synchronize on this map to avoid duplicate
     * and parallel remote DNS lookups.
     */
    private static final ConcurrentMap<String, Pair<ResourceRecordSet, TimePoint>> reverseDNSCache = new ConcurrentHashMap<>();
    private static final Duration MINIMUM_DNS_CACHE_TTL_FOR_NOT_FOUND_ENTRIES = Duration.ONE_SECOND.times(60);
    private static TimePoint timePointOfLastDNSListing;
    @Override
    public String findHostnamesForIP(String ipAddress) {
        synchronized (reverseDNSCache) {
            final TimePoint now = TimePoint.now();
            final boolean validCacheEntryFound; // has an entry or a null result been found that is still within the TTL?
            final Pair<ResourceRecordSet, TimePoint> cacheEntryForPrivateIP = reverseDNSCache.get(ipAddress);
            final ResourceRecordSet cachedRRS;
            if (cacheEntryForPrivateIP != null) {
                if (cacheEntryForPrivateIP.getB().plus(Duration.ONE_SECOND.times(cacheEntryForPrivateIP.getA().ttl())).before(now)) {
                    // the entry existed but has expired
                    reverseDNSCache.remove(ipAddress);
                    cachedRRS = null;
                    validCacheEntryFound = false;
                } else {
                    cachedRRS = cacheEntryForPrivateIP.getA();
                    validCacheEntryFound = true;
                }
            } else {
                cachedRRS = null;
                // the null entry is a valid cache statement if the last full listing happened less than our minimum cache TTL ago
                validCacheEntryFound = timePointOfLastDNSListing != null && !timePointOfLastDNSListing.plus(MINIMUM_DNS_CACHE_TTL_FOR_NOT_FOUND_ENTRIES).before(now);
            }
            String result;
            if (validCacheEntryFound) {
                if (cachedRRS != null) {
                    result = getNameWithoutTrailingDotFromRRS(cachedRRS);
                } else {
                    result = null; // a null result was cached and is still within the 60s TTL
                }
            } else {
                // perform a lookup
                timePointOfLastDNSListing = now;
                result = null;
                final Route53Client route53Client = getRoute53Client();
                for (final HostedZone hostedZone : route53Client.listHostedZones().hostedZones()) {
                    for (final ResourceRecordSet resourceRecordSet : route53Client.listResourceRecordSets(b->b.hostedZoneId(hostedZone.id())).resourceRecordSets()) {
                        for (final ResourceRecord resourceRecord : resourceRecordSet.resourceRecords()) {
                            reverseDNSCache.put(resourceRecord.value(), new Pair<>(resourceRecordSet, now)); // cache all that we visit
                            if (Util.equalsWithNull(resourceRecord.value(), ipAddress)) {
                                result = getNameWithoutTrailingDotFromRRS(resourceRecordSet); // don't abort the inner loop but cache all we got
                            }
                        }
                    }
                }
            }
            return result;
        }
    }

    private String getNameWithoutTrailingDotFromRRS(final ResourceRecordSet cachedRRS) {
        return cachedRRS.name().replaceFirst("\\.$", "");
    }
    
    @Override
    public Iterable<ResourceRecordSet> getResourceRecordSets(String hostname) {
        final Route53Client route53Client = getRoute53Client();
        final String hostedZoneId = getDNSHostedZoneId(AwsLandscape.getHostedZoneName(hostname));
        return Util.filter(route53Client.listResourceRecordSets(b->b.hostedZoneId(hostedZoneId).startRecordName(hostname)).resourceRecordSets(),
                resourceRecordSet->AwsLandscape.removeTrailingDotFromHostname(resourceRecordSet.name()).equals(hostname));
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
        logger.info("Setting minimum size of auto-scaling group "+autoScalingGroup.getName()+" to "+minSize);
        getAutoScalingClient(getRegion(autoScalingGroup.getRegion())).updateAutoScalingGroup(b->b
                .autoScalingGroupName(autoScalingGroup.getAutoScalingGroup().autoScalingGroupName())
                .minSize(minSize)
                .desiredCapacity(minSize));
    }
    
    @Override
    public CompletableFuture<Iterable<LaunchTemplate>> getLaunchTemplatesAsync(com.sap.sse.landscape.Region region) {
        final Set<LaunchTemplate> result = new HashSet<>();
        return getEc2AsyncClient(getRegion(region)).describeLaunchTemplatesPaginator().subscribe(response->
            result.addAll(response.launchTemplates())).handle((v, e)->Collections.unmodifiableCollection(result));
    }
    
    @Override
    public CompletableFuture<Iterable<LaunchTemplateVersion>> getLaunchTemplateDefaultVersionsAsync(com.sap.sse.landscape.Region region) {
        final Set<LaunchTemplateVersion> result = new HashSet<>();
        return getEc2AsyncClient(getRegion(region)).describeLaunchTemplateVersionsPaginator(b->b.versions(LandscapeConstants.DEFAULT_LAUNCH_TEMPLATE_VERSION_NAME)).subscribe(response->
            result.addAll(response.launchTemplateVersions())).handle((v, e)->Collections.unmodifiableCollection(result));
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
                    final TargetGroup<ShardingKey> targetGroup = createTargetGroup(this, region,
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
    public void updateReleaseInAutoScalingGroups(com.sap.sse.landscape.Region region,
            LaunchTemplate oldLaunchTemplate, Iterable<AwsAutoScalingGroup> autoScalingGroups,
            String replicaSetName, Release release) {
        logger.info("Adjusting release for auto-scaling groups "+Util.join(", ", autoScalingGroups)+" to "+release);
        final Ec2Client ec2Client = getEc2Client(getRegion(region));
        final LaunchTemplateVersion oldLaunchTemplateVersion = ec2Client.describeLaunchTemplateVersions(b->b
                .launchTemplateName(oldLaunchTemplate.launchTemplateName())
                .versions(LandscapeConstants.DEFAULT_LAUNCH_TEMPLATE_VERSION_NAME)).launchTemplateVersions().iterator().next();
        final String oldUserData = new String(Base64.getDecoder().decode(oldLaunchTemplateVersion.launchTemplateData().userData().getBytes()));
        final String newUserData = oldUserData.replaceFirst(
                "(?m)^"+DefaultProcessConfigurationVariables.INSTALL_FROM_RELEASE.name()+"=(.*)$",
                DefaultProcessConfigurationVariables.INSTALL_FROM_RELEASE.name() + "=\"" + release.getName() + "\"");
        createUpdatedDefaultLaunchTemplateVersion(region, autoScalingGroups, "Using release "+release.getName(),
                b -> b.launchTemplateData(ldtb->ldtb.userData(Base64.getEncoder().encodeToString(newUserData.getBytes()))));
    }
    
    private CreateLaunchTemplateVersionRequest.Builder copyLaunchTemplateVersionToCreateRequestBuilder(LaunchTemplate launchTemplateToCreateNewVersionFor, com.sap.sse.landscape.Region region) {
        return CreateLaunchTemplateVersionRequest.builder().launchTemplateId(launchTemplateToCreateNewVersionFor.launchTemplateId()).sourceVersion(LandscapeConstants.DEFAULT_LAUNCH_TEMPLATE_VERSION_NAME);
    }

    @Override
    public void updateImageInAutoScalingGroups(com.sap.sse.landscape.Region region, Iterable<AwsAutoScalingGroup> autoScalingGroups, String replicaSetName, AmazonMachineImage<ShardingKey> ami) {
        logger.info("Adjusting AMI for auto-scaling group(s) "+Util.join(", ", autoScalingGroups)+" to "+ami);
        createUpdatedDefaultLaunchTemplateVersion(region, autoScalingGroups,
                "Using AMI "+ami.getName()+" with ID "+ami.getId(),
                b->b.launchTemplateData(ltdb->ltdb.imageId(ami.getId())));
    }

    @Override
    public void updateInstanceTypeInAutoScalingGroup(com.sap.sse.landscape.Region region, Iterable<AwsAutoScalingGroup> autoScalingGroups, String replicaSetName, InstanceType instanceType) {
        logger.info("Adjusting instance type for auto-scaling group(s) "+Util.join(", ", autoScalingGroups)+" to "+instanceType);
        createUpdatedDefaultLaunchTemplateVersion(region, autoScalingGroups,
                "Using new instance type "+instanceType.name(),
                b->b.launchTemplateData(ltdb->ltdb.instanceType(instanceType)));
    }

    /**
     * Creates a new version of the launch template used by the {@code autoScalingGroups}. This method assumes that all
     * of the auto-scaling groups use the same launch template. The new launch template version will be made the
     * "$Default" version, hence all {@code autoScalingGroups} will use the new version automatically since we assume
     * that those groups are all set to use the {@code $Default} version of the launch template.
     * 
     * @param builderConsumer
     *            allows the caller to specify the new version by making modifications to the current "$Default" launch
     *            template version
     */
    private void createUpdatedDefaultLaunchTemplateVersion(com.sap.sse.landscape.Region region, Iterable<AwsAutoScalingGroup> autoScalingGroups,
            String newLaunchTemplateVersionDescription, Consumer<CreateLaunchTemplateVersionRequest.Builder> builderConsumer) {
        if (Util.isEmpty(autoScalingGroups)) {
            throw new IllegalArgumentException("At least one auto-scaling group must be provided for updating a launch template");
        }
        logger.info("Creating a new, adjusted default launch template version for auto-scaling group(s) "+Util.join(", ", autoScalingGroups));
        final LaunchTemplate launchTemplate = Util.first(autoScalingGroups).getLaunchTemplate();
        final CreateLaunchTemplateVersionRequest.Builder createLaunchTemplateVersionRequestBuilder = copyLaunchTemplateVersionToCreateRequestBuilder(launchTemplate, region);
        createLaunchTemplateVersionRequestBuilder
            .versionDescription(newLaunchTemplateVersionDescription);
        builderConsumer.accept(createLaunchTemplateVersionRequestBuilder);
        final CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest = createLaunchTemplateVersionRequestBuilder.build();
        logger.info("Creating new launch template version \""+newLaunchTemplateVersionDescription+"\" for launch template "+launchTemplate.launchTemplateName());
        final Ec2Client ec2Client = getEc2Client(getRegion(region));
        final CreateLaunchTemplateVersionResponse launchTemplateVersionResponse = ec2Client.createLaunchTemplateVersion(createLaunchTemplateVersionRequest);
        ec2Client.modifyLaunchTemplate(b->b
                .launchTemplateId(launchTemplate.launchTemplateId())
                .defaultVersion(launchTemplateVersionResponse.launchTemplateVersion().versionNumber().toString()));
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    void createLaunchTemplateAndAutoScalingGroup(
            com.sap.sse.landscape.Region region, String replicaSetName, Optional<Tags> tags,
            TargetGroup<ShardingKey> publicTargetGroup, String keyName, InstanceType instanceType,
            String imageId, AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> replicaConfiguration,
            int minReplicas, int maxReplicas, int maxRequestsPerTarget) {
        logger.info("Creating launch template for replica set "+replicaSetName);
        final Region awsRegion = getRegion(region);
        final Ec2Client ec2Client = getEc2Client(awsRegion);
        final AutoScalingClient autoScalingClient = getAutoScalingClient(awsRegion);
        final String launchTemplateName = replicaSetName;
        final String autoScalingGroupName = getAutoScalingGroupName(replicaSetName);
        final Iterable<AwsAvailabilityZone> availabilityZones = getAvailabilityZones(region);
        final SecurityGroup securityGroup = getDefaultSecurityGroupForApplicationHosts(region);
        final int instanceWarmupTimeInSeconds = (int) Duration.ONE_MINUTE.times(3).asSeconds();
        ec2Client.createLaunchTemplate(b->b
                .launchTemplateName(launchTemplateName)
                .launchTemplateData(ltdb->ltdb
                    .keyName(keyName)
                    .imageId(imageId)
                    .monitoring(i->i.enabled(true))
                    .securityGroupIds(securityGroup.getId())
                    .userData(Base64.getEncoder().encodeToString(replicaConfiguration.getAsEnvironmentVariableAssignments().getBytes()))
                    .instanceType(instanceType.toString())));
        logger.info("Creating auto-scaling group for replica set "+replicaSetName);
        String vpcIdentifier = String.join(",", Util.mapToArrayList(availabilityZones,
                az -> getSubnetForAvailabilityZoneInSameVpcAsSecurityGroup(az, securityGroup, awsRegion).subnetId())
                .stream().filter(az -> az == null ? false : true).distinct().toArray(String[]::new)); // Fetches all unique subnets that are in the specified AZs and the same VPC as the security group.
        autoScalingClient.createAutoScalingGroup(b -> {
            b.minSize(minReplicas).maxSize(maxReplicas).healthCheckGracePeriod(instanceWarmupTimeInSeconds)
                    .autoScalingGroupName(autoScalingGroupName).vpcZoneIdentifier(vpcIdentifier)
                    .targetGroupARNs(publicTargetGroup.getTargetGroupArn())
                    .launchTemplate(LaunchTemplateSpecification.builder()
                            .launchTemplateName(launchTemplateName)
                            .version(LandscapeConstants.DEFAULT_LAUNCH_TEMPLATE_VERSION_NAME).build());
            tags.ifPresent(t -> {
                final List<software.amazon.awssdk.services.autoscaling.model.Tag> awsTags = new ArrayList<>();
                for (final Entry<String, String> tag : t) {
                    awsTags.add(software.amazon.awssdk.services.autoscaling.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build());
                }
                b.tags(awsTags);
            });
        });
        enableAutoScalingGroupMetricCollection(autoScalingGroupName, autoScalingClient);
        putScalingPolicy(instanceWarmupTimeInSeconds, autoScalingGroupName, publicTargetGroup , maxRequestsPerTarget, region);
    }
    
    private void enableAutoScalingGroupMetricCollection(String autoscalinggroupName, AutoScalingClient client) {
        // see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/autoscaling/model/EnableMetricsCollectionRequest.html
        // If you specify Granularity and don't specify any metrics, all metrics are enabled.
        final EnableMetricsCollectionRequest request = EnableMetricsCollectionRequest.builder().autoScalingGroupName(autoscalinggroupName).granularity("1Minute").build();
        client.enableMetricsCollection(request);
    }

    public String getAutoScalingGroupName(String replicaSetName) {
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
    public CompletableFuture<Void> removeAutoScalingGroupAndLaunchTemplate(AwsAutoScalingGroup autoScalingGroup) {
        final String launchTemplateId = autoScalingGroup.getAutoScalingGroup().launchTemplate()==null?null:autoScalingGroup.getAutoScalingGroup().launchTemplate().launchTemplateId();
        final String launchTemplateName = autoScalingGroup.getAutoScalingGroup().launchTemplate()==null?null:autoScalingGroup.getAutoScalingGroup().launchTemplate().launchTemplateName();
        final Ec2AsyncClient ec2AsyncClient = getEc2AsyncClient(getRegion(autoScalingGroup.getRegion()));
        return removeAutoScalingGroup(autoScalingGroup)
            .thenAccept(response->{
                if (launchTemplateId != null) {
                    logger.info("Removing launch template "+launchTemplateName+" with ID "+launchTemplateId);
                    ec2AsyncClient.deleteLaunchTemplate(b->b.launchTemplateId(launchTemplateId));
                }
            });
    }
    
    @Override
    public CompletableFuture<DeleteAutoScalingGroupResponse> removeAutoScalingGroup(AwsAutoScalingGroup autoScalingGroup) {
        final AutoScalingAsyncClient autoScalingAsyncClient = getAutoScalingAsyncClient(getRegion(autoScalingGroup.getRegion()));
        logger.info("Removing auto-scaling group "+autoScalingGroup.getAutoScalingGroup().autoScalingGroupName());
        return autoScalingAsyncClient.deleteAutoScalingGroup(b->b.forceDelete(true).autoScalingGroupName(autoScalingGroup.getAutoScalingGroup().autoScalingGroupName()));
    }

    @Override
    public TargetGroup<ShardingKey> createTargetGroupWithoutLoadbalancer(com.sap.sse.landscape.Region region, String targetGroupName, int port, String vpcId) {
        return createTargetGroup(region, targetGroupName, port, ApplicationProcess.HEALTH_CHECK_PATH, port, null, vpcId);
    }
    
    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>> 
    String createAutoScalingGroupFromExisting(AwsAutoScalingGroup autoScalingParent,
            String shardName, TargetGroup<ShardingKey> targetGroup, int minSize, Optional<Tags> tags) {
        final AutoScalingClient autoScalingClient = getAutoScalingClient(getRegion(autoScalingParent.getRegion()));
        final String launchTemplateId = autoScalingParent.getLaunchTemplate().launchTemplateId();
        final String autoScalingGroupName = getAutoScalingGroupName(shardName);
        final List<String> availabilityZones = autoScalingParent.getAutoScalingGroup().availabilityZones();
        final int instanceWarmupTimeInSeconds = autoScalingParent.getAutoScalingGroup().defaultInstanceWarmup() != null ? autoScalingParent.getAutoScalingGroup().defaultInstanceWarmup() : 180 ;
        logger.info(
                "Creating Auto-Scaling Group " + autoScalingGroupName +" for Shard "+shardName + ". Inheriting from Auto-Scaling Group: " +
                        autoScalingParent.getName() + ". Starting with " + minSize + " instances.");
        autoScalingClient.createAutoScalingGroup(b->{
            b
                .minSize(minSize)
                .maxSize(autoScalingParent.getAutoScalingGroup().maxSize())
                .healthCheckGracePeriod(instanceWarmupTimeInSeconds)
                .autoScalingGroupName(autoScalingGroupName)
                .availabilityZones(availabilityZones)
                .targetGroupARNs(targetGroup.getTargetGroupArn())
                .launchTemplate(ltb->ltb.launchTemplateId(launchTemplateId).version(LandscapeConstants.DEFAULT_LAUNCH_TEMPLATE_VERSION_NAME));
            final List<software.amazon.awssdk.services.autoscaling.model.Tag> awsTags = new ArrayList<>();
            final List<software.amazon.awssdk.services.autoscaling.model.TagDescription> parentTags = autoScalingParent.getAutoScalingGroup().tags();
            for (final software.amazon.awssdk.services.autoscaling.model.TagDescription parentTag : parentTags) {
                awsTags.add(software.amazon.awssdk.services.autoscaling.model.Tag.builder()
                        .key(parentTag.key())
                        .value(parentTag.key().equals("Name") ? parentTag.value()+" ("+shardName+")" : parentTag.value())
                        .propagateAtLaunch(parentTag.propagateAtLaunch())
                        .build());
            }
            tags.ifPresent(t->{
                for (final Entry<String, String> tag : t) {
                    awsTags.add(software.amazon.awssdk.services.autoscaling.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build());
                }
            });
            b.tags(awsTags);
        });
        enableAutoScalingGroupMetricCollection(autoScalingGroupName, autoScalingClient);
        autoScalingClient.close();
        return autoScalingGroupName;
    }
    
    @Override
    public void resetShardMinAutoscalingGroupSize(String autoscalinggroupName, com.sap.sse.landscape.Region region) {
        final AutoScalingClient autoScalingClient = getAutoScalingClient(getRegion(region));
        autoScalingClient.updateAutoScalingGroup(t -> t.autoScalingGroupName(autoscalinggroupName)
                .minSize(ShardProcedure.DEFAULT_MINIMUM_AUTO_SCALING_GROUP_SIZE).build());
        autoScalingClient.close();
    }

    @Override
    public TargetGroup<ShardingKey> copyTargetGroup(TargetGroup<ShardingKey> parent, String suffix) {
        TargetGroup<ShardingKey> child =  createTargetGroupWithoutLoadbalancer(parent.getRegion(), parent.getName()+ suffix, parent.getPort(),
                parent.getLoadBalancer().getVpcId());
        child.addTargets(parent.getRegisteredTargets().keySet());
        return child;
    }

    @Override
    public Iterable<Rule> modifyRuleActions(com.sap.sse.landscape.Region region, Rule rule) {
        ModifyRuleResponse res = getLoadBalancingClient(getRegion(region))
                .modifyRule(t -> t.actions(rule.actions()).ruleArn(rule.ruleArn()).build());
        return res.rules();
    }

    @Override
    public <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>> void putScalingPolicy(
            int instanceWarmupTimeInSeconds, String autoScalingGroupName, TargetGroup<ShardingKey> targetgroup,
            int maxRequestPerTarget, com.sap.sse.landscape.Region region) {
        final AutoScalingClient autoScalingClient = getAutoScalingClient(getRegion(region));
        autoScalingClient.putScalingPolicy(
                b -> b.autoScalingGroupName(autoScalingGroupName).estimatedInstanceWarmup(instanceWarmupTimeInSeconds)
                        .policyType("TargetTrackingScaling").policyName("KeepRequestsPerTargetAt" + maxRequestPerTarget)
                        .targetTrackingConfiguration(t -> t
                                .predefinedMetricSpecification(p -> p
                                        .resourceLabel("app/" + targetgroup.getLoadBalancer().getName() + "/"
                                                + targetgroup.getLoadBalancer().getId() + "/targetgroup/"
                                                + targetgroup.getName() + "/" + targetgroup.getId())
                                        .predefinedMetricType(MetricType.ALB_REQUEST_COUNT_PER_TARGET))
                                .targetValue((double) AwsAutoScalingGroup.DEFAULT_MAX_REQUESTS_PER_TARGET)));
    }
}
