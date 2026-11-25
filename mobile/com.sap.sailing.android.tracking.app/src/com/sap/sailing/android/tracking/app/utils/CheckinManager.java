package com.sap.sailing.android.tracking.app.utils;

import android.content.DialogInterface;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.sap.sailing.android.shared.data.http.HttpGetRequest;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.CheckinDataActivity;
import com.sap.sailing.android.shared.util.JsonHelper;
import com.sap.sailing.android.shared.util.NetworkHelper;
import com.sap.sailing.android.shared.util.UniqueDeviceUuid;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.valueobjects.BoatCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.BoatUrlData;
import com.sap.sailing.android.tracking.app.valueobjects.CheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CompetitorCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CompetitorUrlData;
import com.sap.sailing.android.tracking.app.valueobjects.MarkCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.MarkUrlData;
import com.sap.sailing.android.tracking.app.valueobjects.UrlData;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.impl.SharedDomainFactoryImpl;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;
import com.sap.sailing.domain.common.tracking.impl.CompetitorJsonConstants;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.MarkDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.BoatJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardGroupBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.TrackingConnectorInfoJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sse.common.Util;
import com.sap.sse.shared.media.ImageDescriptor;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;

public class CheckinManager {

    private final static String TAG = CheckinManager.class.getName();
    private CheckinDataActivity<CheckinData> activity;
    private AppPreferences prefs;
    private UrlData urlData;
    private String url;
    private boolean update;

    public CheckinManager(String url, CheckinDataActivity<CheckinData> activity, boolean update) {
        this.activity = activity;
        this.url = url;
        prefs = new AppPreferences(activity);
        this.update = update;
    }

    public void callServerAndGenerateCheckinData() {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        urlData = extractRequestParametersFromUri(uri, scheme);
        if (urlData == null) {
            setCheckinData(null);
            return;
        }

        activity.showProgressDialog(R.string.please_wait, R.string.getting_event);

        try {
            HttpGetRequest getLeaderboardRequest = new HttpGetRequest(new URL(urlData.leaderboardUrl), activity);
            getLeaderBoardFromServer(getLeaderboardRequest);

        } catch (MalformedURLException e) {
            ExLog.e(activity, TAG,
                    "Error: Failed to perform checking due to a MalformedURLException: " + e.getMessage());
            handleApiError();
        }
    }

