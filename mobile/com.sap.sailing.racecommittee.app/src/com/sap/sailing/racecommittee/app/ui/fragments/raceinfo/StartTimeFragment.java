package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.app.DatePickerDialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.racegroup.RaceGroupSeriesFleet;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.DataStore;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.ui.adapters.DependentRaceSpinnerAdapter;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.DatePickerFragment;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.util.NaturalComparator;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StartTimeFragment extends BaseFragment
        implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePicker.OnTimeChangedListener {

    public static final int MODE_TIME_PANEL = 2;

    private static final int MAX_DIFF_MIN = 60;
    private static final int NONE = -1;
    private static final int ABSOLUTE = 0;
    private static final int RELATIVE = 1;

    private static final String KEY_START_TIME = "start_time";

    private DataStore mDataStore;
    private EventBase mEvent;

    private TimePoint mStartTime;

    private View mAbsolute;
    private View mRelative;
    private Button mAbsoluteButton;
    private Button mRelativeButton;
    private Button mSetStartAbsolute;
    private Button mSetStartRelative;
    private Button mDateButton;

    private NumberPicker mTimeOffset;
    private Spinner mLeaderBoard;
    private Spinner mFleet;
    private Spinner mRace;
    private boolean mRaceSetupFinished;
    private TimePicker mTimePicker;
    private NumberPicker mStartSeconds;
    private TextView mCountdown;
    private TextView mDebugTime;
    private SimpleRaceLogIdentifier mRaceId;
    private Duration mStartTimeOffset;
    private boolean mListenerIgnore;
    private final Map<RaceGroupSeriesFleet, List<ManagedRace>> mGroupHeaders = new LinkedHashMap<>();
    private DependentRaceSpinnerAdapter mLeaderBoardAdapter;
    private DependentRaceSpinnerAdapter mFleetAdapter;
    private DependentRaceSpinnerAdapter mRaceAdapter;
    private SimpleRaceLogIdentifier identifier;

    /**
     * Listens for start time changes
     */
    private RaceStateChangedListener raceStateChangedListener;

    public static StartTimeFragment newInstance(int startMode) {
        StartTimeFragment fragment = new StartTimeFragment();
        Bundle args = new Bundle();
        args.putInt(START_MODE, startMode);
        fragment.setArguments(args);
        return fragment;
    }

    public static StartTimeFragment newInstance(Bundle extraArgs) {
        StartTimeFragment fragment = newInstance(START_MODE_PRESETUP);
        Bundle args = fragment.getArguments();
        if (args != null && extraArgs != null) {
            args.putAll(extraArgs);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataStore = DataManager.create(requireContext()).getDataStore();
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        final Serializable id = mDataStore.getEventUUID();
        if (id != null) {
            mEvent = mDataStore.getEvent(id);
            final TimePoint startDate = mEvent.getStartDate();
            final TimePoint endDate = mEvent.getEndDate();
            if (startDate != null && calendar.before(startDate.asDate())) {
                //Today is before the start date of the event
                calendar.setTime(startDate.asDate());
            } else if (endDate != null && calendar.after(endDate.asDate())) {
                //Today is after the end date of the event
                calendar.setTime(endDate.asDate());
            }
        }
        mStartTime = new MillisecondsTimePoint(calendar.getTime());
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
        final View view = inflater.inflate(R.layout.race_schedule_start_time, container, false);

        mAbsolute = view.findViewById(R.id.time_absolute);
        mRelative = view.findViewById(R.id.time_relative);

        if (preferences.isDependentRacesAllowed()) {
            View tabButtons = view.findViewById(R.id.tab_button);
            if (tabButtons != null) {
                tabButtons.setVisibility(View.VISIBLE);
            }

            mAbsoluteButton = view.findViewById(R.id.absolute);
            if (mAbsoluteButton != null) {
                mAbsoluteButton.setOnClickListener(v -> showTab(ABSOLUTE));
            }

            mRelativeButton = view.findViewById(R.id.relative);
            if (mRelativeButton != null) {
                mRelativeButton.setOnClickListener(v -> showTab(RELATIVE));
            }
        }

        mCountdown = view.findViewById(R.id.start_countdown);

        Button syncMinute = view.findViewById(R.id.sync_to_minute);
        if (syncMinute != null) {
            syncMinute.setOnClickListener(this);
        }

        mSetStartAbsolute = view.findViewById(R.id.set_start_time_absolute);
        if (mSetStartAbsolute != null) {
            mSetStartAbsolute.setOnClickListener(this);
        }

        mSetStartRelative = view.findViewById(R.id.set_start_time_relative);
        if (mSetStartRelative != null) {
            mSetStartRelative.setOnClickListener(this);
        }

        mDebugTime = view.findViewById(R.id.debug_time);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        raceStateChangedListener = new BaseRaceStateChangedListener() {
            @Override
            public void onStartTimeChanged(ReadonlyRaceState state) {
                mStartTime = state.getStartTime();
                onStartTimeTick(MillisecondsTimePoint.now());
            }
        };

        final View view = getView();
        if (view != null && getArguments() != null) {
            View header = view.findViewById(R.id.header);
            View back = view.findViewById(R.id.header_back);
            View text = view.findViewById(R.id.header_text);
            View sync = view.findViewById(R.id.sync_to_minute);
            switch (getArguments().getInt(START_MODE, START_MODE_PRESETUP)) {
                case START_MODE_PLANNED:
                    if (back != null) {
                        back.setVisibility(View.VISIBLE);
                    }
                    if (text != null) {
                        text.setOnClickListener(this);
                    }
                    break;

                case MODE_TIME_PANEL: {
                    final TimePoint startTime = getRaceState().getStartTime();
                    if (startTime != null) {
                        mStartTime = startTime;
                    }
                    if (AppUtils.with(getActivity()).isLandscape()) {
                        if (header != null) {
                            header.setVisibility(View.GONE);
                        }
                    } else {
                        if (back != null) {
                            back.setVisibility(View.VISIBLE);
                        }
                        if (text != null) {
                            text.setOnClickListener(this);
                        }
                    }
                    if (sync != null) {
                        sync.setVisibility(View.VISIBLE);
                    }

                    StartTimeFinderResult result = getRaceState().getStartTimeFinderResult();
                    if (result != null && result.isDependentStartTime()) {
                        mStartTimeOffset = result.getStartTimeDiff();
                        mRaceId = Util.get(result.getDependingOnRaces(), 0);
                    }
                    break;
                }
                //MODE_SETUP
                default: {
                    final TimePoint startTime = (TimePoint) getArguments().getSerializable(MainScheduleFragment.START_TIME);
                    if (startTime == null) {
                        final Calendar calendar = Calendar.getInstance();
                        calendar.setTime(mStartTime.asDate());
                        // In 10 minutes from now, but always a 5-minute-mark.
                        calendar.add(Calendar.MINUTE, 10);
                        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);
                        minute = (int) (Math.ceil((minute / 5.0)) * 5.0);
                        if (minute >= 60) {
                            hourOfDay++;
                            minute = 0;
                        }
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        mStartTime = new MillisecondsTimePoint(calendar.getTime());
                    } else {
                        mStartTime = startTime;
                    }
                    mStartTimeOffset = (Duration) getArguments().getSerializable(MainScheduleFragment.START_TIME_DIFF);
                    mRaceId = (SimpleRaceLogIdentifier) getArguments().getSerializable(MainScheduleFragment.DEPENDENT_RACE);

                    View startSeconds = ViewHelper.get(getView(), R.id.start_time_seconds);
                    if (startSeconds != null) {
                        startSeconds.setVisibility(View.GONE);
                    }
                    break;
                }
            }

            initViewsAbsolute(getView());
            initViewsRelative(getView());
        }

        if (mRaceId == null && mStartTimeOffset == null) {
            showTab(ABSOLUTE);
        } else {
            showTab(RELATIVE);
        }

        // reset Set Time button after init
        if (getArguments() != null && getArguments().getInt(START_MODE, START_MODE_PRESETUP) == MODE_TIME_PANEL) {
            activateSetTime(NONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getRaceState().removeChangedListener(raceStateChangedListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        getRaceState().addChangedListener(raceStateChangedListener);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(KEY_START_TIME, mStartTime);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mStartTime = (TimePoint) savedInstanceState.getSerializable(KEY_START_TIME);
            updateDateButton();
            updateTimePickerAndSeconds();
        }
    }

    private void initViewsRelative(View rootView) {
        mTimeOffset = rootView.findViewById(R.id.time_offset);
        if (mTimeOffset != null) {
            ViewHelper.disableSave(mTimeOffset);
            ThemeHelper.setPickerColor(getActivity(), mTimeOffset,
                    ThemeHelper.getColor(requireContext(), R.attr.white),
                    ThemeHelper.getColor(requireContext(), R.attr.sap_yellow_1));
            mTimeOffset.setMinValue(0);
            mTimeOffset.setMaxValue(MAX_DIFF_MIN);
            mTimeOffset.setWrapSelectorWheel(false);
            mTimeOffset.setValue((mStartTimeOffset == null) ? preferences.getDependentRacesOffset()
                    : (int) mStartTimeOffset.asMinutes());
            mTimeOffset.setOnValueChangedListener((picker, oldVal, newVal) -> activateSetTime(RELATIVE));
        }

        List<ManagedRace> sortedRaces = new ArrayList<>(mDataStore.getRaces());
        Collections.sort(sortedRaces, (lhs, rhs) -> new NaturalComparator().compare(lhs.getId(), rhs.getId()));

        for (ManagedRace race : sortedRaces) {
            RaceGroupSeriesFleet container = new RaceGroupSeriesFleet(race);

            if (!mGroupHeaders.containsKey(container)) {
                mGroupHeaders.put(container, new LinkedList<>());
            }
            final List<ManagedRace> managedRaces = mGroupHeaders.get(container);
            if (managedRaces != null) {
                managedRaces.add(race);
            }
        }

        mLeaderBoard = rootView.findViewById(R.id.dependent_leaderboard);
        if (mLeaderBoard != null) {
            int leaderBoard = -1;
            mLeaderBoardAdapter = new DependentRaceSpinnerAdapter(getActivity(), R.layout.dependent_race_item);
            for (RaceGroupSeriesFleet races : mGroupHeaders.keySet()) {
                Util.Pair<String, String> data = new Util.Pair<>(races.getRaceGroup().getName(),
                        races.getRaceGroup().getDisplayName());
                int position = mLeaderBoardAdapter.add(data);
                if (position >= 0) {
                    if (mRaceId != null) {
                        if (mRaceId.getRegattaLikeParentName().equals(data.getA())) {
                            leaderBoard = position;
                        }
                    } else {
                        if (leaderBoard == -1
                                && getRace().getRaceGroup().getName().equals(races.getRaceGroup().getName())) {
                            leaderBoard = position;
                        }
                    }
                }
            }

            mLeaderBoard.setAdapter(mLeaderBoardAdapter);
            mLeaderBoard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mLeaderBoardAdapter.setSelected(position);
                    initFleetSpinner();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mLeaderBoardAdapter.setSelected(0);
                }
            });
            mLeaderBoard.setSelection(leaderBoard);
        }

        mFleet = rootView.findViewById(R.id.dependent_fleet);
        mRace = rootView.findViewById(R.id.dependent_racegroup);
    }

    private void initFleetSpinner() {
        if (mLeaderBoard != null && mFleet != null) {
            int fleet = -1;
            mFleetAdapter = new DependentRaceSpinnerAdapter(getActivity(), R.layout.dependent_race_item);
            mFleet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mFleetAdapter.setSelected(position);
                    initRaceSpinner();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mFleetAdapter.setSelected(0);
                }
            });
            for (RaceGroupSeriesFleet races : mGroupHeaders.keySet()) {
                Util.Pair<String, String> leaderBoard = mLeaderBoardAdapter
                        .getItem(mLeaderBoard.getSelectedItemPosition());
                if (races.getRaceGroup().getName().equals(leaderBoard.getA())) {
                    Util.Pair<String, String> data = new Util.Pair<>(races.getFleet().getName(), null);
                    if (hasRaces(data.getA())) {
                        int position = mFleetAdapter.add(data);
                        if (position >= 0) {
                            if (mRaceId != null) {
                                if (mRaceId.getFleetName().equals(data.getA())) {
                                    fleet = position;
                                }
                            } else {
                                if (fleet == -1) { // TODO add more heuristic here - to be discussed
                                    fleet = position;
                                }
                            }
                        }
                    }
                }
            }

            if (mFleetAdapter.getCount() > 1 || (mFleetAdapter.getCount() == 1
                    && !mFleetAdapter.getItem(0).getA().equals(LeaderboardNameConstants.DEFAULT_FLEET_NAME))) {
                mFleet.setAdapter(mFleetAdapter);
                mFleet.setSelection(fleet);
                mFleet.setVisibility(View.VISIBLE);
            } else {
                mFleet.setAdapter(null);
                mFleet.setVisibility(View.GONE);
                initRaceSpinner();
            }
        }
    }

    private void initRaceSpinner() {
        if (mLeaderBoard != null && mFleet != null && mRace != null) {
            mRaceAdapter = new DependentRaceSpinnerAdapter(getActivity(), R.layout.dependent_race_item);
            mRace.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mRaceAdapter.setSelected(position);
                    String leaderBoard = mLeaderBoardAdapter.getItem(mLeaderBoard.getSelectedItemPosition()).getA();
                    @SuppressWarnings("unchecked")
                    Util.Pair<String, String> fleet = ((Util.Pair<String, String>) mFleet.getSelectedItem());
                    if (fleet == null) {
                        fleet = new Util.Pair<>(LeaderboardNameConstants.DEFAULT_FLEET_NAME, null);
                    }
                    String race = mRaceAdapter.getItem(mRace.getSelectedItemPosition()).getA();
                    identifier = new SimpleRaceLogIdentifierImpl(leaderBoard, race, fleet.getA());
                    if (mRaceSetupFinished) {
                        activateSetTime(RELATIVE);
                    }
                    mRaceSetupFinished = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    mRaceAdapter.setSelected(0);
                    identifier = null;
                }
            });

            int racePos = -1;
            for (RaceGroupSeriesFleet races : mGroupHeaders.keySet()) {
                Util.Pair<String, String> leaderBoard = mLeaderBoardAdapter
                        .getItem(mLeaderBoard.getSelectedItemPosition());
                if (races.getRaceGroup().getName().equals(leaderBoard.getA())) {
                    @SuppressWarnings("unchecked")
                    Util.Pair<String, String> fleet = ((Util.Pair<String, String>) mFleet.getSelectedItem());
                    if (fleet == null) {
                        fleet = new Util.Pair<>(LeaderboardNameConstants.DEFAULT_FLEET_NAME, null);
                    }
                    if (races.getFleet().getName().equals(fleet.getA())) {
                        final List<ManagedRace> managedRaces = mGroupHeaders.get(races);
                        if (managedRaces != null) {
                            for (ManagedRace race : managedRaces) {
                                if (!getRace().equals(race) && race.getStatus() != RaceLogRaceStatus.FINISHED) {
                                    Util.Pair<String, String> data = new Util.Pair<>(race.getRaceColumnName(), null);
                                    int position = mRaceAdapter.add(data);
                                    if (position >= 0) {
                                        if (mRaceId != null) {
                                            if (mRaceId.getRaceColumnName().equals(data.getA())) {
                                                racePos = position;
                                            }
                                        } else {
                                            if (racePos == -1) { // TODO add more heuristic here - to be discussed
                                                racePos = position;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            mRace.setAdapter(mRaceAdapter);
            mRace.setVisibility(View.VISIBLE);
            mRace.setSelection(racePos);
            if (mRaceAdapter.getCount() > 1) {
                mRace.setEnabled(true);
            } else {
                mRace.setEnabled(false);
            }
        }
    }

    private boolean hasRaces(@Nullable String fleet) {
        for (RaceGroupSeriesFleet races : mGroupHeaders.keySet()) {
            Util.Pair<String, String> leaderBoard = mLeaderBoardAdapter.getItem(mLeaderBoard.getSelectedItemPosition());
            if (races.getRaceGroup().getName().equals(leaderBoard.getA())) {
                if (races.getFleet().getName().equals(fleet)) {
                    final List<ManagedRace> managedRaces = mGroupHeaders.get(races);
                    if (managedRaces != null) {
                        for (ManagedRace race : managedRaces) {
                            if (!getRace().equals(race) && race.getStatus() != RaceLogRaceStatus.FINISHED) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void initViewsAbsolute(View rootView) {
        mDateButton = rootView.findViewById(R.id.start_date_button);
        if (mDateButton != null) {
            mDateButton.setOnClickListener(this);
        }
        updateDateButton();

        mTimePicker = rootView.findViewById(R.id.start_time_picker);
        if (mTimePicker != null) {
            ThemeHelper.setPickerColor(getActivity(), mTimePicker,
                    ThemeHelper.getColor(requireContext(), R.attr.white),
                    ThemeHelper.getColor(requireContext(), R.attr.sap_yellow_1));
            mTimePicker.setOnTimeChangedListener(this);
            mTimePicker.setIs24HourView(true);
        }
        mStartSeconds = rootView.findViewById(R.id.start_time_seconds);
        if (mStartSeconds != null) {
            ViewHelper.disableSave(mStartSeconds);
            ThemeHelper.setPickerColor(getActivity(), mStartSeconds,
                    ThemeHelper.getColor(requireContext(), R.attr.sap_light_gray),
                    ThemeHelper.getColor(requireContext(), R.attr.sap_light_gray));
            ThemeHelper.setPickerTextSize(getActivity(), mStartSeconds, R.dimen.textSize_14);
            mStartSeconds.setEnabled(false);
        }
        updateTimePickerAndSeconds();
    }

    @Override
    public TimePoint getStartTime() {
        return mStartTime;
    }

    @Override
    public TickListener getStartTimeTickListener() {
        return this::onStartTimeTick;
    }

    private void onStartTimeTick(TimePoint now) {
        if (mAbsolute.getVisibility() == View.VISIBLE && mStartTime != null) {
            TimePoint timePoint;
            String duration;
            if (mStartTime.after(now)) {
                timePoint = mStartTime.minus(now.asMillis());
                duration = "-" + TimeUtils.formatDurationUntil(timePoint.asMillis());
            } else {
                timePoint = now.minus(mStartTime.asMillis());
                duration = TimeUtils.formatDurationSince(timePoint.asMillis());
            }

            if (mCountdown != null) {
                mCountdown.setText(duration);
            }

            if (mDebugTime != null) {
                mDebugTime.setText(mStartTime.asDate().toString());
            }
        }
    }

    private void showTab(int tab) {
        int colorGrey = ThemeHelper.getColor(requireContext(), R.attr.sap_light_gray);
        int colorOrange = ThemeHelper.getColor(requireContext(), R.attr.sap_yellow_1);

        if (mAbsolute != null) {
            mAbsolute.setVisibility(View.GONE);
        }
        if (mRelative != null) {
            mRelative.setVisibility(View.GONE);
        }
        if (mAbsoluteButton != null) {
            mAbsoluteButton.setTextColor(colorGrey);
            BitmapHelper.setBackground(mAbsoluteButton, null);
        }
        if (mRelativeButton != null) {
            mRelativeButton.setTextColor(colorGrey);
            BitmapHelper.setBackground(mRelativeButton, null);
        }

        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.nav_drawer_tab_button);

        if (tab == RELATIVE) {
            if (mRelative != null) {
                mRelative.setVisibility(View.VISIBLE);
            }
            if (mRelativeButton != null) {
                mRelativeButton.setTextColor(colorOrange);
                BitmapHelper.setBackground(mRelativeButton, drawable);
            }
        } else {
            // ABSOLUTE
            if (mAbsolute != null) {
                mAbsolute.setVisibility(View.VISIBLE);
            }
            if (mAbsoluteButton != null) {
                mAbsoluteButton.setTextColor(colorOrange);
                BitmapHelper.setBackground(mAbsoluteButton, drawable);
            }
        }
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.set_start_time_absolute) {
            changeFragment(mStartTime);
        } else if (id == R.id.set_start_time_relative) {
            changeFragment(mStartTime, new MillisecondsDurationImpl(mTimeOffset.getValue() * 60 * 1000), identifier);
        } else if (id == R.id.sync_to_minute) {
            syncToMinute();
        } else if (id == R.id.header_text) {
            if (getArguments() != null
                    && getArguments().getInt(START_MODE, START_MODE_PRESETUP) == START_MODE_PRESETUP) {
                changeFragment();
            } else {
                sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
            }
        } else if (id == R.id.start_date_button) {
            TimeUtils.showDatePickerDialog(getChildFragmentManager(), mStartTime, mEvent);
        }
    }

    private void syncToMinute() {
        final TimePoint now = MillisecondsTimePoint.now();
        mStartTime = mStartTime.getNearestModuloOneMinute(now);
        mListenerIgnore = true;
        updateTimePickerAndSeconds();
        activateSetTime(ABSOLUTE);
        unregisterTickListeners();
        registerTickListeners();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(mStartTime.asDate());
        calendar.set(year, month, dayOfMonth);
        mStartTime = new MillisecondsTimePoint(calendar.getTime());
        updateDateButton();
        activateSetTime(ABSOLUTE);
        unregisterTickListeners();
        registerTickListeners();
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(mStartTime.asDate());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        if (mListenerIgnore) {
            mListenerIgnore = false;
        } else {
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            setSeconds(0, 0);
        }
        mStartTime = new MillisecondsTimePoint(calendar.getTime());
        activateSetTime(ABSOLUTE);
        unregisterTickListeners();
        registerTickListeners();
    }

    private void activateSetTime(int tab) {
        switch (tab) {
            case ABSOLUTE:
                if (mSetStartAbsolute != null) {
                    mSetStartAbsolute.setEnabled(true);
                }
                break;

            case RELATIVE:
                if (mSetStartRelative != null) {
                    mSetStartRelative.setEnabled(true);
                }
                break;

            default:
                if (mSetStartAbsolute != null) {
                    mSetStartAbsolute.setEnabled(false);
                }
                if (mSetStartRelative != null) {
                    mSetStartRelative.setEnabled(false);
                }
        }
    }

    private void updateDateButton() {
        if (mDateButton != null) {
            CharSequence text = DateUtils.formatDateTime(requireContext(), mStartTime.asMillis(), DateUtils.FORMAT_ABBREV_ALL);
            if (DateUtils.isToday(mStartTime.asMillis())) {
                text = TextUtils.concat(getText(R.string.today), ", ", text);
            }
            mDateButton.setText(text);
        }
    }

    /**
     * sets the time displayed in the time picker to the value of {@link #mStartTime}
     */
    private void updateTimePickerAndSeconds() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(mStartTime.asDate());

        if (mTimePicker != null) {
            mTimePicker.setOnTimeChangedListener(null);
            mTimePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
            mTimePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
            mTimePicker.setOnTimeChangedListener(this);
        }

        setSeconds(calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND));
    }

    private void setSeconds(int sec, int msec) {
        if (mStartSeconds != null) {
            double seconds = sec + msec / (double) 1000;
            DecimalFormat format = new DecimalFormat("00.000");
            mStartSeconds.setDisplayedValues(new String[]{format.format(seconds)});
        }
    }

    private void changeFragment() {
        changeFragment(null, null, null);
    }

    private void changeFragment(TimePoint startTime) {
        changeFragment(startTime, null, null);
    }

    private void changeFragment(TimePoint startTime, Duration startTimeDiff, SimpleRaceLogIdentifier identifier) {
        final TimePoint now = MillisecondsTimePoint.now();
        final Bundle args = getRecentArguments();
        final RacingProcedureType procedureType = getRaceState().getTypedRacingProcedure().getType();
        getRaceState().setRacingProcedure(now, procedureType);
        int viewId = R.id.racing_view_container;
        RaceFragment fragment = MainScheduleFragment.newInstance();
        if (getArguments() != null && startTime != null) {
            if (getArguments().getInt(START_MODE, START_MODE_PRESETUP) != START_MODE_PRESETUP) {
                if (startTimeDiff == null && identifier == null) {
                    // absolute start time
                    getRaceState().forceNewStartTime(now, startTime, mDataStore.getCourseAreaId());
                } else {
                    // relative start time
                    getRaceState().forceNewDependentStartTime(now, startTimeDiff, identifier, mDataStore.getCourseAreaId());
                }
                viewId = R.id.race_content;
                fragment = RaceFlagViewerFragment.newInstance();
            }
            args.putAll(getArguments());
            if (startTimeDiff == null) {
                args.putSerializable(MainScheduleFragment.START_TIME, startTime);
            } else {
                args.putSerializable(MainScheduleFragment.START_TIME_DIFF, startTimeDiff);
                args.putSerializable(MainScheduleFragment.DEPENDENT_RACE, identifier);
            }
        }
        fragment.setArguments(args);
        requireFragmentManager().beginTransaction()
                .replace(viewId, fragment)
                .commit();
        if (requireActivity().findViewById(R.id.race_edit) != null) {
            sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
        }
    }
}
