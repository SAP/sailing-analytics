package com.sap.sailing.domain.leaderboard;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util.Pair;

public interface HasRaceColumns {
    Iterable<? extends RaceColumn> getRaceColumns();
    
    void addRaceColumnListener(RaceColumnListener listener);
    
    void removeRaceColumnListener(RaceColumnListener listener);
    
    /**
     * Looks through all {@link #getRaceColumns() race columns} and their {@link RaceColumn#getFleets() fleets} and checks
     * if {@code trackedRace} is {@link RaceColumn#getTrackedRace(Fleet) linked} to that combination. If such a slot is found
     * that "slot" is returned by a pair specifying the non-{@code null} {@link RaceColumn} and {@code Fleet} pair. Otherwise,
     * {@code null} is returned. {@code null} is also returned if {@code trackedRace==null}.
     */
    default Pair<RaceColumn, Fleet> getRaceColumnAndFleet(TrackedRace trackedRace) {
        if (trackedRace != null) {
            for (final RaceColumn raceColumn : getRaceColumns()) {
                for (final Fleet fleet : raceColumn.getFleets()) {
                    if (raceColumn.getTrackedRace(fleet) == trackedRace) {
                        return new Pair<>(raceColumn, fleet);
                    }
                }
            }
        }
        return null;
    }
    
    default boolean hasTrackedRace(TrackedRace trackedRace) {
        return getRaceColumnAndFleet(trackedRace) != null;
    }
}