    private UrlData extractRequestParametersFromUri(Uri uri, String scheme) {
        UrlData urlData = null;
        String server = scheme + "://" + uri.getHost();
        int port = uri.getPort();
        Exception exception = null;
        try {
            String defaultCharset = "UTF-8";
            // Secret
            String secretFromQR = uri.getQueryParameter(DeviceMappingConstants.URL_SECRET);
            String urlEncodedLeaderboardNameFromQR = URLEncoder
                    .encode(uri.getQueryParameter(DeviceMappingConstants.URL_LEADERBOARD_NAME), defaultCharset)
                    .replace("+", "%20");
            Set<String> parameterNames = uri.getQueryParameterNames();
            if (parameterNames.contains(DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING)) {
                CompetitorUrlData competitorUrlData = new CompetitorUrlData(server, port);
                competitorUrlData.competitorId = URLEncoder.encode(
                        uri.getQueryParameter(DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING), defaultCharset);
                Uri.Builder builder = Uri.parse(competitorUrlData.hostWithPort).buildUpon()
                        .encodedPath(prefs.getServerCompetitorPath(competitorUrlData.competitorId));
                if (secretFromQR != null) {
                    builder.appendQueryParameter(DeviceMappingConstants.URL_SECRET, secretFromQR);
                    builder.appendQueryParameter(DeviceMappingConstants.URL_LEADERBOARD_NAME_FOR_SECRET, URLDecoder.decode(urlEncodedLeaderboardNameFromQR, "UTF-8"));
                }
                competitorUrlData.competitorUrl = builder.build().toString();
                urlData = competitorUrlData;
            } else if (parameterNames.contains(DeviceMappingConstants.URL_MARK_ID_AS_STRING)) {
                MarkUrlData markUrlData = new MarkUrlData(server, port);
                markUrlData.setMarkId(URLEncoder
                        .encode(uri.getQueryParameter(DeviceMappingConstants.URL_MARK_ID_AS_STRING), defaultCharset));
                Uri.Builder builder = Uri.parse(markUrlData.hostWithPort).buildUpon()
                        .encodedPath(prefs.getServerMarkPath(urlEncodedLeaderboardNameFromQR, markUrlData.getMarkId()));
                if (secretFromQR != null) {
                    builder.appendQueryParameter(DeviceMappingConstants.URL_SECRET, secretFromQR);
                }
                markUrlData.setMarkUrl(builder.build().toString());
                urlData = markUrlData;
            } else if (parameterNames.contains(DeviceMappingConstants.URL_BOAT_ID_AS_STRING)) {
                BoatUrlData boatUrlData = new BoatUrlData(server, port);
                boatUrlData.setBoatId(URLEncoder
                        .encode(uri.getQueryParameter(DeviceMappingConstants.URL_BOAT_ID_AS_STRING), defaultCharset));
                Uri.Builder builder = Uri.parse(boatUrlData.hostWithPort).buildUpon()
                        .encodedPath(prefs.getServerBoatPath(boatUrlData.getBoatId()));
                if (secretFromQR != null) {
                    builder.appendQueryParameter(DeviceMappingConstants.URL_SECRET, secretFromQR);
                    builder.appendQueryParameter(DeviceMappingConstants.URL_LEADERBOARD_NAME_FOR_SECRET, URLDecoder.decode(urlEncodedLeaderboardNameFromQR, "UTF-8"));
                }
                boatUrlData.setBoatUrl(builder.build().toString());
                urlData = boatUrlData;
            } else {
                ExLog.e(activity, TAG, "Neither competitor, boat or boat checkin");
                exception = new Exception();
            }
            if (urlData != null) {
                urlData.uriStr = uri.toString();
                urlData.checkinURLStr = Uri.parse(urlData.hostWithPort).buildUpon()
                        .encodedPath(prefs.getServerCheckinPath().replace("{leaderboard-name}", urlEncodedLeaderboardNameFromQR))
                        .build().toString();
                urlData.secret = secretFromQR;
                urlData.eventId = URLEncoder.encode(uri.getQueryParameter(DeviceMappingConstants.URL_EVENT_ID),
                        defaultCharset);
                urlData.leaderboardName = urlEncodedLeaderboardNameFromQR;
                urlData.deviceUuid = new SmartphoneUUIDIdentifierImpl(
                        UUID.fromString(UniqueDeviceUuid.getUniqueId(activity)));
                Uri.Builder eventBuilder = Uri.parse(urlData.hostWithPort).buildUpon()
                        .encodedPath(prefs.getServerEventPath(urlData.eventId));
                if (secretFromQR != null) {
                    eventBuilder.appendQueryParameter(DeviceMappingConstants.URL_SECRET, secretFromQR);
                }
                urlData.eventUrl = eventBuilder.build().toString();
                Uri.Builder leaderboardBuilder = Uri.parse(urlData.hostWithPort).buildUpon()
                        .encodedPath(prefs.getServerLeaderboardPath(urlData.leaderboardName));
                if (secretFromQR != null) {
                    leaderboardBuilder.appendQueryParameter(DeviceMappingConstants.URL_SECRET, secretFromQR);
                }
                urlData.leaderboardUrl = leaderboardBuilder.build().toString();
            }
        } catch (UnsupportedEncodingException e) {
            ExLog.e(activity, TAG, "Failed to encode leaderboard name: " + e.getMessage());
            exception = e;
        } catch (NullPointerException e) {
            ExLog.e(activity, TAG, "Invalid Barcode (no leaderboard-name set or missing parameter): " + e.getMessage());
            exception = e;
        }
        if (exception != null) {
            Toast.makeText(activity, activity.getString(R.string.error_invalid_qr_code), Toast.LENGTH_LONG).show();
            prefs.setLastScannedQRCode(null);
            urlData = null;
        }
        return urlData;
    }

