package com.sap.sailing.racecommittee.app.ui.fragments.panels;

import android.content.*;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHolder;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.BaseRacingProcedureChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.rrs26.RRS26RacingProcedure;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.CourseFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.StartModeFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.StartProcedureFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.WindFragment;
import com.sap.sailing.racecommittee.app.ui.utils.FlagsResources;

public class SetupPanelFragment extends BasePanelFragment {

    private RaceStateChangedListener mStateListener;
    private RaceProcedureChangedListener mProcedureListener;
    private IntentReceiver mReceiver;

    // Start Procedure Toggle
    private View mStartProcedure;
    private View mStartProcedureLock;
    private TextView mStartProcedureValue;

    // Start Mode Toggle
    private View mStartMode;
    private View mStartModeLock;
    private ImageView mStartModeFlag;

    // Course Toggle
    private View mCourse;
    private View mCourseLock;
    private TextView mCourseValue;

    // Wind Toggle
    private View mWind;
    private View mWindLock;
    private TextView mWindValue;

    public SetupPanelFragment() {
        mReceiver = new IntentReceiver();
    }

    public static SetupPanelFragment newInstance(Bundle args) {
        SetupPanelFragment fragment = new SetupPanelFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_panel_setup, container, false);

        mStateListener = new RaceStateChangedListener();
        mProcedureListener = new RaceProcedureChangedListener();

        mStartProcedure = ViewHolder.get(layout, R.id.start_procedure);
        if (mStartProcedure != null) {
            mStartProcedure.setOnClickListener(new StartProcedureClick());
        }
        mStartProcedureLock = ViewHolder.get(layout, R.id.start_procedure_lock);
        mStartProcedureValue = ViewHolder.get(layout, R.id.start_procedure_value);

        mStartMode = ViewHolder.get(layout, R.id.start_mode);
        if (mStartMode != null) {
            mStartMode.setOnClickListener(new StartModeClick());
        }
        mStartModeLock = ViewHolder.get(layout, R.id.start_mode_lock);
        mStartModeFlag = ViewHolder.get(layout, R.id.start_mode_flag);

        mCourse = ViewHolder.get(layout, R.id.course);
        if (mCourse != null) {
            mCourse.setOnClickListener(new CourseClick());
        }
        mCourseLock = ViewHolder.get(layout, R.id.course_lock);
        mCourseValue = ViewHolder.get(layout, R.id.course_value);

        mWind = ViewHolder.get(layout, R.id.wind);
        if (mWind != null) {
            mWind.setOnClickListener(new WindClick());
        }
        mWindLock = ViewHolder.get(layout, R.id.wind_lock);
        mWindValue = ViewHolder.get(layout, R.id.wind_value);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshPanel();
        checkStatus();

