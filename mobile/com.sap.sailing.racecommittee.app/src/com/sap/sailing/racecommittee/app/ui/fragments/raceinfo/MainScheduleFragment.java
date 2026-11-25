package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.scoring.AdditionalScoringInformationType;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ess.ESSRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.gate.GateStartRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.ConfigurableStartModeFlagRacingProcedure;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.impl.SelectionItem;
import com.sap.sailing.racecommittee.app.ui.activities.RacingActivity;
import com.sap.sailing.racecommittee.app.ui.adapters.SelectionAdapter;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.chooser.RaceInfoFragmentChooser;
import com.sap.sailing.racecommittee.app.ui.utils.FlagsResources;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.util.ArrayList;
import java.util.UUID;

public class MainScheduleFragment extends BaseFragment implements View.OnClickListener, SelectionAdapter.ItemClick {

    public static final String START_TIME = "startTime";
    public static final String DEPENDENT_RACE = "dependentRace";
    public static final String START_TIME_DIFF = "startTimeDiff";
    public static final String RACE_GROUP = "raceGroup";

    private static final String TAG = MainScheduleFragment.class.getName();

    private SelectionAdapter mAdapter;
    private ArrayList<SelectionItem> mItems;
    private SelectionItem mItemStartTime;
    private SelectionItem mItemRaceGroup;
    private SelectionItem mItemStartWind;

    private String mStartTimeString;
    private SimpleRaceLogIdentifier mRaceId;
    private TimePoint mStartTime;
    private Duration mStartTimeDiff;
    private RacingProcedure mRacingProcedure;

    private int mFlagSize;

    public MainScheduleFragment() {}

    public static MainScheduleFragment newInstance() {
        return new MainScheduleFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_schedule, container, false);

        mItems = new ArrayList<>();
        RecyclerView raceData = ViewHelper.get(layout, R.id.race_data);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mAdapter = new SelectionAdapter(getActivity(), mItems, this);
        raceData.setLayoutManager(layoutManager);
        raceData.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        raceData.setAdapter(mAdapter);

        View startButton = ViewHelper.get(layout, R.id.start_race);
        if (startButton != null) {
            startButton.setOnClickListener(this);
        }

