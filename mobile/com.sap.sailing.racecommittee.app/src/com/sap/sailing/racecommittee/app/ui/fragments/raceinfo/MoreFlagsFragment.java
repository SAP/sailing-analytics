package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.DataStore;
import com.sap.sailing.racecommittee.app.domain.impl.Result;
import com.sap.sailing.racecommittee.app.ui.adapters.MoreFlagsAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.MoreFlagsAdapter.MoreFlag;
import com.sap.sailing.racecommittee.app.ui.adapters.MoreFlagsAdapter.MoreFlagItemClick;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.DatePickerFragment;
import com.sap.sailing.racecommittee.app.ui.utils.FlagsResources;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.io.Serializable;
import java.util.Calendar;

public class MoreFlagsFragment extends BaseFragment implements MoreFlagItemClick {

    private MoreFlagsAdapter mAdapter;

    public MoreFlagsFragment() {}

    public static MoreFlagsFragment newInstance() {
        MoreFlagsFragment fragment = new MoreFlagsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sendIntent(AppConstants.ACTION_TIME_HIDE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.flag_list, container, false);

        ListView listView = view.findViewById(R.id.listView);
        if (listView != null) {
            mAdapter = new MoreFlagsAdapter(requireContext(), this);
            listView.setAdapter(mAdapter);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendIntent(AppConstants.ACTION_TIME_SHOW);
    }

    @Override
    public void onClick(MoreFlag flag) {
        mAdapter.notifyDataSetChanged();
        Toast.makeText(getActivity(), flag.file_name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showMore(MoreFlag flag) {
        if (flag.flag == Flags.BLUE) {
            replaceFragment(FinishTimeFragment.newInstance(0),
                    getFrameId(requireActivity(), R.id.race_edit, R.id.race_content, true));
        }
    }

    public static class FinishTimeFragment extends BaseFragment
            implements View.OnClickListener, DatePickerDialog.OnDateSetListener {

        private EventBase mEvent;

        private Button mDateButton;
        private TimePicker mTimePicker;
        private NumberPicker mSecondPicker;
        private TextView mCurrentTime;

        public FinishTimeFragment() {}

        public static FinishTimeFragment newInstance(int startMode) {
            FinishTimeFragment fragment = new FinishTimeFragment();
            Bundle args = new Bundle();
            args.putInt(START_MODE, startMode);
            fragment.setArguments(args);
            return fragment;
        }

        public int getStartMode() {
            return getArguments().getInt(START_MODE);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final DataStore dataStore = DataManager.create(requireContext()).getDataStore();
            final Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MILLISECOND, 0);
            final Serializable id = dataStore.getEventUUID();
            if (id != null) {
                mEvent = dataStore.getEvent(id);
                if (calendar.before(mEvent.getStartDate().asDate())) {
                    //Today is before the start date of the event
                    calendar.setTime(mEvent.getStartDate().asDate());
                } else if (calendar.after(mEvent.getEndDate().asDate())) {
                    //Today is after the end date of the event
                    calendar.setTime(mEvent.getEndDate().asDate());
                }
            }
        }

        @Override
        public void onAttachFragment(Fragment childFragment) {
            if (childFragment instanceof DatePickerFragment) {
                DatePickerFragment fragment = (DatePickerFragment) childFragment;
                fragment.setOnDateSetListener(this);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = inflater.inflate(R.layout.race_finish_config, container, false);

            final Calendar calendar = Calendar.getInstance();
            mDateButton = layout.findViewById(R.id.date_button);
            if (mDateButton != null) {
                mDateButton.setOnClickListener(this);
            }
            updateDateButton(calendar);

            mTimePicker = ViewHelper.get(layout, R.id.time_picker);
            mSecondPicker = ViewHelper.get(layout, R.id.second_picker);
            TimeUtils.initTimePickerWithSeconds(getActivity(), calendar, mTimePicker, mSecondPicker);

            mCurrentTime = ViewHelper.get(layout, R.id.current_time);

            View finishCurrent = ViewHelper.get(layout, R.id.finish_current);
            if (finishCurrent != null) {
                finishCurrent.setOnClickListener(this);
            }

            View finishCustom = ViewHelper.get(layout, R.id.finish_custom);
            if (finishCustom != null) {
                finishCustom.setOnClickListener(this);
            }

            return layout;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            final View view = getView();
            if (view != null) {
                View header = ViewHelper.get(view, R.id.header_text);
                if (header != null) {
                    header.setOnClickListener(this);
                }

                View back = ViewHelper.get(view, R.id.header_back);
                if (back != null) {
                    back.setVisibility(View.VISIBLE);
                }

                final Bundle args = getArguments();
                if (args != null) {
                    switch (args.getInt(START_MODE, 0)) {
                        case 0: // Race-State: Running -> Start Finishing
                            if (AppUtils.with(getActivity()).isLandscape()) {
                                if (header != null) {
                                    header.setOnClickListener(null);
                                }

                                if (back != null) {
                                    back.setVisibility(View.GONE);
                                }
                            }
                            break;

                        case 1: // Race-State: Finishing -> End Finishing
                            ImageView flag = ViewHelper.get(getView(), R.id.header_flag);
                            if (flag != null) {
                                flag.setImageDrawable(FlagsResources.getFlagDrawable(getActivity(), Flags.BLUE.name(),
                                        getResources().getInteger(R.integer.flag_size)));
                            }

                            TextView headline = ViewHelper.get(getView(), R.id.header_headline);
                            if (headline != null) {
                                headline.setText(getString(R.string.race_end_finish_header,
                                        TimeUtils.formatTime(getRaceState().getFinishingTime())));
                            }
                            break;

                        default:
                            break;
                    }
                }
            }
        }

        @Override
        public void onClick(View view) {
            final int id = view.getId();
            if (id == R.id.finish_current) {
                setFinishTime(MillisecondsTimePoint.now());
            } else if (id == R.id.date_button) {
                final Object tag = mDateButton.getTag();
                final Calendar time = tag instanceof Calendar ? (Calendar) tag : Calendar.getInstance();
                TimeUtils.showDatePickerDialog(getChildFragmentManager(), time, mEvent);
            } else if (id == R.id.finish_custom) {
                final Object tag = mDateButton.getTag();
                final Calendar calendar;
                if (tag instanceof Calendar) {
                    calendar = (Calendar) tag;
                } else {
                    calendar = Calendar.getInstance();
                    calendar.clear(Calendar.MILLISECOND);
                }
                final int year = calendar.get(Calendar.YEAR);
                final int month = calendar.get(Calendar.MONTH);
                final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                setFinishTime(TimeUtils.getTime(year, month, dayOfMonth, mTimePicker, mSecondPicker));
            } else {
                sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
            }
        }

        @Override
        public TickListener getCurrentTimeTickListener() {
            return this::onCurrentTimeTick;
        }

        private void onCurrentTimeTick(TimePoint now) {
            if (mCurrentTime != null) {
                mCurrentTime.setText(TimeUtils.formatTime(now));
            }
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            final Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(year, month, dayOfMonth);
            updateDateButton(calendar);
        }

        private void updateDateButton(final Calendar calendar) {
            if (mDateButton != null) {
                final long millis = calendar.getTimeInMillis();
                CharSequence text = DateUtils.formatDateTime(requireContext(), millis, DateUtils.FORMAT_ABBREV_ALL);
                if (DateUtils.isToday(millis)) {
                    text = TextUtils.concat(getText(R.string.today), ", ", text);
                }
                mDateButton.setText(text);
                mDateButton.setTag(calendar);
            }
        }

        private void setFinishTime(TimePoint finishTime) {
            Result result = new Result();
            final Bundle args = getArguments();
            // Race-State: Running -> Start Finishing
            if (args != null && args.getInt(START_MODE, 0) == 1) { // Race-State: Finishing -> End Finishing
                if (RaceLogRaceStatus.FINISHING.equals(getRace().getStatus())) {
                    result = getRace().setFinishedTime(finishTime);
                } else {
                    result.setError(R.string.error_wrong_race_state, RaceLogRaceStatus.FINISHING.name(),
                            getRace().getStatus().name());
                }
            } else {
                if (RaceLogRaceStatus.RUNNING.equals(getRace().getStatus())) {
                    result = getRace().setFinishingTime(finishTime);
                } else {
                    result.setError(R.string.error_wrong_race_state, RaceLogRaceStatus.RUNNING.name(),
                            getRace().getStatus().name());
                }
            }

            if (result.hasError()) {
                Toast.makeText(getActivity(), result.getMessage(requireContext()), Toast.LENGTH_LONG).show();
            }
        }
    }
}
