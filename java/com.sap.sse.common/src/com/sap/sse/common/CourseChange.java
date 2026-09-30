package com.sap.sse.common;

/**
 * A course change represents a change of the course over ground as well as the speed over ground.
 * {@link SpeedWithBearing#applyCourseChange(CourseChange) Applying} this {@link CourseChange} object to a
 * {@link SpeedWithBearing} results in the {@link SpeedWithBearing} as it was after this course change.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface CourseChange {
    double getCourseChangeInDegrees();
    
    double getSpeedChangeInKnots();
}
