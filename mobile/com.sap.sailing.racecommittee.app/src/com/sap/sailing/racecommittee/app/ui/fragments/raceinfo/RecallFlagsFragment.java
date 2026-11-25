package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.RecallFlagsAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.RecallFlagsAdapter.RecallFlag;
import com.sap.sailing.racecommittee.app.ui.adapters.RecallFlagsAdapter.RecallFlagItemClick;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RecallFlagsFragment extends RaceFragment implements RecallFlagItemClick {

    private final static String HEADER_TEXT = "headerText";

    private RecallFlagsAdapter mAdapter;

    public static RecallFlagsFragment newInstance(String header_text) {
        RecallFlagsFragment fragment = new RecallFlagsFragment();
        Bundle args = new Bundle();
        args.putString(HEADER_TEXT, header_text);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.flag_list, container, false);

        HeaderLayout header = ViewHelper.get(layout, R.id.header);
        if (header != null) {
            if (getArguments() != null) {
                header.setHeaderText(getArguments().getString(HEADER_TEXT, getString(R.string.not_available)));
            }
            header.setHeaderOnClickListener(v -> sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT));
        }

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() != null) {
            ListView listView = ViewHelper.get(getView(), R.id.listView);
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
}
