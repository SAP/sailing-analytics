package com.sap.sailing.racecommittee.app.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.gate.GateStartRacingProcedure;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.SeriesBase;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.base.racegroup.RaceGroupSeriesFleet;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.GateStartTimingFragment;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

public class RaceHelper {
    public static String getRaceName(@Nullable ManagedRace race) {
        return getRaceName(race, null);
    }

    public static String getRaceName(@Nullable ManagedRace race, @Nullable String delimiter) {
        delimiter = getDefaultDelimiter(delimiter);

        String raceName = "";
        if (race != null) {
            raceName = getRaceGroupName(race);
            raceName += getSeriesName(race.getSeries(), delimiter);
            raceName += getFleetName(race.getFleet(), delimiter);
            raceName += delimiter + race.getRaceColumnName();
        }

        return raceName;
    }

    public static String getReverseRaceName(@Nullable ManagedRace race) {
        return getReverseRaceName(race, null);
    }

    public static String getReverseRaceName(@Nullable ManagedRace race, @Nullable String delimiter) {
        delimiter = getDefaultDelimiter(delimiter);

        String raceName = "";
        if (race != null) {
            raceName += race.getRaceColumnName();
            raceName += getFleetName(race.getFleet(), delimiter);
            raceName += getSeriesName(race.getSeries(), delimiter);
            raceName += delimiter + getRaceGroupName(race);
        }

        return raceName;
    }

    public static String getReverseRaceFleetName(@Nullable ManagedRace race) {
        return getReverseRaceFleetName(race, null);
    }

    public static String getReverseRaceFleetName(@Nullable ManagedRace race, @Nullable String delimiter) {
        delimiter = getDefaultDelimiter(delimiter);

        String raceName = "";
        if (race != null) {
            raceName += race.getRaceColumnName();
            raceName += getFleetName(race.getFleet(), delimiter);
        }

        return raceName;
    }

    public static String getShortReverseRaceName(@Nullable ManagedRace race, @Nullable String delimiter,
            @NonNull ManagedRace race2) {
        delimiter = getDefaultDelimiter(delimiter);
        String raceName = "";
        if (race != null) {
            int maxElements = 3;
            raceName += race.getRaceColumnName();
            String groupName = getRaceGroupName(race);
            String seriesName = getSeriesName(race.getSeries(), delimiter);
            String fleetName = getFleetName(race.getFleet(), delimiter);
            if (groupName.equals(getRaceGroupName(race2))) {
                maxElements -= 1;
            }
            if (maxElements == 2 && seriesName.equals(getSeriesName(race2.getSeries(), delimiter))) {
                maxElements -= 1;
            }
            if (maxElements >= 1) {
                raceName += fleetName;
            }
            if (maxElements >= 2) {
                raceName += seriesName;
            }
            if (maxElements >= 3) {
                raceName += delimiter + groupName;
            }
        }
        return raceName;
    }

    public static String getRaceGroupName(@Nullable ManagedRace race) {
        String raceGroupName = "";
        if (race != null) {
            raceGroupName = getRaceGroupName(race.getRaceGroup());
        }

        return raceGroupName;
    }

    public static String getRaceGroupName(@Nullable RaceGroup raceGroup) {
        String raceGroupName = "";
        if (raceGroup != null) {
            raceGroupName = raceGroup.getDisplayName();
            if (TextUtils.isEmpty(raceGroupName)) {
                raceGroupName = raceGroup.getName();
            }
        }

        return raceGroupName;
    }

    public static String getFleetSeries(@Nullable ManagedRace race) {
        String fleetSeries = "";
        if (race != null) {
            fleetSeries = getFleetSeries(race.getFleet(), race.getSeries());
        }

        return fleetSeries;
    }

    public static String getFleetSeries(@Nullable Fleet fleet, @Nullable SeriesBase series) {
        return getFleetSeries(fleet, series, null);
    }

    public static String getFleetSeries(@Nullable Fleet fleet, @Nullable SeriesBase series,
            @Nullable String delimiter) {
        delimiter = getDefaultDelimiter(delimiter);

        String fleetSeries = "";
        fleetSeries += getFleetName(fleet, "");
        if (!TextUtils.isEmpty(fleetSeries)) {
            fleetSeries += delimiter;
        }
        fleetSeries += getSeriesName(series, "");

        return fleetSeries;
    }

    public static String getSeriesName(@Nullable SeriesBase series) {
        return getSeriesName(series, null);
    }

