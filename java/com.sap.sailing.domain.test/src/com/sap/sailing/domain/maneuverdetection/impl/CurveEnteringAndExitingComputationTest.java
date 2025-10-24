package com.sap.sailing.domain.maneuverdetection.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.tracking.SpeedWithBearingStep;
import com.sap.sailing.domain.tracking.SpeedWithBearingStepsIterable;
import com.sap.sailing.domain.tracking.impl.SpeedWithBearingStepImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests the computation of maneuver boundaries implemented in {@link ManeuverDetectorImpl}.
 * 
 * @author Vladislav Chumak (D069712)
 * @see com.sap.sailing.domain.tracking.Maneuver
 *
 */
public class CurveEnteringAndExitingComputationTest {

    private static final double maxDeltaForDouble = 0.000000000001;

    private static final double MAX_ABS_COURSE_CHANGE_PER_SECOND_FOR_STABLE_BEARING_ANALYSIS = 1;

    private final TimePoint referenceTimePoint = new MillisecondsTimePoint(
            Date.from(LocalDateTime.of(2017, 10, 26, 10, 0, 0).toInstant(ZoneOffset.UTC)));

    @Test
    public void testMainCurveSearch() {
        ManeuverDetectorImpl maneuverDetector = new ManeuverDetectorImpl();
        // Test that bearing steps with continuous course change into the target direction wraps the whole time range of
        // analyzed steps.
        SpeedWithBearingStepsIterable steps = constructStepsWithBearings(false, 0, 1, 3, 9, 10, 12);
        ManeuverMainCurveDetailsWithBearingSteps mainCurve = maneuverDetector.computeManeuverMainCurve(steps,
                NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(0), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(5), mainCurve.getTimePointAfter());
        assertEquals(constructTimePoint(2.5), mainCurve.getTimePoint());
        assertEquals(12, mainCurve.getDirectionChangeInDegrees(), maxDeltaForDouble);
        assertEquals(6.0, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);
        assertEquals(1, mainCurve.getLowestSpeed().getKnots(), maxDeltaForDouble);

        // Test that outer bearing steps with opposite direction to the target course get cut off.
        steps = constructStepsWithBearings(false, 0, 359, 3, 9, 10, 9);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(1), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(4), mainCurve.getTimePointAfter());
        assertEquals(constructTimePoint(2.5), mainCurve.getTimePoint());
        assertEquals(11, mainCurve.getDirectionChangeInDegrees(), maxDeltaForDouble);
        assertEquals(6.0, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);
        assertEquals(2, mainCurve.getLowestSpeed().getKnots(), maxDeltaForDouble);

