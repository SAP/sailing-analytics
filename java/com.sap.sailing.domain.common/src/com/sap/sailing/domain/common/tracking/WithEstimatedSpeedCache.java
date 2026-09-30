package com.sap.sailing.domain.common.tracking;

import com.sap.sse.common.SpeedWithBearing;

public interface WithEstimatedSpeedCache {
    boolean isEstimatedSpeedCached();

    /**
     * Returns a valid result if {@link #isEstimatedSpeedCached()} returns <code>true</code>
     */
    SpeedWithBearing getCachedEstimatedSpeed();

    void invalidateEstimatedSpeedCache();

    SpeedWithBearing cacheEstimatedSpeed(SpeedWithBearing estimatedSpeed);

}
