package com.sap.sailing.domain.racelogtracking.impl;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util;

public class SmartphoneImeiSerializationHandler {
    private SmartphoneImeiIdentifierImpl castIdentifier(DeviceIdentifier identifier) throws TransformationException {
        if (!(identifier instanceof SmartphoneImeiIdentifierImpl))
            throw new TransformationException("Expected a SmartphoneImeiIdentifier, got instead: " + identifier);
        return (SmartphoneImeiIdentifierImpl) identifier;
    }

    public Util.Pair<String, String> serialize(DeviceIdentifier deviceIdentifier) throws TransformationException {
        return new Util.Pair<String, String>(SmartphoneImeiIdentifierImpl.TYPE, castIdentifier(deviceIdentifier).getImei());
    }

    public DeviceIdentifier deserialize(String input, String type, String stringRep) throws TransformationException {
        return new SmartphoneImeiIdentifierImpl(stringRep);
    }
}
