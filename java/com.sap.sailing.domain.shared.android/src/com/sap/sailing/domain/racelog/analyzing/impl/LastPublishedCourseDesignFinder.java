package com.sap.sailing.domain.racelog.analyzing.impl;

import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.analyzing.impl.RaceLogAnalyzer;

public class LastPublishedCourseDesignFinder extends RaceLogAnalyzer<CourseBase> {

    public LastPublishedCourseDesignFinder(RaceLog raceLog) {
        super(raceLog);
    }

    @Override
    protected CourseBase performAnalyzation() {
        CourseBase lastCourseData = null;
        
        for (RaceLogEvent event : getAllEvents()) {
            if (event instanceof RaceLogCourseDesignChangedEvent) {
                RaceLogCourseDesignChangedEvent courseDesignEvent = (RaceLogCourseDesignChangedEvent) event;
                lastCourseData = courseDesignEvent.getCourseDesign();
            }
        }
        
        return lastCourseData;
    }

}
