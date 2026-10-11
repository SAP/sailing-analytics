package com.sap.sailing.racecommittee.app.ui.fragments.panels;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.ui.NavigationEvents;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.AbortFlagsFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.CourseFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.GateStartPathFinderFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.GateStartTimingFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.MoreFlagsFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.PenaltyFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceFinishingFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceFlagViewerFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RecallFlagsFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.StartModeFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.StartProcedureFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.StartTimeFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.TrackingListFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.WindFragment;
import com.sap.sailing.racecommittee.app.ui.layouts.TimePanelHeaderLayout;
import com.sap.sailing.racecommittee.app.ui.views.PanelButton;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

import static com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult.ResolutionFailed.NO_START_TIME_SET;

public class TimePanelFragment extends BasePanelFragment implements NavigationEvents.NavigationListener {

    private final static String TOGGLED = "toggled";

    private TimePanelHeaderLayout mRaceHeader;
    private PanelButton mCompetitorList;

    private View mTimeLock;
    private TextView mCurrentTime;
    private TextView mHeaderTime;
    private TextView mTimeStart;
    private TextView mFirstVesselDuration;
    private ImageView mLinkIcon;
    private Boolean mLinkedRace = null;

    private TimePoint mLastFinishingTime = null;

    public TimePanelFragment() {
    }

    public static TimePanelFragment newInstance(Bundle args) {
        TimePanelFragment fragment = new TimePanelFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_panel_time, container, false);

        mRaceHeader = ViewHelper.get(layout, R.id.race_content_header);
        mRaceHeader.setRunnable(() -> new RaceHeaderClick().onClick(null));

        mCompetitorList = ViewHelper.get(layout, R.id.button_competitor);

        mTimeLock = ViewHelper.get(layout, R.id.time_start_lock);
        mCurrentTime = ViewHelper.get(layout, R.id.current_time);
        mHeaderTime = ViewHelper.get(layout, R.id.timer_text);
        mTimeStart = ViewHelper.get(layout, R.id.time_start);
        mLinkIcon = ViewHelper.get(layout, R.id.linked_race);
        mFirstVesselDuration = ViewHelper.get(layout, R.id.first_vessel_duration);

        if (getArguments() != null && getArguments().getBoolean(TOGGLED)) {
            toggleMarker(layout, R.id.time_marker);
        }