    public static String getSeriesName(@Nullable SeriesBase series, @Nullable String delimiter) {
        delimiter = getDefaultDelimiter(delimiter);
        String seriesName = "";
        if (series != null && !LeaderboardNameConstants.DEFAULT_SERIES_NAME.equals(series.getName())) {
            seriesName = delimiter + series.getName();
        }
        return seriesName;
    }

    public static String getFleetName(@Nullable Fleet fleet) {
        return getFleetName(fleet, null);
    }

    public static String getFleetName(@Nullable Fleet fleet, @Nullable String delimiter) {
        delimiter = getDefaultDelimiter(delimiter);
        String fleetName = "";
        if (fleet != null && !LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleet.getName())) {
            fleetName = delimiter + fleet.getName();
        }
        return fleetName;
    }

    public static String getGateTiming(Context context, GateStartRacingProcedure procedure, RaceGroup raceGroup) {
        String timing;
        long launchTime = procedure.getGateLaunchStopTime() / GateStartTimingFragment.ONE_MINUTE_MILLISECONDS;
        long golfTime = procedure.getGolfDownTime() / GateStartTimingFragment.ONE_MINUTE_MILLISECONDS;
        if (AppPreferences.on(context, PreferenceHelper.getRegattaPrefFileName(raceGroup.getName()))
                .getGateStartHasAdditionalGolfDownTime()) {
            timing = context.getString(R.string.gate_time_schedule_long, launchTime, golfTime, launchTime + golfTime);
        } else {
            timing = context.getString(R.string.gate_time_schedule_short, launchTime);
        }
        return timing;
    }

    private static String getDefaultDelimiter(String delimiter) {
        return delimiter == null ? " - " : delimiter;
    }

    /**
     * Obtains the set of races in the same race group / regatta and the same series and the same fleet as
     * {@code currentRace}.
     */
    public static List<ManagedRace> getManagedRacesAsList(
            @NonNull LinkedHashMap<RaceGroupSeriesFleet, List<ManagedRace>> racesByGroup,
            @Nullable ManagedRace currentRace) {
        List<ManagedRace> races = new ArrayList<>();
        // Find the race group / series / fleet for the currentRace and add all races in that group/series/fleet
        String raceGroupName = "";
        if (currentRace != null) {
            raceGroupName = getRaceGroupName(currentRace);
            raceGroupName += getSeriesName(currentRace.getSeries());
            raceGroupName += getFleetName(currentRace.getFleet());
        }
        for (RaceGroupSeriesFleet raceGroupSeriesFleet : racesByGroup.keySet()) {
            String currentGroup = getRaceGroupName(raceGroupSeriesFleet.getRaceGroup());
            currentGroup += getSeriesName(raceGroupSeriesFleet.getSeries());
            currentGroup += getFleetName(raceGroupSeriesFleet.getFleet());
            if (currentGroup.equals(raceGroupName)) {
                List<ManagedRace> matchingRaces = racesByGroup.get(raceGroupSeriesFleet);
                races.addAll(matchingRaces);
            }
        }
        return races;
    }

    public static List<ManagedRace> getPreSelectedRaces(
            @NonNull LinkedHashMap<RaceGroupSeriesFleet, List<ManagedRace>> racesByGroup,
            @Nullable ManagedRace currentRace) {
        List<ManagedRace> managedRaces = getManagedRacesAsList(racesByGroup, currentRace);
        List<ManagedRace> preselectedRaces = new ArrayList<>();
        for (ManagedRace race : managedRaces) {
            if (race != null) {
                RaceLogRaceStatus status = race.getState().getStatus();
                boolean check = status != null && (status.equals(RaceLogRaceStatus.PRESCHEDULED)
                        || status.equals(RaceLogRaceStatus.SCHEDULED) || status.equals(RaceLogRaceStatus.STARTPHASE)
                        || status.equals(RaceLogRaceStatus.RUNNING) || status.equals(RaceLogRaceStatus.FINISHING));
                if (check) {
                    preselectedRaces.add(race);
                }
            }
        }
        return preselectedRaces;
    }

    public static SimpleRaceLogIdentifier getSimpleRaceLogIdentifier(@Nullable ManagedRace race) {
        SimpleRaceLogIdentifier identifier = null;
        if (race != null) {
            identifier = new SimpleRaceLogIdentifierImpl(race.getRaceGroup().getName(), race.getRaceColumnName(),
                    race.getFleet().getName());
        }
        return identifier;
    }
}
