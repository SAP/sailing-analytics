package com.sap.sse.common;

import com.sap.sse.common.impl.BearingChangeAnalyzerImpl;

/**
 * Helps analyze a change of a course over ground / {@link Bearing} and in particular can judge whether
 * such a course change passed over a certain other course over ground / {@link Bearing}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface BearingChangeAnalyzer {
    BearingChangeAnalyzer INSTANCE = new BearingChangeAnalyzerImpl();
    
    /**
     * Determines whether during a maneuver a certain course was reached and crossed. When used with the wind direction,
     * this can tell whether a boat tacked or jibed.
     * 
     * @param totalCourseChangeInDegrees
     *            tells how far and to which direction the course was changed. Positive values mean a change to
     *            {@link NauticalSide#STARBOARD}, negative values to {@link NauticalSide#PORT}. This is required to
     *            understand over which side the competitor reached the new course from the old course; if could have
     *            been either way. Example: if the <code>courseBeforeManeuver</code> was 355deg and the
     *            <code>courseAfterManeuver</code> was 005deg then if the <code>totalCourseChangeInDegrees</code> was
     *            positive then the competitor passed the 000deg course, whereas had the
     *            <code>totalCourseChangeInDegrees</code> been negative and less than 370deg then the competitor would
     *            have passed the 180deg course but not the 000deg course.
     * @return the number of times that the course identified by
     *         <code>wasThisCourseReachedAndCrossedDuringManeuver</code> was crossed; <code>0</code> means it was not
     *         crossed; any positive number tells how many times it was crossed, otherwise. Numbers greater than
     *         <code>1</code> require <code>totalCourseChangeInDegrees</code> to be greater than 360 degrees.
     * 
     */
    int didPass(Bearing courseBeforeManeuver, double totalCourseChangeInDegrees,
            Bearing courseAfterManeuver, Bearing wasThisCourseReachedAndCrossedDuringManeuver);
}
