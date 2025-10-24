package com.sap.sse.landscape.aws;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.MachineImage;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.SecurityGroup;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.ApplicationReplicaSet;
import com.sap.sse.landscape.aws.impl.Activator;
import com.sap.sse.landscape.aws.impl.AwsInstanceImpl;
import com.sap.sse.landscape.aws.impl.AwsLandscapeImpl;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.impl.AwsTargetGroupImpl;
import com.sap.sse.landscape.aws.impl.DNSCache;
import com.sap.sse.landscape.aws.orchestration.AwsApplicationConfiguration;
import com.sap.sse.landscape.aws.orchestration.ShardProcedure;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.landscape.mongodb.MongoProcess;
import com.sap.sse.landscape.mongodb.MongoProcessInReplicaSet;
import com.sap.sse.landscape.mongodb.MongoReplicaSet;
import com.sap.sse.landscape.mongodb.impl.MongoProcessImpl;
import com.sap.sse.landscape.ssh.SSHKeyPair;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DeleteAutoScalingGroupResponse;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancerState;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RulePriorityPair;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TagDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealth;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealthDescription;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;
import software.amazon.awssdk.services.sts.model.Credentials;

/**
 * A simplified, largely stateless view onto the AWS SDK API that is geared towards specific ways and patterns of
 * managing an application and infrastructure landscape. Only the credentials provided to this object during its
 * construction constitute its state which is used to create, among others, the {@link Ec2Client},
 * {@link Route53Client}, {@link CloudWatchClient} and {@link ElasticLoadBalancingV2Client} client objects to manage the
 * underlying AWS landscape.
 * <p>
 * 
 * A {@link Activator#getDefaultLandscape() default instance} of this landscape interface may be obtained from this
 * bundle's {@link Activator} if the necessary credentials have been supplied as system properties; it is then
 * registered with the OSGi service registry under this interface. Such a default instance may be used, e.g., for
 * automated, orchestrated infrastructure processes such as establishing dedicated infrastructure for an event that is
 * about to start, or archiving an event that has finished.
 * <p>
 * 
 * Clients may also create dedicated instances of this service wrapper, using their own credentials. See
 * {@link #obtain(String, String, Optional, String)}.
 * <p>
 * 
 * This object interacts with an instance of {@link AwsLandscapeState} which keeps persistent and replicable state about
 * the landscape, such as the set of SSH key pairs.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <ShardingKey>
 * @param <MetricsT>
 */
public interface AwsLandscape<ShardingKey> extends Landscape<ShardingKey> {
    long DEFAULT_DNS_TTL_SECONDS = 60l;
    
    String ACCESS_KEY_ID_SYSTEM_PROPERTY_NAME = "com.sap.sse.landscape.aws.accesskeyid";

    String SECRET_ACCESS_KEY_SYSTEM_PROPERTY_NAME = "com.sap.sse.landscape.aws.secretaccesskey";
    
    String MFA_TOKEN_CODE_SYSTEM_PROPERTY_NAME = "com.sap.sse.landscape.aws.mfatokencode";
    
    String SESSION_TOKEN_SYSTEM_PROPERTY_NAME = "com.sap.sse.landscape.aws.sessiontoken";
    
    /**
     * The name of the tag used on {@link AwsInstance hosts} running one or more {@link MongoProcess}(es). The tag value
     * provides information about the replica sets and the ports on which the respective {@link MongoProcess} is listening.
     * If no port is explicitly specified, the default port {@link MongoProcess#DEFAULT_PORT 27017} is assumed. If the replica
     * set name is empty then the port or at least ":" must be specified and a standalone process is assumed. The
     * format for the tag value is as follows:<pre>
     *   ({replica-set-name}(:{port})?)(,({replica-set-name}(:{port})?))*
     * </pre>
     * In other words, the tag value is expected to be a comma-separated list of replica set name and optional port specifications
     * where the optional port specification is appended to the replica set name separated by a colon (:).<p>
     * 
     * Examples:<pre>
     *   archive:10201,:10202,live:10203
     *   :
     * </pre>
     * The first example means that three MongoDB processes are running on the host, one listening on port 10201 which is part of the
     * replica set "archive", one standalone instance listening on port 10202, and a member of the "live" replica set running
     * on port 10203. The second example (":") means that a single standalong MongoDB process is assumed to be running on the
     * default port (usually 27017).
     */
    String MONGO_REPLICA_SETS_TAG_NAME = "mongo-replica-sets";

    String MONGO_DEFAULT_REPLICA_SET_NAME = "live";
    
    String MONGO_REPLICA_SET_NAME_AND_PORT_SEPARATOR = ":";
    
    /**
     * Based on system properties for the AWS access key ID and the secret access key (see
     * {@link #ACCESS_KEY_ID_SYSTEM_PROPERTY_NAME} and {@link #SECRET_ACCESS_KEY_SYSTEM_PROPERTY_NAME}), this method
     * returns a landscape object which internally has access to the clients for the underlying AWS landscape, such as
     * an EC2 client, a Route53 client, etc. Note that this way no multi-factor authentication (MFA) is possible. If
     * the system properties described above are not set or not valid, an unauthenticated landscape object will result;
     * some rudimentary things may still work, such as querying the set of regions.
     */
    static <ShardingKey, MetricsT extends ApplicationProcessMetrics,
    ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsLandscape<ShardingKey> obtain(String pathPrefixForShardingKey) {
        final AwsLandscape<ShardingKey> result = new AwsLandscapeImpl<>(Activator.getInstance().getLandscapeState(), pathPrefixForShardingKey);
        return result;
    }
    
