package com.sap.sailing.racecommittee.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.logging.ExLog;

/**
 * Helps you with maintaining preferences over different app builds
 */
public class PreferenceHelper {

    private static final String TAG = PreferenceHelper.class.getName();

    /**
     * Whenever you change a preference's type (e.g. from Integer to String) you need to bump this version code to the
     * appropriate app version (see AndroidManifest.xml).
     */
    private final static int LAST_COMPATIBLE_VERSION = 3;

    /**
     * Application stores preference version code in this preference file (and key).
     */
    private final static String HIDDEN_PREFERENCE_VERSION_CODE_KEY = "hiddenPrefsVersionCode";

    private final Context context;

    public PreferenceHelper(Context context) {
        this.context = context;
    }

    public void setupPreferences() {
        setupPreferences(false);
    }

    public void setupPreferences(boolean forceReset) {
        if (forceReset) {
            ExLog.i(TAG, "A preferences reset and read will be forced.");
        }

        boolean isCleared = clearPreferencesIfNeeded(forceReset);
        boolean hasSetDefaultsBefore = hasSetDefaultsBefore();
        boolean readAgain = forceReset || isCleared || !hasSetDefaultsBefore;

        ExLog.i(TAG, String.format("Preference state: {cleared=%s, setDefaultsBefore=%s, readAgain=%s}", isCleared,
                hasSetDefaultsBefore, readAgain));

        PreferenceManager.setDefaultValues(context, R.xml.preference_course_designer, readAgain);
        PreferenceManager.setDefaultValues(context, R.xml.preference_general, readAgain);
        PreferenceManager.setDefaultValues(context, R.xml.preference_regatta_defaults, readAgain);
    }

    private boolean clearPreferencesIfNeeded(boolean forceReset) {
        SharedPreferences versionPreferences = getSharedPreferences(HIDDEN_PREFERENCE_VERSION_CODE_KEY);
        int preferencesVersion = versionPreferences.getInt(HIDDEN_PREFERENCE_VERSION_CODE_KEY, 0);
        if (preferencesVersion < LAST_COMPATIBLE_VERSION) {
            ExLog.i(TAG, "Clearing the preference cache");

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.clear().commit();

            ExLog.i(TAG, String.format("Bumping preference version code to %d", LAST_COMPATIBLE_VERSION));
            versionPreferences.edit().putInt(HIDDEN_PREFERENCE_VERSION_CODE_KEY, LAST_COMPATIBLE_VERSION).commit();
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
}
