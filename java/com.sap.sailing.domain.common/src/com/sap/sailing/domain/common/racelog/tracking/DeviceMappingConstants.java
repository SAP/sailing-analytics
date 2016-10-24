package com.sap.sailing.domain.common.racelog.tracking;

import com.sap.sse.common.util.UrlHelper;

/**
 * Shared between GWT and Android. Used for creating and deciphering the URL encoded by the QRCode, which gives the
 * tracking app all necessary information for creating the device mapping of either the race or leaderboard/regatta.
 * <p>
 * The field that server for mapping a device for an individual race are deprecated (see
 * {@link DeviceMappingOnRaceQRCodeWidget}).
 * <p>
 * The structure of the URL is documented in the <a
 * href="http://wiki.sapsailing.com/wiki/info/api/api-v1">Wiki</a>.
 * {@code http://<host>/tracking/checkin?event_id=<e>&leaderboard_name=<l>&competitor_id=<c>}
 * 
 * @author Fredrik Teschke
 */
public class DeviceMappingConstants {
    // According to the HTTP protocol definition (http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.2.3), only
    // schema and host are case insensitive.
    // But to adhere to best practices, and due to the deficiencies of some web servers and clients,
    // all URL components should be treated as case insensitive, preferring _underscores_ to CamelCase.
    public static final String TRACKING_URL_BASE = "/tracking/checkin";
    public static final String BUOY_TENDER_URL_BASE = "/buoy-tender/checkin";
    public static final String URL_EVENT_ID = "event_id";
    public static final String URL_LEADERBOARD_NAME = "leaderboard_name";
    public static final String URL_COMPETITOR_ID_AS_STRING = "competitor_id";
    public static final String URL_MARK_ID_AS_STRING = "mark_id";
    public static final String URL_FROM_MILLIS = "from_millis";
    public static final String URL_TO_MILLIS = "to_millis";

    public static final String JSON_COMPETITOR_ID_AS_STRING = "competitorId";
    public static final String JSON_MARK_ID_AS_STRING = "markId";
    public static final String JSON_DEVICE_UUID = "deviceUuid";
    public static final String JSON_DEVICE_TYPE = "deviceType";
    public static final String JSON_PUSH_DEVICE_ID = "pushDeviceId";
    public static final String JSON_FROM_MILLIS = "fromMillis";
    public static final String JSON_TO_MILLIS = "toMillis";
    public static final String JSON_TEAM_IMAGE_URI = "teamImageUri";

    public static String getDeviceMappingForRegattaLogUrl(String serverUrlWithoutTrailingSlash, String eventId,
            String leaderboardName, String mappedItemType, String mappedItemId, UrlHelper helper) {
        return serverUrlWithoutTrailingSlash + TRACKING_URL_BASE + "?" + URL_EVENT_ID + "=" + helper.encodeQueryString(eventId) + "&"
                + URL_LEADERBOARD_NAME + "=" + helper.encodeQueryString(leaderboardName) + "&" + helper.encodeQueryString(mappedItemType) + "="
                + helper.encodeQueryString(mappedItemId);
    }
    
    public static String getBuoyTenderInvitationUrl(String serverUrlWithoutTrailingSlash,
            String leaderboardName, String eventId, UrlHelper helper) {
        return serverUrlWithoutTrailingSlash + BUOY_TENDER_URL_BASE + "?" + URL_EVENT_ID + "="
                + helper.encodeQueryString(eventId) + "&" + URL_LEADERBOARD_NAME + "=" + helper.encodeQueryString(leaderboardName);
    }
}
