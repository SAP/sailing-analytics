package com.sap.sse.landscape.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingReleaseRepository;
import com.sap.sailing.landscape.SharedLandscapeConstants;
import com.sap.sailing.landscape.impl.SailingAnalyticsProcessImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.RotatingFileBasedLog;
import com.sap.sse.landscape.aws.impl.AwsRegion;
import com.sap.sse.landscape.aws.orchestration.CreateDNSBasedLoadBalancerMapping;
import com.sap.sse.landscape.impl.ReleaseRepositoryImpl;
import com.sap.sse.landscape.mongodb.MongoEndpoint;
import com.sap.sse.landscape.mongodb.impl.DatabaseImpl;
import com.sap.sse.landscape.ssh.SSHKeyPair;
import com.sap.sse.landscape.ssh.SshCommandChannel;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ActionTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.route53.model.ChangeInfo;
import software.amazon.awssdk.services.route53.model.ChangeStatus;
import software.amazon.awssdk.services.route53.model.RRType;

/**
 * Tests for the AWS SDK landscape wrapper in bundle {@code com.sap.sse.landscape.aws}. To run these tests
 * successfully it is necessary to have valid AWS credentials for region {@code EU_WEST_2} that allow the
 * AWS user account to create keys and launch instances, etc. These are to be provided as explained
 * in the documentation of {@link AwsLandscape#obtain()}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ConnectivityTest<ProcessT extends AwsApplicationProcess<String, SailingAnalyticsMetrics, ProcessT>> {
    private static final Logger logger = Logger.getLogger(ConnectivityTest.class.getName());
    private static final Optional<Duration> optionalTimeout = Optional.of(Duration.ONE_MINUTE.times(5));
    private AwsLandscape<String> landscape;
    private AwsRegion region;
    private byte[] keyPass;
    private String AXELS_KEY_PASS;
    
    @Before
    public void setUp() {
        landscape = AwsLandscape.obtain();
        region = new AwsRegion(Region.EU_WEST_2);
        AXELS_KEY_PASS = new String(Base64.getDecoder().decode(System.getProperty("axelskeypassphrase")));
        keyPass = "lkayrelakuesyrlasp8caorewyc".getBytes();
    }
    
    //@Ignore("Fill in key details for the key used to launch the central reverse proxy in the test landscape")
    @Test
    public void readAndStoreSSHKey() throws Exception {
        final String AXELS_KEY_NAME = "Axel";
        final String PATH_TO_YOUR_PRIVATE_KEY = "c:/Users/d043530/.ssh/id_rsa";
        landscape.deleteKeyPair(region, AXELS_KEY_NAME);
        final KeyPair keyPair = KeyPair.load(new JSch(), PATH_TO_YOUR_PRIVATE_KEY, PATH_TO_YOUR_PRIVATE_KEY+".pub");
        final byte[] pubKeyBytes = getPublicKeyBytes(keyPair);
        // WORKS:
        //  1) decrypt private key read from file containing encrypted key
        //  2) obtain bytes of private key unencrypted
        //  3) "load"/create new KeyPair from private key decrypted this way
        //  4) encrypt private key based on KeyPair loaded from decrypted private key
        //  5) "load"/create new KeyPair from key *encrypted* this way
        //  6) decryption of private key works
        final ByteArrayOutputStream privateKey = new ByteArrayOutputStream();
        keyPair.decrypt(AXELS_KEY_PASS.getBytes());
        keyPair.writePrivateKey(privateKey); // writing plain unencrypted private key WORKS 
        final KeyPair keyPairReadFromFileAndDescrypted = KeyPair.load(new JSch(), privateKey.toByteArray(), pubKeyBytes);
        assertFalse(keyPairReadFromFileAndDescrypted.isEncrypted());
        final ByteArrayOutputStream privateKeyEncrypted = new ByteArrayOutputStream();
        keyPairReadFromFileAndDescrypted.writePrivateKey(privateKeyEncrypted, AXELS_KEY_PASS.getBytes());
        final KeyPair toAndFrom = KeyPair.load(new JSch(), privateKeyEncrypted.toByteArray(), pubKeyBytes);
        assertTrue(toAndFrom.isEncrypted());
        assertTrue(toAndFrom.decrypt(AXELS_KEY_PASS.getBytes()));
        assertFalse(toAndFrom.isEncrypted());
        // WORKS:
        //  1) decrypt private key read from file containing encrypted key using getDecryptedPrivateKeyBytes(...)
        //  2) obtain bytes of private key unencrypted
        //  3) "load"/create new KeyPair from private key decrypted this way
        //  4) encrypt private key based on KeyPair loaded from encrypted private key
        //  5) "load"/create new KeyPair from key *encrypted* this way
        //  6) decryption of private key works
        final byte[] privKeyBytesDecrypted = getDecryptedPrivateKeyBytes(keyPair, AXELS_KEY_PASS.getBytes());
        final KeyPair testKeyFromDecryptedBytes = KeyPair.load(new JSch(), privKeyBytesDecrypted, pubKeyBytes);
        assertFalse(testKeyFromDecryptedBytes.isEncrypted());
        final ByteArrayOutputStream encryptedPrivateKeyFromDecrypted = new ByteArrayOutputStream();
        testKeyFromDecryptedBytes.writePrivateKey(encryptedPrivateKeyFromDecrypted, AXELS_KEY_PASS.getBytes());
        final KeyPair testKeyFromEncryptedBytes = KeyPair.load(new JSch(), encryptedPrivateKeyFromDecrypted.toByteArray(), pubKeyBytes);
        assertTrue(testKeyFromEncryptedBytes.isEncrypted());
        assertTrue(testKeyFromEncryptedBytes.decrypt(AXELS_KEY_PASS.getBytes()));
        assertFalse(testKeyFromEncryptedBytes.isEncrypted());
        // BROKEN:
        //  1) decrypt private key read from file containing encrypted key
        //  2) obtain bytes of private key *encrypted* (writePrivateKey with non-null passphrase)
        //  3) "load"/create new KeyPair from *encrypted* private key
        //  4) decryption of private key FAILS
        // We manifest this BROKENNESS here in this test case:
        final byte[] privKeyBytesEncrypted = getPrivateKeyBytes(keyPair, AXELS_KEY_PASS.getBytes());
        final KeyPair testKeyFromBytes = KeyPair.load(new JSch(), privKeyBytesEncrypted, pubKeyBytes);
        assertTrue(testKeyFromBytes.isEncrypted());
        assertFalse(testKeyFromBytes.decrypt(AXELS_KEY_PASS.getBytes())); // THIS IS THE STRANGE CASE; I THINK IT SHOULD HAVE WORKED!
        assertTrue(testKeyFromBytes.isEncrypted()); // AND AS A CONSEQUENCE THE KEY IS STILL ENCRYPTED!
        // In order to use this to import a valid key existing pair we'll import the last valid key, namely testKeyFromEncryptedBytes:
        landscape.importKeyPair(region, pubKeyBytes, encryptedPrivateKeyFromDecrypted.toByteArray(), AXELS_KEY_NAME);
        final SshCommandChannel sshChannel = landscape.getCentralReverseProxy(region).getHosts().iterator().next()
                .createRootSshChannel(optionalTimeout, /* optional SSH key name */ Optional.empty(),
                        AXELS_KEY_PASS.getBytes());
        final String stdout = sshChannel.runCommandAndReturnStdoutAndLogStderr("ls -al", /* stderr prefix */ null, /* stderr log level */ null);
        assertFalse(stdout.isEmpty());
    }

    /**
     * Requires the key that was used for launching the central reverse proxy for the test landscape to be known in the {@link #landscape}.
     */
    @Test
    public void testApacheProxyBasics() throws Exception {
        final ReverseProxyCluster<String, SailingAnalyticsMetrics, ProcessT, RotatingFileBasedLog> proxy = landscape.getCentralReverseProxy(region);
        final String hostname = "kw2021.sapsailing.com";
        final AwsInstance<String> proxyHost = proxy.getHosts().iterator().next();
        final ProcessT process = createApplicationProcess(proxyHost);
        proxy.setEventRedirect(hostname, process, UUID.randomUUID(), /* optional SSH key name */ Optional.empty(), AXELS_KEY_PASS.getBytes());
        final ByteArrayOutputStream configFileContents = new ByteArrayOutputStream();
        final ChannelSftp sftpChannel = proxyHost.createRootSftpChannel(optionalTimeout, /* optional SSH key name */ Optional.empty(), AXELS_KEY_PASS.getBytes());
        sftpChannel.connect((int) optionalTimeout.orElse(Duration.NULL).asMillis());
        final String configFileName = "/etc/httpd/conf.d/"+hostname+".conf";
        sftpChannel.get(configFileName, configFileContents);
        assertTrue(configFileContents.toString().startsWith("Use Event-SSL "+hostname));
        sftpChannel.getSession().disconnect();
        proxy.removeRedirect(hostname, /* optional SSH key name */ Optional.empty(), AXELS_KEY_PASS.getBytes());
        final SshCommandChannel lsSshChannel = proxyHost.createRootSshChannel(optionalTimeout, /* optional SSH key name */ Optional.empty(), AXELS_KEY_PASS.getBytes());
        final String lsOutput = lsSshChannel.runCommandAndReturnStdoutAndLogStderr("ls "+configFileName, /* stderr prefix */ null, /* stderr log level */ null);
        assertTrue(lsOutput.isEmpty());
    }

    protected ProcessT createApplicationProcess(final AwsInstance<String> host) {
        @SuppressWarnings("unchecked")
        final ProcessT process = (ProcessT) new SailingAnalyticsProcessImpl<String>(8888, host, ApplicationProcessHost.DEFAULT_SERVER_PATH, 2010, landscape);
        return process;
    }
    
    @Test
    public void testConnectivity() throws Exception {
        final String TARGET_GROUP_NAME_PREFIX = "S-test-";
        final String hostedZoneName = "wiesen-weg.de";
        final String hostname = "test-"+new Random().nextInt()+"."+hostedZoneName;
        final String keyName = "MyKey-"+UUID.randomUUID();
        createKeyPair(keyName);
        final MongoEndpoint mongoEndpoint = landscape.getMongoEndpoints(region).iterator().next();
        final AwsInstance<String> host = landscape.launchHost(
                getLatestSailingImage(),
                InstanceType.T3_SMALL, landscape.getAvailabilityZoneByName(region, "eu-west-2b"), keyName, Collections.singleton(()->"sg-0b2afd48960251280"),
                Optional.of(Tags.with("Name", "MyHost").and("Hello", "World")),
                "MONGODB_URI=\""+mongoEndpoint.getURI(Optional.of(new DatabaseImpl(mongoEndpoint, "winddbTest")))+"\"",
                "INSTALL_FROM_RELEASE="+SailingReleaseRepository.INSTANCE.getLatestRelease("bug4811").getName()); // TODO this is the development branch/release; switch to master build later
        try {
            assertNotNull(host);
            final Instance instance = landscape.getInstance(host.getInstanceId(), region);
            boolean foundName = false;
            boolean foundHello = false;
            for (final Tag tag : instance.tags()) {
                if (tag.key().equals("Name") && tag.value().equals("MyHost")) {
                    foundName = true;
                }
                if (tag.key().equals("Hello") && tag.value().equals("World")) {
                    foundHello = true;
                }
            }
            assertTrue(foundName);
            assertTrue(foundHello);
            final ProcessT process = createApplicationProcess(host);
            process.waitUntilReady(optionalTimeout);
            // check env.sh access
            final String envSh = process.getEnvSh(optionalTimeout, /* optional SSH key name */ Optional.empty(), keyPass);
            assertFalse(envSh.isEmpty());
            assertTrue(envSh.contains("SERVER_NAME="));
            final Release release = process.getRelease(new ReleaseRepositoryImpl("http://releases.sapsailing.com", "build"), optionalTimeout, /* optional SSH key name */ Optional.empty(), keyPass);
            assertNotNull(release);
            assertEquals(14888, process.getTelnetPortToOSGiConsole(optionalTimeout, /* optional SSH key name */ Optional.empty(), keyPass));
            final AwsLandscape<String> castLandscape = (AwsLandscape<String>) landscape;
            final CreateDNSBasedLoadBalancerMapping.Builder<?, ?, String, SailingAnalyticsMetrics, ProcessT> builder = CreateDNSBasedLoadBalancerMapping.builder();
            builder
                .setProcess(process)
                .setHostname(hostname)
                .setTargetGroupNamePrefix(TARGET_GROUP_NAME_PREFIX)
                .setLandscape(castLandscape);
            optionalTimeout.ifPresent(builder::setTimeout);
            final CreateDNSBasedLoadBalancerMapping<String, SailingAnalyticsMetrics, ProcessT> createDNSBasedLoadBalancerMappingProcedure =
                    builder.build();
            final String wiesenWegId = landscape.getDNSHostedZoneId(hostedZoneName);
            try {
                createDNSBasedLoadBalancerMappingProcedure.run();
                assertNotNull(createDNSBasedLoadBalancerMappingProcedure.getLoadBalancerUsed());
                assertNotNull(createDNSBasedLoadBalancerMappingProcedure.getMasterTargetGroupCreated());
                assertEquals(TARGET_GROUP_NAME_PREFIX+process.getServerName(optionalTimeout, /* optional SSH key name */ Optional.empty(), keyPass), createDNSBasedLoadBalancerMappingProcedure.getPublicTargetGroupCreated().getName());
            } finally {
                if (createDNSBasedLoadBalancerMappingProcedure.getLoadBalancerUsed() != null) {
                    createDNSBasedLoadBalancerMappingProcedure.getLoadBalancerUsed().delete();
                    landscape.removeDNSRecord(wiesenWegId, hostname, RRType.CNAME, createDNSBasedLoadBalancerMappingProcedure.getLoadBalancerUsed().getDNSName());
                }
            }
        } finally {
            landscape.terminate(host);
            landscape.deleteKeyPair(region, keyName);
        }
    }

    private AmazonMachineImage<String> getLatestSailingImage() {
        return landscape.getLatestImageWithTag(region, "image-type", SharedLandscapeConstants.IMAGE_TYPE_TAG_VALUE_SAILING);
    }
    
    @Test
    public void generateSshKeyPair() throws JSchException, FileNotFoundException, IOException {
        final String publicKeyComment = "Test Key";
        final JSch jsch = new JSch();
        final KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
        final String keyFileBaseName = "test_key";
        keyPair.writePrivateKey(keyFileBaseName, keyPass);
        keyPair.writePublicKey(keyFileBaseName+".pub", publicKeyComment);
        final KeyPair keyPairReadFromFile = KeyPair.load(jsch, keyFileBaseName, keyFileBaseName+".pub");
        assertEquals(publicKeyComment, keyPairReadFromFile.getPublicKeyComment());
        new File(keyFileBaseName).delete();
        new File(keyFileBaseName+".pub").delete();
    }
    
    @Test
    public void generatePublicSshKeyFromPrivateSshKey() throws JSchException, FileNotFoundException, IOException {
        final String publicKeyComment = "Test Key";
        final JSch jsch = new JSch();
        final KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
        final String keyFileBaseName = "test_key";
        keyPair.writePrivateKey(keyFileBaseName, keyPass);
        try {
            final KeyPair keyPairReadFromFile = KeyPair.load(jsch, keyFileBaseName, null);
            keyPairReadFromFile.decrypt(keyPass);
            keyPairReadFromFile.writePublicKey(keyFileBaseName+".pub", publicKeyComment);
            assertTrue(new File(keyFileBaseName+".pub").length() > 0);
        } finally {
            new File(keyFileBaseName).delete();
            new File(keyFileBaseName+".pub").delete();
        }
    }
    
    @Test
    public void generateSshKeyPairWithArrays() throws JSchException, FileNotFoundException, IOException {
        final String publicKeyComment = "Test Key";
        final JSch jsch = new JSch();
        final KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
        final ByteArrayOutputStream privateKey = new ByteArrayOutputStream();
        keyPair.writePrivateKey(privateKey, keyPass);
        final ByteArrayOutputStream publicKey = new ByteArrayOutputStream();
        keyPair.writePublicKey(publicKey, publicKeyComment);
        final KeyPair keyPairReadFromFile = KeyPair.load(jsch, privateKey.toByteArray(), publicKey.toByteArray());
        assertEquals(publicKeyComment, keyPairReadFromFile.getPublicKeyComment());
        assertTrue(keyPairReadFromFile.isEncrypted());
        assertTrue(keyPairReadFromFile.decrypt(keyPass));
        assertFalse(keyPairReadFromFile.isEncrypted());
    }
    
    @Test
    public void testImportKey() throws JSchException {
        final String testKeyName = "My Test Key";
        final JSch jsch = new JSch();
        final KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
        final byte[] pubKeyBytes = getPublicKeyBytes(keyPair);
        final byte[] privKeyBytes = getPrivateKeyBytes(keyPair, keyPass);
        final SSHKeyPair key = landscape.importKeyPair(region, pubKeyBytes, privKeyBytes, testKeyName);
        assertTrue(key.getName().equals(testKeyName));
        final KeyPairInfo awsKeyPairInfo = landscape.getKeyPairInfo(region, testKeyName);
        assertNotNull(awsKeyPairInfo);
        landscape.deleteKeyPair(region, testKeyName);
    }

    private byte[] getPublicKeyBytes(final KeyPair keyPair) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        keyPair.writePublicKey(bos, keyPair.getPublicKeyComment());
        final byte[] pubKeyBytes = bos.toByteArray();
        return pubKeyBytes;
    }
    
    private byte[] getDecryptedPrivateKeyBytes(final KeyPair keyPair, final byte[] passphrase) {
        return getPrivateKeyBytes(keyPair, passphrase, null);
    }
    
    private byte[] getPrivateKeyBytes(final KeyPair keyPair, final byte[] passphraseForDecryption, final byte[] passphraseForEncryption) {
        if (!keyPair.decrypt(passphraseForDecryption)) { // need to decrypt before writePrivateKey would work
            throw new IllegalArgumentException("Passphrase didn't unlock private key of key pair "+keyPair);
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        keyPair.writePrivateKey(bos, passphraseForEncryption);
        final byte[] privKeyBytes = bos.toByteArray();
        return privKeyBytes;
    }
    
    private byte[] getPrivateKeyBytes(final KeyPair keyPair, byte[] passphrase) {
        return getPrivateKeyBytes(keyPair, passphrase, passphrase);
    }
    
    @Test
    public void testSshConnectWithCreatedKey() throws Exception {
        final String keyName = "MyKey-"+UUID.randomUUID();
        createKeyPair(keyName);
        testSshConnectWithKey(keyName);
    }

    @Test
    public void testSshConnectWithImportedKey() throws Exception {
        final String keyName = "MyKey-"+UUID.randomUUID();
        final KeyPair keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, 4096);
        landscape.importKeyPair(region, getPublicKeyBytes(keyPair), getPrivateKeyBytes(keyPair, keyPass), keyName);
        testSshConnectWithKey(keyName);
    }

    private void testSshConnectWithKey(final String keyName) throws Exception {
        final AwsInstance<String> host = landscape.launchHost(getLatestSailingImage(),
                InstanceType.T3_SMALL, landscape.getAvailabilityZoneByName(region, "eu-west-2b"), keyName, Collections.singleton(()->"sg-0b2afd48960251280"), /* tags */ Optional.empty());
        try {
            assertNotNull(host);
            logger.info("Created instance with ID "+host.getInstanceId());
            logger.info("Waiting for public IP address...");
            // wait for public IPv4 address to become available:
            InetAddress address = host.getPublicAddress(optionalTimeout);
            assertNotNull(address);
            logger.info("Obtained public IP address "+address);
            SshCommandChannel shellChannel = host.createRootSshChannel(optionalTimeout, /* optional SSH key name */ Optional.empty(), keyPass);
            assertNotNull(shellChannel);
            logger.info("Shell channel connected. Waiting for it to become responsive...");
            final String stdout = shellChannel.runCommandAndReturnStdoutAndLogStderr("pwd", /* stderr prefix */ null, /* stderr log level */ Level.WARNING);
            assertEquals("/root\n", turnAllLineSeparatorsIntoLineFeed(stdout));
            // now try a simple command, checking for the "init" process to be found
            final SshCommandChannel commandChannel = host.createRootSshChannel(Optional.empty(), /* optional SSH key name */ Optional.empty(), keyPass);
            final String processToLookFor = "init";
            final String output = commandChannel.runCommandAndReturnStdoutAndLogStderr("ps axlw | grep "+processToLookFor, /* stderr prefix */ null, /* stderr log level */ null);
            assertTrue(output.contains(processToLookFor));
            assertEquals(0, commandChannel.getExitStatus());
        } finally {
            landscape.terminate(host);
            landscape.deleteKeyPair(region, keyName);
        }
    }

    private String turnAllLineSeparatorsIntoLineFeed(String string) {
        return string.replaceAll("\n\r", "\n").replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }

    private void createKeyPair(final String keyName) throws JSchException {
        final SSHKeyPair sshKeyPair = landscape.createKeyPair(region, keyName, keyPass);
        assertNotNull(sshKeyPair);
        assertEquals(keyName, sshKeyPair.getName());
    }
    
    @Test
    public void testImageDate() throws ParseException {
        final AmazonMachineImage<String> image = landscape.getImage(region, "ami-0c0907685eae2dbab");
        assertEquals(TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz").parse(
                /* November 9, 2020 at 9:37:16 PM UTC+1 */ "2020-11-09T21:37:16+0100")),
                image.getCreatedAt());
    }
    
    @Test
    public void setDNSRecordTest() {
        final String testHostedZoneDnsName = "wiesen-weg.de";
        final String hostname = "my-test-host-"+new Random().nextInt()+"."+testHostedZoneDnsName+".";
        final String ipAddress = "1.2.3.4";
        final String dnsHostedZoneId = landscape.getDNSHostedZoneId(testHostedZoneDnsName);
        try {
            ChangeInfo changeInfo = landscape.setDNSRecordToValue(dnsHostedZoneId, hostname, ipAddress, /* force */ false);
            int attempts = 10;
            while ((changeInfo=landscape.getUpdatedChangeInfo(changeInfo)).status() != ChangeStatus.INSYNC && --attempts > 0) {
                Thread.sleep(10000);
            };
            assertEquals(ChangeStatus.INSYNC, changeInfo.status());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            landscape.removeDNSRecord(dnsHostedZoneId, hostname, ipAddress);
        }
    }
    
    @Test
    public void createEmptyLoadBalancerTest() throws InterruptedException, ExecutionException {
        final String albName = "MyAlb"+new Random().nextInt();
        final ApplicationLoadBalancer<String> alb = landscape.createLoadBalancer(albName, region);
        try {
            assertNotNull(alb);
            assertEquals(albName, alb.getName());
            assertTrue(Util.contains(Util.map(landscape.getLoadBalancers(region), ApplicationLoadBalancer::getArn), alb.getArn()));
            // now add two rules to the load balancer and check they arrive:
            final String hostnameCondition = "a.wiesen-weg.de";
            @SuppressWarnings("unchecked")
            final Iterable<Rule> rulesCreated = alb
                    .addRules(Rule.builder()
                            .priority("5")
                            .conditions(r -> r.field("host-header").hostHeaderConfig(hhc -> hhc.values(hostnameCondition)))
                            .actions(a -> a.type(ActionTypeEnum.FIXED_RESPONSE).fixedResponseConfig(frc -> frc.statusCode("200").messageBody("Hello world"))).build());
            assertEquals(1, Util.size(rulesCreated));
            assertTrue(hostnameCondition, rulesCreated.iterator().next().conditions().iterator().next().hostHeaderConfig().values().contains(hostnameCondition));
        } finally {
            alb.delete();
        }
    }
    
    @Test
    public void createAndDeleteTargetGroupTest() {
        final String targetGroupName = "TestTargetGroup-"+new Random().nextInt();
        final TargetGroup<String> targetGroup = landscape.createTargetGroup(region, targetGroupName, 80, "/gwt/status", 80,
                /* loadBalancerArn */ null);
        try {
            final TargetGroup<String> fetchedTargetGroup = landscape.getTargetGroup(region, targetGroupName,
                    targetGroup.getTargetGroupArn(), targetGroup.getLoadBalancerArn(), targetGroup.getProtocol(),
                    targetGroup.getPort(), targetGroup.getHealthCheckProtocol(),
                    targetGroup.getHealthCheckPort(), targetGroup.getHealthCheckPath());
            assertEquals(targetGroupName, fetchedTargetGroup.getName());
        } finally {
            landscape.deleteTargetGroup(targetGroup);
        }
    }
    
    @Test
    public void testCentralReverseProxyInEuWest2IsAvailable() throws IOException, InterruptedException, JSchException {
        final ReverseProxyCluster<String, SailingAnalyticsMetrics, ProcessT, ?> proxy = landscape.getCentralReverseProxy(new AwsRegion("eu-west-2"));
        assertEquals(1, Util.size(proxy.getHosts()));
        final HttpURLConnection healthCheckConnection = (HttpURLConnection) new URL("http://"+proxy.getHosts().iterator().next().getPublicAddress().getCanonicalHostName()+proxy.getHealthCheckPath()).openConnection();
        assertEquals(200, healthCheckConnection.getResponseCode());
        healthCheckConnection.disconnect();
    }
}
