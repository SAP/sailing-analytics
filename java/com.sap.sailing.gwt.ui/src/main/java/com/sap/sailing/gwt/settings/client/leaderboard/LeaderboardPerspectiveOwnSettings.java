package com.sap.sailing.gwt.settings.client.leaderboard;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.StringSetting;

public class LeaderboardPerspectiveOwnSettings extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = 8978956250485028963L;
    
    private transient BooleanSetting showRaceDetails;
    private transient BooleanSetting hideToolbar;
    private transient BooleanSetting autoExpandLastRaceColumn;
    private transient BooleanSetting showCharts;
    private transient BooleanSetting showOverallLeaderboard;
    private transient BooleanSetting showSeriesLeaderboards;
    private transient StringSetting zoomTo;
    private transient BooleanSetting embedded;
    private transient BooleanSetting livePlay;
    
    LeaderboardPerspectiveOwnSettings() {
        super();
    }
    
    public LeaderboardPerspectiveOwnSettings(boolean showRaceDetails) {
        this();
        this.showRaceDetails.setValue(showRaceDetails);
    }
    
    public LeaderboardPerspectiveOwnSettings(boolean showRaceDetails, boolean embedded) {
        this(showRaceDetails);
        this.embedded.setValue(embedded);
    }

    LeaderboardPerspectiveOwnSettings(boolean showRaceDetails, boolean hideToolbar, boolean autoExpandLastRaceColumn,
            boolean showCharts, boolean showOverallLeaderboard, boolean showSeriesLeaderboards, String zoomTo,
            boolean embedded, boolean lifeplay) {
        this(showRaceDetails, embedded);
        this.hideToolbar.setValue(hideToolbar);
        this.autoExpandLastRaceColumn.setValue(autoExpandLastRaceColumn);
        this.showCharts.setValue(showCharts);
        this.showOverallLeaderboard.setValue(showOverallLeaderboard);
        this.showSeriesLeaderboards.setValue(showSeriesLeaderboards);
        this.zoomTo.setValue(zoomTo);
        this.livePlay.setValue(lifeplay);
    }

    @Override
    protected void addChildSettings() {
        showRaceDetails = new BooleanSetting("showRaceDetails", this, false);
        hideToolbar = new BooleanSetting("hideToolbar", this, false);
        autoExpandLastRaceColumn = new BooleanSetting(LeaderboardUrlSettings.PARAM_AUTO_EXPAND_LAST_RACE_COLUMN, this, false);
        showCharts = new BooleanSetting("showCharts", this, false);
        showOverallLeaderboard = new BooleanSetting("showOverallLeaderboard", this, false);
        showSeriesLeaderboards = new BooleanSetting("showSeriesLeaderboards", this, false);
        zoomTo = new StringSetting("zoomTo", this, "1");
        embedded = new BooleanSetting("embedded", this, false);
        livePlay = new BooleanSetting("livePlay", this, false);
    }

    public boolean isLifePlay() {
        return livePlay.getValue();
    }

    public boolean isShowRaceDetails() {
        return showRaceDetails.getValue();
    }
    public boolean isHideToolbar() {
        return hideToolbar.getValue();
    }
    public boolean isAutoExpandLastRaceColumn() {
        return autoExpandLastRaceColumn.getValue();
    }
    public boolean isShowCharts() {
        return showCharts.getValue();
    }
    public boolean isShowOverallLeaderboard() {
        return showOverallLeaderboard.getValue();
    }
    public boolean isShowSeriesLeaderboards() {
        return showSeriesLeaderboards.getValue();
    }
    public String getZoomTo() {
        return zoomTo.getValue();
    }
    public boolean isEmbedded() {
        return embedded.getValue();
    }
}
