package com.sap.sailing.domain.queclinkadapter.impl;

import com.sap.sailing.domain.queclinkadapter.HBDMessage;
import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;

public abstract class HBDMessageImpl extends MessageWithProtocolVersionAndCountNumberImpl implements HBDMessage {
    public HBDMessageImpl(Direction direction, int protocolVersion, short countNumber) {
        super(MessageType.HBD, direction, protocolVersion, countNumber);
    }
}
