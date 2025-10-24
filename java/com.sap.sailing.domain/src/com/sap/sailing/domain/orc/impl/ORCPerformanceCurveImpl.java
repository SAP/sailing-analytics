package com.sap.sailing.domain.orc.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.analysis.FunctionUtils;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.function.Constant;
import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math3.exception.DimensionMismatchException;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.impl.ORCCertificateImpl;
import com.sap.sailing.domain.orc.ORCPerformanceCurve;
import com.sap.sailing.domain.orc.ORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.impl.NoCachingWindLegTypeAndLegBearingCache;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.SecondsDurationImpl;
import com.sap.sse.common.util.CubicSpline;
import com.sap.sse.common.util.CubicSpline.SplineBoundaryCondition;

/**
 * For a {@link Competitor} and the {@link ORCPerformanceCurveCourse} which the competitor sailed until the creation of
 * an instance, this class represents a so called "Polar Curve". This Curve is specified by the so called "Implied
 * Wind" a {@link Speed} on the x-Axis and the allowance in s/NM respectively a {@link Duration} on the y-Axis. It
 * represents a simplified polar curve for the given boat and the given part of the course. For a given wind speed the
 * performance curve returns the allowance for the boat or in simpler words: how long should the boat need for a
 * nautical mile when sailing 100% performance.
 * 
 * The implementation is oriented to the pascal code provided by ORC for the performance curve module. Available here
 * <a href="https://data.orc.org/tools.php?c=pcs">https://data.orc.org/tools.php?c=pcs</a>.
 * 
 * See also <a href=
 * "http://bugzilla.sapsailing.com/bugzilla/attachment.cgi?id=146">http://bugzilla.sapsailing.com/bugzilla/attachment.cgi?id=146</a>
 * for details. The true wind angles are symmetrical, assuming that the boat performs equally well on both tacks.
 * 
 * @author Daniel Lisunkin (i505543)
 * @author Axel Uhl (d043530)
 * 
 */
public class ORCPerformanceCurveImpl implements Serializable, ORCPerformanceCurve {
    private static final Logger logger = Logger.getLogger(ORCPerformanceCurveImpl.class.getName());
    private static final long serialVersionUID = 4113356173492168453L;

    /**
     * The specific course for which the PerformanceCurve of a boat is calculated. The course is set during the
     * constructor call.
     */
    private final ORCPerformanceCurveCourse course;
    
    /**
     * This PolynomialSplineFunction is created with the array of course specific allowances for the boat which this
     * ORCPerformanceCurve belongs to. This function contains subfunctions for each interval between two given
     * calculated points. The input for the function is the implied wind speed in knots and the output the expected
     * average speed over ground in knots.
     */
    private final UnivariateDifferentiableFunction functionImpliedWindInKnotsToAverageSpeedInKnotsForCourse;
    
    /**
     * The {@link ORCCertificate#getTrueWindAngles()} from the certificate
     */
    private final Bearing[] trueWindAngles;

    /**
     * The {@link ORCCertificate#getTrueWindSpeeds()} from the certificate
     */
    private final Speed[] trueWindSpeeds;

    /**
     * Accepts the simplified polar data, one "column" for each of the defined true wind speeds, where each column is a
     * map from the true wind angle (here expressed as an object of type {@link Bearing}) and the {@link Duration} the
     * boat is assumed to need at that true wind speed/angle for one nautical mile.
     */
    public ORCPerformanceCurveImpl(ORCCertificate certificate, ORCPerformanceCurveCourse course) throws FunctionEvaluationException {
        this(certificate, course, new NoCachingWindLegTypeAndLegBearingCache());
    }
    
