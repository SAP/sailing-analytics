package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;

import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.BaseRacingProcedureChangedListener;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.activities.RacingActivity;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.panels.FlagPanelFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.panels.SetupPanelFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.panels.TimePanelFragment;

public abstract class BaseRaceInfoRaceFragment<ProcedureType extends RacingProcedure> extends RaceFragment {

    private final RaceStateChangedListener mRaceStateChangedListener;
    private final ProcedureChangedListener mProcedureListener;

    public BaseRaceInfoRaceFragment() {
        mRaceStateChangedListener = new RaceStateChangedListener();
        mProcedureListener = new ProcedureChangedListener();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        replaceFragment(SetupPanelFragment.newInstance(getArguments()), R.id.race_panel_left);
        replaceFragment(FlagPanelFragment.newInstance(getArguments()), R.id.race_panel_right);
        replaceFragment(TimePanelFragment.newInstance(getArguments()), R.id.race_panel_top);
    }

    @Override
    public void onStart() {
        super.onStart();

        setupUi();

        getRaceState().addChangedListener(mRaceStateChangedListener);
        getRacingProcedure().addChangedListener(mProcedureListener);
    }

    @Override
    public void onStop() {
        super.onStop();

        getRaceState().removeChangedListener(mRaceStateChangedListener);
        getRacingProcedure().removeChangedListener(mProcedureListener);
    }

    protected ProcedureType getRacingProcedure() {
        return getRaceState().getTypedRacingProcedure();
    }

    protected abstract void setupUi();

    protected void onIndividualRecallChanged(boolean displayed) {
        // overwrite in derived fragments
    }

    protected void replaceFragment(RaceFragment fragment) {
        replaceFragment(fragment, R.id.race_frame);
    }

    protected void replaceFragment(RaceFragment fragment, @IdRes int id) {
        if (getView() != null && getView().findViewById(id) != null) {
            Bundle args = getRecentArguments();
            if (fragment.getArguments() != null) {
                args.putAll(fragment.getArguments());
            }
            fragment.setArguments(args);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(id, fragment);
            transaction.commit();
        }
    }

    private void showMainContent() {
        Intent intent = new Intent(AppConstants.INTENT_ACTION_SHOW_MAIN_CONTENT);
        BroadcastManager.getInstance(getActivity()).addIntent(intent);
    }

    private class RaceStateChangedListener extends BaseRaceStateChangedListener {

        @Override
        public void onStatusChanged(ReadonlyRaceState state) {
            super.onStatusChanged(state);

            switch (state.getStatus()) {
                case UNSCHEDULED:
                case FINISHED:
                    RacingActivity activity = (RacingActivity) getActivity();
                    if (activity != null) {
                        activity.onRaceItemClicked(getRace(), true);
                    }
                    break;

                default:
                    showMainContent();
                    break;
            }
        }

        @Override
        public void onCourseDesignChanged(ReadonlyRaceState state) {
            super.onCourseDesignChanged(state);

            showMainContent();
        }

        @Override
        public void onWindFixChanged(ReadonlyRaceState state) {
            super.onWindFixChanged(state);

            showMainContent();
        }

        @Override
        public void onRacingProcedureChanged(ReadonlyRaceState state) {
            super.onRacingProcedureChanged(state);

            showMainContent();
        }
    }

    private class ProcedureChangedListener extends BaseRacingProcedureChangedListener {

        @Override
        public void onActiveFlagsChanged(ReadonlyRacingProcedure racingProcedure) {
            super.onActiveFlagsChanged(racingProcedure);

            setupUi();

            if (getRaceState().getStatus() == RaceLogRaceStatus.SCHEDULED) {
                showMainContent();
            }
        }

        @Override
        public void onIndividualRecallDisplayed(ReadonlyRacingProcedure racingProcedure) {
            super.onIndividualRecallDisplayed(racingProcedure);

            onIndividualRecallChanged(true);

            showMainContent();
            sendIntent(AppConstants.INTENT_ACTION_CLEAR_TOGGLE);
        }

        @Override
        public void onIndividualRecallRemoved(ReadonlyRacingProcedure racingProcedure) {
            super.onIndividualRecallRemoved(racingProcedure);

            onIndividualRecallChanged(false);

            showMainContent();
            sendIntent(AppConstants.INTENT_ACTION_CLEAR_TOGGLE);
        }
    }
}
