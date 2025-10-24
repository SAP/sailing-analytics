package com.sap.sailing.landscape.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.sap.sailing.landscape.SailingAnalyticsHost;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.landscape.SailingReleaseRepository;
import com.sap.sailing.landscape.common.RemoteServiceMappingConstants;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.impl.BearerTokenReplicationCredentials;
import com.sap.sailing.landscape.impl.SailingAnalyticsHostImpl;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sailing.landscape.procedures.DeployProcessOnMultiServer;
import com.sap.sailing.landscape.procedures.SailingAnalyticsApplicationConfiguration;
import com.sap.sailing.landscape.procedures.SailingAnalyticsMasterConfiguration;
import com.sap.sailing.landscape.procedures.SailingProcessConfigurationVariables;
import com.sap.sailing.landscape.procedures.StartMultiServer;
import com.sap.sailing.landscape.procedures.StartSailingAnalyticsHost;
import com.sap.sailing.landscape.procedures.StartSailingAnalyticsMasterHost;
import com.sap.sailing.landscape.procedures.UpgradeAmi;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.InboundReplicationConfiguration;
import com.sap.sse.landscape.application.ApplicationReplicaSet;
import com.sap.sse.landscape.application.ProcessFactory;
import com.sap.sse.landscape.aws.AmazonMachineImage;
import com.sap.sse.landscape.aws.ApplicationProcessHost;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.HostSupplier;
import com.sap.sse.landscape.aws.Tags;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.orchestration.CreateDynamicLoadBalancerMapping;
import com.sap.sse.landscape.aws.orchestration.StartMongoDBServer;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.landscape.mongodb.MongoProcess;
import com.sap.sse.landscape.mongodb.MongoReplicaSet;
import com.sap.sse.landscape.ssh.SSHKeyPair;
import com.sap.sse.landscape.ssh.SshCommandChannel;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.route53.model.RRType;

