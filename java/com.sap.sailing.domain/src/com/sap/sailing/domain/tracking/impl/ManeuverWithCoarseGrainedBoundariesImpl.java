package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

/**
 * Maneuver implementation which were detected on tracks with extremely low GPS-sampling rate. The implementation
 * suggests to ignore the following attributes:
 * <ul>
 * <li>Maneuver loss</li>
 * <li>Max. and Avg. Turning rate</li>
 * <li>Time point before and after of all maneuver boundaries</li>
 * <li>Lowest speed within all maneuver boundaries</li>
 * </ul>
 * However, to provide capability with existing code, the attributes are filled with values.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverWithCoarseGrainedBoundariesImpl extends ManeuverImpl {

    private static final long serialVersionUID = -381990329349665889L;

    public ManeuverWithCoarseGrainedBoundariesImpl(ManeuverType type, Tack newTack, Position position,
            TimePoint timePoint, ManeuverCurveBoundaries maneuverBoundaries) {
        super(type, newTack, position, timePoint, maneuverBoundaries, maneuverBoundaries,
                Math.abs(maneuverBoundaries.getDirectionChangeInDegrees()), /* mark passing */ null,
                /* maneuver loss */ null);
    }

    @Override
    public ManeuverCurveBoundaries getManeuverBoundaries() {
        return getMainCurveBoundaries();
    }

}
