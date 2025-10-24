package com.sap.sailing.domain.igtimiadapter.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class DataAccessWindowDeserializer {
    static final String ID = "id";
    static final String START_TIME = "start_time";
    static final String END_TIME = "end_time";
    static final String DEVICE_SERIAL_NUMBER = "device_serial_number";

    public DataAccessWindow createDataAccessWindowFromJson(JSONObject resourceJson) {
        return new DataAccessWindowImpl((Long) resourceJson.getOrDefault(ID, 0),
                new MillisecondsTimePoint(((Number) resourceJson.get(START_TIME)).longValue()),
                new MillisecondsTimePoint(((Number) resourceJson.get(END_TIME)).longValue()),
                (String) resourceJson.get(DEVICE_SERIAL_NUMBER));
    }
}
