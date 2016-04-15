package com.sap.sailing.domain.markpassingcalculation;

import java.io.Serializable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sse.common.TimePoint;

public interface CandidateChooser extends Serializable {
    /**
     * Calculates any new {@link MarkPassing}s and notifies the {@link DynamicTrackedRace}.
     * 
     * @param candidateDeltas
     *            new {@link CandidateImpl}s and those that should be removed.
     */

    public void calculateMarkPassDeltas(Competitor c, Iterable<Candidate> newCans, Iterable<Candidate> oldCans);

    void removeWaypoints(Iterable<Waypoint> ways);

    void addWaypoints(Iterable<Waypoint> waypoints);

    void setFixedPassing(Competitor c, Integer zeroBasedIndexOfWaypoint, TimePoint t);

    void removeFixedPassing(Competitor c, Integer zeroBasedIndexOfWaypoint);

    void suppressMarkPassings(Competitor c, Integer zeroBasedIndexOfWaypoint);

    void stopSuppressingMarkPassings(Competitor c);


}
