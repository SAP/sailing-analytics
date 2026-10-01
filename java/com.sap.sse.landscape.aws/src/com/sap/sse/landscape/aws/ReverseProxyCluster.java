package com.sap.sse.landscape.aws;

import java.util.Optional;
import java.util.concurrent.TimeoutException;
import com.sap.sse.landscape.Log;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import software.amazon.awssdk.services.ec2.model.InstanceType;

/**
 * A cluster of {@link ReverseProxy} processes, for scale and/or availability. Can be grown or shrunk by adding or
 * removing {@link ReverseProxy} instances, respectively. All the {@link ReverseProxy} processes share the same
 * configuration. Updating it through this interface ensures the configuration is updated in all participating
 * {@link ReverseProxy} processes, with the proxies reloaded to reflect the changes.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ReverseProxyCluster<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, LogT extends Log>
        extends ReverseProxy<ShardingKey, MetricsT, ProcessT, LogT> {
    /**
     * A reverse proxy may scale out by adding more hosts.
     * 
     * @return at least one host
     */
    Iterable<AwsInstance<ShardingKey>> getHosts();

    /**
     * Add one host of the instance type specified to the availability zone {@code az}. Additionally, it is added to the
     * target groups with {@link AwsLandscapeConstants#ALL_REVERSE_PROXIES}, once it is running, which may be the reason
     * the success message doesn't immediately appear.
     * 
     * @return the host that was added by this request; it will also be part of the response of {@link #getHosts()} now
     */
    AwsInstance<ShardingKey> createHost(String name, InstanceType instanceType, AwsAvailabilityZone az, String keyName)
            throws TimeoutException, Exception;

    /**
     * Adds the existing passed host to the cluster. The cluster though lacks longevity because any changes to the host are not reflected in the cluster.
     */
    void addHost(AwsInstance<ShardingKey> host);

    /**
     * Removes a single host from this reverse proxy, terminating the host. When trying to remove the last remaining
     * host, an {@link IllegalStateException} will be thrown and the method will not complete the request. Consider
     * using {@link #terminate()} to terminate all hosts forming this reverse proxy. The instance is
     * <strong>only</strong> terminated when no longer in the draining state in a target group (typically takes 5
     * minutes, so expect a delay).
     */
    void removeHost(AwsInstance<ShardingKey> host, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception;
}
