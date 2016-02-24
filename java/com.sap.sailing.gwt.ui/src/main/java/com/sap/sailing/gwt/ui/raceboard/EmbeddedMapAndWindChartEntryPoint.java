
package com.sap.sailing.gwt.ui.raceboard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.AbstractSailingEntryPoint;
import com.sap.sailing.gwt.ui.client.CompetitorColorProvider;
import com.sap.sailing.gwt.ui.client.CompetitorColorProviderImpl;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.LogoAndTitlePanel;
import com.sap.sailing.gwt.ui.client.TimePanel;
import com.sap.sailing.gwt.ui.client.TimePanelSettings;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChart;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChartLifecycle;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChartSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings.HelpLineTypes;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapLifecycle;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapResources;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomModel;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.shared.GwtHttpRequestUtils;

public class EmbeddedMapAndWindChartEntryPoint extends AbstractSailingEntryPoint {
    private static final String PARAM_REGATTA_LIKE_NAME = "regattaLikeName";
    private static final String PARAM_RACE_COLUMN_NAME = "raceColumnName";
    private static final String PARAM_FLEET_NAME = "fleetName";
    private static final String PARAM_SHOW_COMPETITORS = "showCompetitors";
    private static final String PARAM_PLAY = "play";
    private static final String PARAM_SHOW_COURSE_GEOMETRY = "showCourseGeometry";
    private static final String PARAM_MAP_ORIENTATION_WIND_UP = "windUp";
    
    private String regattaLikeName;
    private String raceColumnName;
    private String fleetName;
    
    private static final RaceMapResources raceMapResources = GWT.create(RaceMapResources.class);
    
    private static final int DEFAULT_WIND_CHART_HEIGHT = 200;

