package com.sap.sailing.android.buoy.positioning.app.util;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.CheckinData;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkInfo;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkPingInfo;
import com.sap.sailing.android.shared.data.http.HttpGetRequest;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.CheckinDataActivity;
import com.sap.sailing.android.shared.util.JsonHelper;
import com.sap.sailing.android.shared.util.NetworkHelper;
import com.sap.sailing.android.shared.util.NetworkHelper.NetworkHelperSuccessListener;
import com.sap.sailing.android.shared.util.UniqueDeviceUuid;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.impl.SharedDomainFactoryImpl;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.MarkDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.FlatGPSFixJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.FlatGPSFixJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.MarkJsonSerializerWithPosition;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckinManager {
    private final static String TAG = CheckinManager.class.getName();
    private final CheckinDataActivity<CheckinData> activity;
    private final Context mContext;
    private final AppPreferences prefs;
    private String url;
    private DataChangedListner dataChangedListner;
    private final SharedDomainFactory sharedDomainFactory;

    public CheckinManager(String url, Context context) {
        this(url, context, /* context is not necessarily a CheckinDataActivity */ null);
    }

    private CheckinManager(String url, Context context, CheckinDataActivity<CheckinData> activity) {
        sharedDomainFactory = new SharedDomainFactoryImpl(/* race log resolver not needed in this app */ null);
        this.url = url;
        mContext = context;
        prefs = new AppPreferences(context);
        this.activity = activity;
    }

    public CheckinManager(String url, CheckinDataActivity<CheckinData> activity) {
        this(url, activity, activity);
    }

    public SharedDomainFactory getSharedDomainFactory() {
        return sharedDomainFactory;
    }

    public void callServerAndGenerateCheckinData() {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        final URLData urlData = extractRequestParametersFromUri(uri, scheme);
        if (urlData == null) {
            setCheckinData(null);
            return;
        }
        if (activity != null) {
            activity.showProgressDialog(R.string.please_wait, R.string.getting_marks);
        }
        try {
            HttpGetRequest getLeaderboardRequest = new HttpGetRequest(new URL(urlData.getLeaderboardUrl), mContext);
            getLeaderBoardFromServer(urlData, getLeaderboardRequest);
        } catch (MalformedURLException e) {
            ExLog.e(mContext, TAG,
                    "Error: Failed to perform checking due to a MalformedURLException: " + e.getMessage());
        }
    }

    private URLData extractRequestParametersFromUri(Uri uri, String scheme) {
        assert uri != null;
        URLData urlData = new URLData();
        urlData.uriStr = uri.toString();
        urlData.server = scheme + "://" + uri.getHost();
        urlData.port = uri.getPort();
        urlData.hostWithPort = urlData.server + (urlData.port == -1 ? "" : (":" + urlData.port));
        String leaderboardNameFromQR = "";
        try {
            String defaultCharset = "UTF-8";
            // Secret
            String secretFromQR = uri.getQueryParameter(DeviceMappingConstants.URL_SECRET);
            leaderboardNameFromQR = URLEncoder
                    .encode(uri.getQueryParameter(DeviceMappingConstants.URL_LEADERBOARD_NAME), defaultCharset)
                    .replace("+", "%20");
            urlData.secret = secretFromQR;
            urlData.leaderboardName = leaderboardNameFromQR;
            urlData.deviceUuid = new SmartphoneUUIDIdentifierImpl(
                    UUID.fromString(UniqueDeviceUuid.getUniqueId(mContext)));
            Uri.Builder leaderboardBuilder = Uri.parse(urlData.hostWithPort).buildUpon()
                    .encodedPath(prefs.getServerLeaderboardPath(urlData.leaderboardName));
            if (secretFromQR != null) {
                leaderboardBuilder.appendQueryParameter(DeviceMappingConstants.URL_SECRET, secretFromQR);
            }
            urlData.getLeaderboardUrl = leaderboardBuilder.build().toString();
            Uri.Builder markBuilder = Uri.parse(urlData.hostWithPort).buildUpon()
                    .encodedPath(prefs.getServerMarkPath(urlData.leaderboardName));
            if (secretFromQR != null) {
                markBuilder.appendQueryParameter(DeviceMappingConstants.URL_SECRET, secretFromQR);
            }
            urlData.getMarkUrl = markBuilder.build().toString();
        } catch (UnsupportedEncodingException e) {
            ExLog.e(mContext, TAG, "Failed to encode leaderboard name: " + e.getMessage());
        } catch (NullPointerException e) {
            ExLog.e(mContext, TAG, "Invalid Barcode (no leaderboard-name set): " + e.getMessage());
            Toast.makeText(mContext, mContext.getString(R.string.error_invalid_qr_code), Toast.LENGTH_LONG).show();
            urlData = null;
        }
        return urlData;
    }

    private void getLeaderBoardFromServer(final URLData urlData, HttpGetRequest getLeaderboardRequest) {
        NetworkHelper.getInstance(mContext).executeHttpJsonRequestAsync(getLeaderboardRequest,
                new NetworkHelper.NetworkHelperSuccessListener() {
                    @Override
                    public void performAction(JSONObject response) {
                        final String leaderboardName;
                        try {
                            leaderboardName = response.getString("name");
                        } catch (JSONException e) {
                            ExLog.e(mContext, TAG, "Error getting data from call on URL: " + urlData.getLeaderboardUrl
                                    + ", Error: " + e.getMessage());
                            if (activity != null) {
                                activity.dismissProgressDialog();
                                displayAPIErrorRecommendRetry();
                            }
                            return;
                        }
                        final String leaderboardDisplayName = response.optString("displayName", leaderboardName);
                        HttpGetRequest getMarksRequest;
                        try {
                            getMarksRequest = new HttpGetRequest(new URL(urlData.getMarkUrl), mContext);
                            getMarksFromServer(leaderboardName, leaderboardDisplayName, getMarksRequest, urlData);
                        } catch (MalformedURLException e1) {
                            ExLog.e(mContext, TAG, "Error: Failed to perform checking due to a MalformedURLException: "
                                    + e1.getMessage());
                        }
                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {
                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        ExLog.e(mContext, TAG, "Failed to get event from API: " + e.getMessage());
                        if (activity != null) {
                            activity.dismissProgressDialog();
                            displayAPIErrorRecommendRetry();
                        }
                    }
                });
    }

    /**
     * @param leaderboardDisplayName
     *            the leaderboard's display name if one has been explicitly provided, otherwise the same as
     *            {@code leaderboardName}
     */
    private void getMarksFromServer(final String leaderboardName, final String leaderboardDisplayName,
            HttpGetRequest getMarksRequest, final URLData urlData) {
        NetworkHelper.getInstance(mContext).executeHttpJsonRequestAsync(getMarksRequest,
                new NetworkHelperSuccessListener() {

                    @Override
                    public void performAction(JSONObject response) {
                        try {
                            JSONArray markArray = response.getJSONArray("marks");
                            String checkinDigest = generateCheckinDigest(urlData.uriStr);
                            List<MarkInfo> marks = new ArrayList<>();
                            List<MarkPingInfo> pings = new ArrayList<>();
                            for (int i = 0; i < markArray.length(); i++) {
                                JSONObject jsonMark = (JSONObject) markArray.get(i);
                                org.json.simple.JSONObject simpleMark;
                                simpleMark = JsonHelper.convertToSimple(jsonMark);
                                MarkDeserializer markDeserializer = new MarkDeserializer(getSharedDomainFactory());
                                MarkInfo mark = MarkInfo.create(markDeserializer.deserialize(simpleMark),
                                        jsonMark.getString(MarkJsonSerializer.FIELD_CLASS), checkinDigest);
                                if (jsonMark.has(MarkJsonSerializerWithPosition.FIELD_POSITION)) {
                                    if (!jsonMark.get(MarkJsonSerializerWithPosition.FIELD_POSITION).equals(null)) {
                                        JSONObject positionJson = jsonMark
                                                .getJSONObject(MarkJsonSerializerWithPosition.FIELD_POSITION);
                                        FlatGPSFixJsonDeserializer deserializer = new FlatGPSFixJsonDeserializer();
                                        org.json.simple.JSONObject simplePosition;
                                        simplePosition = JsonHelper.convertToSimple(positionJson);
                                        GPSFix gpsFix = deserializer.deserialize(simplePosition);
                                        // accepts JSON messages without accuracy and with, without will simply be
                                        // displayed as "set"
                                        final MarkPingInfo ping;
                                        if (!positionJson.has(FlatGPSFixJsonSerializer.FIELD_ACCURACY)
                                                || positionJson.getDouble(
                                                        FlatGPSFixJsonSerializer.FIELD_ACCURACY) == FlatGPSFixJsonSerializer.NOT_AVAILABLE_THROUGH_SERVER) {
                                            ping = new MarkPingInfo(mark.getId(), gpsFix,
                                                    FlatGPSFixJsonSerializer.NOT_AVAILABLE_THROUGH_SERVER);
                                        } else {
                                            ping = new MarkPingInfo(mark.getId(), gpsFix,
                                                    positionJson.getDouble(FlatGPSFixJsonSerializer.FIELD_ACCURACY));
                                        }
                                        if (ping != null) {
                                            pings.add(ping);
                                        }
                                    }
                                }
                                marks.add(mark);
                            }
                            urlData.marks = marks;
                            urlData.pings = pings;
                            saveCheckinDataAndNotifyListeners(urlData, leaderboardName, leaderboardDisplayName);

                        } catch (JSONException | ParseException | JsonDeserializationException e) {
                            ExLog.e(mContext, TAG, "Error getting data from call on URL: " + urlData.getMarkUrl
                                    + ", Error: " + e.getMessage());
                            if (activity != null) {
                                activity.dismissProgressDialog();
                                displayAPIErrorRecommendRetry();
                            }
                            return;
                        }

                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {

                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        ExLog.e(mContext, TAG, "Failed to get marks from API: " + e.getMessage());
                        if (activity != null) {
                            activity.dismissProgressDialog();
                            displayAPIErrorRecommendRetry();
                        }
                    }
                });
    }

    /**
     * @param leaderboardDisplayName
     *            the leaderboard's display name if one has been explicitly provided, otherwise the same as
     *            {@code leaderboardName}
     */
    private void saveCheckinDataAndNotifyListeners(URLData urlData, String leaderboardName,
            String leaderboardDisplayName) {
        CheckinData data = new CheckinData();
        data.serverWithPort = urlData.hostWithPort;
        data.leaderboardName = leaderboardName;
        data.leaderboardDisplayName = leaderboardDisplayName;
        data.marks = urlData.marks;
        data.pings = urlData.pings;
        data.deviceUid = urlData.deviceUuid.getStringRepresentation();
        data.uriString = urlData.uriStr;
        try {
            data.setCheckinDigestFromString(urlData.uriStr);
            if (activity != null) {
                activity.dismissProgressDialog();
            }
            setCheckinData(data);
        } catch (UnsupportedEncodingException e) {
            ExLog.e(mContext, TAG,
                    "Failed to get generate digest of qr-code string (" + urlData.uriStr + "). " + e.getMessage());
            if (activity != null) {
                activity.dismissProgressDialog();
                displayAPIErrorRecommendRetry();
            }
        } catch (NoSuchAlgorithmException e) {
            ExLog.e(mContext, TAG,
                    "Failed to get generate digest of qr-code string (" + urlData.uriStr + "). " + e.getMessage());
            if (activity != null) {
                activity.dismissProgressDialog();
                displayAPIErrorRecommendRetry();
            }
        }
    }

    private void setCheckinData(CheckinData data) {
        if (activity != null) {
            activity.onCheckinDataAvailable(data);
        } else if (dataChangedListner != null) {
            dataChangedListner.handleData(data);
        }
    }

    public interface DataChangedListner {
        void handleData(CheckinData data);
    }

    public void setDataChangedListner(DataChangedListner listener) {
        dataChangedListner = listener;
    }

    /**
     * Shows a pop-up-dialog that informs the user than an API-call has failed and recommends a retry.
     */
    private void displayAPIErrorRecommendRetry() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(mContext.getString(R.string.notify_user_api_call_failed));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }

        });
        builder.show();
        setCheckinData(null);
    }

    private String generateCheckinDigest(String url) {
        String checkinDigest = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(url.getBytes("UTF-8"));
            byte[] digest = md.digest();
            StringBuilder buf = new StringBuilder();
            for (byte byt : digest) {
                buf.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
                checkinDigest = buf.toString();
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Exception trying to generate check-in digest", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Exception trying to generate check-in digest", e);
        }
        return checkinDigest;
    }

    private class URLData {
        public String uriStr;
        public String server;
        public int port;
        public String hostWithPort;
        public String secret;
        public List<MarkInfo> marks;
        public List<MarkPingInfo> pings;
        public String leaderboardName;
        public DeviceIdentifier deviceUuid;
        public String getMarkUrl;
        public String getLeaderboardUrl;
    }
}
