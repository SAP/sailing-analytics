package com.sap.sailing.racecommittee.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.racecommittee.app.domain.LoginType;
import com.sap.sailing.racecommittee.app.domain.coursedesign.BoatClassType;
import com.sap.sailing.racecommittee.app.domain.coursedesign.CourseLayouts;
import com.sap.sailing.racecommittee.app.domain.coursedesign.NumberOfRounds;
import com.sap.sailing.racecommittee.app.domain.coursedesign.TrapezoidCourseLayouts;
import com.sap.sailing.racecommittee.app.domain.coursedesign.WindWardLeeWardCourseLayouts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Wrapper for {@link SharedPreferences} for all hidden and non-hidden preferences and state variables.
 */
public class AppPreferences {
    public interface PollingActiveChangedListener {
        void onPollingActiveChanged(boolean isActive);
    }

    private final static String HIDDEN_PREFERENCE_AUTHOR_NAME = "authorName";

    private final static String HIDDEN_PREFERENCE_AUTHOR_PRIORITY = "authorPriority";

    private final static String HIDDEN_PREFERENCE_BOAT_CLASS = "boatClassPref";

    private final static String HIDDEN_PREFERENCE_COURSE_LAYOUT = "courseLayoutPref";

    private final static String HIDDEN_PREFERENCE_NUMBER_OF_ROUNDS = "numberOfRoundsPref";

    private final static String HIDDEN_PREFERENCE_WIND_BEARING = "windBearingPref";

    private final static String HIDDEN_PREFERENCE_WIND_SPEED = "windSpeedPref";

    private final static String HIDDEN_PREFERENCE_WIND_LAT = "windLatPref";

    private final static String HIDDEN_PREFERENCE_WIND_LNG = "windLngPref";

    private final static String HIDDEN_PREFERENCE_LOGIN_TYPE = "loginType";

    public static AppPreferences on(Context context) {
        return new AppPreferences(context);
    }

    public static AppPreferences on(Context context, String preferenceName) {
        return new AppPreferences(context, preferenceName, true);
    }

    public static AppPreferences on(Context context, String preferenceName, boolean overload) {
        return new AppPreferences(context, preferenceName, overload);
    }

    protected final Context context;

