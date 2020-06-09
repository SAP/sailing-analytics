package com.sap.sailing.domain.racelogtracking.impl;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.racelogtracking.SmartphoneUUIDSerializationHandler;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;

public class SmartphoneUUIDMongoHandler extends SmartphoneUUIDSerializationHandler
implements DeviceIdentifierMongoHandler {

    @Override
    public DeviceIdentifier deserialize(Object serialized, String type, String stringRepresentation)
            throws TransformationException {
        return deserialize((String) serialized, type, stringRepresentation);
    }
    
}
