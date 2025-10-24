package com.sap.sailing.domain.igtimiadapter;

import com.sap.sailing.domain.igtimiadapter.impl.DeviceImpl;
import com.sap.sse.common.Renamable;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

public interface Device extends HasId, WithQualifiedObjectIdentifier, Renamable {
    /**
     * @return a string identifying the device, such as "AA-AA-AAAA"
     */
    String getSerialNumber();
    
    /**
     * Returns the time point and the remote address of the last heart beat received from this device,
     * or {@code null} if no heartbeat has been received from this device yet.
     */
    Pair<TimePoint, String> getLastHeartbeat();
    
    /**
     * @param remoteAddress the remote address from which the heart beat was received
     */
    void setLastHeartbeat(TimePoint timePointOfLastHeartbeat, String remoteAddress);

    static Device create(long id, String serialNumber, String name) {
        return new DeviceImpl(id, serialNumber, name);
    }

    static Device create(long id, String deviceSerialNumber) {
        return new DeviceImpl(id, deviceSerialNumber);
    }
}
