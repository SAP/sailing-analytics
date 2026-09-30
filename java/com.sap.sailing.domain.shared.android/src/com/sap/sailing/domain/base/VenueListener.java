package com.sap.sailing.domain.base;

public interface VenueListener {
    void courseAreaAdded(Venue venue, CourseArea courseArea);
    void courseAreaRemoved(Venue venue, CourseArea courseArea);
}
