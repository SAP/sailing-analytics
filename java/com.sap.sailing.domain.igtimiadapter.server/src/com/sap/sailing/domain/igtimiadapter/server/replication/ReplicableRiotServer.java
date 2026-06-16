package com.sap.sailing.domain.igtimiadapter.server.replication;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.common.TimePoint;

public interface ReplicableRiotServer extends RiotServer {
    Void internalRemoveDevice(long deviceId);

    Void internalUpdateDeviceName(long deviceId, String name);
    
    Void internalUpdateDeviceLastHeartbeat(long deviceId, TimePoint timePointOfLastHeartbeat, String remoteAddress);
    
    DataAccessWindow internalCreateDataAccessWindow(String deviceSerialNumber, TimePoint startTime, TimePoint endTime);

    Void internalRemoveDataAccessWindow(long dawId);

    Void internalNotifyListeners(Msg message, String deviceSerialNumber);

    Device internalCreateDevice(String deviceSerialNumber);

    boolean internalSendCommand(String deviceSerialNumber, String command) throws IOException, InterruptedException, ExecutionException;

    boolean internalEnableOverTheAirLog(String deviceSerialNumber, boolean enable) throws IOException;

    /**
     * Invoked after the {@link #internalCreateDevice(String)} method was asked to create a new {@link Device}
     * on the primary/master instance. A no-op on the primary; on replicas this is expected to unblock any
     * fixes that were received for that device's serial number but couldn't be applied yet because the
     * {@link Device} didn't exist yet. Now, when a replica receives a call to this operation, it can ensure
     * it creates the {@link Device} object with the correct ID, avoiding any race conditions, and then
     * apply any fixes buffered so far.
     */
    Void internalDeviceCreated(long id, String deviceSerialNumber);
}
