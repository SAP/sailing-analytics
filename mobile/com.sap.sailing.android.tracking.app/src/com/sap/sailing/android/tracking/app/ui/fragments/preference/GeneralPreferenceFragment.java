package com.sap.sailing.android.tracking.app.ui.fragments.preference;

import com.sap.sailing.android.shared.ui.fragments.preference.BasePreferenceFragment;
import com.sap.sailing.android.shared.util.UniqueDeviceUuid;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;

import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

public class GeneralPreferenceFragment extends BasePreferenceFragment implements OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_general);

        AppPreferences prefs = new AppPreferences(getActivity());
        String currentDeviceId = prefs.getDeviceIdentifier();

        Preference deviceIdPreference = findPreference(getString(R.string.preference_device_identifier_key));

        if (currentDeviceId != null && currentDeviceId.length() > 0) {
            deviceIdPreference.setSummary(currentDeviceId);
        } else {
            // this would, in most cases, be set elsewhere, but since we don't want to show an empty summary,
            // we can just set it here. (UniqueDeviceUuid.getUniqueId sets it in prefs.)
            String newDeviceId = UniqueDeviceUuid.getUniqueId(getActivity());
            deviceIdPreference.setSummary(newDeviceId);
        }

        ListPreference prefSendingInterval = findPreference(R.string.preference_energy_saving_sending_interval_key);
        prefSendingInterval.setDefaultValue(R.string.preference_energy_saving_sending_interval_default);
        prefSendingInterval.setOnPreferenceChangeListener(this);

        updateEnergySavingMessage(null);
    }

    @Override
    public void onPause() {
        super.onPause();

        // SwitchPreference prefEnergy = findPreference(R.string.preference_energy_saving_enabled_key);
        SwitchPreferenceCompat prefDeclination = findPreference(
                R.string.preference_heading_with_declination_subtracted_key);

        AppPreferences prefs = new AppPreferences(getActivity());

        // prefs.setEnergySavingEnabledByUser(prefEnergy.isChecked());
        prefs.setDisplayHeadingWithSubtractedDeclination(prefDeclination.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        AppPreferences prefs = new AppPreferences(getActivity());

        // GPS message sending interval settings
        if (preference.getKey().equals(getString(R.string.preference_energy_saving_sending_interval_key))) {
            int msgSndInt = Integer.parseInt((String) newValue);
            switch (msgSndInt) {
            case 0:

                // send every second
                prefs.setMessageResendIntervalInMillis(1000);
                break;
            default:
                prefs.setMessageResendIntervalInMillis(msgSndInt * 1000);
                break;
            }
            updateEnergySavingMessage(newValue.toString());
        }
        return true;
    }

    private void updateEnergySavingMessage(String value) {
        Preference msgSndIntMessage = findPreference(
                getString(R.string.preference_energy_saving_sending_interval_message_key));
        ListPreference msgSndIntPreference = (ListPreference) findPreference(
                getString(R.string.preference_energy_saving_sending_interval_key));

        // when value == null, (meaning the settings screen just launched) - current entry will be selected
        // otherwise, we need to get the index of the newly selected entry as the one resulting from getEntry() will be
        // the previous one
        String entry;
        if (value == null) {
            entry = msgSndIntPreference.getEntry().toString();
        } else {
            int indexOfEntry = msgSndIntPreference.findIndexOfValue(value);
            entry = msgSndIntPreference.getEntries()[indexOfEntry].toString();
        }

        // possibly fill in the word "every"
        String fillEvery = (entry.equals(msgSndIntPreference.getEntries()[0])) ? ""
                : " " + getString(R.string.preference_energy_saving_sending_interval_message_every);

        // set Text
        msgSndIntMessage.setSummary(
                String.format(getString(R.string.preference_energy_saving_sending_interval_message), fillEvery, entry));
    }
}