    /**
     * Based on an explicit AWS access key ID and the secret access key, this method returns a landscape object, but not
     * multi factor-authenticated (MFA). Can be used for operations not requiring MFA, such as obtaining an MFA-authenticated
     * version of the landscape.
     */
    static <ShardingKey, MetricsT extends ApplicationProcessMetrics,
    ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsLandscape<ShardingKey> obtain(String accessKey, String secret, String pathPrefixForShardingKey) {
        final AwsLandscape<ShardingKey> result = new AwsLandscapeImpl<>(Activator.getInstance().getLandscapeState(), accessKey, secret, pathPrefixForShardingKey);
        return result;
    }
    
    /**
     * Based on an explicit AWS access key ID, the secret access key, and an MFA token code, this method returns a
     * landscape object which internally has access to the clients for the underlying AWS landscape, such as an EC2
     * client, a Route53 client, etc.
     */
    static <ShardingKey, MetricsT extends ApplicationProcessMetrics,
    ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsLandscape<ShardingKey> obtain(String accessKey, String secret, String sessionToken, String pathPrefixForShardingKey) {
        final AwsLandscape<ShardingKey> result = new AwsLandscapeImpl<>(Activator.getInstance().getLandscapeState(), accessKey, secret, sessionToken, pathPrefixForShardingKey);
        return result;
    }
    
    default AwsInstance<ShardingKey> launchHost(MachineImage image, InstanceType instanceType,
            AwsAvailabilityZone availabilityZone, String keyName, Iterable<SecurityGroup> securityGroups,
            Optional<Tags> tags, String... userData) {
        final HostSupplier<ShardingKey, AwsInstance<ShardingKey>> hostSupplier =
                (instanceId, az, privateIpAddress, launchTimePoint, landscape)->new AwsInstanceImpl<ShardingKey>(instanceId, az,
                        privateIpAddress, launchTimePoint, landscape);
        return launchHost(hostSupplier, image, instanceType, availabilityZone, keyName, securityGroups, tags, userData);
    }
    
    /**
     * Launches a new {@link Host} from a given image into the availability zone specified and controls network access
     * to that instance by setting the security groups specified for the resulting host.
     * @param keyName
     *            the SSH key pair name to use when launching; this will grant root access with the corresponding
     *            private key; see also {@link #getKeyPairInfo(Region, String)}
     * @param userData
     *            zero or more strings representing the user data to be passed to the instance; multiple strings will be
     *            concatenated, using the line separator to join them. The instance is able to read the user data throuh
     *            the AWS SDK installed on the instance.
     */
    default <HostT extends AwsInstance<ShardingKey>> HostT launchHost(
            HostSupplier<ShardingKey, HostT> hostSupplier,
            MachineImage fromImage, InstanceType instanceType, AwsAvailabilityZone az, String keyName,
            Iterable<SecurityGroup> securityGroups, Optional<Tags> tags, String... userData) {
        return launchHosts(hostSupplier, /* numberOfHostsToLaunch */ 1, fromImage, instanceType, az, keyName,
                securityGroups, tags, userData).iterator().next();
    }

    // -------------------- technical landscape services -----------------
    
    /**
     * Launches a number of new {@link Host}s from a given image into the availability zone specified and controls
     * network access to that instance by setting the security groups specified for the resulting host.
     * @param keyName
     *            the SSH key pair name to use when launching; this will grant root access with the corresponding
     *            private key; see also {@link #getKeyPairInfo(Region, String)}
     */
    <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> launchHosts(
            HostSupplier<ShardingKey, HostT> hostSupplier, int numberOfHostsToLaunch,
            MachineImage fromImage, InstanceType instanceType,
            AwsAvailabilityZone az, String keyName, Iterable<SecurityGroup> securityGroups, Optional<Tags> tags,
            String... userData);

    AmazonMachineImage<ShardingKey> getImage(Region region, String imageId);

    AmazonMachineImage<ShardingKey> createImage(AwsInstance<ShardingKey> instance, String imageName, Optional<Tags> tags);

    void deleteImage(Region region, String imageId);

    AmazonMachineImage<ShardingKey> getLatestImageWithTag(Region region, String tagName, String tagValue);
    
    default AmazonMachineImage<ShardingKey> getLatestImageWithType(Region region, String imageType) {
        return getLatestImageWithTag(region, IMAGE_TYPE_TAG_NAME, imageType);
    }
    
    Iterable<AmazonMachineImage<ShardingKey>> getAllImagesWithTag(Region region, String tagName, String tagValue);

    default Iterable<AmazonMachineImage<ShardingKey>> getAllImagesWithType(Region region, String imageType) {
        return getAllImagesWithTag(region, IMAGE_TYPE_TAG_NAME, imageType);
    }

    /**
     * Use the results, e.g., as second parameter in in {@link #getLatestImageWithType(Region, String)}.
     */
    Iterable<String> getMachineImageTypes(Region region);
    
    void setSnapshotName(Region region, String snapshotId, String snapshotName);
    
    void deleteSnapshot(Region region, String snapshotId);
    
    /**
     * Finds EC2 instances in the {@code region} that have a tag named {@code tagName} with value {@code tagValue}. The
     * result includes instances regardless their state; they are not required to be RUNNING.
     * 
     * @see #getRunningHostsWithTagValue(Region, String)
     */
    <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> getHostsWithTagValue(Region region, String tagName, String tagValue, HostSupplier<ShardingKey, HostT> hostSupplier);

