package com.sap.sailing.windestimation.model.regressor.twdtransition;

import com.sap.sailing.windestimation.data.TwdTransition;
import com.sap.sailing.windestimation.model.ContextSpecificModelMetadata;
import com.sap.sailing.windestimation.model.store.ContextType;

public class TwdTransitionRegressorModelMetadata extends ContextSpecificModelMetadata<TwdTransition> {

    private static final long serialVersionUID = 1120422671027132155L;

    public TwdTransitionRegressorModelMetadata() {
        super(ContextType.TWD_TRANSITION);
    }

    @Override
    public double[] getX(TwdTransition instance) {
        return new double[] { getDistanceValue(instance), getDurationValue(instance) };
    }

    public double getDurationValue(double[] x) {
        return x[1];
    }

    public double getDistanceValue(double[] x) {
        return x[0];
    }

    public double getDurationValue(TwdTransition instance) {
        return instance.getDuration().asSeconds();
    }

    public double getDistanceValue(TwdTransition instance) {
        return instance.getDistance().getMeters();
    }

    @Override
    public boolean isContainsAllFeatures(TwdTransition instance) {
        return true;
    }

    @Override
    public int getNumberOfInputFeatures() {
        return 2;
    }

    @Override
    public int getNumberOfPossibleTargetValues() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return "TwdTransitionRegression";
    }

    @Override
    public int hashCode() {
        return getContextType().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof TwdTransitionRegressorModelMetadata) {
            return getContextType().equals(((TwdTransitionRegressorModelMetadata) obj).getContextType());
        }
        return false;
    }

    @Override
    public String toString() {
        return "TwdTransitionRegressorModelMetadata [getContextType()=" + getContextType() + "]";
    }

}
