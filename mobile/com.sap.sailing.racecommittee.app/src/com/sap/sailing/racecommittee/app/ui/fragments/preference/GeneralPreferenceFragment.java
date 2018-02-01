package com.sap.sailing.racecommittee.app.ui.fragments.preference;

import com.sap.sailing.android.shared.ui.fragments.preference.BasePreferenceFragment;
import com.sap.sailing.android.shared.ui.views.EditSetPreference;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.BuildConfig;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.utils.QRHelper;
import com.sap.sailing.racecommittee.app.utils.autoupdate.AutoUpdater;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

public class GeneralPreferenceFragment extends BasePreferenceFragment {

    private static int requestCodeQRCode = 45392;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_general);

        setupConnection();
        setupPolling();
        setupGeneral();
        setupDeveloperOptions();
    }

    private void setupGeneral() {
        setupLanguageButton();
        setupCourseAreasList();

        bindPreferenceSummaryToSet(getPreferenceManager(), findPreference(R.string.preference_course_areas_key));
        bindPreferenceSummaryToValue(getPreferenceManager(), findPreference(R.string.preference_mail_key));
        bindPreferenceToListEntry(findPreference(R.string.preference_theme_key), getString(R.string.preference_theme_default));
        addOnPreferenceChangeListener(findPreference(R.string.preference_theme_key), new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog);
                builder.setTitle(getString(R.string.theme_changed_title));
                builder.setMessage(R.string.theme_changed_message);
                builder.setPositiveButton(getString(android.R.string.ok), null);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            }
        });
        addOnPreferenceChangeListener(findPreference(R.string.preference_non_public_events_key), new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AppPreferences.on(getActivity()).setNeedConfigRefresh(true);
                if (DataManager.create(getActivity()).getDataStore().getCourseUUID() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog);
                    builder.setTitle(getString(R.string.non_public_changed_title));
                    builder.setMessage(getString(R.string.app_refresh_message));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                }
                return true;
            }
        });
    }

    private void setupPolling() {
        Preference intervalPreference = findPreference(R.string.preference_polling_interval_key);
        CheckBoxPreference activePreference = findPreference(R.string.preference_polling_active_key);
        bindPreferenceToCheckbox(activePreference, intervalPreference);
        bindPreferenceSummaryToInteger(getPreferenceManager(), intervalPreference);
    }

    private void setupDeveloperOptions() {
        PreferenceScreen screen = getPreferenceScreen();
        PreferenceCategory category = findPreference(R.string.preference_developer_key);
        if (!BuildConfig.DEBUG) {
            if (screen != null && category != null) {
                screen.removePreference(category);
            }
        }
    }

    private void setupConnection() {
        setupIdentifierBox();
        setupServerUrlBox();
        setupSyncQRCodeButton();
        setupForceUpdateButton();
    }

    private void setupIdentifierBox() {
        final AppPreferences appPreferences = AppPreferences.on(getActivity());
        EditTextPreference identifierPreference = findPreference(R.string.preference_identifier_key);
        identifierPreference.setSummary(appPreferences.getDeviceIdentifier());
        addOnPreferenceChangeListener(identifierPreference, new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;
                if (value.isEmpty()) {
                    preference.setSummary(appPreferences.getDeviceIdentifier());
                } else {
                    preference.setSummary(value);
                }
                return true;
            }
        });
    }

    private void setupServerUrlBox() {
        EditTextPreference serverUrlPreference = findPreference(R.string.preference_server_url_key);
        addOnPreferenceChangeListener(serverUrlPreference, new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AppPreferences.on(getActivity()).setNeedConfigRefresh(true);
                if (DataManager.create(getActivity()).getDataStore().getCourseUUID() != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_AlertDialog);
                    builder.setTitle(getString(R.string.url_refresh_title));
                    builder.setMessage(getString(R.string.app_refresh_message));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                }
                return true;
            }
        });
    }

    private void setupSyncQRCodeButton() {
        Preference preference = findPreference(R.string.preference_sync_key);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                requestQRCodeScan();
                return false;
            }
        });
    }

    private void setupForceUpdateButton() {
        Preference preference = findPreference(R.string.preference_update_key);
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AutoUpdater(getActivity()).checkForUpdate(true);
                return false;
            }
        });
    }

    private void setupCourseAreasList() {
        EditSetPreference preference = findPreference(R.string.preference_course_areas_key);
        // TODO: example values from DataStore
        preference.setExampleValues(getResources().getStringArray(R.array.preference_course_areas_example));
    }

    private void setupLanguageButton() {
        Preference preference = findPreference(R.string.preference_language_key);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.settings", "com.android.settings.LanguageSettings");
                startActivity(intent);
                return true;
            }
        });
    }

    private boolean requestQRCodeScan() {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, requestCodeQRCode);
            return true;
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != requestCodeQRCode) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            QRHelper.with(getActivity()).saveData(data.getStringExtra("SCAN_RESULT"));

            final AppPreferences appPreferences = AppPreferences.on(getActivity());
            appPreferences.setNeedConfigRefresh(true);
            // update device identifier in ui
            EditTextPreference identifierPreference = findPreference(R.string.preference_identifier_key);
            identifierPreference.setText(appPreferences.getDeviceIdentifier());
            identifierPreference.setSummary(appPreferences.getDeviceIdentifier());
            // update server url in ui
            EditTextPreference serverUrlPreference = findPreference(R.string.preference_server_url_key);
            serverUrlPreference.setText(appPreferences.getServerBaseURL());
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_scanning_qr, resultCode), Toast.LENGTH_LONG).show();
        }
    }
}
