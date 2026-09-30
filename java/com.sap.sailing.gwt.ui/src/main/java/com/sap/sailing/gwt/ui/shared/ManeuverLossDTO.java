package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;

public class ManeuverLossDTO implements IsSerializable {
    private Position maneuverStartPosition;
    private Position maneuverEndPosition;
    private SpeedWithBearing speedWithBearingBefore;
    private Double middleManeuverAngle;
    private Duration maneuverDuration;
    private Distance projectedDistanceLost;
    
    @Deprecated // for GWT serialization only
    ManeuverLossDTO() {
    }

    public ManeuverLossDTO(Position maneuverStartPosition, Position maneuverEndPosition,
            SpeedWithBearing speedWithBearingBefore, Double middleManeuverAngle, Duration maneuverDuration, Distance projectedDistanceLost) {
        this.maneuverStartPosition = maneuverStartPosition;
        this.maneuverEndPosition = maneuverEndPosition;
        this.speedWithBearingBefore = speedWithBearingBefore;
        this.middleManeuverAngle = middleManeuverAngle;
        this.maneuverDuration = maneuverDuration;
        this.projectedDistanceLost = projectedDistanceLost;
    }
    
    public Duration getManeuverDuration() {
        return maneuverDuration;
    }
    public Position getManeuverStartPosition() {
        return maneuverStartPosition;
    }
    public Position getManeuverEndPosition() {
        return maneuverEndPosition;
    }
    public Double getMiddleManeuverAngle() {
        return middleManeuverAngle;
    }
    public SpeedWithBearing getSpeedWithBearingBefore() {
        return speedWithBearingBefore;
    }
    public Distance getDistanceLost() {
        return projectedDistanceLost;
    }
}