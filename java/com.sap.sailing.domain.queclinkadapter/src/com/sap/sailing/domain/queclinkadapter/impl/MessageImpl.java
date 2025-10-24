package com.sap.sailing.domain.queclinkadapter.impl;

import com.sap.sailing.domain.queclinkadapter.Message;
import com.sap.sailing.domain.queclinkadapter.MessageType;
import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;

public abstract class MessageImpl implements Message {
    private final MessageType type;
    private final Direction direction;
    
    public MessageImpl(MessageType type, Direction direction) {
        super();
        this.type = type;
        this.direction = direction;
    }

    @Override
    public MessageType getType() {
        return type;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }
}
