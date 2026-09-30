package com.sap.sailing.domain.maneuverhash;

import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.tracking.Maneuver;

public interface ManeuverCache {
    void resume();

    List<Maneuver> get(Competitor key, boolean waitForLatest);

    void suspend();

    void triggerUpdate(Competitor key);
    
    boolean canBeUpdated();

    /**
     * Called after a race has finished loading. Different implementations may react differently to this event.
     * A cache loaded from the DB will simply ignore this request. A cache that is empty and based on calculation
     * will need to trigger an update.
     */
    void recalculate(Competitor competitor);
}