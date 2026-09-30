package com.sap.sailing.polars.mining;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.polars.impl.CubicEquation;
import com.sap.sailing.polars.regression.IncrementalLeastSquares;
import com.sap.sailing.polars.regression.impl.IncrementalAnyOrderLeastSquaresImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.confidence.BearingWithConfidence;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

/**
 * This container has two regressions. One for boatSpeed over windSpeed and one for TWA (true wind angle) over windSpeed
 * estimations.<p>
 * 
 * It can return speed and angle for a given windSpeed and should only be used for restricted sets of input data. There
 * should be one instance per BoatClass+LegType combination. This seperation needs to be taken care of in a higher level
 * of the application.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class AngleAndSpeedRegression implements Serializable {

    private static final long serialVersionUID = 6343595388753945979L;
    private final IncrementalLeastSquares speedRegression;
    private final IncrementalLeastSquares angleRegression;

    private double maxWindSpeedInKnots;

    public AngleAndSpeedRegression() {
        speedRegression = new IncrementalAnyOrderLeastSquaresImpl(3, false);
        angleRegression = new IncrementalAnyOrderLeastSquaresImpl(3);
        maxWindSpeedInKnots = -1;
    }

    /**
     * Constructor with parameters used by {@link AngleAndSpeedRegressionDeserializer} to deserialize regression data
     * from remote server
     */
    public AngleAndSpeedRegression(double maxWindSpeedInKnots, IncrementalLeastSquares speedRegression,
            IncrementalLeastSquares angleRegression) {
        this.speedRegression = speedRegression;
        this.angleRegression = angleRegression;
        this.maxWindSpeedInKnots = maxWindSpeedInKnots;
    }

    public void addData(WindWithConfidence<Pair<Position, TimePoint>> windSpeed,
            BearingWithConfidence<Void> angleToTheWind, SpeedWithBearingWithConfidence<TimePoint> boatSpeed) {
        double windSpeedInKnots = windSpeed.getObject().getKnots();
        if (windSpeedInKnots > maxWindSpeedInKnots) {
            maxWindSpeedInKnots = windSpeedInKnots;
        }
        speedRegression.addData(windSpeedInKnots, boatSpeed.getObject().getKnots());
        angleRegression.addData(windSpeedInKnots, angleToTheWind.getObject().getDegrees());
    }

    /**
     * Estimate the speed and angle for a given windSpeed by using the regressions contained in this class.
     * Seperation by boatclass and legtype needs to be done on a higher level.
     */
    public SpeedWithBearingWithConfidence<Void> estimateSpeedAndAngle(Speed windSpeed)
            throws NotEnoughDataHasBeenAddedException {
        double windSpeedInKnots = windSpeed.getKnots();
        if (windSpeedInKnots > maxWindSpeedInKnots) {
            throw new NotEnoughDataHasBeenAddedException();
        }
        double estimatedSpeed = speedRegression.getOrCreatePolynomialFunction().value(windSpeedInKnots);
        double estimatedAngle = angleRegression.getOrCreatePolynomialFunction().value(windSpeedInKnots);
        Bearing bearing = new DegreeBearingImpl(estimatedAngle);
        SpeedWithBearing speedWithBearing = new KnotSpeedWithBearingImpl(estimatedSpeed, bearing);
        return new SpeedWithBearingWithConfidenceImpl<Void>(speedWithBearing, Math.min(1,
                speedRegression.getNumberOfAddedPoints() / 100.0), null);
    }

    /**
     * Estimates wind speed candidates for the given input scenario based on the speed and angle regressions.
     */
    public Set<SpeedWithBearingWithConfidence<Void>> estimateTrueWindSpeedAndAngleCandidates(Speed speedOverGround,
            LegType legType, Tack tack) throws NotEnoughDataHasBeenAddedException {
        Set<SpeedWithBearingWithConfidence<Void>> result = new HashSet<>();
        final double[] coefficients = speedRegression.getOrCreatePolynomialFunction().getCoefficients();
        if (coefficients.length > 2) { // we've seen cases where this array only held two elements; see bug5762
            CubicEquation equation = new CubicEquation(coefficients[2], coefficients[1], coefficients[0],
                    -speedOverGround.getKnots());
            double[] windSpeedCandidates = equation.solve();
            for (int i = 0; i < windSpeedCandidates.length; i++) {
                double windSpeedCandidateInKnots = windSpeedCandidates[i];
                if (windSpeedCandidateInKnots >= 0 && windSpeedCandidateInKnots <= maxWindSpeedInKnots) {
                    double angle = 0;
                    boolean angleFound;
                    try {
                        angle = angleRegression.getOrCreatePolynomialFunction().value(windSpeedCandidateInKnots);
                        if (tack == Tack.PORT) {
                            angle = -angle;
                        }
                        angleFound = true;
                    } catch (NotEnoughDataHasBeenAddedException e) {
                        angleFound = false;
                    }
                    if (angleFound) {
                        result.add(new SpeedWithBearingWithConfidenceImpl<Void>(new KnotSpeedWithBearingImpl(
                                windSpeedCandidateInKnots, new DegreeBearingImpl(angle)), 0.5 /* FIXME */, null));
                    }
                }
            }
        }
        return result;
    }

    public PolynomialFunction getSpeedRegressionFunction() throws NotEnoughDataHasBeenAddedException {
        return speedRegression.getOrCreatePolynomialFunction();
    }

    public PolynomialFunction getAngleRegressionFunction() throws NotEnoughDataHasBeenAddedException {
        return angleRegression.getOrCreatePolynomialFunction();
    }

    public IncrementalAnyOrderLeastSquaresImpl getSpeedRegression() {
        return (IncrementalAnyOrderLeastSquaresImpl) speedRegression;
    }

    public IncrementalAnyOrderLeastSquaresImpl getAngleRegression() {
        return (IncrementalAnyOrderLeastSquaresImpl) angleRegression;
    }

    public double getMaxWindSpeedInKnots() {
        return maxWindSpeedInKnots;
    }
    
}
