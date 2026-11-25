package com.sap.sailing.racecommittee.app.ui.fragments.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sap.sailing.android.shared.ui.fragments.preference.BasePreferenceFragment;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.activities.PreferenceActivity;

import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.ListPreference;
import android.text.TextUtils;

/**
 * <p>
 * This preference fragment can be redirected to display the {@link RegattaConfiguration} of a specific regatta.
 * </p>
 * <p/>
 * <p/>
 * See {@link PreferenceActivity}.
 */
public class RegattaPreferenceFragment extends BasePreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && !TextUtils
                .isEmpty(getArguments().getString(PreferenceActivity.EXTRA_SPECIFIC_REGATTA_PREFERENCES_NAME))) {
            getPreferenceManager().setSharedPreferencesName(
                    getArguments().getString(PreferenceActivity.EXTRA_SPECIFIC_REGATTA_PREFERENCES_NAME));
        }
        addPreferencesFromResource(R.xml.preference_regatta_defaults);

        setupGeneral();
        setupRRS26();
        setupSWC();
        setupGateStart();
        setupESS();
        setupBasic();
        setupLeague();
    }

    private void setupGeneral() {
        setupRacingProcedureTypePreference();
        setupCourseDesignerTypePreference();
        setupDependentRacesPreference();
        setupProtestTimePreference();
    }

    private void setupRRS26() {
        setupClassFlagList(
                this.<ListPreference> findPreference(R.string.preference_racing_procedure_rrs26_classflag_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_rrs26_classflag_key));

        setupStartmodeFlagsList(R.string.preference_racing_procedure_rrs26_startmode_flags_key);
        bindPreferenceSummaryToSet(findPreference(R.string.preference_racing_procedure_rrs26_startmode_flags_key));
    }

    private void setupSWC() {
        setupClassFlagList(
                this.<ListPreference> findPreference(R.string.preference_racing_procedure_swc_classflag_key));
        bindPreferenceSummaryToSet(findPreference(R.string.preference_racing_procedure_swc_startmode_flags_key));

        setupStartmodeFlagsList(R.string.preference_racing_procedure_swc_startmode_flags_key);
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_swc_classflag_key));
    }

    private void setupGateStart() {
        setupClassFlagList(
                this.<ListPreference> findPreference(R.string.preference_racing_procedure_gatestart_classflag_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_gatestart_classflag_key));
    }

    private void setupESS() {
        setupClassFlagList(
                this.<ListPreference> findPreference(R.string.preference_racing_procedure_ess_classflag_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_ess_classflag_key));
    }

    private void setupBasic() {
        setupClassFlagList(
                this.<ListPreference> findPreference(R.string.preference_racing_procedure_basic_classflag_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_basic_classflag_key));
    }

    private void setupLeague() {
        setupClassFlagList(
                this.<ListPreference> findPreference(R.string.preference_racing_procedure_league_classflag_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_league_classflag_key));
    }

    private void setupRacingProcedureTypePreference() {
        final ListPreference startProcedurePreference = findPreference(
                R.string.preference_racing_procedure_override_key);

        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();
        for (RacingProcedureType type : RacingProcedureType.validValues()) {
            entries.add(type.toString());
            entryValues.add(type.name());
        }

        startProcedurePreference.setEntries(entries.toArray(new String[entries.size()]));
        startProcedurePreference.setEntryValues(entryValues.toArray(new String[entryValues.size()]));

        bindPreferenceSummaryToValue(startProcedurePreference);
    }

    private void setupCourseDesignerTypePreference() {
        final ListPreference preference = findPreference(R.string.preference_course_designer_override_key);

        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();
        for (CourseDesignerMode type : CourseDesignerMode.validValues()) {
            entries.add(type.toString());
            entryValues.add(type.name());
        }

        preference.setEntries(entries.toArray(new String[entries.size()]));
        preference.setEntryValues(entryValues.toArray(new String[entryValues.size()]));

        bindPreferenceSummaryToValue(preference);

        preference.setSummary(CourseDesignerMode.valueOf(preference.getValue()).toString());
    }

    private void setupDependentRacesPreference() {
        bindPreferenceSummaryToInteger(findPreference(R.string.preference_dependent_races_offset_key));
    }

    private void setupProtestTimePreference() {
        bindPreferenceSummaryToInteger(findPreference(R.string.preference_protest_time_duration_key));
    }

    private void setupStartmodeFlagsList(@StringRes int prefId) {
        MultiSelectListPreference preference = findPreference(prefId);
        List<String> flags = new ArrayList<>();
        for (Flags flag : Flags.validValues()) {
            flags.add(flag.name());
        }

        Collections.sort(flags);
        preference.setEntries(flags.toArray(new String[flags.size()]));
        preference.setEntryValues(flags.toArray(new String[flags.size()]));

        Set<String> values = preference.getValues();
        List<String> checkedFlags = new ArrayList<>(values);
        Collections.sort(checkedFlags);
        preference.setSummary(checkedFlags.toString());
    }

    private void setupClassFlagList(ListPreference preference) {
        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();
        for (Flags flag : Flags.validValues()) {
            entries.add(flag.toString());
            entryValues.add(flag.name());
        }

        Collections.sort(entries);
        Collections.sort(entryValues);
        preference.setEntries(entries.toArray(new String[entries.size()]));
        preference.setEntryValues(entryValues.toArray(new String[entryValues.size()]));
    }
}
