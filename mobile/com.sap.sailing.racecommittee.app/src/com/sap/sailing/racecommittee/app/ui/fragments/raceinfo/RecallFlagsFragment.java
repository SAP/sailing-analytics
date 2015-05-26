package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.RecallFlagsAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.RecallFlagsAdapter.RecallFlag;
import com.sap.sailing.racecommittee.app.ui.adapters.RecallFlagsAdapter.RecallFlagItemClick;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RecallFlagsFragment extends RaceFragment implements RecallFlagItemClick {

    private RecallFlagsAdapter mAdapter;

    public RecallFlagsFragment() {

    }

    public static RecallFlagsFragment newInstance() {
        RecallFlagsFragment fragment = new RecallFlagsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.flag_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() != null) {
            ListView listView = (ListView) getView().findViewById(R.id.listView);
            if (listView != null) {
                mAdapter = new RecallFlagsAdapter(getActivity(), getRaceState().getTypedRacingProcedure(), this);
                listView.setAdapter(mAdapter);
            }
        }
    }

    @Override
    public void onClick(RecallFlag flag) {
        mAdapter.notifyDataSetChanged();
        if (flag.flag.equals(Flags.XRAY)) {
            RacingProcedure procedure = getRaceState().getTypedRacingProcedure();
            procedure.displayIndividualRecall(MillisecondsTimePoint.now());
        }
        if (flag.flag.equals(Flags.FIRSTSUBSTITUTE)) {
            TimePoint now = MillisecondsTimePoint.now();
            getRaceState().setGeneralRecall(now);
            // TODO see bug 1649: Explicit passing of pass identifier in RaceState interface
            getRaceState().setAdvancePass(now);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        sendIntent(AppConstants.INTENT_ACTION_TIME_HIDE);
    }

    @Override
    public void onPause() {
        super.onPause();

        sendIntent(AppConstants.INTENT_ACTION_TIME_SHOW);
    }
}
