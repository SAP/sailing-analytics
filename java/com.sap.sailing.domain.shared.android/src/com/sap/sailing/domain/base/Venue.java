package com.sap.sailing.domain.base;

import com.sap.sse.common.Named;
import com.sap.sse.common.Renamable;

/**
 * Describes the place where a sailing event with one or more regattas takes place.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface Venue extends Named, Renamable {
    void addListener(VenueListener listener);
    
    void removeListener(VenueListener listener);
    
    Iterable<CourseArea> getCourseAreas();
    
    void addCourseArea(CourseArea courseArea);
    
    void removeCourseArea(CourseArea courseArea);
}
