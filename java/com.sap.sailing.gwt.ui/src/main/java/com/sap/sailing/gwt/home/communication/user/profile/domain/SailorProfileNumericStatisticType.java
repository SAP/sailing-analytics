package com.sap.sailing.gwt.home.communication.user.profile.domain;

import com.sap.sailing.gwt.ui.raceboard.RaceBoardModes;

/** current types of statistics currently supported in the SailorProfiles and the corresponding backend service */
public enum SailorProfileNumericStatisticType {
    MAX_SPEED(StatisticType.HIGHEST_IS_BEST, RaceBoardModes.PLAYER, false, true, false), 
    BEST_DISTANCE_TO_START(StatisticType.LOWEST_IS_BEST, RaceBoardModes.START_ANALYSIS, true, true, false), 
    BEST_STARTLINE_SPEED(StatisticType.HIGHEST_IS_BEST, RaceBoardModes.START_ANALYSIS, false, true, false), 
    AVERAGE_STARTLINE_DISTANCE(StatisticType.AVERAGE, null, true, true, true),
    
    AVERAGE_STARTLINE_DISTANCE_WITH_VALIDATION(StatisticType.AVERAGE, null, true, false, true),
    AVERAGE_VELOCITY_MADE_GOOD_UPWIND_LEG(StatisticType.AVERAGE, null, false, false, true),
    AVERAGE_VELOCITY_MADE_GOOD_DOWNWIND_LEG(StatisticType.AVERAGE, null, false, false, true),
    AVERAGE_SPEED_OVER_GROUND_UPWIND_LEG(StatisticType.AVERAGE, null, false, false, true),
    AVERAGE_SPEED_OVER_GROUND_DOWNWIND_LEG(StatisticType.AVERAGE, null, false, false, true);
    
    private StatisticType type;
    private RaceBoardModes mode;

    private boolean lowerIsBetter;
    private boolean showInStatisticsTables;
    private boolean showInRadarChart;

    SailorProfileNumericStatisticType(StatisticType type, RaceBoardModes mode, 
            boolean lowerIsBetter, boolean showInStatisticsTables, boolean showInRadarChart) {
        this.type = type;
        this.mode = mode;
        
        this.lowerIsBetter = lowerIsBetter;
        this.showInStatisticsTables = showInStatisticsTables;
        this.showInRadarChart = showInRadarChart;
    }

    public StatisticType getAggregationType() {
        return type;
    }

    public RaceBoardModes getPlayerMode() {
        return this.mode;
    }
    
    public boolean isLowerIsBetter() {  
        return lowerIsBetter;
    }
    
    public boolean isShowInStatisticsTables() {
        return showInStatisticsTables;
    }
    
    public boolean isShowInRadarChart() {
        return showInRadarChart;
    }

    public static enum StatisticType {
        LOWEST_IS_BEST, HIGHEST_IS_BEST, AVERAGE
    }

}
