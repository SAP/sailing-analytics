package com.sap.sailing.android.buoy.positioning.app.ui.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.activities.PositioningActivity;
import com.sap.sailing.android.buoy.positioning.app.util.DatabaseHelper;
import com.sap.sailing.android.buoy.positioning.app.util.PingHelper;
import com.sap.sailing.android.buoy.positioning.app.util.PingServerReplyCallback;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkInfo;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkPingInfo;
import com.sap.sailing.android.shared.data.CheckinUrlInfo;
import com.sap.sailing.android.shared.data.LeaderboardInfo;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.customviews.GPSQuality;
import com.sap.sailing.android.shared.ui.customviews.SignalQualityIndicatorView;
import com.sap.sailing.android.shared.ui.fragments.BaseFragment;
import com.sap.sailing.android.shared.util.LocationHelper;
import com.sap.sailing.android.shared.util.ViewHelper;

import static com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants.URL_SECRET;

public class BuoyFragment extends BaseFragment implements android.location.LocationListener, OnMapReadyCallback {

    private static final String TAG = BuoyFragment.class.getName();

    private static final int GPS_MIN_TIME = 1000;
    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 41;

    private float minLocationUpdateDistanceInMeters = 0f;

    private TextView accuracyTextView;
    private TextView distanceTextView;
    private Button setPositionButton;
    private Location lastKnownLocation;
    private LatLng savedPosition;
    private pingListener pingListener;
    private SignalQualityIndicatorView signalQualityIndicatorView;
    private boolean initialLocationUpdate;
    private IntentReceiver mReceiver;
    private PositioningActivity positioningActivity;
    private LocalBroadcastManager mBroadcastManager;
    private GoogleMap mMap;

    protected LocationManager locationManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View layout = inflater.inflate(R.layout.fragment_buoy_postion_detail, container, false);

        accuracyTextView = ViewHelper.get(layout, R.id.marker_gps_accuracy);
        distanceTextView = ViewHelper.get(layout, R.id.marker_gps_distance);
        ClickListener clickListener = new ClickListener();

        setUpSetPositionButton(layout, clickListener);

        signalQualityIndicatorView = ViewHelper.get(layout, R.id.signal_quality_indicator);
        signalQualityIndicatorView.setSignalQuality(GPSQuality.noSignal);

        mReceiver = new IntentReceiver();
        mBroadcastManager = LocalBroadcastManager.getInstance(inflater.getContext());

