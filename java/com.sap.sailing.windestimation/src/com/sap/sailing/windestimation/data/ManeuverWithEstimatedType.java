package com.sap.sailing.windestimation.data;

/**
 * Represents a final maneuver classification with estimated maneuver type and its confidence.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverWithEstimatedType extends SimpleManeuverWithEstimatedTypeImpl<ManeuverForEstimation> {

    public ManeuverWithEstimatedType(ManeuverForEstimation maneuver, ManeuverTypeForClassification maneuverType, double confidence) {
        super(maneuver, maneuverType, confidence);
    }
}
