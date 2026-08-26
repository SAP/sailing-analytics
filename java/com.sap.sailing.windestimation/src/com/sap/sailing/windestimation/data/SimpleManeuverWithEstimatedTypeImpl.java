package com.sap.sailing.windestimation.data;

public class SimpleManeuverWithEstimatedTypeImpl<T extends SimpleManeuverForEstimation> implements SimpleManeuverWithEstimatedType<T> {
    private final ManeuverTypeForClassification maneuverType;
    private final double confidence;
    private final T maneuver;

    
    public SimpleManeuverWithEstimatedTypeImpl(T maneuver, ManeuverTypeForClassification maneuverType, double confidence) {
        super();
        this.maneuverType = maneuverType;
        this.confidence = confidence;
        this.maneuver = maneuver;
    }

    @Override
    public ManeuverTypeForClassification getManeuverType() {
        return maneuverType;
    }

    @Override
    public T getManeuver() {
        return maneuver;
    }

    @Override
    public double getConfidence() {
        return confidence;
    }

    @Override
    public int compareTo(SimpleManeuverWithEstimatedType<T> o) {
        return getManeuver().compareTo(o.getManeuver());
    }

    @Override
    public String toString() {
        return "" + maneuver + " of type " + maneuverType + ", confidence="
                + confidence;
    }
}