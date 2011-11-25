package com.sap.sailing.domain.tracking.impl;

import java.util.Comparator;

import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.tracking.Wind;

/**
 * Compares two {@link Wind} objects first by their {@link Wind#getTimePoint() time point}. Only if both
 * wind fixes were taken at the same time, their position is used as a secondary criteria. A more or less
 * arbitrary ordering is used, sorting first by latitude, and if that is equal too, sorting by longitude.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class WindComparator implements Comparator<Timed> {
    public static final Comparator<Timed> INSTANCE = new WindComparator();

    @Override
    public int compare(Timed o1, Timed o2) {
        int result = o1.getTimePoint().compareTo(o2.getTimePoint());
        if (result == 0) {
            if (o1 instanceof Wind && o2 instanceof Wind) {
                Wind o1Wind = (Wind) o1;
                Wind o2Wind = (Wind) o2;
                // use the coordinates as secondary criteria:
                result = Double.compare(o1Wind.getPosition().getLatDeg(), o2Wind.getPosition().getLatDeg());
                if (result == 0) {
                    result = Double.compare(o1Wind.getPosition().getLngDeg(), o2Wind.getPosition().getLngDeg());
                }
            }
        }
        return result;
    }
}

