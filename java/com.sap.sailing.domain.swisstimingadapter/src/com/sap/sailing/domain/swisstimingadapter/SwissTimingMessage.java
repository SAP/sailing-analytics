package com.sap.sailing.domain.swisstimingadapter;

import com.sap.sailing.udpconnector.UDPMessage;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

public interface SwissTimingMessage extends UDPMessage {
    String getRaceID();

    int getPacketID();

    TimePoint getTimestamp();

    int getGpsID();

    Position getPosition();

    Speed getSpeed();

    int getNumberOfSatellites();

    int getBatteryPercent();
    
    /**
     * @return the number of bytes that were used to encode this message
     */
    int length();
}
