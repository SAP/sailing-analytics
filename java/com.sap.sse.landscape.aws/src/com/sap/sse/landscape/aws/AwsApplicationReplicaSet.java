package com.sap.sse.landscape.aws;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.sap.sse.ServerInfo;
import com.sap.sse.common.Duration;
import com.sap.sse.landscape.Process;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.ApplicationReplicaSet;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.route53.model.RRType;
import software.amazon.awssdk.services.route53.model.ResourceRecordSet;

/**
 * Represents a cluster of server processes all running under the same "server name" and reachable from the outside through
 * a single {@link #getHostname() host name}. They all reside in the same {@link Region}. (Cross-region replication will be
 * a future topic which will lead to multiple load balancers being responsible, one per region, and with only one master.)<p>
 * 
 * The default configuration is such that a 
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <ShardingKey>
 * @param <MetricsT>
 * @param <ProcessT>
 */
public interface AwsApplicationReplicaSet<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
extends ApplicationReplicaSet<ShardingKey, MetricsT, ProcessT> {
    /**
     * @return may return {@code null} in case this application replica set is not managed by a load balancer, such as
     *         is currently the case for the "ARCHIVE" server(s) which is targeted through a reverse proxy.
     */
    ApplicationLoadBalancer<ShardingKey> getLoadBalancer() throws InterruptedException, ExecutionException;
    
    TargetGroup<ShardingKey> getMasterTargetGroup() throws InterruptedException, ExecutionException;
    
    TargetGroup<ShardingKey> getPublicTargetGroup() throws InterruptedException, ExecutionException;
    
    /**
     * Identifies the DNS hosted zone that hosts the DNS record for {@link #getHostname()}. The resource record set name
     * may either be a wildcard record such as {@code *.sapsailing.com} or the fully-qualified hostname which then is expected
     * to match up with {@link #getHostname()}. See also {@link #getResourceRecordSet}.
     */
    String getHostedZoneId() throws InterruptedException, ExecutionException;
    
    /**
     * The DNS entry in the Route53 hosted zone identified by {@link #getHostedZoneId()} that maps to the {@link #getLoadBalancer()}(s)
     * responsible for this application replica set. The {@link ResourceRecord}s are expected to be of type {@link RRType#CNAME}, providing
     * an alias to the load balancer's {@link ApplicationLoadBalancer#getDNSName() DNS name}.<p>
     * 
     * Should multi-region support be added in the future, the resulting resource record set can be expected to hold a {@link ResourceRecord}
     * for the load balancer in each region that manages this application replica set in that region.
     */
    ResourceRecordSet getResourceRecordSet() throws InterruptedException, ExecutionException;
    
    /**
     * @return the {@link ApplicationLoadBalancer#getRules() rules} from the {@link #getLoadBalancer() load balancer}
     *         that react to this application replica set's {@link #getHostname() hostname}.
     */
    Iterable<Rule> getLoadBalancerRules() throws InterruptedException, ExecutionException;
    
    /**
     * The rule that handles the "/" path and redirects users to a specific target path, such as to a specific event's
     * landing page, or the general "/gwt/Home.html" entry point.
     */
    Rule getDefaultRedirectRule() throws InterruptedException, ExecutionException;
    
    /**
     * The auto-scaling group is responsible for scaling the set of replicas registered with the
     * {@link #getPublicTargetGroup() public target group}. This is optional, so {@code null} may
     * be returned.
     */
    AwsAutoScalingGroup getAutoScalingGroup() throws InterruptedException, ExecutionException;
    
    /**
     * Checks whether the {@code host} is eligible for accepting a deployment of a process that belongs to this
     * application replica set, either its master or a replica. In order to be eligible, the host must
     * <ul>
     * <li>not run any other application process on the {@link #getPort() HTTP port} used by this application replica set</li>
     * <li>not have a process already deployed under the same {@link #getServerName() server name} used by this replica set</li>
     * <li>not be managed by an auto-scaling group</li>
     * </ul>
     */
    boolean isEligibleForDeployment(ApplicationProcessHost<ShardingKey, MetricsT, ProcessT> host,
            Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * Any {@link #getReplicas() replica in this replica set} that is not running on a host
     * {@link AwsInstance#isManagedByAutoScalingGroup(AwsAutoScalingGroup) managed} by this replica set's
     * {@link #getAutoScalingGroup() auto-scaling group} (in case there is no auto-scaling group defined for this
     * replica set, all replicas) will be stopped, and if it was the last application process on its host, the host
     * will be terminated.
     */
    void stopAllUnmanagedReplicas(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * In addition to what the super-interface does (checking the {@link #getMaster() master's} {@link Process#getPort()
     * port}), in case a master instance currently cannot be found, this implementation can resort to checking the
     * {@link #getMasterTargetGroup() master target group's} {@link TargetGroup#getPort() port} setting.
     */
    @Override
    default int getPort() throws InterruptedException, ExecutionException {
        final int port;
        if (getMaster() != null) {
            port = getMaster().getPort();
        } else {
            port = getMasterTargetGroup().getPort();
        }
        return port;
    }

    /**
     * For all existing replicas, a {@code ./stop; ./start} sequence is executed which will in particular have the effect that
     * all replicas will sync up to the current master and listen to replicated operations. This is useful, e.g., after telling
     * all replicas to stop replicating because the master will temporarily become unavailable for a version upgrade or a
     * scaling operation. After running {@code ./stop; ./start} on a replica, the method waits until that replica has become
     * ready again before proceeding with the stop/start cycle for the next replica. This way, availability of a set of replicas
     * is temporarily reduced by no more than one replica at a time.
     */
    void restartAllReplicas(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * @return {@code true} if this replica set is the one that this method is being run on. See
     * {@link ServerInfo}.
     */
    boolean isLocalReplicaSet();
    
    public String getShardName();
    
    public Map<Integer, AwsShard<ShardingKey>> getShards();
}
