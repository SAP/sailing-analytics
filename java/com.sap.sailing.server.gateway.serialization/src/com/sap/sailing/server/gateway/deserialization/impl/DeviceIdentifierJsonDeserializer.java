package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.racelog.tracking.SingleTypeBasedServiceFinderImpl;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.common.racelog.tracking.TypeBasedServiceFinder;
import com.sap.sailing.domain.racelog.tracking.DeviceIdentifier;
import com.sap.sailing.domain.racelog.tracking.PlaceHolderDeviceIdentifier;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.PlaceHolderDeviceIdentifierJsonHandler;

public class DeviceIdentifierJsonDeserializer implements JsonDeserializer<DeviceIdentifier> {

    public static final String FIELD_DEVICE_ID = "id";
    public static final String FIELD_DEVICE_TYPE = "type";
    public static final String FIELD_STRING_REPRESENTATION = "stringRepresentation";

    private final TypeBasedServiceFinder<DeviceIdentifierJsonHandler> deviceServiceFinder;
    
    /**
     * Create a deserializer for a single type, with fallback to {@link PlaceHolderDeviceIdentifier}.
     */
    public static DeviceIdentifierJsonDeserializer create(DeviceIdentifierJsonHandler singleHandler, String type) {
        return new DeviceIdentifierJsonDeserializer(new SingleTypeBasedServiceFinderImpl<DeviceIdentifierJsonHandler>(singleHandler,
                new PlaceHolderDeviceIdentifierJsonHandler(), type));
    }

    public DeviceIdentifierJsonDeserializer(TypeBasedServiceFinder<DeviceIdentifierJsonHandler> deviceServiceFinder) {
        this.deviceServiceFinder = deviceServiceFinder;
    }

    @Override
    public DeviceIdentifier deserialize(JSONObject object) throws JsonDeserializationException {
        Object deviceIdObject = object.get(FIELD_DEVICE_ID);
        String deviceType = (String) object.get(FIELD_DEVICE_TYPE);
        String deviceStringRep = (String) object.get(FIELD_STRING_REPRESENTATION);
        try {
            return deviceServiceFinder.findService(deviceType).deserialize(deviceIdObject, deviceType, deviceStringRep);
        } catch (TransformationException e) {
            throw new JsonDeserializationException(e);
        }
    }

}
