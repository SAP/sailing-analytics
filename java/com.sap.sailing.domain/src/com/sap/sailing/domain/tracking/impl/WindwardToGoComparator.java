package com.sap.sailing.domain.tracking.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.NoWindError;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;

/**
 * Compares competitor tracks based on the windward distance they still have to go and/or leg completion times at a
 * given point in time for the same leg. Two tracks of different competitors may end up being ranked equal by this
 * comparator. So take care and don't use this comparator class when inserting into an {@link SortedSet} when you want
 * something like a ranking. It may overwrite existing entries. Use {@link Collections#sort(java.util.List, Comparator)}
 * instead.
 */
public class WindwardToGoComparator implements Comparator<TrackedLegOfCompetitor> {
    private final TrackedLeg trackedLeg;
    private final TimePoint timePoint;
    private final Map<TrackedLegOfCompetitor, Distance> wwdtgCache;

    public WindwardToGoComparator(TrackedLeg trackedLeg, TimePoint timePoint) {
        this.trackedLeg = trackedLeg;
        this.timePoint = timePoint;
        wwdtgCache = new HashMap<TrackedLegOfCompetitor, Distance>();
    }
    
    @Override
    public int compare(TrackedLegOfCompetitor o1, TrackedLegOfCompetitor o2) {
        assert o1.getLeg() == o2.getLeg();
        try {
            int result;
            if (o1.hasFinishedLeg(timePoint)) {
                if (o2.hasFinishedLeg(timePoint)) {
                    result = trackedLeg.getTrackedRace().getMarkPassing(o1.getCompetitor(), trackedLeg.getLeg().getTo()).getTimePoint().compareTo(
                            trackedLeg.getTrackedRace().getMarkPassing(o2.getCompetitor(), trackedLeg.getLeg().getTo()).getTimePoint());
                } else {
                    result = -1; // o1 < o2 because o1 already finished the leg but o2 didn't
                }
            } else if (o2.hasFinishedLeg(timePoint)) {
                result = 1; // o1 > o2 because o2 already finished the leg but o1 didn't
            } else {
                // both didn't finish the leg yet; check which one has started:
                if (o1.hasStartedLeg(timePoint)) {
                    if (o2.hasStartedLeg(timePoint)) {
                        Distance o1d = getWindwardDistanceToGo(o1);
                        Distance o2d = getWindwardDistanceToGo(o2);
                        result = o1d==null?(o2d==null?0:1):o2d==null?1:o1d.compareTo(o2d); // smaller distance to go means smaller rank
                    } else {
                        result = -1;
                    }
                } else if (o2.hasStartedLeg(timePoint)) {
                    result = 1;
                } else {
                    // both did not start the leg:
                    result = 0;
                }
            }
            return result;
        } catch (NoWindException e) {
            throw new NoWindError(e);
        }
    }

    private Distance getWindwardDistanceToGo(TrackedLegOfCompetitor o1) throws NoWindException {
        Distance result = wwdtgCache.get(o1);
        if (result == null) {
            result = o1.getWindwardDistanceToGo(timePoint);
            wwdtgCache.put(o1, result);
        }
        return result;
    }
}
