package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;

import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.impl.Result;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.IntDef;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import android.widget.Toast;

public class RaceTimeChangeFragment extends BaseFragment implements View.OnClickListener {

    @IntDef({ START_TIME_MODE, FINISHING_TIME_MODE, FINISHED_TIME_MODE })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TIME_CHANGE_MODE {
    }

    private final static String TIME_MODE = "timeMode";
    public final static int START_TIME_MODE = 0;
    public final static int FINISHING_TIME_MODE = 1;
    public final static int FINISHED_TIME_MODE = 2;

    private static final int FUTURE_DAYS_DEFAULT = 3;
    private static final int PAST_DAYS_DEFAULT = -3;

    private NumberPicker mDatePicker;
    private TimePicker mTimePicker;
    private NumberPicker mSecondPicker;

    private int mFutureDays = FUTURE_DAYS_DEFAULT;
    private int mPastDays = PAST_DAYS_DEFAULT;

    public static RaceTimeChangeFragment newInstance(@TIME_CHANGE_MODE int timeMode) {
        RaceTimeChangeFragment fragment = new RaceTimeChangeFragment();
        Bundle args = new Bundle();
        args.putInt(TIME_MODE, timeMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.race_time_change, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View layout = getView();
        if (layout == null) {
            return;
        }

        final Calendar calendar = Calendar.getInstance();
        HeaderLayout header = (HeaderLayout) layout.findViewById(R.id.header);
        if (header != null) {
            header.setHeaderOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeFragment();
                }
            });
            switch (getArguments().getInt(TIME_MODE, START_TIME_MODE)) {
            case START_TIME_MODE:
                calendar.setTime(getRaceState().getStartTime().asDate());
                header.setHeaderText(getString(R.string.race_summary_start));
                break;

            case FINISHING_TIME_MODE:
                calendar.setTime(getRaceState().getFinishingTime().asDate());
                header.setHeaderText(getString(R.string.race_summary_finish_begin));
                break;

            case FINISHED_TIME_MODE:
                calendar.setTime(getRaceState().getFinishedTime().asDate());
                header.setHeaderText(getString(R.string.race_summary_finish_end));
                break;
            }
        }

        mDatePicker = ViewHelper.get(layout, R.id.date_picker);
        if (mDatePicker != null) {
            Calendar start = (Calendar) calendar.clone();
            Calendar finishing = (Calendar) calendar.clone();
            Calendar finished = (Calendar) calendar.clone();
            switch (getArguments().getInt(TIME_MODE, START_TIME_MODE)) {
            case START_TIME_MODE:
                finishing.setTime(getRaceState().getFinishingTime().asDate());
                mFutureDays = TimeUtils.daysBetween(finishing, calendar);
                break;

            case FINISHING_TIME_MODE:
                start.setTime(getRaceState().getStartTime().asDate());
                mPastDays = TimeUtils.daysBetween(start, calendar);
                finished.setTime(getRaceState().getFinishedTime().asDate());
                mFutureDays = TimeUtils.daysBetween(finished, calendar);
                break;

            case FINISHED_TIME_MODE:
                finishing.setTime(getRaceState().getFinishingTime().asDate());
                mPastDays = TimeUtils.daysBetween(finishing, calendar);
                break;

            default:
                // use default values
            }
            ViewHelper.disableSave(mDatePicker);
            ThemeHelper.setPickerColor(getActivity(), mDatePicker, ThemeHelper.getColor(getActivity(), R.attr.white),
                    ThemeHelper.getColor(getActivity(), R.attr.sap_yellow_1));
            TimeUtils.initDatePicker(getActivity(), mDatePicker, calendar, mPastDays, mFutureDays, false);
            mDatePicker.setValue(Math.abs(mPastDays));
            mDatePicker.setTag(calendar);
            if (mDatePicker.getMinValue() == mDatePicker.getMaxValue()) {
                mDatePicker.setVisibility(View.GONE);
            }
        }

        mTimePicker = ViewHelper.get(layout, R.id.time_picker);
        mSecondPicker = ViewHelper.get(layout, R.id.second_picker);
        TimeUtils.initTimePickerWithSeconds(getActivity(), calendar, mTimePicker, mSecondPicker);

        View setTime = layout.findViewById(R.id.set_date_time);
        if (setTime != null) {
            setTime.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        TimePoint time = getPickerTime();
        Result result;
        switch (getArguments().getInt(TIME_MODE)) {
        case FINISHING_TIME_MODE:
            result = getRace().setFinishingTime(time);
            if (result.hasError()) {
                Toast.makeText(getActivity(), result.getMessage(getActivity()), Toast.LENGTH_LONG).show();
            }
            break;
        case FINISHED_TIME_MODE:
            result = getRace().setFinishedTime(time);
            if (result.hasError()) {
                Toast.makeText(getActivity(), result.getMessage(getActivity()), Toast.LENGTH_LONG).show();
            }
            break;
        default: // START_TIME_MODE
            final ReadonlyDataManager dataManager = OnlineDataManager.create(getActivity());
            getRaceState().forceNewStartTime(MillisecondsTimePoint.now(), time, dataManager.getDataStore().getCourseAreaId());
            break;
        }
        closeFragment();
    }

    private TimePoint getPickerTime() {
        Calendar calendar = (Calendar) mDatePicker.getTag();
        calendar.add(Calendar.DAY_OF_MONTH, mDatePicker.getValue() + mPastDays);
        calendar.set(Calendar.HOUR_OF_DAY, mTimePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE, mTimePicker.getCurrentMinute());
        calendar.set(Calendar.SECOND, mSecondPicker.getValue());
        calendar.set(Calendar.MILLISECOND, 0);
        return new MillisecondsTimePoint(calendar.getTime());
    }

    private void closeFragment() {
        Intent intent = new Intent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
        BroadcastManager.getInstance(getActivity()).addIntent(intent);
    }
}
