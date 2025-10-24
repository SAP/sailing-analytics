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
}
