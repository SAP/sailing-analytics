package com.sap.sailing.datamining.impl.data;

import java.util.Calendar;

import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.tracking.TrackedRace;

public class TrackedRaceWithContext implements HasTrackedRaceContext {

    private final Regatta regatta;
    private final Fleet fleet;
    private final TrackedRace trackedRace;
    private Integer year;
    private boolean yearHasBeenInitialized;

    public TrackedRaceWithContext(Regatta regatta, Fleet fleet, TrackedRace trackedRace) {
        this.regatta = regatta;
        this.fleet = fleet;
        this.trackedRace = trackedRace;
    }
    
    @Override
    public Regatta getRegatta() {
        return regatta;
    }
    
    @Override
    public CourseArea getCourseArea() {
        return getRegatta().getDefaultCourseArea();
    }
    
    @Override
    public BoatClass getBoatClass() {
        return getRegatta().getBoatClass();
    }
    
    @Override
    public TrackedRace getTrackedRace() {
        return trackedRace;
    }

    @Override
    public Fleet getFleet() {
        return fleet;
    }
    
    @Override
    public RaceDefinition getRace() {
        return getTrackedRace().getRace();
    }

    @Override
    public Integer getYear() {
        if (!yearHasBeenInitialized) {
            year = calculateYear();
            yearHasBeenInitialized = true;
        }
        return year;
    }

    private Integer calculateYear() {
        TimePoint startOfRace = getTrackedRace().getStartOfRace();
        TimePoint time = startOfRace != null ? startOfRace : getTrackedRace().getStartOfTracking();
        if (time == null) {
            year = 0;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time.asDate());
        return calendar.get(Calendar.YEAR);
    }

}