package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.ManeuverLoss;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

/**
 * Represents maneuvers which start and end encompasses the main curve as well as the section where the speed and course
 * appear unstable.
 * 
 * @author Vladislav Chumak (D069712)
 * @see Maneuver
 */
public class ManeuverWithStableSpeedAndCourseBoundariesImpl extends ManeuverImpl {

    private static final long serialVersionUID = 5831188137884083419L;

    public ManeuverWithStableSpeedAndCourseBoundariesImpl(ManeuverType type, Tack newTack, Position position,
             TimePoint timePoint, ManeuverCurveBoundaries mainCurveBoundaries,
            ManeuverCurveBoundaries maneuverCurveWithStableSpeedAndCourseBoundaries,
            double maxTurningRateInDegreesPerSecond, MarkPassing markPassing, ManeuverLoss maneuverLoss) {
        super(type, newTack, position, timePoint, mainCurveBoundaries,
                maneuverCurveWithStableSpeedAndCourseBoundaries, maxTurningRateInDegreesPerSecond, markPassing, maneuverLoss);
    }

    @Override
    public ManeuverCurveBoundaries getManeuverBoundaries() {
        return getManeuverCurveWithStableSpeedAndCourseBoundaries();
    }

}
