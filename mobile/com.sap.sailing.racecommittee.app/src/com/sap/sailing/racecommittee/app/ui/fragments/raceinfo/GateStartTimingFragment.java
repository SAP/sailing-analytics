package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.sap.sailing.android.shared.util.ActivityHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.gate.GateStartRacingProcedure;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class GateStartTimingFragment extends BaseFragment {

    public final static int ONE_MINUTE_MILLISECONDS = 60000;

    private final static int MIN_VALUE = 0;
    private final static int MAX_VALUE = 60;

    private TextView mTotalTime;
    private NumberPicker mTimeLaunch;
    private NumberPicker mTimeGolf;
    private GateStartRacingProcedure mProcedure;

    public static GateStartTimingFragment newInstance() {
        return newInstance(START_MODE_PRESETUP);
    }

    public static GateStartTimingFragment newInstance(@START_MODE_VALUES int startMode) {
        GateStartTimingFragment fragment = new GateStartTimingFragment();
        Bundle args = new Bundle();
        args.putInt(START_MODE, startMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = LayoutInflater.from(getActivity()).inflate(R.layout.race_schedule_procedure_timing, container,
                false);

        HeaderLayout header = ViewHelper.get(layout, R.id.header);
        if (header != null) {
            header.setHeaderOnClickListener(v -> goHome());
        }

        View additional = ViewHelper.get(layout, R.id.addition_golf_time);
        if (additional != null) {
            if (preferences.getGateStartHasAdditionalGolfDownTime()) {
                additional.setVisibility(View.VISIBLE);
            }
        }

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTotalTime = getActivity().findViewById(R.id.total_time_text);
        mTimeLaunch = getActivity().findViewById(R.id.time_launch);
        mTimeGolf = getActivity().findViewById(R.id.time_golf);
        mProcedure = getRaceState().getTypedRacingProcedure();
        ViewHelper.disableSave(mTimeLaunch);
        ThemeHelper.setPickerColor(getActivity(), mTimeLaunch, ThemeHelper.getColor(getActivity(), R.attr.white),
                ThemeHelper.getColor(getActivity(), R.attr.sap_yellow_1));
        ViewHelper.disableSave(mTimeGolf);
        ThemeHelper.setPickerColor(getActivity(), mTimeGolf, ThemeHelper.getColor(getActivity(), R.attr.white),
                ThemeHelper.getColor(getActivity(), R.attr.sap_yellow_1));

        setTimeLaunchWidget();
        setTimeGolfWidget();
        setButton();
    }

    private void setTimeLaunchWidget() {
        if (mTimeLaunch != null) {
            mTimeLaunch.setMinValue(MIN_VALUE);
            mTimeLaunch.setMaxValue(MAX_VALUE);
            mTimeLaunch.setWrapSelectorWheel(false);
            int value = (int) (mProcedure.getGateLaunchStopTime() / ONE_MINUTE_MILLISECONDS);
            mTimeLaunch.setValue(value);
            mTimeLaunch.setOnValueChangedListener((picker, oldVal, newVal) -> mTotalTime.setText("" + (newVal + mTimeGolf.getValue())));
        }
    }

    private void setTimeGolfWidget() {
        if (mTimeGolf != null) {
            mTimeGolf.setMinValue(MIN_VALUE);
            mTimeGolf.setMaxValue(MAX_VALUE);
            mTimeGolf.setWrapSelectorWheel(false);
            int value = (int) (mProcedure.getGolfDownTime() / ONE_MINUTE_MILLISECONDS);
            mTimeGolf.setValue(value);
            mTimeGolf.setOnValueChangedListener((picker, oldVal, newVal) -> {
                int launch = 0;
                if (mTimeLaunch != null) {
                    launch = mTimeLaunch.getValue();
                }
                mTotalTime.setText("" + (newVal + launch));
            });
            int launch = 0;
            if (mTimeLaunch != null) {
                launch = mTimeLaunch.getValue();
            }
            mTotalTime.setText("" + (launch + mTimeGolf.getValue()));
        }
    }

    private void setButton() {
        View button = requireActivity().findViewById(R.id.set_gate_time);
        if (button != null) {
            button.setOnClickListener(v -> {
                ActivityHelper.with(getActivity()).hideKeyboard();
                long launch = 0;
                long golf = 0;

                if (mTimeLaunch != null) {
                    launch = mTimeLaunch.getValue() * ONE_MINUTE_MILLISECONDS;
                }
                if (mTimeGolf != null) {
                    golf = mTimeGolf.getValue() * ONE_MINUTE_MILLISECONDS;
                }

                GateStartRacingProcedure procedure = getRaceState().getTypedRacingProcedure();
                procedure.setGateLineOpeningTimes(MillisecondsTimePoint.now(), launch, golf);
                goHome();
            });
        }
    }
}
