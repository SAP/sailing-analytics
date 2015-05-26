package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.sap.sailing.android.shared.util.ViewHolder;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.unscheduled.StartProcedure;
import com.sap.sailing.racecommittee.app.ui.adapters.unscheduled.StartProcedureAdapter;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.util.ArrayList;

public class StartProcedureFragment extends BaseFragment implements StartProcedureAdapter.RacingProcedureTypeClick {

    private final static String START_MODE = "startMode";

    private final ArrayList<StartProcedure> startProcedure = new ArrayList<>();

    public StartProcedureFragment() {

    }

    public static StartProcedureFragment newInstance(int startMode) {
        StartProcedureFragment fragment = new StartProcedureFragment();
        Bundle args = new Bundle();
        args.putInt(START_MODE, startMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_schedule_procedure, container, false);

        LinearLayout headerText = ViewHolder.get(layout, R.id.header_text);
        if (headerText != null) {
            headerText.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    openMainScheduleFragment();
                }
            });
        }

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments() != null) {
            switch (getArguments().getInt(START_MODE, 0)) {
            case 1:
                if (getView() != null) {
                    View header = getView().findViewById(R.id.header);
                    header.setVisibility(View.GONE);
                }
                break;

            default:
                break;
            }
        }

        String className;
        for (RacingProcedureType procedureType : RacingProcedureType.validValues()) {
            if (procedureType.equals(RacingProcedureType.GateStart)) {
                className = GateStartFragment.class.getSimpleName();
            } else {
                className = null;
            }
            startProcedure.add(new StartProcedure(procedureType, (getRaceState().getRacingProcedure().getType() == procedureType), className));
        }

        ListView listView = (ListView) getActivity().findViewById(R.id.listView);
        if (listView != null) {
            StartProcedureAdapter adapter = new StartProcedureAdapter(getActivity(), startProcedure, this);
            listView.setAdapter(adapter);
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

    @Override
    public void onClick(RacingProcedureType procedureType, String className) {
        getRaceState().setRacingProcedure(MillisecondsTimePoint.now(), procedureType);
        if (TextUtils.isEmpty(className)) {
            if (getArguments() != null && getArguments().getInt(START_MODE, 0) == 0) {
                openMainScheduleFragment();
            } else {
                getRaceState().forceNewStartTime(MillisecondsTimePoint.now(), getRaceState().getStartTime());
            }
        } else {
            if (getArguments() != null && getArguments().getInt(START_MODE, 0) == 0) {
                replaceFragment(GateStartFragment.Pathfinder.newInstance());
            } else {
                replaceFragment(GateStartFragment.Pathfinder.newInstance(1), R.id.race_frame);
            }
        }
    }
}
