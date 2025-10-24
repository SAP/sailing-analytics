package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.math3.analysis.FunctionUtils;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.function.Constant;
import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.util.CubicSpline;
import com.sap.sse.common.util.CubicSpline.SplineBoundaryCondition;

/**
 * This test class has the goal to show the accuracy of the splining method used. The spline is calculated when creating
 * an {@link ORCPerformanceCurve} for a {@link Competitor} for a given {@link TimePoint} on the {@link Course}.
 * <p>
 * 
 * The used course in here consists out of the following legs:
 * 
 * <pre>
 * [No. | TWA | length]
 * - 1. | 010° | 2.23nm
 * - 2. | 170° | 2.00nm
 * - 3. | 000° | 0.97nm
 * - 4. | 015° | 1.03nm
 * - 5. | 165° | 1.03nm
 * - 6. | 180° | 1.17nm
 * --------------------
 * Total Length: 8.43nm
 * </pre>
 * 
 * The calculated velocity predictions for the total course used in the interpolation are calculated correctly. These
 * velocities are checked with the values from Manage2Sail/SwissTiming and the used ORC certificates are from the 15th
 * July 2019.
 * <p>
 * 
 * Note that the ORC PCS algorithm prescribes two additional knots in the spline input: one at (0, 0) and one at (10000,
 * &lt;value-for-20kts&gt;).
 * 
 * @author Daniel Lisunkin (i505543)
 *
 */
public class TestORCSplineCalculation {

    private static final double[] xn = { 0, 6, 8, 10, 12, 14, 16, 20, 10000 };
    private static final double totalCourseLengthInNauticalMiles = 8.43;
    private static final double accuracy = 0.00003;

    @Test
    public void testMoana() {
        assertEquals(1.5,
                interpolate(new double[] { 0, 4.641211342235869, 5.736695797981516, 6.546702285181061, 7.02579946374626,
                        7.298101614710148, 7.608915286969485, 8.275181564996913, 8.275181564996913 }, 7.76218),
                accuracy);
    }

    @Test
    public void testMoanaInverse() {
        assertEquals(7.76218,
                interpolate(1.5,
                        new double[] { 0, 4.641211342235869, 5.736695797981516, 6.546702285181061, 7.02579946374626,
                                7.298101614710148, 7.608915286969485, 8.275181564996913, 8.275181564996913 }),
                accuracy);
    }

    @Test
    public void testMilan() { // currently M2S Values, not SAP
        assertEquals(1.0, interpolate(new double[] { 0, 5.33185058489951, 6.6728248852317, 7.6093958376704,
                8.22730088526476, 8.72313604446258, 9.26000390485965, 10.2627508086351, 10.2627508086351 }, 12.80881),
                accuracy);
    }

    @Test
    public void testMilanInverse() { // currently M2S Values, not SAP
        assertEquals(
                12.80881, interpolate(1.0, new double[] { 0, 5.33185058489951, 6.6728248852317, 7.6093958376704,
                        8.22730088526476, 8.72313604446258, 9.26000390485965, 10.2627508086351, 10.2627508086351 }),
                accuracy);
    }

    @Test
    public void testTutima() { // currently M2S Values, not SAP
        assertEquals(1.5, interpolate(new double[] { 0, 4.37052612624555, 5.35354546146804, 6.0793252346297,
                6.57769022120612, 6.90362398496227, 7.18395896558393, 7.64346679121362, 7.64346679121362 }, 8.65816),
                accuracy);
    }

    @Test
    public void testTutimaInverse() { // currently M2S Values, not SAP
        assertEquals(8.65816, interpolate(1.5, new double[] { 0, 4.37052612624555, 5.35354546146804, 6.0793252346297,
                6.57769022120612, 6.90362398496227, 7.18395896558393, 7.64346679121362, 7.64346679121362 }),
                accuracy);
    }

    @Test
    public void testBank() { // currently M2S Values, not SAP
        assertEquals(1.5, interpolate(new double[] { 0, 4.46956401552239, 5.580405095336, 6.43249233334714,
                6.94538913004541, 7.2362905433394, 7.53723401035461, 8.08306640881786, 8.08306640881786 }, 8.07975),
                accuracy);
    }

    @Test
    public void testBankInverse() { // currently M2S Values, not SAP
        assertEquals(8.07975, interpolate(1.5, new double[] { 0, 4.46956401552239, 5.580405095336, 6.43249233334714,
                6.94538913004541, 7.2362905433394, 7.53723401035461, 8.08306640881786, 8.08306640881786 }),
                accuracy);
    }

    @Test
    public void testHaspa() { // currently M2S Values, not SAP
        assertEquals(1.5, interpolate(new double[] { 0, 4.60774719206753, 5.72829292214811, 6.56131742170233,
                7.08141937866223, 7.38334720210887, 7.71474004575847, 8.31942615578485, 8.31942615578485 }, 7.78413),
                accuracy);
    }

    @Test
    public void testHaspaInverse() { // currently M2S Values, not SAP
        assertEquals(7.78413, interpolate(1.5, new double[] { 0, 4.60774719206753, 5.72829292214811, 6.56131742170233,
                7.08141937866223, 7.38334720210887, 7.71474004575847, 8.31942615578485, 8.31942615578485 }),
                accuracy);
    }

    @Test
    public void testHalbtrocken() { // currently M2S Values, not SAP
        assertEquals(2.0, interpolate(new double[] { 0, 3.54855412341278, 4.35655332156811, 5.0403620360792,
                5.56104371720063, 5.8858664928928, 6.0785496349971, 6.36326266675515, 6.36326266675515 }, 7.62407),
                accuracy);
    }

    @Test
    public void testHalbtrockenInverse() { // currently M2S Values, not SAP
        assertEquals(7.62407, interpolate(2.0, new double[] { 0, 3.54855412341278, 4.35655332156811, 5.0403620360792,
                5.56104371720063, 5.8858664928928, 6.0785496349971, 6.36326266675515, 6.36326266675515 }),
                accuracy);
    }

    // for a given implied wind, we expect to get back the velocity prediction for the whole course. The total Course
    // Length can be divided with the prediction to get the Allowance in hours.
    private double interpolate(double[] yn, double input) {
        final CubicSpline interpolator = CubicSpline.interpolateBoundariesSorted(xn, yn,
                SplineBoundaryCondition.ParabolicallyTerminated, /* leftBoundary */ 0,
                SplineBoundaryCondition.ParabolicallyTerminated, /* rightBoundary */ 0);
        return totalCourseLengthInNauticalMiles / interpolator.interpolate(input);
    }

    private double interpolate(double durationInHours, double[] yn) {
        final CubicSpline interpolator = CubicSpline.interpolateBoundariesSorted(xn, yn,
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
        final Constant averageSpeedInKnots = new Constant(totalCourseLengthInNauticalMiles / durationInHours);
        final NewtonRaphsonSolver newtonSolver = new NewtonRaphsonSolver(0.0000000001);
        final UnivariateDifferentiableFunction targetZeroFunction = FunctionUtils.add(splineFunction,
                FunctionUtils.multiply((UnivariateDifferentiableFunction) averageSpeedInKnots,
                        (UnivariateDifferentiableFunction) new Constant(-1)));
        return newtonSolver.solve(1000, targetZeroFunction, 10 /* knots of true wind speed */);
    }
}
