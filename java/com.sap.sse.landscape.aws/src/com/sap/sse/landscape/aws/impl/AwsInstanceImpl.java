package com.sap.sse.landscape.aws.impl;

import java.io.ByteArrayInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.SecurityGroup;
import com.sap.sse.landscape.aws.AwsAutoScalingGroup;
import com.sap.sse.landscape.aws.AwsAvailabilityZone;
import com.sap.sse.landscape.aws.AwsInstance;
import com.sap.sse.landscape.aws.AwsLandscape;
import com.sap.sse.landscape.aws.orchestration.StartAwsHost;
import com.sap.sse.landscape.ssh.JCraftLogAdapter;
import com.sap.sse.landscape.ssh.SSHKeyPair;
import com.sap.sse.landscape.ssh.SshCommandChannel;
import com.sap.sse.landscape.ssh.SshCommandChannelImpl;
import com.sap.sse.landscape.ssh.YesUserInfo;
import com.sap.sse.shared.util.Wait;

import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AwsInstanceImpl<ShardingKey> implements AwsInstance<ShardingKey> {
    private final static Logger logger = Logger.getLogger(AwsInstanceImpl.class.getName());
    private static final String ROOT_USER_NAME = "root";
    private final String instanceId;
    private final AwsAvailabilityZone availabilityZone;
    private final InetAddress privateAddress;
    private InetAddress publicAddress;
    private final TimePoint launchTimePoint;
    private final AwsLandscape<ShardingKey> landscape;
    private String name;
    private String imageId;
    
    public AwsInstanceImpl(String instanceId, AwsAvailabilityZone availabilityZone, InetAddress privateAddress, TimePoint launchTimePoint, AwsLandscape<ShardingKey> landscape) {
        this.instanceId = instanceId;
        this.availabilityZone = availabilityZone;
        this.privateAddress = privateAddress;
        this.launchTimePoint = launchTimePoint;
        this.landscape = landscape;
    }
    
    @Override
    public boolean equals(Object other) {
        return ((AwsInstance<?>) other).getInstanceId().equals(getInstanceId());
    }

    @Override
    public int hashCode() {
        return getInstance().hashCode();
    }
    
    @Override
    public TimePoint getLaunchTimePoint() {
        return launchTimePoint;
    }

    @Override
    public InetAddress getPublicAddress() {
        if (publicAddress == null) {
            try {
                publicAddress = getPublicAddress(Optional.of(Duration.NULL));
            } catch (TimeoutException e) {
                logger.info("Couldn't get public address of instance "+getId());
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return publicAddress;
    }
    
    @Override
    public InetAddress getPublicAddress(Optional<Duration> timeoutEmptyMeaningForever) throws TimeoutException, Exception {
        if (publicAddress == null) {
            final Instance instance = getInstance();
            // for RUNNING and PENDING instances it's worthwhile waiting for the address to show; in all other cases we return null immediately
            if (instance.state().name() == InstanceStateName.RUNNING || instance.state().name() == InstanceStateName.PENDING) {
                final String publicIpAddress = Wait.wait(()->getInstance().publicIpAddress(), ipAddress->ipAddress != null, /* retryOnException */ false,
                        timeoutEmptyMeaningForever, /* sleep between attempts */ Duration.ONE_SECOND.times(5),
                        Level.INFO, "Waiting for public IP address of instance "+instance.instanceId());
                publicAddress = publicIpAddress == null ? null : InetAddress.getByName(publicIpAddress);
            } else {
                publicAddress = null;
            }
        }
        return publicAddress;
    }
    
    /**
     * Checks if there is a name tag and returns the value, if it exists. This implementation caches and so may cause
     * caching issues, because the name of the instance can change over time. 
     * 
     * @return name tag value
     */
    public String getNameTag() {
        String result = "No name tag found";
        if (name == null) {
            final Instance instance = getInstance();
            for (Tag tag : instance.tags()) {
                if (tag.key().equals(StartAwsHost.NAME_TAG_NAME)) {
                    name = tag.value();
                    result = name;
                    break;
                }
            }
        } else {
            result = name;
        }
        return result;
    }
    
    public String getImageId() {
        final String result;
        if (imageId == null) {
            final Instance instance = getInstance();
            imageId = instance.imageId();
            result = imageId;
        } else {
            result = imageId;
        }
        return result;
    }
    

    @Override
    public InetAddress getPrivateAddress() {
        return privateAddress;
    }
    
    @Override
    public InetAddress getPrivateAddress(Optional<Duration> timeoutEmptyMeaningForever) {
        return getPrivateAddress();
    }
    
    @Override
    public String getHostname() {
        final String privateIpAddress = getPrivateAddress().getHostAddress();
        return getHostnameForPrivateAddress(privateIpAddress);
    }

    @Override
    public String getHostname(Optional<Duration> timeoutEmptyMeaningForever) {
        final String privateIpAddress = getPrivateAddress(timeoutEmptyMeaningForever).getHostAddress();
        return getHostnameForPrivateAddress(privateIpAddress);
    }

    private String getHostnameForPrivateAddress(final String privateIpAddress) {
        final String result;
        if (privateIpAddress == null) {
            result = null;
        } else {
            // try a DNS reverse lookup:
            final String hostname = landscape.findHostnamesForIP(privateIpAddress);
            if (hostname == null) {
                result = privateIpAddress;
            } else {
                result = hostname;
            }
        }
        return result;
    }

    /**
     * Establishes an unconnected session configured for the "root" user.
     * 
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     * @see #createRootSshChannel
     */
    public com.jcraft.jsch.Session createRootSshSession(Optional<String> optionalKeyName,
            byte[] privateKeyEncryptionPassphrase) throws TimeoutException, Exception {
        return createSshSession(ROOT_USER_NAME, optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    /**
     * Establishes an unconnected session configured for the user specified by {@code sshUserName}, trying to find and
     * unlock the SSH private key for the key pair whose name is provided by the {@code keyName} parameter. A
     * {@link NullPointerException} will be thrown if such a key cannot be found in the {@link #getRegion() region} of
     * this instance.
     * 
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase to unlock the private key that belongs to the key pair identified by {@code keyName}
     * @throws JSchException 
     * @see #createRootSshChannel
     */
    public com.jcraft.jsch.Session createSshSession(String sshUserName, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws JSchException {
        final String keyName = optionalKeyName.orElseGet(()->getInstance().keyName()); // the SSH key pair name that can be used to log on
        final SSHKeyPair keyPair = landscape.getSSHKeyPair(getRegion(), keyName);
        if (keyPair == null) {
            throw new IllegalStateException("Couldn't find key pair "+keyName+" in landscape.");
        }
        final JSch jsch = new JSch();
        JSch.setLogger(new JCraftLogAdapter());
        jsch.addIdentity(keyName, keyPair.getEncryptedPrivateKey(), keyPair.getPublicKey(), privateKeyEncryptionPassphrase);
        final InetAddress address = getPublicAddress();
        if (address == null) {
            throw new IllegalStateException("Instance "+getInstanceId()+" doesn't have a public IP address");
        }
        return jsch.getSession(sshUserName, address.getHostAddress());
    }

    /**
     * Connects to an SSH session for the "root" user with a "shell" channel
     * 
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     * 
     * @return {@code null} in case the connection attempt timed out
     * @see #createSshChannel(String, Optional, byte[])
     */
    @Override
    public SshCommandChannel createRootSshChannel(Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return createSshChannel(ROOT_USER_NAME, optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }
    
    /**
     * Connects to an SSH session for the username specified and opens a "shell" channel. Use the {@link Channel}
     * returned by {@link Channel#setInputStream(java.io.InputStream) setting an input stream} from which the commands
     * to be sent to the server will be read, and by {@link Channel#setOutputStream(java.io.OutputStream) setting the
     * output stream} to which the server will send its output. You will usually want to use either a
     * {@link ByteArrayInputStream} to provide a set of predefined commands to sent to the server, and a
     * {@link PipedInputStream} wrapped around a {@link PipedOutputStream} which you set to the channel.
     * 
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     * @return {@code null} in case the connection attempt timed out
     */
    @Override
    public SshCommandChannel createSshChannel(String sshUserName, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final ChannelExec channelExec = (ChannelExec) createSshChannelInternal(sshUserName, "exec", optionalTimeout,
                        optionalKeyName, privateKeyEncryptionPassphrase);
        return channelExec == null ? null : new SshCommandChannelImpl(channelExec);
    }
    
    /**
     * @return {@code null} in case the connection attempt timed out
     */
    private Channel createSshChannelInternal(String sshUserName, String channelType, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        logger.info(
                "Creating SSH "+channelType+" channel for SSH user "+sshUserName+
                " with key "+optionalKeyName+
                " to instance with ID "+getInstanceId());
        Channel result;
        if (!optionalKeyName.isPresent() && !Util.hasLength(getInstance().keyName())) {
            logger.severe("SSH connection to "+this+" cannot be made because no key name is provided, neither explicitly nor during start-up");
            result = null;
        } else {
            try {
                result = Wait.wait(()->{
                        Session session = null;
                        try {
                            session = createSshSession(sshUserName, optionalKeyName, privateKeyEncryptionPassphrase);
                            session.setUserInfo(new YesUserInfo());
                            session.connect(optionalTimeout.map(d->d.asMillis()).orElse(0l).intValue());
                            return session.openChannel(channelType);
                        } catch (JSchException | IllegalStateException e) {
                            if (session != null) {
                                session.disconnect();
                            }
                            throw e;
                        }
                    },
                    channel->channel != null,
                    /* retryOnException */ true, optionalTimeout,
                    Duration.ONE_SECOND.times(5), Level.INFO,
                    "Trying to connect to " + getInstanceId() + " with user " + sshUserName + " using SSH");
            } catch (TimeoutException timeout) {
                result = null;
            }
        }
        return result;
    }
    
    @Override
    public ChannelSftp createSftpChannel(String sshUserName, Optional<Duration> optionalTimeout,
            Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return (ChannelSftp) createSshChannelInternal(sshUserName, "sftp", optionalTimeout,
                optionalKeyName, privateKeyEncryptionPassphrase);
    }

    @Override
    public ChannelSftp createRootSftpChannel(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase)
            throws Exception {
        return createSftpChannel(ROOT_USER_NAME, optionalTimeout, optionalKeyName, privateKeyEncryptionPassphrase);
    }

    @Override
    public AwsAvailabilityZone getAvailabilityZone() {
        return availabilityZone;
    }

    @Override
    public InstanceType getInstanceType() {
        return getInstance().instanceType();
    }

    @Override
    public Iterable<SecurityGroup> getSecurityGroups() {
        return Util.map(getInstance().securityGroups(), groupIdentifier -> 
            landscape.getSecurityGroup(groupIdentifier.groupId(), getRegion()));
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }
    
    @Override
    public void terminate() {
        landscape.terminate(this);
    }
    
    @Override
    public AwsLandscape<ShardingKey> getLandscape() {
        return landscape;
    }
    
    @Override
    public boolean isManagedByAutoScalingGroup() {
        return getInstance().tags().stream().filter(tag->tag.key().equals(AWS_AUTOSCALING_GROUP_NAME_TAG)).findAny().isPresent();
    }

    @Override
    public boolean isManagedByAutoScalingGroup(Iterable<AwsAutoScalingGroup> autoScalingGroups) {
        return getInstance().tags().stream().filter(tag -> tag.key().equals(AWS_AUTOSCALING_GROUP_NAME_TAG)
                && Util.stream(autoScalingGroups).filter(autoScalingGroup -> autoScalingGroup.getName().equals(tag.value())).findAny().isPresent())
                .findAny().isPresent();
    }

    @Override
    public String toString() {
        return getInstanceId();
    }

    @Override
    public boolean verifySshKey( Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) {
        final String keyName = optionalKeyName.orElseGet(()->getInstance().keyName()); // the SSH key pair name that can be used to log on
        final SSHKeyPair keyPair = landscape.getSSHKeyPair(getRegion(), keyName);
        if (keyPair == null) {
            return false;
        }
        final JSch jsch = new JSch();
        return keyPair.checkPassphrase(jsch, privateKeyEncryptionPassphrase);
    }
}
