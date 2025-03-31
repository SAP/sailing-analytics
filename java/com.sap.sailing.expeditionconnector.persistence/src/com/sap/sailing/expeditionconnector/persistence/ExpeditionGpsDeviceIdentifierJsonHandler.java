package com.sap.sailing.expeditionconnector.persistence;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sse.common.TransformationException;

public class ExpeditionGpsDeviceIdentifierJsonHandler extends ExpeditionGpsDeviceIdentifierSerializationHandler 
implements DeviceIdentifierJsonHandler {

    @Override
    public DeviceIdentifier deserialize(Object serialized, String type, String stringRepresentation)
            throws TransformationException {
        return deserialize((String) serialized, type, stringRepresentation);
    }

}
