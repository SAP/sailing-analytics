package com.sap.sailing.domain.leaderboard;

import com.sap.sailing.domain.base.CourseArea;

public interface HasCourseAreasListener {
    void courseAreasChanged(HasCourseAreas hasCourseAreas, Iterable<CourseArea> oldCourseAreas, Iterable<CourseArea> newCourseAreas);
}