    private OnSharedPreferenceChangeListener pollingActiveChangedListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key(R.string.preference_polling_active_key).equals(key)) {
                for (PollingActiveChangedListener listener : pollingActiveChangedListeners) {
                    listener.onPollingActiveChanged(isPollingActive());
                }
            }
        }
    };
    private Set<PollingActiveChangedListener> pollingActiveChangedListeners = new HashSet<>();

    protected Helper helper;

    protected AppPreferences(Context context) {
        this.context = context.getApplicationContext();
        helper = new Helper(PreferenceManager.getDefaultSharedPreferences(context), null);
    }

    public AppPreferences(Context context, String preferenceName, boolean overload) {
        this.context = context;

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences customSharedPreferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        if (overload) {
            helper = new Helper(defaultSharedPreferences, customSharedPreferences);
        } else {
            helper = new Helper(customSharedPreferences, null);
        }
    }

    public void setAuthor(AbstractLogEventAuthor author) {
        helper.getEditor().putString(HIDDEN_PREFERENCE_AUTHOR_NAME, author.getName())
                .putInt(HIDDEN_PREFERENCE_AUTHOR_PRIORITY, author.getPriority()).commit();
    }

    public AbstractLogEventAuthor getAuthor() {
        String authorName = helper.getString(HIDDEN_PREFERENCE_AUTHOR_NAME, "<anonymous>");
        int authorPriority = helper.getInt(HIDDEN_PREFERENCE_AUTHOR_PRIORITY, 0);
        return new LogEventAuthorImpl(authorName, authorPriority);
    }

    public BoatClassType getBoatClass() {
        String boatClass = helper.getString(HIDDEN_PREFERENCE_BOAT_CLASS, null);
        if (boatClass == null) {
            return null;
        }
        return BoatClassType.valueOf(boatClass);
    }

    public List<String> getByNameCourseDesignerCourseNames() {
        Set<String> values = helper.getStringSet(key(R.string.preference_course_designer_by_name_course_names_key),
                new HashSet<String>());
        return new ArrayList<>(values);
    }

    public Context getContext() {
        return context;
    }

    public CourseLayouts getCourseLayout() {
        String courseLayout = helper.getString(HIDDEN_PREFERENCE_COURSE_LAYOUT, null);
        if (courseLayout == null)
            return null;
        CourseLayouts storedCourseLayout;
        // FIXME this is not nice
        try {
            storedCourseLayout = TrapezoidCourseLayouts.valueOf(courseLayout);
        } catch (IllegalArgumentException iae) {
            storedCourseLayout = WindWardLeeWardCourseLayouts.valueOf(courseLayout);
        }

        return storedCourseLayout;
    }

    public CourseDesignerMode getDefaultCourseDesignerMode() {
        String mode = helper.getString(key(R.string.preference_course_designer_override_key), "BY_NAME");
        return CourseDesignerMode.valueOf(mode);
    }

    public RacingProcedureType getDefaultRacingProcedureType() {
        String defaultStartProcedureType = helper.getString(key(R.string.preference_racing_procedure_override_key),
                key(R.string.preference_racing_procedure_override_default));
        return RacingProcedureType.valueOf(defaultStartProcedureType);
    }

    public String getDeviceConfigurationName() {
        return getDeviceConfigurationName(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
    }

    public String getDeviceConfigurationName(String defaultValue) {
        String identifier = helper.getString(key(R.string.preference_identifier_key), "");
        return TextUtils.isEmpty(identifier) ? defaultValue : identifier;
    }

    public UUID getDeviceConfigurationUuid() {
        final String uuidAsString = helper.getString(key(R.string.preference_config_uuid_key), "");
        return TextUtils.isEmpty(uuidAsString) ? null : UUID.fromString(uuidAsString);
    }

    public boolean getGateStartHasAdditionalGolfDownTime() {
        return helper.getBoolean(key(R.string.preference_racing_procedure_gatestart_hasadditionalgolfdowntime_key),
                true);
    }

    public boolean getGateStartHasPathfinder() {
        return helper.getBoolean(key(R.string.preference_racing_procedure_gatestart_haspathfinder_key), true);
    }

    public LoginType getLoginType() {
        int type = helper.getInt(HIDDEN_PREFERENCE_LOGIN_TYPE, -1);
        switch (type) {
            case 0: {
                return LoginType.NONE;
            }
            case 1: {
                return LoginType.VIEWER;
            }
            case 2: {
                return LoginType.OFFICER;
            }

            default: {
                return LoginType.NONE;
            }
        }
    }

    public String getMailRecipient() {
        return helper.getString(key(R.string.preference_mail_key), "");
    }

    public List<String> getManagedCourseAreaNames() {
        Set<String> values = helper.getStringSet(key(R.string.preference_course_areas_key), new HashSet<String>());
        return new ArrayList<>(values);
    }

    public NumberOfRounds getNumberOfRounds() {
        String numberOfRounds = helper.getString(HIDDEN_PREFERENCE_NUMBER_OF_ROUNDS, null);
        if (numberOfRounds == null)
            return null;
        return NumberOfRounds.valueOf(numberOfRounds);
    }

    /**
     * Gets polling interval in seconds
     */
    public int getPollingIntervalInSeconds() {
        return helper.getInt(key(R.string.preference_polling_interval_key), 0);
    }

    public Flags getRacingProcedureClassFlag(RacingProcedureType type) {
        String key = getRacingProcedureClassFlagKey(type);
        return Flags.valueOf(helper.getString(key, Flags.CLASS.name()));
    }

    private String getRacingProcedureClassFlagKey(RacingProcedureType type) {
        switch (type) {
            case RRS26:
            case RRS26_3MIN:
                return key(R.string.preference_racing_procedure_rrs26_classflag_key);
            case SWC:
            case SWC_4MIN:
                return key(R.string.preference_racing_procedure_swc_classflag_key);
            case GateStart:
                return key(R.string.preference_racing_procedure_gatestart_classflag_key);
            case ESS:
                return key(R.string.preference_racing_procedure_ess_classflag_key);
            case BASIC:
                return key(R.string.preference_racing_procedure_basic_classflag_key);
            case LEAGUE:
                return key(R.string.preference_racing_procedure_league_classflag_key);
            default:
                throw new IllegalArgumentException("Unknown racing procedure type.");
        }
    }

    public boolean getRacingProcedureHasIndividualRecall(RacingProcedureType type) {
        String key = getRacingProcedureHasIndividualRecallKey(type);
        return helper.getBoolean(key, false);
    }

    private String getRacingProcedureHasIndividualRecallKey(RacingProcedureType type) {
        switch (type) {
            case RRS26:
            case RRS26_3MIN:
                return key(R.string.preference_racing_procedure_rrs26_hasxray_key);
            case SWC:
            case SWC_4MIN:
                return key(R.string.preference_racing_procedure_swc_hasxray_key);
            case GateStart:
                return key(R.string.preference_racing_procedure_gatestart_hasxray_key);
            case ESS:
                return key(R.string.preference_racing_procedure_ess_hasxray_key);
            case BASIC:
                return key(R.string.preference_racing_procedure_basic_hasxray_key);
            case LEAGUE:
                return key(R.string.preference_racing_procedure_league_hasxray_key);
            default:
                throw new IllegalArgumentException("Unknown racing procedure type.");
        }
    }

    public boolean getRacingProcedureIsResultEntryEnabled(RacingProcedureType type) {
        String key = getRacingProcedureIsResultEntryEnabledKey(type);
        return helper.getBoolean(key, false);
    }

    private String getRacingProcedureIsResultEntryEnabledKey(RacingProcedureType type) {
        switch (type) {
            case RRS26:
            case RRS26_3MIN:
                return key(R.string.preference_racing_procedure_rrs26_resultentryenabled_key);
            case SWC:
            case SWC_4MIN:
                return key(R.string.preference_racing_procedure_swc_resultentryenabled_key);
            case GateStart:
                return key(R.string.preference_racing_procedure_gatestart_resultentryenabled_key);
            case ESS:
                return key(R.string.preference_racing_procedure_ess_resultentryenabled_key);
            case BASIC:
                return key(R.string.preference_racing_procedure_basic_resultentryenabled_key);
            case LEAGUE:
                return key(R.string.preference_racing_procedure_league_resultentryenabled_key);
            default:
                throw new IllegalArgumentException("Unknown racing procedure type.");
        }
    }

    public Set<Flags> getRRS26StartmodeFlags() {
        Set<String> flagNames = helper.getStringSet(key(R.string.preference_racing_procedure_rrs26_startmode_flags_key),
                new HashSet<String>());
        Set<Flags> flags = new HashSet<>();
        for (String flagName : flagNames) {
            flags.add(Flags.valueOf(flagName));
        }
        return flags;
    }

    public Set<Flags> getSWCStartmodeFlags() {
        Set<String> flagNames = helper.getStringSet(key(R.string.preference_racing_procedure_swc_startmode_flags_key),
                new HashSet<String>());
        Set<Flags> flags = new HashSet<>();
        for (String flagName : flagNames) {
            flags.add(Flags.valueOf(flagName));
        }
        return flags;
    }

    public String getServerBaseURL() {
        return helper.getString(
                key(R.string.preference_server_url_key),
                context.getString(R.string.preference_server_url_default)
        );
    }

    public double getWindBearingFromDirection() {
        long windBearingAsLong = helper.getLong(HIDDEN_PREFERENCE_WIND_BEARING, 0);
        return Double.longBitsToDouble(windBearingAsLong);
    }

    public double getWindSpeed() {
        long windSpeedAsLong = helper.getLong(HIDDEN_PREFERENCE_WIND_SPEED, 0);
        return Double.longBitsToDouble(windSpeedAsLong);
    }

    public LatLng getWindPosition() {
        double lat = Double.longBitsToDouble(helper.getLong(HIDDEN_PREFERENCE_WIND_LAT, 0));
        double lng = Double.longBitsToDouble(helper.getLong(HIDDEN_PREFERENCE_WIND_LNG, 0));
        return new LatLng(lat, lng);
    }

    public boolean isPollingActive() {
        return helper.getBoolean(key(R.string.preference_polling_active_key), false);
    }

    public boolean isSendingActive() {
        return helper.getBoolean(context.getResources().getString(R.string.preference_isSendingActive_key),
                context.getResources().getBoolean(R.bool.preference_isSendingActive_default));
    }

    protected String key(int keyId) {
        return context.getString(keyId);
    }

    public void registerPollingActiveChangedListener(final PollingActiveChangedListener listener) {
        if (pollingActiveChangedListeners.isEmpty()) {
            helper.getDevice().registerOnSharedPreferenceChangeListener(pollingActiveChangedListener);
        }
        pollingActiveChangedListeners.add(listener);
    }

    public void setBoatClass(BoatClassType boatClass) {
        String boatClassString = boatClass.name();
        helper.getEditor().putString(HIDDEN_PREFERENCE_BOAT_CLASS, boatClassString).commit();
    }

    public void setByNameCourseDesignerCourseNames(List<String> courseNames) {
        helper.getEditor().putStringSet(key(R.string.preference_course_designer_by_name_course_names_key),
                new HashSet<>(courseNames)).commit();
    }

    public void setCourseLayout(CourseLayouts courseLayout) {
        String courseLayoutString = courseLayout.name();
        helper.getEditor().putString(HIDDEN_PREFERENCE_COURSE_LAYOUT, courseLayoutString).commit();
    }

    public void setDefaultCourseDesignerMode(CourseDesignerMode mode) {
        helper.getEditor().putString(key(R.string.preference_course_designer_override_key), mode.name()).commit();
    }

    public void setDefaultRacingProcedureType(RacingProcedureType type) {
        helper.getEditor().putString(key(R.string.preference_racing_procedure_override_key), type.name()).commit();
    }

    public void setGateStartHasAdditionalGolfDownTime(boolean hasAdditionalGolfDownTime) {
        helper.getEditor().putBoolean(key(R.string.preference_racing_procedure_gatestart_hasadditionalgolfdowntime_key),
                hasAdditionalGolfDownTime).commit();
    }

    public void setGateStartHasPathfinder(boolean hasPathfinder) {
        helper.getEditor()
                .putBoolean(key(R.string.preference_racing_procedure_gatestart_haspathfinder_key), hasPathfinder)
                .commit();
    }

    public void setLoginType(LoginType type) {
        ExLog.i(context, this.getClass().toString(), "setLoginType: " + type);

        Editor setEdit = helper.getEditor();

        switch (type) {
            case NONE: {
                setEdit.putInt(HIDDEN_PREFERENCE_LOGIN_TYPE, 0);
                break;
            }
            case VIEWER: {
                setEdit.putInt(HIDDEN_PREFERENCE_LOGIN_TYPE, 1);
                break;
            }
            case OFFICER: {
                setEdit.putInt(HIDDEN_PREFERENCE_LOGIN_TYPE, 2);
                break;
            }

            default: {
                break;
            }
        }

        setEdit.commit();
    }

    public void setMailRecipient(String mail) {
        helper.getEditor().putString(key(R.string.preference_mail_key), mail).commit();
    }

    public void setManagedCourseAreaNames(List<String> courseAreaNames) {
        helper.getEditor().putStringSet(key(R.string.preference_course_areas_key), new HashSet<>(courseAreaNames))
                .commit();
    }

    public void setNumberOfRounds(NumberOfRounds numberOfRounds) {
        final String numberOfRoundsString = numberOfRounds.name();
        helper.getEditor().putString(HIDDEN_PREFERENCE_NUMBER_OF_ROUNDS, numberOfRoundsString).commit();
    }

    public void setRacingProcedureClassFlag(RacingProcedureType type, Flags flag) {
        final String key = getRacingProcedureClassFlagKey(type);
        helper.getEditor().putString(key, flag.name()).commit();
    }

    public void setRacingProcedureHasIndividualRecall(RacingProcedureType type, Boolean hasRecall) {
        final String key = getRacingProcedureHasIndividualRecallKey(type);
        helper.getEditor().putBoolean(key, hasRecall).commit();
    }

    public void setRacingProcedureIsResultEntryEnabled(RacingProcedureType type, Boolean resultEntryEnabled) {
        final String key = getRacingProcedureIsResultEntryEnabledKey(type);
        helper.getEditor().putBoolean(key, resultEntryEnabled).commit();
    }

    public void setRRS26StartmodeFlags(Set<Flags> flags) {
        Set<String> flagNames = new HashSet<>();
        for (Flags flag : flags) {
            flagNames.add(flag.name());
        }
        helper.getEditor().putStringSet(key(R.string.preference_racing_procedure_rrs26_startmode_flags_key), flagNames)
                .commit();
    }

    public void setSWCStartmodeFlags(Set<Flags> flags) {
        Set<String> flagNames = new HashSet<>();
        for (Flags flag : flags) {
            flagNames.add(flag.name());
        }
        helper.getEditor().putStringSet(key(R.string.preference_racing_procedure_swc_startmode_flags_key), flagNames)
                .commit();
    }

    public void setSendingActive(boolean activate) {
        ExLog.i(context, this.getClass().toString(), "setSendingActive: " + activate);
        helper.getEditor()
                .putBoolean(context.getResources().getString(R.string.preference_isSendingActive_key), activate)
                .commit();
    }

    public void setWindBearingFromDirection(double enteredWindBearing) {
        long windBearingAsLong = Double.doubleToLongBits(enteredWindBearing);
        helper.getEditor().putLong(HIDDEN_PREFERENCE_WIND_BEARING, windBearingAsLong).commit();
    }

    public void setWindSpeed(double enteredWindSpeed) {
        long windSpeedAsLong = Double.doubleToLongBits(enteredWindSpeed);
        helper.getEditor().putLong(HIDDEN_PREFERENCE_WIND_SPEED, windSpeedAsLong).commit();
    }

    public void setWindPosition(LatLng latLng) {
        long lat = Double.doubleToLongBits(latLng.latitude);
        long lng = Double.doubleToLongBits(latLng.longitude);
        helper.getEditor().putLong(HIDDEN_PREFERENCE_WIND_LAT, lat).putLong(HIDDEN_PREFERENCE_WIND_LNG, lng).commit();
    }

    public void unregisterPollingActiveChangedListener(PollingActiveChangedListener listener) {
        pollingActiveChangedListeners.remove(listener);
        if (pollingActiveChangedListeners.isEmpty()) {
            helper.getDevice().unregisterOnSharedPreferenceChangeListener(pollingActiveChangedListener);
        }
    }

    public boolean isDemoAllowed() {
        return helper.getBoolean(context.getString(R.string.preference_allow_demo_key),
                context.getResources().getBoolean(R.bool.preference_allow_demo_default));
    }

    public boolean wakelockEnabled() {
        return helper.getBoolean(context.getString(R.string.preference_wakelock_key),
                context.getResources().getBoolean(R.bool.preference_wakelock_default));
    }

    public boolean isOfflineMode() {
        return helper.getBoolean(context.getString(R.string.preference_offline_key),
                context.getResources().getBoolean(R.bool.preference_offline_default));
    }

    public boolean isDependentRacesAllowed() {
        return helper.getBoolean(context.getString(R.string.preference_allow_dependent_races_key),
                context.getResources().getBoolean(R.bool.preference_allow_dependent_races_default));
    }

    public int getDependentRacesOffset() {
        return helper.getInt(context.getString(R.string.preference_dependent_races_offset_key),
                context.getResources().getInteger(R.integer.preference_dependent_races_offset_default));
    }

    public int getProtestTimeDurationInMinutes() {
        return helper.getInt(context.getString(R.string.preference_protest_time_duration_key),
                context.getResources().getInteger(R.integer.preference_protest_time_duration_default));
    }

    public void setDefaultProtestTimeDurationInMinutes(int protestTimeInMinutes) {
        helper.getEditor()
                .putInt(context.getString(R.string.preference_protest_time_duration_key), protestTimeInMinutes)
                .commit();
    }

    public void setAccessToken(String accessToken) {
        helper.getEditor().putString(context.getString(R.string.preference_access_token_key), accessToken).commit();
    }

    public String getAccessToken() {
        return helper.getString(context.getString(R.string.preference_access_token_key), null);
    }

    public boolean isMagnetic() {
        return helper.getBoolean(context.getString(R.string.preference_heading_with_declination_subtracted_key),
                context.getResources().getBoolean(R.bool.preference_heading_with_declination_subtracted_default));
    }

    public boolean isRaceFactorChangeAllow() {
        return helper.getBoolean(context.getString(R.string.preference_allow_edit_race_factor_key),
                context.getResources().getBoolean(R.bool.preference_allow_edit_race_factor_default));
    }

    public String showNonPublic() {
        return helper.getBoolean(context.getString(R.string.preference_non_public_events_key),
                context.getResources().getBoolean(R.bool.preference_non_public_events_default)) ? "true" : "false";
    }

    public boolean needConfigRefresh() {
        return helper.getBoolean(context.getString(R.string.preference_config_needs_refresh_key),
                context.getResources().getBoolean(R.bool.preference_config_needs_refresh_default));
    }

    public void setNeedConfigRefresh(boolean refresh) {
        helper.getEditor().putBoolean(context.getString(R.string.preference_config_needs_refresh_key), refresh)
                .commit();
    }

    private class Helper {
        private SharedPreferences device;
        private SharedPreferences regatta;

        public Helper(@NonNull SharedPreferences device, @Nullable SharedPreferences regatta) {
            this.device = device;
            this.regatta = regatta;
        }

        public Editor getEditor() {
            return device.edit();
        }

        public SharedPreferences getDevice() {
            return device;
        }

        public boolean getBoolean(String key, boolean defValue) {
            boolean value = device.getBoolean(key, defValue);
            if (regatta != null) {
                value = regatta.getBoolean(key, value);
            }
            return value;
        }

        public String getString(String key, String defValue) {
            String value = device.getString(key, defValue);
            if (regatta != null) {
                value = regatta.getString(key, value);
            }
            return value;
        }

        public Set<String> getStringSet(String key, Set<String> defValue) {
            Set<String> value = device.getStringSet(key, defValue);
            if (regatta != null) {
                value = regatta.getStringSet(key, value);
            }
            return value;
        }

        public int getInt(String key, int defValue) {
            int value = device.getInt(key, defValue);
            if (regatta != null) {
                value = regatta.getInt(key, value);
            }
            return value;
        }

        public long getLong(String key, long defValue) {
            long value = device.getLong(key, defValue);
            if (regatta != null) {
                value = regatta.getLong(key, value);
            }
            return value;
        }
    }
}