        // Test that outer bearing steps with major direction to the target course do not get cut off due to short
        // deviations from target course within the main curve
        steps = constructStepsWithBearings(true, 0, 10, 5, 15, 10, 20);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(0), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(5), mainCurve.getTimePointAfter());
        assertEquals(constructTimePoint(0.5), mainCurve.getTimePoint());
        assertEquals(20, mainCurve.getDirectionChangeInDegrees(), maxDeltaForDouble);
        assertEquals(10.0, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);

        // Test that the returned maneuver main curve is null, if the maneuver direction does not match the target
        // direction and
        // the code inside does not crash.
        steps = constructStepsWithBearings(true, 0, 359, 358, 357, 356, 355);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(null, mainCurve);

        // Test that the returned maneuver main curve is null, if the boat is not turning at all and
        // the code inside does not crash.
        steps = constructStepsWithBearings(true, 0, 0, 0, 0, 0, 0);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(null, mainCurve);

        // Test that the outer bearing steps with unstable course changes without contribution to the maximal total
        // course change get cut off and the inner bearing steps which do contribute to
        // the maximal course change are kept.
        steps = constructStepsWithBearings(true, 0, 1, 0, 2, 1, 2, 6, 12, 16, 17, 16, 18, 17, 18);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(2), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(11), mainCurve.getTimePointAfter());
        assertEquals(constructTimePoint(6.5), mainCurve.getTimePoint());
        assertEquals(18, mainCurve.getDirectionChangeInDegrees(), maxDeltaForDouble);
        assertEquals(6.0, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);

        // Test that the time point with the highest turn rate in the wrong direction gets cut off and is not picked as
        // maximal turning rate.
        steps = constructStepsWithBearings(true, 0, 340, 0, 2, 1, 2, 6, 12, 16, 17, 16, 18, 17, 18);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(1), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(11), mainCurve.getTimePointAfter());
        assertEquals(constructTimePoint(1.5), mainCurve.getTimePoint());
        assertEquals(38, mainCurve.getDirectionChangeInDegrees(), maxDeltaForDouble);
        assertEquals(20.0, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);

        // Test that the time point with the highest turn rate gets cut off and is not picked as
        // maximal turning rate, because it its position is before the actual main curve.
        steps = constructStepsWithBearings(true, 0, 340, 350, 345, 340, 335, 340, 345, 350, 359);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(5), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(9), mainCurve.getTimePointAfter());
        assertEquals(constructTimePoint(8.5), mainCurve.getTimePoint());
        assertEquals(24, mainCurve.getDirectionChangeInDegrees(), maxDeltaForDouble);
        assertEquals(9.0, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);

        // test that when there are only small direction changes at the end they are cut off
        steps = constructStepsWithBearings(true, 0, 1, 3, 9, 10, 10.0001, 10.0002, 10.0003, 10.0004, 10.0005, 10.0006,
                10.0007, 10.0008, 10.0009);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(0), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(4), mainCurve.getTimePointAfter());

        steps = constructStepsWithBearings(true, 359.9985, 359.9986, 359.9987, 359.9988, 359.9989, 359.9989, 359.999, 0,
                1, 3, 9, 10);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(7), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(11), mainCurve.getTimePointAfter());

        // even when the small changes aggregate to a total change exceeding the threshold, they all do not belong to
        // the maneuver
        steps = constructStepsWithBearings(true, 0, 1, 3, 9, 10, 10.0001, 10.0002, 10.0003, 10.0004, 10.0005, 10.0006,
                10.0007, 10.0008, 10.0009, 10.001);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(0), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(4), mainCurve.getTimePointAfter());

        steps = constructStepsWithBearings(true, 359.9979, 359.9980, 359.9981, 359.9982, 359.9983, 359.9984, 359.9985,
                359.9986, 359.9987, 359.9988, 359.9989, 359.9989, 359.999, 0, 1, 3, 9, 10);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(13), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(17), mainCurve.getTimePointAfter());

        steps = constructStepsWithBearings(true, 358, 358.0001, 359.9979, 359.9980, 0, 1, 3, 9, 9, 0001, 11, 11.0001);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(1), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(10), mainCurve.getTimePointAfter());

        // check that the extreme course change towards opposite direction does not affect main curve calculation
        steps = constructStepsWithBearings(true, 0, 10, 0, 70, 350);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(2), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(3), mainCurve.getTimePointAfter());
        assertEquals(2, mainCurve.getLowestSpeed().getKnots(), maxDeltaForDouble);
        assertEquals(constructTimePoint(2.5), mainCurve.getTimePoint());
        assertEquals(70, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);

        steps = constructStepsWithBearings(false, 0, 100, 180, 0, 150);
        mainCurve = maneuverDetector.computeManeuverMainCurve(steps, NauticalSide.STARBOARD);
        assertEquals(constructTimePoint(0), mainCurve.getTimePointBefore());
        assertEquals(constructTimePoint(2), mainCurve.getTimePointAfter());
        assertEquals(3, mainCurve.getLowestSpeed().getKnots(), maxDeltaForDouble);
        assertEquals(constructTimePoint(0.5), mainCurve.getTimePoint());
        assertEquals(100, mainCurve.getMaxTurningRateInDegreesPerSecond(), maxDeltaForDouble);
    }

    @Test
    public void testSpeedMaximaSearch() {
        ManeuverDetectorImpl maneuverDetector = new ManeuverDetectorImpl();
        // Test with time forward call that speed steps with continuous speed increase wraps the whole time range of
        // analyzed steps.
        SpeedWithBearingStepsIterable steps = constructStepsWithSpeeds(0, 1, 3, 9, 10, 12);
        ManeuverCurveBoundaryExtension extension = maneuverDetector.findSpeedMaximum(steps, false, null);
        assertEquals(constructTimePoint(5), extension.getExtensionTimePoint());
        assertEquals(5, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

        // Test with time backward call that speed steps with continuous speed decrease wraps the whole time range of
        // analyzed steps.
        steps = constructStepsWithSpeeds(12, 10, 9, 3, 1, 0);
        extension = maneuverDetector.findSpeedMaximum(steps, true, null);
        assertEquals(constructTimePoint(0), extension.getExtensionTimePoint());
        assertEquals(5, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

        // Test with time forward call that the last step with decreasing speed gets removed
        steps = constructStepsWithSpeeds(0, 1, 3, 9, 10, 9);
        extension = maneuverDetector.findSpeedMaximum(steps, false, null);
        assertEquals(constructTimePoint(4), extension.getExtensionTimePoint());
        assertEquals(4, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

        // Test with time backward call that the last step with decreasing speed gets removed
        steps = constructStepsWithSpeeds(9, 10, 9, 3, 1, 0);
        extension = maneuverDetector.findSpeedMaximum(steps, true, null);
        assertEquals(constructTimePoint(1), extension.getExtensionTimePoint());
        assertEquals(4, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

        // Test with time forward call with the limit of global maximum search such that the value is found in the
        // global maximum area
        steps = constructStepsWithSpeeds(0, 1, 3, 10, 9, 20);
        TimePoint globalMaximumSearchUntilTimePoint = constructTimePoint(2);
        extension = maneuverDetector.findSpeedMaximum(steps, false, globalMaximumSearchUntilTimePoint);
        assertEquals(constructTimePoint(3), extension.getExtensionTimePoint());
        assertEquals(3, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

        // Test with time backward call with the limit of global maximum search such that the value is found in the
        // global maximum area
        steps = constructStepsWithSpeeds(20, 9, 10, 3, 1, 0);
        extension = maneuverDetector.findSpeedMaximum(steps, true, globalMaximumSearchUntilTimePoint);
        assertEquals(constructTimePoint(2), extension.getExtensionTimePoint());
        assertEquals(3, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

        // Another test with time forward call with the limit of global maximum search such that the value is found in
        // the local maximum area
        steps = constructStepsWithSpeeds(0, 1, 3, 10, 20, 9, 50);
        extension = maneuverDetector.findSpeedMaximum(steps, false, globalMaximumSearchUntilTimePoint);
        assertEquals(constructTimePoint(4), extension.getExtensionTimePoint());
        assertEquals(4, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

        // Another test with time backward call with the limit of global maximum search such that the value is found in
        // the local maximum area
        steps = constructStepsWithSpeeds(50, 9, 20, 10, 3, 1, 0);
        globalMaximumSearchUntilTimePoint = constructTimePoint(4);
        extension = maneuverDetector.findSpeedMaximum(steps, true, globalMaximumSearchUntilTimePoint);
        assertEquals(constructTimePoint(2), extension.getExtensionTimePoint());
        assertEquals(4, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);

    }

    @Test
    public void testStableBearingSearch() {
        ManeuverDetectorImpl maneuverDetector = new ManeuverDetectorImpl();
        // Test time forward
        SpeedWithBearingStepsIterable steps = constructStepsWithBearings(true, 0, 2, 5, 4, 10, 12);
        ManeuverCurveBoundaryExtension extension = maneuverDetector.findStableBearingWithMaxAbsCourseChangeSpeed(steps,
                false, MAX_ABS_COURSE_CHANGE_PER_SECOND_FOR_STABLE_BEARING_ANALYSIS);
        assertEquals(constructTimePoint(2), extension.getExtensionTimePoint());
        assertEquals(5, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);
        // Test time backward
        steps = constructStepsWithBearings(true, 12, 10, 4, 5, 2, 0);
        extension = maneuverDetector.findStableBearingWithMaxAbsCourseChangeSpeed(steps, true,
                MAX_ABS_COURSE_CHANGE_PER_SECOND_FOR_STABLE_BEARING_ANALYSIS);
        assertEquals(constructTimePoint(3), extension.getExtensionTimePoint());
        assertEquals(-5, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);
        // Test time forward with a list which contains only unstable bearings such that it delivers last time point
        steps = maneuverDetector.getSpeedWithBearingStepsWithinTimeRange(steps, constructTimePoint(0),
                constructTimePoint(2));
        extension = maneuverDetector.findStableBearingWithMaxAbsCourseChangeSpeed(steps, false,
                MAX_ABS_COURSE_CHANGE_PER_SECOND_FOR_STABLE_BEARING_ANALYSIS);
        assertEquals(constructTimePoint(2), extension.getExtensionTimePoint());
        assertEquals(-8, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);
        // Test time backward with a list which contains only unstable bearings such that it delivers first time point
        steps = maneuverDetector.getSpeedWithBearingStepsWithinTimeRange(steps, constructTimePoint(0),
                constructTimePoint(2));
        extension = maneuverDetector.findStableBearingWithMaxAbsCourseChangeSpeed(steps, true,
                MAX_ABS_COURSE_CHANGE_PER_SECOND_FOR_STABLE_BEARING_ANALYSIS);
        assertEquals(constructTimePoint(0), extension.getExtensionTimePoint());
        assertEquals(-8, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);
        // Test time forward with a list which contains only one element
        steps = maneuverDetector.getSpeedWithBearingStepsWithinTimeRange(steps, constructTimePoint(0),
                constructTimePoint(0));
        extension = maneuverDetector.findStableBearingWithMaxAbsCourseChangeSpeed(steps, false,
                MAX_ABS_COURSE_CHANGE_PER_SECOND_FOR_STABLE_BEARING_ANALYSIS);
        assertEquals(constructTimePoint(0), extension.getExtensionTimePoint());
        assertEquals(0, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);
        // Test time backward with a list which contains only one element
        extension = maneuverDetector.findStableBearingWithMaxAbsCourseChangeSpeed(steps, true,
                MAX_ABS_COURSE_CHANGE_PER_SECOND_FOR_STABLE_BEARING_ANALYSIS);
        assertEquals(constructTimePoint(0), extension.getExtensionTimePoint());
        assertEquals(0, extension.getCourseChangeInDegreesWithinExtensionArea(), maxDeltaForDouble);
    }

    private SpeedWithBearingStepsIterable constructStepsWithBearings(boolean speedIncreasing,
            double... bearingsInDegrees) {
        List<SpeedWithBearingStep> steps = new ArrayList<>(bearingsInDegrees.length);
        SpeedWithBearingStep previousStep = null;
        for (int i = 0; i < bearingsInDegrees.length; i++) {
            SpeedWithBearingStep step = constructStep(i, speedIncreasing ? i : bearingsInDegrees.length - i,
                    bearingsInDegrees[i], previousStep);
            steps.add(step);
            previousStep = step;

        }
        return new SpeedWithBearingStepsIterable(steps);
    }

    private SpeedWithBearingStepsIterable constructStepsWithSpeeds(double... speedsInKnots) {
        List<SpeedWithBearingStep> steps = new ArrayList<>(speedsInKnots.length);
        SpeedWithBearingStep previousStep = null;
        for (int i = 0; i < speedsInKnots.length; i++) {
            SpeedWithBearingStep step = constructStep(i, speedsInKnots[i], i, previousStep);
            steps.add(step);
            previousStep = step;

        }
        return new SpeedWithBearingStepsIterable(steps);
    }

    private SpeedWithBearingStep constructStep(double secondsAfterRefenceTimePoint, double speedInKnots,
            double bearingInDegrees, SpeedWithBearingStep previousStep) {
        Bearing bearing = new DegreeBearingImpl(bearingInDegrees);
        SpeedWithBearing speedWithBearing = new KnotSpeedWithBearingImpl(speedInKnots, bearing);
        TimePoint timePoint = constructTimePoint(secondsAfterRefenceTimePoint);
        double courseChangeInDegrees = previousStep == null ? 0.0
                : previousStep.getSpeedWithBearing().getBearing().getDifferenceTo(bearing).getDegrees();
        double turningRateInDegreesPerSecond = previousStep == null ? 0
                : Math.abs(courseChangeInDegrees / previousStep.getTimePoint().until(timePoint).asSeconds());
        return new SpeedWithBearingStepImpl(timePoint, speedWithBearing, courseChangeInDegrees,
                turningRateInDegreesPerSecond);
    }

    private TimePoint constructTimePoint(double secondsAfterRefenceTimePoint) {
        return referenceTimePoint.plus((long) (secondsAfterRefenceTimePoint * 1000));
    }

}
