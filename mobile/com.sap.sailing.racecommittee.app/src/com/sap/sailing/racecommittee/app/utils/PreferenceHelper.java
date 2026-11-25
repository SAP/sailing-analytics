package com.sap.sailing.racecommittee.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.preference.PreferenceManager;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.activities.PreferenceActivity;

/**
 * Helps you with maintaining preferences over different app builds
 */
public class PreferenceHelper {

    private static final String TAG = PreferenceHelper.class.getName();

    public static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    /**
     * Whenever you change a preference's type (e.g. from Integer to String) you need to bump this version code to the
     * appropriate app version (see AndroidManifest.xml).
     */
    private final static int LAST_COMPATIBLE_VERSION = 4;

    /**
     * Application stores preference version code in this preference file (and key).
     */
    private final static String HIDDEN_PREFERENCE_VERSION_CODE_KEY = "hiddenPrefsVersionCode";

    private final Context context;
    private final String sharedPreferencesName;

    public PreferenceHelper(Context context) {
        this(context, getDefaultSharedPreferencesName(context));
    }

    public PreferenceHelper(Context context, String sharedPreferencesName) {
        this.context = context;
        this.sharedPreferencesName = sharedPreferencesName;
    }

    public void setupPreferences() {
        setupPreferences(false);
    }

    public void setupPreferences(boolean forceReset) {
        if (forceReset) {
            ExLog.i(context, TAG, "A preferences reset and read will be forced.");
        }

        boolean isCleared = clearPreferencesIfNeeded(forceReset);
        boolean hasSetDefaultsBefore = hasSetDefaultsBefore();
        boolean readAgain = forceReset || isCleared || !hasSetDefaultsBefore;

        ExLog.i(context, TAG, String.format("Preference state: {cleared=%s, setDefaultsBefore=%s, readAgain=%s}",
                isCleared, hasSetDefaultsBefore, readAgain));

        resetPreferences(readAgain);
    }

    public void resetPreferences(boolean force) {
        PreferenceManager.setDefaultValues(context, sharedPreferencesName, Context.MODE_PRIVATE,
                R.xml.preference_course_designer, force);
        PreferenceManager.setDefaultValues(context, sharedPreferencesName, Context.MODE_PRIVATE,
                R.xml.preference_general, force);
        PreferenceManager.setDefaultValues(context, sharedPreferencesName, Context.MODE_PRIVATE,
                R.xml.preference_regatta_defaults, force);
    }

    public void clearPreferences() {
        SharedPreferences.Editor editor = getSharedPreferences(sharedPreferencesName).edit();
        editor.clear().apply();
    }

    private boolean clearPreferencesIfNeeded(boolean forceReset) {
        SharedPreferences versionPreferences = getSharedPreferences(HIDDEN_PREFERENCE_VERSION_CODE_KEY);
        int preferencesVersion = versionPreferences.getInt(HIDDEN_PREFERENCE_VERSION_CODE_KEY, 0);
        if (preferencesVersion < LAST_COMPATIBLE_VERSION) {
            ExLog.i(context, TAG, "Clearing the preference cache");

            clearPreferences();

            ExLog.i(context, TAG, String.format("Bumping preference version code to %d", LAST_COMPATIBLE_VERSION));
            versionPreferences.edit().putInt(HIDDEN_PREFERENCE_VERSION_CODE_KEY, LAST_COMPATIBLE_VERSION).apply();
            return true;
        }
        return false;
    }

    private boolean hasSetDefaultsBefore() {
        SharedPreferences defaultValueSp = getSharedPreferences(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES);
        return defaultValueSp.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false);
    }

    private SharedPreferences getSharedPreferences(String preferenceName) {
        return context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
    }

    public static String getRegattaPrefFileName(String regattaName) {
        String fileName = PreferenceActivity.SPECIFIC_REGATTA_PREFERENCES_NAME + regattaName;
        return Uri.encode(fileName);
    }
}
