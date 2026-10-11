package com.sap.sailing.racecommittee.app.ui.fragments.panels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.scoring.AdditionalScoringInformationType;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ess.ESSRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.gate.GateStartRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.BaseRacingProcedureChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.ConfigurableStartModeFlagRacingProcedure;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.NavigationEvents;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.BaseFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.CourseFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.GateStartPathFinderFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.GateStartTimingFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.RaceFactorFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.StartModeFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.StartProcedureFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.WindFragment;
import com.sap.sailing.racecommittee.app.ui.utils.FlagsResources;
import com.sap.sailing.racecommittee.app.ui.views.PanelButton;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.text.DecimalFormat;

public class SetupPanelFragment extends BasePanelFragment implements NavigationEvents.NavigationListener {

    private final static String ARGS_PAGE = "page";

    private RaceStateChangedListener mStateListener;
    private RaceProcedureChangedListener mProcedureListener;
    private final IntentReceiver mReceiver = new IntentReceiver();

    private PanelButton mButtonProcedure;
    private PanelButton mButtonMode;
    private PanelButton mButtonPathfinder;
    private PanelButton mButtonTiming;
    private PanelButton mButtonRaceGroup;
    private PanelButton mButtonFactor;
    private PanelButton mButtonCourse;
    private PanelButton mButtonWind;

    private DecimalFormat mFactorFormat;

