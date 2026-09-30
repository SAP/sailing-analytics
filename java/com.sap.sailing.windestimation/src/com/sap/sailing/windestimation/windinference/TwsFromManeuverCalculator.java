package com.sap.sailing.windestimation.windinference;

import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;

/**
 * 
 * Strategy for TWS derivation from the features of a maneuver with estimated maneuver type.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface TwsFromManeuverCalculator {

    /**
     * Determines TWS from maneuver considering already determined TWD.
     * 
     * @param maneuver
     *            The maneuver at which the TWS is determined
     * @param windCourse
     *            Inverted TWD
     * @return TWS. If TWS is zero, then TWS could be determined.
     */
    Speed getWindSpeed(SimpleManeuverForEstimation maneuver, Bearing windCourse);

}
