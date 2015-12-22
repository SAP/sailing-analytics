package com.sap.sailing.gwt.ui.raceboard;

import com.sap.sse.common.settings.AbstractSettings;
import com.sap.sse.gwt.shared.GwtHttpRequestUtils;

/**
 * Represents the parameters for configuring the raceboard view
 * 
 * @author Frank
 *
 */
public class RaceBoardPerspectiveSettings extends AbstractSettings {
    private final boolean showLeaderboard;
    private final boolean showWindChart;
    private final boolean showCompetitorsChart;
    private final String activeCompetitorsFilterSetName;
    private final boolean canReplayDuringLiveRaces;
    private final boolean showViewStreamlets;
    private final boolean showViewStreamletColors;
    private final boolean showViewSimulation;

    public static final String PARAM_VIEW_MODE = "viewMode";
    public static final String PARAM_VIEW_SHOW_LEADERBOARD = "viewShowLeaderboard";
    public static final String PARAM_VIEW_SHOW_NAVIGATION_PANEL = "viewShowNavigationPanel";
    public static final String PARAM_VIEW_SHOW_WINDCHART = "viewShowWindChart";
    public static final String PARAM_VIEW_SHOW_COMPETITORSCHART = "viewShowCompetitorsChart";
    public static final String PARAM_VIEW_SHOW_MAPCONTROLS = "viewShowMapControls";
    public static final String PARAM_VIEW_SHOW_STREAMLETS = "viewShowStreamlets";
    public static final String PARAM_VIEW_SHOW_STREAMLET_COLORS = "viewShowStreamletColors";
    public static final String PARAM_VIEW_SHOW_SIMULATION = "viewShowSimulation";
    public static final String PARAM_VIEW_COMPETITOR_FILTER = "viewCompetitorFilter";
    public static final String PARAM_CAN_REPLAY_DURING_LIVE_RACES = "canReplayDuringLiveRaces";

    public RaceBoardPerspectiveSettings() {
        this(/* activeCompetitorsFilterSetName */null, /* showLeaderboard */true,
        /* showWindChart */false, /* showCompetitorsChart */false, /* showViewStreamlets */false, /* showViewStreamletColors */
        false, /* showViewSimulation */false, /* canReplayDuringLiveRaces */false);
    }

    public RaceBoardPerspectiveSettings(String activeCompetitorsFilterSetName, boolean showLeaderboard,
            boolean showWindChart, boolean showCompetitorsChart, boolean showViewStreamlets,
            boolean showViewStreamletColors, boolean showViewSimulation, boolean canReplayDuringLiveRaces) {
        this.activeCompetitorsFilterSetName = activeCompetitorsFilterSetName;
        this.showLeaderboard = showLeaderboard;
        this.showWindChart = showWindChart;
        this.showCompetitorsChart = showCompetitorsChart;
        this.showViewStreamlets = showViewStreamlets;
        this.showViewStreamletColors = showViewStreamletColors;
        this.showViewSimulation = showViewSimulation;
        this.canReplayDuringLiveRaces = canReplayDuringLiveRaces;
    }

    public boolean isShowLeaderboard() {
        return showLeaderboard;
    }

    public boolean isShowWindChart() {
        return showWindChart;
    }

    public boolean isShowViewStreamlets() {
        return showViewStreamlets;
    }

    public boolean isShowViewStreamletColors() {
        return showViewStreamletColors;
    }

    public boolean isShowViewSimulation() {
        return showViewSimulation;
    }

    public boolean isShowCompetitorsChart() {
        return showCompetitorsChart;
    }

    public String getActiveCompetitorsFilterSetName() {
        return activeCompetitorsFilterSetName;
    }

    public boolean isCanReplayDuringLiveRaces() {
        return canReplayDuringLiveRaces;
    }

    public static RaceBoardPerspectiveSettings readSettingsFromURL() {
        final boolean showLeaderboard = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_LEADERBOARD, true /* default */);
        final boolean showWindChart = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_WINDCHART, false /* default */);
        final boolean showViewStreamlets = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_STREAMLETS, false /* default */);
        final boolean showViewStreamletColors = GwtHttpRequestUtils.getBooleanParameter(
                PARAM_VIEW_SHOW_STREAMLET_COLORS, false /* default */);
        final boolean showViewSimulation = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_SIMULATION, false /* default */);
        final boolean showCompetitorsChart = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_COMPETITORSCHART, false /* default */);
        String activeCompetitorsFilterSetName = GwtHttpRequestUtils.getStringParameter(PARAM_VIEW_COMPETITOR_FILTER, null /* default */);

        return new RaceBoardPerspectiveSettings(activeCompetitorsFilterSetName, showLeaderboard, showWindChart,
                showCompetitorsChart, showViewStreamlets, showViewStreamletColors, showViewSimulation, /* canReplayWhileLiveIsPossible */
                false);
    }
}
