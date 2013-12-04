package com.sap.sailing.racecommittee.app.ui.fragments.preference;


import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;

import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.racecommittee.app.R;

public class RegattaDefaultsPreferenceFragment extends BasePreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_regatta_defaults);
        
        setupGeneral();
        setupRRS26();
        setupGateStart();
        setupESS();
        setupBasic();
        
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_rrs26_classflag_key));
        bindPreferenceSummaryToSet(findPreference(R.string.preference_racing_procedure_rrs26_startmode_flags_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_gatestart_classflag_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_ess_classflag_key));
        bindPreferenceSummaryToValue(findPreference(R.string.preference_racing_procedure_basic_classflag_key));
    }

    private void setupRRS26() {
        setupRRS26StartmodeFlagsList();
        setupClassFlagList(this.<ListPreference>findPreference(R.string.preference_racing_procedure_rrs26_classflag_key));
    }

    private void setupGateStart() {
        setupClassFlagList(this.<ListPreference>findPreference(R.string.preference_racing_procedure_gatestart_classflag_key));
    }

    private void setupESS() {
        setupClassFlagList(this.<ListPreference>findPreference(R.string.preference_racing_procedure_ess_classflag_key));
    }

    private void setupBasic() {
        setupClassFlagList(this.<ListPreference>findPreference(R.string.preference_racing_procedure_basic_classflag_key));
    }

    private void setupGeneral() {
        setupRacingProcedureTypePreference();
        setupCourseDesignerTypePreference();
    }

    private void setupRacingProcedureTypePreference() {
        final ListPreference startProcedurePreference = findPreference(R.string.preference_racing_procedure_override_key);
        
        List<CharSequence> entries = new ArrayList<CharSequence>();
        List<CharSequence> entryValues = new ArrayList<CharSequence>();
        for (RacingProcedureType type : RacingProcedureType.validValues()) {
            entries.add(type.toString());
            entryValues.add(type.name());
        }
        
        startProcedurePreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
        startProcedurePreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        
        bindPreferenceSummaryToValue(startProcedurePreference);
    }
    

    private void setupCourseDesignerTypePreference() {
        final ListPreference courseDesignerPreference = findPreference(R.string.preference_course_designer_override_key);
        
        List<CharSequence> entries = new ArrayList<CharSequence>();
        List<CharSequence> entryValues = new ArrayList<CharSequence>();
        for (CourseDesignerMode type : CourseDesignerMode.validValues()) {
            entries.add(type.toString());
            entryValues.add(type.name());
        }
        
        courseDesignerPreference.setEntries(entries.toArray(new CharSequence[0]));
        courseDesignerPreference.setEntryValues(entryValues.toArray(new CharSequence[0]));
        
        bindPreferenceSummaryToValue(courseDesignerPreference);
    }

    private void setupRRS26StartmodeFlagsList() {
        MultiSelectListPreference listPreference = findPreference(R.string.preference_racing_procedure_rrs26_startmode_flags_key);
        List<String> flags = new ArrayList<String>();
        for (Flags flag : Flags.validValues()) {
            flags.add(flag.name());
        }
        listPreference.setEntries(flags.toArray(new String[flags.size()]));
        listPreference.setEntryValues(flags.toArray(new String[flags.size()]));
    }

    private void setupClassFlagList(ListPreference preference) {
        List<CharSequence> entries = new ArrayList<CharSequence>();
        List<CharSequence> entryValues = new ArrayList<CharSequence>();
        for (Flags flag: Flags.validValues()) {
            entries.add(flag.toString());
            entryValues.add(flag.name());
        }
        
        preference.setEntries(entries.toArray(new CharSequence[entries.size()]));
        preference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
    }
}
