package com.sap.sse.landscape.aws;

import java.util.Map;
import java.util.Optional;

import com.jcraft.jsch.JSchException;
import com.sap.sse.landscape.AvailabilityZone;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.MachineImage;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.SecurityGroup;
import com.sap.sse.landscape.application.ApplicationMasterProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.ApplicationReplicaProcess;
import com.sap.sse.landscape.aws.impl.AmazonMachineImage;
import com.sap.sse.landscape.aws.impl.AwsLandscapeImpl;
import com.sap.sse.landscape.aws.impl.AwsTargetGroupImpl;
import com.sap.sse.landscape.ssh.SSHKeyPair;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetHealth;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.RRType;

/**
 * A simplified view onto the AWS SDK API that is geared towards specific ways and patterns of managing an application
 * and infrastructure landscape. Among others, it uses {@link Ec2Client}, {@link Route53Client},
 * {@link CloudWatchClient} and {@link ElasticLoadBalancingV2Client} to manage the underlying AWS landscape.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <ShardingKey>
 * @param <MetricsT>
 */
public interface AwsLandscape<ShardingKey, MetricsT extends ApplicationProcessMetrics,
MasterProcessT extends ApplicationMasterProcess<ShardingKey, MetricsT, MasterProcessT, ReplicaProcessT>,
ReplicaProcessT extends ApplicationReplicaProcess<ShardingKey, MetricsT, MasterProcessT, ReplicaProcessT>>
extends Landscape<ShardingKey, MetricsT, MasterProcessT, ReplicaProcessT> {
    static String ACCESS_KEY_ID_SYSTEM_PROPERTY_NAME = "com.sap.sse.landscape.aws.accesskeyid";

    static String SECRET_ACCESS_KEY_SYSTEM_PROPERTY_NAME = "com.sap.sse.landscape.aws.secretaccesskey";

    /**
     * Based on system properties for the AWS access key ID and the secret access key (see
     * {@link #ACCESS_KEY_ID_SYSTEM_PROPERTY_NAME} and {@link #SECRET_ACCESS_KEY_SYSTEM_PROPERTY_NAME}), this method
     * returns a landscape object which internally has access to the clients for the underlying AWS landscape, such as
     * an EC2 client, a Route53 client, etc.
     */
    static <ShardingKey, MetricsT extends ApplicationProcessMetrics,
    MasterProcessT extends ApplicationMasterProcess<ShardingKey, MetricsT, MasterProcessT, ReplicaProcessT>,
    ReplicaProcessT extends ApplicationReplicaProcess<ShardingKey, MetricsT, MasterProcessT, ReplicaProcessT>>
    AwsLandscape<ShardingKey, MetricsT, MasterProcessT, ReplicaProcessT> obtain() {
        return new AwsLandscapeImpl<>();
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
    default AwsInstance<ShardingKey, MetricsT> launchHost(
            MachineImage<? extends AwsInstance<ShardingKey, MetricsT>> fromImage, InstanceType instanceType,
            AwsAvailabilityZone az, String keyName, Iterable<SecurityGroup> securityGroups, Optional<Tags> tags, String... userData) {
        return launchHosts(1, fromImage, instanceType, az, keyName, securityGroups, tags, userData).iterator().next();
    }

    /**
     * Launches a number of new {@link Host}s from a given image into the availability zone specified and controls
     * network access to that instance by setting the security groups specified for the resulting host.
     * @param keyName
     *            the SSH key pair name to use when launching; this will grant root access with the corresponding
     *            private key; see also {@link #getKeyPairInfo(Region, String)}
     */
    Iterable<AwsInstance<ShardingKey, MetricsT>> launchHosts(int numberOfHostsToLaunch,
            MachineImage<? extends AwsInstance<ShardingKey, MetricsT>> fromImage, InstanceType instanceType,
            AwsAvailabilityZone az, String keyName, Iterable<SecurityGroup> securityGroups, Optional<Tags> tags,
            String... userData);

    AmazonMachineImage<ShardingKey, MetricsT> getImage(Region region, String imageId);
    
    KeyPairInfo getKeyPairInfo(Region region, String keyName);
    
    void deleteKeyPair(Region region, String keyName);
    
    /**
     * Uploads the public key to AWS under the name "keyName", stores it in this landscape and returns the key pair ID
     */
    String importKeyPair(Region region, byte[] publicKey, byte[] unencryptedPrivateKey, String keyName) throws JSchException;

    void terminate(AwsInstance<ShardingKey, MetricsT> host);

    SSHKeyPair getSSHKeyPair(Region region, String keyName);
    
    byte[] getDecryptedPrivateKey(SSHKeyPair keyPair) throws JSchException;

    void addSSHKeyPair(SSHKeyPair keyPair);

    /**
     * Creates a key pair with the given name in the region specified and obtains the key details and stores them in
     * this landscape persistently, such that {@link #getKeyPairInfo(Region, String)} as well as
     * {@link #getSSHKeyPair(Region, String)} will be able to obtain (information on) the key.
     * 
     * @return the key ID as string, usually starting with the prefix "key-"
     */
    SSHKeyPair createKeyPair(Region region, String keyName) throws JSchException;

    Instance getInstance(String instanceId, Region region);

    /**
     * @param hostname the fully-qualified host name
     */
    ChangeInfo setDNSRecordToHost(String hostedZoneId, String hostname, Host host);

    /**
     * @param hostname the fully-qualified host name
     */
    ChangeInfo setDNSRecordToApplicationLoadBalancer(String hostedZoneId, String hostname, ApplicationLoadBalancer<ShardingKey, MetricsT> alb);

    String getDefaultDNSHostedZoneId();
    
    String getDNSHostedZoneId(String hostedZoneName);

    /**
     * @param hostname the fully-qualified host name
     */
    ChangeInfo setDNSRecordToValue(String hostedZoneId, String hostname, String value);

    /**
     * @param hostname
     *            the fully-qualified host name
     * @param value
     *            the address to which the record to remove did resolve the hostname, e.g., the value passed to the
     *            {@link #setDNSRecordToValue(String, String, String)} earlier
     */
    ChangeInfo removeDNSRecord(String hostedZoneId, String hostname, RRType type, String value);

    /**
     * Removes the A record (IPv4 address) for {@code hostname}
     * 
     * @param hostname
     *            the fully-qualified host name
     * @param value
     *            the address to which the record to remove did resolve the hostname, e.g., the value passed to the
     *            {@link #setDNSRecordToValue(String, String, String)} earlier
     */
    ChangeInfo removeDNSRecord(String hostedZoneId, String hostname, String value);
    
    ChangeInfo getUpdatedChangeInfo(ChangeInfo changeInfo);

    Iterable<ApplicationLoadBalancer<ShardingKey, MetricsT>> getLoadBalancers(Region region);
    
    ApplicationLoadBalancer<ShardingKey, MetricsT> getLoadBalancer(String loadBalancerArn, Region region);

    ApplicationLoadBalancer<ShardingKey, MetricsT> createLoadBalancer(String name, Region region);

    Iterable<Listener> getListeners(ApplicationLoadBalancer<ShardingKey, MetricsT> alb);

    Iterable<AvailabilityZone> getAvailabilityZones(Region awsRegion);

    AwsAvailabilityZone getAvailabilityZoneByName(Region region, String availabilityZoneName);

    /**
     * Deletes this load balancer and all its target groups (the target groups to which this load balancer currently
     * forwards any traffic).
     */
    void deleteLoadBalancer(ApplicationLoadBalancer<ShardingKey, MetricsT> alb);
    
    /**
     * All target groups that have the load balancer identified by the ARN as "their" load balancer which means that
     * this load balancer is forwarding traffic to all those target groups.
     */
    Iterable<TargetGroup<ShardingKey, MetricsT>> getTargetGroupsByLoadBalancerArn(Region region, String loadBalancerArn);

    /**
     * Creates a target group with a default configuration that includes a health check URL. Stickiness is enabled with
     * the default duration of one day. The load balancing algorithm is set to {@code least_outstanding_requests}.
     */
    TargetGroup<ShardingKey, MetricsT> createTargetGroup(Region region, String targetGroupName, int port,
            String healthCheckPath, int healthCheckPort);

    default TargetGroup<ShardingKey, MetricsT> getTargetGroup(Region region, String targetGroupName, String targetGroupArn) {
        return new AwsTargetGroupImpl<>(this, region, targetGroupName, targetGroupArn);
    }

    software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroup(Region region, String targetGroupName);

    software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup getAwsTargetGroupByArn(Region region, String targetGroupArn);

    Map<AwsInstance<ShardingKey, MetricsT>, TargetHealth> getTargetHealthDescriptions(TargetGroup<ShardingKey, MetricsT> targetGroup);

    <SK, MT extends ApplicationProcessMetrics> void deleteTargetGroup(TargetGroup<SK, MT> targetGroup);

    Iterable<Rule> getLoadBalancerListenerRules(Listener loadBalancerListener, Region region);

    /**
     * Use {@link Rule.Builder} to create {@link Rule} objects you'd like to set for the {@link Listener} passed as parameter.
     * Obviously, there is no need to set the {@link Rule.Builder#ruleArn(String) rule's ARN} as this is created by executing
     * the request.
     * 
     * @return the rule objects, now including the {@link Rule#ruleArn() rule ARNs}, created by this request
     */
    Iterable<Rule> createLoadBalancerListenerRules(Region region, Listener listener, Rule... rulesToAdd);

    void deleteLoadBalancerListenerRules(Region region, Rule... rulesToDelete);

    /**
     * Obtains the reverse proxy in the given {@code region} that is used to receive (and possibly redirect to HTTPS or
     * forward to a host proxied by the reverse proxy) all HTTP requests and any HTTPS request not handled by a
     * dedicated load balancer rule, such as "cold storage" hostnames that have been archived. May return {@code null}
     * in case in the given {@code region} no such reverse proxy has been configured / set up yet.
     */
    ReverseProxy<ShardingKey, MetricsT, MasterProcessT, ReplicaProcessT> getCentralReverseProxy(Region region);

    SecurityGroup getSecurityGroup(String securityGroupId, Region region);

    void addTargetsToTargetGroup(
            TargetGroup<ShardingKey, MetricsT> targetGroup,
            Iterable<AwsInstance<ShardingKey, MetricsT>> targets);

    void removeTargetsFromTargetGroup(
            TargetGroup<ShardingKey, MetricsT> targetGroup,
            Iterable<AwsInstance<ShardingKey, MetricsT>> targets);

    LoadBalancer getAwsLoadBalancer(String loadBalancerArn, Region region);
}