    public ORCPerformanceCurveImpl(ORCCertificate certificate, ORCPerformanceCurveCourse course,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws FunctionEvaluationException {
        this.course = course;
        this.trueWindAngles = certificate.getTrueWindAngles();
        this.trueWindSpeeds = certificate.getTrueWindSpeeds();
        functionImpliedWindInKnotsToAverageSpeedInKnotsForCourse = createPerformanceCurve(certificate, cache);
    }

    /**
     * Computes a function that, given a true wind speed (TWS) calculates the average speed in knots at which the boat
     * to which this performance curve belongs is expected to sail the {@link #course}.
     * <p>
     * 
     * The original interpolation in the ORC PCS Pascal code uses the true wind speed values on the X axis and the
     * average velocities in knots on the Y axis. The allowances in seconds per nautical mile are then obtained by
     * dividing 3600 by the average velocity in knots.
     * <p>
     * 
     * The points for the interpolation are constructed from the allowances for each true wind speed value taken from
     * {@link ORCCertificateImpl#ALLOWANCES_TRUE_WIND_SPEEDS}, extended on the left by the value 0 and to the right by
     * the value 10000. On the left, the velocity assumed at 0kts true wind speed is 0kts. On the right, the velocity
     * assumed at 10000kts true wind speed is the same as for the highest wind speed from the original list.
     * <p>
     */
    private UnivariateDifferentiableFunction createPerformanceCurve(ORCCertificate certificate,
            ORCPerformanceCurveCache cache) throws FunctionEvaluationException {
        final Map<Speed, Duration> allowancesForCoursePerTrueWindSpeed = createAllowancesPerCourse(certificate, cache);
        double[] xs = new double[certificate.getTrueWindSpeeds().length+2];
        double[] ys = new double[certificate.getTrueWindSpeeds().length+2];
        int i = 0;
        xs[i] = 0; // see original Pascal code; the first "knot" is set to (0.0, 0.0)
        ys[i] = 0;
        i++;
        for (final Entry<Speed, Duration> entry : allowancesForCoursePerTrueWindSpeed.entrySet()) {
            xs[i] = entry.getKey().getKnots();
            ys[i] = getCourse().getTotalLength().inTime(entry.getValue()).getKnots();
            i++;
        }
        xs[i] = 10000;   // see original Pascal code; the last "knot" is at 10000 knots of true wind speed
        ys[i] = ys[i-1]; // and repeats the last allowance, probably to flatten the curve at its end
        final CubicSpline interpolator = CubicSpline.interpolateBoundariesSorted(xs, ys,
                SplineBoundaryCondition.ParabolicallyTerminated, /* leftBoundary */ 0,
                SplineBoundaryCondition.ParabolicallyTerminated, /* rightBoundary */ 0);
        final UnivariateDifferentiableFunction splineFunction = new UnivariateDifferentiableFunction() {
            @Override
            public double value(double x) {
                return interpolator.interpolate(x);
            }

            @Override
            public DerivativeStructure value(DerivativeStructure t) throws DimensionMismatchException {
                return new DerivativeStructure(t.getFreeParameters(), t.getOrder(), interpolator.interpolate(t.getValue()),
                        interpolator.differentiate(t.getValue()));
            }
        };
        return splineFunction;
    }

    /**
     * Computes the duration the boat to which this performance curve belongs is expected to sail to complete the
     * {@link #course}, keyed by the different true wind speeds. The resulting {@link LinkedHashMap}'s iteration
     * order is guaranteed to deliver the true wind speed keys in the order of ascending wind speeds.
     */
    public LinkedHashMap<Speed, Duration> createAllowancesPerCourse(ORCCertificate certificate,
            ORCPerformanceCurveCache cache) throws FunctionEvaluationException {
        final LinkedHashMap<Speed, Duration> result = new LinkedHashMap<>();
        final Map<ORCPerformanceCurveLeg, Map<Speed, Duration>> allowancesPerLeg = new HashMap<>();
        for (final ORCPerformanceCurveLeg leg : course.getLegs()) {
            allowancesPerLeg.put(leg, createAllowancePerLeg(leg, certificate, cache));
        }
        for (final Speed tws : certificate.getTrueWindSpeeds()) {
            Duration allowancePerTws = new SecondsDurationImpl(0);
            for (final ORCPerformanceCurveLeg leg : course.getLegs()) {
                allowancePerTws = allowancePerTws.plus(allowancesPerLeg.get(leg).get(tws));
            }
            result.put(tws, allowancePerTws);
        }
        return result;
    }

    private Map<Speed, Duration> createAllowancePerLeg(ORCPerformanceCurveLeg leg, ORCCertificate certificate,
            ORCPerformanceCurveCache cache) throws FunctionEvaluationException {
        Map<Speed, Map<Bearing, Speed>> twaAllowances = certificate.getVelocityPredictionPerTrueWindSpeedAndAngle();
        Map<Speed, Bearing> beatAngles = certificate.getBeatAngles();
        Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed = certificate.getBeatVMGPredictions(); 
        Map<Speed, Duration> beatAllowancePerTrueWindSpeed = certificate.getBeatAllowances();
        Map<Speed, Bearing> runAngles = certificate.getRunAngles();
        Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed = certificate.getRunVMGPredictions();
        Map<Speed, Duration> runAllowancePerTrueWindSpeed = certificate.getRunAllowances();
        final Map<Speed, Duration> result = new HashMap<>();
        switch (leg.getType()) {
        case TWA:
            final Bearing absoluteLegTwa = leg.getTwa(cache).abs();
            for (final Speed tws : certificate.getTrueWindSpeeds()) {
                // Case switching on TWA (0. TWA == 0; 1. TWA < Beat; 2. Beat < TWA < Gybe; 3. Gybe < TWA; 4. TWA == 180)
                if (absoluteLegTwa.compareTo(beatAngles.get(tws)) <= 0) {
                    // Case 0 & 1 - result = beatVMG * distance * cos(TWA)
                    result.put(tws, beatAllowancePerTrueWindSpeed.get(tws).times(leg.getLength().getNauticalMiles()).times(Math.cos(absoluteLegTwa.getRadians())));
                } else if (absoluteLegTwa.compareTo(runAngles.get(tws)) >= 0) {
                    // Case 3 & 4 - result = runVMG * distance * cos(TWA)
                    result.put(tws, runAllowancePerTrueWindSpeed.get(tws).times(leg.getLength().getNauticalMiles()).times(Math.cos(Math.PI - absoluteLegTwa.getRadians())));
                } else {
                    // Case 2 - result is given through the laGrange Interpolation, between the Beat and Gybe Angles
                    result.put(tws,
                            getLagrangeSpeedPredictionForTrueWindSpeedAndAngle(twaAllowances, beatAngles,
                                    beatVMGPredictionPerTrueWindSpeed, runAngles, runVMGPredictionPerTrueWindSpeed, tws,
                                    absoluteLegTwa).getDuration(leg.getLength()));
                }
            }
            break;
        case CIRCULAR_RANDOM:
            for (final Speed tws : certificate.getTrueWindSpeeds()) {
                result.put(tws, certificate.getCircularRandomSpeedPredictions().get(tws).getDuration(leg.getLength()));
            }
            break;
        case LONG_DISTANCE:
            for (final Speed tws : certificate.getTrueWindSpeeds()) {
                result.put(tws, certificate.getLongDistanceSpeedPredictions().get(tws).getDuration(leg.getLength()));
            }
            break;
        case WINDWARD_LEEWARD:
        case WINDWARD_LEEWARD_REAL_LIVE:
            for (final Speed tws : certificate.getTrueWindSpeeds()) {
                result.put(tws, certificate.getWindwardLeewardSpeedPrediction().get(tws).getDuration(leg.getLength()));
            }
            break;
        case NON_SPINNAKER:
            for (final Speed tws :certificate.getTrueWindSpeeds()) {
                result.put(tws, certificate.getNonSpinnakerSpeedPredictions().get(tws).getDuration(leg.getLength()));
            }
            break;
        }
        return result;
    }

    /**
     * @return an array containing two arrays: the first array holds the TWAs, the second array holds the corresponding
     *         speed over ground in knots for the TWA at the corresponding index in the first array
     */
    private double[][] createPolarsPerTrueWindSpeed(Speed trueWindSpeed, Map<Speed, Map<Bearing, Speed>> reachingSpeedPredictionsPerTrueWindSpeedAndAngle,
            Map<Speed, Bearing> beatAngles, Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed,
            Map<Speed, Bearing> runAngles, Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed, Bearing[] trueWindAngles) {
        ArrayList<Double> resultWindAngles = new ArrayList<>();
        ArrayList<Double> resultSpeedsOverGroundInKnots = new ArrayList<>();
        Bearing beatAngle = beatAngles.get(trueWindSpeed);
        Bearing runAngle = runAngles.get(trueWindSpeed);
        final double TWO = 2;
        final Bearing TWO_DEGREES = new DegreeBearingImpl(TWO);
        final Bearing MINUS_TWO_DEGREES = new DegreeBearingImpl(-TWO);
        resultWindAngles.add(beatAngle.add(MINUS_TWO_DEGREES).getDegrees());
        resultSpeedsOverGroundInKnots.add(beatVMGPredictionPerTrueWindSpeed.get(trueWindSpeed).getKnots() / Math.cos(beatAngle.add(MINUS_TWO_DEGREES).getRadians()));
        resultWindAngles.add(beatAngle.getDegrees());
        resultSpeedsOverGroundInKnots.add(beatVMGPredictionPerTrueWindSpeed.get(trueWindSpeed).getKnots() / Math.cos(beatAngle.getRadians()));
        for (final Bearing twa : trueWindAngles) {
            if (twa.compareTo(beatAngle) > 0 && twa.compareTo(runAngle) < 0) {
                resultWindAngles.add(twa.getDegrees());
                resultSpeedsOverGroundInKnots.add(reachingSpeedPredictionsPerTrueWindSpeedAndAngle.get(trueWindSpeed).get(twa).getKnots());
            }
        }
        resultWindAngles.add(runAngle.getDegrees());
        resultSpeedsOverGroundInKnots.add(runVMGPredictionPerTrueWindSpeed.get(trueWindSpeed).getKnots() / Math.cos(Math.PI-runAngle.getRadians()));
        resultWindAngles.add(runAngle.add(TWO_DEGREES).getDegrees());
        resultSpeedsOverGroundInKnots.add(runVMGPredictionPerTrueWindSpeed.get(trueWindSpeed).getKnots() / Math.cos(Math.PI-runAngle.add(TWO_DEGREES).getRadians()));
        return new double[][] {resultWindAngles.stream().mapToDouble(d -> d).toArray(), resultSpeedsOverGroundInKnots.stream().mapToDouble(d -> d).toArray()};
    }

    @Override
    public Duration getAllowancePerCourse(Speed trueWindSpeed) throws ArgumentOutsideDomainException {
        return new KnotSpeedImpl(
                functionImpliedWindInKnotsToAverageSpeedInKnotsForCourse.value(trueWindSpeed.getKnots()))
                        .getDuration(getCourse().getTotalLength());
    }

    @Override
    public Speed getImpliedWind(Duration durationToCompleteCourse) throws MaxIterationsExceededException, FunctionEvaluationException{
        final Speed averageSpeedOnCourse = getCourse().getTotalLength().inTime(durationToCompleteCourse);
        final double[] predictedSpeedsInKnotsForTotalCourseByTrueWindSpeed = Arrays.stream(trueWindSpeeds).mapToDouble
                (tws->{ return functionImpliedWindInKnotsToAverageSpeedInKnotsForCourse.value(tws.getKnots()); }).toArray();
        final Speed result;
        // Corner cases for Allowance > Allowance(20kt) or Allowance < Allowance(6kt)
        if (averageSpeedOnCourse.getKnots() >= predictedSpeedsInKnotsForTotalCourseByTrueWindSpeed[predictedSpeedsInKnotsForTotalCourseByTrueWindSpeed.length-1]) {
            result = trueWindSpeeds[trueWindSpeeds.length-1];
        } else if (averageSpeedOnCourse.equals(Speed.NULL) || averageSpeedOnCourse.getKnots() <= predictedSpeedsInKnotsForTotalCourseByTrueWindSpeed[0]) {
            result = trueWindSpeeds[0];
        } else {
            // find the polynomial splined function that produces the durationToCompleteCourse within its validity range
            int i = 1; // skip the auxiliary spline segment from (0.0, 0.0) to (6.0, ...)
            while (i < predictedSpeedsInKnotsForTotalCourseByTrueWindSpeed.length && averageSpeedOnCourse.getKnots() >= predictedSpeedsInKnotsForTotalCourseByTrueWindSpeed[i-1]) {
                i++;
            }
            i--;
            // PolynomialFunction which will be solved by the Newton Approach
            final Constant averageSpeedInKnots = new Constant(averageSpeedOnCourse.getKnots());
            final NewtonRaphsonSolver newtonSolver = new NewtonRaphsonSolver(0.0000000001);
            final UnivariateDifferentiableFunction targetZeroFunction = FunctionUtils.add(functionImpliedWindInKnotsToAverageSpeedInKnotsForCourse,
                    FunctionUtils.multiply((UnivariateDifferentiableFunction) averageSpeedInKnots,
                            (UnivariateDifferentiableFunction) new Constant(-1)));
            final double impliedWindSpeedInKnots = newtonSolver.solve(1000, targetZeroFunction, 12 /* knots of true wind speed */);
            result = new KnotSpeedImpl(impliedWindSpeedInKnots);
        }
        return result;
    }

    @Override
    public Duration getCalculatedTime(ORCPerformanceCurve referenceBoat, Duration sailedDurationPerNauticalMile)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        return referenceBoat.getAllowancePerCourse(getImpliedWind(sailedDurationPerNauticalMile));
    }
    