        getRaceState().addChangedListener(mStateListener);
        getRaceState().getRacingProcedure().addChangedListener(mProcedureListener);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.INTENT_ACTION_TOGGLE);
        filter.addAction(AppConstants.INTENT_ACTION_CLEAR_TOGGLE);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);

        sendIntent(AppConstants.INTENT_ACTION_CLEAR_TOGGLE);
    }

    @Override
    public void onPause() {
        super.onPause();

        getRaceState().removeChangedListener(mStateListener);
        getRaceState().getRacingProcedure().removeChangedListener(mProcedureListener);

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }

    private void refreshPanel() {
        if (mCourseValue != null) {
            mCourseValue.setText(null);
            mCourseValue.setCompoundDrawables(null, null, null, null);
            String courseName = getCourseName();
            mCourseValue.setText(courseName);
        }

        if (mStartProcedureValue != null && getRaceState().getTypedRacingProcedure() != null) {
            mStartProcedureValue.setText(getRaceState().getTypedRacingProcedure().getType().toString());
        }

        if (mStartModeFlag != null) {
            try {
                if (mStartMode != null) {
                    mStartMode.setVisibility(View.VISIBLE);
                }
                RRS26RacingProcedure procedure = getRaceState().getTypedRacingProcedure();
                mStartModeFlag.setImageDrawable(FlagsResources.getFlagDrawable(getActivity(), procedure.getStartModeFlag().name(), 48));
            } catch (Exception ex) {
                if (mStartMode != null) {
                    mStartMode.setVisibility(View.GONE);
                }
            }
        }

        Wind wind = getRaceState().getWindFix();
        if (mWindValue != null && wind != null) {
            String sensorData = getString(R.string.wind_panel);
            sensorData = sensorData.replace("#FROM#", String.format("%.0f", wind.getFrom().getDegrees()));
            sensorData = sensorData.replace("#SPEED#", String.format("%.1f", wind.getKnots()));
            mWindValue.setText(sensorData);
        }
    }

    private void checkStatus() {
        switch (getRace().getStatus()) {
        case UNSCHEDULED:
            changeVisibility(mStartProcedureLock, View.GONE);
            changeVisibility(mStartModeLock, View.GONE);
            changeVisibility(mCourseLock, View.GONE);
            changeVisibility(mWindLock, View.GONE);
            break;

        case SCHEDULED:
            changeVisibility(mStartProcedureLock, View.GONE);
            changeVisibility(mStartModeLock, View.GONE);
            changeVisibility(mCourseLock, View.GONE);
            changeVisibility(mWindLock, View.GONE);
            break;

        case STARTPHASE:
            changeVisibility(mStartProcedureLock, View.VISIBLE);
            changeVisibility(mStartModeLock, View.VISIBLE);
            changeVisibility(mCourseLock, View.GONE);
            changeVisibility(mWindLock, View.GONE);
            break;

        case RUNNING:
            changeVisibility(mStartProcedureLock, View.VISIBLE);
            changeVisibility(mStartModeLock, View.VISIBLE);
            changeVisibility(mCourseLock, View.GONE);
            changeVisibility(mWindLock, View.GONE);
            uncheckMarker(mStartProcedure);
            uncheckMarker(mStartMode);
            break;

        case FINISHING:
            changeVisibility(mStartProcedureLock, View.VISIBLE);
            changeVisibility(mStartModeLock, View.VISIBLE);
            changeVisibility(mCourseLock, View.VISIBLE);
            changeVisibility(mWindLock, View.GONE);
            uncheckMarker(mStartProcedure);
            uncheckMarker(mStartMode);
            uncheckMarker(mCourseLock);
            break;

        case FINISHED:
            changeVisibility(mStartProcedureLock, View.GONE);
            changeVisibility(mStartModeLock, View.GONE);
            changeVisibility(mCourseLock, View.GONE);
            changeVisibility(mWindLock, View.GONE);
            uncheckMarker(mStartProcedure);
            uncheckMarker(mStartMode);
            uncheckMarker(mCourse);
            uncheckMarker(mWind);
            break;

        default:
            changeVisibility(mStartProcedureLock, View.VISIBLE);
            changeVisibility(mStartModeLock, View.VISIBLE);
            changeVisibility(mCourseLock, View.VISIBLE);
            changeVisibility(mWindLock, View.VISIBLE);
            uncheckMarker(mStartProcedure);
            uncheckMarker(mStartMode);
            uncheckMarker(mCourse);
            uncheckMarker(mWind);
            break;
        }
    }

    private void uncheckMarker(View view) {
        if (view != null) {
            if (!view.equals(mStartProcedure)) {
                resetFragment(mStartProcedureLock, R.id.race_frame, StartProcedureFragment.class);
                setMarkerLevel(mStartProcedure, R.id.start_procedure_marker, 0);
            }

            if (!view.equals(mStartMode)) {
                resetFragment(mStartModeLock, R.id.race_frame, StartModeFragment.class);
                setMarkerLevel(mStartMode, R.id.start_mode_marker, 0);
            }

            if (!view.equals(mCourse)) {
                resetFragment(mCourseLock, R.id.race_frame, CourseFragment.class);
                setMarkerLevel(mCourse, R.id.course_marker, 0);
            }

            if (!view.equals(mWind)) {
                resetFragment(mWindLock, R.id.race_frame, WindFragment.class);
                setMarkerLevel(mWind, R.id.wind_marker, 0);
            }
        }
    }

    private class RaceStateChangedListener extends BaseRaceStateChangedListener {

        private View mView;

        public RaceStateChangedListener() {
            mView = new View(getActivity());
        }

        @Override
        public void onRacingProcedureChanged(ReadonlyRaceState state) {
            super.onRacingProcedureChanged(state);

            state.getRacingProcedure().addChangedListener(mProcedureListener);

            refreshPanel();
            uncheckMarker(mView);
        }

        @Override
        public void onCourseDesignChanged(ReadonlyRaceState state) {
            super.onCourseDesignChanged(state);

            refreshPanel();
            uncheckMarker(mView);
        }

        @Override
        public void onWindFixChanged(ReadonlyRaceState state) {
            super.onWindFixChanged(state);

            refreshPanel();
            uncheckMarker(mView);
        }

        @Override
        public void onStatusChanged(ReadonlyRaceState state) {
            super.onStatusChanged(state);

            checkStatus();
            uncheckMarker(mView);
        }
    }

    private class RaceProcedureChangedListener extends BaseRacingProcedureChangedListener {

        private View mView;

        public RaceProcedureChangedListener() {
            mView = new View(getActivity());
        }

        @Override
        public void onActiveFlagsChanged(ReadonlyRacingProcedure racingProcedure) {
            super.onActiveFlagsChanged(racingProcedure);

            refreshPanel();
            uncheckMarker(mView);
        }
    }

    private class StartProcedureClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = StartProcedureClick.class.getName();
        private final View container = mStartProcedure;
        private final int markerId = R.id.start_procedure_marker;

        public void onClick(View v) {
            if (mStartProcedureLock != null) {
                if (mStartProcedureLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            sendIntent(AppConstants.INTENT_ACTION_TOGGLE, AppConstants.INTENT_ACTION_EXTRA, AppConstants.INTENT_ACTION_TOGGLE_PROCEDURE);
            switch (toggleMarker(container, markerId)) {
            case 0:
                sendIntent(AppConstants.INTENT_ACTION_SHOW_MAIN_CONTENT);
                break;

            case 1:
                replaceFragment(StartProcedureFragment.newInstance(1));
                break;

            default:
                ExLog.i(getActivity(), TAG, "Unknown return value");
                break;
            }
            disableToggle(container, markerId);
        }
    }

    private class StartModeClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = StartModeClick.class.getName();
        private final View container = mStartMode;
        private final int markerId = R.id.start_mode_marker;

        public void onClick(View v) {
            if (mStartModeLock != null) {
                if (mStartModeLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            sendIntent(AppConstants.INTENT_ACTION_TOGGLE, AppConstants.INTENT_ACTION_EXTRA, AppConstants.INTENT_ACTION_TOGGLE_MODE);
            switch (toggleMarker(container, markerId)) {
            case 0:
                sendIntent(AppConstants.INTENT_ACTION_SHOW_MAIN_CONTENT);
                break;

            case 1:
                replaceFragment(StartModeFragment.newInstance(1));
                break;

            default:
                ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class CourseClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = CourseClick.class.getName();
        private final View container = mCourse;
        private final int markerId = R.id.course_marker;

        @Override
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
            sendIntent(AppConstants.INTENT_ACTION_TOGGLE, AppConstants.INTENT_ACTION_EXTRA, AppConstants.INTENT_ACTION_TOGGLE_COURSE);
            switch (toggleMarker(container, markerId)) {
            case 0:
                sendIntent(AppConstants.INTENT_ACTION_SHOW_MAIN_CONTENT);
                break;

            case 1:
                replaceFragment(CourseFragment.newInstance(1, getRace()));
                break;

            default:
                ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class WindClick implements View.OnClickListener, DialogInterface.OnClickListener {

        private final String TAG = WindClick.class.getName();
        private final View container = mWind;
        private final int markerId = R.id.wind_marker;

        @Override
        public void onClick(View v) {
            if (mWindLock != null) {
                if (mWindLock.getVisibility() == View.VISIBLE && isNormal(container, markerId)) {
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
            sendIntent(AppConstants.INTENT_ACTION_TOGGLE, AppConstants.INTENT_ACTION_EXTRA, AppConstants.INTENT_ACTION_TOGGLE_WIND);
            switch (toggleMarker(container, markerId)) {
            case 0:
                sendIntent(AppConstants.INTENT_ACTION_SHOW_MAIN_CONTENT);
                break;

            case 1:
                replaceFragment(WindFragment.newInstance(1));
                break;

            default:
                ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            disableToggle(container, markerId);
        }
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AppConstants.INTENT_ACTION_CLEAR_TOGGLE.equals(action)) {
                uncheckMarker(new View(context));
            }

            if (AppConstants.INTENT_ACTION_TOGGLE.equals(action)) {
                if (intent.getExtras() != null) {
                    String data = intent.getExtras().getString(AppConstants.INTENT_ACTION_EXTRA);
                    if (AppConstants.INTENT_ACTION_TOGGLE_PROCEDURE.equals(data)) {
                        uncheckMarker(mStartProcedure);
                    } else if (AppConstants.INTENT_ACTION_TOGGLE_MODE.equals(data)) {
                        uncheckMarker(mStartMode);
                    } else if (AppConstants.INTENT_ACTION_TOGGLE_COURSE.equals(data)) {
                        uncheckMarker(mCourse);
                    } else if (AppConstants.INTENT_ACTION_TOGGLE_WIND.equals(data)) {
                        uncheckMarker(mWind);
                    } else {
                        uncheckMarker(new View(context));
                    }
                }
            }
        }
    }
}
