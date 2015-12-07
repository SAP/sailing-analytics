package com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;

import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;

/**
 * Contains a {@link PolynomialSplineFunction} that describes the boat speed at incoming wind angles from 0 to 180 degrees.
 * 
 * @author Alexander Ries (D062114)
 *
 */
public class DefaultPolarWindAngleBoatSpeedFunction {
    private PolynomialSplineFunction windAngleBoatBearingSpeedFunction;
    
    /**
     * X and Y values for the wind angle boat speed spline function. Ranging from 0 to 180 degrees wind angle.
     * */
    private final double [] windAngleXValues = {0.0, 10.0, 33.0, 44.0, 55.0, 96.0, 130.0, 155.0, 180.0};
    private final double [] boatBearingYValues = {0.0, 7.0, 11.0, 11.5, 12.5, 14.5, 15.0, 17.0, 11.0};
       
    public DefaultPolarWindAngleBoatSpeedFunction() {
        SplineInterpolator splineInterpolator = new SplineInterpolator();
        windAngleBoatBearingSpeedFunction = splineInterpolator.interpolate(windAngleXValues, boatBearingYValues);
    }
    
    public Speed getBoatSpeedForWindAngleAndSpeed(double angle, double speed) {
        Speed result = null;
        try {
            double boatSpeed = windAngleBoatBearingSpeedFunction.value(convert360AngleTo180RangeAngle(angle));
            result = new KnotSpeedImpl(boatSpeed);
        } catch (ArgumentOutsideDomainException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    private double convert360AngleTo180RangeAngle(double angle) {
        double result = 0;
        if (angle <= 180) {
            result = angle;
        }
        if (angle > 180) {
            result = 360 - angle;
        }
        return result;
    }
}
