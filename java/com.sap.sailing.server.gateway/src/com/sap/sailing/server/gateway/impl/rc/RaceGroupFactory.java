package com.sap.sailing.server.gateway.impl.rc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.racegroup.RaceCell;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.base.racegroup.RaceRow;
import com.sap.sailing.domain.base.racegroup.SeriesWithRows;
import com.sap.sailing.domain.base.racegroup.impl.RaceCellImpl;
import com.sap.sailing.domain.base.racegroup.impl.RaceGroupImpl;
import com.sap.sailing.domain.base.racegroup.impl.RaceRowImpl;
import com.sap.sailing.domain.base.racegroup.impl.SeriesWithRowsImpl;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;

/**
 * Used to convert {@link Leaderboard}s to {@link RaceGroup}s.
 */
public class RaceGroupFactory {

    /**
     * Convert given {@link Leaderboard} to a {@link RaceGroup}.
     * @param leaderboard to be converted.
     * @return the {@link RaceGroup}.
     */
    public RaceGroup convert(Leaderboard leaderboard) {
        String name = leaderboard.getName();

        CourseArea courseArea = leaderboard.getDefaultCourseArea();

        BoatClass boatClass = null;
        if (leaderboard instanceof RegattaLeaderboard) {
            boatClass = ((RegattaLeaderboard) leaderboard).getRegatta().getBoatClass();
        }

        Iterable<SeriesWithRows> series = getSeries(leaderboard);

        return new RaceGroupImpl(name, boatClass, courseArea, series);
    }

    public Iterable<SeriesWithRows> getSeries(Leaderboard leaderboard) {

        Map<Series, List<RaceColumn>> seriesToRaceColumns = getSeriesToRaceColumns(leaderboard);

        Collection<SeriesWithRows> seriesWithRows = new ArrayList<>();
        for (Series series : seriesToRaceColumns.keySet()) {
            seriesWithRows.add(new SeriesWithRowsImpl(series.getName(), series.isMedal(), getRows(series,
                    seriesToRaceColumns.get(series))));
        }
        return seriesWithRows;
    }

    private Collection<RaceRow> getRows(Series series, List<RaceColumn> raceColumns) {
        Collection<RaceRow> rows = new ArrayList<>();
        for (Fleet fleet : series.getFleets()) {
            rows.add(new RaceRowImpl(fleet, getCells(fleet, raceColumns)));
        }
        return rows;
    }

    private Collection<RaceCell> getCells(Fleet fleet, List<RaceColumn> raceColumns) {
        Collection<RaceCell> cells = new ArrayList<>();
        for (RaceColumn raceColumn : raceColumns) {
            cells.add(new RaceCellImpl(raceColumn.getName(), raceColumn.getRaceLog(fleet)));
        }
        return cells;
    }

    /**
     * Returns a series to race column mapping. If there are no series all race columns will
     * be mapped from a default series.
     */
    private Map<Series, List<RaceColumn>> getSeriesToRaceColumns(Leaderboard leaderboard) {
        Map<Series, List<RaceColumn>> seriesToRaceColumns = new HashMap<>();

        Series defaultSeries = null;
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            if (raceColumn instanceof RaceColumnInSeries) {
                Series raceColumnSeries = ((RaceColumnInSeries) raceColumn).getSeries();
                insertSeriesIfNew(seriesToRaceColumns, raceColumnSeries).add(raceColumn);
            } else {
                if (defaultSeries == null) {
                    defaultSeries = createDefaultSeries(raceColumn.getFleets());
                    insertSeriesIfNew(seriesToRaceColumns, defaultSeries);
                }
                seriesToRaceColumns.get(defaultSeries).add(raceColumn);
            }
        }
        return seriesToRaceColumns;
    }

    private Series createDefaultSeries(Iterable<? extends Fleet> fleets) {
        Series defaultSeries;
        defaultSeries = new SeriesImpl(LeaderboardNameConstants.DEFAULT_SERIES_NAME, false,
                fleets, Collections.<String> emptyList(), null);
        return defaultSeries;
    }

    private List<RaceColumn> insertSeriesIfNew(Map<Series, List<RaceColumn>> target, Series newSeries) {
        if (!target.containsKey(newSeries)) {
            target.put(newSeries, new ArrayList<RaceColumn>());
        }
        return target.get(newSeries);
    }

}
