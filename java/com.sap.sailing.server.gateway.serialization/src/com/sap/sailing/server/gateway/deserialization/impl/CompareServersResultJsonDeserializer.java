package com.sap.sailing.server.gateway.deserialization.impl;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sse.common.Util;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class CompareServersResultJsonDeserializer implements JsonDeserializer<CompareServersResult> {
    @Override
    public CompareServersResult deserialize(JSONObject object) throws JsonDeserializationException {
        assert object.size() == 2;
        final Iterator<Object> i = object.keySet().iterator();
        final String serverAName = i.next().toString();
        final String serverBName = i.next().toString();
        final Iterable<JSONObject> serverADiffs = getDiffs(object, serverAName);
        final Iterable<JSONObject> serverBDiffs = getDiffs(object, serverBName);
        return new CompareServersResult() {
            @Override
            public String getServerB() {
                return serverBName;
            }
            
            @Override
            public String getServerA() {
                return serverAName;
            }
            
            @Override
            public Iterable<JSONObject> getADiffs() {
                return serverADiffs;
            }
            
            @Override
            public Iterable<JSONObject> getBDiffs() {
                return serverBDiffs;
            }
        };
    }

    private Iterable<JSONObject> getDiffs(JSONObject object, String serverName) {
        return Util.map((JSONArray) object.get(serverName), o->(JSONObject) o);
    }
}
