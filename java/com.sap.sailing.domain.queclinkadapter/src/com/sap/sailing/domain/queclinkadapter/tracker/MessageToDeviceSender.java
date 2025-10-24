package com.sap.sailing.domain.queclinkadapter.tracker;

import java.io.IOException;

import com.sap.sailing.domain.queclinkadapter.Message;
import com.sap.sailing.domain.racelogtracking.SmartphoneImeiIdentifier;

public interface MessageToDeviceSender {
    void sendToDevice(SmartphoneImeiIdentifier deviceIdentifier, Message message) throws IOException;
}
