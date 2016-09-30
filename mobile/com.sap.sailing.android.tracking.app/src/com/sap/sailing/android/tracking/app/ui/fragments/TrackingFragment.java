package com.sap.sailing.android.tracking.app.ui.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.APIConnectivity;
import com.sap.sailing.android.shared.ui.customviews.SignalQualityIndicatorView;
import com.sap.sailing.android.shared.util.LocationHelper;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.services.TrackingService;
import com.sap.sailing.android.tracking.app.services.TrackingService.GPSQuality;
import com.sap.sailing.android.tracking.app.ui.activities.TrackingActivity;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;

public class TrackingFragment extends BaseFragment {

    static final String SIS_MODE = "instanceStateMode";
    static final String SIS_STATUS = "instanceStateStatus";
    static final String SIS_GPS_QUALITY = "instanceStateGpsQuality";
    static final String SIS_GPS_ACCURACY = "instanceStateGpsAccuracy";
    static final String SIS_GPS_UNSENT_FIXES = "instanceStateGpsUnsentFixes";

    private AppPreferences prefs;
    private long lastGPSQualityUpdate;
    private BroadcastReceiver gpsDisabledReceiver;
    private Toast gpsToast;
    private boolean gpsFound = true;

    private String TAG = TrackingFragment.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_tracking, container, false);

        prefs = new AppPreferences(getActivity());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // so it initally updates to "battery-saving" etc.
        setAPIConnectivityStatus(APIConnectivity.noAttempt);
        
        //setup receiver to get message from tracking service if GPS is disabled while tracking
        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingService.GPS_DISABLED_MESSAGE); 

        gpsDisabledReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocationHelper.showNoGPSError(getActivity(), getString(R.string.enable_gps));
            }
        };
        getActivity().registerReceiver(gpsDisabledReceiver,filter);
        if(!isLocationEnabled(getActivity()))
            LocationHelper.showNoGPSError(getActivity(), getString(R.string.enable_gps));
    }
    
    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(gpsDisabledReceiver);
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            TextView modeText = (TextView) getActivity().findViewById(R.id.mode);
            TextView statusText = (TextView) getActivity().findViewById(R.id.tracking_status);
            SignalQualityIndicatorView qualityIndicator = (SignalQualityIndicatorView) getActivity().findViewById(
                    R.id.gps_quality_indicator);
            TextView accuracyText = (TextView) getActivity().findViewById(R.id.gps_accuracy_label);
            TextView unsentFixesText = (TextView) getActivity().findViewById(R.id.tracking_unsent_fixes);

            modeText.setText(savedInstanceState.getString(SIS_MODE));
            statusText.setText(savedInstanceState.getString(SIS_STATUS));
            qualityIndicator.setSignalQuality(savedInstanceState.getInt(SIS_GPS_QUALITY));
            accuracyText.setText(savedInstanceState.getString(SIS_GPS_ACCURACY));
            unsentFixesText.setText(savedInstanceState.getString(SIS_GPS_UNSENT_FIXES));
        } else {
            //initially set quality to "No GPS" on start tracking
            updateTrackingStatus(GPSQuality.noSignal);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        TextView modeText = (TextView) getActivity().findViewById(R.id.mode);
        TextView statusText = (TextView) getActivity().findViewById(R.id.tracking_status);
        SignalQualityIndicatorView qualityIndicator = (SignalQualityIndicatorView) getActivity().findViewById(
                R.id.gps_quality_indicator);
        TextView accuracyText = (TextView) getActivity().findViewById(R.id.gps_accuracy_label);
        TextView unsentFixesText = (TextView) getActivity().findViewById(R.id.tracking_unsent_fixes);

        outState.putString(SIS_MODE, modeText.getText().toString());
        outState.putString(SIS_STATUS, statusText.getText().toString());
        outState.putInt(SIS_GPS_QUALITY, qualityIndicator.getSignalQuality());
        outState.putString(SIS_GPS_ACCURACY, accuracyText.getText().toString());
        outState.putString(SIS_GPS_UNSENT_FIXES, unsentFixesText.getText().toString());
    }

    /**
     * If last GPS update is too long ago, let's assume there's no signal and set quality to .noSignal
     */
    public void checkLastGPSReceived() {
        if (System.currentTimeMillis() - lastGPSQualityUpdate > 3000 && !isLocationEnabled(getActivity())) {
            if (BuildConfig.DEBUG) {
                ExLog.i(getActivity(), TAG,
                        "Setting GPS Quality to 0 because timeout occurred and location is reported as disabled.");
            }
            setGPSQualityAndAcurracy(GPSQuality.noSignal, 0);
        }
    }

    /**
     * Update tracking status string in UI
     *
     * @param quality
     */
    public void updateTrackingStatus(GPSQuality quality) {
        if (isAdded()) {
            TextView textView = (TextView) getActivity().findViewById(R.id.tracking_status);

            if (quality == GPSQuality.noSignal) {
                textView.setText(getString(R.string.tracking_status_no_gps_signal));
                textView.setTextColor(getResources().getColor(R.color.sap_red));
                //either GPS has been found before or tracking just started (but is not available now) --> show noGPS Toast
                if (gpsFound || gpsToast == null) {
                    gpsToast = Toast.makeText(getActivity(),getString(R.string.tracking_status_no_gps_signal), Toast.LENGTH_LONG);
                    gpsToast.show();
                    gpsFound = false;
                }
            } else {
                textView.setText(getString(R.string.tracking_status_tracking));
                textView.setTextColor(getResources().getColor(R.color.fiori_text_color));
                gpsToast.cancel();
                //GPS was lost before but is available again now --> show gpsFound toast
                if (!gpsFound) {
                    gpsToast = Toast.makeText(getActivity(),getString(R.string.tracking_status_gps_found), Toast.LENGTH_SHORT);
                    gpsToast.show();
                    gpsFound = true;
                }
            }
        }
    }

    /**
     * Update UI and tell user if app is caching or sending fixes to api
     *
     * @param apiConnectivity
     */
    public void setAPIConnectivityStatus(final APIConnectivity apiConnectivity) {
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) getActivity().findViewById(R.id.mode);
                    if (apiConnectivity == APIConnectivity.transmissionSuccess) {
                        if (prefs.getEnergySavingEnabledAutomatically() && !prefs.getEnergySavingOverride()) {
                            textView.setText(getString(R.string.tracking_mode_battery_saving));
                            textView.setTextColor(getResources().getColor(R.color.sap_red));
                        } else {
                            textView.setText(getString(R.string.tracking_mode_live));
                            textView.setTextColor(getResources().getColor(R.color.fiori_text_color));
                        }

                    } else if (apiConnectivity == APIConnectivity.noAttempt) {
                        textView.setText(getString(R.string.tracking_mode_offline));
                        textView.setTextColor(getResources().getColor(R.color.fiori_text_color));
                    } else if (apiConnectivity == APIConnectivity.transmissionError) {
                        textView.setText(getString(R.string.tracking_mode_api_error));
                        textView.setTextColor(getResources().getColor(R.color.sap_red));
                    } else {
                        textView.setText(getString(R.string.tracking_mode_caching));
                        textView.setTextColor(getResources().getColor(R.color.fiori_text_color));
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
                return locationProviders.contains("gps");
            }
        } else {
            return false;
        }
    }

    public void userTappedBackButton() {
        TrackingActivity activity = (TrackingActivity) getActivity();
        activity.showStopTrackingConfirmationDialog();
    }

    public void setGPSQualityAndAcurracy(GPSQuality quality, float gpsAccurracy) {
        if (isAdded()) {
            Activity activity = getActivity();
            SignalQualityIndicatorView indicatorView = (SignalQualityIndicatorView) activity.findViewById(R.id.gps_quality_indicator);
            indicatorView.setSignalQuality(quality.toInt());
            TextView accuracyTextView = (TextView) getActivity().findViewById(R.id.gps_accuracy_label);
            accuracyTextView.setText("~ " + String.valueOf(Math.round(gpsAccurracy)) + " m");
            updateTrackingStatus(quality);
            lastGPSQualityUpdate = System.currentTimeMillis();
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