    public static SetupPanelFragment newInstance(Bundle args, int page) {
        SetupPanelFragment fragment = new SetupPanelFragment();
        args.putInt(ARGS_PAGE, page);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final int page = getArguments() != null ? getArguments().getInt(ARGS_PAGE) : 0;
        View layout;
        if (page == 1) {
            layout = inflater.inflate(R.layout.race_panel_setup_hor_2, container, false);
        } else {
            layout = inflater.inflate(R.layout.race_panel_setup, container, false);
        }

        mStateListener = new RaceStateChangedListener();
        mProcedureListener = new RaceProcedureChangedListener();

        mButtonProcedure = ViewHelper.get(layout, R.id.button_procedure);
        if (mButtonProcedure != null) {
            mButtonProcedure.setListener(new StartProcedureListener());
        }

        mButtonMode = ViewHelper.get(layout, R.id.button_mode);
        if (mButtonMode != null) {
            mButtonMode.setListener(new ButtonModeListener());
        }

        mButtonPathfinder = ViewHelper.get(layout, R.id.button_pathfinder);
        if (mButtonPathfinder != null) {
            mButtonPathfinder.setListener(new ButtonPathfinderListener());
        }

        mButtonTiming = ViewHelper.get(layout, R.id.button_timing);
        if (mButtonTiming != null) {
            mButtonTiming.setListener(new ButtonTimingListener());
        }

        mButtonRaceGroup = ViewHelper.get(layout, R.id.button_race_group);
        if (mButtonRaceGroup != null) {
            mButtonRaceGroup.setListener(new ButtonRaceGroupListener());
        }

        mButtonFactor = ViewHelper.get(layout, R.id.button_factor);
        if (mButtonFactor != null) {
            mButtonFactor.setListener(new ButtonFactorListener());
        }

        mButtonCourse = ViewHelper.get(layout, R.id.button_course);
        if (mButtonCourse != null) {
            mButtonCourse.setListener(new ButtonCourseListener());
        }

        mButtonWind = ViewHelper.get(layout, R.id.button_wind);
        if (mButtonWind != null) {
            mButtonWind.setListener(new ButtonWindListener());
        }

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();

        refreshPanel();
        checkStatus();

        getRaceState().addChangedListener(mStateListener);
        getRaceState().getRacingProcedure().addChangedListener(mProcedureListener);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.ACTION_UPDATE_SCREEN);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filter);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFactorFormat = new DecimalFormat(getString(R.string.race_factor_format));
    }

    @Override
    public void onStop() {
        super.onStop();

        getRaceState().removeChangedListener(mStateListener);
        getRaceState().getRacingProcedure().removeChangedListener(mProcedureListener);

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
    }

    private void refreshPanel() {
        if (getRaceState().getTypedRacingProcedure() != null) {
            if (mButtonProcedure != null) {
                mButtonProcedure.setPanelText(getRaceState().getTypedRacingProcedure().getType().toString());
            }

            if (mButtonMode != null) {
                mButtonMode.setVisibility(View.GONE);
            }
            if (mButtonPathfinder != null) {
                mButtonPathfinder.setVisibility(View.GONE);
            }
            if (mButtonTiming != null) {
                mButtonTiming.setVisibility(View.GONE);
            }
            if (mButtonRaceGroup != null) {
                mButtonRaceGroup.setVisibility(View.GONE);
            }

            if (getRaceState().getRacingProcedure() instanceof ConfigurableStartModeFlagRacingProcedure) {
                if (mButtonMode != null) {
                    ConfigurableStartModeFlagRacingProcedure typedProcedure = getRaceState().getTypedRacingProcedure();
                    mButtonMode.setPanelImage(FlagsResources.getFlagDrawable(getActivity(),
                            typedProcedure.getStartModeFlag().name(), getResources().getInteger(R.integer.flag_size)));
                    mButtonMode.setVisibility(View.VISIBLE);
                }
            }
            if (getRaceState().getRacingProcedure() instanceof GateStartRacingProcedure) {
                GateStartRacingProcedure typedProcedure = getRaceState().getTypedRacingProcedure();
                if (mButtonPathfinder != null) {
                    mButtonPathfinder.setPanelText(typedProcedure.getPathfinder());
                    mButtonPathfinder.setVisibility(View.VISIBLE);
                }
                if (mButtonTiming != null) {
                    mButtonTiming.setPanelText(
                            RaceHelper.getGateTiming(getActivity(), typedProcedure, getRace().getRaceGroup()));
                    mButtonTiming.setVisibility(View.VISIBLE);
                }
            }
            if (getRaceState().getRacingProcedure() instanceof ESSRacingProcedure) {
                if (mButtonRaceGroup != null) {
                    mButtonRaceGroup.setVisibility(View.VISIBLE);
                    mButtonRaceGroup.setPanelSwitch(getRaceState().isAdditionalScoringInformationEnabled(
                            AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE));
                }
            }
        }

        if (mButtonFactor != null) {
            mButtonFactor.setVisibility(preferences.isRaceFactorChangeAllow() ? View.VISIBLE : View.GONE);
            if (getRace().getExplicitFactor() != null) {
                mButtonFactor.setPanelText(mFactorFormat.format(getRace().getExplicitFactor()));
            } else {
                mButtonFactor.setPanelText(null);
            }
        }

        if (mButtonCourse != null) {
            mButtonCourse.setPanelText(getCourseName());
        }

        Wind wind = getRaceState().getWindFix();
        if (mButtonWind != null && wind != null) {
            String sensorData = getString(R.string.wind_panel, wind.getFrom().getDegrees(), wind.getKnots());
            mButtonWind.setPanelText(sensorData);
        }
    }

    private void checkStatus() {
        switch (getRace().getStatus()) {
            case UNSCHEDULED:
            case PRESCHEDULED:
            case SCHEDULED:
                changeVisibility(mButtonProcedure, false);
                changeVisibility(mButtonMode, false);
                changeVisibility(mButtonPathfinder, false);
                changeVisibility(mButtonTiming, false);
                changeVisibility(mButtonRaceGroup, false);
                changeVisibility(mButtonCourse, false);
                changeVisibility(mButtonWind, false);
                break;

            case STARTPHASE:
                changeVisibility(mButtonProcedure, true);
                changeVisibility(mButtonMode, true);
                changeVisibility(mButtonPathfinder, true);
                changeVisibility(mButtonTiming, true);
                changeVisibility(mButtonRaceGroup, true);
                changeVisibility(mButtonCourse, false);
                changeVisibility(mButtonWind, false);
                break;

            case RUNNING:
                changeVisibility(mButtonProcedure, true);
                changeVisibility(mButtonMode, true);
                changeVisibility(mButtonPathfinder, true);
                changeVisibility(mButtonTiming, true);
                changeVisibility(mButtonRaceGroup, true);
                changeVisibility(mButtonCourse, false);
                changeVisibility(mButtonWind, false);
                break;

            case FINISHING:
                changeVisibility(mButtonProcedure, true);
                changeVisibility(mButtonMode, true);
                changeVisibility(mButtonPathfinder, true);
                changeVisibility(mButtonTiming, true);
                changeVisibility(mButtonRaceGroup, true);
                changeVisibility(mButtonCourse, true);
                changeVisibility(mButtonWind, false);
                break;

            case FINISHED:
                changeVisibility(mButtonProcedure, false);
                changeVisibility(mButtonMode, false);
                changeVisibility(mButtonPathfinder, false);
                changeVisibility(mButtonTiming, false);
                changeVisibility(mButtonRaceGroup, false);
                changeVisibility(mButtonCourse, false);
                changeVisibility(mButtonWind, false);
                break;

            default:
                changeVisibility(mButtonProcedure, true);
                changeVisibility(mButtonMode, true);
                changeVisibility(mButtonPathfinder, true);
                changeVisibility(mButtonTiming, true);
                changeVisibility(mButtonRaceGroup, true);
                changeVisibility(mButtonCourse, true);
                changeVisibility(mButtonWind, true);
                break;
        }
    }

    private void changeVisibility(PanelButton view, boolean showLock) {
        if (view != null) {
            view.setLock(showLock);
        }
    }

    private void uncheckMarker(PanelButton view) {
        if (isAdded()) {
            if (mButtonProcedure != null && mButtonProcedure.equals(view)) {
                mButtonProcedure.setMarkerLevel(PanelButton.LEVEL_NORMAL);
            }
            if (mButtonMode != null && mButtonMode.equals(view)) {
                mButtonMode.setMarkerLevel(PanelButton.LEVEL_NORMAL);
            }
            if (mButtonPathfinder != null && mButtonPathfinder.equals(view)) {
                mButtonPathfinder.setMarkerLevel(PanelButton.LEVEL_NORMAL);
            }
            if (mButtonTiming != null && mButtonTiming.equals(view)) {
                mButtonTiming.setMarkerLevel(PanelButton.LEVEL_NORMAL);
            }
            if (mButtonFactor != null && mButtonFactor.equals(view)) {
                mButtonFactor.setMarkerLevel(PanelButton.LEVEL_NORMAL);
            }
            if (mButtonCourse != null && mButtonCourse.equals(view)) {
                mButtonCourse.setMarkerLevel(PanelButton.LEVEL_NORMAL);
            }
            if (mButtonWind != null && mButtonWind.equals(view)) {
                mButtonWind.setMarkerLevel(PanelButton.LEVEL_NORMAL);
            }
        }
    }

    @Override
    public void onFragmentAttach(Fragment fragment) {
    }

    @Override
    public void onFragmentDetach(Fragment fragment) {
        if (fragment instanceof StartProcedureFragment) {
            uncheckMarker(mButtonProcedure);
        } else if (fragment instanceof StartModeFragment) {
            uncheckMarker(mButtonMode);
        } else if (fragment instanceof GateStartPathFinderFragment) {
            uncheckMarker(mButtonPathfinder);
        } else if (fragment instanceof GateStartTimingFragment) {
            uncheckMarker(mButtonTiming);
        } else if (fragment instanceof RaceFactorFragment) {
            uncheckMarker(mButtonFactor);
        } else if (fragment instanceof CourseFragment) {
            uncheckMarker(mButtonCourse);
        } else if (fragment instanceof WindFragment) {
            uncheckMarker(mButtonWind);
        }
    }

    private class RaceStateChangedListener extends BaseRaceStateChangedListener {

        @Override
        public void onRacingProcedureChanged(ReadonlyRaceState state) {
            super.onRacingProcedureChanged(state);

            state.getRacingProcedure().addChangedListener(mProcedureListener);

            refreshPanel();
        }

        @Override
        public void onCourseDesignChanged(ReadonlyRaceState state) {
            super.onCourseDesignChanged(state);

            refreshPanel();
        }

        @Override
        public void onWindFixChanged(ReadonlyRaceState state) {
            super.onWindFixChanged(state);

            refreshPanel();
        }

        @Override
        public void onStatusChanged(ReadonlyRaceState state) {
            super.onStatusChanged(state);

            checkStatus();
        }
    }

    private class RaceProcedureChangedListener extends BaseRacingProcedureChangedListener {

        @Override
        public void onActiveFlagsChanged(ReadonlyRacingProcedure racingProcedure) {
            super.onActiveFlagsChanged(racingProcedure);

            refreshPanel();
        }
    }

    private class StartProcedureListener implements PanelButton.PanelButtonClick {

        private final String TAG = StartProcedureListener.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case PanelButton.LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case PanelButton.LEVEL_TOGGLED:
                    replaceFragment(StartProcedureFragment.newInstance(BaseFragment.START_MODE_PLANNED));
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

    private class ButtonModeListener implements PanelButton.PanelButtonClick {

        private final String TAG = ButtonModeListener.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case PanelButton.LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case PanelButton.LEVEL_TOGGLED:
                    replaceFragment(StartModeFragment.newInstance(StartModeFragment.START_MODE_PLANNED));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            view.disableToggle();
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            // no-op
        }
    }

    private class ButtonPathfinderListener implements PanelButton.PanelButtonClick {

        private final String TAG = ButtonPathfinderListener.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case PanelButton.LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case PanelButton.LEVEL_TOGGLED:
                    replaceFragment(
                            GateStartPathFinderFragment.newInstance(GateStartPathFinderFragment.START_MODE_PLANNED));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            view.disableToggle();
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            // no-op
        }
    }

    private class ButtonTimingListener implements PanelButton.PanelButtonClick {

        private final String TAG = ButtonTimingListener.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case PanelButton.LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case PanelButton.LEVEL_TOGGLED:
                    replaceFragment(GateStartTimingFragment.newInstance(GateStartTimingFragment.START_MODE_PLANNED));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            view.disableToggle();
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            // no-op
        }
    }

    private class ButtonRaceGroupListener implements PanelButton.PanelButtonClick {

        @Override
        public void onClick(PanelButton view) {
            // no-op
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            getRaceState().setAdditionalScoringInformationEnabled(MillisecondsTimePoint.now(), /* enable */isChecked,
                    AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE);
        }
    }

    private class ButtonFactorListener implements PanelButton.PanelButtonClick {
        private final String TAG = ButtonFactorListener.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(RaceFactorFragment.newInstance(BaseFragment.START_MODE_PLANNED));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            view.disableToggle();
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            // no-op
        }
    }

    private class ButtonCourseListener implements PanelButton.PanelButtonClick {
        private final String TAG = ButtonCourseListener.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(CourseFragment.newInstance(BaseFragment.START_MODE_PLANNED, preferences));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            view.disableToggle();
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            // no-op
        }
    }

    private class ButtonWindListener implements PanelButton.PanelButtonClick {
        private final String TAG = ButtonWindListener.class.getName();

        @Override
        public void onClick(PanelButton view) {
            final int toggle = view.toggleMarker();
            switch (toggle) {
                case LEVEL_NORMAL:
                    sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                    break;

                case LEVEL_TOGGLED:
                    replaceFragment(WindFragment.newInstance(BaseFragment.START_MODE_PLANNED));
                    break;

                default:
                    ExLog.i(getActivity(), TAG, "Unknown return value");
            }
            view.disableToggle();
        }

        @Override
        public void onChangedSwitch(PanelButton view, boolean isChecked) {
            // no-op
        }
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AppConstants.ACTION_UPDATE_SCREEN.equals(intent.getAction())) {
                refreshPanel();
            }
        }
    }
}
