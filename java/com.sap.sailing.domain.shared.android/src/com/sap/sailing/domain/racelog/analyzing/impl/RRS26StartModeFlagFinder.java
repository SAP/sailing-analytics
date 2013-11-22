package com.sap.sailing.domain.racelog.analyzing.impl;

import java.util.List;
import java.util.Arrays;

import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;

public class RRS26StartModeFlagFinder extends RaceLogAnalyzer<Flags> {

    private final static List<Flags> defaultStartModeFlags = Arrays.asList(Flags.PAPA, Flags.ZULU, Flags.BLACK, Flags.INDIA);
    
    private final RacingProcedureTypeAnalyzer procedureAnalyzer;
    private final List<Flags> startModeFlags;

    /**
     * Searches for the start mode flag of a RRS26 race.
     * 
     * @param procedureAnalyzer
     *            to be used to ensure a RRS26 race. Must operate on the same race log. Otherwise a
     *            {@link IllegalArgumentException} is thrown.
     */
    public RRS26StartModeFlagFinder(RacingProcedureTypeAnalyzer procedureAnalyzer, RaceLog raceLog) {
        this(procedureAnalyzer, raceLog, defaultStartModeFlags);
    }
    
    public RRS26StartModeFlagFinder(RacingProcedureTypeAnalyzer procedureAnalyzer, RaceLog raceLog, List<Flags> startModeFlags) {
        super(raceLog);
        if (raceLog != procedureAnalyzer.getRaceLog()) {
            throw new IllegalArgumentException("Both analyzers must operate on the same race log.");
        }
        this.procedureAnalyzer = procedureAnalyzer;
        this.startModeFlags = startModeFlags;
    }

    @Override
    protected Flags performAnalysis() {
        RacingProcedureType type = procedureAnalyzer.analyze();
        if (RacingProcedureType.RRS26.equals(type)) {
            for (RaceLogEvent event : getPassEventsDescending()) {
                if (event instanceof RaceLogFlagEvent) {
                    RaceLogFlagEvent flagEvent = (RaceLogFlagEvent) event;
                    if (isStartModeFlagEvent(flagEvent)) {
                        return flagEvent.getUpperFlag();
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isStartModeFlagEvent(RaceLogFlagEvent event) {
        // no matter if displayed or removed
        if (event.getLowerFlag().equals(Flags.NONE)) {
            return startModeFlags.contains(event.getUpperFlag());
        }
        return false;
    }
    
    

}
