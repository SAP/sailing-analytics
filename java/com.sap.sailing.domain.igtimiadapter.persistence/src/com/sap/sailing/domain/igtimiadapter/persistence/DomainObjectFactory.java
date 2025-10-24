package com.sap.sailing.domain.igtimiadapter.persistence;

import java.util.Set;

import com.google.protobuf.InvalidProtocolBufferException;
import com.igtimi.IgtimiData.DataPoint;
import com.igtimi.IgtimiData.DataPoint.DataCase;
import com.igtimi.IgtimiStream.Msg;
import com.mongodb.client.ClientSession;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sse.common.MultiTimeRange;

public interface DomainObjectFactory {
    Iterable<DataAccessWindow> getDataAccessWindows(ClientSession clientSessionOrNull);

    Iterable<Device> getDevices(ClientSession clientSessionOrNull);
    
    Iterable<Msg> getMessages(String deviceSerialNumber, MultiTimeRange timeRanges, Set<DataCase> dataCases, ClientSession clientSessionOrNull);

    /**
     * Finds the latest message received from the device identified by {@code deviceSerialNumber}
     * that has a {@link DataPoint} of the correct {@link DataCase}.
     */
    Msg getLatestMessage(String deviceSerialNumber, DataCase dataCase, MultiTimeRange timeRanges, ClientSession clientSessionOrNull) throws InvalidProtocolBufferException;
}