    /**
     * Finds EC2 instances in the {@code region} that have a tag named {@code tagName}. The tag may have any value. The
     * result includes instances regardless their state; they are not required to be RUNNING.
     * 
     * @see #getRunningHostsWithTag(Region, String, HostSupplier<ShardingKey, HostT>)
     */
    <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> getHostsWithTag(Region region, String tagName, HostSupplier<ShardingKey, HostT> hostSupplier);
    
    <HostT extends AwsInstance<ShardingKey>> HostT getHostByInstanceId(com.sap.sse.landscape.Region region, final String instanceId, HostSupplier<ShardingKey, HostT> hostSupplier);

    /**
     * Finds EC2 instances in the {@code region} that have a tag named {@code tagName}. The tag may have any value. The
     * instances returned have been in state RUNNING at the time of the request.
     */
    <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> getRunningHostsWithTag(Region region, String tagName, HostSupplier<ShardingKey, HostT> hostSupplier);

    <HostT extends AwsInstance<ShardingKey>> HostT getHostByPrivateIpAddress(Region region, String publicIpAddress,
            HostSupplier<ShardingKey, HostT> hostSupplier);

    <HostT extends AwsInstance<ShardingKey>> HostT getHostByPublicIpAddress(Region region, String publicIpAddress,
            HostSupplier<ShardingKey, HostT> hostSupplier);

    /**
     * Finds EC2 instances in the {@code region} that have a tag named {@code tagName} with value {@code tagValue}. The
     * instances returned have been in state RUNNING at the time of the request.
     */
    <HostT extends AwsInstance<ShardingKey>> Iterable<HostT> getRunningHostsWithTagValue(Region region, String tagName, String tagValue, HostSupplier<ShardingKey, HostT> hostSupplier);

    KeyPairInfo getKeyPairInfo(Region region, String keyName);

    Iterable<KeyPairInfo> getAllKeyPairInfos(Region region);
    
    void deleteKeyPair(Region region, String keyName);
    
    /**
     * Uploads the public key to AWS under the name "keyName", stores it in this landscape and returns the key pair.<p>
     * 
     * The calling subject must have {@code CREATE} permission for the key and the {@code CREATE_OBJECT} permission for
     * the current server.
     */
    SSHKeyPair importKeyPair(Region region, byte[] publicKey, byte[] encryptedPrivateKey, String keyName) throws JSchException;

    void terminate(AwsInstance<ShardingKey> host);

    /**
     * The calling subject must have {@code READ} permission for the key requested.
     */
    SSHKeyPair getSSHKeyPair(Region region, String keyName);
    
    /**
     * Obtains all SSH key pairs known by this landscape. Clients shall check the {@code READ} permission before handing out those keys.
     */
    Iterable<SSHKeyPair> getSSHKeyPairs();

    /**
     * Adds a key pair with {@link KeyPair#decrypt(byte[]) decrypted} private key to the AWS {@code region} identified
     * and stores it persistently also in the local server's database with the private key encrypted. This will currently
     * not work for keys of type ED25519 because the {@code getPrivateKey()} method of {@link KeyPair} is not implemented
     * for that key type and instead throws an {@link UnsupportedOperationException}.
     * <p>
     * 
     * The calling subject must have {@code CREATE} permission for the key and the {@code CREATE_OBJECT} permission for
     * the current server.
     */
    SSHKeyPair addSSHKeyPair(com.sap.sse.landscape.Region region, String creator, String keyName, KeyPair keyPairWithDecryptedPrivateKey) throws JSchException;

    /**
     * Creates an RSA key pair with the given name in the region specified and obtains the key details and stores them in
     * this landscape persistently, such that {@link #getKeyPairInfo(Region, String)} as well as
     * {@link #getSSHKeyPair(Region, String)} will be able to obtain (information on) the key. The private key is
     * stored encrypted with the passphrase provided as parameter {@code privateKeyEncryptionPassphrase}.<p>
     * 
     * The calling subject must have {@code CREATE} permission for the key and the {@code CREATE_OBJECT} permission for
     * the current server.
     * 
     * @return the key ID as string, usually starting with the prefix "key-"
     */
    SSHKeyPair createKeyPair(Region region, String keyName, byte[] privateKeyEncryptionPassphrase) throws JSchException;

    Instance getInstance(String instanceId, Region region);

    Instance getInstanceByPublicIpAddress(Region region, String publicIpAddress);

    Instance getInstanceByPrivateIpAddress(Region region, String publicIpAddress);

    /**
     * @param hostname
     *            the fully-qualified host name
     * @param force
     *            if {@code true} and a DNS record with the name as specified by {@code hostname} already exists in the
     *            hosted zone with ID {@code hostedZoneId} then that resource record is updated; if {@code false}, an
     *            {@link IllegalStateException} is thrown if the DNS record already exists and has a value different
     *            from the {@code host}'s {@link Host#getPublicAddress() public IP address}.
     */
    ChangeInfo setDNSRecordToHost(String hostedZoneId, String hostname, Host host, boolean force);

    /**
     * Creates a {@code CNAME} record in the DNS's hosted zone identified by the {@code hostedZoneId} that lets
     * {@code hostname} point to the {@code alb} application load balancer's {@link ApplicationLoadBalancer#getDNSName()
     * A record name}.
     * 
     * @param hostname
     *            the fully-qualified host name
     * @param force
     *            if {@code true} and a DNS record with the name as specified by {@code hostname} already exists in the
     *            hosted zone with ID {@code hostedZoneId} then that resource record is updated; if {@code false}, an
     *            {@link IllegalStateException} is thrown if the DNS record already exists and has a value different
     *            from the {@code alb}'s {@link ApplicationLoadBalancer#getDNSName() DNS name}.
     */
    ChangeInfo setDNSRecordToApplicationLoadBalancer(String hostedZoneId, String hostname, ApplicationLoadBalancer<ShardingKey> alb, boolean force);

