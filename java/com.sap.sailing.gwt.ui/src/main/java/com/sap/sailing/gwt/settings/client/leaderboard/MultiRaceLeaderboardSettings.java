package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.Collection;
import java.util.List;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardPanel;
import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.EnumSetting;
import com.sap.sse.common.settings.generic.IntegerSetting;
import com.sap.sse.common.settings.generic.StringSetSetting;
import com.sap.sse.common.settings.generic.support.SettingsUtil;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class MultiRaceLeaderboardSettings extends LeaderboardSettings {
    private static final long serialVersionUID = -3445146715292390755L;

    /**
     * Only one of {@link #namesOfRaceColumnsToShow} and {@link #namesOfRacesToShow} must be non-<code>null</code>.
     * Only valid when the {@link #activeRaceColumnSelectionStrategy} is set to EXPLIZIT
     */
    protected StringSetSetting namesOfRaceColumnsToShow;

    /**
     * Only valid when the {@link #activeRaceColumnSelectionStrategy} is set to LAST_N
     */
    protected IntegerSetting numberOfLastRacesToShow;
    
    protected EnumSetting<RaceColumnSelectionStrategies> activeRaceColumnSelectionStrategy;

    
    public MultiRaceLeaderboardSettings(SecurityChildSettingsContext context) {
        super(false, context);
    }

    public MultiRaceLeaderboardSettings(Collection<DetailType> maneuverDetailsToShow,
            Collection<DetailType> legDetailsToShow, Collection<DetailType> raceDetailsToShow,
            Collection<DetailType> overallDetailsToShow, List<String> namesOfRaceColumnsToShow,
            Integer numberOfLastRacesToShow,
            Long delayBetweenAutoAdvancesInMilliseconds,
            RaceColumnSelectionStrategies activeRaceColumnSelectionStrategy,
            boolean showAddedScores, boolean showCompetitorShortNameColumn, boolean showCompetitorFullNameColumn,
            boolean showCompetitorBoatInfoColumn, boolean isCompetitorNationalityColumnVisible,
            SecurityChildSettingsContext context) {
        super(maneuverDetailsToShow, legDetailsToShow, raceDetailsToShow, overallDetailsToShow, delayBetweenAutoAdvancesInMilliseconds,
                showAddedScores, showCompetitorShortNameColumn,
                showCompetitorFullNameColumn, showCompetitorBoatInfoColumn, isCompetitorNationalityColumnVisible, context);
        this.namesOfRaceColumnsToShow.setValues(namesOfRaceColumnsToShow);
        this.numberOfLastRacesToShow.setValue(numberOfLastRacesToShow);
        this.activeRaceColumnSelectionStrategy.setValue(activeRaceColumnSelectionStrategy);
    }

    public MultiRaceLeaderboardSettings(Iterable<String> namesOfRaceColumnsToShow, SecurityChildSettingsContext context) {
        super(false, context);
        this.namesOfRaceColumnsToShow.setValues(namesOfRaceColumnsToShow);
    }

    @Override
    protected void addChildSettings(SecurityChildSettingsContext context) {
        super.addChildSettings(context);
        namesOfRaceColumnsToShow = new StringSetSetting("namesOfRaceColumnsToShow", this);
        numberOfLastRacesToShow = new IntegerSetting("numberOfLastRacesToShow", this, null);
        activeRaceColumnSelectionStrategy = new EnumSetting<>("activeRaceColumnSelectionStrategy", this, RaceColumnSelectionStrategies.EXPLICIT, RaceColumnSelectionStrategies::valueOf);
    }
    
    /**
     * If <code>null</code>, this is to mean that the race columns should not be modified by
     * {@link LeaderboardPanel#updateSettings(LeaderboardSettings)}. Otherwise a
     * live collection that reflects the current state of the settings of a leaderboard panel
     */
    public List<String> getNamesOfRaceColumnsToShow() {
        return activeRaceColumnSelectionStrategy.getValue() == RaceColumnSelectionStrategies.EXPLICIT ? (namesOfRaceColumnsToShow.isValuesEmpty() ? null : Util.asList(namesOfRaceColumnsToShow.getValues())) : null;
    }

    /**
     * If <code>null</code>, this is to mean that the race columns should not be modified by
     * {@link LeaderboardPanel#updateSettings(LeaderboardSettings)}.
     */
    public Integer getNumberOfLastRacesToShow() {
        return activeRaceColumnSelectionStrategy.getValue() == RaceColumnSelectionStrategies.LAST_N ? numberOfLastRacesToShow.getValue() : null;
    }

    public RaceColumnSelectionStrategies getActiveRaceColumnSelectionStrategy() {
        return activeRaceColumnSelectionStrategy.getValue();
    }

    /**
     * Constructs a new {@link MultiRaceLeaderboardSettings} instance with all defaults and values taken from this
     * instance except the defaults for namesOfRaceColumnsToShow which are set to the given values.
     */
    public MultiRaceLeaderboardSettings withNamesOfRaceColumnsToShowDefaults(
            final Iterable<String> namesOfRaceColumnsToShow, SecurityChildSettingsContext context) {
        final MultiRaceLeaderboardSettings newSettings = new MultiRaceLeaderboardSettings(context);
        SettingsUtil.copyValuesAndDefaults(this, this, newSettings);
        newSettings.namesOfRaceColumnsToShow.setDefaultValues(namesOfRaceColumnsToShow);
        return newSettings;
    }

    /**
     * Constructs a new {@link MultiRaceLeaderboardSettings} instance with all defaults and values taken from this
     * instance except the namesOfRaceColumnsToShow. The defaults as well as the value of namesOfRaceColumnsToShow are
     * set to the given values.
     */
    public MultiRaceLeaderboardSettings withNamesOfRaceColumnsToShowDefaultsAndValues(
            final Iterable<String> namesOfRaceColumnsToShow, SecurityChildSettingsContext context) {
        final MultiRaceLeaderboardSettings newSettings = withNamesOfRaceColumnsToShowDefaults(namesOfRaceColumnsToShow, context);
        newSettings.namesOfRaceColumnsToShow.setValues(namesOfRaceColumnsToShow);
        return newSettings;
    }

    /**
     * Constructs a new {@link MultiRaceLeaderboardSettings} instance with all defaults and values taken from this
     * instance except of the values for activeRaceColumnSelectionStrategy, numberOfLastRacesToShow and
     * namesOfRaceColumnsToShow which are taken from the given instance.
     */
    public MultiRaceLeaderboardSettings withRaceColumnSelectionValuesFrom(
            MultiRaceLeaderboardSettings settingsWithRaceColumnSelection) {
        // take over context from origin settings, because they are valid also for new settings
        final MultiRaceLeaderboardSettings newSettings = new MultiRaceLeaderboardSettings(settingsWithRaceColumnSelection.getContext());
        SettingsUtil.copyValuesAndDefaults(this, this, newSettings);
        newSettings.activeRaceColumnSelectionStrategy
                .setValue(settingsWithRaceColumnSelection.getActiveRaceColumnSelectionStrategy());
        newSettings.numberOfLastRacesToShow.setValue(settingsWithRaceColumnSelection.getNumberOfLastRacesToShow());
        newSettings.namesOfRaceColumnsToShow.setValues(settingsWithRaceColumnSelection.getNamesOfRaceColumnsToShow());
        return newSettings;
    }

    /**
     * Constructs a new {@link MultiRaceLeaderboardSettings} instance using defaults, resets the
     * namesOfRaceColumnsToShow, sets the activeRaceColumnSelectionStrategy to last-n and uses the given value for
     * numberOfLastRacesToShow.
     */
    public static MultiRaceLeaderboardSettings createDefaultSettingsWithLastNRaceColumnSelection(
            int numberOfLastRacesToShow, SecurityChildSettingsContext context) {
        final MultiRaceLeaderboardSettings newSettings = new MultiRaceLeaderboardSettings(context);
        newSettings.namesOfRaceColumnsToShow.setValues(null);
        newSettings.activeRaceColumnSelectionStrategy.setValue(RaceColumnSelectionStrategies.LAST_N);
        newSettings.numberOfLastRacesToShow.setValue(numberOfLastRacesToShow);
        return newSettings;
    }

}
