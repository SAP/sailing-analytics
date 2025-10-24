package com.sap.sailing.domain.queclinkadapter;

import com.sap.sse.common.TimePoint;

public interface MessageWithDeviceOrigin extends MessageWithProtocolVersionAndCountNumber {
    String getImei();
    
    String getDeviceName();
    
    TimePoint getSendTime();
}
