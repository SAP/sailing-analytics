package com.sap.sse.landscape.aws;

import java.util.Optional;
import java.util.UUID;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.Log;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.ApplicationReplicaSet;
import com.sap.sse.landscape.application.Scope;

import software.amazon.awssdk.services.ec2.model.InstanceType;

/**
 * Represents an HTTP/HTTPS reverse proxy that is able to manage redirection rule sets for "level 7" routing, primarily
 * based on hostnames but probably also on {@link Scope}s and their identifiers. If this reverse proxy employs several
 * {@link Host}s to provide its service then those are assumed to share a common configuration. This interface grants
 * access to this common configuration.
 * <p>
 * 
 * Each hostname can be mapped to at most one {@link ApplicationReplicaSet application replica set}, and only one
 * redirect strategy can be selected for such a hostname. The reverse proxy will rewrite such requests, including HTTP
 * to HTTPS forwarding and depending on the strategy selected appending a path to a bare URL with no path components
 * that point to a specific landing page.
 * <p>
 * 
 * Generally, setting a redirect for a hostname will replace an already existing mapping for an equal hostname. Clients
 * using this interface don't have to worry about the details of reloading the configuration or restarting the reverse
 * proxy service; calling the methods through this interface will do what it takes.
 * <p>
 * 
 * We think that such reverse proxies should be used as "cold-storage" handlers, mapping scopes and hostnames of content
 * through "default" load balancer rules. Such default rules should forward all requests to a target group that contains
 * the {@link #getHosts() hosts} forming this reverse proxy. By allowing for multiple hosts that all share the same
 * reverse proxy configuration, scalability as well as availability can be addressed.<p>
 * 
 * In the future, support may be added to maintain a set of {@link TargetGroup target groups} forwarding traffic to this
 * reverse proxy as hosts are added to or removed from this reverse proxy installation.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ReverseProxy<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, LogT extends Log> {
    default InstanceType getDefaultInstanceType() {
        return InstanceType.T3_SMALL;
    }

    /**
     * Configures a redirect in this reverse proxy such that requests for it will go to the
     * {@code /index.html} landing page for the application replica set provided.
     */
    void setPlainRedirect(String hostname, ProcessT applicationProcess, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * Configures a redirect in this reverse proxy such that requests for it will go to the
     * {@code /gwt/Home.html} landing page for the application replica set provided.
     */
    void setHomeRedirect(String hostname, ProcessT applicationProcess, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * Configures a redirect in this reverse proxy such that requests for it will go to the
     * event page for the event with ID {@code eventId} that is expected to be hosted by the
     * application replica set provided.
     */
    void setEventRedirect(String hostname, ProcessT applicationProcess, UUID eventId, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * Configures a redirect in this reverse proxy such that requests for it will go to the event series page for the
     * event series identified by the UUID of the leaderboard group that represents the series and which is expected to
     * be hosted by the application replica set provided.
     */
    void setEventSeriesRedirect(String hostname, ProcessT applicationProcess,
            UUID leaderboardGroupId, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * Like {@link #setEventRedirect(String, ApplicationProcess, UUID, Optional, byte[])}, only that the redirect points
     * to the {@code ARCHIVE} server in the landscape.
     */
    void setEventArchiveRedirect(String hostname, UUID eventId, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * Like {@link #setEventSeriesRedirect(String, ApplicationProcess, UUID, Optional, byte[])}, only that the redirect
     * points to the {@code ARCHIVE} server in the landscape.
     */
    void setEventSeriesArchiveRedirect(String hostname, UUID leaderboardGroupId, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * Like {@link #setHomeRedirect(String, ApplicationProcess, Optional, byte[])}, only that the redirect
     * points to the {@code ARCHIVE} server in the landscape.
     */
    void setHomeArchiveRedirect(String hostname, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * Configures a rule for requests for anything from within {@code scope} such that those requests
     * are sent to the {@code applicationReplicaSet}.
     */
    void setScopeRedirect(Scope<ShardingKey> scope, ProcessT applicationProcess) throws Exception;
    
    /**
     * Removes any existing redirect mapping for the {@code hostname} provided. If no such mapping
     * exists, the method does nothing.
     */
    void removeRedirect(String hostname, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    void removeRedirect(Scope<ShardingKey> scope, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * {@link AwsLandscape#terminate(AwsInstance) Terminates} all {@link #getHosts() hosts} that form this reverse
     * proxy.
     */
    void terminate();

    /**
     * Gets the basic path that will indicate the health of a reverse proxy.
     */
    String getHealthCheckPath();
    
    /**
     * Gets the health check path that should be used by a target group, containing only reverse proxies.
     * 
     * @param targetGroupArn
     *            identifies the target group expected to run the health check; this can be used to construct a URL path
     *            and health check that is aware of this target group and may, e.g., take into account the set of its
     *            member instances and their availability zones.
     */
    String getTargetGroupHealthCheckPath(String targetGroupArn);

    /**
     * Fetches the ARCHIVE_IP and ARCHIVE_FAILOVER_IP definitions from the 000-macros.conf file in the reverse proxy and returns
     * them, in this order, as strings.
     */
    Pair<String, String> getArchiveAndFailoverIPs(Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    void setArchiveAndFailoverIPs(String productionArchiveServerInternalIPAddress, String failoverArchiveServerInternalIPAddress,
            Optional<String> ofNullable, byte[] privateKeyEncryptionPassphrase) throws Exception;
}
