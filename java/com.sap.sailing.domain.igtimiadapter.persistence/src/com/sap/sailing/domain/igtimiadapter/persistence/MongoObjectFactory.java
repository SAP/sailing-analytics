package com.sap.sailing.domain.igtimiadapter.persistence;

import com.igtimi.IgtimiStream.Msg;
import com.mongodb.client.ClientSession;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sse.common.TimePoint;

public interface MongoObjectFactory {
    void storeDevice(Device device, ClientSession clientSessionOrNull);
    void removeDevice(long deviceId, ClientSession clientSessionOrNull);
    void storeDataAccessWindow(DataAccessWindow daw, ClientSession clientSessionOrNull);
    void removeDataAccessWindow(long dawId, ClientSession clientSessionOrNull);
    
    /**
     * Tries to extract a time stamp from the {@code message} and stores the message's
     * binary protobuf data with the time stamp extracted (defaulting to {@link TimePoint#now()})
     * and the {@code serialNumber} to identify the device.
     * 
     * @param serialNumber if {@code null}, the message won't be stored
     */
    void storeMessage(String serialNumber, Msg message, ClientSession clientSessionOrNull);
    void clear(ClientSession clientSessionOrNull);
}
