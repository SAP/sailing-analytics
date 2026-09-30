package com.sap.sse.landscape.aws.impl;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.application.Scope;
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
     * five minutes of timeout for most network-related actions
     */
    private static final Optional<Duration> TIMEOUT = Optional.of(Duration.ONE_MINUTE.times(5)); 
    
    /**
     * The configuration directory within the "httpd/config" git repo where files with extension {@link #CONFIG_FILE_EXTENSION} can be placed which
     * a {@code reload} will pick up and evaluate.
     */
    private static final String RELATIVE_CONFIG_PATH = "conf.d";
    
    /**
     * The user which contains the checked-out copy of the httpd configuration git.
     */
    private static final String CONFIG_USER = "httpdConf";
   
    /**
     * The absolute path to the "httpd config" git repo which stores all httpd configuration files.
     */
    private static final String CONFIG_REPO_PATH = "~" + CONFIG_USER + "/checked-out";
    
    /**
     * The branch name that contains the production httpd configuration.
     */
    private static final String CONFIG_REPO_MAIN_BRANCH_NAME = "main";
    
    /**
     * Extension for files in the {@link #CONFIG_PATH} folder that will automatically be picked up when reloading
     * the proxy's configuration.
     */
    private static final String CONFIG_FILE_EXTENSION = ".conf";
    private static final String HOME_REDIRECT_MACRO = "Home";
    private static final String PLAIN_REDIRECT_MACRO = "Plain";
    private static final String EVENT_REDIRECT_MACRO = "Event";
    private static final String SERIES_REDIRECT_MACRO = "Series";
    private static final String HOME_ARCHIVE_REDIRECT_MACRO = "Home-ARCHIVE";
    private static final String EVENT_ARCHIVE_REDIRECT_MACRO = "Event-ARCHIVE";
    private static final String SERIES_ARCHIVE_REDIRECT_MACRO = "Series-ARCHIVE";
    private static final String CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION = "000-macros"+CONFIG_FILE_EXTENSION;
    
    /**
     * Name of the "macro"/variable definition used in the file identified by {@link #CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION}
     * that specifies the internal IP address of the primary ARCHIVE server to use.
     */
    private static final String ARCHIVE_IP = "ARCHIVE_IP";

    /**
     * Name of the "macro"/variable definition used in the file identified by {@link #CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION}
     * that specifies the internal IP address of the fail-over ARCHIVE server to use.
     */
    private static final String ARCHIVE_FAILOVER_IP = "ARCHIVE_FAILOVER_IP";
    
    private final AwsInstance<ShardingKey> host;
    
    public ApacheReverseProxy(AwsLandscape<ShardingKey> landscape, AwsInstance<ShardingKey> host) {
        super(landscape);
        this.host = host;
    }
    
    private String getConfigFileNameForScope(Scope<ShardingKey> scope) {
        return scope.toString() + CONFIG_FILE_EXTENSION;
    }

    /**
     * Appends the {@link #CONFIG_FILE_EXTENSION} to the hostname.
     */
    private String getConfigFileNameForHostname(String hostname) {
        return hostname + CONFIG_FILE_EXTENSION;
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

    @Override
    public void setArchiveAndFailoverIPs(String productionArchiveIP, String failoverArchiveIP, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String absolute000MacrosConfigFilePath = getAbsoluteConfigFilePath(CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION);
        final SshCommandChannel sshChannel = getHost().createRootSshChannel(TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
        String patch000MacrosCommand = "su - " + CONFIG_USER + " -c 'cd " + CONFIG_REPO_PATH + " && git checkout "
                + CONFIG_REPO_MAIN_BRANCH_NAME
                + " && sed -i -e \"s/^Define "+ARCHIVE_IP+" .*$/Define "+ARCHIVE_IP+" "+productionArchiveIP+"/\" -e \"s/^Define "+ARCHIVE_FAILOVER_IP+" .*$/Define "+ARCHIVE_FAILOVER_IP+" "+failoverArchiveIP+"/\" "+absolute000MacrosConfigFilePath
                + " && " + createCommitAndPushString(CONFIG_FILE_FOR_ARCHIVE_AND_FAILOVER_DEFINITION, "Switching to new ARCHIVE server", /* performPush */ true)
                + "'"; // concludes the "su"; re-loading is expected to happen through the post-receive hook triggered by the push
        final String stdout = sshChannel.runCommandAndReturnStdoutAndLogStderr(patch000MacrosCommand, "Standard error from switching to new ARCHIVE server", Level.WARNING);
        logger.info("Stdout from upgrading to new ARCHIVE: "+stdout);
    }

    /**
     * Creates a redirect file and updates the git repo.
     * 
     * @param configFileNameForHostname
     *            The config file to create or edit, with the appropriate extension appended (eg. .conf).
     * @param macroName
     *            The name of the macro to use.
     * @param hostname
     *            The hostname the macro will affect. Typically the same as the file name.
     * @param optionalKeyName
     *            Key name to use for the ssh channel.
     * @param privateKeyEncryptionPassphrase
     *            The passphrase for the passed key.
     * @param doCommit
     *            Boolean indicating whether to commit. True if the changed file should be committed.
     * @param doPush
     *            Boolean indicating whether to push the committed changes. True if the commit should be pushed.
     * @param macroArguments
     *            Optional macro arguments.
     */
    private void setRedirect(String configFileNameForHostname, String macroName, String hostname,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase, boolean doCommit, boolean doPush, String... macroArguments)
            throws Exception {
        String command = "su - " + CONFIG_USER + " -c 'cd " + CONFIG_REPO_PATH + " && git checkout "
                + CONFIG_REPO_MAIN_BRANCH_NAME + " && echo \"Use " + macroName + " " + hostname + " "
                + String.join(" ", macroArguments) + "\" > " + getAbsoluteConfigFilePath(configFileNameForHostname);
        if (doCommit) {
           command = command + " && cd "
            + CONFIG_REPO_PATH + " && " + createCommitAndPushString(configFileNameForHostname,
                    "Set " + configFileNameForHostname + " redirect", doPush);
        }
        command = command + "'; service httpd reload"; // Concludes the su. And reloads as the root user.
        logger.info("Standard output from setting up the re-direct for " + hostname
                + " and reloading the Apache httpd server: "
                + runCommandAndReturnStdoutAndLogStderr(command,
                        "Standard error from setting up the re-direct for " + hostname
                                + " and reloading the Apache httpd server: ",
                        Level.INFO, optionalKeyName, privateKeyEncryptionPassphrase));
    }
    
    /**
     * Overloads {@link #setRedirect(String, String, String, Optional, byte[], boolean, boolean, String...)} and
     * defaults to {@code true} and {@code true} for committing and pushing.
     * 
     * @see #setRedirect(String, String, String, Optional, byte[], boolean, boolean, String...)
     */
    private void setRedirect(String configFileNameForHostname, String macroName, String hostname,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase, String... macroArguments)
            throws Exception {
        setRedirect(configFileNameForHostname, macroName, hostname, optionalKeyName, privateKeyEncryptionPassphrase,
                /* doCommit */ true, /* doPush */ true, macroArguments);
    }
    
    private String runCommandAndReturnStdoutAndLogStderr(String command, String stderrLogPrefix, Level stderrLogLevel,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final SshCommandChannel sshChannel = getHost().createRootSshChannel(TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase);
        final String stdout = sshChannel.runCommandAndReturnStdoutAndLogStderr(command, stderrLogPrefix, stderrLogLevel);
        return stdout;
    }
    
    /**
     * Creates a command, that can be ran on an instance to commit, and optionally push, changes to a file (within a git
     * repository). ASSUMES the command is ran from within the repository.
     * 
     * @param editedFileName
     *            The file name edited, created or deleted to commit. This includes the {@link #CONFIG_FILE_EXTENSION},
     *            but not a path. The method appends the relative path.
     * @param commitMsg
     *            The commit message, without escaped speech marks.
     * @param performPush
     *            Boolean indicating whether to push changes or not. True for performing a push.
     * @return Returns the created command (in String form) to perform a commit and optional push.
     */
    private String createCommitAndPushString(String editedFileName, String commitMsg, boolean performPush) {
        StringBuilder command = new StringBuilder(" git add " + getRelativeConfigFilePath(editedFileName)
                + " && git commit -m " + "\"" + commitMsg + "\""); // space at beginning and after -m, for safety
        if (performPush) {
            command.append(" ; GIT_SSH_COMMAND=\"ssh -o StrictHostKeyChecking=no\" git push origin " + CONFIG_REPO_MAIN_BRANCH_NAME);
        }
        return command.toString();
    }
    
    /**
     * 
     * @param configFileNameForHostname
     *            The name of the file to append to the relative path.
     * @return Returns the relative path. This is the path, within the directory specified by {@link #CONFIG_REPO_PATH},
     *         to where the argument file is or may be.
     */
    private String getRelativeConfigFilePath(String configFileNameForHostname) {
        return RELATIVE_CONFIG_PATH + "/" + configFileNameForHostname;
    }
    
   /**
    * 
    * @param configFileNameForHostname The filename to append to the absolute path.
    * @return Returns the absolute path to the config file passed as an argument (which may be for creation, deletion or just finding the file).
    */
    private String getAbsoluteConfigFilePath(String configFileNameForHostname) {
        return CONFIG_REPO_PATH + "/" + RELATIVE_CONFIG_PATH + "/" + configFileNameForHostname;
    }

    @Override
    public void setScopeRedirect(Scope<ShardingKey> scope, ProcessT applicationReplicaSet) {
        // TODO Implement ApacheReverseProxy.setScopeRedirect(...)
    }

    @Override
    public void setPlainRedirect(String hostname, ProcessT applicationProcess, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String host = applicationProcess.getHost().getPrivateAddress().getHostAddress();
        final int port = applicationProcess.getPort();
        setRedirect(getConfigFileNameForHostname(hostname), PLAIN_REDIRECT_MACRO, hostname, optionalKeyName, privateKeyEncryptionPassphrase, host, ""+port);
    }

    @Override
    public void setHomeRedirect(String hostname, ProcessT applicationProcess, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String host = applicationProcess.getHost().getPrivateAddress().getHostAddress();
        final int port = applicationProcess.getPort();
        setRedirect(getConfigFileNameForHostname(hostname), HOME_REDIRECT_MACRO, hostname, optionalKeyName, privateKeyEncryptionPassphrase, host, ""+port);
    }

    @Override
    public void setEventRedirect(String hostname, ProcessT applicationProcess, UUID eventId, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String host = applicationProcess.getHost().getPrivateAddress().getHostAddress();
        final int port = applicationProcess.getPort();
        setRedirect(getConfigFileNameForHostname(hostname), EVENT_REDIRECT_MACRO, hostname, optionalKeyName, privateKeyEncryptionPassphrase, eventId.toString(), host, ""+port);
    }

    @Override
    public void setEventSeriesRedirect(String hostname, ProcessT applicationProcess,
            UUID leaderboardGroupId, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String host = applicationProcess.getHost().getPrivateAddress().getHostAddress();
        final int port = applicationProcess.getPort();
        setRedirect(getConfigFileNameForHostname(hostname), SERIES_REDIRECT_MACRO, hostname, optionalKeyName, privateKeyEncryptionPassphrase, leaderboardGroupId.toString(), host, ""+port);
    }

    @Override
    public void setHomeArchiveRedirect(String hostname, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(getConfigFileNameForHostname(hostname), HOME_ARCHIVE_REDIRECT_MACRO, hostname, optionalKeyName, privateKeyEncryptionPassphrase);
    }

    @Override
    public void setEventArchiveRedirect(String hostname, UUID eventId, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(getConfigFileNameForHostname(hostname), EVENT_ARCHIVE_REDIRECT_MACRO, hostname, optionalKeyName, privateKeyEncryptionPassphrase, eventId.toString());
    }

    @Override
    public void setEventSeriesArchiveRedirect(String hostname, UUID leaderboardGroupId,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        setRedirect(getConfigFileNameForHostname(hostname), SERIES_ARCHIVE_REDIRECT_MACRO, hostname, optionalKeyName, privateKeyEncryptionPassphrase, leaderboardGroupId.toString());
    }

    @Override
    public void removeRedirect(Scope<ShardingKey> scope, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String configFileName = getConfigFileNameForScope(scope);
        removeRedirect(configFileName, scope.toString(), optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    @Override
    public void removeRedirect(String hostname, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final String configFileName = getConfigFileNameForHostname(hostname);
        removeRedirect(configFileName, hostname, optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    /**
     * @param configFileName The name of the file to remove.
     * @param hostname The hostname which was removed.
     */
    private void removeRedirect(String configFileName, String hostname,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        StringBuilder command = new StringBuilder("su - " + CONFIG_USER + " -c '"); // The Git commit must be ran as the CONFIG_USER.
        command.append("cd ");
        command.append(CONFIG_REPO_PATH);
        command.append(" && git checkout ");
        command.append(CONFIG_REPO_MAIN_BRANCH_NAME);
        command.append(" && rm ");
        command.append(getRelativeConfigFilePath(configFileName));
        command.append("; ");
        command.append(createCommitAndPushString(configFileName, "Removed " + hostname, /* Perform push */ true));
        command.append("'; service httpd reload;"); // ' closes the su. The reload must be run as the root user.
        logger.info("Standard output from removing the re-direct for " + hostname
                + " and reloading the Apache httpd server: "
                + runCommandAndReturnStdoutAndLogStderr(command.toString(),
                        "Standard error from removing the re-direct for " + hostname
                                + " and reloading the Apache httpd server: ",
                        Level.INFO, optionalKeyName, privateKeyEncryptionPassphrase));
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