    String getDNSHostedZoneId(String hostedZoneName);

    /**
     * In the DNS fully-qualified hostnames are regularly listed with a trailing "." (dot) character whereas
     * for DNS requests more often than not this dot is missing. Comparing hostnames requires normalization.
     * This method removes a trailing dot from a string if it is present.
     */
    static String removeTrailingDotFromHostname(String hostname) {
        return hostname.replaceFirst("\\.$", "");
    }

    /**
     * @param hostname
     *            the fully-qualified host name
     * @param force
     *            if {@code true} and a DNS record with the name as specified by {@code hostname} already exists in the
     *            hosted zone with ID {@code hostedZoneId} then that resource record is updated; if {@code false}, an
     *            {@link IllegalStateException} is thrown if the DNS record already exists and has a value different
     *            from {@code value}.
     */
    ChangeInfo setDNSRecordToValue(String hostedZoneId, String hostname, String value, boolean force);

    /**
     * @param hostname
     *            the fully-qualified host name
     * @param value
     *            the address to which the record to remove did resolve the hostname, e.g., the value passed to the
     *            {@link #setDNSRecordToValue(String, String, String, boolean)} earlier
     */
    ChangeInfo removeDNSRecord(String hostedZoneId, String hostname, RRType type, String value);

    /**
     * Removes the A record (IPv4 address) for {@code hostname}
     * 
     * @param hostname
     *            the fully-qualified host name
     * @param value
     *            the address to which the record to remove did resolve the hostname, e.g., the value passed to the
     *            {@link #setDNSRecordToValue(String, String, String, boolean)} earlier
     */
    ChangeInfo removeDNSRecord(String hostedZoneId, String hostname, String value);
    
    ChangeInfo getUpdatedChangeInfo(ChangeInfo changeInfo);

    Iterable<ApplicationLoadBalancer<ShardingKey>> getLoadBalancers(Region region);

    CompletableFuture<Map<TargetGroup<ShardingKey>, Iterable<TargetHealthDescription>>> getTargetGroupsAsync(Region region);
    
    Iterable<TargetGroup<ShardingKey>> getTargetGroups(com.sap.sse.landscape.Region region);

    CompletableFuture<Iterable<TargetHealthDescription>> getTargetHealthDescriptionsAsync(Region region, TargetGroup<ShardingKey> targetGroup);

    CompletableFuture<Map<Listener, Iterable<Rule>>> getLoadBalancerListenerRulesAsync(
            Region region, CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> allLoadBalancersInRegion);

    CompletableFuture<Iterable<ApplicationLoadBalancer<ShardingKey>>> getLoadBalancersAsync(Region region);
    
    ApplicationLoadBalancer<ShardingKey> getLoadBalancer(String loadBalancerArn, Region region);

    ApplicationLoadBalancer<ShardingKey> getLoadBalancerByName(String name, Region region);

    /**
     * Creates an application load balancer with the name and in the region specified. The method returns once the
     * request has been responded to. The load balancer may still be in a pre-ready state. Use
     * {@link #getApplicationLoadBalancerStatus(ApplicationLoadBalancer)} to find out more.
     * <p>
     * 
     * The load balancer features two listeners: an HTTP listener for port 80 that redirects all requests to HTTPS port
     * 443 with host, path, and query left unchanged; and an HTTPS listener that forwards to a default target group to
     * which the default central reverse proxy of the {@code region} is added as a target.
     * 
     * @param securityGroupForVpc
     *            if provided, the security group's VPC association will be used to constrain the subnets for the AZs to
     *            that VPC; if {@code null}, the default subnet for each respective AZ is used
     */
    ApplicationLoadBalancer<ShardingKey> createLoadBalancer(String name, Region region,
            SecurityGroup securityGroupForVpc) throws InterruptedException, ExecutionException;

    Iterable<Listener> getListeners(ApplicationLoadBalancer<ShardingKey> alb);

    CompletableFuture<Listener> getHttpsListenerAsync(Region region, ApplicationLoadBalancer<ShardingKey> loadBalancer);
    
    LoadBalancerState getApplicationLoadBalancerStatus(ApplicationLoadBalancer<ShardingKey> alb);

    Iterable<AwsAvailabilityZone> getAvailabilityZones(Region awsRegion);

    AwsAvailabilityZone getAvailabilityZoneByName(Region region, String availabilityZoneName);

    /**
     * Deletes this load balancer and all its target groups (the target groups to which this load balancer currently
     * forwards any traffic).
     */
    void deleteLoadBalancer(ApplicationLoadBalancer<ShardingKey> alb);
    
    /**
     * All target groups that have the load balancer identified by the ARN as "their" load balancer which means that
     * this load balancer is forwarding traffic to all those target groups.
     */
    Iterable<TargetGroup<ShardingKey>> getTargetGroupsByLoadBalancerArn(Region region, String loadBalancerArn);

    /**
     * Looks up a target group by its name in a region. The main reason is to obtain the target group's ARN in order to
     * enable construction of the {@link TargetGroup} wrapper object. The {@link TargetGroup#getLoadBalancerArn()}
     * 
     * 
     * @return {@code null} if no target group named according to the value of {@code targetGroupName} is found in the
     *         {@code region}
     */
    TargetGroup<ShardingKey> getTargetGroup(Region region, String targetGroupName);

