package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.logging.LogEvent;
import com.sap.sailing.racecommittee.app.ui.activities.RacingActivity;
import com.sap.sailing.racecommittee.app.ui.adapters.AbortFlagsAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.AbortFlagsAdapter.AbortFlagItemClick;
import com.sap.sailing.racecommittee.app.ui.adapters.AbortNovemberAdapter;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceListFragment;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class AbortFlagsFragment extends RaceFragment implements AbortFlagItemClick {

    private final static String HEADER_TEXT = "headerText";

    public static AbortFlagsFragment newInstance(Flags flag, String headerText) {
        if (flag != Flags.AP && flag != Flags.NOVEMBER) {
            throw new IllegalArgumentException(
                    "The abort fragment can only be instantiated with AP or NOVEMBER, but was " + flag.name());
        }

        AbortFlagsFragment fragment = new AbortFlagsFragment();
        Bundle args = new Bundle();
        args.putString(AppConstants.KEY_RACE_FLAG, flag.name());
        args.putString(HEADER_TEXT, headerText);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.flag_list, container, false);

        ListView listView = ViewHelper.get(layout, R.id.listView);
        if (listView != null) {
            Flags flag = getFlag();
            listView.setAdapter(new AbortFlagsAdapter(requireContext(), this, flag));
        }

        HeaderLayout header = ViewHelper.get(layout, R.id.header);
        if (header != null) {
            if (getArguments() != null) {
                header.setHeaderText(getArguments().getString(HEADER_TEXT, getString(R.string.not_available)));
            }
            header.setHeaderOnClickListener(v -> sendIntent(AppConstants.ACTION_SHOW_MAIN_CONTENT));
        }

        return layout;
    }

    public Flags getFlag() {
        if (getArguments() != null) {
            return Flags.valueOf(getArguments().getString(AppConstants.KEY_RACE_FLAG));
        }
        return Flags.NONE;
    }

    @Override
    public void onClick(Flags flag) {
        TimePoint now = MillisecondsTimePoint.now();
        RaceState state = getRaceState();
        Flags mainFlag = getFlag();
        switch (mainFlag) {
            case AP:
                logFlag(flag);
                state.setAborted(now, /* postponed */ true, flag);
                if (flag == Flags.ALPHA) {
                    setProtestTime();
                }
                state.setAdvancePass(now);
                break;

            case NOVEMBER:
                logFlag(flag);
                abortRunningRaces(now, flag);
                break;

            default:
                logFlag(flag);
                state.setAdvancePass(now);
                break;
        }
    }

    private void logFlag(Flags flag) {
        switch (flag) {
            case ALPHA:
                ExLog.i(getActivity(), LogEvent.RACE_CHOOSE_ABORT_ALPHA, getRace().getId());
                break;

            case HOTEL:
                ExLog.i(getActivity(), LogEvent.RACE_CHOOSE_ABORT_HOTEL, getRace().getId());
                break;

            default:
                ExLog.i(getActivity(), LogEvent.RACE_CHOOSE_ABORT_NONE, getRace().getId());
                break;
        }
    }

    private void setProtestTime() {
        RaceListFragment.showProtest(getActivity(), getRace());
    }

    private static abstract class ActionWithNowTimePoint implements Runnable {
        private final TimePoint now;

        public ActionWithNowTimePoint(TimePoint now) {
            super();
            this.now = now;
        }

        protected TimePoint getNow() {
            return now;
        }
    }

    private class AbortAction extends ActionWithNowTimePoint {
        private final ManagedRace raceToAbort;
        private final Flags flag;

        public AbortAction(ManagedRace raceToAbort, TimePoint now, Flags flag) {
            super(now);
            this.raceToAbort = raceToAbort;
            this.flag = flag;
        }

        protected ManagedRace getRaceToAbort() {
            return raceToAbort;
        }

        @Override
        public void run() {
            abortRace(getNow(), raceToAbort.getState(), flag);
        }
    }

    private class AbortAndRenewRelativeStartTime extends AbortAction {
        private final Duration startTimeDiff;
        private final SimpleRaceLogIdentifier dependentOn;

        public AbortAndRenewRelativeStartTime(ManagedRace raceToAbort, TimePoint now, Flags flag,
                                              Duration startTimeDiff, SimpleRaceLogIdentifier dependentOn) {
            super(raceToAbort, now, flag);
            this.startTimeDiff = startTimeDiff;
            this.dependentOn = dependentOn;
        }

        @Override
        public void run() {
            super.run();
            final ReadonlyDataManager dataManager = OnlineDataManager.create(getActivity());
            getRaceToAbort().getState().forceNewDependentStartTime(getNow(), startTimeDiff, dependentOn,
                    dataManager.getDataStore().getCourseAreaId());
        }
    }

    private class MaterializeAbsoluteStartTime extends ActionWithNowTimePoint {
        private final ManagedRace onRace;
        private final TimePoint absoluteStartTime;

        public MaterializeAbsoluteStartTime(TimePoint now, ManagedRace onRace, TimePoint absoluteStartTime) {
            super(now);
            this.onRace = onRace;
            this.absoluteStartTime = absoluteStartTime;
        }

        @Override
        public void run() {
            final ReadonlyDataManager dataManager = OnlineDataManager.create(getActivity());
            onRace.getState().forceNewStartTime(getNow(), absoluteStartTime, dataManager.getDataStore().getCourseAreaId());
        }
    }

    private void abortRunningRaces(final TimePoint now, final Flags flag) {
        final RacingActivity activity = (RacingActivity) requireActivity();
        final LinkedHashMap<String, ManagedRace> races = activity.getRunningRaces();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View layout = LayoutInflater.from(builder.getContext()).inflate(R.layout.race_november_dialog, null);
        RecyclerView recyclerView = layout.findViewById(R.id.abort_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        final AbortNovemberAdapter adapter = new AbortNovemberAdapter(races, getRace());
        recyclerView.setAdapter(adapter);
        builder.setTitle(R.string.abort_title);
        builder.setView(layout);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            // We assume that adapter.getSelected() returns the races in an order such that races occurring at
            // lesser
            // indices may depend (directly or transitively) on races with greater indices, but not the other way
            // around.
            final List<ManagedRace> racesToAbort = adapter.getSelected();
            final Set<ActionWithNowTimePoint> actions = new HashSet<>();
            for (final ManagedRace raceToAbort : racesToAbort) { // raceToAbort is "R" from bug 3148 comment #2
                final List<ManagedRace> dependentRaces = activity.getRacesWithStartTimeImmediatelyDependingOn(
                        raceToAbort,
                        new RaceLogRaceStatus[]{RaceLogRaceStatus.STARTPHASE, RaceLogRaceStatus.RUNNING,
                                RaceLogRaceStatus.FINISHING, RaceLogRaceStatus.FINISHED}); // Q
                final Iterable<SimpleRaceLogIdentifier> dependingOnRaces = raceToAbort.getState()
                        .getStartTimeFinderResult().getDependingOnRaces();
                final SimpleRaceLogIdentifier immediatelyDependsOnRace = dependingOnRaces.iterator().hasNext()
                        ? dependingOnRaces.iterator().next()
                        : null; // P
                // create abort command for raceToAbort; if it has a relative start time and immediately depends on
                // another race that is also to be aborted
                // renew the relative start time in the new pass; if it has a relative start time and immediately
                // depends on another race that is *not*
                // to be aborted, only abort but leave without start time definition.
                if (immediatelyDependsOnRace != null) {
                    final ManagedRace raceStateOfRaceThatTheRaceToBeAbortedImmediatelyDependsUpon = getManagedRace(
                            racesToAbort, immediatelyDependsOnRace);
                    if (raceStateOfRaceThatTheRaceToBeAbortedImmediatelyDependsUpon != null) {
                        // the race on which raceToAbort depends immediately also is to be aborted
                        final Duration startTimeDiff = raceToAbort.getState().getStartTimeFinderResult()
                                .getStartTimeDiff();
                        actions.add(new AbortAndRenewRelativeStartTime(raceToAbort, now, flag, startTimeDiff,
                                immediatelyDependsOnRace));
                    } else {
                        // the race on which raceToAbort depends immediately is NOT to be aborted
                        actions.add(new AbortAction(raceToAbort, now, flag));
                    }
                } else {
                    actions.add(new AbortAction(raceToAbort, now, flag));
                }
                for (final ManagedRace dependentRace : dependentRaces) { // dependentRace is "Q" from bug 3148 comment #2
                    final RaceState raceStateOfDependentRace = dependentRace.getState();
                    if (!racesToAbort.contains(dependentRace)) {
                        // materialize an absolute start time for dependentRace
                        actions.add(new MaterializeAbsoluteStartTime(now, dependentRace,
                                raceStateOfDependentRace.getStartTimeFinderResult().getStartTime()));
                    }
                }
            }
            if (!racesToAbort.isEmpty() && flag == Flags.ALPHA) {
                setProtestTime();
            }
            for (final ActionWithNowTimePoint action : actions) {
                action.run();
            }
            if (!racesToAbort.contains(getRace())) {
                BroadcastManager.getInstance(getActivity())
                        .addIntent(new Intent(AppConstants.ACTION_SHOW_MAIN_CONTENT));
            }
        });
        builder.setNegativeButton(R.string.abort_dismiss, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        final Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            int maxItemHeight = (int) getItemHeight(activity) * races.size()
                    + getResources().getDimensionPixelSize(R.dimen.abort_dialog_height);
            lp.height = Math.min(maxItemHeight, (int) getMaxScreenHeight(activity));
            window.setAttributes(lp);
        }
    }

    private ManagedRace getManagedRace(final Iterable<ManagedRace> managedRaces,
                                       final SimpleRaceLogIdentifier raceLogIdentifier) {
        for (final ManagedRace managedRace : managedRaces) {
            if (RaceHelper.getSimpleRaceLogIdentifier(managedRace).equals(raceLogIdentifier)) {
                return managedRace;
            }
        }
        return null;
    }

    private void abortRace(TimePoint now, RaceState raceState, Flags flag) {
        raceState.setAborted(now, /* postponed */ false, flag);
        raceState.setAdvancePass(now);
    }

    private float getItemHeight(Activity activity) {
        TypedValue value = new android.util.TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
        TypedValue.coerceToString(value.type, value.data);
        DisplayMetrics metrics = new android.util.DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return value.getDimension(metrics);
    }

    private float getMaxScreenHeight(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels * 0.85f;
    }
}
