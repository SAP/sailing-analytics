package com.sap.sailing.domain.common;

import java.io.Serializable;

public interface RaceIdentifier extends Serializable {
    String getEventName();
    
    String getRaceName();
    
    Object getRace(RaceFetcher raceFetcher);

    /**
     * Blocks and waits if the tracked race for this race identifier doesn't exist yet.
     */
    Object getTrackedRace(RaceFetcher raceFetcher);

    /**
     * Immediately returns <code>null</code> if the tracked race for this race identifier doesn't exist yet.
     */
    Object getExistingTrackedRace(RaceFetcher sailingServiceImpl);
}
