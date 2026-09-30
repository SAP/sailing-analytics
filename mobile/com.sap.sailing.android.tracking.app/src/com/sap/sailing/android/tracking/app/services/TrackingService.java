package com.sap.sailing.android.tracking.app.services;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.android.shared.ui.customviews.GPSQuality;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.android.tracking.app.utils.DatabaseHelper;
import com.sap.sailing.android.tracking.app.valueobjects.EventInfo;
import com.sap.sse.common.impl.MeterPerSecondSpeedImpl;
import com.sap.sailing.domain.common.tracking.impl.FlatSmartphoneUuidAndGPSFixMovingJsonSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class TrackingService extends Service implements LocationListener {

    private static final String TAG = TrackingService.class.getName();
    private static final String THREAD_NAME = "Location WatchDog";

    private static final int NO_LOCATION = 0;
    private static final long NO_LOCATION_CHECK = Duration.ONE_SECOND.times(5).asMillis();

    private static final int POOR_DISTANCE = 48;
    private static final int GREAT_DISTANCE = 10;
    private static final int NO_DISTANCE = 0;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 40;

    private AppPreferences prefs;

    private GPSQualityListener gpsQualityListener;
    private final IBinder trackingBinder = new TrackingBinder();

    public final static int UPDATE_INTERVAL_IN_MILLIS_DEFAULT = 1000;
    public final static String GPS_DISABLED_MESSAGE = "gpsDisabled";

    private String checkinDigest;
    private EventInfo event;

    private LocationManager locationManager;
    private LocationWatchDog locationWatchDog;

    /**
     * Must be synchronized upon while modifying the {@link #timerForDelayingSendingMessages} field and while modifying
     * the {@link #locationsQueuedBasedOnSendingInterval} list.
     */
    private final Object messageSendingTimerMonitor = new Object();

    /**
     * If {@code null}, no timer is currently active for sending out messages queued in
     * {@link #locationsQueuedBasedOnSendingInterval} after the send interval, and the next message sending intent
     * arriving can be forwarded to the sending service immediately, with the timer being started immediately afterwards
     * to delay the sending of messages arriving later until the resend interval has expired. If not {@code null},
     * messages that arrive in {@link #enqueueForSending(String, Location)} will be appended to
     * {@link #locationsQueuedBasedOnSendingInterval}.
     * <p>
     *
     * When the timer "rings" it acquires the {@link #messageSendingTimerMonitor} and checks for messages to send. If
     * messages are found, they are removed from the {@link #locationsQueuedBasedOnSendingInterval} list, the monitor is
     * released and the messages are then forwarded to the sending service. The timer remains running. Otherwise, the
     * timer stops itself, sets this field to {@code null} and releases the monitor.
     */
    private Timer timerForDelayingSendingMessages;

    /**
     * When a {@link Location} is added and {@link #timerForDelayingSendingMessages} is {@code null}, a new timer will
     * be created and assigned to {@link #timerForDelayingSendingMessages}. Otherwise, we can assume that the existing
     * timer will pick up this new element upon its next turn.
     */
    private LinkedHashMap<String, List<Location>> locationsQueuedBasedOnSendingInterval;

    @Override
    public void onCreate() {
        locationsQueuedBasedOnSendingInterval = new LinkedHashMap<>();
        prefs = new AppPreferences(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            if (BuildConfig.DEBUG) {
                ExLog.i(this, TAG, "Starting Tracking Service with checkinDigest: " + checkinDigest);
            }

            if (intent.getAction() != null) {
                if (intent.getAction().equals(getString(R.string.tracking_service_stop))) {
                    stopTracking();
                } else {
                    if (intent.getExtras() != null) {
                        checkinDigest = intent.getExtras()
                                .getString(getString(R.string.tracking_service_checkin_digest_parameter));

                        event = DatabaseHelper.getInstance().getEventInfo(this, checkinDigest);

                        if (BuildConfig.DEBUG) {
                            ExLog.i(this, TAG, "Starting Tracking Service with checkinDigest: " + checkinDigest);
                        }
                        startTracking();
                    }
                }
            } else {
                stopTracking();
            }
        }
        return Service.START_STICKY;
    }

    @SuppressWarnings("MissingPermission")
    private void startTracking() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_IN_MILLIS_DEFAULT, 0f, this);
        ExLog.i(this, TAG, "Started Tracking");

        prefs.setTrackerIsTracking(true);
        prefs.setTrackerIsTrackingCheckinDigest(checkinDigest);

        if (locationWatchDog == null) {
            // create new Location WatchDog
            HandlerThread thread = new HandlerThread(THREAD_NAME, THREAD_PRIORITY_BACKGROUND);
            thread.start();
            locationWatchDog = new LocationWatchDog(thread.getLooper());

            // start WatchDog
            Message msg = locationWatchDog.obtainMessage(NO_LOCATION);
            msg.obj = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationWatchDog.sendMessageDelayed(msg, NO_LOCATION_CHECK);
        }
    }

    private void stopTracking() {
        locationManager.removeUpdates(this);

        prefs.setTrackerIsTracking(false);
        prefs.setTrackerIsTrackingCheckinDigest(null);

        if (locationWatchDog != null) {
            // stop the Location WatchDog
            locationWatchDog.getLooper().quit();
            locationWatchDog = null;
        }

        stopSelf();
        ExLog.i(this, TAG, "Stopped Tracking");
    }

    private void reportGPSQualityBearingAndSpeed(float gpsAccuracy, float bearing, float speed, double latitude,
            double longitude, double altitude) {
        Bearing bearingImpl = null;
        Speed speedImpl = null;

        if (bearing != 0.0) {
            bearingImpl = new DegreeBearingImpl(bearing);
        }

        if (speed > 0.0) {
            speedImpl = new MeterPerSecondSpeedImpl(speed);
        }

        if (prefs.getDisplayHeadingWithSubtractedDeclination() && bearingImpl != null) {
            GeomagneticField geomagneticField = new GeomagneticField((float) latitude, (float) longitude,
                    (float) altitude, System.currentTimeMillis());
            bearingImpl.add(new DegreeBearingImpl(-geomagneticField.getDeclination()));
        }

        if (gpsQualityListener != null) {
            GPSQuality quality = GPSQuality.noSignal;
            if (gpsAccuracy <= NO_DISTANCE) {
                quality = GPSQuality.noSignal;
            } else if (gpsAccuracy > POOR_DISTANCE) {
                quality = GPSQuality.poor;
            } else if (gpsAccuracy > GREAT_DISTANCE) {
                quality = GPSQuality.good;
            } else if (gpsAccuracy <= GREAT_DISTANCE) {
                quality = GPSQuality.great;
            }

            gpsQualityListener.gpsQualityAndAccuracyUpdated(quality, gpsAccuracy, bearingImpl, speedImpl);
        }
    }

    private JSONObject createJsonLocationFix(Location location) throws JSONException {
        JSONObject fixJson = new JSONObject();
        fixJson.put(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.BEARING_DEG, location.getBearing());
        fixJson.put(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TIME_MILLIS, location.getTime());
        fixJson.put(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.SPEED_M_PER_S, location.getSpeed());
        fixJson.put(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.LON_DEG, location.getLongitude());
        fixJson.put(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.LAT_DEG, location.getLatitude());
        return fixJson;
    }

    private JSONArray createJsonLocationFixes(Iterable<Location> locations) throws JSONException {
        final JSONArray fixesAsJson = new JSONArray();
        for (final Location location : locations) {
            fixesAsJson.put(createJsonLocationFix(location));
        }
        return fixesAsJson;
    }

    private JSONObject createFixesMessage(Iterable<Location> locations) throws JSONException {
        final JSONObject fixesMessage = new JSONObject();
        final JSONArray fixesJson = createJsonLocationFixes(locations);
        fixesMessage.put(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.FIXES, fixesJson);
        fixesMessage.put(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.DEVICE_UUID, prefs.getDeviceIdentifier());
        return fixesMessage;
    }

    /**
     * Based on the {@link #prefs} and the {@link AppPreferences#getMessageSendingIntervalInMillis()} the message
     * sending intent is either immediately forwarded to the message sending service or it is enqueued for a timer to
     * pick it up in a bulk operation later, after the sending interval has expired.
     *
     * @param postUrl
     *            URL to send fixes
     * @param location
     *            the location fix to enqueue for sending to the URL specified by {@code postUrl}
     */
    private void enqueueForSending(String postUrl, Location location) {
        synchronized (messageSendingTimerMonitor) {
            newOrAppendPayload(postUrl, location);
            if (timerForDelayingSendingMessages == null) {
                timerForDelayingSendingMessages = new Timer("Message collecting timer", /* daemon */ true);
                final TimerTask timerTask = createTimerTask();
                timerForDelayingSendingMessages.schedule(timerTask, /* initial delay 0, send new fix immediately */ 0);
            }
        }
    }

    /**
     * Add the {@code payload} to the HashMap, if the postUrl isn't included. If the url is included the fixes will be
     * concat to the current waiting data.
     *
     * @param postUrl
     *            URL to send fixes
     * @param location
     *            the fix to store in {@link #locationsQueuedBasedOnSendingInterval}
     */
    private void newOrAppendPayload(String postUrl, Location location) {
        synchronized (messageSendingTimerMonitor) {
            List<Location> locationsForUrl = locationsQueuedBasedOnSendingInterval.get(postUrl);
            if (locationsForUrl == null) {
                locationsForUrl = new ArrayList<>();
                locationsQueuedBasedOnSendingInterval.put(postUrl, locationsForUrl);
            }
            locationsForUrl.add(location);
        }
    }

    private TimerTask createTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    final List<Intent> intentsToSend = new ArrayList<>();
                    boolean reschedule = false;
                    synchronized (messageSendingTimerMonitor) {
                        if (!locationsQueuedBasedOnSendingInterval.isEmpty()) {
                            reschedule = true;
                            for (Map.Entry<String, List<Location>> pair : locationsQueuedBasedOnSendingInterval
                                    .entrySet()) {
                                if (!pair.getValue().isEmpty()) {
                                    intentsToSend.add(MessageSendingService.createMessageIntent(TrackingService.this,
                                            pair.getKey(), null, UUID.randomUUID(),
                                            createFixesMessage(pair.getValue()).toString(), null));
                                }
                            }
                            locationsQueuedBasedOnSendingInterval.clear();
                        }
                        if (!reschedule) {
                            timerForDelayingSendingMessages.cancel();
                            timerForDelayingSendingMessages = null;
                        } else {
                            timerForDelayingSendingMessages.schedule(createTimerTask(),
                                    prefs.getMessageSendingIntervalInMillis());
                        }
                    }
                    for (final Intent intentToSend : intentsToSend) {
                        startService(intentToSend);
                    }
                } catch (JSONException e) {
                    ExLog.e(TrackingService.this, TAG,
                            "Internal error converting location fixes to JSON message: " + e.getMessage());
                }
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return trackingBinder;
    }

    @Override
    public void onDestroy() {
        stopTracking();
        Toast.makeText(this, R.string.tracker_stopped, Toast.LENGTH_SHORT).show();
    }

    /**
     * Methods implemented through LocationManager
     */
    @Override
    public void onLocationChanged(Location location) {
        reportGPSQualityBearingAndSpeed(location.getAccuracy(), location.getBearing(), location.getSpeed(),
                location.getLatitude(), location.getLongitude(), location.getAltitude());
        final String postUrlStr = event.server + prefs.getServerGpsFixesPostPath();
        enqueueForSending(postUrlStr, location);

        if (locationWatchDog != null) {
            // clear message queue
            locationWatchDog.removeMessages(NO_LOCATION);

            // add new message with last location
            Message msg = locationWatchDog.obtainMessage(NO_LOCATION);
            msg.obj = location;
            locationWatchDog.sendMessageDelayed(msg, NO_LOCATION_CHECK);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Status Update by the provider (GPS)
    }

    @Override
    public void onProviderDisabled(String provider) {
        // provider (GPS) disabled by the user while tracking
        Intent local = new Intent();
        local.setAction(GPS_DISABLED_MESSAGE);
        this.sendBroadcast(local);
    }

    @Override
    public void onProviderEnabled(String provider) {
        // provider (GPS) (re)enabled by the user while tracking
    }

    public void registerGPSQualityListener(GPSQualityListener listener) {
        gpsQualityListener = listener;
    }

    public void unregisterGPSQualityListener() {
        gpsQualityListener = null;
    }

    public class TrackingBinder extends Binder {
        public TrackingService getService() {
            return TrackingService.this;
        }
    }

    public interface GPSQualityListener {
        void gpsQualityAndAccuracyUpdated(GPSQuality quality, float accuracy, Bearing bearing, Speed speed);
    }

    private class LocationWatchDog extends Handler {

        LocationWatchDog(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == NO_LOCATION) {
                ExLog.i(getApplicationContext(), TAG, "No Location");
                Location location = (Location) msg.obj;
                if (location == null) {
                    location = new Location(LocationManager.GPS_PROVIDER);
                }
                reportGPSQualityBearingAndSpeed(NO_DISTANCE, location.getBearing(), location.getSpeed(),
                        location.getLatitude(), location.getLongitude(), location.getAltitude());
            }
        }
    }
}