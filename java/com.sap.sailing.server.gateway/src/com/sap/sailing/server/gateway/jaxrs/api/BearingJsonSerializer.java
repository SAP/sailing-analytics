package com.sap.sailing.server.gateway.jaxrs.api;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class BearingJsonSerializer implements JsonSerializer<Bearing> {
    @Override
    public JSONObject serialize(Bearing object) {
        JSONObject result = new JSONObject();
        result.put("truebearingdegrees", object.getDegrees());
        return result;
    }
}
