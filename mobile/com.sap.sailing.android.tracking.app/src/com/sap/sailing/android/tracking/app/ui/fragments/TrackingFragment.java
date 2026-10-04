package com.sap.sailing.android.tracking.app.ui.fragments;

import com.sap.sailing.android.shared.services.sending.MessageSendingService.APIConnectivity;
import com.sap.sailing.android.shared.ui.customviews.GPSQuality;
import com.sap.sailing.android.shared.ui.customviews.SignalQualityIndicatorView;
import com.sap.sailing.android.shared.util.LocationHelper;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.services.TrackingService;
import com.sap.sailing.android.tracking.app.ui.activities.TrackingActivity;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;

import android.annotation.TargetApi;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TrackingFragment extends BaseFragment {

    static final String SIS_MODE = "mode";
    static final String SIS_STATUS = "status";
    static final String SIS_GPS_ACCURACY = "gpsAccuracy";
    static final String SIS_GPS_UNSENT_FIXES = "gpsUnsentFixes";

    private BroadcastReceiver gpsDisabledReceiver;

    private TextView modeText;
    private TextView statusText;
    private TextView accuracyText;
    private TextView unsentFixesText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.fragment_tracking, container, false);
        prefs = new AppPreferences(getActivity());

        modeText = (TextView) layout.findViewById(R.id.mode);
        statusText = (TextView) layout.findViewById(R.id.tracking_status);
        accuracyText = (TextView) layout.findViewById(R.id.gps_accuracy_label);
        unsentFixesText = (TextView) layout.findViewById(R.id.tracking_unsent_fixes);

        if (savedInstanceState != null) {
            modeText.setText(savedInstanceState.getString(SIS_MODE));
            statusText.setText(savedInstanceState.getString(SIS_STATUS));
            accuracyText.setText(savedInstanceState.getString(SIS_GPS_ACCURACY));
            unsentFixesText.setText(savedInstanceState.getString(SIS_GPS_UNSENT_FIXES));
        } else {
            // initially set quality to "No GPS" on start tracking
            updateTrackingStatus(GPSQuality.noSignal);
        }

        return layout;
    }

    @Override
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void onResume() {
        super.onResume();
        // so it initally updates to "battery-saving" etc.
        setAPIConnectivityStatus(APIConnectivity.noAttempt);

        // setup receiver to get message from tracking service if GPS is disabled while tracking
        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingService.GPS_DISABLED_MESSAGE);

        gpsDisabledReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocationHelper.showNoGPSError(getActivity(), getString(R.string.enable_gps));
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getActivity().registerReceiver(gpsDisabledReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getActivity().registerReceiver(gpsDisabledReceiver, filter);
        }
        if (!isLocationEnabled(getActivity())) {
            LocationHelper.showNoGPSError(getActivity(), getString(R.string.enable_gps));
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        getActivity().unregisterReceiver(gpsDisabledReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SIS_MODE, modeText.getText().toString());
        outState.putString(SIS_STATUS, statusText.getText().toString());
        outState.putString(SIS_GPS_ACCURACY, accuracyText.getText().toString());
        outState.putString(SIS_GPS_UNSENT_FIXES, unsentFixesText.getText().toString());
    }

    /**
     * Update tracking status string in UI
     *
     * @param quality
     */
    public void updateTrackingStatus(GPSQuality quality) {
        if (isAdded()) {
            if (quality == GPSQuality.noSignal) {
                statusText.setText(getString(R.string.tracking_status_no_gps_signal));
                statusText.setTextColor(getResources().getColor(R.color.sap_red));
            } else {
                statusText.setText(getString(R.string.tracking_status_tracking));
                statusText.setTextColor(getResources().getColor(R.color.fiori_text_color));
            }
        }
    }

    /**
     * Update UI and tell user if app is caching or sending fixes to api
     */
    public void setAPIConnectivityStatus(final APIConnectivity apiConnectivity) {
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (apiConnectivity) {
                    case transmissionSuccess:
                        modeText.setText(getString(R.string.tracking_mode_live));
                        modeText.setTextColor(getResources().getColor(R.color.fiori_text_color));
                        break;

                    case noAttempt:
                        modeText.setText(getString(R.string.tracking_mode_no_attempt));
                        modeText.setTextColor(getResources().getColor(R.color.fiori_text_color));
                        break;

                    case transmissionError:
                        modeText.setText(getString(R.string.tracking_mode_api_error));
                        modeText.setTextColor(getResources().getColor(R.color.sap_red));
                        break;

                    default:
                        modeText.setText(getString(R.string.tracking_mode_caching));
                        modeText.setTextColor(getResources().getColor(R.color.fiori_text_color));

                    }
                }
            });
        }
    }

    /**
     * Returns if location is enabled on the device
     *
     * @param context
     * @return true if location is enabled, false otherwise
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    private boolean isLocationEnabled(Context context) {
        if (isAdded()) {
            int locationMode = 0;
            String locationProviders;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                } catch (SettingNotFoundException e) {
                    e.printStackTrace();
                }

                return locationMode != Settings.Secure.LOCATION_MODE_OFF;

            } else {
                locationProviders = Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                return locationProviders.contains(LocationManager.GPS_PROVIDER);
            }
        } else {
            return false;
        }
    }

    public void userTappedBackButton() {
        TrackingActivity activity = (TrackingActivity) getActivity();
        activity.showStopTrackingConfirmationDialog();
    }

    public void setGPSQualityAndAccuracy(GPSQuality quality, float accuracy) {
        if (isAdded()) {
            View layout = getView();
            if (layout != null) {
                SignalQualityIndicatorView indicatorView = (SignalQualityIndicatorView) layout
                        .findViewById(R.id.gps_quality_indicator);
                indicatorView.setSignalQuality(quality);
                updateTrackingStatus(quality);
                if (quality != GPSQuality.noSignal) {
                    accuracyText.setText("~ " + Math.round(accuracy) + " m");
                } else {
                    accuracyText.setText(null);
                }
            }
        }
    }

    public void setUnsentGPSFixesCount(final int count) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    TextView unsentGpsFixesTextView = (TextView) getActivity().findViewById(R.id.tracking_unsent_fixes);
                    if (count == 0) {
                        unsentGpsFixesTextView.setText(getString(R.string.none));
                    } else {
                        unsentGpsFixesTextView.setText(String.valueOf(count));
                    }
                }
            }
        });
    }
}
