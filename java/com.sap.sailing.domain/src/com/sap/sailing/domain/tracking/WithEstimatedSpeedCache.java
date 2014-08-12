package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.SpeedWithBearing;

public interface WithEstimatedSpeedCache {
    boolean isEstimatedSpeedCached();

    /**
     * Returns a valid result if {@link #isEstimatedSpeedCached()} returns <code>true</code>
     */
    SpeedWithBearing getCachedEstimatedSpeed();

    void invalidateEstimatedSpeedCache();

    void cacheEstimatedSpeed(SpeedWithBearing estimatedSpeed);

}
