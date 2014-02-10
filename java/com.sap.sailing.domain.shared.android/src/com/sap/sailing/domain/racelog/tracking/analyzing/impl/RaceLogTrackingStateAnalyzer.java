package com.sap.sailing.domain.racelog.tracking.analyzing.impl;

import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.analyzing.impl.RaceLogAnalyzer;
import com.sap.sailing.domain.racelog.tracking.CreateRaceEvent;
import com.sap.sailing.domain.racelog.tracking.DenoteForTrackingEvent;
import com.sap.sailing.domain.racelog.tracking.RaceLogTrackingState;

public class RaceLogTrackingStateAnalyzer extends RaceLogAnalyzer<RaceLogTrackingState> {

    public RaceLogTrackingStateAnalyzer(RaceLog raceLog) {
        super(raceLog);
    }

    @Override
    protected RaceLogTrackingState performAnalysis() {
        for (RaceLogEvent event : getAllEventsDescending()) {
            if (event instanceof CreateRaceEvent) {
                return RaceLogTrackingState.TRACKING;
            } else if (event instanceof DenoteForTrackingEvent) {
                return RaceLogTrackingState.AWAITING_RACE_DEFINITION;
            }
        }
        return RaceLogTrackingState.NOT_A_RACELOG_TRACKED_RACE;
    }

}
