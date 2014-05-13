package com.sap.sailing.server.gateway.serialization.racelog.tracking.impl;

import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.racelog.tracking.DeviceIdentifier;
import com.sap.sailing.domain.racelog.tracking.impl.PlaceHolderDeviceIdentifierSerializationHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;

public class PlaceHolderDeviceIdentifierJsonHandler extends PlaceHolderDeviceIdentifierSerializationHandler
implements DeviceIdentifierJsonHandler {

    @Override
    public DeviceIdentifier deserialize(Object serialized, String type, String stringRepresentation)
            throws TransformationException {
        return deserialize((String) serialized, type, stringRepresentation);
    }

}
