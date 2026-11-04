package com.sap.sse.landscape.aws;

import java.util.Optional;
import java.util.UUID;

import com.sap.sse.landscape.Log;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.Scope;

public interface CentralReverseProxy<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>, LogT extends Log>
        extends ReverseProxy<ShardingKey, MetricsT, ProcessT, LogT> {
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
    
    void setArchiveAndFailoverIPs(String productionArchiveServerInternalIPAddress, String failoverArchiveServerInternalIPAddress,
            Optional<String> ofNullable, byte[] privateKeyEncryptionPassphrase) throws Exception;
}
