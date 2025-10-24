package com.sap.sailing.domain.igtimiadapter.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.igtimiadapter.Device;

public class DeviceSerializer {
    public JSONObject createJsonFromDevice(Device device) {
        final JSONObject result = new JSONObject();
        result.put(DeviceDeserializer.ID, device.getId());
        result.put(DeviceDeserializer.SERIAL_NUMBER, device.getSerialNumber());
        result.put(DeviceDeserializer.NAME, device.getName());
        return result;
    }
}
