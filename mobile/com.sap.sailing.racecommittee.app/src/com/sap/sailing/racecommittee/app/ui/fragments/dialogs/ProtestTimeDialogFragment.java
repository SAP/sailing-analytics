package com.sap.sailing.racecommittee.app.ui.fragments.dialogs;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.ScreenHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.FinishedTimeFinder;
import com.sap.sailing.domain.base.racegroup.RaceGroupSeriesFleet;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.utils.PreferenceHelper;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ProtestTimeDialogFragment extends AttachedDialogFragment implements View.OnClickListener {

    protected final static int MOVE_DOWN = -1;
    protected final static int MOVE_NONE = 0;
    protected final static int MOVE_UP = 1;
    private static String ARGS_RACE_IDS = "args_race_ids";
    protected ArrayList<ImageView> mDots;
    protected ArrayList<View> mPanels;
    protected int mActivePage = 0;
    private List<ManagedRace> races;
    private ListView mRacesList;
    private TimePicker mTimePicker;
    private TextView mProtestDuration;
    private TextView mProtestEndTime;
    private View customView;
    private View mHome;
    private Button mChoose;
    private AppPreferences mPreferences;
    private Integer mDuration = null;

    public ProtestTimeDialogFragment() {
        races = new ArrayList<>();
    }

    public static ProtestTimeDialogFragment newInstance(List<ManagedRace> races) {
        ArrayList<String> raceIds = new ArrayList<>();
        for (ManagedRace race : races) {
            raceIds.add(race.getId());
        }
        Bundle args = new Bundle();
        args.putStringArrayList(ARGS_RACE_IDS, raceIds);
        ProtestTimeDialogFragment fragment = new ProtestTimeDialogFragment();
        fragment.setArguments(args);
        return fragment;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = super.onCreateView(inflater, container, savedInstanceState);
        if (customView == null) {
            layout = inflater.inflate(R.layout.protest_time_fragment, container, false);

            mDots = new ArrayList<>();
            mPanels = new ArrayList<>();

            ImageView dot;
            dot = ViewHelper.get(layout, R.id.dot_1);
            if (dot != null) {
                mDots.add(dot);
            }
            dot = ViewHelper.get(layout, R.id.dot_2);
            if (dot != null) {
                mDots.add(dot);
            }

            ImageView btnPrev = ViewHelper.get(layout, R.id.nav_prev);
            if (btnPrev != null) {
                btnPrev.setOnClickListener(this);
            }

            ImageView btnNext = ViewHelper.get(layout, R.id.nav_next);
            if (btnNext != null) {
                btnNext.setOnClickListener(this);
            }

            mHome = ViewHelper.get(layout, R.id.header_text);
            if (mHome != null) {
                mHome.setOnClickListener(v -> {
                    Intent intent = new Intent(AppConstants.ACTION_REMOVE_PROTEST);
                    BroadcastManager.getInstance(getActivity()).addIntent(intent);
                });
            }

            mPanels.add(ViewHelper.get(layout, R.id.protest_time_races_list));
            mPanels.add(ViewHelper.get(layout, R.id.protest_time_time_layout));

            getRacesFromArguments();

            mRacesList = ViewHelper.get(layout, R.id.protest_time_races_list);
            setupRacesList(mRacesList);
            mTimePicker = ViewHelper.get(layout, R.id.protest_time_time_picker);
            setupTimePicker(mTimePicker);

            mChoose = ViewHelper.get(layout, R.id.choose);
            if (mChoose != null) {
                mChoose.setOnClickListener(v -> setAndAnnounceProtestTime());
            }

            mProtestDuration = ViewHelper.get(layout, R.id.protest_duration);
            mProtestEndTime = ViewHelper.get(layout, R.id.protest_end);
            View changeDuration = ViewHelper.get(layout, R.id.change_duration);
            if (changeDuration != null) {
                changeDuration.setOnClickListener(new DurationChangeListener());
            }
            updateProtestRange();

            viewPanel(MOVE_NONE);
        }
        return layout;
    }

    @Override
    protected AlertDialog.Builder createDialog(AlertDialog.Builder builder) {
        getRacesFromArguments();
        customView = setupView();
        return builder.setTitle(getString(R.string.protest_dialog_title)).setView(customView);
    }

    @Override
    protected CharSequence getNegativeButtonLabel() {
        return getString(R.string.cancel);
    }

    @Override
    protected CharSequence getPositiveButtonLabel() {
        return getString(R.string.choose);
    }

    @Override
    protected DialogListenerHost getListenerHost() {
        return () -> new DialogListenerHost.DialogResultListener() {
            @Override
            public void onDialogNegativeButton(AttachedDialogFragment dialog) {
                // no operation
            }

            @Override
            public void onDialogPositiveButton(AttachedDialogFragment dialog) {
                setAndAnnounceProtestTime();
            }
        };
    }

    private View setupView() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.protest_time_dialog, null);
        mRacesList = view.findViewById(R.id.protest_time_races_list);
        setupRacesList(mRacesList);
        mTimePicker = view.findViewById(R.id.protest_time_time_picker);
        setupTimePicker(mTimePicker);
        mProtestDuration = view.findViewById(R.id.protest_duration);
        mProtestEndTime = view.findViewById(R.id.protest_end);
        View changeDuration = view.findViewById(R.id.change_duration);
        if (changeDuration != null) {
            changeDuration.setOnClickListener(new DurationChangeListener());
        }
        updateProtestRange();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // TODO: @Samuel -> please check, why we did this, because I can't remember
            ViewGroup.LayoutParams layoutParams = mRacesList.getLayoutParams();
            layoutParams.height = 49 * races.size();
            int screenHeight = (int) (ScreenHelper.on(getActivity()).getScreenHeight() * 0.65);
            if (layoutParams.height > screenHeight) {
                layoutParams.height = screenHeight;
            }
        }
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
        SparseBooleanArray checked = racesList.getCheckedItemPositions();
        for (int i = 0; i < racesList.getCount(); i++) {
            if (checked.get(i)) {
                racesList.setSelection(i);
                break;
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
        Date suggestedDate;
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

        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> updateProtestRange());

        ThemeHelper.setPickerColor(getActivity(), timePicker, ThemeHelper.getColor(requireContext(), R.attr.white),
                ThemeHelper.getColor(requireContext(), R.attr.sap_yellow_1));
    }

    private void getRacesFromArguments() {
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("Arguments needed!");
        }
        ReadonlyDataManager manager = DataManager.create(getActivity());
        ArrayList<String> raceIds = args.getStringArrayList(ARGS_RACE_IDS);
        String raceGroup = null;
        if (raceIds != null) {
            for (String id : raceIds) {
                ManagedRace managedRace = manager.getDataStore().getRace(id);
                if (raceGroup == null) {
                    raceGroup = managedRace.getRaceGroup().getName();
                }
                races.add(managedRace);
            }
        }
        if (raceGroup != null) {
            mPreferences = AppPreferences.on(getActivity(), PreferenceHelper.getRegattaPrefFileName(raceGroup));
        } else {
            mPreferences = AppPreferences.on(getActivity());
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setAndAnnounceProtestTime() {
        List<ManagedRace> selectedRaces = getSelectedRaces();
        TimePoint startTime = TimeUtils.getTime(mTimePicker);
        TimePoint now = MillisecondsTimePoint.now();
        for (ManagedRace race : selectedRaces) {
            Duration duration = Duration.ONE_MINUTE.times(mDuration);
            TimeRange protestTime = new TimeRangeImpl(startTime, startTime.plus(duration));
            race.getState().setProtestTime(now, protestTime);
        }
        if (mHome != null) {
            mHome.callOnClick();
        }
    }

    private List<ManagedRace> getSelectedRaces() {
        List<ManagedRace> result = new ArrayList<>();
        SparseBooleanArray checked = mRacesList.getCheckedItemPositions();
        for (int i = 0; i < mRacesList.getCount(); i++) {
            if (checked.get(i)) {
                result.add(races.get(i));
            }
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.nav_prev) {
            viewPanel(MOVE_DOWN);
        } else if (id == R.id.nav_next) {
            viewPanel(MOVE_UP);
        }
    }

    protected void viewPanel(int direction) {
        if (mDots.size() == 0) {
            return;
        }

        // find next active page (with overflow)
        mActivePage += direction;
        if (mActivePage < 0) {
            mActivePage = mDots.size() - 1;
        }
        if (mActivePage == mDots.size()) {
            mActivePage = 0;
        }

        // ignore invisible dots
        if (mDots.get(mActivePage).getVisibility() == View.GONE) {
            viewPanel(direction);
        }

        // tint all dots gray
        for (ImageView mDot : mDots) {
            int tint = ThemeHelper.getColor(requireContext(), R.attr.sap_light_gray);
            Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
            mDot.setImageDrawable(drawable);
        }

        // tint current dot black
        int tint = ThemeHelper.getColor(requireContext(), R.attr.black);
        Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
        mDots.get(mActivePage).setImageDrawable(drawable);

        // hide all panels
        for (View view : mPanels) {
            view.setVisibility(View.GONE);
        }

        // show current panel
        mPanels.get(mActivePage).setVisibility(View.VISIBLE);

        if (mChoose != null) {
            mChoose.setEnabled(mActivePage == mDots.size() - 1 && getSelectedRaces().size() != 0);
        }
    }

    private void updateProtestRange() {
        if (mDuration == null) {
            mDuration = mPreferences.getProtestTimeDurationInMinutes();
        }
        if (mProtestDuration != null) {
            mProtestDuration.setText(getString(R.string.protest_duration, mDuration));
        }
        if (mProtestEndTime != null) {
            TimePoint startTime = TimeUtils.getTime(mTimePicker);
            TimePoint endTime = startTime.plus(Duration.ONE_MINUTE.times(mDuration));
            mProtestEndTime.setText(getString(R.string.protest_end_time, TimeUtils.formatTime(endTime)));
        }
    }

    private static class ProtestTimeAdapter extends ArrayAdapter<ManagedRaceItem> {

        public ProtestTimeAdapter(Context context, List<ManagedRace> objects) {
            super(context, R.layout.themeable_protest_list_item, wrap(objects));
        }

        private static List<ManagedRaceItem> wrap(List<ManagedRace> races) {
            List<ManagedRaceItem> wrapped = new ArrayList<>();
            for (ManagedRace race : races) {
                wrapped.add(new ManagedRaceItem(race));
            }
            return wrapped;
        }

    }

    private static class ManagedRaceItem {

        private RaceGroupSeriesFleet group;
        private ManagedRace race;

        public ManagedRaceItem(ManagedRace race) {
            this.race = race;
            this.group = new RaceGroupSeriesFleet(race);
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s - %s", group.getDisplayName(true), race.getRaceColumnName());
        }
    }

    private class DurationChangeListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            FrameLayout layout = (FrameLayout) LayoutInflater.from(v.getContext()).inflate(R.layout.protest_duration,
                    null);
            final EditText duration = layout.findViewById(R.id.protest_duration);
            duration.setText(String.valueOf(mDuration));
            duration.setSelection(duration.length());
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(v.getContext().getString(R.string.protest_duration_dialog_title));
            builder.setView(layout);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                try {
                    mDuration = Integer.parseInt(duration.getText().toString());
                } catch (NumberFormatException ignored) {
                }
                updateProtestRange();
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }
}
