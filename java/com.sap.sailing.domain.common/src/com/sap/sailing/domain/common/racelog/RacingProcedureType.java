package com.sap.sailing.domain.common.racelog;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to identify a RacingProcedure's type.
 * 
 * When modifying these values also check res/preferences.xml of racecommittee.app!
 */
public enum RacingProcedureType {
    UNKNOWN("Unknown"),
    RRS26("Fix Line Start (RRS26)"),
    SWC("Sailing World Cup Start"),
    GateStart("Gate Start"),
    ESS("\"Extreme Sailing Series\"-Start"),
    BASIC("Basic Countdown Start"),
    LEAGUE("League Start"),
    RRS26_3MIN("Fix Line Start with 3min Sequence (RRS26/3)"),
    SWC_5MIN("Sailing World Cup Start with 5min Sequence (2025 and onwards)"),
    SWC_4MIN("Sailing World Cup Start with 4min Sequence (Kites, Surfers)");
    
    private String displayName;

    private RacingProcedureType(String displayName) {
        this.displayName = displayName;
    }

    @Override 
    public String toString() {
        return displayName;
    }

    public static RacingProcedureType[] validValues() {
        List<RacingProcedureType> validValues = new ArrayList<RacingProcedureType>();
        for (RacingProcedureType type : values()) {
            if (type != UNKNOWN) {
                validValues.add(type);
            }
        }
        return validValues.toArray(new RacingProcedureType[0]);
    }
}