        positioningActivity = (PositioningActivity) getActivity();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            /**
             * prior to JellyBean, the minimum time for location updates parameter MIGHT be ignored, so providing a
             * minimum distance value greater than 0 is recommended
             */
            minLocationUpdateDistanceInMeters = .5f;
        }

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        disablePositionButton();
        positioningActivity = (PositioningActivity) getActivity();
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        initialLocationUpdate = true;
        MarkInfo mark = positioningActivity.getMarkInfo();
        signalQualityIndicatorView.setSignalQuality(GPSQuality.noSignal);
        if (mark != null) {
            setUpTextUI(lastKnownLocation);
            updateMap();
        }
        initMarkerReceiver();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Enabling MyLocation Layer of Google Map
        if (!hasPermissions()) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        if (savedPosition != null || (initialLocationUpdate && lastKnownLocation != null)) {
            LatLng lastKnownLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            LatLng position = savedPosition != null ? savedPosition : lastKnownLatLng;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
            initialLocationUpdate = false;
        }
    }

    private void setUpSetPositionButton(View layout, ClickListener clickListener) {
        setPositionButton = ViewHelper.get(layout, R.id.marker_set_position_button);
        disablePositionButton();
        setPositionButton.setOnClickListener(clickListener);
    }

    private void disablePositionButton() {
        setPositionButton.setEnabled(false);
        int resId = LocationHelper.isGPSEnabled(getActivity()) ? R.string.set_position_no_gps_yet
                : R.string.set_position_disabled_gps;
        setPositionButton.setText(resId);
    }

    private void initMarkerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.database_changed));
        filter.addAction(getString(R.string.ping_reached_server));
        mBroadcastManager.registerReceiver(mReceiver, filter);
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onStart() {
        super.onStart();
        if (!hasPermissions()) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_MIN_TIME,
                minLocationUpdateDistanceInMeters, this);
        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);

        mBroadcastManager.unregisterReceiver(mReceiver);
    }

    private void setUpTextUI(Location location) {
        String accuracyText = this.getString(R.string.unknown);
        String distanceText = this.getString(R.string.unknown);
        if (location != null) {
            accuracyText = getString(R.string.buoy_detail_accuracy_ca, location.getAccuracy());
            MarkPingInfo markPing = positioningActivity.getMarkPing();
            if (markPing != null) {
                double savedLatitude = Double.parseDouble(markPing.getLatitude());
                double savedLongitude = Double.parseDouble(markPing.getLongitude());
                savedPosition = new LatLng(savedLatitude, savedLongitude);
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), savedLatitude, savedLongitude,
                        results);
                float distance = results[0];
                distanceText = getString(R.string.buoy_detail_distance, distance);
            }
        }

        ExLog.w(getActivity(), getTag(), "Setting accuracy to: " + accuracyText);
        ExLog.w(getActivity(), getTag(), "Setting distance to: " + distanceText);

        accuracyTextView.setText(accuracyText);
        distanceTextView.setText(distanceText);
    }

    /**
     * Methods implemented through LocationManager
     */
    @Override
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;
        reportGPSQuality(lastKnownLocation.getAccuracy());
        setPositionButton.setEnabled(true);
        setPositionButton.setAllCaps(true);
        setPositionButton.setText(R.string.set_position);
        setUpTextUI(lastKnownLocation);
        updateMap();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // no-op
    }

    @Override
    public void onProviderDisabled(String provider) {
        // provider (GPS) disabled by the user while tracking
        disablePositionButton();
        setUpTextUI(null);
        signalQualityIndicatorView.setSignalQuality(GPSQuality.noSignal);

        LocationHelper.showNoGPSError(getActivity(), getString(R.string.enable_gps));
    }

    @Override
    public void onProviderEnabled(String provider) {
        // provider (GPS) (re)enabled by the user while tracking
        // call disablePositionButton() so that its text says "searching for GPS" again
        disablePositionButton();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQUEST_PERMISSIONS_REQUEST_CODE) {
            return;
        }
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_MIN_TIME,
                    minLocationUpdateDistanceInMeters, this);
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    private void updateMap() {
        if (savedPosition == null || mMap == null) {
            return;
        }
        mMap.clear();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(savedPosition);
        markerOptions.visible(true);
        mMap.addMarker(markerOptions);
    }

    private void reportGPSQuality(float gpsAccuracy) {
        GPSQuality quality = GPSQuality.noSignal;

        if (gpsAccuracy > 48) {
            quality = GPSQuality.poor;
        } else if (gpsAccuracy > 10) {
            quality = GPSQuality.good;
        } else if (gpsAccuracy <= 10) {
            quality = GPSQuality.great;
        }
        signalQualityIndicatorView.setSignalQuality(quality);
    }

    private void handleSuccessfulResponse() {
        Toast.makeText(getActivity(), getString(R.string.position_set), Toast.LENGTH_SHORT).show();
    }

    public void setPingListener(pingListener listener) {
        pingListener = listener;
    }

    public interface pingListener {
        void updatePing();
    }

    private boolean hasPermissions() {
        boolean fine = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine && coarse;
    }

    private class ClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.marker_set_position_button) {
                PingHelper helper = new PingHelper();
                if (lastKnownLocation != null) {
                    MarkInfo mark = positioningActivity.getMarkInfo();
                    LeaderboardInfo leaderBoard = positioningActivity.getLeaderBoard();
                    CheckinUrlInfo checkinUrl = DatabaseHelper.getInstance().getCheckinUrl(requireContext(), leaderBoard.checkinDigest);
                    String secret = Uri.parse(checkinUrl.urlString).getQueryParameter(URL_SECRET);
                    helper.storePingInDatabase(getActivity(), lastKnownLocation, mark);
                    helper.sendPingToServer(getActivity(), lastKnownLocation, leaderBoard, mark,
                            secret, PingServerReplyCallback.class);
                    ((PositioningActivity) getActivity()).updatePing();
                    savedPosition = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    pingListener.updatePing();
                    setUpTextUI(lastKnownLocation);
                    updateMap();
                } else {
                    Toast.makeText(getActivity(), "Location is not available yet", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // Broadcast receiver to update ui on data changed
    private class IntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Action: " + action);
            if (action.equals(getString(R.string.database_changed)) && isAdded()) {
                positioningActivity.loadDataFromDatabase();
                setUpTextUI(lastKnownLocation);
                updateMap();
            }
            if (action.equals(getString(R.string.ping_reached_server))) {
                Log.d(TAG, "Response reached Buoy Fragment");
                handleSuccessfulResponse();
            }
        }
    }
}
