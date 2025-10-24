package com.sap.sailing.domain.igtimiadapter.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;

public class DataAccessWindowSerializer {
    public JSONObject createJsonFromDataAccessWindow(DataAccessWindow daw) {
        final JSONObject result = new JSONObject();
        result.put(DataAccessWindowDeserializer.ID, daw.getId());
        result.put(DataAccessWindowDeserializer.START_TIME, daw.getStartTime()==null?null:daw.getStartTime().asMillis());
        result.put(DataAccessWindowDeserializer.END_TIME, daw.getEndTime()==null?null:daw.getEndTime().asMillis());
        result.put(DataAccessWindowDeserializer.DEVICE_SERIAL_NUMBER, daw.getDeviceSerialNumber());
        return result;
    }
}
