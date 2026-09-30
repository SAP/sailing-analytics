package com.sap.sailing.windestimation.data;

public interface SimpleManeuverWithEstimatedType<T extends SimpleManeuverForEstimation> extends Comparable<SimpleManeuverWithEstimatedType<T>> {

    ManeuverTypeForClassification getManeuverType();

    T getManeuver();

    double getConfidence();

}