    private void getLeaderBoardFromServer(HttpGetRequest request) {
        NetworkHelper.getInstance(activity).executeHttpJsonRequestAsync(request,
                new NetworkHelper.NetworkHelperSuccessListener() {
                    @Override
                    public void performAction(JSONObject response) {
                        final String leaderboardDisplayName = response.optString("displayName",
                                urlData.leaderboardName);
                        HttpGetRequest getEventRequest;
                        try {
                            getEventRequest = new HttpGetRequest(new URL(urlData.eventUrl), activity);
                            getEventFromServer(getEventRequest, leaderboardDisplayName);
                        } catch (MalformedURLException e1) {
                            ExLog.e(activity, TAG, "Error: Failed to perform checking due to a MalformedURLException: "
                                    + e1.getMessage());
                            handleApiError();
                        }
                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {
                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        ExLog.e(activity, TAG, "Failed to get event from API: " + e.getMessage());
                        handleApiError();
                    }
                });
    }

    private void getEventFromServer(HttpGetRequest request, final String leaderboardDisplayName) {
        NetworkHelper.getInstance(activity).executeHttpJsonRequestAsync(request,
                new NetworkHelper.NetworkHelperSuccessListener() {
                    @Override
                    public void performAction(JSONObject response) {
                        EventBaseJsonDeserializer deserializer = new EventBaseJsonDeserializer(
                                new VenueJsonDeserializer(new CourseAreaJsonDeserializer(
                                        new SharedDomainFactoryImpl(new RaceLogResolver() {
                                            @Override
                                            public RaceLog resolve(SimpleRaceLogIdentifier identifier) {
                                                return null;
                                            }
                                        }))),
                                new LeaderboardGroupBaseJsonDeserializer(),
                                new TrackingConnectorInfoJsonDeserializer());
                        try {
                            final EventBase event = deserializer.deserialize(
                                    (org.json.simple.JSONObject) new JSONParser().parse(response.toString()));
                            urlData.eventId = event.getId().toString();
                            urlData.eventName = event.getName();
                            urlData.eventStartDateStr = (event.getStartDate() != null)
                                    ? "" + event.getStartDate().asMillis()
                                    : "0";
                            urlData.eventEndDateStr = (event.getEndDate() != null) ? "" + event.getEndDate().asMillis()
                                    : "0";
                            Iterable<ImageDescriptor> imageUrls = event.getImages();
                            if (!Util.isEmpty(imageUrls)) {
                                urlData.eventFirstImageUrl = imageUrls.iterator().next().getURL().toString();
                            } else {
                                urlData.eventFirstImageUrl = null;
                            }
                        } catch (JsonDeserializationException | ParseException e) {
                            ExLog.e(activity, TAG, "Error getting data from call on URL: " + urlData.eventUrl
                                    + ", Error: " + e.getMessage());
                            handleApiError();
                            return;
                        }
                        HttpGetRequest getRequest;
                        if (urlData instanceof CompetitorUrlData) {
                            CompetitorUrlData competitorUrlData = (CompetitorUrlData) urlData;
                            try {
                                getRequest = new HttpGetRequest(new URL(competitorUrlData.competitorUrl), activity);
                                getCompetitorFromServer(getRequest, competitorUrlData, leaderboardDisplayName);
                            } catch (MalformedURLException e2) {
                                ExLog.e(activity, TAG,
                                        "Error: Failed to perform checking due to a MalformedURLException: "
                                                + e2.getMessage());
                                handleApiError();
                            }
                        } else if (urlData instanceof MarkUrlData) {
                            MarkUrlData markUrlData = (MarkUrlData) urlData;
                            try {
                                getRequest = new HttpGetRequest(new URL(markUrlData.getMarkUrl()), activity);
                                getMarkFromServer(getRequest, markUrlData, leaderboardDisplayName);
                            } catch (MalformedURLException exception) {
                                ExLog.e(activity, TAG,
                                        "Error: Failed to perform checking due to a MalformedURLException: "
                                                + exception.getMessage());
                                handleApiError();
                            }
                        } else if (urlData instanceof BoatUrlData) {
                            BoatUrlData boatUrlData = (BoatUrlData) urlData;
                            try {
                                getRequest = new HttpGetRequest(new URL(boatUrlData.getBoatUrl()), activity);
                                getBoatFromServer(getRequest, boatUrlData, leaderboardDisplayName);
                            } catch (MalformedURLException exception) {
                                ExLog.e(activity, TAG,
                                        "Error: Failed to perform checking du to a MalformedURLException: "
                                                + exception.getMessage());
                                handleApiError();
                            }
                        }

                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {

                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        ExLog.e(activity, TAG, "Failed to get leaderboard from API: " + e.getMessage());
                        handleApiError();
                    }
                });
    }

    private void getCompetitorFromServer(HttpGetRequest request, final CompetitorUrlData data,
            final String leaderboardDisplayName) {
        NetworkHelper.getInstance(activity).executeHttpJsonRequestAsync(request,
                new NetworkHelper.NetworkHelperSuccessListener() {
                    @Override
                    public void performAction(JSONObject response) {
                        activity.dismissProgressDialog();
                        try {
                            data.competitorName = response.getString(CompetitorJsonConstants.FIELD_NAME);
                            data.competitorId = response.getString(CompetitorJsonConstants.FIELD_ID);
                            if (response.has(CompetitorJsonConstants.FIELD_SAIL_ID)) {
                                data.competitorSailId = response.getString(CompetitorJsonConstants.FIELD_SAIL_ID);
                            } else if (response.has(CompetitorJsonConstants.FIELD_SHORT_NAME)) {
                                data.competitorSailId = response.getString(CompetitorJsonConstants.FIELD_SHORT_NAME);
                            } else {
                                data.competitorSailId = "n/a";
                            }
                            // TODO read CompetitorJsonConstants.FIELD_SHORT_NAME and manage as an alternative to a
                            // non-existing sail ID
                            data.competitorNationality = response.getString(CompetitorJsonConstants.FIELD_NATIONALITY);
                            data.competitorCountryCode = response.getString(CompetitorJsonConstants.FIELD_COUNTRY_CODE);
                            // TODO Bug 3358: get optional team image from CompetitorJsonConstants.FIELD_TEAM_IMAGE_URI
                        } catch (JSONException e) {
                            ExLog.e(activity, TAG, "Error getting data from call on URL: " + data.competitorUrl
                                    + ", Error: " + e.getMessage());
                            handleApiError();
                            return;
                        }
                        saveCheckinDataAndNotifyListeners(data, leaderboardDisplayName);
                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {
                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        ExLog.e(activity, TAG, "Failed to get competitor from API: " + e.getMessage());
                        handleApiError();
                    }
                });
    }

    private void getMarkFromServer(HttpGetRequest request, final MarkUrlData data,
            final String leaderboardDisplayName) {
        NetworkHelper.getInstance(activity).executeHttpJsonRequestAsync(request,
                new NetworkHelper.NetworkHelperSuccessListener() {
                    @Override
                    public void performAction(JSONObject response) {
                        activity.dismissProgressDialog();
                        try {
                            org.json.simple.JSONObject simpleMark = JsonHelper.convertToSimple(response);
                            MarkDeserializer markDeserializer = new MarkDeserializer(new SharedDomainFactoryImpl(null));
                            data.setMark(markDeserializer.deserialize(simpleMark));
                        } catch (ParseException | JsonDeserializationException e) {
                            ExLog.e(activity, TAG, "Failed to deserialize mark");
                            handleApiError();
                            return;
                        }
                        saveCheckinDataAndNotifyListeners(data, leaderboardDisplayName);
                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {
                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        ExLog.e(activity, TAG, "Failed to get mark from server: " + e.getMessage());
                        handleApiError();
                    }
                });
    }

    private void getBoatFromServer(HttpGetRequest request, final BoatUrlData data,
            final String leaderboardDisplayName) {
        NetworkHelper.getInstance(activity).executeHttpJsonRequestAsync(request,
                new NetworkHelper.NetworkHelperSuccessListener() {
                    @Override
                    public void performAction(JSONObject response) {
                        activity.dismissProgressDialog();
                        try {
                            org.json.simple.JSONObject simpleBoat = JsonHelper.convertToSimple(response);
                            BoatJsonDeserializer boatDeserializer = BoatJsonDeserializer
                                    .create(new SharedDomainFactoryImpl(null));
                            data.setBoat(boatDeserializer.deserialize(simpleBoat));
                        } catch (ParseException | JsonDeserializationException e) {
                            ExLog.e(activity, TAG, "Failed to deserialize boat");
                            handleApiError();
                            return;
                        }
                        saveCheckinDataAndNotifyListeners(data, leaderboardDisplayName);
                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {
                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        ExLog.e(activity, TAG, "Failed to get boat from server: " + e.getMessage());
                        handleApiError();
                    }
                });
    }

    private void saveCheckinDataAndNotifyListeners(UrlData data, String leaderboardDisplayName) {
        CheckinData checkinData;
        if (data instanceof CompetitorUrlData) {
            CompetitorUrlData competitorUrlData = (CompetitorUrlData) data;
            checkinData = new CompetitorCheckinData(competitorUrlData, leaderboardDisplayName);
        } else if (data instanceof BoatUrlData) {
            BoatUrlData boatUrlData = (BoatUrlData) data;
            checkinData = new BoatCheckinData(boatUrlData, leaderboardDisplayName);
        } else {
            MarkUrlData markUrlData = (MarkUrlData) data;
            checkinData = new MarkCheckinData(markUrlData, leaderboardDisplayName);
        }
        checkinData.setUpdate(update);
        try {
            checkinData.setCheckinDigestFromString(data.uriStr);
            activity.dismissProgressDialog();
            setCheckinData(checkinData);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            ExLog.e(activity, TAG,
                    "Failed to get generate digest of qr-code string (" + data.uriStr + "). " + e.getMessage());
            handleApiError();
        }
    }

    private void setCheckinData(CheckinData data) {
        activity.onCheckinDataAvailable(data);
    }

    private void handleApiError() {
        prefs.setLastScannedQRCode(null);
        if (activity != null) {
            activity.dismissProgressDialog();
        }
        displayAPIErrorRecommendRetry();
        setCheckinData(null);
    }

    /**
     * Shows a pop-up-dialog that informs the user than an API-call has failed and recommends a retry.
     */
    private void displayAPIErrorRecommendRetry() {
        setCheckinData(null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(activity.getString(R.string.notify_user_api_call_failed));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }

        });
        builder.show();
    }

}
