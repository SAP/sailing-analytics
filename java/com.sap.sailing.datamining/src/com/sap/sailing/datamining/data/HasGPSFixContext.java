package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TackType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

public interface HasGPSFixContext extends HasWindOnTrackedLegOfCompetitor {
    @Connector(scanForStatistics = false)
    HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext();

    @Connector(ordinal = 1)
    GPSFixMoving getGPSFix();
    
    @Dimension(messageKey="TackType", ordinal=6)
    TackType getTackType() throws NoWindException;

    @Statistic(messageKey = "TrueWindAngle")
    Bearing getTrueWindAngle() throws NoWindException;

    @Statistic(messageKey = "AbsoluteTrueWindAngle")
    Bearing getAbsoluteTrueWindAngle() throws NoWindException;

    @Statistic(messageKey = "VMG", resultDecimals = 2)
    SpeedWithBearing getVelocityMadeGood();

    @Statistic(messageKey = "XTE", resultDecimals = 2)
    Distance getXTE();

    @Statistic(messageKey = "AbsoluteXTE", resultDecimals = 2)
    Distance getAbsoluteXTE();
    
    @Statistic(messageKey = "XTERelativeSigned", resultDecimals = 2)
    Double getRelativeXTESigned();
    
    @Statistic(messageKey = "XTERelativeUnsigned", resultDecimals = 2)
    Double getRelativeXTEUnsigned();
    
    /**
     * Instead of projecting onto the course middle line connecting start and end of the leg,
     * projects onto the wind axis, attached to the leg's end waypoint.
     */
    @Statistic(messageKey = "XTEToWindAxisUnsigned", resultDecimals = 2)
    Distance getXTEToWindAxisUnsigned();
    
    /**
     * Instead of projecting onto the course middle line connecting start and end of the leg,
     * projects onto the wind axis, attached to the leg's end waypoint.
     */
    @Statistic(messageKey = "XTEToWindAxisSigned", resultDecimals = 2)
    Distance getXTEToWindAxisSigned();
    
    /**
     * Instead of projecting onto the course middle line connecting start and end of the leg,
     * projects onto the wind axis, attached to the leg's end waypoint. That distance is divided
     * by half the leg length.
     */
    @Statistic(messageKey = "XTEToWindAxisRelativeUnsigned", resultDecimals = 2)
    Double getXTEToWindAxisRelativeUnsigned();
    
    /**
     * Instead of projecting onto the course middle line connecting start and end of the leg,
     * projects onto the wind axis, attached to the leg's end waypoint. That distance is divided
     * by half the leg length.
     */
    @Statistic(messageKey = "XTEToWindAxisRelativeSigned", resultDecimals = 2)
    Double getXTEToWindAxisRelativeSigned();
    
    @Statistic(messageKey = "SmoothedSpeed", resultDecimals = 2)
    Speed getSmoothedSpeed();
    
    /**
     * @return the tenth of the leg the fix is in, computed based on the "windward distance" along the leg; the first
     *         tenth has number 1, the last tenth has number 10. Should the fix not be linked to any leg, the result is
     *         {@code null}. For reaching legs, "windward distance" translates to rhumbline-based distance
     *         (along-course).
     */
    @Dimension(messageKey="TenthInLeg")
    Integer getTenthOfLeg();
}