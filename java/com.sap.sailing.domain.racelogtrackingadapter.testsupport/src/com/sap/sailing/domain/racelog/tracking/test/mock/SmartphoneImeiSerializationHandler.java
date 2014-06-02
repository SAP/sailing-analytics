package com.sap.sailing.domain.racelog.tracking.test.mock;

import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.racelog.tracking.DeviceIdentifier;
import com.sap.sse.common.UtilNew;

public class SmartphoneImeiSerializationHandler {
    private SmartphoneImeiIdentifier castIdentifier(DeviceIdentifier identifier) throws TransformationException {
        if (!(identifier instanceof SmartphoneImeiIdentifier))
            throw new TransformationException("Expected a SmartphoneImeiIdentifier, got instead: " + identifier);
        return (SmartphoneImeiIdentifier) identifier;
    }

    public UtilNew.Pair<String, String> serialize(DeviceIdentifier deviceIdentifier) throws TransformationException {
        return new UtilNew.Pair<String, String>(SmartphoneImeiIdentifier.TYPE, castIdentifier(deviceIdentifier).getImei());
    }

    public DeviceIdentifier deserialize(String input, String type, String stringRep) throws TransformationException {
        return new SmartphoneImeiIdentifier(stringRep);
    }
}
