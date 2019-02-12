package com.sap.sailing.windestimation.windinference;

import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.ManeuverWithEstimatedType;
import com.sap.sse.common.Bearing;

/**
 * Determines TWD from the middle course of tack or jibe maneuvers. For head-up and bear-away maneuvers, no TWD is
 * determined.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class MiddleCourseBasedTwdCalculatorImpl implements TwdFromManeuverCalculator {

    private static final long serialVersionUID = -7920503233105279148L;

    @Override
    public Bearing getTwd(ManeuverWithEstimatedType maneuverWithEstimatedType) {
        Bearing twd = null;
        if (maneuverWithEstimatedType.getManeuverType() == ManeuverTypeForClassification.TACK
                || maneuverWithEstimatedType.getManeuverType() == ManeuverTypeForClassification.JIBE) {
            ManeuverForEstimation maneuver = maneuverWithEstimatedType.getManeuver();
            twd = maneuver.getMiddleCourse();
            if (maneuverWithEstimatedType.getManeuverType() == ManeuverTypeForClassification.JIBE) {
                twd = twd.reverse();
            }
        }
        return twd;
    }

}
