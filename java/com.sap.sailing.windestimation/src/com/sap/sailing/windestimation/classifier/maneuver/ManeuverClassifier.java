package com.sap.sailing.windestimation.classifier.maneuver;

import com.sap.sailing.windestimation.data.ManeuverForEstimation;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface ManeuverClassifier {

    ManeuverWithProbabilisticTypeClassification classifyManeuver(ManeuverForEstimation maneuver);

    double getTestScore();
    
    boolean hasSupportForProvidedFeatures();

}
