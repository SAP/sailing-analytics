package com.sap.sailing.gwt.ui.raceboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.maps.client.events.idle.IdleMapEvent;
import com.google.gwt.maps.client.events.idle.IdleMapHandler;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettings;
import com.sap.sailing.gwt.ui.client.shared.charts.ChartSettings;
import com.sap.sailing.gwt.ui.client.shared.charts.MultiCompetitorRaceChart;
import com.sap.sailing.gwt.ui.client.shared.charts.MultiCompetitorRaceChartSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.settings.ComponentContextWithSettingsStorageAndAdditionalSettingsLayers.OnSettingsReloadedCallback;

/**
 * The start analysis mode makes the competitor chart visible and sets it to speed over ground; the
 * {@link LeaderboardSettings} are adjusted such that no leg columns but only start parameters are
 * shown. The top three starters are selected when the leaderboard has been updated after setting
 * the timer to a few seconds after the start. The {@link PlayStates#Paused} is used for the timer.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class StartAnalysisMode extends RaceBoardModeWithPerRaceCompetitors {
    private static final Duration DURATION_AFTER_START_TO_SET_TIMER_TO_FOR_START_ANALYSIS = Duration.ONE_SECOND.times(10);
    private static final Duration DURATION_BEFORE_START_TO_INCLUDE_IN_CHART_TIME_RANGE = Duration.ONE_SECOND.times(30);
    private static final Duration DURATION_AFTER_START_TO_INCLUDE_IN_CHART_TIME_RANGE = Duration.ONE_SECOND.times(30);

    private MultiCompetitorRaceChart competitorChart;

    private boolean leaderboardSettingsAdjusted;

    private boolean competitorSelectionUpdated;
    private boolean timerAdjusted;
    private boolean mapSettingsAndTimeZoomAdjusted;

    @Override
    public void applyTo(RaceBoardPanel raceBoardPanel) {
        super.applyTo(raceBoardPanel);
        this.competitorChart = raceBoardPanel.getCompetitorChart();
    }

    @Override
    protected void updateCompetitorSelection() {
        updateCompetitorSelection(/* howManyTopCompetitorsInRaceToSelect */ 3, getLeaderboardForSpecificTimePoint());
    }

    private void adjustMapSettings() {
        RaceMap raceMap = getRaceBoardPanel().getMap();
        final RaceMapSettings defaultSettings = raceMap.getLifecycle().createDefaultSettings();
        RaceMapZoomSettings raceMapZoomSettings = new RaceMapZoomSettings(Collections.singleton(ZoomTypes.BOATS), /* zoomToSelected */ false);
        boolean isWindUp = true;
        final RaceMapSettings additiveSettings = new RaceMapSettings
                .RaceMapSettingsBuilder(defaultSettings, raceMap.getLifecycle().getRaceDTO(), raceMap.getLifecycle().getPaywallResolver())
                .withZoomSettings(raceMapZoomSettings)
                .withWindUp(isWindUp)
                .build();
        ((RaceBoardComponentContext) raceMap.getComponentContext()).addModesPatching(raceMap, additiveSettings, new OnSettingsReloadedCallback<RaceMapSettings>() {
            @Override
            public void onSettingsReloaded(RaceMapSettings patchedSettings) {
             // try to update the settings once; the problem is the "wind up" display; it changes pan/zoom
                // which is a major source of instability for the map. Going twice in the map idle event handler
                // to ensure the settings are really applied.
                final Object[] registration = new Object[1];
                registration[0] = getRaceBoardPanel().getMap().getMap().addIdleHandler(new IdleMapHandler() {
                    @Override
                    public void onEvent(IdleMapEvent event) {
                        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                            @Override
                            public void execute() {
                                getRaceBoardPanel().getMap().updateSettings(patchedSettings);
                            }
                        });
                        ((HandlerRegistration) registration[0]).removeHandler();
                    }
                });
                raceMap.updateSettings(patchedSettings);
            }
            
        });
    }

    @Override
    protected void trigger() {
        if (!timerAdjusted && getRaceTimesInfoForRace() != null && getRaceTimesInfoForRace().startOfRace != null) {
            timerAdjusted = true;
            // we've done our adjustments; remove listener and let go
            stopReceivingRaceTimesInfos();
            // make sure we're no longer in live/playing mode/state; the start analysis is supposed to be a "frozen" display at first
            if (getTimer().getPlayMode() == PlayModes.Live) {
                getTimer().setPlayMode(PlayModes.Replay);
            }
            getTimer().pause();
            // the following call will always trigger a leaderboard load and therefore a callback
            // to updatedLeaderboard; therefore, updatedLeaderboard can decide when it's time to
            // trigger the chart display and the map settings application
            getTimer().setTime(new MillisecondsTimePoint(getRaceTimesInfoForRace().startOfRace).plus(DURATION_AFTER_START_TO_SET_TIMER_TO_FOR_START_ANALYSIS).asMillis());
            // no need to do anything if we don't have the timing info yet; after obtaining the timing
            // info we can start waiting for the simulation overlay and then adjust the map settings and,
            // if available, show the competitor chart
            if (!mapSettingsAndTimeZoomAdjusted) {
                mapSettingsAndTimeZoomAdjusted = true;
                Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
                    @Override
                    public boolean execute() {
                        // keep trying in 500ms steps until the simulation overlay is found; this way, changing the competitor chart
                        // settings is largely more successful
                        final boolean simulationOverlayFound = getRaceBoardPanel().getMap().getSimulationOverlay() != null;
                        if (simulationOverlayFound) {
                            if (isCompetitorChartEnabled()) {
                                getRaceBoardPanel().setCompetitorChartVisible(true);
                                final MultiCompetitorRaceChartSettings additiveSettings = new MultiCompetitorRaceChartSettings(
                                        new ChartSettings(/* stepSizeInMillis */ 1000), DetailType.RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS,
                                        /* no second series */ null);
                                ((RaceBoardComponentContext) competitorChart.getComponentContext()).addModesPatching(competitorChart, additiveSettings, new OnSettingsReloadedCallback<MultiCompetitorRaceChartSettings>() {

                                    @Override
                                    public void onSettingsReloaded(MultiCompetitorRaceChartSettings patchedSettings) {
                                        competitorChart.updateSettings(patchedSettings);
                                    }
                                    
                                });
                                getRaceTimePanel().getTimeRangeProvider().setTimeZoom(
                                        new MillisecondsTimePoint(getRaceTimesInfoForRace().startOfRace).minus(DURATION_BEFORE_START_TO_INCLUDE_IN_CHART_TIME_RANGE).asDate(),
                                        new MillisecondsTimePoint(getRaceTimesInfoForRace().startOfRace).plus(DURATION_AFTER_START_TO_INCLUDE_IN_CHART_TIME_RANGE).asDate());
                            }
                            adjustMapSettings();
                        }
                        return !simulationOverlayFound;
                    }
                }, /* delay in milliseconds */ 500);
            }
        }
        if (!leaderboardSettingsAdjusted && getLeaderboard() != null) {
            leaderboardSettingsAdjusted = true;
            stopReceivingLeaderboard();
            adjustLeaderboardSettings();
        }
        if (getLeaderboardForSpecificTimePoint() == null &&
                getRaceColumn() != null && getLeaderboard() != null
                && timerAdjusted) {
            loadLeaderboardForSpecificTimePoint(getLeaderboard().getName(), getRaceColumn().getName(), getTimer().getTime());
        }
        if (!competitorSelectionUpdated && getLeaderboardForSpecificTimePoint() != null && getCompetitorsInRace() != null) {
            competitorSelectionUpdated = true;
            stopReceivingCompetitorsInRace();
            updateCompetitorSelection();
        }
    }

    private void adjustLeaderboardSettings() {
        final SingleRaceLeaderboardPanel leaderboardPanel = getLeaderboardPanel();
        final List<DetailType> raceDetailsToShow = new ArrayList<>();
        raceDetailsToShow.add(DetailType.RACE_SPEED_OVER_GROUND_FIVE_SECONDS_BEFORE_START);
        raceDetailsToShow.add(DetailType.RACE_DISTANCE_TO_START_FIVE_SECONDS_BEFORE_RACE_START);
        raceDetailsToShow.add(DetailType.DISTANCE_TO_START_AT_RACE_START);
        raceDetailsToShow.add(DetailType.DISTANCE_TO_STARBOARD_END_OF_STARTLINE_WHEN_PASSING_START_IN_METERS);
        raceDetailsToShow.add(DetailType.SPEED_OVER_GROUND_AT_RACE_START);
        raceDetailsToShow.add(DetailType.SPEED_OVER_GROUND_WHEN_PASSING_START);
        raceDetailsToShow.add(DetailType.TIME_BETWEEN_RACE_START_AND_COMPETITOR_START);
        raceDetailsToShow.add(DetailType.START_TACK);
        raceDetailsToShow.add(DetailType.RACE_GAP_TO_LEADER_IN_SECONDS);
        SecurityChildSettingsContext context = new SecurityChildSettingsContext(getLeaderboard(), leaderboardPanel.getPaywallResolver());
        final SingleRaceLeaderboardSettings additiveSettings = SingleRaceLeaderboardSettings
                .createDefaultSettingsWithRaceDetailValues(raceDetailsToShow, context);
        ((RaceBoardComponentContext) leaderboardPanel.getComponentContext()).addModesPatching(leaderboardPanel, additiveSettings, new OnSettingsReloadedCallback<SingleRaceLeaderboardSettings>() {

            @Override
            public void onSettingsReloaded(SingleRaceLeaderboardSettings patchedSettings) {
                leaderboardPanel.updateSettings(patchedSettings);
            }
            
        });
    }
    
    private boolean isCompetitorChartEnabled() {
        return competitorChart != null;
    }
}
