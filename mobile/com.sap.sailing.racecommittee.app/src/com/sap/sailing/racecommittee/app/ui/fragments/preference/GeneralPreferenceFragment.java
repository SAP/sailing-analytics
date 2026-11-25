package com.sap.sailing.racecommittee.app.ui.fragments.preference;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.sap.sailing.android.shared.ui.activities.BarcodeCaptureActivity;
import com.sap.sailing.android.shared.ui.fragments.preference.BasePreferenceFragment;
import com.sap.sailing.android.shared.ui.views.EditSetPreference;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.BuildConfig;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.utils.autoupdate.AutoUpdater;

import io.branch.referral.Defines;

public class GeneralPreferenceFragment extends BasePreferenceFragment {

    private static final String TAG = GeneralPreferenceFragment.class.getName();

    private static int REQUEST_CODE_QR_CODE = 45392;

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

        bindPreferenceSummaryToSet(findPreference(R.string.preference_course_areas_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_mail_key));
        addOnPreferenceChangeListener(findPreference(R.string.preference_non_public_events_key),
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        AppPreferences.on(getActivity()).setNeedConfigRefresh(true);
                        if (DataManager.create(getActivity()).getDataStore().getCourseAreaId() != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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
        bindPreferenceSummaryToInteger(intervalPreference);
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
        identifierPreference.setSummary(appPreferences.getDeviceConfigurationName());
        addOnPreferenceChangeListener(identifierPreference, new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;
                if (value.isEmpty()) {
                    preference.setSummary(appPreferences.getDeviceConfigurationName());
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
            public boolean onPreferenceChange(Preference preference, @NonNull Object newValue) {
                final String summary = (String) newValue;
                final AppPreferences prefs = AppPreferences.on(getActivity());
                if (summary.isEmpty()) {
                    preference.setSummary(prefs.getServerBaseURL());
                } else {
                    preference.setSummary(summary);
                }
                prefs.setNeedConfigRefresh(true);
                if (DataManager.create(getActivity()).getDataStore().getCourseAreaId() != null) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.url_refresh_title))
                            .setMessage(getString(R.string.app_refresh_message))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
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

    private void requestQRCodeScan() {
        Intent intent = new Intent(getContext(), BarcodeCaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE_QR_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_QR_CODE) {
            return;
        }

        if (resultCode == CommonStatusCodes.SUCCESS && data != null) {
            final Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
            final Uri uri = Uri.parse(barcode.displayValue);
            final Intent intent = requireContext().getPackageManager()
                    .getLaunchIntentForPackage(requireContext().getPackageName());
            if (intent != null) {
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Defines.Jsonkey.ForceNewBranchSession.getKey(), true);
                startActivity(intent);
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_scanning_qr, resultCode), Toast.LENGTH_LONG).show();
        }
    }
}