    /**
     * Like {@link #getTargetGroup(Region, String)}, but if a non-{@code null} {@code loadBalancerArn}
     * is provided, it is used; otherwise, the load balancer ARN as discovered from the target group will
     * be used; for a target group to which no load balancer rule points currently, a {@code null} value
     * will result.
     */
    TargetGroup<ShardingKey> getTargetGroup(Region region, String targetGroupName, String loadBalancerArn);

    /**
     * Creates a target group with a default configuration that includes a health check URL. Stickiness is enabled with
     * the default duration of one day. The load balancing algorithm is set to {@code least_outstanding_requests}. The
     * protocol (HTTP or HTTPS) is inferred from the port: 443 means HTTPS; anything else means HTTP.
     * @param loadBalancerArn
     *            will be set as the resulting target group's {@link TargetGroup#getLoadBalancerArn() load balancer
     *            ARN}. This is helpful if you already know to which load balancer you will add rules in a moment that
     *            will forward to this target group. Just created, the target group's load balancer ARN in AWS will still
     *            be {@code null}, so cannot be discovered.
     * @param vpcId if {@code null}, the {@code region}'s default VPC will be used
     */
    TargetGroup<ShardingKey> createTargetGroup(Region region, String targetGroupName, int port,
            String healthCheckPath, int healthCheckPort, String loadBalancerArn, String vpcId);
    /**
     * Copies a target group from an existing target group. The name gets extended with {@code suffix}
     * @param parent 
     *          target group to copy from
     * @param suffix
     *          suffix to append to the parent's name for the name of the new created target group
     * @return
     *          newly created target group.
     */
    TargetGroup<ShardingKey> copyTargetGroup(TargetGroup<ShardingKey> parent, String suffix);

    default TargetGroup<ShardingKey> getTargetGroup(Region region, String targetGroupName, String targetGroupArn,
            String loadBalancerArn, ProtocolEnum protocol, Integer port, ProtocolEnum healthCheckProtocol,
            Integer healthCheckPort, String healthCheckPath) {
        return new AwsTargetGroupImpl<>(this, region, targetGroupName, targetGroupArn,
                loadBalancerArn, protocol, port, healthCheckProtocol, healthCheckPort,
                healthCheckPath);
    }

    /**
     * @return {@code null} if a target group by the name specified isn't found in the {@code region}
     */
    software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroup(Region region, String targetGroupName);

    /**
     * @return {@code null} if a target group by the ARN specified isn't found in the {@code region}
     */
    software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroupByArn(Region region, String targetGroupArn);

    Map<AwsInstance<ShardingKey>, TargetHealth> getTargetHealthDescriptions(TargetGroup<ShardingKey> targetGroup);

    <SK> void deleteTargetGroup(TargetGroup<SK> targetGroup);

    Iterable<Rule> getLoadBalancerListenerRules(Listener loadBalancerListener, Region region);
    /**
     * Modifies an existing rule that is identified by the passed {@code rule}'s ARN. Only the conditions are modified and nothing
     * else gets touched.
     * 
     * @param region
     *          AWS Region
     * @param rule
     *          The Rule to modify. Only ARN and conditions are necessary.
     * @return
     *          the modified Rule as an Iterable.
     */
    Iterable<Rule> modifyRuleConditions(Region region, Rule rule);
    
    /**
     * Modifies an existing rule that is identified by the passed {@code rule}'s ARN. Only the actions are modified and nothing
     * else gets touched.
     * @param region
     *          AWS Region
     * @param rule
     *          The Rule to modify. Only ARN and actions are necessary.
     * @return
     *          the modified Rule as an Iterable.
     */
    Iterable<Rule> modifyRuleActions(Region region, Rule rule);

    /**
     * Use {@link Rule.Builder} to create {@link Rule} objects you'd like to set for the {@link Listener} passed as parameter.
     * Obviously, there is no need to set the {@link Rule.Builder#ruleArn(String) rule's ARN} as this is created by executing
     * the request.
     * 
     * @return the rule objects, now including the {@link Rule#ruleArn() rule ARNs}, created by this request
     */
    Iterable<Rule> createLoadBalancerListenerRules(Region region, Listener listener, Rule... rulesToAdd);

    void deleteLoadBalancerListenerRules(Region region, Rule... rulesToDelete);

    void updateLoadBalancerListenerRule(Region region, Rule ruleToUpdate);

    void updateLoadBalancerListenerRulePriorities(Region region, Iterable<RulePriorityPair> newRulePriorities);

    void deleteLoadBalancerListener(Region region, Listener listener);

    SecurityGroup getSecurityGroup(String securityGroupId, Region region);

    Optional<SecurityGroup> getSecurityGroupByName(String securityGroupName, Region region);

    void addTargetsToTargetGroup(
            TargetGroup<ShardingKey> targetGroup,
            Iterable<AwsInstance<ShardingKey>> targets);

    void removeTargetsFromTargetGroup(
            TargetGroup<ShardingKey> targetGroup,
            Iterable<AwsInstance<ShardingKey>> targets);

    LoadBalancer getAwsLoadBalancer(String loadBalancerArn, Region region);
    
