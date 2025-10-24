package com.sap.sse.landscape;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import com.sap.sse.common.Duration;

public interface Process<LogT extends Log, MetricsT extends Metrics> {
    /**
     * The TCP port through which this process is typically accessed. For example, a MongoDB
     * would default this to 27017; an HTTP server would be using 80.
     */
    int getPort();
    
    default int[] getAllTCPPorts(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return new int[] { getPort() };
    }
    
    default int[] getAllUDPPorts(Optional<Duration> optionalTimeout, Optional<String> optionalKeyName, byte[] privateKeyEncryptionPassphrase) throws Exception {
        return new int[0];
    }
    
    /**
     * The host that this process is running on
     */
    Host getHost();
    
    default String getHostname() {
        // TODO consider caching this or requiring it upon object creation
        return getHost().getHostname();
    }
    
    default String getHostname(Optional<Duration> timeoutEmptyMeaningForever) {
        return getHost().getHostname(timeoutEmptyMeaningForever);
    }
    
    /**
     * Grants access to the log that this process produces
     */
    LogT getLog();
    
    MetricsT getMetrics();
    
    /**
     * Tells whether this process is still alive and will at some point (again)
     * become {@link #isReady(Optional<Duration>) ready} to accept requests. An example of a process {@link #isAlive(Optional) alive}
     * but not {@link #isReady(Optional<Duration>) ready} would be a replica process that has started to receive the initial
     * load from its master. It can answer in a well-defined way to health check / status requests, but you
     * shouldn't route regular traffic to it yet.<p>
     * 
     * The default implementation tries to open a socket connection to the host's public address and the {@link #getPort() port}.
     */
    default boolean isAlive(Optional<Duration> optionalTimeout) throws TimeoutException, Exception {
        Socket socket = null;
        try {
            final boolean result;
            final InetAddress publicAddress = getHost().getPublicAddress(optionalTimeout);
            if (publicAddress != null) {
                socket = new Socket(publicAddress, getPort());
                result = socket.isConnected();
            } else {
                result = false;
            }
            return result;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
    
    /**
     * Tells whether this process is ready to accept requests. Use this for a health check in a target group
     * that decides whether traffic will be sent to this process. {@link #isReady(Optional<Duration>)} implies {@link #isAlive(Optional)}.
     */
    boolean isReady(Optional<Duration> optionalTimeout) throws MalformedURLException, IOException;
}
