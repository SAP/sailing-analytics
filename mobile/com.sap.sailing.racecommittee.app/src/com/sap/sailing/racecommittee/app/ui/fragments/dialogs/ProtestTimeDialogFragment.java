package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TimePicker;

import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.racelog.analyzing.impl.FinishedTimeFinder;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.impl.BoatClassSeriesFleet;

public class ProtestTimeDialogFragment extends BaseDialogFragment {

    private static String ARGS_RACE_IDS = ProtestTimeDialogFragment.class.getSimpleName() + ".raceids";

    public static ProtestTimeDialogFragment newInstace(List<ManagedRace> races) {
        ArrayList<Serializable> raceIds = new ArrayList<Serializable>();
        for (ManagedRace race : races) {
            raceIds.add(race.getId());
        }
        Bundle args = new Bundle();
        args.putSerializable(ARGS_RACE_IDS, raceIds);

        ProtestTimeDialogFragment fragment = new ProtestTimeDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private List<ManagedRace> races;
    private ListView racesList;
    private TimePicker timePicker;

    public ProtestTimeDialogFragment() {
        races = new ArrayList<ManagedRace>();
    }

    @Override
    protected Builder createDialog(Builder builder) {
        getRacesFromArguments();
        View view = setupView();
        return builder.setTitle(getString(R.string.protest_dialog_title)).setView(view);
    }

    @Override
    protected CharSequence getNegativeButtonLabel() {
        return "Cancel";
    }

    @Override
    protected CharSequence getPositiveButtonLabel() {
        return "Set";
    }

    @Override
    protected DialogFragmentButtonListener getHost() {
        return new DialogFragmentButtonListener() {

            @Override
            public void onDialogPositiveButton() {
                setAndAnnounceProtestTime();
            }

            @Override
            public void onDialogNegativeButton() {

            }
        };
    }

    private View setupView() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.protest_time_view, null);
        racesList = (ListView) view.findViewById(R.id.protest_time_races_list);
        setupRacesList(racesList);
        timePicker = (TimePicker) view.findViewById(R.id.protest_time_time_time_picker);
        setupTimePicker(timePicker);
        return view;
    }

    private void setupRacesList(ListView racesList) {
        racesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        racesList.setAdapter(new ProtestTimeAdapter(getActivity(), races));
        {
            int i = 0;
            for (ManagedRace race : races) {
                racesList.setItemChecked(i++, isFinishedToday(race));
            }
        }
        {
            SparseBooleanArray checked = racesList.getCheckedItemPositions();
            for (int i = 0; i < racesList.getCount(); i++) {
                if (checked.get(i)) {
                    racesList.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupTimePicker(TimePicker timePicker) {
        timePicker.setIs24HourView(true);

        TimePoint recentFinishedTime = null;
        for (ManagedRace race : races) {
            TimePoint currentFinishedTime = race.getState().getFinishedTime();
            if (currentFinishedTime != null
                    && (recentFinishedTime == null || recentFinishedTime.before(currentFinishedTime))) {
                recentFinishedTime = currentFinishedTime;
            }
        }
        Date suggestedDate = null;
        if (recentFinishedTime != null) {
            suggestedDate = recentFinishedTime.asDate();
        } else {
            suggestedDate = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(suggestedDate);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        timePicker.setCurrentHour(hours);
        timePicker.setCurrentMinute(minutes);
    }

    private void getRacesFromArguments() {
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("Arguments needed!");
        }

        ReadonlyDataManager manager = DataManager.create(getActivity());
        @SuppressWarnings("unchecked")
        ArrayList<Serializable> raceIds = (ArrayList<Serializable>) args.getSerializable(ARGS_RACE_IDS);
        for (Serializable id : raceIds) {
            races.add(manager.getDataStore().getRace(id));
        }
    }

    private void setAndAnnounceProtestTime() {
        List<ManagedRace> selectedRaces = getSelectedRaces();
        TimePoint protestTime = getProtestTime();
        for (ManagedRace race : selectedRaces) {
            race.getState().setProtestStartTime(protestTime);
        }
    }

    private List<ManagedRace> getSelectedRaces() {
        List<ManagedRace> result = new ArrayList<ManagedRace>();
        SparseBooleanArray checked = racesList.getCheckedItemPositions();
        for (int i = 0; i < racesList.getCount(); i++) {
            if (checked.get(i)) {
                result.add(races.get(i));
            }
        }
        return result;
    }

    private TimePoint getProtestTime() {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
        time.set(Calendar.MINUTE, timePicker.getCurrentMinute());
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return new MillisecondsTimePoint(time.getTime());
    }

    private static boolean isFinishedToday(ManagedRace race) {
        if (race.getStatus().equals(RaceLogRaceStatus.FINISHED)) {
            FinishedTimeFinder analyzer = new FinishedTimeFinder(race.getRaceLog());
            TimePoint finishedTime = analyzer.analyze();
            if (finishedTime != null) {
                Calendar finishedCalendar = Calendar.getInstance();
                finishedCalendar.setTime(finishedTime.asDate());
                Calendar now = Calendar.getInstance();
                return finishedCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && finishedCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
            }
        }
        return false;
    }

    private static class ProtestTimeAdapter extends ArrayAdapter<ManagedRaceItem> {

        private static List<ManagedRaceItem> wrap(List<ManagedRace> races) {
            List<ManagedRaceItem> wrapped = new ArrayList<ManagedRaceItem>();
            for (ManagedRace race : races) {
                wrapped.add(new ManagedRaceItem(race));
            }
            return wrapped;
        }

        public ProtestTimeAdapter(Context context, List<ManagedRace> objects) {
            super(context, android.R.layout.simple_list_item_multiple_choice, wrap(objects));
        }

    }

    private static class ManagedRaceItem {

        private BoatClassSeriesFleet group;
        private ManagedRace race;

        public ManagedRaceItem(ManagedRace race) {
            this.race = race;
            this.group = new BoatClassSeriesFleet(race);
        }

        @Override
        public String toString() {
            return String.format("%s - %s", group.getDisplayName(), race.getRaceName());
        }

    }

}