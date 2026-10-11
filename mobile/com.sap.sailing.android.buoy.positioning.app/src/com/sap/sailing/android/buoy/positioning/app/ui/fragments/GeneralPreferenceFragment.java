package com.sap.sailing.android.buoy.positioning.app.ui.fragments;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.util.AppPreferences;
import com.sap.sailing.android.shared.ui.fragments.preference.BasePreferenceFragment;

import android.os.Bundle;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

public class GeneralPreferenceFragment extends BasePreferenceFragment {

    private EditTextPreference prefServerSync;
    private AppPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new AppPreferences(getActivity());
        addPreferencesFromResource(R.xml.preference_general);
        prefServerSync = findPreference(R.string.preference_data_refresh_interval_seconds_key);
        prefServerSync.setSummary("" + prefs.getDataRefreshInterval());
        prefServerSync.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary("" + newValue);
                return true;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.setDataRefreshInterval(Long.parseLong(prefServerSync.getText()));
    }
}
