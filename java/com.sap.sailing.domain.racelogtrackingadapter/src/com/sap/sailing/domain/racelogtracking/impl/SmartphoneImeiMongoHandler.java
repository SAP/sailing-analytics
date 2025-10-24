package com.sap.sailing.domain.racelogtracking.impl;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sse.common.TransformationException;

public class SmartphoneImeiMongoHandler extends SmartphoneImeiSerializationHandler
implements DeviceIdentifierMongoHandler {

    @Override
    public DeviceIdentifier deserialize(Object serialized, String type, String stringRepresentation)
            throws TransformationException {
        return deserialize((String) serialized, type, stringRepresentation);
    }
    
}
