package com.sap.sailing.domain.queclinkadapter.impl;

import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;
import com.sap.sailing.domain.queclinkadapter.MessageWithProtocolVersionAndCountNumber;

public abstract class MessageWithProtocolVersionAndCountNumberImpl extends MessageImpl
        implements MessageWithProtocolVersionAndCountNumber {
    private final int protocolVersion;
    private final short countNumber;
    
    public MessageWithProtocolVersionAndCountNumberImpl(MessageType type, Direction direction, int protocolVersion, short countNumber) {
        super(type, direction);
        this.protocolVersion = protocolVersion;
        this.countNumber = countNumber;
    }

    @Override
    public int getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public short getCountNumber() {
        return countNumber;
    }
}
