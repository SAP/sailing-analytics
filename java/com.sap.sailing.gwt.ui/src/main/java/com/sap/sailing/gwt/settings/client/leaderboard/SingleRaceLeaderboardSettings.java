package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.Collection;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class SingleRaceLeaderboardSettings extends LeaderboardSettings {
    private static final long serialVersionUID = 2891220120957743158L;

    private BooleanSetting showRaceRankColumn;
    
    public SingleRaceLeaderboardSettings(boolean showCompetitorBoatInfoColumnDefault, SecurityChildSettingsContext context) {
        super(showCompetitorBoatInfoColumnDefault, context);
    }

    public SingleRaceLeaderboardSettings(Collection<DetailType> maneuverDetailsToShow,
            Collection<DetailType> legDetailsToShow, Collection<DetailType> raceDetailsToShow,
            Collection<DetailType> overallDetailsToShow, Long delayBetweenAutoAdvancesInMilliseconds,
            boolean showAddedScores, boolean showCompetitorShortNameColumn, boolean showCompetitorFullNameColumn,
            boolean showCompetitorBoatInfoColumn, boolean isCompetitorNationalityColumnVisible,
            boolean showRaceRankColumn, SecurityChildSettingsContext context) {
        super(maneuverDetailsToShow, legDetailsToShow, raceDetailsToShow, overallDetailsToShow,
                delayBetweenAutoAdvancesInMilliseconds, showAddedScores, showCompetitorShortNameColumn,
                showCompetitorFullNameColumn, showCompetitorBoatInfoColumn, isCompetitorNationalityColumnVisible, context);
        this.showRaceRankColumn.setValue(showRaceRankColumn);
    }
    
    public boolean isShowRaceRankColumn() {
        return showRaceRankColumn.getValue();
    }
    
    @Override
    protected void addChildSettings(SecurityChildSettingsContext context) {
        super.addChildSettings(context);
        showRaceRankColumn = new BooleanSetting("racerank", this, false);
    }

    /**
     * Constructs a new {@link SingleRaceLeaderboardSettings} instance using defaults except the values for
     * raceDetailsToShow which are set to the given values.
     */
    public static SingleRaceLeaderboardSettings createDefaultSettingsWithRaceDetailValues(
            final Iterable<DetailType> raceDetailsToShow, SecurityChildSettingsContext context) {
        final SingleRaceLeaderboardSettings newSettings = new SingleRaceLeaderboardSettings(false, context);
        newSettings.raceDetailsToShow.setValues(raceDetailsToShow);
        return newSettings;
    }
}
