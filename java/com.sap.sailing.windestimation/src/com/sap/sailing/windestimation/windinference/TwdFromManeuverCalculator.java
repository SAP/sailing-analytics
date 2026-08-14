package com.sap.sailing.windestimation.windinference;

import java.io.Serializable;

import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedType;
import com.sap.sse.common.Bearing;

/**
 * Strategy for TWD derivation from the features of a maneuver with estimated maneuver type.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface TwdFromManeuverCalculator extends Serializable {

    /**
     * Determines inverted TWD considering the maneuver and its estimated maneuver type.
     * 
     * @param maneuverWithEstimatedType
     *            The maneuver with its estimated type from which TWD will be derived
     * @return Inverted TWD or {@code null} if no TWD could be determined
     */
    Bearing getTwd(SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation> maneuverWithEstimatedType);

}
