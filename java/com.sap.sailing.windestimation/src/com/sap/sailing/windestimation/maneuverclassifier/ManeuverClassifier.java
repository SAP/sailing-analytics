package com.sap.sailing.windestimation.maneuverclassifier;

import com.sap.sailing.windestimation.data.ManeuverForEstimation;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface ManeuverClassifier {

    ManeuverEstimationResult classifyManeuver(ManeuverForEstimation maneuver);

    double getTestScore();

}
