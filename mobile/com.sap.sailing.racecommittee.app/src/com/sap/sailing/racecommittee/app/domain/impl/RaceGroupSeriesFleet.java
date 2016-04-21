package com.sap.sailing.racecommittee.app.domain.impl;

import android.text.TextUtils;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.SeriesBase;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sse.common.Util;

/**
 * Represents a triple of {@link RaceGroup} (representing a regatta or a flexible leaderboard), a {@link SeriesBase series}
 * and a {@link Fleet}. An instance can be constructed for a {@link ManagedRace} which then extracts these three properties
 * from the race. Note that equal objects of this type can result for different races as long as they are in the equal
 * {@link RaceGroup}, {@link SeriesBase series} and {@link Fleet}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RaceGroupSeriesFleet {

    private RaceGroup raceGroup;
    private Fleet fleet;
    private SeriesBase series;
    private int seriesOrder;
    private int fleetOrder;

    public RaceGroupSeriesFleet(ManagedRace race) {
        raceGroup = race.getRaceGroup();
        series = race.getSeries();
        fleet = race.getFleet();
        seriesOrder = getSeriesIndex(race, series);
        fleetOrder = getFleetIndex(series.getFleets(), race.getFleet());
    }

    private static int getSeriesIndex(ManagedRace race, SeriesBase series) {
        return Util.indexOf(race.getRaceGroup().getSeries(), series);
    }

    private int getFleetIndex(Iterable<? extends Fleet> fleets, Fleet fleet) {
        return Util.indexOf(fleets, fleet);
    }

    public RaceGroup getRaceGroup() {
        return raceGroup;
    }

    public SeriesBase getSeries() {
        return series;
    }

    public Fleet getFleet() {
        return fleet;
    }

    public String getRaceGroupName() {
        return raceGroup.getName();
    }

    public String getSeriesName() {
        return series.getName();
    }

    public String getFleetName() {
        return fleet.getName();
    }

    public String getDisplayName() {
        return getDisplayName(false);
    }

    public String getDisplayName(boolean useDisplayName) {
        String name = raceGroup.getDisplayName();
        if (!useDisplayName || TextUtils.isEmpty(name)) {
            name = raceGroup.getName();
        }
        if (series != null && !series.getName().equals(LeaderboardNameConstants.DEFAULT_SERIES_NAME)) {
            name += " - " + series.getName();
        }
        if (fleet != null && !fleet.getName().equals(LeaderboardNameConstants.DEFAULT_FLEET_NAME)) {
            name += " - " + fleet.getName();
        }
        return name;
    }

    public int getSeriesOrder() {
        return seriesOrder;
    }

    public int getFleetOrder() {
        return fleetOrder;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((raceGroup == null) ? 0 : raceGroup.hashCode());
        result = prime * result + ((series == null) ? 0 : series.hashCode());
        result = prime * result + ((fleet == null) ? 0 : fleet.hashCode());
        result = prime * result + seriesOrder;
        result = prime * result + fleetOrder;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RaceGroupSeriesFleet other = (RaceGroupSeriesFleet) obj;
        if (raceGroup == null) {
            if (other.raceGroup != null) {
                return false;
            }
        } else if (!raceGroup.equals(other.raceGroup)) {
            return false;
        }
        if (fleet == null) {
            if (other.fleet != null) {
                return false;
            }
        } else if (!fleet.equals(other.fleet)) {
            return false;
        }
        if (series == null) {
            if (other.series != null) {
                return false;
            }
        } else if (!series.equals(other.series)) {
            return false;
        }
        if (seriesOrder != other.seriesOrder) {
            return false;
        }
        if (fleetOrder != other.fleetOrder) {
            return false;
        }
        return true;
    }

}
