package com.sap.sailing.domain.common.tracking;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

/**
 * Specific {@link SensorFix} for bravo devices (http://www.bravosystems.es/).
 * 
 * <p>
 * <b>NOTE:</b> This fix type is currently used to track ESS foiling data, only.
 * </p>
 * 
 * 
 */
public interface BravoFix extends SensorFix {
    Distance MIN_FOILING_HEIGHT_THRESHOLD = new MeterDistance(0.1);
    /**
     * Gets the ride height (foiling) for this fix.
     * 
     * @return the ride height
     */
    @Statistic(messageKey="rideHeight", resultDecimals=1)
    Distance getRideHeight();

    @Statistic(messageKey="rideHeightPort", resultDecimals=1)
    Distance getRideHeightPortHull();

    @Statistic(messageKey="rideHeightStarboard", resultDecimals=1)
    Distance getRideHeightStarboardHull();
    
    @Dimension(messageKey="IsFoiling")
    boolean isFoiling();
    
    /**
     * Same as {@link #isFoiling()}, only that instead of using the {@link #MIN_FOILING_HEIGHT_THRESHOLD} constant as
     * the threshold value for the ride height, here the caller can provide a specific ride height that must be reached
     * for a fix to be considered foiling.
     */
    boolean isFoiling(Distance minimumRideHeight);

    @Statistic(messageKey="pitch", resultDecimals=1)
    Bearing getPitch();

    @Statistic(messageKey = "heel", resultDecimals = 1)
    Bearing getHeel();

}