    // --------------- abstract landscape view --------------
    /**
     * Obtains the reverse proxies, which make up  a reverse proxy cluster, in the given {@code region} that are used to receive (and possibly redirect to HTTPS or
     * forward to a host proxied by the reverse proxy) all HTTP requests and any HTTPS request not handled by a
     * dedicated load balancer rule, such as "cold storage" hostnames that have been archived. May return {@code null}
     * if, in the given {@code region}, no such reverse proxy has been configured / set up yet.
     */
    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> getReverseProxyCluster(Region region);
    
    /**
     * Returns the reverse proxy in the given region, but encapsulated within a ReverseProxyCluster.
     * @return
     */
    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    ReverseProxyCluster<ShardingKey, MetricsT, ProcessT, RotatingFileBasedLog> getCentralReverseProxy(Region region);
    
    /**
     * Each region can have a single load balancer per {@code wildcardDomain} that is the target for any sub-domain of
     * that wildcard domain. For example, there is a dynamic load balancer for "sapsailing.com" that is the default for
     * any sub-domain that does not have a dedicated DNS entry (usually then pointing at a dedicated
     * {@link #getDNSMappedLoadBalancerFor(Region, String) DNS-mapped load balancer}). The wildcard domain is used
     * in the construction of the load balancer's name which has to be unique per region.<p>
     * 
     * This load balancer shall be used for rather short-lived scope mappings and their rules because changes become
     * effective immediately with the change, other than for DNS records, for example, which take a while to propagate
     * through the world-wide DNS infrastructure.
     * 
     * @param wildcardDomain e.g., "sapsailing.com" without leading and without trailing dot
     * 
     */
    ApplicationLoadBalancer<ShardingKey> getNonDNSMappedLoadBalancer(Region region, String wildcardDomain);
    
    /**
     * Creates an application load balancer (ALB) intended to serve requests for dynamically-mapped sub-domains where
     * only a wildcard DNS record exists for the domain. There is a naming rule in place such that
     * {@link #getNonDNSMappedLoadBalancer(Region, String)}, when called with an equal {@code wildcardDomain} and
     * {@code region}, will deliver the load balancer created by this call.
     * 
     * @param securityGroupForVpc
     *            if provided, the security group's VPC association will be used to constrain the subnets for the AZs to
     *            that VPC; if {@code null}, the default subnet for each respective AZ is used
     */
    ApplicationLoadBalancer<ShardingKey> createNonDNSMappedLoadBalancer(Region region, String wildcardDomain,
            SecurityGroup securityGroupForVpc) throws InterruptedException, ExecutionException;

    /**
     * Looks up the hostname in the DNS and assumes to get a load balancer CNAME record for it that exists in the {@code region}
     * specified. The load balancer is then looked up by its {@link ApplicationLoadBalancer#getDNSName() host name}.
     */
    ApplicationLoadBalancer<ShardingKey> getDNSMappedLoadBalancerFor(Region region, String hostname);

    /**
     * Looks up the hostname in the DNS to get the CNAME, then extract the name and region ID from the A-record to which the CNAME
     * points and search for the load balancer there.
     */
    ApplicationLoadBalancer<ShardingKey> getDNSMappedLoadBalancerFor(String hostname);
    
    /**
     * The default MongoDB configuration to connect to. See also {@link #MONGO_DEFAULT_REPLICA_SET_NAME} and
     * {@link #getDatabaseConfigurationForReplicaSet(Region, String)}, but expect that this could also be a
     * standalone MongoDB instance.
     */
    MongoEndpoint getDatabaseConfigurationForDefaultReplicaSet(Region region);
    
    /**
     * Computes the {@link Tags tag value} to add for the {@link #MONGO_REPLICA_SETS_TAG_NAME} tag to represent the
     * configuration of the {@code mongoProcess}. In line with
     * {@link #getDatabaseConfigurationForReplicaSet(Region, String)}, this will encode the replica set name, if any,
     * followed by a colon and the port number into the tag value.
     * 
     * @param tagsToAddTo
     *            must not be {@code null}; the {@link #MONGO_REPLICA_SETS_TAG_NAME} tag will be
     *            {@link Tags#and(String, String) added}.
     * @return the {@code tagsToAddTo} object, for chaining
     */
    Tags getTagForMongoProcess(Tags tagsToAddTo, String replicaSetName, int port);

    /**
     * Searches the region for {@link AwsInstance hosts} that are tagged with the {@link #MONGO_REPLICA_SETS_TAG_NAME}
     * tag such that the {@code mongoReplicaSetName} is part of the host's specification. For each such host found, a
     * {@link MongoProcessInReplicaSet} is constructed and added to the {@link MongoReplicaSet} returned from which the
     * called may obtain the {@link MongoReplicaSet#getInstances() replicas} and from them, e.g., the
     * {@link MongoProcess#getPort() port} and the host name
     * ({@link MongoProcess#getHost()}.{@link AwsInstance#getPrivateAddress() getPrivateAddress()}).
     * 
     * @param mongoReplicaSetName
     *            must not be {@code null}. If you're looking for a standalone {@link MongoProcess}, simply use the
     *            {@link MongoProcessImpl} constructor.
     */
    MongoReplicaSet getDatabaseConfigurationForReplicaSet(com.sap.sse.landscape.Region region, String mongoReplicaSetName);

    MongoReplicaSet getDatabaseConfigurationForReplicaSet(String mongoReplicaSetName, Iterable<Pair<AwsInstance<ShardingKey>, Integer>> hostsAndPortsOfNodes);
    
    MongoProcessImpl getDatabaseConfigurationForSingleNode(AwsInstance<ShardingKey> host, int port);

    Iterable<MongoEndpoint> getMongoEndpoints(Region region);

    Database getDatabase(Region region, String databaseName);

