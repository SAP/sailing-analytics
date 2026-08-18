package com.sap.sailing.domain.maneuverhash.impl;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.maneuverhash.SerializableManeuverCache;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * Stores a {@link TrackedRace}'s {@link Maneuver}s after they were loaded successfully from the persistent store.
 * This happens when a race was considered equal regarding its "fingerprint" to a version loaded previously with
 * all maneuvers computed and stored persistently. This saves the computational effort to compute the maneuvers
 * again (persistent caching).<p>
 * 
 * This class collaborates with {@link ManeuverCacheDelegate}.
 */
public class ManeuversFromDatabase implements SerializableManeuverCache {
    private static final long serialVersionUID = 1872340928634087L;
    private static final Logger logger = Logger.getLogger(ManeuversFromDatabase.class.getName());
    private final Map<Competitor, List<Maneuver>> maneuvers;

    /**
     * @param maneuvers may be {@code null}
     */
    public ManeuversFromDatabase(Map<Competitor, List<Maneuver>> maneuvers) {
        super();
        this.maneuvers = maneuvers;
    }
    
    @Override
    public boolean canBeUpdated() {
        return false;
    }

    @Override
    public void recalculate(Competitor competitor) {
        // a no-op because we have everything from the DB already
    }

    public void resume() {
        logger.log(Level.WARNING, "Method should never be called");
        throw new IllegalStateException("Method should never be called");
    }

    public void suspend() {
        // nothing to suspend here
    }

    public List<Maneuver> get(Competitor competitor, boolean waitForLatest) {
        return maneuvers == null ? null : maneuvers.get(competitor);
    }

    @Override
    public void triggerUpdate(Competitor key) {
        logger.log(Level.WARNING, "Method should never be called");
                throw new IllegalStateException("If Fingerprint matches, no Update should be triggered");
    }

    @Override
    public void setManeuverRaceFingerprintRegistry(ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        // no-op; nothing to set here
    }

    @Override
    public void ensureFilled() {
        // no-op; a read-only cache of this type is always filled as good as it can
    }
}