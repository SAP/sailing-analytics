package com.sap.sailing.landscape.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.jcraft.jsch.JSchException;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.SailingReleaseRepository;
import com.sap.sailing.landscape.procedures.SailingProcessConfigurationVariables;
import com.sap.sailing.landscape.procedures.StartSailingAnalyticsHost;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.Region;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.MongoUriParser;
import com.sap.sse.landscape.aws.impl.AwsApplicationProcessImpl;
import com.sap.sse.landscape.impl.ReleaseImpl;
import com.sap.sse.landscape.mongodb.Database;
import com.sap.sse.shared.util.Wait;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public class SailingAnalyticsProcessImpl<ShardingKey>
extends AwsApplicationProcessImpl<ShardingKey, SailingAnalyticsMetrics, SailingAnalyticsProcess<ShardingKey>>
implements SailingAnalyticsProcess<ShardingKey> {
    private static final Logger logger = Logger.getLogger(SailingAnalyticsProcessImpl.class.getName());;
    private static final String STATUS_SERVERNAME_PROPERTY_NAME = "servername";
    private static final String STATUS_SERVERDIRECTORY_PROPERTY_NAME = "serverdirectory";
    private static final String STATUS_RELEASE_PROPERTY_NAME = "release";
    private static final String MONGODB_CONFIGURATION_PROPERTY_NAME = "mongoDbConfiguration";
    private Integer expeditionUdpPort;
    private Integer igtimiRiotPort;
    private Release release;
    private TimePoint startTimePoint;
    
    public SailingAnalyticsProcessImpl(int port, SailingAnalyticsHost<ShardingKey> host, String serverDirectory, Integer expeditionUdpPort, Integer igtimiRiotPort, AwsLandscape<ShardingKey> landscape) {
        super(port, host, serverDirectory, landscape);
        this.expeditionUdpPort = expeditionUdpPort;
        this.igtimiRiotPort = igtimiRiotPort;
    }

    public SailingAnalyticsProcessImpl(int port,
            SailingAnalyticsHost<ShardingKey> host,
            String serverDirectory, Integer telnetPort, String serverName, Integer expeditionUdpPort, Integer igtimiRiotPort, AwsLandscape<ShardingKey> landscape) {
        super(port, host, serverDirectory, telnetPort, serverName, landscape);
        this.expeditionUdpPort = expeditionUdpPort;
        this.igtimiRiotPort = igtimiRiotPort;
    }

    @Override
    public String getHealthCheckPath() {
        return HEALTH_CHECK_PATH;
    }
    
    @Override
    protected String getDefaultApplicationUsername() {
        return StartSailingAnalyticsHost.SAILING_USER_NAME;
    }

    private JSONObject getStatus(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        final HttpGet getStatusRequest = new HttpGet(getHealthCheckUrl(optionalTimeout).toString());
        final JSONObject status = Wait.wait(()->{
                    final HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
                    final HttpResponse result = client.execute(getStatusRequest);
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    result.getEntity().writeTo(bos);
                    return (JSONObject) (new JSONParser().parse(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()),
                            HttpUrlConnectionHelper.getCharsetFromHttpEntity(result.getEntity(), "UTF-8"))));
                }, json->json != null, /* retryOnException */ true, optionalTimeout,
                /* sleepBetweenAttempts */ Duration.ONE_SECOND.times(5), Level.INFO, "getStatus() on "+getHost()+":"+getPort());
        updateStartTimePointFromStatus(status);
        updateReleaseFromStatus(status);
        updateServerNameFromStatus(status);
        updateServerDirectoryFromStatus(status);
        return status;
    }

    private boolean updateReleaseFromStatus(JSONObject status) {
        final boolean success;
        if (status.containsKey(STATUS_RELEASE_PROPERTY_NAME)) {
            release = new ReleaseImpl((String) status.get(STATUS_RELEASE_PROPERTY_NAME), SailingReleaseRepository.INSTANCE);
            success = true;
        } else {
            success = false;
        }
        return success;
    }
    
    /**
     * Here we assume that {@code /gwt/status} has a "release" field we can query
     */
    @Override
    public Release getRelease(ReleaseRepository releaseRepository, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        if (release == null) {
            final JSONObject status = getStatus(optionalTimeout);
            if (!updateReleaseFromStatus(status)) {
                // for backward compatibility
                release = super.getRelease(releaseRepository, optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
            }
        }
        return release;
    }
    
    @Override
    public Database getDatabaseConfiguration(Region region, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final JSONObject mongoDBConfiguration = (JSONObject) getStatus(optionalTimeout).get(MONGODB_CONFIGURATION_PROPERTY_NAME);
        return new MongoUriParser<ShardingKey>(getLandscape(), region).parseMongoDBConfigurationFromStatus(mongoDBConfiguration);
    }
    
    private void updateServerNameFromStatus(JSONObject status) {
        serverName = status.get(STATUS_SERVERNAME_PROPERTY_NAME).toString();
    }
    
    private void updateServerDirectoryFromStatus(JSONObject status) {
        if (serverDirectory == null) {
            if (status.containsKey(STATUS_SERVERDIRECTORY_PROPERTY_NAME)) {
                serverDirectory = status.get(STATUS_SERVERDIRECTORY_PROPERTY_NAME).toString();
            } else {
                serverDirectory = ApplicationProcessHost.DEFAULT_SERVERS_PATH + "/" + status.get(STATUS_SERVERNAME_PROPERTY_NAME).toString();
            }
        }
    }
    
    /**
     * For a sailing application process we know that there is a {@code /gwt/status} end point from which much
     * information about server name as well as availability and replication status can be obtained.
     * <p>
     * 
     * This redefinition does not require the {@code optionalKeyName} nor the {@code privateKeyEncryptionPassphrase}.
     * 
     * @param optionalTimeout
     *            used for the HTTP(S) connection to the status servlet
     */
    @Override
    public String getServerName(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws TimeoutException, Exception {
        if (serverName == null) {
            getStatus(optionalTimeout); // triggers updateServerNameFromStatus
        }
        return serverName;
    }
    
    /**
     * For a sailing application process we know that there is a {@code /gwt/status} end point from which information
     * about the server directory can be obtained. Legacy servers, however, may not expose this field in their status
     * document in which case the server directory will default to the
     * {@link ApplicationProcessHost#DEFAULT_SERVERS_PATH default servers directory} to which the
     * {@link #getServerName(Optional, Optional, byte[]) server name} is appended.
     * <p>
     * 
     * This redefinition does not require the {@code optionalKeyName} nor the {@code privateKeyEncryptionPassphrase}.
     * 
     * @param optionalTimeout
     *            used for the HTTP(S) connection to the status servlet
     */
    @Override
    public String getServerDirectory(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        if (serverDirectory == null) {
            getStatus(optionalTimeout); // triggers updateServerNameFromStatus
        }
        return serverDirectory;
    }
    
    @Override
    public Release getVersion(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return getRelease(SailingReleaseRepository.INSTANCE, optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    @Override
    public TimePoint getStartTimePoint(Optional<Duration> optionalTimeout) throws Exception {
        if (startTimePoint == null) {
            getStatus(optionalTimeout); // triggers updateStartTimePointFromStatus(status);
        }
        return startTimePoint;
    }

    private void updateStartTimePointFromStatus(final JSONObject status) throws ParseException {
        final Object startTimeMillis = status.get("start_time_millis");
        if (startTimeMillis == null) {
            // try legacy approach: extract from "buildversion" attribute which has the general format "^.* Started: [0-9]+$"
            // where the "Started" value has format yyyyMMddhhmm, usually provided in UTC
            final String buildversion = (String) status.get("buildversion");
            final Pattern buildversionPattern = Pattern.compile("^.* Started: ([0-9]+)$");
            final Matcher matcher = buildversionPattern.matcher(buildversion);
            if (buildversion != null && matcher.matches()) {
                final String timestamp = matcher.group(1);
                startTimePoint = TimePoint.of(new SimpleDateFormat("yyyyMMddhhmmX").parse(timestamp+"Z"));
            } else {
                startTimePoint = null;
            }
        } else {
            startTimePoint = TimePoint.of(Long.valueOf(startTimeMillis.toString()));
        }
    }

    @Override
    public int getExpeditionUdpPort(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        if (expeditionUdpPort == null) {
            expeditionUdpPort = Integer.parseInt(getEnvShValueFor(SailingProcessConfigurationVariables.EXPEDITION_PORT.name(),
                optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase));
        }
        return expeditionUdpPort;
    }
    
    @Override
    public Integer getIgtimiRiotPort(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        if (igtimiRiotPort == null) {
            final String igtimiRiotPortEnvVar = getEnvShValueFor(SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT.name(),
                    optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
            igtimiRiotPort = Util.hasLength(igtimiRiotPortEnvVar) ? Integer.parseInt(igtimiRiotPortEnvVar) : null;
        }
        return igtimiRiotPort;
    }

    @Override
    public int[] getAllTCPPorts(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final Integer igtimiRiotPort = getIgtimiRiotPort(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        final int[] result;
        if (igtimiRiotPort == null) {
            result = super.getAllTCPPorts(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        } else {
            final int[] superTCPPorts = super.getAllTCPPorts(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
            result = new int[superTCPPorts.length + 1];
            System.arraycopy(superTCPPorts, 0, result, 0, superTCPPorts.length);
            result[result.length-1] = igtimiRiotPort;
        }
        return result;
    }

    @Override
    public int[] getAllUDPPorts(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final int[] superUDPPorts = super.getAllUDPPorts(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        final int[] result = new int[superUDPPorts.length + 1];
        System.arraycopy(superUDPPorts, 0, result, 0, superUDPPorts.length);
        result[result.length-1] = getExpeditionUdpPort(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
        return result;
    }


    @Override
    public void stopAndTerminateIfLast(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) {
        try {
            tryShutdown(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
            logger.info("Removing server directory "+getServerDirectory(Landscape.WAIT_FOR_PROCESS_TIMEOUT)+" of "+this);
            getHost().createRootSshChannel(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase)
                .runCommandAndReturnStdoutAndLogStderr("rm -rf \""+getServerDirectory(Landscape.WAIT_FOR_PROCESS_TIMEOUT)+"\"", "Removing server directory "+getServerDirectory(Landscape.WAIT_FOR_PROCESS_TIMEOUT), Level.INFO);
            final Iterable<SailingAnalyticsProcess<ShardingKey>> applicationProcesses = getHost()
                    .getApplicationProcesses(optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase,
                            /*
                             * Should throw an exception if there was an error for preventing false positives (process
                             * is running, but there was an error) -> Bug5786
                             */ true);
            if (Util.isEmpty(applicationProcesses)) {
                logger.info("No more application processes running on "+getHost()+"; terminating");
                getHost().terminate();
            } else {
                logger.info("There are other application processes deployed on " + getHost() + ": "
                        + Util.joinStrings(", ", applicationProcesses) + ". Leaving " + getHost() + " running.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SailingAnalyticsHost<ShardingKey> getHost() {
        final SailingAnalyticsHost<ShardingKey> result = (SailingAnalyticsHost<ShardingKey>) super.getHost();
        return result;
    }

    @Override
    public void refreshToRelease(Release release, Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws IOException, InterruptedException, JSchException, Exception {
        logger.info("Upgrading process "+this+" to release "+release.getName());
        getHost().createRootSshChannel(Landscape.WAIT_FOR_PROCESS_TIMEOUT, optionalKeyName, privateKeyEncryptionPassphrase)
            .runCommandAndReturnStdoutAndLogStderr("su -l "+StartSailingAnalyticsHost.SAILING_USER_NAME+" -c \""+
                    "cd "+getServerDirectory(Landscape.WAIT_FOR_PROCESS_TIMEOUT).replaceAll("\"", "\\\\\"")+"; "+
                    "./refreshInstance.sh install-release "+release.getName()+" && ./stop && ./start"+
                    "\"", "Refreshing process to release "+release.getName(), Level.INFO);
        this.release = release;
    }
}
