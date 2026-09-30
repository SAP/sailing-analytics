package com.sap.sailing.polars.windestimation;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

public interface ManeuverClassification {

    double getLikelihoodForManeuverType(ManeuverType jibe);

    double getManeuverAngleDeg();

    Bearing getMiddleManeuverCourse();

    Speed getSpeedAtManeuverStart();

    String format(String string);

    SpeedWithBearingWithConfidence<Void> getEstimatedWindSpeedAndBearing(ManeuverType maneuverType);

    Position getPosition();

    TimePoint getTimePoint();

    ScalableBearingAndScalableDouble getScalableMiddleManeuverCourseAndManeuverAngleDeg();

    BoatClass getBoatClass();

    String getCompetitorName();

}
