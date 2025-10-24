package com.sap.sailing.domain.igtimiadapter.websocket;

import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;

/**
 * Helps bundling live data connections for the same set of devices. Clients can request a connection for a set of devices.
 * If one already exists, a wrapper to it is returned. This wrapper's {@link LiveDataConnection#stop()} method work such that
 * it only decrements a usage counter in this factory (and does so at most once), such that the actual connection is only
 * terminated if the last client has stopped using it.
 *  
 * @author Axel Uhl (D043530)
 *
 */
public interface LiveDataConnectionFactory {
    /**
     * @param deviceSerialNumbers
     *            the serial numbers of the devices of which the caller is interested in receiving live data; if empty
     *            or {@code null}, a {@code null} result is returned
     */
    LiveDataConnection getOrCreateLiveDataConnection(Iterable<String> deviceSerialNumbers) throws Exception;
}
