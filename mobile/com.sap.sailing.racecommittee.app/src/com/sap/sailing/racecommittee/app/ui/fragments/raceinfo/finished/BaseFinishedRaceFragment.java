package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.finished;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.fragments.panels.FinishedButtonFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.panels.FinishedSubmitFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.BaseRaceInfoRaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceSummaryFragment;

public abstract class BaseFinishedRaceFragment<ProcedureType extends RacingProcedure>
        extends BaseRaceInfoRaceFragment<ProcedureType> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.race_finished, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() != null) {
            replaceFragment(FinishedButtonFragment.newInstance(getArguments()), R.id.finished_panel_buttons);
            replaceFragment(FinishedSubmitFragment.newInstance(getArguments()), R.id.finished_panel_desc);
            replaceFragment(RaceSummaryFragment.newInstance(getArguments()), R.id.finished_content);
        }
    }
}
