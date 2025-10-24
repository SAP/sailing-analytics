package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.queclinkadapter.MessageType.Direction;

public interface Command extends Message {
    @Override
    default Direction getDirection() {
        return Direction.AT;
    }
    
    @Override
    default String getPrefix() {
        return String.format("AT+GT%s", getDirection(), getType().name());
    }
    
    @Override
    default char getTypeSeparator() {
        return '=';
    }
}
