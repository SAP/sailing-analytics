package com.sap.sailing.racecommittee.app.ui.fragments.panels;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.NavigationEvents;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.AbortFlagsFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.EmptyFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.MoreFlagsFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RecallFlagsFragment;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.TimePoint;

public class FlagPanelFragment extends BasePanelFragment implements NavigationEvents.NavigationListener {

    // Abandon Toggle
    private View mAbandonFlags;
    private View mAbandonLock;
    private View mAbandonLayer;

    // Recall Toggle
    private View mRecallFlags;
    private View mRecallLock;
    private View mRecallLayer;

    // Postpone Toggle
    private View mPostponeFlags;
    private View mPostponeLock;
    private View mPostponeLayer;

    // Course Toggle
    private View mCourseFlags;
    private View mCourseLock;

    // Blue First
    private View mBlueFirstFlag;
    private View mBlueFirstLock;

    // Blue Last
    private View mBlueLastFlag;
    private View mBlueLastLock;
    private TextView mBlueLastText;

    private final RaceStateChangedListener stateChangedListener = new BaseRaceStateChangedListener() {
        @Override
        public void onStatusChanged(ReadonlyRaceState state) {
            checkStatus();
        }
    };

    public static FlagPanelFragment newInstance(Bundle args) {
        FlagPanelFragment fragment = new FlagPanelFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_panel_flags, container, false);

        mAbandonFlags = ViewHelper.get(layout, R.id.abandon_flags);
        if (mAbandonFlags != null) {
            mAbandonFlags.setOnClickListener(new AbandonFlagsClick());
        }
        mAbandonLock = ViewHelper.get(layout, R.id.abandon_flags_lock);
        mAbandonLayer = ViewHelper.get(layout, R.id.abandon_flags_layer);

        mRecallFlags = ViewHelper.get(layout, R.id.recall_flags);
        if (mRecallFlags != null) {
            mRecallFlags.setOnClickListener(new RecallFlagsClick());
        }
        mRecallLock = ViewHelper.get(layout, R.id.recall_flags_lock);
        mRecallLayer = ViewHelper.get(layout, R.id.recall_flags_layer);

        mPostponeFlags = ViewHelper.get(layout, R.id.postpone_flags);
        if (mPostponeFlags != null) {
            mPostponeFlags.setOnClickListener(new PostponeFlagsClick());
        }
        mPostponeLock = ViewHelper.get(layout, R.id.postpone_flags_lock);
        mPostponeLayer = ViewHelper.get(layout, R.id.postpone_flags_layer);

        mCourseFlags = ViewHelper.get(layout, R.id.course_flags);
        if (mCourseFlags != null) {
            mCourseFlags.setOnClickListener(new CourseFlagsClick());
        }
        mCourseLock = ViewHelper.get(layout, R.id.course_flags_lock);

        mBlueFirstFlag = ViewHelper.get(layout, R.id.blue_first_flags);
        if (mBlueFirstFlag != null) {
            mBlueFirstFlag.setOnClickListener(new BlueFirstFlagClick());
        }
        mBlueFirstLock = ViewHelper.get(layout, R.id.blue_first_flags_lock);

        mBlueLastFlag = ViewHelper.get(layout, R.id.blue_last_flags);
        if (mBlueLastFlag != null) {
            mBlueLastFlag.setOnClickListener(new BlueLastFlagClick());
        }
        mBlueLastLock = ViewHelper.get(layout, R.id.blue_last_flags_lock);
        mBlueLastText = ViewHelper.get(layout, R.id.blue_last_flags_text);

