package com.sap.sailing.domain.racelog.analyzing.impl;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogWindFixEvent;
import com.sap.sailing.domain.tracking.Wind;

public class WindFixesFinder extends RaceLogAnalyzer<List<Wind>> {

    public WindFixesFinder(RaceLog raceLog) {
        super(raceLog);
    }

    @Override
    protected List<Wind> performAnalyzation() {
        List<Wind> windFixes = new ArrayList<Wind>();
        for (RaceLogEvent event : getAllEventsDescending()) {
            if (event instanceof RaceLogWindFixEvent) {
                RaceLogWindFixEvent windFixEvent = (RaceLogWindFixEvent) event;
                windFixes.add(windFixEvent.getWindFix());
            }
        }
        
        return windFixes;
    }

}
