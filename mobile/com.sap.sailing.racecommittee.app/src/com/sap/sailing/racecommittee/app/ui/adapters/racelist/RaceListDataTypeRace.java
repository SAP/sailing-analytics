package com.sap.sailing.racecommittee.app.ui.adapters.racelist;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.sap.sailing.domain.base.racegroup.RaceGroupSeriesFleet;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sse.common.TimePoint;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RaceListDataTypeRace implements RaceListDataType {

    private static String unscheduledTemplate;
    private static String prescheduledTemplate;
    private static String scheduldedTemplate;
    private static String startPhaseTemplate;
    private static String runningTemplate;
    private static String finishingTemplate;
    private static String finishedTemplate;
    private static String unknownTemplate;

    private final ManagedRace race;
    private final RaceGroupSeriesFleet fleet;
    private final LayoutInflater mInflater;
    private boolean updateIndicatorVisible = false;
    private RaceLogRaceStatus currentStatus = RaceLogRaceStatus.UNKNOWN;

    private Format scheduleFormatter = new SimpleDateFormat("HH:mm", Locale.US);

    public static void initializeTemplates(Fragment fragment) {
        unscheduledTemplate = fragment.getString(R.string.racelist_unscheduled);
        prescheduledTemplate = fragment.getString(R.string.racelist_prescheduled);
        scheduldedTemplate = fragment.getString(R.string.racelist_scheduled);
        startPhaseTemplate = fragment.getString(R.string.racelist_startphase);
        runningTemplate = fragment.getString(R.string.racelist_running);
        finishingTemplate = fragment.getString(R.string.racelist_finishing);
        finishedTemplate = fragment.getString(R.string.racelist_finished);
        unknownTemplate = fragment.getString(R.string.racelist_unknown);
    }

    public RaceListDataTypeRace(ManagedRace race, LayoutInflater layoutInflater) {
        this(race, null, layoutInflater);
    }

    public RaceListDataTypeRace(ManagedRace race, RaceGroupSeriesFleet fleet, LayoutInflater layoutInflater) {
        this.race = race;
        this.fleet = fleet;
        this.currentStatus = race.getStatus();
        this.mInflater = layoutInflater;
    }

    public void onStatusChanged(RaceLogRaceStatus status, boolean allowUpdateIndicator) {
        if (!currentStatus.equals(status)) {
            currentStatus = status;
            setUpdateIndicatorVisible(allowUpdateIndicator);
        }
    }

    public void setUpdateIndicatorVisible(boolean visible) {
        this.updateIndicatorVisible = visible;
    }

    public boolean isUpdateIndicatorVisible() {
        return updateIndicatorVisible;
    }

    public String getRaceName() {
        return race.getName();
    }

    public ManagedRace getRace() {
        return race;
    }

    public RaceGroupSeriesFleet getFleet() {
        return fleet;
    }

    public RaceLogRaceStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getStatusText() {
        return getStatusString(currentStatus);
    }

    private String getStatusString(RaceLogRaceStatus status) {
        switch (status) {
        case UNSCHEDULED:
            return unscheduledTemplate;
        case PRESCHEDULED:
            return String.format(prescheduledTemplate, formatStartTime());
        case SCHEDULED:
            return String.format(scheduldedTemplate, formatStartTime());
        case STARTPHASE:
            return String.format(startPhaseTemplate, formatStartTime());
        case RUNNING:
            return String.format(runningTemplate, formatStartTime());
        case FINISHING:
            return finishingTemplate;
        case FINISHED:
            return finishedTemplate;
        default:
            return unknownTemplate;
        }
    }

    private String formatStartTime() {
        TimePoint startTime = race.getState().getStartTime();
        if (startTime == null) {
            return unknownTemplate;
        }
        return scheduleFormatter.format(startTime.asDate());
    }

    @Override
    public View getView(ViewGroup parent) {
        return mInflater.inflate(R.layout.race_list_area_item, parent, false);
    }

}
