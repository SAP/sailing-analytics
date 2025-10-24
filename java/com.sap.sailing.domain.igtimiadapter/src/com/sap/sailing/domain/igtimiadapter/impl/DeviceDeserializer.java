package com.sap.sailing.domain.igtimiadapter.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.igtimiadapter.Device;

public class DeviceDeserializer {
    static final String ID = "id";
    static final String SERIAL_NUMBER = "serial_number";
    static final String NAME = "name";

    public Device createDeviceFromJson(JSONObject deviceJson) {
        return new DeviceImpl((Long) deviceJson.getOrDefault(ID, 0), (String) deviceJson.get(SERIAL_NUMBER),
                (String) deviceJson.get(NAME));
    }
}
