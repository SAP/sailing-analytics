package com.sap.sailing.windestimation.data;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

public interface SimpleManeuverForEstimation extends Comparable<SimpleManeuverForEstimation> {

    Bearing getMiddleCourse();

    Position getManeuverPosition();

    TimePoint getManeuverTimePoint();

    BoatClass getBoatClass();

    boolean isClean();

    SpeedWithBearing getSpeedWithBearingBefore();

    SpeedWithBearing getSpeedWithBearingAfter();

}
