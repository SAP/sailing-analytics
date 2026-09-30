package com.sap.sse.landscape.application;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.landscape.AvailabilityZone;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.Process;
import com.sap.sse.landscape.ProcessConfigurationVariable;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.replication.ReplicationServletActions;
import com.sap.sse.shared.util.Wait;
import com.sap.sse.util.HttpUrlConnectionHelper;

/**
 * Equality / hash code is based on the {@link #getHost() host} and {@link #getPort() port}.
 * 
 * @author Axel Uhl (d043530)
 */
public interface ApplicationProcess<ShardingKey, MetricsT extends ApplicationProcessMetrics,
ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
extends Process<RotatingFileBasedLog, MetricsT> {
    
    String HEALTH_CHECK_PATH = "/gwt/status";
    
    static Logger logger = Logger.getLogger(ApplicationProcess.class.getName());
    static String REPLICATION_STATUS_POST_URL_PATH_AND_QUERY = ReplicationServletActions.REPLICATION_SERVLET_BASE_PATH+"?"+ReplicationServletActions.ACTION_PARAMETER_NAME+"="+
                            ReplicationServletActions.Action.STATUS.name();
    static String STOP_REPLICATION_POST_URL_PATH_AND_QUERY = ReplicationServletActions.REPLICATION_SERVLET_BASE_PATH+"?"+ReplicationServletActions.ACTION_PARAMETER_NAME+"="+
                            ReplicationServletActions.Action.STOP_REPLICATING.name();
    
    @FunctionalInterface
    public static interface ApplicationProcessFactory<ShardingKey, MetricsT extends ApplicationProcessMetrics, ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>> {
        ProcessT createApplicationProcess(String instanceId, AvailabilityZone availabilityZone,
                Landscape<ShardingKey> landscape, ProcessFactory<ShardingKey, MetricsT, ProcessT, Host> processFactoryFromHostAndServerDirectory);
    }
    
    /**
     * @param releaseRepository
     *            mandatory parameter required to enable resolving the release and enabling the download of its
     *            artifacts, including its release notes
     * @return the release that this process is currently running
     */
    Release getRelease(ReleaseRepository releaseRepository, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * Tries to shut down an OSGi application server process cleanly by sending the "shutdown" OSGi command to this
     * process's OSGi console using the {@link #getTelnetPortToOSGiConsole() telnet port}. If the instance hasn't
     * terminated after some time, a hard kill command will be used terminate the virtual machine. All this is
     * implemented in the {@code stop} script in the {@link #getServerDirectory(Optional) server's directory}.<p>
     * 
     * The server directory and the {@link #getHost()} are left untouched by this. In particular, a subsequent execution
     * of the {@code start} script in the {@link #getServerDirectory(Optional) server directory} can be expected to start the
     * application process again.
     */
    void tryShutdown(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws IOException, InterruptedException, JSchException, Exception;
    
    int getTelnetPortToOSGiConsole(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * @return the directory as an absolute path that can be used, e.g., in a {@link ChannelSftp} to change directory to
     *         it or to copy files to or read files from there.
     */
    String getServerDirectory(Optional<Duration> optionalTimeout) throws TimeoutException, Exception;
    
    /**
     * The name that is the basis for the user group name; e.g., a server named "my" will by default be owned by a
     * dedicated user group named "my-server". For multi-instance servers, a default setup will use this server name also
     * as the base name of the {@link #getServerDirectory(Optional) server's directory}. Often, it is also used as the name of
     * the {@link Database}, at least when this is a master node, and the name of the RabbitMQ fan-out exchange used
     * for replication.
     */
    String getServerName(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    String getEnvSh(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * The URL path (everything following the hostname and starting with "/" but without any fragment) that can be
     * appended to the protocol, hostname and port specification in order to produce a full health check URL. The
     * health check URL, when connected to, is expected to return a 200 response if the service is healthy, and anything
     * else in case it's not.
     */
    String getHealthCheckPath();

    /**
     * For this application process finds out whether it is replicating from another application process which then is
     * the master. Note that the master server name may differ from this application process's
     * {@link #getServerName(Optional, Optional, byte[]) server name}, e.g., if this application process is a master
     * with regards to the actual application content, but a replica with regards to, say, security.
     */
    String getMasterServerName(Optional<Duration> optionalTimeout) throws Exception;

    /**
     * Obtains the last definition of the process configuration variable specified, or {@code null} if that variable cannot be found
     * in the evaluated {@code env.sh} file.
     */
    String getEnvShValueFor(ProcessConfigurationVariable variable, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception;

    /**
     * Obtains the last definition of the process configuration variable specified, or {@code null} if that variable isn't set
     * by evaluating the {@code env.sh} file on the {@link #getHost() host}.
     */
    String getEnvShValueFor(String variableName, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception;

    /**
     * Tells whether this process is ready to accept requests. Use this for a health check in a target group
     * that decides whether traffic will be sent to this process. {@link #isReady(Optional<Duration>)} implies {@link #isAlive(Optional)}.
     */
    default boolean isReady(Optional<Duration> optionalTimeout) throws IOException {
        try {
            final HttpURLConnection connection = (HttpURLConnection) HttpUrlConnectionHelper.redirectConnection(getHealthCheckUrl(optionalTimeout));
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            logger.info("Ready-check failed for "+this+": "+e.getMessage());
            return false;
        }
    }

    default URL getHealthCheckUrl(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        return getUrl(getHealthCheckPath(), optionalTimeout);
    }
    
    default URL getReplicationStatusPostUrlAndQuery(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        return getUrl(REPLICATION_STATUS_POST_URL_PATH_AND_QUERY, optionalTimeout);
    }
    
    default URL getReplicationStatusPostUrlAndQuery(String hostname, int port) throws MalformedURLException {
        return new URL(port==443 ? "https" : "http", hostname, port, REPLICATION_STATUS_POST_URL_PATH_AND_QUERY);
    }
    
    /**
     * Uses the {@code /replication} end point on the replica to request stopping the replication from master for
     * all replicables running in this process. To authenticate the request, the {@code bearerToken} is used in
     * the {@code Authorization:} header.
     */
    void stopReplicatingFromMaster(String bearerToken, Optional<Duration> optionalTimeout) throws MalformedURLException, IOException, TimeoutException, Exception;
    
    default URL getUrl(String pathAndQuery, Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        final int port = getPort();
        return new URL(port==443 ? "https" : "http",
                Wait.wait(()->getHost().getPublicAddress(optionalTimeout),
                        publicAddress->publicAddress != null, /* retryOnException */ true,
                        optionalTimeout, /* sleep duration between attempts */ Duration.ONE_SECOND.times(10),
                        Level.INFO, "Waiting for non-null public address").getCanonicalHostName(), port, pathAndQuery);
    }
    
    default boolean waitUntilReady(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        return Wait.wait(()->isReady(optionalTimeout), optionalTimeout, Duration.ONE_SECOND.times(5), Level.INFO, ""+this+" not yet ready");
    }

    default boolean waitUntilAlive(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        return Wait.wait(()->isAlive(optionalTimeout), success->success, /* retryOnException */ true,
                optionalTimeout, Duration.ONE_SECOND.times(5), Level.INFO, ""+this+" not yet alive");
    }

    Release getVersion(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    TimePoint getStartTimePoint(Optional<Duration> optionalTimeout) throws Exception;

    /**
     * Executes a {@code ./stop; ./start} sequence which waits for the graceful stopping of the process, then starts it again.
     */
    void restart(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception;
}
