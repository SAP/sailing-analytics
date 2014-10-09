package com.sap.sailing.android.tracking.app.services;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.ui.activities.LaunchActivity;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.server.gateway.deserialization.impl.FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer;

public class TrackingService extends Service implements ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener {

    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private NotificationManager notificationManager;
    private boolean locationUpdateRequested = false;
    private final AppPreferences prefs = new AppPreferences(this);

    private static final String TAG = TrackingService.class.getName();

    public static final String WEB_SERVICE_PATH = "/tracking/recordFixesFlatJson";
    public static final int FIX_INTERVAL_MS = 500;
    public static final int FASTEST_FIX_INTERVAL_MS = 100;
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION_ID = R.string.tracker_started;

    @Override
    public void onCreate() {
        super.onCreate();

        // http://developer.android.com/training/location/receive-location-updates.html
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(FIX_INTERVAL_MS);
        locationRequest.setFastestInterval(FASTEST_FIX_INTERVAL_MS);

        locationClient = new LocationClient(this, this, this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTracking();
        return Service.START_STICKY;
    }

    public void startTracking() {
        locationClient.connect();
        locationUpdateRequested = true;

        ExLog.i(this, TAG, "Started Tracking");
        showNotification();
    }

    public void stopTracking() {
        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(this);
        }
        locationClient.disconnect();
        locationUpdateRequested = false;
        ExLog.i(this, TAG, "Stopped Tracking");
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        ExLog.e(this, TAG, "Failed to connect to Google Play Services for location updates");
    }

    @Override
    public void onConnected(Bundle arg0) {
        if (locationUpdateRequested) {
            locationClient.requestLocationUpdates(locationRequest, this);
        }
    }

    @Override
    public void onDisconnected() {
        ExLog.i(this, TAG, "LocationClient was disconnected");
    }

    private String getWebServiceURL() {
        return prefs.getServerURL() + WEB_SERVICE_PATH;
    }

    @Override
    public void onLocationChanged(Location location) {
        JSONObject json = new JSONObject();
        try {
            json.put(FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer.DEVICE_UUID,
                    prefs.getDeviceIdentifier());
            json.put(FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer.LAT_DEG, location.getLatitude());
            json.put(FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer.LON_DEG, location.getLongitude());
            json.put(FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer.TIME_MILLIS, location.getTime());
            json.put(FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer.SPEED_M_PER_S, location.getSpeed());
            json.put(FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer.BEARING_DEG, location.getBearing());
        } catch (JSONException e) {
            ExLog.e(this, TAG, "Error serializing fix: " + e.getMessage());
        }
        String jsonString = json.toString();
        
        startService(MessageSendingService.createMessageIntent(this, getWebServiceURL(),
                null, UUID.randomUUID(), jsonString, null));
        
        // TODO also store to SD card
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopTracking();
        notificationManager.cancel(NOTIFICATION_ID);
        Toast.makeText(this, R.string.tracker_stopped, Toast.LENGTH_SHORT).show();
    }

    private void showNotification() {
        CharSequence text = getText(R.string.tracker_started);
        Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
        Intent i = new Intent(this, LaunchActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        notification.setLatestEventInfo(this, getText(R.string.app_name), text, pi);
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(NOTIFICATION_ID, notification);
    }
}