package com.sap.sailing.domain.leaderboard;

import com.sap.sailing.domain.base.CourseArea;

public interface HasCourseAreas {
    Iterable<CourseArea> getCourseAreas();
    void addCourseAreaChangeListener(HasCourseAreasListener listener);
    void removeCourseAreaChangeListener(HasCourseAreasListener listener);
}
