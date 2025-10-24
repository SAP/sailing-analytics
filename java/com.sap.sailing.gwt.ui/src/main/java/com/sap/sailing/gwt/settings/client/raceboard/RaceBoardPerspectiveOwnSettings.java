package com.sap.sailing.gwt.settings.client.raceboard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.gwt.settings.client.settingtypes.DurationSetting;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.LongSetting;
import com.sap.sse.common.settings.generic.StringSetSetting;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.gwt.shared.GwtHttpRequestUtils;

/**
 * The settings of the raceboard perspective
 * @author Frank
 *
 */
public class RaceBoardPerspectiveOwnSettings extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = 5471954179434008459L;
    
    private transient BooleanSetting showLeaderboard;
    private transient BooleanSetting showWindChart;
    private transient BooleanSetting showCompetitorsChart;
    private transient StringSetting activeCompetitorsFilterSetName;
    private transient BooleanSetting canReplayDuringLiveRaces;
    private transient DurationSetting initialDurationAfterRaceStartInReplay;
    private transient StringSetting selectedCompetitor;
    private transient StringSetSetting selectedCompetitors;
    private transient BooleanSetting showTags;
    private transient BooleanSetting showManeuverTable;
    private transient StringSetting jumpToTag;
    private transient LongSetting zoomEnd;
    private transient LongSetting zoomStart;
    private transient BooleanSetting autoExpandPreSelectedRace;
    
    public static final String PARAM_VIEW_MODE = "viewMode";
    public static final String PARAM_VIEW_SHOW_LEADERBOARD = "viewShowLeaderboard";
    public static final String PARAM_VIEW_SHOW_WINDCHART = "viewShowWindChart";
    public static final String PARAM_VIEW_SHOW_COMPETITORSCHART = "viewShowCompetitorsChart";
    public static final String PARAM_VIEW_SHOW_MAPCONTROLS = "viewShowMapControls";
    public static final String PARAM_VIEW_COMPETITOR_FILTER = "viewCompetitorFilter";
    public static final String PARAM_VIEW_CHART_SUPPORT_ENABLED = "viewChartSupportEnabled";
    public static final String PARAM_CAN_REPLAY_DURING_LIVE_RACES = "canReplayDuringLiveRaces";
    public static final String PARAM_TIME_AFTER_RACE_START_AS_HOURS_COLON_MILLIS_COLON_SECONDS = "t";
    public static final String PARAM_SELECTED_COMPETITOR = "c";
    public static final String PARAM_SELECTED_COMPETITORS = "selectedCompetitors";
    public static final String PARAM_VIEW_SHOW_TAGS = "viewShowTags";
    public static final String PARAM_VIEW_SHOW_MANEUVER_TABLE = "viewShowManeuverTable";
    public static final String PARAM_JUMP_TO_TAG = TagDTO.TAG_URL_PARAMETER;
    public static final String PARAM_ZOOM_START = "zoomStart";
    public static final String PARAM_ZOOM_END = "zoomEnd";
    public static final String PARAM_AUTO_EXPAND_PRE_SELECTED_RACE = "autoExpandPreSelectedRace";


    public RaceBoardPerspectiveOwnSettings() {
        super();
    }

    public RaceBoardPerspectiveOwnSettings(Duration initialDurationAfterRaceStartInReplay) {
        this();
        this.initialDurationAfterRaceStartInReplay.setValue(initialDurationAfterRaceStartInReplay);
    }
    
    public static RaceBoardPerspectiveOwnSettings createDefaultWithCanReplayDuringLiveRaces(
            boolean canReplayDuringLiveRaces) {
        RaceBoardPerspectiveOwnSettings settings = new RaceBoardPerspectiveOwnSettings();
        settings.canReplayDuringLiveRaces.setValue(canReplayDuringLiveRaces);
        return settings;
    }

    @Override
    protected void addChildSettings() {
        this.showLeaderboard = new BooleanSetting("showLeaderboard", this, true);
        this.showWindChart = new BooleanSetting("showWindChart", this, false);
        this.showCompetitorsChart = new BooleanSetting("showCompetitorsChart", this, false);
        this.activeCompetitorsFilterSetName = new StringSetting("activeCompetitorsFilterSetName", this, null);
        this.canReplayDuringLiveRaces = new BooleanSetting("canReplayDuringLiveRaces", this, false);
        this.initialDurationAfterRaceStartInReplay = new DurationSetting(PARAM_TIME_AFTER_RACE_START_AS_HOURS_COLON_MILLIS_COLON_SECONDS, this);
        this.selectedCompetitor = new StringSetting(PARAM_SELECTED_COMPETITOR, this, null);
        this.selectedCompetitors = new StringSetSetting(PARAM_SELECTED_COMPETITORS, this, null);
        this.showTags = new BooleanSetting(PARAM_VIEW_SHOW_TAGS, this, false);
        this.showManeuverTable = new BooleanSetting(PARAM_VIEW_SHOW_MANEUVER_TABLE, this, false);
        this.jumpToTag = new StringSetting(PARAM_JUMP_TO_TAG, this, null);
        this.zoomStart = new LongSetting(PARAM_ZOOM_START, this, null);
        this.zoomEnd = new LongSetting(PARAM_ZOOM_END, this, null);
        this.autoExpandPreSelectedRace = new BooleanSetting(PARAM_AUTO_EXPAND_PRE_SELECTED_RACE, this, false);
    }

    public RaceBoardPerspectiveOwnSettings(String activeCompetitorsFilterSetName, Boolean showLeaderboard,
            Boolean showWindChart, Boolean showCompetitorsChart, Boolean canReplayDuringLiveRaces,
            Duration initialDurationAfterRaceStartInReplay, String selectedCompetitor, Iterable<String> selectedCompetitors, 
            Boolean showTags, Boolean showManeuverTable, String jumpToTag, Long zoomStart, Long zoomEnd, Boolean autoExpandPreSelectedRace) {
        super();
        this.showTags.setValue(showTags);
        this.showManeuverTable.setValue(showManeuverTable);
        this.activeCompetitorsFilterSetName.setValue(activeCompetitorsFilterSetName);
        this.showLeaderboard.setValue(showLeaderboard);
        this.showWindChart.setValue(showWindChart);
        this.showCompetitorsChart.setValue(showCompetitorsChart);
        this.canReplayDuringLiveRaces.setValue(canReplayDuringLiveRaces);
        this.initialDurationAfterRaceStartInReplay.setValue(initialDurationAfterRaceStartInReplay);
        this.selectedCompetitor.setValue(selectedCompetitor);
        this.selectedCompetitors.setValues(selectedCompetitors);
        this.jumpToTag.setValue(jumpToTag);
        this.zoomStart.setValue(zoomStart);
        this.zoomEnd.setValue(zoomEnd);
        this.autoExpandPreSelectedRace.setValue(autoExpandPreSelectedRace);
    }

    public boolean isShowLeaderboard() {
        return showLeaderboard.getValue();
    }

    public boolean isShowWindChart() {
        return showWindChart.getValue();
    }

    public boolean isShowCompetitorsChart() {
        return showCompetitorsChart.getValue();
    }

    public String getActiveCompetitorsFilterSetName() {
        return activeCompetitorsFilterSetName.getValue();
    }
    
    public Duration getInitialDurationAfterRaceStartInReplay() {
        return initialDurationAfterRaceStartInReplay.getValue();
    }

    public boolean isCanReplayDuringLiveRaces() {
        return canReplayDuringLiveRaces.getValue();
    }

    public boolean isShowTags() {
        return showTags.getValue();
    }

    public boolean isShowManeuver() {
        return showManeuverTable.getValue();
    }

    public String getJumpToTag() {
        return jumpToTag.getValue();
    }
    
    public Long getZoomStart() {
        return zoomStart.getValue();
    }
    
    public Long getZoomEnd() {
        return zoomEnd.getValue();
    }
    
    public Boolean isAutoExpandPreSelectedRace() {
        return autoExpandPreSelectedRace.getValue();
    }
    
    public void resetShowTags() {
        showTags.resetToDefault();
    }

    public void resetShowManeuver() {
        showManeuverTable.resetToDefault();
    }
    
    public void resetShowLeaderBoard() {
        this.showLeaderboard.resetToDefault();
    }
    
    public void resetShowWindChart() {
        this.showWindChart.resetToDefault();
    }
    
    public void resetShowCompetitorsChart() {
        this.showCompetitorsChart.resetToDefault();
    }

    public void resetCanReplayDuringLiveRaces() {
        this.canReplayDuringLiveRaces.resetToDefault();
    }
    
    public void resetActiveCompetitorsFilterSetName() {
        this.activeCompetitorsFilterSetName.resetToDefault();
    }

    public void resetInitialDurationAfterRaceStartInReplay() {
        this.initialDurationAfterRaceStartInReplay.resetToDefault();
    }

    public void resetSelectedCompetitor() {
        this.selectedCompetitor.resetToDefault();
    }

    public void resetSelectedCompetitors() {
        this.selectedCompetitors.resetToDefault();
    }

    public void resetJumpToTag() {
        this.jumpToTag.resetToDefault();
    }
    
    public void resetZoomStart() {
        this.zoomStart.resetToDefault();
    }
    
    public void resetZoomEnd() {
        this.zoomEnd.resetToDefault();
    }
    
    public void resetAutoExpandPreSelectedRace() {
        this.autoExpandPreSelectedRace.resetToDefault();
    }

    public static RaceBoardPerspectiveOwnSettings readSettingsFromURL(boolean defaultForViewShowLeaderboard,
            boolean defaultForViewShowWindchart, boolean defaultForViewShowCompetitorsChart,
            String defaultForViewCompetitorFilter, boolean defaultForCanReplayDuringLiveRaces, boolean defaultForViewShowTags,
            boolean defaultForViewShowManeuverTable, String defaultForJumpToTag, Long defaultForZoomStart, Long defaultForZoomEnd, boolean defaultForAutoExpandPreSelectedRace) {
        final boolean showLeaderboard = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_LEADERBOARD, defaultForViewShowLeaderboard /* default */);
        final boolean showWindChart = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_WINDCHART, defaultForViewShowWindchart /* default */);
        final boolean showCompetitorsChart = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_COMPETITORSCHART, defaultForViewShowCompetitorsChart /* default */);
        final String activeCompetitorsFilterSetName = GwtHttpRequestUtils.getStringParameter(PARAM_VIEW_COMPETITOR_FILTER, defaultForViewCompetitorFilter /* default */);
        final boolean canReplayWhileLiveIsPossible = GwtHttpRequestUtils.getBooleanParameter(PARAM_CAN_REPLAY_DURING_LIVE_RACES, defaultForCanReplayDuringLiveRaces /* default */);
        final Duration initialDurationAfterRaceStartInReplay = parseDuration(GwtHttpRequestUtils.getStringParameter(
                PARAM_TIME_AFTER_RACE_START_AS_HOURS_COLON_MILLIS_COLON_SECONDS, null /* default */));
        final Set<String> selectedCompetitors = new HashSet<>(Arrays.asList(GwtHttpRequestUtils.getStringParameters(PARAM_SELECTED_COMPETITORS)));
        final String selectedCompetitor = GwtHttpRequestUtils.getStringParameter(PARAM_SELECTED_COMPETITOR,
                null /* default */);
        final boolean showTags = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_TAGS, defaultForViewShowTags);
        final boolean showManeuverTable = GwtHttpRequestUtils.getBooleanParameter(PARAM_VIEW_SHOW_MANEUVER_TABLE, defaultForViewShowManeuverTable);
        final String jumpToTag = GwtHttpRequestUtils.getStringParameter(PARAM_JUMP_TO_TAG, defaultForJumpToTag /* default */);
        final Long zoomStart = defaultForZoomStart == null ? null : GwtHttpRequestUtils.getLongParameter(PARAM_ZOOM_START, defaultForZoomStart);
        final Long zoomEnd = defaultForZoomEnd == null ? null : GwtHttpRequestUtils.getLongParameter(PARAM_ZOOM_END, defaultForZoomEnd);
        final boolean autoExpandPreSelectedRace = GwtHttpRequestUtils.getBooleanParameter(PARAM_AUTO_EXPAND_PRE_SELECTED_RACE, defaultForAutoExpandPreSelectedRace);
        return new RaceBoardPerspectiveOwnSettings(activeCompetitorsFilterSetName, showLeaderboard, showWindChart,
                showCompetitorsChart, canReplayWhileLiveIsPossible, initialDurationAfterRaceStartInReplay,
                selectedCompetitor, selectedCompetitors, showTags, showManeuverTable, jumpToTag, zoomStart, zoomEnd, autoExpandPreSelectedRace);
    }

    /**
     * Understands [hh:[mm:]]ss and parses into a {@link Duration}. If {@code durationAsString} is {@code null} then
     * so is the result.
     */
    private static Duration parseDuration(String durationAsString) {
        final Duration result;
        if (durationAsString == null) {
            result = null;
        } else {
            long seconds = 0;
            for (final String hhmmss : durationAsString.split(":")) {
                seconds = 60*seconds + Long.valueOf(hhmmss);
            }
            result = new MillisecondsDurationImpl(1000l * seconds);
        }
        return result;
    }

    public Iterable<String> getSelectedCompetitors() {
        return selectedCompetitors.getValues();
    }
    
    public String getSelectedCompetitor() {
        return selectedCompetitor.getValue();
    }
}