        return layout;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        NavigationEvents.INSTANCE.subscribeFragmentAttachment(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        NavigationEvents.INSTANCE.unSubscribeFragmentAttachment(this);
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

    private void checkStatus() {
        changeVisibility(mPostponeLock, mPostponeLayer, View.GONE);
        changeVisibility(mAbandonLock, mAbandonLayer, View.GONE);
        changeVisibility(mRecallLock, null, View.GONE);
        changeVisibility(mCourseLock, null, View.GONE);
        changeVisibility(mBlueFirstLock, null, View.GONE);
        changeVisibility(mBlueLastLock, null, View.GONE);

        switch (getRace().getStatus()) {
            case UNSCHEDULED:
                changeVisibility(mPostponeFlags, null, View.VISIBLE);
                changeVisibility(mAbandonFlags, null, View.VISIBLE);
                changeVisibility(mRecallFlags, null, View.VISIBLE);
                changeVisibility(mCourseFlags, null, View.GONE);
                changeVisibility(mBlueFirstFlag, null, View.VISIBLE);
                changeVisibility(mBlueLastFlag, null, View.GONE);
                break;

            case PRESCHEDULED:
                changeVisibility(mPostponeFlags, null, View.VISIBLE);
                changeVisibility(mAbandonFlags, null, View.GONE);
                changeVisibility(mRecallFlags, null, View.GONE);
                changeVisibility(mCourseFlags, null, View.GONE);
                changeVisibility(mBlueFirstFlag, null, View.GONE);
                changeVisibility(mBlueLastFlag, null, View.GONE);
                break;

            case SCHEDULED:
            case STARTPHASE:
                changeVisibility(mPostponeFlags, null, View.VISIBLE);
                changeVisibility(mAbandonFlags, null, View.GONE);
                changeVisibility(mRecallFlags, null, View.GONE);
                changeVisibility(mCourseFlags, null, View.GONE);
                changeVisibility(mBlueFirstFlag, null, View.GONE);
                changeVisibility(mBlueLastFlag, null, View.GONE);
                break;

            case RUNNING:
                changeVisibility(mPostponeLock, mPostponeLayer, View.VISIBLE);

                changeVisibility(mPostponeFlags, null, View.VISIBLE);
                changeVisibility(mAbandonFlags, null, View.VISIBLE);
                changeVisibility(mRecallFlags, null, View.VISIBLE);
                changeVisibility(mCourseFlags, null, View.GONE);
                changeVisibility(mBlueFirstFlag, null, View.VISIBLE);
                changeVisibility(mBlueLastFlag, null, View.GONE);
                break;

            case FINISHING:
                changeVisibility(mPostponeFlags, null, View.GONE);
                changeVisibility(mAbandonFlags, null, View.VISIBLE);
                changeVisibility(mRecallFlags, null, View.GONE);
                changeVisibility(mCourseFlags, null, View.GONE);
                changeVisibility(mBlueFirstFlag, null, View.GONE);
                changeVisibility(mBlueLastFlag, null,
                        preferences.getRacingProcedureIsResultEntryEnabled(getRaceState().getRacingProcedure().getType())
                                ? View.VISIBLE
                                : View.GONE);
                break;

            case FINISHED:
                changeVisibility(mPostponeFlags, null, View.GONE);
                changeVisibility(mAbandonFlags, null, View.GONE);
                changeVisibility(mRecallFlags, null, View.GONE);
                changeVisibility(mCourseFlags, null, View.GONE);
                changeVisibility(mBlueFirstFlag, null, View.GONE);
                changeVisibility(mBlueLastFlag, null, View.GONE);
                break;

            default:
                changeVisibility(mPostponeFlags, mPostponeLayer, View.GONE);
                changeVisibility(mAbandonFlags, null, View.GONE);
                changeVisibility(mRecallFlags, null, View.GONE);
                changeVisibility(mCourseFlags, null, View.GONE);
                changeVisibility(mBlueFirstFlag, null, View.GONE);
                changeVisibility(mBlueLastFlag, null, View.GONE);
                break;
        }
    }

    private void uncheckMarker(View view) {
        if (isAdded()) {
            if (mAbandonFlags.equals(view)) {
                setMarkerLevel(mAbandonFlags, R.id.abandon_flags_marker, LEVEL_NORMAL);
            }

            if (mRecallFlags.equals(view)) {
                setMarkerLevel(mRecallFlags, R.id.recall_flags_marker, LEVEL_NORMAL);
            }

            if (mPostponeFlags.equals(view)) {
                setMarkerLevel(mPostponeFlags, R.id.postpone_flags_marker, LEVEL_NORMAL);
            }

            if (mCourseFlags.equals(view)) {
                setMarkerLevel(mCourseFlags, R.id.course_flags_marker, LEVEL_NORMAL);
            }

            if (mBlueFirstFlag.equals(view)) {
                setMarkerLevel(mBlueFirstFlag, R.id.first_blue_flags_marker, LEVEL_NORMAL);
            }

            if (mBlueLastFlag.equals(view)) {
                setMarkerLevel(mBlueLastFlag, R.id.blue_down_flags_marker, LEVEL_NORMAL);
            }
        }
    }

    @Override
    public TickListener getStartTimeTickListener() {
        return this::onStartTimeTick;
    }

    private void onStartTimeTick(TimePoint now) {
        final RaceState state = getRaceState();
        switch (state.getStatus()) {
            case RUNNING:
                TimePoint start = state.getStartTime();
                if (start != null) {
                    long diff = now.minus(start.asMillis()).asMillis();
                    if (diff >= 60000) {
                        changeVisibility(mRecallLock, mRecallLayer, View.VISIBLE);
                    } else {
                        changeVisibility(mRecallLock, mRecallLayer, View.GONE);
                    }
                } else {
                    changeVisibility(mRecallLock, mRecallLayer, View.GONE);
                }
                break;

            case FINISHING:
                mBlueLastText.setText(TimeUtils.formatTimeAgo(getActivity(),
                        now.minus(state.getFinishingTime().asMillis()).asMillis()));
                changeVisibility(mRecallLock, mRecallLayer, View.VISIBLE);
                break;

            case FINISHED:
                changeVisibility(mRecallLock, mRecallLayer, View.VISIBLE);
                break;

            default:
                // nothing
        }
    }

    @Override
    public void onFragmentAttach(Fragment fragment) {
    }

    @Override
    public void onFragmentDetach(Fragment fragment) {
        if (fragment instanceof MoreFlagsFragment.FinishTimeFragment) {
            MoreFlagsFragment.FinishTimeFragment finishTimeFragment = (MoreFlagsFragment.FinishTimeFragment) fragment;
            int mode = finishTimeFragment.getStartMode();
            if (mode == 1) {
                uncheckMarker(mBlueLastFlag);
            } else if (mode == 0) {
                uncheckMarker(mBlueFirstFlag);
            }
        } else if (fragment instanceof EmptyFragment) {
            uncheckMarker(mCourseFlags);
        } else if (fragment instanceof AbortFlagsFragment) {
            final AbortFlagsFragment abortFlagsFragment = (AbortFlagsFragment) fragment;
            if (abortFlagsFragment.getFlag() == Flags.AP) {
                uncheckMarker(mPostponeFlags);
            } else if (abortFlagsFragment.getFlag() == Flags.NOVEMBER) {
                uncheckMarker(mAbandonFlags);
            }
        } else if (fragment instanceof RecallFlagsFragment) {
            uncheckMarker(mRecallFlags);
        }
    }

    private class AbandonFlagsClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = AbandonFlagsClick.class.getName();
        private final View container = mAbandonFlags;
        private final int markerId = R.id.abandon_flags_marker;

        public void onClick(View v) {
            if (mAbandonLock != null) {
                if (mAbandonLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            final int toggle = toggleMarker(container, markerId);
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(AbortFlagsFragment.newInstance(Flags.NOVEMBER, getString(R.string.flags_abandon)));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class RecallFlagsClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = RecallFlagsClick.class.getName();
        private final View container = mRecallFlags;
        private final int markerId = R.id.recall_flags_marker;

        public void onClick(View v) {
            if (mRecallLock != null) {
                if (mRecallLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            final int toggle = toggleMarker(container, markerId);
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(RecallFlagsFragment.newInstance(getString(R.string.flags_recall)));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class PostponeFlagsClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = PostponeFlagsClick.class.getName();
        private final View container = mPostponeFlags;
        private final int markerId = R.id.postpone_flags_marker;

        public void onClick(View v) {
            if (mPostponeLock != null) {
                if (mPostponeLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            final int toggle = toggleMarker(container, markerId);
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(AbortFlagsFragment.newInstance(Flags.AP, getString(R.string.flags_postpone)));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class CourseFlagsClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = CourseFlagsClick.class.getName();
        private final View container = mCourseFlags;
        private final int markerId = R.id.course_flags_marker;

        public void onClick(View v) {
            if (mCourseLock != null) {
                if (mCourseLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            final int toggle = toggleMarker(container, markerId);
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(EmptyFragment.newInstance());
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class BlueFirstFlagClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = BlueFirstFlagClick.class.getName();
        private final View container = mBlueFirstFlag;
        private final int markerId = R.id.first_blue_flags_marker;

        @Override
        public void onClick(View v) {
            if (mBlueFirstLock != null) {
                if (mBlueFirstLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            final int toggle = toggleMarker(container, markerId);
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(MoreFlagsFragment.FinishTimeFragment.newInstance(0));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class BlueLastFlagClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = BlueLastFlagClick.class.getName();
        private final View container = mBlueLastFlag;
        private final int markerId = R.id.blue_down_flags_marker;

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mBlueLastLock != null) {
                if (mBlueLastLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
                    showChangeDialog(this);
                } else {
                    toggleFragment();
                }
            }
        }

        @Override
        public void onClick(View v) {
            toggleFragment();
        }

        private void toggleFragment() {
            final int toggle = toggleMarker(container, markerId);
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(MoreFlagsFragment.FinishTimeFragment.newInstance(1));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }
}