/**
 * Tests for the AWS SDK landscape wrapper in bundle {@code com.sap.sse.landscape.aws}. To run these tests
 * successfully it is necessary to have valid AWS credentials for region {@code EU_WEST_2} that allow the
 * AWS user account to create keys and launch instances, etc. These are to be provided as explained
 * in the documentation of {@link AwsLandscape#obtain(String)}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class TestProcedures {
    private static final Logger logger = Logger.getLogger(TestProcedures.class.getName());
    private static final Optional<Duration> optionalTimeout = Optional.of(Duration.ONE_MINUTE.times(10));
    private AwsLandscape<String> landscape;
    private AwsRegion region;
    private final static String MAIL_SMTP_PASSWORD = "mail.smtp.password";
    private final static String SECURITY_SERVICE_REPLICATION_BEARER_TOKEN = "security.service.replication.bearer.token";
    private String securityServiceReplicationBearerToken;
    private String mailSmtpPassword;
    
    /**
     * Used for the symmetric encryption / decryption of private SSH keys. See also
     * {@link #getDecryptedPrivateKey(SSHKeyPair, byte[])}.
     */
    private byte[] privateKeyEncryptionPassphrase;
    
    @BeforeEach
    public void setUp() {
        privateKeyEncryptionPassphrase = ("awptyf87l"+"097384sf;,57").getBytes();
        landscape = AwsLandscape.obtain(RemoteServiceMappingConstants.pathPrefixForShardingKey);
        region = new AwsRegion(Region.EU_WEST_2, landscape);
        securityServiceReplicationBearerToken = System.getProperty(SECURITY_SERVICE_REPLICATION_BEARER_TOKEN);
        mailSmtpPassword = System.getProperty(MAIL_SMTP_PASSWORD);
    }
    
    @Test
    public void testGetImageTypes() {
        final Iterable<String> imageTypes = landscape.getMachineImageTypes(region);
        assertTrue(Util.contains(imageTypes, SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING));
        assertTrue(Util.contains(imageTypes, "mongodb-server"));
    }
    
    @Test
    public void testGetMongoEndpoints() {
        final Iterable<MongoEndpoint> mongoEndpoints = landscape.getMongoEndpoints(new AwsRegion(Region.EU_WEST_1, landscape));
        assertTrue(!Util.isEmpty(Util.filter(mongoEndpoints, mongoEndpoint->
            (mongoEndpoint instanceof MongoReplicaSet &&
             ((MongoReplicaSet) mongoEndpoint).getName().equals("live") &&
             Util.size(((MongoReplicaSet) mongoEndpoint).getInstances()) == 3))));
        assertTrue(!Util.isEmpty(Util.filter(mongoEndpoints, mongoEndpoint->
        (mongoEndpoint instanceof MongoReplicaSet &&
             ((MongoReplicaSet) mongoEndpoint).getName().equals("archive") &&
             Util.size(((MongoReplicaSet) mongoEndpoint).getInstances()) == 1)))); // unless archive restart is ongoing...
        assertTrue(!Util.isEmpty(Util.filter(mongoEndpoints, mongoEndpoint->
            (mongoEndpoint instanceof MongoProcess &&
             ((MongoProcess) mongoEndpoint).getPort() == 10202))));
    }
    
    @Test
    public void testStartupEmptyMultiServerAndDeployAnotherProcess() throws Exception {
        final String keyName = "MyKey-"+UUID.randomUUID();
        landscape.createKeyPair(region, keyName, privateKeyEncryptionPassphrase);
        final StartMultiServer.Builder<?, String> builder = StartMultiServer.builder();
        final String sailingAnalyticsServerTag = SharedLandscapeConstants.SAILING_ANALYTICS_APPLICATION_HOST_TAG;
        final StartMultiServer<String> startEmptyMultiServer = builder
              .setLandscape(landscape)
              .setKeyName(keyName)
              .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase)
              .setTags(Tags.with(sailingAnalyticsServerTag, ""))
              .setOptionalTimeout(optionalTimeout)
              .build();
        try {
            // this is expected to have connected to the default "live" replica set.
            startEmptyMultiServer.run();
            final SailingAnalyticsHost<String> host = startEmptyMultiServer.getHost();
            final SshCommandChannel sshChannel = host.createRootSshChannel(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase);
            final String result = sshChannel.runCommandAndReturnStdoutAndLogStderr("ls "+ApplicationProcessHost.DEFAULT_SERVERS_PATH, /* stderr prefix */ null, /* stderr log level */ null);
            assertTrue(result.isEmpty());
            final HttpURLConnection connection = (HttpURLConnection) new URL("http", host.getPublicAddress().getCanonicalHostName(), 80, "").openConnection();
            assertTrue(connection.getHeaderField("Server").startsWith("Apache"));
            connection.disconnect();
            final String aServerName = "a";
            SailingAnalyticsProcess<String> processA = launchMasterOnMultiServer(host, aServerName);
            final String bServerName = "b";
            SailingAnalyticsProcess<String> processB = launchMasterOnMultiServer(host, bServerName);
            assertTrue(processA.waitUntilReady(optionalTimeout));
            assertTrue(processB.waitUntilReady(optionalTimeout));
            assertEquals(new HashSet<>(Arrays.asList(SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_PORT, SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_PORT+1)),
                    new HashSet<>(Arrays.asList(processA.getPort(), processB.getPort())));
            assertEquals(new HashSet<>(Arrays.asList(SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_TELNET_PORT, SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_TELNET_PORT+1)),
                    new HashSet<>(Arrays.asList(processA.getTelnetPortToOSGiConsole(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase), processB.getTelnetPortToOSGiConsole(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase))));
            assertEquals(new HashSet<>(Arrays.asList(SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_EXPEDITION_PORT, SailingAnalyticsApplicationConfiguration.Builder.DEFAULT_EXPEDITION_PORT+1)),
                    new HashSet<>(Arrays.asList(processA.getExpeditionUdpPort(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase), processB.getExpeditionUdpPort(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase))));
            final SshCommandChannel curlChannel = host.createRootSshChannel(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase);
            final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            curlChannel.sendCommandLineSynchronously(
                    "curl -k -i -H \"Host: b.sapsailing.com\" \"https://127.0.0.1\"", stderr);
            final String curlOutput = curlChannel.getStreamContentsAsString();
            assertTrue(curlOutput.matches("(?ms).* 302 Found$.*"));
            assertTrue(curlOutput.replaceAll("\r", "").matches("(?ms).*^Location: https://b.sapsailing.com/gwt/Home.html$.*"));
            // Now check if the landscape can find this "sailing-analytics-server" in the region and determine which applications it has running:
            ProcessFactory<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>, SailingAnalyticsHost<String>> processFactoryFromHostAndServerDirectory =
                    (theHost, thePort, dir, telnetPort, serverName, additionalProperties)->{
                        try {
                            final Number expeditionUdpPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.EXPEDITION_PORT.name());
                            final Number igtimiRiotPort = (Number) additionalProperties.get(SailingProcessConfigurationVariables.IGTIMI_RIOT_PORT.name());
                            return new SailingAnalyticsProcessImpl<String>(thePort, theHost, dir, telnetPort, serverName,
                                    expeditionUdpPort == null ? null : expeditionUdpPort.intValue(),
                                    igtimiRiotPort == null ? null : igtimiRiotPort.intValue(), landscape);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
            final HostSupplier<String, SailingAnalyticsHost<String>> hostSupplier = (instanceId, availabilityZone, privateIpAddress, launchTimePoint, landscape)->
                new SailingAnalyticsHostImpl<String, SailingAnalyticsHost<String>>(instanceId, availabilityZone, privateIpAddress,
                        launchTimePoint, landscape, processFactoryFromHostAndServerDirectory);
            final Iterable<AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>> replicaSets =
                    landscape.getApplicationReplicaSetsByTag(region, sailingAnalyticsServerTag, hostSupplier, optionalTimeout, 
                            /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase);
            // expecting to find at least "a" and "b"
            assertTrue(Util.containsAll(Util.map(replicaSets, ApplicationReplicaSet::getName), Arrays.asList(aServerName, bServerName)));
            Util.filter(replicaSets, rs->rs.getName().equals(aServerName) || rs.getName().equals(bServerName)).forEach(rs->assertTrue(Util.isEmpty(rs.getReplicas()) && rs.getMaster() != null));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while trying to create a MongoDB replica", e);
            throw e;
        } finally {
            if (startEmptyMultiServer.getHost() != null) {
                startEmptyMultiServer.getHost().terminate();
            }
            landscape.deleteKeyPair(region, keyName);
        }
    }
    
    private <AppConfigBuilderT extends SailingAnalyticsApplicationConfiguration.Builder<AppConfigBuilderT, SailingAnalyticsApplicationConfiguration<String>, String>,
    MultiServerDeployerBuilderT extends DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String,
    ApplicationProcessHost<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>,
    SailingAnalyticsApplicationConfiguration<String>, AppConfigBuilderT>>
    SailingAnalyticsProcess<String> launchMasterOnMultiServer(SailingAnalyticsHost<String> host, String serverName) throws IOException, InterruptedException, JSchException, SftpException, Exception {
        final AppConfigBuilderT multiServerAppConfigBuilder = (AppConfigBuilderT) SailingAnalyticsApplicationConfiguration.<AppConfigBuilderT, SailingAnalyticsApplicationConfiguration<String>, String>builder();
        final DeployProcessOnMultiServer.Builder<MultiServerDeployerBuilderT, String,
                ApplicationProcessHost<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>,
                SailingAnalyticsApplicationConfiguration<String>, AppConfigBuilderT> multiServerAppDeployerBuilder =
                DeployProcessOnMultiServer.<MultiServerDeployerBuilderT, String,
                        ApplicationProcessHost<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>,
                        SailingAnalyticsApplicationConfiguration<String>, AppConfigBuilderT> builder(multiServerAppConfigBuilder, host);
        multiServerAppDeployerBuilder
            .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase)
            .setOptionalTimeout(optionalTimeout);
        multiServerAppConfigBuilder
            .setServerName(serverName)
            .setRelease(SailingReleaseRepository.INSTANCE.getLatestRelease("bug4811")); // TODO this is the debug config for the current branch bug4811 and its releases
        final DeployProcessOnMultiServer<String, ApplicationProcessHost<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>>,
            SailingAnalyticsApplicationConfiguration<String>, AppConfigBuilderT> deployer = multiServerAppDeployerBuilder.build();
        deployer.run();
        return deployer.getProcess();
    }
    
    @Test
    public void testAddMongoReplica() throws Exception {
        final String keyName = "MyKey-"+UUID.randomUUID();
        landscape.createKeyPair(region, keyName, privateKeyEncryptionPassphrase);
        final StartMongoDBServer.Builder<?, String, MongoProcess> builder = StartMongoDBServer.builder();
        final StartMongoDBServer<String, MongoProcess> startMongoDBServerProcedure = builder
              .setLandscape(landscape)
              .setKeyName(keyName)
              .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase)
              .setOptionalTimeout(optionalTimeout)
              .build();
        try {
            // this is expected to have connected to the default "live" replica set.
            startMongoDBServerProcedure.run();
            final MongoProcess result = startMongoDBServerProcedure.getMongoProcess();
            connectAndWaitForReplicaSet(result, AwsLandscape.MONGO_DEFAULT_REPLICA_SET_NAME);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while trying to create a MongoDB replica", e);
            throw e;
        } finally {
            if (startMongoDBServerProcedure.getHost() != null) {
                startMongoDBServerProcedure.getHost().terminate();
            }
            landscape.deleteKeyPair(region, keyName);
        }
    }
    
    private void connectAndWaitForReplicaSet(MongoProcess mongoProcess, String mongoDefaultReplicaSetName) throws JSchException, IOException, InterruptedException {
        final TimePoint start = TimePoint.now();
        boolean fine = true;
        do {
            try {
                final SshCommandChannel sshChannel = mongoProcess.getHost().createSshChannel("ec2-user", optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase);
                if (sshChannel == null) {
                    logger.info("Timeout trying to connect to "+mongoProcess.getHost());
                    fine = false;
                } else {
                    final String stdout = sshChannel.runCommandAndReturnStdoutAndLogStderr(
                            "i=0; while [ $i -lt $(echo \"rs.status().members.length\" | mongo  2>/dev/null | tail -n +5 | head -n +1) ]; do  echo \"rs.status().members[$i].stateStr\" | mongo  2>/dev/null | tail -n +5 | head -n +1; i=$((i+1)); done",
                            "stderr while trying to fetch replica set members", Level.WARNING);
                    fine = stdout.contains("PRIMARY") && stdout.contains("SECONDARY");
                }
            } catch (Exception e) {
                logger.info("No success (yet) finding replica set "+mongoDefaultReplicaSetName);
                fine = false;
            }
        } while (!fine && start.until(TimePoint.now()).compareTo(optionalTimeout.get()) < 0);
    }

    @Test
    public void testImageUpgrade() throws Exception {
        final String keyName = "MyKey-"+UUID.randomUUID();
        // Comment the following line to get default eu-west-2 test environment; uncomment to upgrade current production images in eu-west-1
        // final AwsRegion region = new AwsRegion(Region.EU_WEST_1);
        landscape.createKeyPair(region, keyName, privateKeyEncryptionPassphrase);
        final com.sap.sailing.landscape.procedures.UpgradeAmi.Builder<?, String, SailingAnalyticsProcess<String>> imageUpgradeProcedureBuilder = UpgradeAmi.builder();
        final UpgradeAmi<String> imageUpgradeProcedure =
                imageUpgradeProcedureBuilder
                    .setLandscape(landscape)
                    .setRegion(region) // only required in case a non-default region is used; see Landscape.getDefaultRegion()
                    .setKeyName(keyName)
                    .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase)
                    .setOptionalTimeout(optionalTimeout)
                    .build();
        try {
            imageUpgradeProcedure.run();
            final AmazonMachineImage<String> upgradedAmi = imageUpgradeProcedure.getUpgradedAmi();
            assertTrue(upgradedAmi.getCreatedAt().until(TimePoint.now()).compareTo(Duration.ONE_MINUTE.times(10)) < 0);
            assertEquals(3, Util.size(upgradedAmi.getBlockDeviceMappings()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception during test", e);
            fail(e.getMessage());
        } finally {
            landscape.deleteKeyPair(region, keyName);
            if (imageUpgradeProcedure.getUpgradedAmi() != null) {
                imageUpgradeProcedure.getUpgradedAmi().delete();
            }
        }
    }
    
    @Test
    public <AppConfigBuilderT extends SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String>,
    StartMasterHostBuilderT extends StartSailingAnalyticsMasterHost.Builder<StartMasterHostBuilderT, String>>
    void testConnectivity() throws Exception {
        final String serverName = "test"+new Random().nextInt();
        final String keyName = "MyKey-"+UUID.randomUUID();
        landscape.createKeyPair(region, keyName, privateKeyEncryptionPassphrase);
        SailingAnalyticsMasterConfiguration.Builder<AppConfigBuilderT, String> applicationConfigurationBuilder = SailingAnalyticsMasterConfiguration.masterBuilder();
        applicationConfigurationBuilder
            .setServerName(serverName)
            .setRelease(SailingReleaseRepository.INSTANCE.getLatestRelease("bug4811")) // TODO this is the debug config for the current branch bug4811 and its releases
            .setCommaSeparatedEmailAddressesToNotifyOfStartup("axel.uhl@sap.com")
            .setInboundReplicationConfiguration(InboundReplicationConfiguration.builder()
                    .setCredentials(new BearerTokenReplicationCredentials(securityServiceReplicationBearerToken))
                    .build())
            .setMailSmtpPassword(mailSmtpPassword);
        final StartMasterHostBuilderT builder = StartSailingAnalyticsMasterHost.masterHostBuilder(applicationConfigurationBuilder);
        final StartSailingAnalyticsHost<String> startSailingAnalyticsMaster = builder
                .setLandscape(landscape)
                .setRegion(region)
                .setInstanceType(InstanceType.T3_LARGE)
                .setKeyName(keyName)
                .setPrivateKeyEncryptionPassphrase(privateKeyEncryptionPassphrase)
                .setTags(Tags.with("Hello", "World"))
                .setOptionalTimeout(optionalTimeout)
                .build();
        startSailingAnalyticsMaster.run();
        final ApplicationProcessHost<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> host = startSailingAnalyticsMaster.getHost();
        try {
            assertNotNull(host);
            final Instance instance = landscape.getInstance(host.getInstanceId(), region);
            boolean foundName = false;
            boolean foundHello = false;
            for (final Tag tag : instance.tags()) {
                if (tag.key().equals("Name") && tag.value().equals("SL "+serverName+" (Master)")) {
                    foundName = true;
                }
                if (tag.key().equals("Hello") && tag.value().equals("World")) {
                    foundHello = true;
                }
            }
            assertTrue(foundName);
            assertTrue(foundHello);
            // check env.sh access
            final SailingAnalyticsProcess<String> process = startSailingAnalyticsMaster.getSailingAnalyticsProcess();
            assertTrue(process.waitUntilReady(optionalTimeout));
            final String envSh = process.getEnvSh(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase);
            assertFalse(envSh.isEmpty());
            assertTrue(envSh.contains("SERVER_NAME=\""+serverName+"\""), "Couldn't find SERVER_NAME=\""+serverName+"\" in env.sh:\n"+envSh);
            assertEquals(14888, process.getTelnetPortToOSGiConsole(optionalTimeout, /* optional SSH key pair name */ Optional.empty(), privateKeyEncryptionPassphrase));
            // Now create an ALB mapping, assuming to create the dynamic ALB:
            final String domain = "wiesen-weg.de";
            final String hostname = serverName+"."+domain;
            CreateDynamicLoadBalancerMapping.Builder<?, ?, String, SailingAnalyticsMetrics,
                    SailingAnalyticsProcess<String>> createAlbProcedureBuilder = CreateDynamicLoadBalancerMapping.builder();
            createAlbProcedureBuilder
                .setProcess(process)
                .setHostname(hostname)
                .setTargetGroupNamePrefix("S-ded-") // TODO when we combine procedures for launching dedicated hosts (StartSailingAnlayticsHost and specializations) then "S-ded-" should be the default; for DeployProcessOnMultiServer, "S-shared-" should be the default
                .setLandscape(landscape);
            optionalTimeout.ifPresent(createAlbProcedureBuilder::setTimeout);
            final CreateDynamicLoadBalancerMapping<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> createAlbProcedure = createAlbProcedureBuilder.build();
            try {
                createAlbProcedure.run();
                // A few validations:
                // Is the process's host part of the public and master target groups?
                assertNotNull(createAlbProcedure.getMasterTargetGroupCreated());
                assertNotNull(createAlbProcedure.getPublicTargetGroupCreated());
                assertTrue(createAlbProcedure.getMasterTargetGroupCreated().getRegisteredTargets().keySet().contains(process.getHost()));
                assertTrue(createAlbProcedure.getPublicTargetGroupCreated().getRegisteredTargets().keySet().contains(process.getHost()));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during test case", e);
            } finally {
                createAlbProcedure.getLoadBalancerUsed().delete();
                landscape.removeDNSRecord(landscape.getDNSHostedZoneId(domain), "*."+domain,
                        RRType.CNAME, createAlbProcedure.getLoadBalancerUsed().getDNSName());
            }
        } finally {
            landscape.terminate(host);
            landscape.deleteKeyPair(region, keyName);
        }
    }

}