        return layout;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        NavigationEvents.INSTANCE.subscribeFragmentAttachment(this);
    }

    @Override
    public void onDetach() {
        NavigationEvents.INSTANCE.unSubscribeFragmentAttachment(this);
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (preferences.getRacingProcedureIsResultEntryEnabled(getRaceState().getRacingProcedure().getType())) {
            mCompetitorList.setListener(new CompetitorPanelClick());
            mCompetitorList.setVisibility(View.VISIBLE);
            checkWarnings(getRaceState());
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        checkStatus();

        getRaceState().addChangedListener(stateChangedListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        getRaceState().removeChangedListener(stateChangedListener);
    }

    @Override
    public TickListener getCurrentTimeTickListener() {
        return this::onCurrentTimeTick;
    }

    @Override
    public TickListener getStartTimeTickListener() {
        return this::onStartTimeTick;
    }

    private void onCurrentTimeTick(TimePoint now) {
        if (mCurrentTime != null) {
            mCurrentTime.setText(TimeUtils.formatTime(now));
        }
    }

    private void onStartTimeTick(TimePoint now) {
        final TimePoint startTime = getRaceState().getStartTime();

        if (mTimeStart != null) {
            if (startTime != null) {
                mTimeStart.setText(getString(R.string.time_start, TimeUtils.formatTime(startTime, false)));
            } else {
                mTimeStart.setText(getString(R.string.time_start, "N/A"));
            }
        }

        if (mHeaderTime != null && startTime != null) {
            String duration;
            if (startTime.after(now)) {
                duration = "-" + TimeUtils.formatDurationUntil(startTime.minus(now.asMillis()).asMillis(), false);
            } else {
                duration = TimeUtils.formatDurationSince(now.minus(startTime.asMillis()).asMillis(), false);
            }
            mHeaderTime.setText(duration);
        }

        if (mLinkedRace == null) {
            StartTimeFinderResult result = getRaceState().getStartTimeFinderResult();
            if (result != null) {
                mLinkedRace = result.isDependentStartTime();
                if (mLinkedRace && mHeaderTime != null && result.getResolutionFailed() == NO_START_TIME_SET) {
                    SimpleRaceLogIdentifier identifier = Util.get(result.getDependingOnRaces(), 0);
                    ManagedRace race = DataManager.create(getActivity()).getDataStore().getRace(identifier);
                    mHeaderTime.setText(getString(R.string.minutes_after_long, result.getStartTimeDiff().asMinutes(),
                            RaceHelper.getShortReverseRaceName(race, " / ", getRace())));
                }
            }
        }

        if (mLinkedRace != null && mLinkIcon != null) {
            mLinkIcon.setVisibility(mLinkedRace ? View.VISIBLE : View.GONE);
        }

        if (mFirstVesselDuration != null && mFirstVesselDuration.getVisibility() == View.VISIBLE) {
            if (!getRaceState().getFinishingTime().equals(mLastFinishingTime)) {
                mLastFinishingTime = getRaceState().getFinishingTime();
                String raceDuration = TimeUtils.formatTimeAgo(getActivity(),
                        mLastFinishingTime.minus(getRaceState().getStartTime().asMillis()).asMillis());
                mFirstVesselDuration.setText(raceDuration);
            }
        }
    }

    private void uncheckMarker() {
        if (isAdded()) {
            if (!isNormal(mRaceHeader, R.id.time_marker)) {
                resetFragment(mTimeLock, getFrameId(requireActivity(), R.id.race_edit, R.id.race_content, false),
                        StartTimeFragment.class);
                setMarkerLevel(mRaceHeader, R.id.time_marker, LEVEL_NORMAL);
            }
            resetFragment(mCompetitorList.isLocked(),
                    getFrameId(requireActivity(), R.id.race_edit, R.id.race_content, false), PenaltyFragment.class);
            mCompetitorList.setMarkerLevel(PanelButton.LEVEL_NORMAL);
        }
    }

    private void checkStatus() {
        switch (getRace().getStatus()) {
            case UNSCHEDULED:
            case PRESCHEDULED:
            case SCHEDULED:
            case STARTPHASE:
                changeVisibility(mTimeLock, null, View.GONE);
                changeVisibility(mFirstVesselDuration, null, View.GONE);
                break;
            case FINISHING:
            case FINISHED:
                changeVisibility(mTimeLock, null, View.VISIBLE);
                changeVisibility(mFirstVesselDuration, null, View.VISIBLE);
                break;
            default:
                changeVisibility(mTimeLock, null, View.VISIBLE);
                changeVisibility(mFirstVesselDuration, null, View.GONE);
                break;
        }
    }

    private void checkWarnings(ReadonlyRaceState state) {
        CompetitorResults draft = state.getFinishPositioningList();
        CompetitorResults confirmed = state.getConfirmedFinishPositioningList().getCompetitorResults();
        mCompetitorList.showAdditionalImage(
                (draft != null && draft.hasConflicts()) || (confirmed != null && confirmed.hasConflicts()));
    }

    @Override
    public void onFragmentAttach(Fragment fragment) {
        if (fragment instanceof StartTimeFragment) {
            setMarkerLevel(mRaceHeader, R.id.time_marker, LEVEL_TOGGLED);
        }
        if (fragment instanceof PenaltyFragment || fragment instanceof TrackingListFragment) {
            mCompetitorList.setMarkerLevel(PanelButton.LEVEL_TOGGLED);
        }
        handlePanelTimeVisibility(fragment);

    }

    @Override
    public void onFragmentDetach(Fragment fragment) {
        if (fragment instanceof StartTimeFragment) {
            setMarkerLevel(mRaceHeader, R.id.time_marker, LEVEL_NORMAL);
        }
        if (fragment instanceof PenaltyFragment || fragment instanceof TrackingListFragment) {
            mCompetitorList.setMarkerLevel(LEVEL_NORMAL);
        }
    }

    private final RaceStateChangedListener stateChangedListener = new BaseRaceStateChangedListener() {
        @Override
        public void onStatusChanged(ReadonlyRaceState state) {
            checkStatus();
            uncheckMarker();
        }

        @Override
        public void onStartTimeChanged(ReadonlyRaceState state) {
            mLinkedRace = null;
        }

        @Override
        public void onFinishingPositionsChanged(ReadonlyRaceState state) {
            checkWarnings(state);
        }

        @Override
        public void onFinishingPositionsConfirmed(ReadonlyRaceState state) {
            checkWarnings(state);
        }
    };

    private class RaceHeaderClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = RaceHeaderClick.class.getName();
        private final View container = mRaceHeader;
        private final int markerId = R.id.time_marker;

        public void onClick(View v) {
            if (mTimeLock != null) {
                if (mTimeLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
                    showChangeDialog(this);
                } else {
                    toggleFragment();
                }
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            toggleFragment();
        }

        private void toggleFragment() {
            switch (toggleMarker(container, markerId)) {
                case 0:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case 1:
                    replaceFragment(StartTimeFragment.newInstance(StartTimeFragment.MODE_TIME_PANEL));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
                    break;
            }
            disableToggle(container, markerId);
        }
    }

    private class CompetitorPanelClick implements PanelButton.PanelButtonClick {

        private final String TAG = CompetitorPanelClick.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case PanelButton.LEVEL_NORMAL:
                    Intent intent = new Intent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    intent.putExtra(AppConstants.ACTION_EXTRA_FORCED, true);
                    BroadcastManager.getInstance(getActivity()).addIntent(intent);
                    break;

                case PanelButton.LEVEL_TOGGLED:
                    Bundle args = new Bundle();
                    RaceFragment content;
                    args.putSerializable(AppConstants.EXTRA_RACE_ID, getRace().getId());
                    if (getRace().getStatus() != RaceLogRaceStatus.FINISHING) {
                        content = PenaltyFragment.newInstance();
                    } else {
                        content = TrackingListFragment.newInstance(args, 1);
                    }
                    replaceFragment(content, R.id.race_content);
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
                    break;
            }
            view.disableToggle();
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            // no-op
        }
    }

    private void setPanelTimeVisibility(final boolean visible) {
        final FragmentActivity fragmentActivity = requireActivity();
        View view = fragmentActivity.findViewById(R.id.race_panel_time);
        if (fragmentActivity.findViewById(R.id.race_edit) == null && view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void handlePanelTimeVisibility(Fragment fragment) {
        boolean shouldHide = fragment instanceof AbortFlagsFragment ||
                fragment instanceof CourseFragment ||
                fragment instanceof GateStartPathFinderFragment ||
                fragment instanceof GateStartTimingFragment ||
                fragment instanceof MoreFlagsFragment ||
                fragment instanceof MoreFlagsFragment.FinishTimeFragment ||
                fragment instanceof RecallFlagsFragment ||
                fragment instanceof StartModeFragment ||
                fragment instanceof StartProcedureFragment ||
                fragment instanceof WindFragment;
        boolean shouldShow = fragment instanceof PenaltyFragment ||
                fragment instanceof TrackingListFragment ||
                fragment instanceof RaceFinishingFragment ||
                fragment instanceof StartTimeFragment ||
                fragment instanceof RaceFlagViewerFragment;
        setPanelTimeVisibility(!shouldHide && shouldShow);
    }
}
