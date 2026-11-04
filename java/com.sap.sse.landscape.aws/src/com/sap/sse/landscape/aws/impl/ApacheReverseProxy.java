package com.sap.sse.landscape.aws.impl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.ssh.SshCommandChannel;

/**
 * An Apache2-based reverse proxy implementation (httpd) that makes specific assumptions about the availability of an
 * {@link AmazonMachineImage} that can be used to launch and configure such a reverse proxy instance on one or more
 * instances running in one or more {@link AwsAvailabilityZone availability zones}.<p>
 * 
 * For each "scope" that has a redirect rule in this reverse proxy, a separate file is maintained under
 * {@code /etc/httpd/conf.d} that is named after the scope, with the usual {@code .conf} suffix such that
 * a {@code systemctl reload httpd} will automatically pick those up.<p>
 * 
 * TODO how do we remember the hosts/instances/nodes/processes that together form this {@link ApacheReverseProxy}? DB Persistence? Tags?
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ApacheReverseProxy<ShardingKey, MetricsT extends ApplicationProcessMetrics,
ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
extends AbstractApacheReverseProxy<ShardingKey, MetricsT, ProcessT>
implements com.sap.sse.landscape.Process<RotatingFileBasedLog, MetricsT> {
    private static final Logger logger = Logger.getLogger(ApacheReverseProxy.class.getName());
    
    /**
     * Extension for files in the {@link #CONFIG_PATH} folder that will automatically be picked up when reloading
     * the proxy's configuration.
     */
    protected static final String CONFIG_FILE_EXTENSION = ".conf";
    protected static final String CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION = "000-macros"+CONFIG_FILE_EXTENSION;
    
    /**
     * The absolute path to the "httpd config" git repo which stores all httpd configuration files, for the "root"
     * user, as found on all reverse proxies
     */
    private static final String ROOT_CONFIG_REPO_PATH = "/etc/httpd";
    
    /**
     * The configuration directory within the "httpd/config" git repo where files with extension {@link #CONFIG_FILE_EXTENSION} can be placed which
     * a {@code reload} will pick up and evaluate.
     */
    protected static final String RELATIVE_CONFIG_PATH = "conf.d";
    
    /**
     * Name of the "macro"/variable definition used in the file identified by {@link #CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION}
     * that specifies the internal IP address of the primary ARCHIVE server to use.
     */
    protected static final String ARCHIVE_IP = "ARCHIVE_IP";

    /**
     * Name of the "macro"/variable definition used in the file identified by {@link #CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION}
     * that specifies the internal IP address of the fail-over ARCHIVE server to use.
     */
    protected static final String ARCHIVE_FAILOVER_IP = "ARCHIVE_FAILOVER_IP";
    
    private final AwsInstance<ShardingKey> host;
    
    public ApacheReverseProxy(AwsLandscape<ShardingKey> landscape, AwsInstance<ShardingKey> host) {
        super(landscape);
        this.host = host;
    }
    
    /**
     * Forces a logrotate on the instance and logs the output.
     * @param optionalKeyName The optional key to use for the ssh connection.
     * @param privateKeyEncryptionPassphrase The password for the key.
     */
    public void rotateLogs(Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String command = "logrotate --force -v /etc/logrotate.d/httpd 2>&1;  echo \"logrotate done\"";
        logger.info("Standard output from forced log rotate on " + this.getHostname() + ": " + runCommandAndReturnStdoutAndLogStderr(command, "Standard error from logrotate ",
                        Level.ALL, optionalKeyName, privateKeyEncryptionPassphrase));
    }
    
    /**
     * @param configFileNameForHostname
     *            The filename to append to the absolute path.
     * @return Returns the absolute path to the config file passed as an argument (which may be for creation, deletion
     *         or just finding the file).
     */
    protected String getAbsoluteConfigFilePath(String configFileNameForHostname) {
        return getConfigRepoPath() + "/" + RELATIVE_CONFIG_PATH + "/" + configFileNameForHostname;
    }
    
    protected String getConfigRepoPath() {
        return ROOT_CONFIG_REPO_PATH;
    }
    
    @Override
    public Pair<String, String> getArchiveAndFailoverIPs(Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String absolute000MacrosConfigFilePath = getAbsoluteConfigFilePath(CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION);
        final String command = "cat "+absolute000MacrosConfigFilePath+" | grep \"^Define "+ARCHIVE_IP+"\" | sed -e 's/^Define "+ARCHIVE_IP+" //'; "
                             + "cat "+absolute000MacrosConfigFilePath+" | grep \"^Define "+ARCHIVE_FAILOVER_IP+"\" | sed -e 's/^Define "+ARCHIVE_FAILOVER_IP+" //'";
        final String[] archiveAndFailoverIPs = runCommandAndReturnStdoutAndLogStderr(command,
                "Standard error from getting "+ARCHIVE_IP+" and "+ARCHIVE_FAILOVER_IP+": ",
                Level.INFO, optionalKeyName, privateKeyEncryptionPassphrase).split("\n");
        return new Pair<>(archiveAndFailoverIPs[0], archiveAndFailoverIPs[1]);
    }

    protected String runCommandAndReturnStdoutAndLogStderr(String command, String stderrLogPrefix, Level stderrLogLevel,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final SshCommandChannel sshChannel = getHost().createRootSshChannel(TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
        final String stdout = sshChannel.runCommandAndReturnStdoutAndLogStderr(command, stderrLogPrefix, stderrLogLevel);
        return stdout;
    }
    
    @Override
    public void terminate() {
        getLandscape().terminate(host);
    }

    @Override
    public int getPort() {
        return 80;
    }

    /**
     * Making things more specific: as we're in the AWS universe here, the {@link Host} returned more specifically is an
     * {@link AwsInstance}.
     */
    @Override
    public AwsInstance<ShardingKey> getHost() {
        return host;
    }

    @Override
    public RotatingFileBasedLog getLog() {
        // TODO Implement Process<LogT,MetricsT>.getLog(...)
        return null;
    }

    @Override
    public MetricsT getMetrics() {
        // TODO Implement Process<LogT,MetricsT>.getMetrics(...)
        return null;
    }

    @Override
    public boolean isReady(Optional<Duration> optionalTimeout) {
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(getPort() == 443 ? "https" : "http",
                    getHost().getPublicAddress(optionalTimeout).getCanonicalHostName(), getPort(), getHealthCheckPath())
                            .openConnection();
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            logger.info("Ready-check failed for "+this+": "+e.getMessage());
            return false;
        }
    }

}