        mFlagSize = getResources().getInteger(R.integer.flag_size);

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initStartTime();
        initStartMode();
        initCourse();
        initWind();
    }

    @Override
    public void onStart() {
        super.onStart();
        getRaceState().addChangedListener(stateChangedListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        getRaceState().removeChangedListener(stateChangedListener);
    }

    private void initCourse() {
        Runnable runnable = () -> openFragment(CourseFragment.newInstance(START_MODE_PRESETUP, preferences));
        SelectionItem courseItem = new SelectionItem(getString(R.string.course), null, null, false, false, runnable);
        if (getRaceState().getCourseDesign() != null) {
            courseItem.setValue(getCourseName());
        }
        mItems.add(courseItem);
    }

    private void initWind() {
        Runnable runnable = () -> openFragment(WindFragment.newInstance(START_MODE_PRESETUP));
        mItemStartWind = new SelectionItem(getString(R.string.wind), null, null, false, false, runnable);
        mItems.add(mItemStartWind);
    }

    private void initStartMode() {
        mRacingProcedure = getRaceState().getRacingProcedure();
        if (mRacingProcedure != null) {
            Runnable runnableProcedure = () -> openFragment(StartProcedureFragment.newInstance(START_MODE_PRESETUP));
            mItems.add(new SelectionItem(getString(R.string.start_procedure), mRacingProcedure.getType().toString(),
                    null, false, false, runnableProcedure));
            if (mRacingProcedure instanceof ConfigurableStartModeFlagRacingProcedure) {
                final ConfigurableStartModeFlagRacingProcedure procedure = getRaceState().getTypedRacingProcedure();
                Flags flag = procedure.getStartModeFlag();
                Runnable runnableMode = () -> openFragment(StartModeFragment.newInstance(START_MODE_PRESETUP));
                Drawable drawable = FlagsResources.getFlagDrawable(getActivity(), flag.name(), mFlagSize);
                mItems.add(new SelectionItem(getString(R.string.start_mode), flag.name(), drawable, false, false,
                        runnableMode));
            } else if (mRacingProcedure instanceof GateStartRacingProcedure) {
                GateStartRacingProcedure procedure = getRaceState().getTypedRacingProcedure();
                if (procedure != null) {
                    Runnable runnablePathfinder = () -> openFragment(GateStartPathFinderFragment.newInstance(START_MODE_PRESETUP));
                    mItems.add(new SelectionItem(getString(R.string.gate_start_pathfinder), procedure.getPathfinder(),
                            null, false, false, runnablePathfinder));

                    Runnable runnableTiming = () -> openFragment(GateStartTimingFragment.newInstance(START_MODE_PRESETUP));
                    mItems.add(new SelectionItem(getString(R.string.gate_start_timing),
                            RaceHelper.getGateTiming(getActivity(), procedure, getRace().getRaceGroup()), null, false,
                            false, runnableTiming));
                }
            } else if (mRacingProcedure instanceof ESSRacingProcedure) {
                ESSRacingProcedure procedure = getRaceState().getTypedRacingProcedure();
                if (procedure != null) {
                    boolean checked = false;
                    final Bundle args = getArguments();
                    if (args != null) {
                        checked = args.getBoolean(RACE_GROUP,
                                getRaceState().isAdditionalScoringInformationEnabled(
                                        AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE));
                    }
                    mItemRaceGroup = new SelectionItem(getString(R.string.race_group), null, null, true, checked, null);
                    mItems.add(mItemRaceGroup);
                }
            }
        }
    }

    private void initStartTime() {
        final RacingActivity activity = (RacingActivity) getActivity();
        TimePoint timePoint = null;
        final Bundle args = getArguments();
        if (args != null) {
            timePoint = (TimePoint) args.getSerializable(START_TIME);
            mRaceId = (SimpleRaceLogIdentifier) getArguments().getSerializable(DEPENDENT_RACE);
            mStartTimeDiff = (Duration) getArguments().getSerializable(START_TIME_DIFF);
        }
        if (timePoint == null && activity != null) {
            timePoint = activity.getStartTime();
        }
        if (timePoint != null) {
            if (activity != null) {
                activity.setStartTime(timePoint);
            }
            mStartTime = timePoint;
            mStartTimeString = TimeUtils.formatTime(timePoint);
        }

        if (mRaceId != null && mStartTimeDiff != null) {
            ManagedRace race = DataManager.create(getActivity()).getDataStore().getRace(mRaceId);
            mStartTimeString = getString(R.string.minutes_after_long, mStartTimeDiff.asMinutes(),
                    RaceHelper.getShortReverseRaceName(race, " / ", getRace()));
        }

        Runnable runnable = () -> openFragment(StartTimeFragment.newInstance(getArguments()));
        mItemStartTime = new SelectionItem(getString(R.string.start_time), mStartTimeString, null, false, false,
                runnable);
        mItems.add(mItemStartTime);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start_race) {
            startRace();
        } else {
            ExLog.i(getActivity(), TAG, "Clicked on " + view);
        }
    }

    private void startRace() {
        ConfigurableStartModeFlagRacingProcedure lineStartRacingProcedure = null;
        Flags flag = null;
        if (getRaceState().getRacingProcedure() instanceof ConfigurableStartModeFlagRacingProcedure) {
            lineStartRacingProcedure = getRaceState().getTypedRacingProcedure();
            flag = lineStartRacingProcedure.getStartModeFlag();
        }
        TimePoint now = MillisecondsTimePoint.now();
        getRaceState().setRacingProcedure(now, mRacingProcedure.getType());
        if (mRacingProcedure instanceof ESSRacingProcedure && mItemRaceGroup != null) {
            getRaceState().setAdditionalScoringInformationEnabled(MillisecondsTimePoint.now(),
                    /* enable */ mItemRaceGroup.isChecked(),
                    AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
        }
        final ReadonlyDataManager dataManager = OnlineDataManager.create(getActivity());
        final UUID courseAreaId = dataManager.getDataStore().getCourseAreaId();
        if (mStartTimeDiff == null && mRaceId == null) {
            getRaceState().forceNewStartTime(now, mStartTime, courseAreaId);
        } else {
            getRaceState().forceNewDependentStartTime(now, mStartTimeDiff, mRaceId, courseAreaId);
        }
        if (lineStartRacingProcedure != null) {
            lineStartRacingProcedure.setStartModeFlag(MillisecondsTimePoint.now(), flag);
        }
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
        if (mItemStartTime != null && !TextUtils.isEmpty(mStartTimeString)) {
            if (mRaceId == null && mStartTimeDiff == null) {
                String startTimeValue = getString(R.string.start_time_value, mStartTimeString, calcCountdown(now));
                mItemStartTime.setValue(startTimeValue);
            } else {
                mItemStartTime.setValue(mStartTimeString);
            }
        }

        if (getRaceState().getWindFix() != null) {
            Wind wind = getRaceState().getWindFix();
            String sensorData = getString(R.string.wind_sensor, TimeUtils.formatTime(wind.getTimePoint()),
                    wind.getFrom().getDegrees(), wind.getKnots());
            mItemStartWind.setValue(sensorData);
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private String calcCountdown(TimePoint timePoint) {
        if (mStartTime.before(timePoint)) {
            final String duration = TimeUtils.formatDurationSince(timePoint.minus(mStartTime.asMillis()).asMillis());
            return "-" + duration;
        }
        return TimeUtils.formatDurationUntil(mStartTime.minus(timePoint.asMillis()).asMillis());
    }

    private void openFragment(RaceFragment fragment) {
        if (fragment.getArguments() != null) {
            fragment.getArguments().putAll(getRecentArguments());
        } else {
            fragment.setArguments(getRecentArguments());
        }

        Bundle args = fragment.getArguments();
        args.putSerializable(START_TIME, mStartTime);
        args.putSerializable(START_TIME_DIFF, mStartTimeDiff);
        args.putSerializable(DEPENDENT_RACE, mRaceId);
        if (mItemRaceGroup != null) {
            args.putBoolean(RACE_GROUP, mItemRaceGroup.isChecked());
        }

        requireFragmentManager().beginTransaction()
                .replace(R.id.racing_view_container, fragment)
                .commit();
    }

    @Override
    public void onItemClick(Runnable runnable) {
        if (runnable != null) {
            Handler handler = new Handler();
            handler.post(runnable);
        }
    }

    private final RaceStateChangedListener stateChangedListener = new BaseRaceStateChangedListener() {
        @Override
        public void onStatusChanged(ReadonlyRaceState state) {
            super.onStatusChanged(state);
            openFragment(RaceInfoFragmentChooser.choose(getActivity(), getRace()));
        }
    };
}
