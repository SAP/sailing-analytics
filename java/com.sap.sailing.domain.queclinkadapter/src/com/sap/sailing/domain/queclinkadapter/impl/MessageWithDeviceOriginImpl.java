package com.sap.sailing.domain.queclinkadapter.impl;

import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sse.common.TimePoint;
import com.sap.sailing.domain.queclinkadapter.MessageWithDeviceOrigin;

public abstract class MessageWithDeviceOriginImpl extends MessageWithProtocolVersionAndCountNumberImpl
        implements MessageWithDeviceOrigin {
    private final String imei;
    private final String deviceName;
    private final TimePoint sendTime;

    public MessageWithDeviceOriginImpl(MessageType type, Direction direction, int protocolVersion, String imei, String deviceName, TimePoint sendTime, short countNumber) {
        super(type, direction, protocolVersion, countNumber);
        this.imei = imei;
        this.deviceName = deviceName;
        this.sendTime = sendTime;
    }

    @Override
    public TimePoint getSendTime() {
        return sendTime;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String getImei() {
        return imei;
    }
}
