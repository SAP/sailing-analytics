
package com.sap.sailing.gwt.ui.raceboard;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel.Direction;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.common.communication.routing.ProvidesLeaderboardRouting;
import com.sap.sailing.gwt.settings.client.embeddedmapandwindchart.EmbeddedMapAndWindChartContextDefinition;
import com.sap.sailing.gwt.settings.client.embeddedmapandwindchart.EmbeddedMapAndWindChartSettings;
import com.sap.sailing.gwt.settings.client.raceboard.RaceBoardPerspectiveOwnSettings;
import com.sap.sailing.gwt.ui.client.AbstractSailingReadEntryPoint;
import com.sap.sailing.gwt.ui.client.CompetitorColorProvider;
import com.sap.sailing.gwt.ui.client.CompetitorColorProviderImpl;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.TimePanel;
import com.sap.sailing.gwt.ui.client.TimePanelSettings;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChart;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChartLifecycle;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChartSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.DefaultQuickFlagDataProvider;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceCompetitorSet;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings.HelpLineTypes;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapLifecycle;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapResources;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomModel;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.authentication.generic.sapheader.SAPHeaderWithAuthentication;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class EmbeddedMapAndWindChartEntryPoint extends AbstractSailingReadEntryPoint implements ProvidesLeaderboardRouting {
    
    private static final RaceMapResources raceMapResources = GWT.create(RaceMapResources.class);
    private static final int DEFAULT_WIND_CHART_HEIGHT = 200;
    private static final SettingsToUrlSerializer SERIALIZER = new SettingsToUrlSerializer();

    private EmbeddedMapAndWindChartContextDefinition contextDefinition;
    private EmbeddedMapAndWindChartSettings settings;
    
    @Override
    protected void doOnModuleLoad() {
        final PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
        super.doOnModuleLoad();
        // read mandatory parameters
        contextDefinition = SERIALIZER.deserializeFromCurrentLocation(new EmbeddedMapAndWindChartContextDefinition());
        if (!contextDefinition.isValidContext()) {
            createErrorPage(getStringMessages().requiresValidRegatta(), paywallResolver);
            return;
        }
        // read optional parameters
        final RaceBoardPerspectiveOwnSettings raceboardPerspectiveSettings = RaceBoardPerspectiveOwnSettings
                .readSettingsFromURL(/* defaultForViewShowLeaderboard */ true, /* defaultForViewShowWindchart */ true,
                        /* defaultForViewShowCompetitorsChart */ false, /* defaultForViewCompetitorFilter */ null,
                        /* defaultForCanReplayDuringLiveRaces */ false, /* defaultShowTags */ false, /* defaultShowManeuverTable */false,
                        /* defaultForJumpToTag */ null, /* zoomStart */ null, /* zoomEnd */ null, false);
        final RaceMapSettings defaultRaceMapSettings = RaceMapSettings.readSettingsFromURL(
                /* defaultForShowMapControls */ true, /* defaultForShowCourseGeometry */ true,
                /* defaultForMapOrientationWindUp */ true, /* defaultForViewShowStreamlets */ false,
                /* defaultForViewShowStreamletColors */ false, /* defaultForViewShowSimulation */ false,
                /* defaultForTailLengthInMilliseconds */ 1l, paywallResolver, null);
        settings = SERIALIZER.deserializeFromCurrentLocation(new EmbeddedMapAndWindChartSettings());
        RaceMapZoomSettings raceMapZoomSettings = new RaceMapZoomSettings(Arrays.asList(ZoomTypes.BUOYS), /* zoom to selection */ false);
        Set<HelpLineTypes> helpLineTypes = new HashSet<>();
        Util.addAll(defaultRaceMapSettings.getHelpLinesSettings().getVisibleHelpLineTypes(), helpLineTypes);
        if (settings.isShowCourseGeometry()) {
            helpLineTypes.add(HelpLineTypes.COURSEGEOMETRY);
        }
        final String regattaLikeName = contextDefinition.getRegattaLikeName();
        final String raceColumnName = contextDefinition.getRaceColumnName();
        final String fleetName = contextDefinition.getFleetName();
        getSailingService().getRaceIdentifierAndTrackedRaceSecuredDTO(regattaLikeName, raceColumnName, fleetName,
                new AsyncCallback<Pair<RegattaAndRaceIdentifier, SecuredDTO>>() {
                    @Override
                    public void onSuccess(
                            final Pair<RegattaAndRaceIdentifier, SecuredDTO> selectedRaceIdentifierAndTrackedRaceSecuredDTO) {
                        final RegattaAndRaceIdentifier selectedRaceIdentifier = selectedRaceIdentifierAndTrackedRaceSecuredDTO
                                .getA();
                        final SecuredDTO raceDTO = selectedRaceIdentifierAndTrackedRaceSecuredDTO.getB();
                        RaceMapHelpLinesSettings raceMapHelpLinesSettings = new RaceMapHelpLinesSettings(helpLineTypes);
                        final RaceMapSettings raceMapSettings = new RaceMapSettings.RaceMapSettingsBuilder(defaultRaceMapSettings, raceDTO, paywallResolver)
                                .withHelpLinesSettings(raceMapHelpLinesSettings)
                                .withZoomSettings(raceMapZoomSettings)
                                .withShowEstimatedDuration(true)
                                .withWindUp(settings.isWindUp())
                                .build();
                        if (selectedRaceIdentifier == null) {
                            createErrorPage(getStringMessages().couldNotObtainRace(regattaLikeName, raceColumnName,
                                    fleetName, /* technicalErrorMessage */ ""), paywallResolver);
                        } else {
                            getSailingService().getCompetitorBoats(selectedRaceIdentifier,
                                    new AsyncCallback<Map<CompetitorDTO, BoatDTO>>() {
                                        @Override
                                        public void onSuccess(Map<CompetitorDTO, BoatDTO> competitorsAndTheirBoats) {
                                            createEmbeddedMap(selectedRaceIdentifier, competitorsAndTheirBoats,
                                                    raceboardPerspectiveSettings, raceMapSettings, raceDTO);
                                        }

                                        @Override
                                        public void onFailure(Throwable caught) {
                                            reportError(getStringMessages()
                                                    .errorTryingToCreateEmbeddedMap(caught.getMessage()));
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        createErrorPage(getStringMessages().couldNotObtainRace(regattaLikeName, raceColumnName,
                                fleetName, caught.getMessage()), paywallResolver);
                    }
                });
    }
    
    private void createErrorPage(String message, PaywallResolver paywallResolver) {
        final DockLayoutPanel vp = new DockLayoutPanel(Unit.PX);
        final SAPHeaderWithAuthentication header = new SAPSailingHeaderWithAuthentication();
        new FixedSailingAuthentication(getUserService(), paywallResolver, header.getAuthenticationMenuView());
        RootLayoutPanel.get().add(vp);
        vp.addNorth(header, 100);
        final Label infoText = new Label(message);
        infoText.getElement().getStyle().setMargin(1, Unit.EM);
        vp.add(infoText);
        // TODO: Styling of error page slightly differs from the other usages of SAPSailingHeaderWithAuthentication
        // because of the root font-size. Adjustments are postponed because they might affect the whole page content.
    }

    private void createEmbeddedMap(final RegattaAndRaceIdentifier selectedRaceIdentifier,
            final Map<CompetitorDTO, BoatDTO> competitorsAndBoats,
            final RaceBoardPerspectiveOwnSettings raceboardPerspectiveSettings, final RaceMapSettings raceMapSettings,
            SecuredDTO raceDTOProxy) {
        final StringBuilder title = new StringBuilder(contextDefinition.getRegattaLikeName());
        title.append('/');
        title.append(contextDefinition.getRaceColumnName());
        final String fleetName = contextDefinition.getFleetName();
        if (!fleetName.equals(LeaderboardNameConstants.DEFAULT_FLEET_NAME)) {
            title.append('/');
            title.append(fleetName);
        }
        Window.setTitle(title.toString());
        final long refreshInterval = Duration.ONE_SECOND.times(3).asMillis();
        final Timer timer = new Timer(settings.isPlay() ? PlayModes.Live : PlayModes.Replay);
        AsyncActionsExecutor asyncActionsExecutor = new AsyncActionsExecutor();
        final TimeRangeWithZoomProvider timeRangeWithZoomProvider = new TimeRangeWithZoomModel();
        // Use a TimePanel to manage wind chart zoom, although the TimePanel itself is not being displayed;
        // let the time panel always return to "live" mode.
        final TimePanel<TimePanelSettings> timePanel = new TimePanel<TimePanelSettings>(null, null,
                timer, timeRangeWithZoomProvider, getStringMessages(), /* canReplayWhileLive */ false,
                /* isScreenLargeEnoughToOfferChartSupport set to true iff wind chart will be displayed */ raceboardPerspectiveSettings
                        .isShowWindChart(),
                getUserService(), raceDTOProxy) {
            protected boolean isLiveModeToBeMadePossible() {
                return true;
            }
        };
        RaceTimesInfoProvider raceTimesInfoProvider = new RaceTimesInfoProvider(getSailingService(), asyncActionsExecutor, /* errorReporter */ this,
                Collections.singleton(selectedRaceIdentifier), 30000l /* requestInterval*/);
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(new RaceTimesInfoProviderListener() {
            @Override
            public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo,
                    long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest,
                    long clientTimeWhenResponseWasReceived) {
                timer.setLivePlayDelayInMillis(raceTimesInfo.get(selectedRaceIdentifier).delayToLiveInMs);
            }
        });
        final Button backToLivePlayButton = timePanel.getBackToLiveButton();
        timePanel.updateSettings(new TimePanelSettings(refreshInterval));
        raceMapResources.raceMapStyle().ensureInjected();
        final CompetitorColorProvider colorProvider = new CompetitorColorProviderImpl(selectedRaceIdentifier, competitorsAndBoats);
        final RaceCompetitorSelectionProvider competitorSelection;
        if (settings.isShowCompetitors()) {
            competitorSelection = new RaceCompetitorSelectionModel(/* hasMultiSelection */ true, colorProvider, competitorsAndBoats);
        } else {
            competitorSelection = createEmptyFilterCompetitorModel(colorProvider, competitorsAndBoats); // show no competitors
        }
        final PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
        final RaceMap raceMap = new RaceMap(/* parent */ null, /* context */ null, new RaceMapLifecycle(getStringMessages(), paywallResolver, raceDTOProxy),
                raceMapSettings, getSailingService(), asyncActionsExecutor, /* errorReporter */ EmbeddedMapAndWindChartEntryPoint.this, timer,
                competitorSelection, new RaceCompetitorSet(competitorSelection), getStringMessages(), selectedRaceIdentifier,
                raceMapResources, /* showHeaderPanel */ false, new DefaultQuickFlagDataProvider(), paywallResolver,
                /* isSimulationEnabled */ false) {
            @Override
            protected void showAdditionalControls(MapWidget map) {
                backToLivePlayButton.removeFromParent();
                map.setControls(ControlPosition.RIGHT_BOTTOM, backToLivePlayButton);
            }
        };
        final WindChart windChart;
        if (raceboardPerspectiveSettings.isShowWindChart()) {
            windChart = new WindChart(null, null, new WindChartLifecycle(getStringMessages()), getSailingService(),
                    selectedRaceIdentifier, timer,
                    timeRangeWithZoomProvider, new WindChartSettings(), getStringMessages(),
                    asyncActionsExecutor, /* errorReporter */
                    EmbeddedMapAndWindChartEntryPoint.this, /* compactChart */ true);
        } else {
            windChart = null;
        }
        createRaceBoardInOneScreenMode(raceMap, windChart, paywallResolver, raceDTOProxy);
        timeRangeWithZoomProvider.setTimeRange(new MillisecondsTimePoint(timer.getTime()).minus(Duration.ONE_MINUTE.times(15)).asDate(),
                new MillisecondsTimePoint(timer.getTime()).plus(Duration.ONE_MINUTE.times(3)).asDate());
        timer.setTime(timer.getTime().getTime()-1000l);
    }  

    private RaceCompetitorSelectionProvider createEmptyFilterCompetitorModel(CompetitorColorProvider colorProvider, Map<CompetitorDTO, BoatDTO> competitorsAndBoats) {
        final RaceCompetitorSelectionModel result = new RaceCompetitorSelectionModel(/* hasMultiSelection */ true, colorProvider, competitorsAndBoats);
        final FilterSet<CompetitorDTO, Filter<CompetitorDTO>> filterSet = result.getOrCreateCompetitorsFilterSet("Empty");
        filterSet.addFilter(new Filter<CompetitorDTO>() {
            @Override public boolean matches(CompetitorDTO object) { return false; }
            @Override public String getName() { return "Never matching filter"; }
        });
        return result;
    }

    private void createRaceBoardInOneScreenMode(final RaceMap raceMap, final WindChart windChart, PaywallResolver paywallResolver, SecuredDTO dtoContext) {
        final TouchSplitLayoutPanel panel = new TouchSplitLayoutPanel(/* horizontal splitter width */ 3, /* vertical splitter height */ 25, paywallResolver, dtoContext);
        if (windChart != null) {
            panel.insert(windChart.getEntryWidget(), windChart, Direction.SOUTH, DEFAULT_WIND_CHART_HEIGHT);
        }
        final Consumer<Boolean> forceLayoutCallback = hidden -> panel.setWidgetVisibility(windChart.getEntryWidget(),
                windChart, hidden, DEFAULT_WIND_CHART_HEIGHT);
        panel.insert(raceMap.getEntryWidget(), raceMap, Direction.CENTER, 400);
        panel.lastComponentHasBeenAdded(forceLayoutCallback, new AbsolutePanel(), emptyList());
        if (windChart != null) {
            forceLayoutCallback.accept(false);
        }
        panel.addStyleName("dockLayoutPanel");
        RootLayoutPanel.get().add(panel);
    }

    @Override
    public String getLeaderboardName() {
        return contextDefinition.getRegattaLikeName();
    }
}