    @Override
    protected void doOnModuleLoad() {
        super.doOnModuleLoad();
        // read mandatory parameters
        regattaLikeName = Window.Location.getParameter(PARAM_REGATTA_LIKE_NAME);
        raceColumnName = Window.Location.getParameter(PARAM_RACE_COLUMN_NAME);
        fleetName = Window.Location.getParameter(PARAM_FLEET_NAME);
        if (regattaLikeName == null || regattaLikeName.isEmpty() || raceColumnName == null || raceColumnName.isEmpty() ||
                fleetName == null || fleetName.isEmpty()) {
            createErrorPage(getStringMessages().requiresValidRegatta());
            return;
        }
        
        // read optional parameters
        final RaceBoardPerspectiveSettings raceboardPerspectiveSettings = RaceBoardPerspectiveSettings.readSettingsFromURL();
        final RaceMapSettings raceMapSettings = RaceMapSettings.readSettingsFromURL(); 
        final boolean showCompetitors = GwtHttpRequestUtils.getBooleanParameter(PARAM_SHOW_COMPETITORS, false /* default */);
        final boolean play = GwtHttpRequestUtils.getBooleanParameter(PARAM_PLAY, false /* default */);
        final boolean showCourseGeometry = GwtHttpRequestUtils.getBooleanParameter(PARAM_SHOW_COURSE_GEOMETRY, true /* default */);
        final boolean windUp = GwtHttpRequestUtils.getBooleanParameter(PARAM_MAP_ORIENTATION_WIND_UP, true /* default */);
        sailingService.getRaceIdentifier(regattaLikeName, raceColumnName, fleetName, new AsyncCallback<RegattaAndRaceIdentifier>() {
            @Override
            public void onSuccess(final RegattaAndRaceIdentifier selectedRaceIdentifier) {
                if (selectedRaceIdentifier == null) {
                    createErrorPage(getStringMessages().couldNotObtainRace(regattaLikeName, raceColumnName, fleetName, /* technicalErrorMessage */ ""));
                } else {
                    sailingService.getCompetitorBoats(selectedRaceIdentifier, new AsyncCallback<Map<CompetitorDTO, BoatDTO>>() {
                        @Override
                        public void onSuccess(Map<CompetitorDTO, BoatDTO> result) {
                            createEmbeddedMap(selectedRaceIdentifier, result, raceboardPerspectiveSettings, raceMapSettings, 
                                              showCompetitors, play, showCourseGeometry, windUp);
                        }
                        
                        @Override
                        public void onFailure(Throwable caught) {
                            reportError(getStringMessages().errorTryingToCreateEmbeddedMap(caught.getMessage()));
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Throwable caught) {
                createErrorPage(getStringMessages().couldNotObtainRace(regattaLikeName, raceColumnName, fleetName, caught.getMessage()));
            }
        });
    }
    
    private void createErrorPage(String message) {
        final DockLayoutPanel vp = new DockLayoutPanel(Unit.PX);
        LogoAndTitlePanel logoAndTitlePanel = new LogoAndTitlePanel(getStringMessages(), this, getUserService());
        logoAndTitlePanel.addStyleName("LogoAndTitlePanel");
        RootLayoutPanel.get().add(vp);
        vp.addNorth(logoAndTitlePanel, 100);
        vp.add(new Label(message));
    }

    private void createEmbeddedMap(final RegattaAndRaceIdentifier selectedRaceIdentifier, Map<CompetitorDTO, BoatDTO> competitorBoats,
            final RaceBoardPerspectiveSettings raceboardPerspectiveSettings, final RaceMapSettings raceMapSettings, 
            final boolean showCompetitors, final boolean play, final boolean showCourseGeometry, final boolean windUp) {
        final StringBuilder title = new StringBuilder(regattaLikeName);
        title.append('/');
        title.append(raceColumnName);
        if (!fleetName.equals(LeaderboardNameConstants.DEFAULT_FLEET_NAME)) {
            title.append('/');
            title.append(fleetName);
        }
        Window.setTitle(title.toString());
        final long refreshInterval = Duration.ONE_SECOND.times(3).asMillis();
        final Timer timer = new Timer(play ? PlayModes.Live : PlayModes.Replay);
        AsyncActionsExecutor asyncActionsExecutor = new AsyncActionsExecutor();
        final TimeRangeWithZoomProvider timeRangeWithZoomProvider = new TimeRangeWithZoomModel();
        // Use a TimePanel to manage wind chart zoom, although the TimePanel itself is not being displayed;
        // let the time panel always return to "live" mode.
        final TimePanel<TimePanelSettings> timePanel = new TimePanel<TimePanelSettings>(
                timer, timeRangeWithZoomProvider, getStringMessages(), /* canReplayWhileLive */ false,
                /* isScreenLargeEnoughToOfferChartSupport set to true iff wind chart will be displayed */ raceboardPerspectiveSettings.isShowWindChart()) {
            protected boolean isLiveModeToBeMadePossible() {
                return true;
            }
        };
        final Button backToLivePlayButton = timePanel.getBackToLiveButton();
        timePanel.updateSettings(new TimePanelSettings(refreshInterval));
        raceMapResources.combinedWindPanelStyle().ensureInjected();
        final CompetitorColorProvider colorProvider = new CompetitorColorProviderImpl(selectedRaceIdentifier, competitorBoats);
        final CompetitorSelectionProvider competitorSelection;
        if (showCompetitors) {
            competitorSelection = new CompetitorSelectionModel(/* hasMultiSelection */ true, colorProvider);
        } else {
            competitorSelection = createEmptyFilterCompetitorModel(colorProvider); // show no competitors
        }
        final RaceMap raceMap = new RaceMap(new RaceMapLifecycle(getStringMessages()), raceMapSettings, sailingService, asyncActionsExecutor, /* errorReporter */ EmbeddedMapAndWindChartEntryPoint.this, timer,
                competitorSelection, getStringMessages(), selectedRaceIdentifier, raceMapResources.combinedWindPanelStyle(),
                raceboardPerspectiveSettings.isSimulationEnabled(), /* showHeaderPanel */ false) {
            @Override
            protected void showAdditionalControls(MapWidget map) {
                backToLivePlayButton.removeFromParent();
                map.setControls(ControlPosition.RIGHT_BOTTOM, backToLivePlayButton);
            }
        };
        final RaceMapSettings mapSettings = new RaceMapSettings(raceMap.getSettings());
        mapSettings.setZoomSettings(new RaceMapZoomSettings(Arrays.asList(ZoomTypes.BUOYS), /* zoom to selection */ false));
        if (showCourseGeometry) {
            Set<HelpLineTypes> helpLineTypes = new HashSet<>();
            Util.addAll(mapSettings.getHelpLinesSettings().getVisibleHelpLineTypes(), helpLineTypes);
            helpLineTypes.add(HelpLineTypes.COURSEGEOMETRY);
            mapSettings.setHelpLinesSettings(new RaceMapHelpLinesSettings(helpLineTypes));
        }
        mapSettings.setWindUp(windUp);
        raceMap.updateSettings(mapSettings);
        final WindChart windChart;
        if (raceboardPerspectiveSettings.isShowWindChart()) {
            windChart = new WindChart(new WindChartLifecycle(getStringMessages()), sailingService, selectedRaceIdentifier, timer,
                    timeRangeWithZoomProvider, new WindChartSettings(), getStringMessages(),
                    asyncActionsExecutor, /* errorReporter */
                    EmbeddedMapAndWindChartEntryPoint.this, /* compactChart */ true);
        } else {
            windChart = null;
        }
        createRaceBoardInOneScreenMode(raceMap, windChart);
        timer.setTime(timer.getTime().getTime()-1000l);
    }  

    private CompetitorSelectionProvider createEmptyFilterCompetitorModel(CompetitorColorProvider colorProvider) {
        final CompetitorSelectionModel result = new CompetitorSelectionModel(/* hasMultiSelection */ true, colorProvider);
        final FilterSet<CompetitorDTO, Filter<CompetitorDTO>> filterSet = result.getOrCreateCompetitorsFilterSet("Empty");
        filterSet.addFilter(new Filter<CompetitorDTO>() {
            @Override public boolean matches(CompetitorDTO object) { return false; }
            @Override public String getName() { return "Never matching filter"; }
        });
        return result;
    }

    private void createRaceBoardInOneScreenMode(final RaceMap raceMap, final WindChart windChart) {
        final TouchSplitLayoutPanel p = new TouchSplitLayoutPanel(/* horizontal splitter width */ 3, /* vertical splitter height */ 25);
        RootLayoutPanel.get().add(p);
        if (windChart != null) {
            windChart.setVisible(true);
            p.insert(windChart.getEntryWidget(), windChart, Direction.SOUTH, DEFAULT_WIND_CHART_HEIGHT);
            p.setWidgetVisibility(windChart.getEntryWidget(), windChart, /* hidden */ false, DEFAULT_WIND_CHART_HEIGHT);
        }
        p.insert(raceMap.getEntryWidget(), raceMap, Direction.CENTER, 400);
        p.addStyleName("dockLayoutPanel");
    }
}
