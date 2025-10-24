package com.sap.sailing.domain.racelog.tracking.test.mock;

import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiJsonHandler;
import com.sap.sailing.server.gateway.deserialization.impl.DeviceAndSessionIdentifierWithGPSFixesDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.DeviceIdentifierJsonDeserializer;

public class MockDeviceAndSessionIdentifierWithGPSFixesDeserializer extends
DeviceAndSessionIdentifierWithGPSFixesDeserializer {

    public MockDeviceAndSessionIdentifierWithGPSFixesDeserializer() {
        super(new MockServiceFinder<>(new MockGPSFixJsonHandler()),
                DeviceIdentifierJsonDeserializer.create(new SmartphoneImeiJsonHandler(), SmartphoneImeiIdentifierImpl.TYPE));
    }

}
