package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.MarkPassing;
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
            Distance maneuverLoss, TimePoint timePoint, ManeuverCurveBoundaries mainCurveBoundaries,
            ManeuverCurveBoundaries maneuverCurveWithStableSpeedAndCourseBoundaries,
            double maxAngularVelocityInDegreesPerSecond, MarkPassing markPassing) {
        super(type, newTack, position, maneuverLoss, timePoint, mainCurveBoundaries,
                maneuverCurveWithStableSpeedAndCourseBoundaries, maxAngularVelocityInDegreesPerSecond, markPassing);
    }

    @Override
    public ManeuverCurveBoundaries getManeuverBoundaries() {
        return getManeuverCurveWithStableSpeedAndCourseBoundaries();
    }

}
