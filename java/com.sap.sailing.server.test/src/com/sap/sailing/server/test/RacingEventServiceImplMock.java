package com.sap.sailing.server.test;

import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.server.impl.RacingEventServiceImpl;





public class RacingEventServiceImplMock extends RacingEventServiceImpl{

    
    public RacingEventServiceImplMock() {
        super();
    }
    
    public Map<String, Event> getEventsByNameMap(){
        return eventsByName;
    }
    
    public Map<Event, Set<RaceTracker>> getRaceTrackersByEventMap(){
        return raceTrackersByEvent;
    }
    
    public Map<Object, RaceTracker> getRaceTrackersByIDMap(){
        return raceTrackersByID;
    }
    
    public Map<String, Event> getEventsByName(){
        return eventsByName;
    }
}