    @Override
    public ORCPerformanceCurveCourse getCourse() {
        return course;
    }

    // public accessibility needed for tests, not part of the ORCPerformanceCurve contract
    public Speed getLagrangeSpeedPredictionForTrueWindSpeedAndAngle(Map<Speed, Map<Bearing, Speed>> twaAllowances,
            Map<Speed, Bearing> beatAngles, Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed,
            Map<Speed, Bearing> runAngles, Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed, Speed trueWindSpeed,
            Bearing trueWindAngle) throws FunctionEvaluationException, IllegalArgumentException {
        return getLagrangeSpeedPredictionForTrueWindSpeedAndAngle(twaAllowances, beatAngles,
                beatVMGPredictionPerTrueWindSpeed, runAngles, runVMGPredictionPerTrueWindSpeed, trueWindSpeed,
                trueWindAngle, trueWindAngles);
    }
    
    public Speed getLagrangeSpeedPredictionForTrueWindSpeedAndAngle(Map<Speed, Map<Bearing, Speed>> twaAllowances,
            Map<Speed, Bearing> beatAngles, Map<Speed, Speed> beatVMGPredictionPerTrueWindSpeed,
            Map<Speed, Bearing> runAngles, Map<Speed, Speed> runVMGPredictionPerTrueWindSpeed, Speed trueWindSpeed,
            Bearing trueWindAngle, Bearing[] trueWindAngles) throws FunctionEvaluationException, IllegalArgumentException {
        final Speed result;
        double[][] polarPoints = createPolarsPerTrueWindSpeed(trueWindSpeed, twaAllowances, beatAngles,
                beatVMGPredictionPerTrueWindSpeed, runAngles, runVMGPredictionPerTrueWindSpeed, trueWindAngles);
        double[] twaPolarPoints = polarPoints[0];
        double[] speedsOverGroundInKnotsPolarPoints = polarPoints[1];
        int i = -1; // after the loop, i equals the next higher available polar data for the given TWA
        for (int j = 0; j < twaPolarPoints.length; j++) {
            if (trueWindAngle.getDegrees() < twaPolarPoints[j]) {
                i = j;
                break;
            }
        }
        // This part is implemented equally to the part from the ORC PCSLib.pas.
        // ORC decides to use only up to the nearest 4 values to the searched TWA for interpolation,
        // cutting off at the rims, so potentially working only with three values.
        if (i >= 0) {
            int upperBound = Math.min(i + 1, twaPolarPoints.length - 1);
            int lowerBound = Math.max(i - 2, 0);
            double[] xn = new double[upperBound - lowerBound + 1];
            double[] yn = new double[upperBound - lowerBound + 1];
            for (i = lowerBound; i <= upperBound; i++) {
                xn[i - lowerBound] = twaPolarPoints[i];
                yn[i - lowerBound] = speedsOverGroundInKnotsPolarPoints[i];
            }
            result = new KnotSpeedImpl(new PolynomialFunctionLagrangeForm(xn, yn).value(trueWindAngle.getDegrees()));
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Obtains the durations that the boat for which this performance curve was created is expected to sail to complete
     * the {@link #course} (which may be a prefix of a longer course) at the true wind speed provided to the resulting
     * map as the key. Iteration order is in ascending true wind speeds.
     */
    private LinkedHashMap<Speed, Duration> getAllowancesPerTrueWindSpeedsForCourse() throws ArgumentOutsideDomainException {
        final LinkedHashMap<Speed, Duration> result = new LinkedHashMap<>();
        for (final Speed tws : trueWindSpeeds) {
            result.put(tws, getAllowancePerCourse(tws));
        }
        return result;
    }
    
    @Override
    public String toString() {
        try {
            return "ORCPerformanceCurve [Allowances " + allowancesToString(getAllowancesPerTrueWindSpeedsForCourse()) + "]";
        } catch (FunctionEvaluationException e) {
            logger.warning("Exception trying to compute string representation of an ORC Performance Curve object: "+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String allowancesToString(Map<Speed, Duration> allowancesPerTrueWindSpeedsForCourse) {
        final StringBuilder result = new StringBuilder();
        for (final Entry<Speed, Duration> e : allowancesPerTrueWindSpeedsForCourse.entrySet()) {
            result.append(e.getKey());
            result.append(':');
            final Speed averageSpeed = getCourse().getTotalLength().inTime(e.getValue());
            result.append(averageSpeed.getDuration(ORCCertificate.NAUTICAL_MILE).asSeconds());
            result.append("s/NM, ");
            result.append(e.getValue().asSeconds());
            result.append("s total; ");
        }
        return result.substring(0, result.length()-2);
    }
}
