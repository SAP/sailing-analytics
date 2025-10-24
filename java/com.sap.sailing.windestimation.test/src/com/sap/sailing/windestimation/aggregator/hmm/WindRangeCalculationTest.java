package com.sap.sailing.windestimation.aggregator.hmm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.windestimation.aggregator.hmm.WindCourseRange.CombinationModeOnViolation;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindRangeCalculationTest {

    private static final double TOLERANCE = 0.0001;

    @Test
    public void testWithinRangeCalculation() {
        WindCourseRange range = new WindCourseRange(20, 10);
        assertEquals(range.isWindCourseWithinRange(20), true);
        assertEquals(range.isWindCourseWithinRange(25), true);
        assertEquals(range.isWindCourseWithinRange(30), true);
        assertEquals(range.isWindCourseWithinRange(19), false);
        assertEquals(range.isWindCourseWithinRange(200), false);
        assertEquals(range.isWindCourseWithinRange(31), false);

        range = new WindCourseRange(350, 20);
        assertEquals(range.isWindCourseWithinRange(350), true);
        assertEquals(range.isWindCourseWithinRange(0), true);
        assertEquals(range.isWindCourseWithinRange(10), true);
        assertEquals(range.isWindCourseWithinRange(349), false);
        assertEquals(range.isWindCourseWithinRange(180), false);
        assertEquals(range.isWindCourseWithinRange(11), false);

        range = new WindCourseRange(340, 20);
        assertEquals(range.isWindCourseWithinRange(0), true);
        assertEquals(range.isWindCourseWithinRange(1), false);
        assertEquals(range.isWindCourseWithinRange(339), false);
        assertEquals(range.isWindCourseWithinRange(180), false);

        range = new WindCourseRange(340, 350);
        assertEquals(range.isWindCourseWithinRange(341), true);
        assertEquals(range.isWindCourseWithinRange(330), true);
        assertEquals(range.isWindCourseWithinRange(331), false);
        assertEquals(range.isWindCourseWithinRange(339), false);
    }

    @Test
    public void testWindRangeIntersectionCalculation() {
        WindCourseRange range = new WindCourseRange(20, 15);
        WindCourseRange other = new WindCourseRange(10, 20);
        IntersectedWindRange intersect = range.intersect(other, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect.getFromPortside(), 20, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 10, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        IntersectedWindRange intersect2 = other.intersect(range, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect, intersect2);

        range = new WindCourseRange(340, 15);
        other = new WindCourseRange(350, 20);
        intersect = range.intersect(other, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect.getFromPortside(), 350, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 5, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        intersect2 = other.intersect(range, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect, intersect2);

        range = new WindCourseRange(340, 30);
        other = new WindCourseRange(10, 20);
        intersect = range.intersect(other, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect.getFromPortside(), 10, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 0, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        intersect2 = other.intersect(range, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect, intersect2);

        range = new WindCourseRange(340, 30);
        other = new WindCourseRange(330, 20);
        intersect = range.intersect(other, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect.getFromPortside(), 340, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 10, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        intersect2 = other.intersect(range, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect, intersect2);
    }

    @Test
    public void testWindRangeIntersectionCalculationWithViolations() {
        WindCourseRange range = new WindCourseRange(20, 15);
        WindCourseRange other = new WindCourseRange(10, 4);
        IntersectedWindRange intersect = range.intersect(other, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect.getFromPortside(), 14, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 6, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 6, TOLERANCE);
        IntersectedWindRange intersect2 = other.intersect(range, CombinationModeOnViolation.INTERSECTION);
        assertEquals(intersect, intersect2);
    }
    
    @Test
    public void testWindRangeExpansionCalculation() {
        WindCourseRange range = new WindCourseRange(20, 15);
        WindCourseRange other = new WindCourseRange(10, 20);
        IntersectedWindRange intersect = range.intersect(other, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect.getFromPortside(), 10, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 25, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        IntersectedWindRange intersect2 = other.intersect(range, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect, intersect2);

        range = new WindCourseRange(340, 15);
        other = new WindCourseRange(350, 20);
        intersect = range.intersect(other, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect.getFromPortside(), 340, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 30, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        intersect2 = other.intersect(range, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect, intersect2);

        range = new WindCourseRange(340, 30);
        other = new WindCourseRange(10, 20);
        intersect = range.intersect(other, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect.getFromPortside(), 340, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 50, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        intersect2 = other.intersect(range, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect, intersect2);

        range = new WindCourseRange(340, 30);
        other = new WindCourseRange(330, 20);
        intersect = range.intersect(other, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect.getFromPortside(), 330, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 40, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 0, TOLERANCE);
        intersect2 = other.intersect(range, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect, intersect2);
    }

    @Test
    public void testWindRangeExpansionCalculationWithViolations() {
        WindCourseRange range = new WindCourseRange(20, 15);
        WindCourseRange other = new WindCourseRange(10, 4);
        IntersectedWindRange intersect = range.intersect(other, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect.getFromPortside(), 10, TOLERANCE);
        assertEquals(intersect.getAngleTowardStarboard(), 25, TOLERANCE);
        assertEquals(intersect.getViolationRange(), 6, TOLERANCE);
        IntersectedWindRange intersect2 = other.intersect(range, CombinationModeOnViolation.EXPANSION);
        assertEquals(intersect, intersect2);
    }

    @Test
    public void testWindRangeInversion() {
        WindCourseRange range = new WindCourseRange(10, 15);
        WindCourseRange invert = range.invert();
        assertEquals(invert.getFromPortside(), 25, TOLERANCE);
        assertEquals(invert.getAngleTowardStarboard(), 345, TOLERANCE);
        assertEquals(invert.invert(), range);
    }

}
