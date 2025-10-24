package com.sap.sailing.domain.queclinkadapter;

public interface MessageWithProtocolVersionAndCountNumber extends Message {
    int getProtocolVersion();
    
    short getCountNumber();
}
