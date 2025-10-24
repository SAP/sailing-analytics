package com.sap.sse.landscape;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.WithID;
import com.sap.sse.landscape.ssh.SshCommandChannel;

/**
 * Equality / hash code is based on the {@link WithID#getId() ID}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface Host extends WithID {
    /**
     * Obtains the public IP address of this host. Note that during the boot phase and after shutdown/termination a host
     * may not yet have such a public IP address assigned. In this case, {@code null} may be returned. To avoid this,
     * you can alternatively call {@link #getPublicAddress(Optional)} to wait for a public IP address to become
     * available.
     */
    InetAddress getPublicAddress();
    
    /**
     * Obtains the public IP address of this host, waiting for one to become available for the duration of
     * {@code timeout}, or forever in case {@code timeout} is {@code null}
     * 
     * @param timeoutEmptyMeaningForever
     *            if not {@link Optional#isPresent() present}, waits forever
     */
    InetAddress getPublicAddress(Optional<Duration> timeoutEmptyMeaningForever) throws TimeoutException, Exception;
    
    /**
     * Obtains the private IP address of this host. Note that during the boot phase and after shutdown/termination a host
     * may not yet have such a public IP address assigned. In this case, {@code null} may be returned. To avoid this,
     * you can alternatively call {@link #getPrivateAddress(Optional)} to wait for a public IP address to become
     * available.
     */
    InetAddress getPrivateAddress();

    /**
     * Obtains the private IP address of this host, waiting for one to become available for the duration of
     * {@code timeout}, or forever in case {@code timeout} is {@code null}
     * 
     * @param timeoutEmptyMeaningForever
     *            if {@code null}, waits forever
     */
    InetAddress getPrivateAddress(Optional<Duration> timeoutEmptyMeaningForever);
    
    /**
     * By default, this returns the stringified {@link #getPrivateAddress()}. Specialized implementations
     * may choose to try to map this to a host name through a reverse DNS lookup.
     */
    default String getHostname() {
        return getPrivateAddress().getHostAddress();
    }

    /**
     * By default, this returns the stringified {@link #getPrivateAddress()}. Specialized implementations
     * may choose to try to map this to a host name through a reverse DNS lookup.
     */
    default String getHostname(Optional<Duration> timeoutEmptyMeaningForever) {
        return getPrivateAddress(timeoutEmptyMeaningForever).getHostAddress();
    }

    /**
     * Connects to an SSH session for the username specified, using the SSH key pair used to launch the instance, and
     * opens a "shell" channel. Use the {@link Channel} returned by {@link Channel#setInputStream(java.io.InputStream)
     * setting an input stream} from which the commands to be sent to the server will be read, and by
     * {@link Channel#setOutputStream(java.io.OutputStream) setting the output stream} to which the server will send its
     * output. You will usually want to use either a {@link ByteArrayInputStream} to provide a set of predefined
     * commands to sent to the server, and a {@link PipedInputStream} wrapped around a {@link PipedOutputStream} which
     * you set to the channel.
     * 
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     */
    SshCommandChannel createSshChannel(String sshUserName, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * Connects to an SSH session for the "root" user with a "shell" channel
     * 
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     * 
     * @return {@code null} in case the connection attempt timed out
     * @see #createSshChannel(String, Optional, byte[])
     */
    SshCommandChannel createRootSshChannel(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     * @return {@code null} in case the connection attempt timed out
     */
    ChannelSftp createSftpChannel(String sshUserName, Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;

    /**
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     * @return {@code null} in case the connection attempt timed out
     */
    ChannelSftp createRootSftpChannel(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception;
    
    /**
     * Tells where in the cloud this host runs; the availability zone {@link AvailabilityZone#getRegion() implies} the
     * {@link Region}.
     */
    AvailabilityZone getAvailabilityZone();
    
    default Region getRegion() {
        return getAvailabilityZone().getRegion();
    }
    
    Iterable<SecurityGroup> getSecurityGroups();

    TimePoint getLaunchTimePoint();
    
    /**
     * Checks whether an SSH connection with the "root" user can be established successfully. A ten-second timeout is
     * used.
     * 
     * @param optionalKeyName
     *            the name of the SSH key pair to use to log on; must identify a key pair available for the
     *            {@link #getRegion() region} of this instance. If not provided, the the SSH private key for the key
     *            pair that was originally used when the instance was launched will be used.
     * @param privateKeyEncryptionPassphrase
     *            the pass phrase for the private key that belongs to the instance's public key used for start-up
     */
    default boolean isReady(Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        final boolean result;
        final SshCommandChannel channel = createRootSshChannel(Optional.of(Duration.ONE_SECOND.times(10)),
                optionalKeyName, privateKeyEncryptionPassphrase);
        if (channel != null) {
            channel.sendCommandLineSynchronously("pwd", new ByteArrayOutputStream());
            final String response = channel.getStreamContentsAsString();
            result = Util.hasLength(response);
        } else {
            result = false;
        }
        return result;
    }
    
    /**
     * Checks whether this host is a "shared" one to which multiple application processes may be deployed.<p>
     * 
     * This default implementation returns {@code false}.
     */
    default boolean isSharedHost() {
        return false;
    }
    
    /**
     * Checks if there is a name tag and returns the value, if it exists.
     * 
     * @return name tag value
     */
    String getNameTag();
    
    /**
     * Fetches the AMI id value. We may assume that this is impossible to change, given any amount of time.
     */
    String getImageId();
}
