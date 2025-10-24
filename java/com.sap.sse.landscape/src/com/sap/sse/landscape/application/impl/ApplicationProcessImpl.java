package com.sap.sse.landscape.application.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.DefaultProcessConfigurationVariables;
import com.sap.sse.landscape.Host;
import com.sap.sse.landscape.ProcessConfigurationVariable;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.application.ApplicationProcess;
import com.sap.sse.landscape.application.ApplicationProcessMetrics;
import com.sap.sse.landscape.impl.ProcessImpl;
import com.sap.sse.landscape.impl.ReleaseImpl;
import com.sap.sse.landscape.ssh.SshCommandChannel;
import com.sap.sse.shared.util.Wait;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public abstract class ApplicationProcessImpl<ShardingKey, MetricsT extends ApplicationProcessMetrics,
ProcessT extends ApplicationProcess<ShardingKey, MetricsT, ProcessT>>
extends ProcessImpl<RotatingFileBasedLog, MetricsT>
implements ApplicationProcess<ShardingKey, MetricsT, ProcessT> {
    private static final String ENV_SH = "env.sh";
    private static final String VERSION_TXT = "configuration/jetty/version.txt";
    
    /**
     * Absolute path in the file system of the host on which this process is running and that represents
     * this process's working directory. This directory is expected to contain a file named {@link #ENV_SH}
     * whose contents can be obtained using the {@link #getEnvSh(Optional, Optional, byte[])} method.<p>
     * 
     * Under certain circumstances and for specific subclasses the value of this field may start out as {@code null}
     * and may later be determined, e.g., by fetching the server's status document and extracting it from there,
     * just like with the {@link #serverName} field.
     */
    protected String serverDirectory;
    
    protected String serverName;
    
    private Integer telnetPortToOSGiConsole;
    
    public ApplicationProcessImpl(int port, Host host, String serverDirectory) {
        super(port, host);
        this.serverDirectory = serverDirectory;
    }

    public ApplicationProcessImpl(int port, Host host, String serverDirectory, Integer telnetPort, String serverName) {
        this(port, host, serverDirectory);
        this.telnetPortToOSGiConsole = telnetPort;
        this.serverName = serverName;
    }

    @Override
    public int hashCode() {
        return getHost().hashCode() ^ getPort();
    }
    
    @Override
    public boolean equals(Object o) {
        ApplicationProcess<?, ?, ?> other = (ApplicationProcess<?, ?, ?>) o;
        return Util.equalsWithNull(getHost(), other.getHost())
            && getPort() == other.getPort();
    }
    
    /**
     * When this application is {@link #restart(Optional, Optional, byte[]) restarted}, drop into this user first,
     * using {@code su - <username>}, before invoking the {@code ./start} script.
     */
    protected abstract String getDefaultApplicationUsername();
    
    @Override
    public Release getRelease(ReleaseRepository releaseRepository, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        // TODO figure this out using the /gwt/status "health check" REST end point; we need to separate the various parameters in the status output's "buildversion" field into separate fields
        final Pattern pattern = Pattern.compile("^[^-]*-([^ ]*) System:");
        final String versionTxt = getVersionTxt(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        final Release result;
        if (versionTxt == null) {
            result = null;
        } else {
            final Matcher matcher = pattern.matcher(versionTxt);
            if (matcher.find()) {
                result = new ReleaseImpl(matcher.group(1), releaseRepository);
            } else {
                result = null;
            }
        }
        return result;
    }
    
    /**
     * Under the path {@code configuration/jetty/version.txt} each release is expected to present a version string in
     * the format
     * <tt>{commitId}-{releaseName} System: {mongoDbUriWithoutSchemaOrHostPortDBName}-{expeditionPort}-{replicationHost}:{replicationPort}/{replicationChannel} Started: {yyyyMMddHHmm}</tt>.
     * With this it is possible to infer the release that will be run upon the next process start, which is also the one
     * running now if this process is currently running and no other release has been deployed since the process has
     * started.
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     * @return {@code null} in case the connection attempt timed out
     */
    private String getVersionTxt(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return getFileContents(getServerDirectory(optionalTimeout)+"/"+VERSION_TXT, optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }

    @Override
    public void tryShutdown(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws IOException, InterruptedException, JSchException, Exception {
        logger.info("Stopping application process "+this);
        final SshCommandChannel sshChannel = getHost().createRootSshChannel(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        if (sshChannel == null) {
            logger.warning("Couldn't create an SSH connection to "+this+" for shutdown. Assuming it is already shut down.");
        } else {
            sshChannel.runCommandAndReturnStdoutAndLogStderr("cd "+getServerDirectory(optionalTimeout)+"; ./stop", "Shutting down "+this, Level.INFO);
        }
    }
    
    @Override
    public void restart(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws IOException, InterruptedException, JSchException, Exception {
        logger.info("Restarting application process "+this);
        getHost().createRootSshChannel(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase)
            .runCommandAndReturnStdoutAndLogStderr("su -l "+getDefaultApplicationUsername()+" -c \"cd "+getServerDirectory(optionalTimeout)+"; ./stop; ./start\"", "Shutting down "+this, Level.INFO);
    }
    
    @Override
    public int getTelnetPortToOSGiConsole(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        if (telnetPortToOSGiConsole == null) {
            telnetPortToOSGiConsole = Integer.parseInt(getEnvShValueFor(DefaultProcessConfigurationVariables.TELNET_PORT, optionalTimeout,
                optionalKeyName, privateKeyEncryptionPassphrase));
        }
        return telnetPortToOSGiConsole;
    }
    
    @Override
    public int[] getAllTCPPorts(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final int[] superTCPPorts = super.getAllTCPPorts(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        final int[] result = new int[superTCPPorts.length + 1];
        System.arraycopy(superTCPPorts, 0, result, 0, superTCPPorts.length);
        result[result.length-1] = getTelnetPortToOSGiConsole(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        return result;
    }

    /**
     * Obtains the last definition of the process configuration variable specified, or {@code null} if that variable isn't set
     * by evaluating the {@code env.sh} file on the {@link #getHost() host} or the connection to the host timed out.
     */
    @Override
    public String getEnvShValueFor(String variableName, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return getEnvShValueFor(getHost(), getServerDirectory(optionalTimeout), variableName, optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    /**
     * @return {@code null} in case the connection attempt timed out
     */
    protected static String getEnvShValueFor(Host host, String serverDirectory, String variableName,
            Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        final SshCommandChannel sshChannel = host.createRootSshChannel(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        final String result;
        if (sshChannel == null) {
            result = null;
        } else {
            final String variableValue = sshChannel.runCommandAndReturnStdoutAndLogStderr(". "+getEnvShPath(serverDirectory)+">/dev/null 2>/dev/null; "+
                                                    "echo \"${"+variableName+"}\"", /* stderr prefix */ null, /* stderr log level */ null);
            result = variableValue.endsWith("\n") ? variableValue.substring(0, variableValue.length()-1) : variableValue;
        }
        return result;
    }
    
    /**
     * Obtains the last definition of the process configuration variable specified, or {@code null} if that variable cannot be found
     * in the evaluated {@code env.sh} file.
     */
    @Override
    public String getEnvShValueFor(ProcessConfigurationVariable variable, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return getEnvShValueFor(variable.name(), optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }

    @Override
    public String getServerDirectory(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        return serverDirectory;
    }
    
    private static String getEnvShPath(String serverDirectory) {
        return serverDirectory+"/"+ENV_SH;
        
    }
    private String getEnvShPath(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        return getEnvShPath(getServerDirectory(optionalTimeout));
    }

    @Override
    public String getServerName(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        if (serverName == null) {
            serverName = getEnvShValueFor(DefaultProcessConfigurationVariables.SERVER_NAME, optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        }
        return serverName;
    }

    @Override
    public String getEnvSh(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return getFileContents(getEnvShPath(optionalTimeout), optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }

    /**
     * @return {@code null} in case the connection attempt timed out
     */
    protected String getFileContents(String path, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        String result;
        final ChannelSftp sftpChannel = getHost().createRootSftpChannel(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        if (sftpChannel == null) {
            // timeout
            result = null;
        } else {
            try {final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                sftpChannel.connect((int) optionalTimeout.orElse(Duration.NULL).asMillis()); 
                sftpChannel.get(path, bos);
                result = bos.toString();
            } finally {
                sftpChannel.getSession().disconnect();
            }
        }
        return result;
    }
    
    /**
     * No good health check path known for arbitrary process; returning {@code "/"} as a default value.
     */
    @Override
    public String getHealthCheckPath() {
        return "/";
    }
    
    protected JSONObject getReplicationStatus(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        return getReplicationStatus(getReplicationStatusPostUrlAndQuery(optionalTimeout));
    }

    private JSONObject getReplicationStatus(final URL url)
            throws IOException, ClientProtocolException, ParseException {
        final HttpPost postRequest = new HttpPost(url.toString());
        final HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final HttpEntity entity = client.execute(postRequest).getEntity();
        entity.writeTo(bos);
        return (JSONObject) new JSONParser().parse(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()),
                HttpUrlConnectionHelper.getCharsetFromHttpEntity(entity, "UTF-8")));
    }

    @Override
    public String getMasterServerName(Optional<Duration> optionalTimeout) throws ClientProtocolException, IOException, ParseException, InterruptedException {
        final String[] result = new String[1];
        try {
            Wait.wait(()->{
                try {
                    final JSONObject replicationStatus = getReplicationStatus(optionalTimeout);
                    for (final Object replicable : (JSONArray) replicationStatus.get("replicables")) {
                        final JSONObject replicableAsJson = (JSONObject) replicable;
                        if (replicableAsJson.get("replicatedfrom") != null) {
                            final JSONObject replicatedFrom = (JSONObject) replicableAsJson.get("replicatedfrom");
                            final String hostname = replicatedFrom.get("hostname").toString();
                            final int port = ((Number) replicatedFrom.get("port")).intValue();
                            // the bearer token that is good for the connection to this process instance should then also
                            // be recognized as valid on this instance's master and have the READ_REPLICATOR permission for the same server name
                            final JSONObject masterReplicationStatus = getReplicationStatus(optionalTimeout, hostname, port);
                            result[0] = masterReplicationStatus.get("servername").toString();
                            break;
                        }
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }, optionalTimeout, /* sleep between attempts */ Duration.ONE_SECOND.times(5), Level.INFO, "Waiting for master server name of "+this);
        } catch (Exception e) {
            logger.info("Exception while waiting for master server name of "+this+": "+e.getMessage());
        }
        return result[0];
    }

    private JSONObject getReplicationStatus(Optional<Duration> optionalTimeout, String hostname, int port)
            throws ClientProtocolException, IOException, ParseException {
        return getReplicationStatus(getReplicationStatusPostUrlAndQuery(hostname, port));
    }
    
    @Override
    public void stopReplicatingFromMaster(String bearerToken, Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        final URLConnection deregistrationRequestConnection = HttpUrlConnectionHelper
                .redirectConnectionWithBearerToken(getUrl(STOP_REPLICATION_POST_URL_PATH_AND_QUERY, optionalTimeout),
                        /* HTTP method */ "POST", bearerToken);
        StringBuilder uuid = new StringBuilder();
        InputStream content = (InputStream) deregistrationRequestConnection.getContent();
        byte[] buf = new byte[256];
        int read = content.read(buf);
        while (read != -1) {
            uuid.append(new String(buf, 0, read));
            read = content.read(buf);
        }
        content.close();
    }
    
    @Override
    public String toString() {
        return "ApplicationProcessImpl [serverDirectory=" + serverDirectory + ", serverName=" + serverName + ", port=" + getPort() + ", host=" + getHost() + "]";
    }
}