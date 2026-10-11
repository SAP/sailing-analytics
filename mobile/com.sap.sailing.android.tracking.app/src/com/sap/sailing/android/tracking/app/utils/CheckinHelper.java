package com.sap.sailing.android.tracking.app.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;

import org.json.JSONException;
import org.json.JSONObject;

public class CheckinHelper {

    @NonNull
    private static JSONObject getBaseCheckinJson(String deviceUuid, String pushDeviceId,
                                                 long fromMillis, JSONObject jsonObject)
            throws JSONException {
        jsonObject.put(DeviceMappingConstants.JSON_DEVICE_TYPE, "android");
        jsonObject.put(DeviceMappingConstants.JSON_DEVICE_UUID, deviceUuid);
        jsonObject.put(DeviceMappingConstants.JSON_PUSH_DEVICE_ID, pushDeviceId);
        jsonObject.put(DeviceMappingConstants.JSON_FROM_MILLIS, fromMillis);
        return jsonObject;
    }

    @NonNull
    public static JSONObject getCompetitorCheckinJson(@Nullable String secret, String competitorId,
                                                      String deviceUuid, String pushDeviceId,
                                                      long fromMillis) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt(DeviceMappingConstants.JSON_REGISTER_SECRET, secret);
        jsonObject.put(DeviceMappingConstants.JSON_COMPETITOR_ID_AS_STRING, competitorId);
        return getBaseCheckinJson(deviceUuid, pushDeviceId, fromMillis, jsonObject);
    }

    @NonNull
    public static JSONObject getMarkCheckinJson(@Nullable String secret, String markId,
                                                String deviceUuid, String pushDeviceId,
                                                long fromMillis) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt(DeviceMappingConstants.JSON_REGISTER_SECRET, secret);
        jsonObject.put(DeviceMappingConstants.JSON_MARK_ID_AS_STRING, markId);
        return getBaseCheckinJson(deviceUuid, pushDeviceId, fromMillis, jsonObject);
    }

    @NonNull
    public static JSONObject getBoatCheckinJson(@Nullable String secret, String boatId,
                                                String deviceUuid, String pushDeviceId,
                                                long fromMillis) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt(DeviceMappingConstants.JSON_REGISTER_SECRET, secret);
        jsonObject.put(DeviceMappingConstants.JSON_BOAT_ID_AS_STRING, boatId);
        return getBaseCheckinJson(deviceUuid, pushDeviceId, fromMillis, jsonObject);
    }

}
