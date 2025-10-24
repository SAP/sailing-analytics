package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.tracking.impl.CourseChangeCalculator;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class CourseChangeCalculatorTest {
    private CourseChangeCalculator ccc;

    @BeforeEach
    public void setUp() {
        ccc = new CourseChangeCalculator();
    }
    
    @Test
    public void testSimpleTurn() {
        assertEquals(123, getTotalCourseChange(0, 123, 122, NauticalSide.STARBOARD), 0.000001);
    }

    @Test
    public void testSlightBackTurn() {
        assertEquals(123, getTotalCourseChange(0, 123, 124, NauticalSide.STARBOARD), 0.000001);
    }
    
    @Test
    public void testHeavyBackTurnThatWorkedTheOtherWay() {
        assertEquals(350, getTotalCourseChange(0, 350, 10, NauticalSide.STARBOARD), 0.000001);
    }
    
    @Test
    public void testMoreThan360() {
        assertEquals(450, getTotalCourseChange(0, 90, 420, NauticalSide.STARBOARD), 0.000001);
    }

    @Test
    public void testMoreThan360WithSlightBackSpin() {
        assertEquals(440, getTotalCourseChange(0, 80, 450, NauticalSide.STARBOARD), 0.000001);
    }

    @Test
    public void testMoreThan360WithHeavyBackSpinThatWorksTheOtherWay() {
        assertEquals(450+(340-(450%360)), getTotalCourseChange(0, 340, 450, NauticalSide.STARBOARD), 0.000001);
    }

    @Test
    public void testToPortMoreThan360WithSlightBackSpin() {
        assertEquals(-440, getTotalCourseChange(0, 280, -450, NauticalSide.PORT), 0.000001);
    }

    private double getTotalCourseChange(double courseBeforeManeuverInDegrees, double currentCourseInDegrees,
            double courseChangeInDegreesSoFar, NauticalSide maneuverDirection) {
        return ccc.getTotalCourseChange(new DegreeBearingImpl(courseBeforeManeuverInDegrees), new DegreeBearingImpl(currentCourseInDegrees),
                courseChangeInDegreesSoFar, maneuverDirection);
    }
}
