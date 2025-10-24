package com.sap.sailing.domain.queclinkadapter;

public interface NonCommand extends Message {
    @Override
    default String getPrefix() {
        return String.format("+%s:GT%s", getDirection(), getType().name());
    }
    
    @Override
    default char getTypeSeparator() {
        return ',';
    }
}
