package com.sap.sailing.windestimation.data;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

public class SimpleManeuverForEstimationImpl implements SimpleManeuverForEstimation {
    private final TimePoint maneuverTimePoint;
    private final Position maneuverPosition;
    private final Bearing middleCourse;
    private final SpeedWithBearing speedWithBearingBefore;
    private final SpeedWithBearing speedWithBearingAfter;
    private final boolean clean;
    private final BoatClass boatClass;

    public SimpleManeuverForEstimationImpl(TimePoint maneuverTimePoint, Position maneuverPosition, Bearing middleCourse,
            SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter, boolean clean,
            BoatClass boatClass) {
        super();
        this.maneuverTimePoint = maneuverTimePoint;
        this.maneuverPosition = maneuverPosition;
        this.middleCourse = middleCourse;
        this.speedWithBearingBefore = speedWithBearingBefore;
        this.speedWithBearingAfter = speedWithBearingAfter;
        this.clean = clean;
        this.boatClass = boatClass;
    }

    @Override
    public Bearing getMiddleCourse() {
        return middleCourse;
    }

    @Override
    public Position getManeuverPosition() {
        return maneuverPosition;
    }

    @Override
    public TimePoint getManeuverTimePoint() {
        return maneuverTimePoint;
    }

    @Override
    public BoatClass getBoatClass() {
        return boatClass;
    }

    @Override
    public boolean isClean() {
        return clean;
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingBefore() {
        return speedWithBearingBefore;
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingAfter() {
        return speedWithBearingAfter;
    }

    @Override
    public int compareTo(SimpleManeuverForEstimation o) {
        return getManeuverTimePoint().compareTo(o.getManeuverTimePoint());
    }
}
