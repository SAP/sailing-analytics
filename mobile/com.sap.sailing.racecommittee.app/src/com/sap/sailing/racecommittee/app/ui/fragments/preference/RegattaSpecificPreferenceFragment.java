package com.sap.sailing.racecommittee.app.ui.fragments.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.sap.sailing.android.shared.ui.fragments.preference.BasePreferenceFragment;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.ui.activities.PreferenceActivity;
import com.sap.sse.common.util.NaturalComparator;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;

public class RegattaSpecificPreferenceFragment extends BasePreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        Set<RaceGroup> raceGroups = getRaceGroups(context);
        if (raceGroups.isEmpty()) {
            Preference preference = new Preference(context);
            preference.setTitle(R.string.preference_there_are_no_regattas);
            screen.addPreference(preference);
        } else {
            List<RaceGroup> sortedGroups = new ArrayList<RaceGroup>(raceGroups);
            Collections.sort(sortedGroups, new RaceGroupComparator());
            for (RaceGroup raceGroup : sortedGroups) {
                if (raceGroup.getRegattaConfiguration() != null) {
                    addPreference(screen, raceGroup);
                }
            }
        }
        setPreferenceScreen(screen);
    }

    private Set<RaceGroup> getRaceGroups(Context context) {
        ReadonlyDataManager dataManager = DataManager.create(context);
        return dataManager.getDataStore().getRaceGroups();
    }

    private void addPreference(PreferenceScreen screen, final RaceGroup raceGroup) {
        Preference preference = new Preference(screen.getContext());
        preference.setTitle(getString(R.string.configure_regatta, raceGroup.getName()));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                PreferenceActivity.openSpecificRegattaConfiguration(getActivity(), raceGroup);
                return false;
            }
        });
        screen.addPreference(preference);
    }

    private static class RaceGroupComparator implements Comparator<RaceGroup> {

        private NaturalComparator nameComparator = new NaturalComparator();

        @Override
        public int compare(RaceGroup lhs, RaceGroup rhs) {
            return nameComparator.compare(lhs.getName(), rhs.getName());
        }

    }

}
