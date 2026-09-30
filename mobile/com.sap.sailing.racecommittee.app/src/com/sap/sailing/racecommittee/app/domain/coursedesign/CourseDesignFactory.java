package com.sap.sailing.racecommittee.app.domain.coursedesign;

import com.sap.sse.common.Position;
import com.sap.sse.common.Bearing;

public interface CourseDesignFactory {
    CourseDesign createCourseDesign(Position startBoatPosition, Double windSpeed, Bearing windDirection,
            BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds, TargetTime targetTime);

}