    /**
     * The region to use as the default region for instance creation, DB connectivity, reverse proxy config, ...
     */
    AwsRegion getDefaultRegion();

    /**
     * Looks for a tag with key as specified by {@code tagName} on the {@code host} specified. If found, the tag's value
     * is returned. Otherwise, the {@link Optional} returned {@link Optional#isPresent() is not present}.
     */
    Optional<String> getTag(AwsInstance<ShardingKey> host, String tagName);

    /**
     * Obtains all hosts with a tag named {@code tagName}, regardless the tag's value, and returns them as
     * {@link ApplicationProcessHost}s. Callers have to provide the bi-function that produces instances of the desired
     * {@link ApplicationProcess} subtype for each server directory holding a process installation on that host.
     */
    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>, HostT extends ApplicationProcessHost<ShardingKey, MetricsT, ProcessT>>
    Iterable<HostT> getApplicationProcessHostsByTag(Region region, String tagName, HostSupplier<ShardingKey, HostT> hostSupplier);

    /**
     * Obtains all {@link #getApplicationProcessHostsByTag(Region, String, HostSupplier) hosts} with a tag whose key is
     * specified by {@code tagName} and discovers all application server processes configured on it. These are then
     * grouped by {@link ApplicationProcess#getServerName(Optional, Optional, byte[]) server name}, and using
     * {@link ApplicationProcess#getMasterServerName(Optional)} the master/replica relationships between the processes with equal server
     * name are discovered. From this, an {@link ApplicationReplicaSet} is established per server name.<p>
     * 
     * Should more than one master exist with equal server name, only the newest master is considered.
     * 
     * @param optionalTimeout
     *            an optional timeout for communicating with the application server(s) to try to read the application
     *            configuration; used, e.g., as timeout during establishing SSH connections
     */
    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>,
    HostT extends ApplicationProcessHost<ShardingKey, MetricsT, ProcessT>>
    Iterable<AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT>> getApplicationReplicaSetsByTag(Region region,
            String tagName, HostSupplier<ShardingKey, HostT> hostSupplier,
            Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * Like {@link #getApplicationReplicaSetsByTag(Region, String, HostSupplier, Optional, Optional, byte[])}, only that the tag's
     * value can also be constrained using this method. This way, callers can, e.g., search for a specific replica set as long as
     * the master runs on a dedicated host with only this replica set name in the {@code sailing-analytics-server} tag.<p>
     * 
     * If multiple replica sets matching the tag/value criterion are found, one of them is returned randomly.
     */
    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>,
    HostT extends ApplicationProcessHost<ShardingKey, MetricsT, ProcessT>>
    AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> getApplicationReplicaSetByTagValue(
            Region region, String tagName, String tagValue, HostSupplier<ShardingKey, HostT> hostSupplier,
            Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception;

    /**
     * Obtains session credentials using an MFA token code valid for the user for which this landscape object was authenticated
     * during its creation with an access key ID and a secret. 
     */
    Credentials getMfaSessionCredentials(String nonEmptyMfaTokenCode);

    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    void createLaunchTemplateAndAutoScalingGroup(Region region, String replicaSetName, Optional<Tags> tags,
                    TargetGroup<ShardingKey> targetGroup, String keyName, InstanceType instanceType, String imageId,
                    AwsApplicationConfiguration<ShardingKey, MetricsT, ProcessT> replicaConfiguration, int minReplicas,
                    int maxReplicas, int maxRequestsPerTarget);

    Snapshot getSnapshot(AwsRegion region, String snapshotId);

    /**
     * Tries to find a load balancer by looking up a hostname, assuming to find a CNAME record that maps to
     * a load balancer's host name defined as an A-record in the DNS; from that load balancer's host name
     * (such as {@code "DNSMapped-0-325768077.eu-west-2.elb.amazonaws.com"}) this method will look up the
     * corresponding load balancer, assuming the region name can be obtained from the host name (such as
     * {@code "eu-west-2"} in the example above). If the load balancer is found, it is returned. Otherwise,
     * {@code null} is returned.
     */
    ApplicationLoadBalancer<ShardingKey> getLoadBalancerByHostname(String hostname);

    static String getHostedZoneName(String hostname) {
        return hostname.substring(hostname.indexOf('.')+1);
    }

    CompletableFuture<Iterable<ResourceRecordSet>> getResourceRecordSetsAsync(String hostname);

    /**
     * For an IP address looks for Route53 resource records that hold an A- or AAAA-record pointing
     * to that {@code ipAddress} and returns the record set name as the hostname. This searches
     * the whole of all resource record sets in the AWS account.<p>
     * 
     * Example: asking for "172.31.13.233" may return something like "mongo0.internal.sapsailing.com"<p>
     * 
     * Note that this strips off any trailing "." (dot) from a resource record set name.
     * 
     * @return the fully-qualified resource record set name if found, otherwise {@code null}
     */
    String findHostnamesForIP(String ipAddress);
    
    Iterable<ResourceRecordSet> getResourceRecordSets(String hostname);
    
    DNSCache getNewDNSCache();

    CompletableFuture<Iterable<AutoScalingGroup>> getAutoScalingGroupsAsync(Region region);

    CompletableFuture<Iterable<LaunchTemplate>> getLaunchTemplatesAsync(Region region);

    CompletableFuture<Iterable<LaunchTemplateVersion>> getLaunchTemplateDefaultVersionsAsync(Region region);

    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>>
    AwsApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> getApplicationReplicaSet(Region region, String serverName,
            ProcessT master, Iterable<ProcessT> replicas, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws InterruptedException, ExecutionException, TimeoutException;

    CompletableFuture<Void> removeAutoScalingGroupAndLaunchTemplate(AwsAutoScalingGroup autoScalingGroup);
    
    CompletableFuture<DeleteAutoScalingGroupResponse> removeAutoScalingGroup(AwsAutoScalingGroup autoScalingGroup);
    
    /**
     * updates minimum and desired size to {@code minSize}
     */
    void updateAutoScalingGroupMinSize(AwsAutoScalingGroup autoScalingGroup, int minSize);

    /**
     * @param autoScalingGroups
     *            The launch template version used by {@link AwsApplicationReplicaSet#getAllAutoScalingGroups() all}
     *            auto-scaling groups of the replica set is updated.
     */
    void updateReleaseInAutoScalingGroups(Region region, LaunchTemplate oldLaunchTemplate, Iterable<AwsAutoScalingGroup> autoScalingGroups, String replicaSetName, Release release);

    /**
     * @param autoScalingGroups
     *            The launch template version used by {@link AwsApplicationReplicaSet#getAllAutoScalingGroups() all}
     *            auto-scaling groups of the replica set is updated.
     */
    void updateImageInAutoScalingGroups(Region region, Iterable<AwsAutoScalingGroup> autoScalingGroups, String replicaSetName, AmazonMachineImage<ShardingKey> ami);

    /**
     * @param autoScalingGroups
     *            The launch template version used by {@link AwsApplicationReplicaSet#getAllAutoScalingGroups() all}
     *            auto-scaling groups of the replica set is updated.
     */
    void updateInstanceTypeInAutoScalingGroup(Region region, Iterable<AwsAutoScalingGroup> autoScalingGroups, String replicaSetName, InstanceType instanceType);

    TargetGroup<ShardingKey> createTargetGroupWithoutLoadbalancer(Region region, String targetGroupName, int port, String vpcId);
    
    /**
     * Creates a new auto-scaling group, using an existing one as a template and only deriving a new name for the
     * auto-scaling group based on the {@code shareName}, configuring it to create its instances into
     * {@code targetGroup} instead of the {@code autoScalingParent}'s target group, and optionally adding the
     * {@code tags} to those copied anyhow from the {@code autoScalingParent}. The minimum size is the current size of
     * the {@code autoScalingParent} unless it is less than two; in that case, the new auto-scaling group will be
     * configured with a minimum size of two (see {@link ShardProcedure#DEFAULT_MINIMUM_AUTO_SCALING_GROUP_SIZE}), ensuring
     * availability in case one target fails.
     * 
     * @return the new auto-scaling group's name.
     */
    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>> String createAutoScalingGroupFromExisting(
            AwsAutoScalingGroup autoScalingParent, String shardName, TargetGroup<ShardingKey> targetGroup, int minSize,
            Optional<Tags> tags);

    /**
     * Changes the autoscaling group with {@code autoscalinggroupName} as name in {@code region}. It sets the minSize of
     * this autoscalingGroup to {@link ShardProcedure#DEFAULT_MINIMUM_AUTO_SCALING_GROUP_SIZE}.
     */
    public void resetShardMinAutoscalingGroupSize(String autoscalinggroupName, Region region);

    <MetricsT extends ApplicationProcessMetrics, ProcessT extends AwsApplicationProcess<ShardingKey, MetricsT, ProcessT>> 
    void putScalingPolicy(
            int instanceWarmupTimeInSeconds, String shardname, TargetGroup<ShardingKey> targetgroup, int maxRequestPerTarget, com.sap.sse.landscape.Region region);
    
    Iterable<TagDescription> getTargetGroupTags(String arn, com.sap.sse.landscape.Region region);
    
    /**
     * 
     *  See AWS doc for tag restrictions: 
     *  https://docs.aws.amazon.com/elasticloadbalancing/latest/application/target-group-tags.html
     *   If a resource already has a tag with the same key,
     *   the value gets updated.
     *  
     * @param arn
     *          Target group's ARN to add the tag to. 
     * @param key
     *          Key of the tag. See AWS logs for restrictions. 
     * @param value
     *          value of tag. See AWS logs for restrictions. 
     * @param region
     *          AWS Region of target group
     * @return
     *          Returns the added Tag
     */
    Tags addTargetGroupTag(String arn, String key, String value, com.sap.sse.landscape.Region region);
    
    String getAutoScalingGroupName(String replicaSetName);

    /**
     * If a {@link #sessionToken} was provided to this landscape, use it to create {@link AwsSessionCredentials}; otherwise
     * an {@link AwsBasicCredentials} object will be produced from the {@link #accessKeyId} and the {@link #secretAccessKey}.
     */
    AwsCredentials getCredentials();

    /**
     * Lists the availability zones (AZs) in the {@code region}; if {@code vpcId} is {@link Optional#isPresent() present},
     * it is used to filter for only those AZs that have a subnet configured in the VPC.
     */
    Iterable<AwsAvailabilityZone> getAvailabilityZones(com.sap.sse.landscape.Region region, Optional<String> vpcId);

    /**
     * Adds hosts to an IP-based target group.
     */
    void addIpTargetToTargetGroup(TargetGroup<ShardingKey> targetGroup, Iterable<AwsInstance<ShardingKey>> hosts);
    
    /**
     * Removes hosts from an IP-based target group.
     */
    void removeIpTargetFromTargetGroup(TargetGroup<ShardingKey> targetGroup, Iterable<AwsInstance<ShardingKey>> hosts);
}
