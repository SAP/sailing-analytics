package com.sap.sailing.domain.tracking;

import java.io.Serializable;

import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;

/**
 * Updated ManeuverLoss class to use it to visualize the correct maneuver loss on the race map. To draw the ManeuverLoss
 * needs the maneuverStartPosition, maneuverEndPosition, middleManeuverAngle, speedWithBearingBefore and
 * maneuverDuration. The ManeuverLoss is calculated by {@link ManeuverDetectorImpl#getManeuverLoss()}.
 * 
 * @author Vladislav Chumak (D069712)
 */
public class ManeuverLoss implements Serializable {
    private static final long serialVersionUID = 5622852569855L;
    private final Distance distanceSailedProjectedOnMiddleManeuverAngle;
    private final Distance distanceSailedIfNotManeuveringProjectedOnMiddeManeuverAngle;
    private final Position maneuverStartPosition;
    private final Position maneuverEndPosition;
    private final Duration maneuverDuration;
    private final SpeedWithBearing speedWithBearingBefore;
    private final Bearing middleManeuverAngle;
    
    public ManeuverLoss(Distance distanceSailedProjectedOnMiddleManeuverAngle,
            Distance distanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle, Position maneuverStartPosition,
            Position maneuverEndPosition, Duration maneuverDuration, SpeedWithBearing speedWithBearingBefore,
            Bearing middleManeuverAngle) {
        this.distanceSailedProjectedOnMiddleManeuverAngle = distanceSailedProjectedOnMiddleManeuverAngle;
        this.distanceSailedIfNotManeuveringProjectedOnMiddeManeuverAngle = distanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle;
        this.maneuverStartPosition = maneuverStartPosition;
        this.maneuverEndPosition = maneuverEndPosition;
        this.maneuverDuration = maneuverDuration;
        this.speedWithBearingBefore = speedWithBearingBefore;
        this.middleManeuverAngle = middleManeuverAngle;
    }

    public Distance getDistanceSailedProjectedOnMiddleManeuverAngle() {
        return distanceSailedProjectedOnMiddleManeuverAngle;
    }

    public Distance getDistanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle() {
        return distanceSailedIfNotManeuveringProjectedOnMiddeManeuverAngle;
    }
    
    public Distance getProjectedDistanceLost() {
        return distanceSailedIfNotManeuveringProjectedOnMiddeManeuverAngle.add(distanceSailedProjectedOnMiddleManeuverAngle.scale(-1));
    }
    
    /**
     * Gets the ratio between {@link #getDistanceSailedProjectedOnMiddleManeuverAngle()} and
     * {@link #getDistanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle()}.
     */
    public double getRatioBetweenDistanceSailedWithAndWithoutManeuver() {
        return getDistanceSailedProjectedOnMiddleManeuverAngle().getMeters() / getDistanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle().getMeters();
    }
    
    public Position getManeuverStartPosition() {
        return maneuverStartPosition;
    }
    public Position getManeuverEndPosition() {
        return maneuverEndPosition;
    }
    public Duration getManeuverDuration() {
        return maneuverDuration;
    }
    public SpeedWithBearing getSpeedWithBearingBefore() {
        return speedWithBearingBefore;
    }
    public Bearing getMiddleManeuverAngle() {
        return middleManeuverAngle;
    }
}
