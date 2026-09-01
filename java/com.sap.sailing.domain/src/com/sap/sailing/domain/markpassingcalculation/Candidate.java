package com.sap.sailing.domain.markpassingcalculation;

import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateImpl;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;

/**
 * Represent the possible {@link MarkPassing} of a {@link Waypoint}. It contains the {@link Waypoint} it might be passing, a
 * {@link TimePoint}, the probability that this candidate is a passing (e.g. based on the distance to the
 * {@link Waypoint}) and the one-based(!) ID of this Waypoint. The ID is one based because the standard implementation
 * of {@link CandidateChooser} (see {@link CandidateChooserImpl}) uses a proxy Candidates at the end and the beginning
 * of the race, the one at the beginning recieves the ID 0. 
 * 
 * @author Nicolas Klose
 * 
 */
public interface Candidate extends Comparable<CandidateImpl>, Timed {

    Waypoint getWaypoint();
    
    /**
     * Is this a candidate for a fixed mark passing?
     */
    default boolean isFixed() {
        return false;
    }

    int getOneBasedIndexOfWaypoint();

    /**
     * @return the probability that this actually is a passing.
     */
    Double getProbability();

    int compareTo(Candidate other);
    
    long getTimePointAsMillis();
}
