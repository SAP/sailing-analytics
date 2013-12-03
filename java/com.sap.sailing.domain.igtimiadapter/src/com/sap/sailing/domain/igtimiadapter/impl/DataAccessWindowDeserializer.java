package com.sap.sailing.domain.igtimiadapter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.Permission;
import com.sap.sailing.domain.igtimiadapter.Resource;

public class DataAccessWindowDeserializer {
    public Resource createResourceFromJson(JSONObject resourceJson, IgtimiConnection conn) {
        Boolean blob = (Boolean) resourceJson.get("blob");
        return new ResourceImpl((Long) resourceJson.get("id"),
                new MillisecondsTimePoint(((Double) resourceJson.get("start_time")).longValue()),
                new MillisecondsTimePoint(((Double) resourceJson.get("end_time")).longValue()),
                (String) resourceJson.get("device_serial_number"),
                getDataTypes((JSONArray) resourceJson.get("data_types")),
                getPermissions((JSONObject) resourceJson.get("permissions")),
                blob == null ? false : blob, conn);
    }

    private Iterable<Permission> getPermissions(JSONObject permissions) {
        final List<Permission> result = new ArrayList<>();
        for (Entry<Object, Object> e : permissions.entrySet()) {
            if ((Boolean) e.getValue()) {
                result.add(Permission.valueOf((String) e.getKey()));
            }
        }
        return result;
    }

    private int[] getDataTypes(JSONArray jsonArray) {
        final int[] result = new int[jsonArray.size()];
        int i=0;
        for (Object o : jsonArray) {
            result[i++] = ((Long) o).intValue();
        }
        return result;
    }
}
