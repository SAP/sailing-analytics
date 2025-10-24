package com.sap.sailing.gwt.ui.raceboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType.TrackedRaceActions;
import com.sap.sailing.gwt.common.authentication.SailingAuthenticationEntryPointLinkFactory;
import com.sap.sailing.gwt.common.client.NavigatorUtil;
import com.sap.sailing.gwt.common.client.help.HelpButton;
import com.sap.sailing.gwt.common.client.help.HelpButtonResources;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.raceboard.RaceBoardPerspectiveOwnSettings;
import com.sap.sailing.gwt.settings.client.raceboard.RaceboardContextDefinition;
import com.sap.sailing.gwt.ui.client.CompetitorColorProvider;
import com.sap.sailing.gwt.ui.client.CompetitorColorProviderImpl;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.EntryPointLinkFactory;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.RaceTimePanel;
import com.sap.sailing.gwt.ui.client.RaceTimePanelLifecycle;
import com.sap.sailing.gwt.ui.client.RaceTimePanelSettings;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.media.MediaPlayerLifecycle;
import com.sap.sailing.gwt.ui.client.media.MediaPlayerManager.PlayerChangeListener;
import com.sap.sailing.gwt.ui.client.media.MediaPlayerManagerComponent;
import com.sap.sailing.gwt.ui.client.media.MediaPlayerSettings;
import com.sap.sailing.gwt.ui.client.media.PopupPositionProvider;
import com.sap.sailing.gwt.ui.client.shared.charts.EditMarkPassingsPanel;
import com.sap.sailing.gwt.ui.client.shared.charts.EditMarkPositionPanel;
import com.sap.sailing.gwt.ui.client.shared.charts.MultiCompetitorRaceChart;
import com.sap.sailing.gwt.ui.client.shared.charts.MultiCompetitorRaceChartLifecycle;
import com.sap.sailing.gwt.ui.client.shared.charts.MultiCompetitorRaceChartSettings;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChart;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChartLifecycle;
import com.sap.sailing.gwt.ui.client.shared.charts.WindChartSettings;
import com.sap.sailing.gwt.ui.client.shared.filter.CompetitorsFilterSets;
import com.sap.sailing.gwt.ui.client.shared.filter.FilterWithUI;
import com.sap.sailing.gwt.ui.client.shared.filter.LeaderboardFetcher;
import com.sap.sailing.gwt.ui.client.shared.filter.LeaderboardWithSecurityFetcher;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceCompetitorSet;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapLifecycle;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapResources;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.maneuver.ManeuverTableLifecycle;
import com.sap.sailing.gwt.ui.client.shared.racemap.maneuver.ManeuverTablePanel;
import com.sap.sailing.gwt.ui.client.shared.racemap.maneuver.ManeuverTableSettings;
import com.sap.sailing.gwt.ui.leaderboard.ClassicLeaderboardStyle;
import com.sap.sailing.gwt.ui.leaderboard.CompetitorFilterPanel;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardResources.RaceBoardMainCss;
import com.sap.sailing.gwt.ui.raceboard.tagging.TaggingComponent;
import com.sap.sailing.gwt.ui.shared.RaceWithCompetitorsAndBoatsDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.TrackingConnectorInfoDTO;
import com.sap.sailing.gwt.ui.shared.databylogo.DataByLogo;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.settings.AbstractSettings;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.controls.dropdown.Dropdown;
import com.sap.sse.gwt.client.controls.slider.TimeSlider.BarOverlay;
import com.sap.sse.gwt.client.formfactor.DeviceDetector;
import com.sap.sse.gwt.client.panels.ResizableFlowPanel;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomModel;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.LinkWithSettingsGenerator;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.perspective.AbstractPerspectiveComposite;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.view.AuthenticationMenuView;
import com.sap.sse.security.ui.authentication.view.AuthenticationMenuViewImpl;
import com.sap.sse.security.ui.authentication.view.FlyoutAuthenticationView;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.WithSecurity;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

/**
 * A view showing a list of components visualizing a race from the regattas announced by calls to {@link #fillRegattas(List)}.
 * The race selection is provided by a {@link RaceSelectionProvider} for which this is a {@link RaceSelectionChangeListener listener}.
 * {@link RaceIdentifier}-based race selection changes are converted to {@link RaceDTO} objects using the {@link #racesByIdentifier}
 * map maintained during {@link #fillRegattas(List)}. The race selection provider is expected to be single selection only.
 * 
 * @author Frank Mittag, Axel Uhl (d043530)
 *
 */
public class RaceBoardPanel
        extends AbstractPerspectiveComposite<RaceBoardPerspectiveLifecycle, RaceBoardPerspectiveOwnSettings>
        implements LeaderboardUpdateListener, PopupPositionProvider, RequiresResize {
    public static final String RACEBOARD_PATH = "/gwt/RaceBoard.html";
    private final SailingServiceAsync sailingService;
    private SailingServiceWriteAsync sailingServiceWrite;
    private final MediaServiceAsync mediaService;
    private final MediaServiceWriteAsync mediaServiceWrite;
    private final UUID eventId;
    private final StringMessages stringMessages;
    private final ErrorReporter errorReporter;
    private String raceBoardName;
        
    private final RaceTimePanel racetimePanel;
    private final Timer timer;
    private final UserAgentDetails userAgent;
    private final RaceCompetitorSelectionProvider competitorSelectionProvider;
    private final TimeRangeWithZoomModel timeRangeWithZoomModel; 
    private final RegattaAndRaceIdentifier selectedRaceIdentifier;

    private final String leaderboardName;
    private final SingleRaceLeaderboardPanel leaderboardPanel;
    private WindChart windChart;
    private MultiCompetitorRaceChart competitorChart;
    private MediaPlayerManagerComponent mediaPlayerManagerComponent;
    private EditMarkPassingsPanel editMarkPassingPanel;
    private EditMarkPositionPanel editMarkPositionPanel;
    
    private final TaggingComponent taggingComponent;
    
    private final DockLayoutPanel dockPanel;
    private final ResizableFlowPanel timePanelWrapper;
    private final static int TIMEPANEL_COLLAPSED_HEIGHT = 67;
    private final static int TIMEPANEL_EXPANDED_HEIGHT = 96;
    
    /**
     * The component viewer
     */
    private SideBySideComponentViewer mapViewer;

    private final AsyncActionsExecutor asyncActionsExecutor;
    
    private final RaceTimesInfoProvider raceTimesInfoProvider;
    private final RaceMap raceMap;
    
    private final FlowPanel racePicker;
    private final FlowPanel regattaAndRaceTimeInformationHeader;
    private final AuthenticationMenuView userManagementMenuView;
    private boolean currentRaceHasBeenSelectedOnce;
    
    private final RaceBoardResources raceBoardResources = RaceBoardResources.INSTANCE; 
    private final RaceBoardMainCss mainCss = raceBoardResources.mainCss();
    private final QuickFlagDataFromLeaderboardDTOProvider quickFlagDataProvider;
    private ManeuverTablePanel maneuverTablePanel;

    private static final RaceMapResources raceMapResources = GWT.create(RaceMapResources.class);
    private TrackingConnectorInfoDTO trackingConnectorInfo;
    private CompetitorFilterPanel competitorSearchTextBox;
    private final RaceboardContextDefinition raceboardContextDefinition;

    /**
     * @param eventId
     *            an optional event that can be used for "back"-navigation in case the race board shows a race in the
     *            context of an event; may be <code>null</code>.
     * @param isScreenLargeEnoughToOfferChartSupport
     *            if the screen is large enough to display charts such as the competitor chart or the wind chart, a
     *            padding is provided for the RaceTimePanel that aligns its right border with that of the charts, and
     *            the charts are created. This decision is made once on startup in the {@link RaceBoardEntryPoint}
     *            class.
     * @param showChartMarkEditMediaButtonsAndVideo
     *            if <code>true</code> charts, such as the competitor chart or the wind chart, (as well as edit mark
     *            panels and manage media buttons) are shown and a padding is provided for the RaceTimePanel that aligns
     *            its right border with that of the chart. Otherwise those components will be hidden.
     * @param availableDetailTypes
     *            A list of all Detailtypes, that will be offered in the Settingsdialog. Can be used to hide settings no
     *            data exists for, eg Bravo, Expdition ect.
     * @param trackingConnectorInfo 
     *            Information about the tracking technology provider, that was used to track the race.
     */
    public RaceBoardPanel(Component<?> parent,
            ComponentContext<PerspectiveCompositeSettings<RaceBoardPerspectiveOwnSettings>> componentContext,
            RaceBoardPerspectiveLifecycle lifecycle,
            PerspectiveCompositeSettings<RaceBoardPerspectiveOwnSettings> settings, SailingServiceAsync sailingService,
            MediaServiceAsync mediaService, MediaServiceWriteAsync mediaServiceWrite,
            AsyncActionsExecutor asyncActionsExecutor, Map<CompetitorDTO, BoatDTO> competitorsAndTheirBoats,
            Timer timer, RegattaAndRaceIdentifier selectedRaceIdentifier, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, UUID eventId, ErrorReporter errorReporter,
            final StringMessages stringMessages, UserAgentDetails userAgent,
            RaceTimesInfoProvider raceTimesInfoProvider, boolean showChartMarkEditMediaButtonsAndVideo,
            boolean showHeaderPanel, Iterable<DetailType> availableDetailTypes,
            StrippedLeaderboardDTO leaderboardDTO, final RaceWithCompetitorsAndBoatsDTO raceDTO,
            TrackingConnectorInfoDTO trackingConnectorInfo, SailingServiceWriteAsync sailingServiceWrite,
            RaceboardContextDefinition raceboardContextDefinition,
            WithSecurity withSecurity) {
        super(parent, componentContext, lifecycle, settings);
        this.sailingService = sailingService;
        this.sailingServiceWrite = sailingServiceWrite;
        this.mediaService = mediaService;
        this.mediaServiceWrite = mediaServiceWrite;
        this.stringMessages = stringMessages;
        this.raceTimesInfoProvider = raceTimesInfoProvider;
        this.errorReporter = errorReporter;
        this.userAgent = userAgent;
        this.timer = timer;
        this.eventId = eventId;
        this.trackingConnectorInfo = trackingConnectorInfo;
        this.currentRaceHasBeenSelectedOnce = false;
        this.leaderboardName = leaderboardName;
        this.selectedRaceIdentifier = selectedRaceIdentifier;
        this.setRaceBoardName(selectedRaceIdentifier.getRaceName());
        this.asyncActionsExecutor = asyncActionsExecutor;
        final RaceBoardPerspectiveOwnSettings parsedPerspectiveOwnSettings = settings.getPerspectiveOwnSettings();
        this.raceboardContextDefinition = raceboardContextDefinition;
        FlowPanel mainPanel = new ResizableFlowPanel();
        mainPanel.setSize("100%", "100%");
        racePicker = new FlowPanel();
        racePicker.setStyleName("RegattaRaceInformation-Header");
        regattaAndRaceTimeInformationHeader = new FlowPanel();
        regattaAndRaceTimeInformationHeader.setStyleName("RegattaAndRaceTime-Header");
        regattaAndRaceTimeInformationHeader.getElement().getStyle().setProperty("pointerEvents", "auto");
        Runnable shareLinkAction = null;
        if (raceboardContextDefinition != null) {
            final RaceboardContextDefinition strippedRaceBoardContextDefinition = new RaceboardContextDefinition(
                    raceboardContextDefinition.getRegattaName(), raceboardContextDefinition.getRaceName(),
                    raceboardContextDefinition.getLeaderboardName(), raceboardContextDefinition.getLeaderboardGroupName(),
                    raceboardContextDefinition.getLeaderboardGroupId(), raceboardContextDefinition.getEventId(), null);
            final LinkWithSettingsGenerator<Settings> linkWithSettingsGenerator = new LinkWithSettingsGenerator<>(RACEBOARD_PATH, strippedRaceBoardContextDefinition);
            if (showChartMarkEditMediaButtonsAndVideo) {
                shareLinkAction = () -> {
                    final ShareLinkDialog shareLinkDialog = new ShareLinkDialog(RACEBOARD_PATH, lifecycle,
                            getSettings(), sailingService, stringMessages, linkWithSettingsGenerator);
                    shareLinkDialog.initLinkAndShow();
                };
            } else {
                if (NavigatorUtil.clientHasNavigatorShareSupport()) {
                    shareLinkAction = () -> {
                        NavigatorUtil.shareUrl(linkWithSettingsGenerator.createUrl(getSettings()), null);
                    };
                } else if (NavigatorUtil.clientHasNavigatorCopyToClipboardSupport()) {
                    shareLinkAction = () ->{
                        NavigatorUtil.copyToClipboard(linkWithSettingsGenerator.createUrl(getSettings()));
                    };
                }
            }
        }
        
        this.userManagementMenuView = new AuthenticationMenuViewImpl(new Anchor(), mainCss.usermanagement_loggedin(),
                mainCss.usermanagement_open(), mainCss.user_menu_premium());
        this.userManagementMenuView.asWidget().setStyleName(mainCss.usermanagement_icon());
        this.userManagementMenuView.asWidget().getElement().getStyle().setProperty("pointerEvents", "auto");
        this.userManagementMenuView.asWidget().getElement().getStyle().setProperty("display", "inline-block");
        this.userManagementMenuView.asWidget().getElement().getStyle().setProperty("position", "relative");
        this.userManagementMenuView.asWidget().getElement().getStyle().setProperty("top", "0px");
        this.userManagementMenuView.asWidget().getElement().getStyle().setProperty("right", "0px");
        timeRangeWithZoomModel = new TimeRangeWithZoomModel();
        final CompetitorColorProvider colorProvider = new CompetitorColorProviderImpl(selectedRaceIdentifier, competitorsAndTheirBoats);
        competitorSelectionProvider = new RaceCompetitorSelectionModel(/* hasMultiSelection */ true, colorProvider, competitorsAndTheirBoats);
        raceMapResources.raceMapStyle().ensureInjected();
        final PaywallResolver paywallResolverRace = new PaywallResolverImpl(withSecurity.getUserService(), withSecurity.getSubscriptionServiceFactory());
        RaceMapLifecycle raceMapLifecycle = new RaceMapLifecycle(stringMessages, paywallResolverRace, raceDTO);
        RaceMapSettings defaultRaceMapSettings = settings.findSettingsByComponentId(raceMapLifecycle.getComponentId());
        RaceTimePanelLifecycle raceTimePanelLifecycle = lifecycle.getRaceTimePanelLifecycle();
        RaceTimePanelSettings raceTimePanelSettings = settings
                .findSettingsByComponentId(raceTimePanelLifecycle.getComponentId());
        final RaceCompetitorSet raceCompetitorSet = new RaceCompetitorSet(competitorSelectionProvider);
        quickFlagDataProvider = new QuickFlagDataFromLeaderboardDTOProvider(raceCompetitorSet, selectedRaceIdentifier);
        raceMap = new RaceMap(this, componentContext, raceMapLifecycle, defaultRaceMapSettings, sailingService, asyncActionsExecutor,
                errorReporter, timer,
                competitorSelectionProvider, raceCompetitorSet, stringMessages, selectedRaceIdentifier, 
                raceMapResources, /* showHeaderPanel */ true, quickFlagDataProvider, this::showInWindChart,
                leaderboardName, leaderboardGroupName, leaderboardGroupId, shareLinkAction, paywallResolverRace) {
            private static final String INDENT_SMALL_CONTROL_STYLE = "indentsmall";
            private static final String INDENT_BIG_CONTROL_STYLE = "indentbig";
            @Override
            public void onResize() {
                super.onResize();
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        // Show/hide the leaderboard panels toggle button text based on the race map height
                        mapViewer.setLeftComponentToggleButtonTextVisibilityAndDraggerPosition(raceMap.getOffsetHeight() > 400);
                        mapViewer.setRightComponentToggleButtonTextVisibilityAndDraggerPosition(raceMap.getOffsetHeight() > 400);
                    }
                });
            }
            
            @Override
            protected String getLeftControlsIndentStyle() {
                // Calculate style name for left control indent based on race map height an leaderboard panel visibility
                if (getOffsetHeight() <= 300) {
                    return INDENT_BIG_CONTROL_STYLE;
                }
                if (leaderboardPanel.isVisible() && getOffsetHeight() <= 500) {
                    return INDENT_SMALL_CONTROL_STYLE;
                }
                return super.getLeftControlsIndentStyle();
            }
        };
        // now that the raceMap field has been initialized, check whether the buoy zone radius shall be looked up from
        // the regatta model on the server:
        if (defaultRaceMapSettings.isBuoyZoneRadiusDefaultValue()) {
            sailingService.getRegattaByName(selectedRaceIdentifier.getRegattaName(), new AsyncCallback<RegattaDTO>() {
                @Override
                public void onSuccess(RegattaDTO regattaDTO) {
                    Distance buoyZoneRadius = regattaDTO.getCalculatedBuoyZoneRadius();
                    RaceMapSettings existingMapSettings = raceMap.getSettings();
                    if (existingMapSettings.isBuoyZoneRadiusDefaultValue()
                            && !Util.equalsWithNull(buoyZoneRadius, existingMapSettings.getBuoyZoneRadius())) {
                        final RaceMapSettings newRaceMapSettings = new RaceMapSettings.RaceMapSettingsBuilder(
                                existingMapSettings, regattaDTO, paywallResolverRace)
                                .withBuoyZoneRadius(buoyZoneRadius)
                                .build();
                        raceMap.updateSettings(newRaceMapSettings);
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                }
            });
        }
        competitorSearchTextBox = new CompetitorFilterPanel(competitorSelectionProvider, stringMessages, raceMap,
                new LeaderboardFetcher() {
                    @Override
                    public LeaderboardDTO getLeaderboard() {
                        return leaderboardPanel.getLeaderboard();
                    }
                }, selectedRaceIdentifier, withSecurity.getUserService().getStorage());
        raceMap.getHeaderPanel().add(racePicker);
        final FlowPanel filler = new FlowPanel();
        filler.setStyleName("RaceMap-Header-Filler"); // to create space between race picker and event/time/data-by display
        raceMap.getHeaderPanel().add(filler);
        raceMap.getHeaderPanel().add(regattaAndRaceTimeInformationHeader);
        final FlowPanel userManagementMenuPanel = new FlowPanel();
        userManagementMenuPanel.addStyleName("AuthenticationButton");
        userManagementMenuPanel.add(userManagementMenuView);
        raceMap.getHeaderPanel().add(userManagementMenuPanel);
        addChildComponent(raceMap);
        // add panel for tagging functionality, hidden if no URL parameter "tag" is passed 
        final String sharedTagURLParameter = parsedPerspectiveOwnSettings.getJumpToTag();
        String sharedTagTitle = null;
        TimePoint sharedTagTimePoint = null;
        boolean showTaggingComponent = false;
        if (sharedTagURLParameter != null) {
            showTaggingComponent = true;
            int indexOfSeperator = sharedTagURLParameter.indexOf(",");
            if (indexOfSeperator != -1) {
                try {
                    sharedTagTimePoint = new MillisecondsTimePoint(Long.parseLong(sharedTagURLParameter.substring(0, indexOfSeperator)));
                    sharedTagTitle = sharedTagURLParameter.substring(indexOfSeperator + 1, sharedTagURLParameter.length());
                } catch(NumberFormatException nfe) {
                    GWT.log("Problem extracting tag time point from URL parameter "+TagDTO.TAG_URL_PARAMETER, nfe);
                }
            }
        }
        taggingComponent = new TaggingComponent(parent, componentContext, stringMessages, sailingService, withSecurity.getUserService(), timer,
                raceTimesInfoProvider, sharedTagTimePoint, sharedTagTitle, leaderboardDTO, sailingServiceWrite, selectedRaceIdentifier);
        addChildComponent(taggingComponent);
        taggingComponent.setVisible(showTaggingComponent);
        // Determine if the screen is large enough to initially display the leaderboard panel on the left side of the
        // map based on the initial screen width. Afterwards, the leaderboard panel visibility can be toggled as usual.
        boolean isScreenLargeEnoughToInitiallyDisplayLeaderboard = Document.get().getClientWidth() >= 1024;
        leaderboardPanel = createLeaderboardPanel(lifecycle, settings, leaderboardName, leaderboardGroupName,
                competitorSearchTextBox, availableDetailTypes, withSecurity);
        addChildComponent(leaderboardPanel);
        leaderboardPanel.addVisibilityListener(visible->{
            quickFlagDataProvider.setLeaderboardNotCurrentlyUpdating(!visible);
        });
        leaderboardPanel.setTitle(stringMessages.leaderboard());
        leaderboardPanel.getElement().getStyle().setMarginLeft(6, Unit.PX);
        leaderboardPanel.getElement().getStyle().setMarginTop(10, Unit.PX);
        createOneScreenView(lifecycle, settings, leaderboardName, leaderboardGroupName, leaderboardGroupId, eventId, mainPanel,
                isScreenLargeEnoughToInitiallyDisplayLeaderboard,
                raceMap, withSecurity.getUserService(), showChartMarkEditMediaButtonsAndVideo, leaderboardDTO, raceDTO, paywallResolverRace); // initializes the raceMap field
        leaderboardPanel.addLeaderboardUpdateListener(this);
        raceMap.addMediaPlayerManagerComponent(mediaPlayerManagerComponent);
        // in case the URL configuration contains the name of a competitors filter set we try to activate it
        // FIXME the competitorsFilterSets has now moved to CompetitorSearchTextBox (which should probably be renamed); pass on the parameters to the LeaderboardPanel and see what it does with it
        if (parsedPerspectiveOwnSettings.getActiveCompetitorsFilterSetName() != null) {
            for (FilterSet<CompetitorDTO, FilterWithUI<CompetitorDTO>> filterSet : competitorSearchTextBox.getCompetitorsFilterSets().getFilterSets()) {
                if (filterSet.getName().equals(parsedPerspectiveOwnSettings.getActiveCompetitorsFilterSetName())) {
                    competitorSearchTextBox.updateCompetitorFilterSetAndView(filterSet);
                    break;
                }
            }
        }
        racetimePanel = new RaceTimePanel(this, componentContext, raceTimePanelLifecycle, withSecurity.getUserService(), timer,
                timeRangeWithZoomModel,
                stringMessages, raceTimesInfoProvider, parsedPerspectiveOwnSettings.isCanReplayDuringLiveRaces(),
                showChartMarkEditMediaButtonsAndVideo, selectedRaceIdentifier,
                parsedPerspectiveOwnSettings.getInitialDurationAfterRaceStartInReplay(), raceDTO);
        racetimePanel.updateSettings(raceTimePanelSettings);
        timeRangeWithZoomModel.addTimeZoomChangeListener(racetimePanel);
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(racetimePanel);
        this.timePanelWrapper = createTimePanelLayoutWrapper();
        boolean advanceTimePanelEnabled = true;
        if (advanceTimePanelEnabled) {
            manageTimePanelToggleButton(advanceTimePanelEnabled);
        }
        addChildComponent(racetimePanel);
        final Long zoomStartMillis = parsedPerspectiveOwnSettings.getZoomStart();
        final Long zoomEndMillis = parsedPerspectiveOwnSettings.getZoomEnd();
        if (isScreenLargeEnoughToInitiallyDisplayLeaderboard && zoomStartMillis != null && zoomEndMillis != null) {
            final Date zoomStart = new Date(zoomStartMillis);
            final Date zoomEnd = new Date(zoomEndMillis);
            Scheduler.get().scheduleDeferred(new Command() {
                public void execute () {
                  timeRangeWithZoomModel.setTimeZoom(zoomStart, zoomEnd);
                }
            });
        }
        dockPanel = new DockLayoutPanel(Unit.PX);
        dockPanel.addSouth(timePanelWrapper, TIMEPANEL_COLLAPSED_HEIGHT);
        dockPanel.add(mainPanel);
        dockPanel.addStyleName("dockLayoutPanel");
        initWidget(dockPanel);
    }
    
    /**
     * Creates the overall split pane view with the map in the center and all the components around of it. 
     * Except race time panel on the bottom.
     *  
     * @param event
     *            an optional event; may be <code>null</code> or else can be used to show some context information.
     * @param showChartMarkEditMediaButtonsAndVideo 
     * @param isScreenLargeEnoughToOfferChartSupport
     *            if the screen is large enough to display charts such as the competitor chart or the wind chart, a
     *            padding is provided for the RaceTimePanel that aligns its right border with that of the charts, and
     *            the charts are created.
     */
    private void createOneScreenView(RaceBoardPerspectiveLifecycle lifecycle,
            PerspectiveCompositeSettings<RaceBoardPerspectiveOwnSettings> settings, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, UUID event, FlowPanel mainPanel,
            boolean isScreenLargeEnoughToInitiallyDisplayLeaderboard, RaceMap raceMap, UserService userService,
            boolean showChartMarkEditMediaButtonsAndVideo, StrippedLeaderboardDTO leaderboard,
            final RaceWithCompetitorsAndBoatsDTO raceDTO, PaywallResolver paywallResolver) {
        MediaPlayerLifecycle mediaPlayerLifecycle = getPerspectiveLifecycle().getMediaPlayerLifecycle();
        MediaPlayerSettings mediaPlayerSettings = settings
                .findSettingsByComponentId(mediaPlayerLifecycle.getComponentId());
        WindChartLifecycle windChartLifecycle = getPerspectiveLifecycle().getWindChartLifecycle();
        WindChartSettings windChartSettings = settings.findSettingsByComponentId(windChartLifecycle.getComponentId());
        ManeuverTableLifecycle maneuverTableLifecycle = getPerspectiveLifecycle().getManeuverTable();
        ManeuverTableSettings maneuverTableSettings = settings
                .findSettingsByComponentId(maneuverTableLifecycle.getComponentId());
        MultiCompetitorRaceChartLifecycle multiCompetitorRaceChartLifecycle = getPerspectiveLifecycle()
                .getMultiCompetitorRaceChartLifecycle();
        MultiCompetitorRaceChartSettings multiCompetitorRaceChartSettings = settings
                .findSettingsByComponentId(multiCompetitorRaceChartLifecycle.getComponentId());
        // create the default leaderboard and select the right race
        final RaceBoardPerspectiveOwnSettings initialPerspectiveOwnSettings = settings.getPerspectiveOwnSettings();
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(raceMap);
        List<Pair<Component<?>, Action>> componentsForSideBySideViewer = new ArrayList<>();
        if (showChartMarkEditMediaButtonsAndVideo) {
            competitorChart = new MultiCompetitorRaceChart(this, getComponentContext(),
                    multiCompetitorRaceChartLifecycle,
                    sailingService,
                    asyncActionsExecutor,
                    competitorSelectionProvider, selectedRaceIdentifier, timer, timeRangeWithZoomModel, stringMessages,
                    errorReporter, true, true, leaderboardGroupName, leaderboardGroupId, leaderboardName);
            competitorChart.setVisible(false);
            competitorChart.updateSettings(multiCompetitorRaceChartSettings);
            new SliceRaceHandler(sailingServiceWrite, sailingService, userService, errorReporter, competitorChart, selectedRaceIdentifier,
                    leaderboardGroupName, leaderboardGroupId, leaderboardName, event, leaderboard, raceDTO, stringMessages);
            componentsForSideBySideViewer.add(new Pair<Component<?>, Action>(competitorChart, TrackedRaceActions.VIEWANALYSISCHARTS));
            windChart = new WindChart(this, getComponentContext(), windChartLifecycle, sailingService,
                    selectedRaceIdentifier, timer,
                    timeRangeWithZoomModel,
                    windChartSettings, stringMessages, asyncActionsExecutor, errorReporter, /* compactChart */
                    true);
            windChart.setVisible(false);
            componentsForSideBySideViewer.add(new Pair<Component<?>, Action>(windChart, TrackedRaceActions.VIEWANALYSISCHARTS));
        }
        maneuverTablePanel = new ManeuverTablePanel(this, getComponentContext(), sailingService, asyncActionsExecutor,
                selectedRaceIdentifier, stringMessages, competitorSelectionProvider, errorReporter, timer,
                maneuverTableSettings, timeRangeWithZoomModel, new ClassicLeaderboardStyle(), userService, raceDTO);
        maneuverTablePanel.getEntryWidget().setTitle(stringMessages.maneuverTable());
        if (showChartMarkEditMediaButtonsAndVideo) {
            componentsForSideBySideViewer.add(new Pair<Component<?>, Action>(maneuverTablePanel, TrackedRaceActions.VIEWANALYSISCHARTS));
        }
        editMarkPassingPanel = new EditMarkPassingsPanel(this, getComponentContext(), sailingService,
                sailingServiceWrite,
                selectedRaceIdentifier,
                stringMessages, competitorSelectionProvider, errorReporter, timer);
        if (showChartMarkEditMediaButtonsAndVideo) {
            editMarkPassingPanel.setLeaderboard(leaderboardPanel.getLeaderboard());
            editMarkPassingPanel.getEntryWidget().setTitle(stringMessages.editMarkPassings());
            componentsForSideBySideViewer.add(new Pair<Component<?>, Action>(editMarkPassingPanel, null));
        }
        editMarkPositionPanel = new EditMarkPositionPanel(this, getComponentContext(), raceMap, leaderboardPanel,
                selectedRaceIdentifier,
                leaderboardName, stringMessages, sailingService, timer, timeRangeWithZoomModel,
                asyncActionsExecutor, errorReporter, sailingServiceWrite);
        if (showChartMarkEditMediaButtonsAndVideo) {
            editMarkPositionPanel.setLeaderboard(leaderboardPanel.getLeaderboard());
            componentsForSideBySideViewer.add(new Pair<Component<?>, Action>(editMarkPositionPanel, null));
        }
        mediaPlayerManagerComponent = new MediaPlayerManagerComponent(this, getComponentContext(), mediaPlayerLifecycle,
                sailingServiceWrite, selectedRaceIdentifier, raceTimesInfoProvider, timer, mediaService,
                mediaServiceWrite, userService, stringMessages, errorReporter, userAgent, this, mediaPlayerSettings,
                raceDTO, leaderboardGroupName, event);
        final LeaderboardWithSecurityFetcher asyncFetcher = new LeaderboardWithSecurityFetcher() {
            @Override
            public void getLeaderboardWithSecurity(Consumer<StrippedLeaderboardDTO> consumer) {
                sailingService.getLeaderboardWithSecurity(leaderboardName,
                        new AsyncCallback<StrippedLeaderboardDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(
                                        stringMessages.errorCommunicatingWithServer() + " " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(StrippedLeaderboardDTO result) {
                                consumer.accept(result);
                            }
                        });
            }
        };
        mapViewer = new SideBySideComponentViewer(leaderboardPanel, raceMap, taggingComponent,
                mediaPlayerManagerComponent, componentsForSideBySideViewer, stringMessages, userService,
                editMarkPassingPanel, editMarkPositionPanel, maneuverTablePanel, asyncFetcher, paywallResolver, raceDTO);
        mediaPlayerManagerComponent.addPlayerChangeListener(new PlayerChangeListener() {
            @Override
            public void notifyStateChange() {
                updateRaceTimePanelOverlay();
            }
        });
        addChildComponent(mediaPlayerManagerComponent);
        for (Pair<Component<?>, Action> componentAndAction : componentsForSideBySideViewer) {
            addChildComponent(componentAndAction.getA());
        }
        this.setupUserManagementControlPanel(userService, paywallResolver);
        mainPanel.add(mapViewer.getViewerWidget());
        boolean showLeaderboard = initialPerspectiveOwnSettings.isShowLeaderboard()
                && isScreenLargeEnoughToInitiallyDisplayLeaderboard;
        setLeaderboardVisible(showLeaderboard);
        leaderboardPanel.setAutoExpandPreSelected(initialPerspectiveOwnSettings.isAutoExpandPreSelectedRace());
        if (showChartMarkEditMediaButtonsAndVideo) {
            // TODO bug5672 Shift to Settings based handling of these permission checks, instead of manually checking
            final boolean hasAnalasysChartPermission = paywallResolver
                    .hasPermission(TrackedRaceActions.VIEWANALYSISCHARTS, raceDTO);
            setWindChartVisible(hasAnalasysChartPermission && initialPerspectiveOwnSettings.isShowWindChart());
            setCompetitorChartVisible(hasAnalasysChartPermission && initialPerspectiveOwnSettings.isShowCompetitorsChart());
            setManeuverTableVisible(hasAnalasysChartPermission && initialPerspectiveOwnSettings.isShowManeuver());
            setTagPanelVisible(initialPerspectiveOwnSettings.isShowTags());
        }
        // make sure to load leaderboard data for filtering to work
        if (!showLeaderboard) {
            leaderboardPanel.setVisible(true);
            leaderboardPanel.setVisible(false);
        }
    }
    
    protected void updateRaceTimePanelOverlay() {
        ArrayList<BarOverlay> overlays = new ArrayList<BarOverlay>();
        Set<MediaTrack> videoPlaying = mediaPlayerManagerComponent.getPlayingVideoTracks();
        Set<MediaTrackWithSecurityDTO> audioPlaying = mediaPlayerManagerComponent.getPlayingAudioTrack();
        for (MediaTrack track : mediaPlayerManagerComponent.getAssignedMediaTracks()) {
            final double start = track.startTime.asMillis();
            final TimePoint endTp = track.deriveEndTime();
            // set to max value if null
            final double end = endTp == null ? Double.MAX_VALUE : endTp.asMillis();
            final boolean isPlaying = videoPlaying.contains(track) || audioPlaying.contains(track);
            overlays.add(new BarOverlay(start, end, isPlaying, track.title));
        }
        racetimePanel.setBarOverlays(overlays);
    }

    private void setupUserManagementControlPanel(UserService userService, PaywallResolver paywallResolver) {
        mainCss.ensureInjected();
        final FlyoutAuthenticationView display = new RaceBoardAuthenticationView();
        final GenericAuthentication genericAuthentication = new GenericAuthentication(userService, paywallResolver, userManagementMenuView, display, 
                SailingAuthenticationEntryPointLinkFactory.INSTANCE, raceBoardResources);
        new RaceBoardLoginHintPopup(genericAuthentication.getAuthenticationManager());
    }

    @SuppressWarnings("unused")
    private <SettingsType extends AbstractSettings> void addSettingsMenuItem(MenuBar settingsMenu, final Component<SettingsType> component) {
        if (component.hasSettings()) {
            MenuItem settingsMenuItem = settingsMenu.addItem(component.getLocalizedShortName(), new Command() {
                public void execute() {
                    new SettingsDialog<SettingsType>(component, stringMessages).show();
                  }
            });
        }
    }
    
    private SingleRaceLeaderboardPanel createLeaderboardPanel(RaceBoardPerspectiveLifecycle lifecycle,
            PerspectiveCompositeSettings<RaceBoardPerspectiveOwnSettings> settings, String leaderboardName,
            String leaderboardGroupName, CompetitorFilterPanel competitorSearchTextBox,
            Iterable<DetailType> availableDetailTypes, WithSecurity withSecurity) {
        SingleRaceLeaderboardPanelLifecycle leaderboardPanelLifecycle = getPerspectiveLifecycle().getLeaderboardPanelLifecycle();
        SingleRaceLeaderboardSettings leaderboardSettings = settings
                .findSettingsByComponentId(leaderboardPanelLifecycle.getComponentId());
        return new SingleRaceLeaderboardPanel(this, getComponentContext(), sailingService, asyncActionsExecutor,
                leaderboardSettings, selectedRaceIdentifier != null, selectedRaceIdentifier,
                competitorSelectionProvider, timer, leaderboardGroupName, leaderboardName, errorReporter, stringMessages,
                /* showRaceDetails */ true, competitorSearchTextBox,
                /* showSelectionCheckbox */ true, raceTimesInfoProvider, /* autoExpandLastRaceColumn */ false,
                /* don't adjust the timer's delay from the leaderboard; control it solely from the RaceTimesInfoProvider */ false,
                /* autoApplyTopNFilter */ false, /* showCompetitorFilterStatus */ false, /* enableSyncScroller */ false,
                new ClassicLeaderboardStyle(), FlagImageResolverImpl.get(), availableDetailTypes, withSecurity);
    }

    private void setComponentVisible(SideBySideComponentViewer componentViewer, Component<?> component, boolean visible) {
        component.setVisible(visible);
        componentViewer.forceLayout();
    }
    
    SingleRaceLeaderboardPanel getLeaderboardPanel() {
        return leaderboardPanel;
    }
    
    MultiCompetitorRaceChart getCompetitorChart() {
        return competitorChart;
    }
    
    WindChart getWindChart() {
        return windChart;
    }
    
    RaceTimePanel getRaceTimePanel() {
        return racetimePanel;
    }
    
    Timer getTimer() {
        return timer;
    }
    
    RaceMap getMap() {
        return raceMap;
    }
    
    RegattaAndRaceIdentifier getSelectedRaceIdentifier() {
        return selectedRaceIdentifier;
    }
    
    CompetitorSelectionProvider getCompetitorSelectionProvider() {
        return competitorSelectionProvider;
    }
    
    /**
     * Sets the collapsable panel for the leaderboard open or close, if in <code>CASCADE</code> view mode.<br />
     * Displays or hides the leaderboard, if in <code>ONESCREEN</code> view mode.<br /><br />
     * 
     * The race board should be completely rendered before this method is called, or a few exceptions could be thrown.
     * 
     * @param visible <code>true</code> if the leaderboard shall be open/visible
     */
    public void setLeaderboardVisible(boolean visible) {
        setComponentVisible(mapViewer, leaderboardPanel, visible);
    }
    
    /**
     * Sets the collapsable panel for the tagging open or close, if in <code>CASCADE</code> view mode.<br />
     * Displays or hides the tagging panel, if in <code>ONESCREEN</code> view mode.<br /><br />
     * 
     * The race board should be completely rendered before this method is called, or a few exceptions could be thrown.
     * 
     * @param visible <code>true</code> if the leaderboard shall be open/visible
     */
    public void setTaggingPanelVisible(boolean visible) {
        setComponentVisible(mapViewer, taggingComponent, visible);
    }

    /**
     * Sets the collapsable panel for the wind chart open or close, if in <code>CASCADE</code> view mode.<br />
     * Displays or hides the wind chart, if in <code>ONESCREEN</code> view mode.<br /><br />
     * 
     * The race board should be completely rendered before this method is called, or a few exceptions could be thrown.
     * 
     * @param visible <code>true</code> if the wind chart shall be open/visible
     */
    public void setWindChartVisible(boolean visible) {
        setComponentVisible(mapViewer, windChart, visible);
    }
    
    public void showInWindChart(WindSource windprovider) {
        setComponentVisible(mapViewer, windChart, true);
        windChart.showProvider(windprovider);
    }

    /**
     * Sets the collapsable panel for the competitor chart open or close, if in <code>CASCADE</code> view mode.<br />
     * Displays or hides the competitor chart, if in <code>ONESCREEN</code> view mode.<br /><br />
     * 
     * The race board should be completely rendered before this method is called, or a few exceptions could be thrown.
     * 
     * @param visible <code>true</code> if the competitor chart shall be open/visible
     */
    public void setCompetitorChartVisible(boolean visible) {
        setComponentVisible(mapViewer, competitorChart, visible);
    }
    
    public void setTagPanelVisible(boolean visible) {
        setComponentVisible(mapViewer, taggingComponent, visible);
    }
    
    public void setManeuverTableVisible(boolean visible) {
        setComponentVisible(mapViewer, maneuverTablePanel, visible);
    }
    
    protected SailingServiceAsync getSailingService() {
        return sailingService;
    }

    protected String getRaceBoardName() {
        return raceBoardName;
    }

    protected void setRaceBoardName(String raceBoardName) {
        this.raceBoardName = raceBoardName;
    }

    protected ErrorReporter getErrorReporter() {
        return errorReporter;
    }
    
    @Override
    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
        if (editMarkPassingPanel != null) {
            editMarkPassingPanel.setLeaderboard(leaderboard);
        }
        if (editMarkPositionPanel != null) {
            editMarkPositionPanel.setLeaderboard(leaderboard);
        }
        quickFlagDataProvider.updateFlagData(leaderboard);
    }
    
    private Dropdown createRaceDropDown(final RaceColumnDTO raceColumnOfSelectedRace, final FleetDTO fleetOfSelectedRace) {
        final Dropdown result = new Dropdown(RaceboardDropdownResources.INSTANCE);
        for (final RaceColumnDTO raceColumn : getLeaderboardPanel().getLeaderboard().getRaceList()) {
            for (final FleetDTO fleet : raceColumn.getFleets()) {
                final RaceIdentifier raceIdentifier = raceColumn.getRaceIdentifier(fleet);
                if (raceIdentifier != null) {
                    final String displayName = (LeaderboardNameConstants.DEFAULT_SERIES_NAME.equals(raceColumn.getSeriesName())?"":(raceColumn.getSeriesName()+"/\u200b"))
                            +raceColumn.getName()
                            +(LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleet.getName())?"":("/\u200b"+fleet.getName()));
                    final boolean selected = raceColumn.equals(raceColumnOfSelectedRace) && fleet.equals(fleetOfSelectedRace);
                    result.addItem(displayName, /* link */ null, selected, ()->{
                        final RaceboardContextDefinition strippedRaceBoardContextDefinition = new RaceboardContextDefinition(
                                raceIdentifier.getRegattaName(), raceIdentifier.getRaceName(),
                                getLeaderboardPanel().getLeaderboard().getName(), raceboardContextDefinition.getLeaderboardGroupName(),
                                raceboardContextDefinition.getLeaderboardGroupId(), raceboardContextDefinition.getEventId(), null);
                        final LinkWithSettingsGenerator<Settings> linkWithSettingsGenerator = new LinkWithSettingsGenerator<>(RACEBOARD_PATH, strippedRaceBoardContextDefinition);
                        final String url = linkWithSettingsGenerator.createUrl(getSettings()); // launch with the same settings as the current race
                        Window.open(url, raceIdentifier.toString(), "");
                        result.hide();
                    });
                    if (selected) {
                        // select the race selected for this RaceBoardPanel
                        result.setDisplayedText(displayName);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void currentRaceSelected(RaceIdentifier raceIdentifier, RaceColumnDTO raceColumn) {
        if (!currentRaceHasBeenSelectedOnce) {
            final FleetDTO fleet = raceColumn.getFleet(raceIdentifier);
            final Dropdown raceDropDown = createRaceDropDown(raceColumn, fleet);
            final Label raceNameLabel = new Label(stringMessages.race() + " " + raceColumn.getRaceColumnName());
            raceNameLabel.setStyleName("RaceName-Label");
            racePicker.clear();
            racePicker.add(raceDropDown);
            final Anchor regattaNameAnchor = new Anchor(raceIdentifier.getRegattaName());
            regattaNameAnchor.setTitle(raceIdentifier.getRegattaName());
            if (eventId != null) {
                String link = EntryPointLinkFactory.createRacesTabLink(eventId.toString(), leaderboardName);
                regattaNameAnchor.setHref(link);
            } else {
                String leaderboardGroupNameParam = getLeaderboardGroupName();
                if (leaderboardGroupNameParam != null) {
                    Map<String, String> leaderboardGroupLinkParameters = new HashMap<String, String>();
                    leaderboardGroupLinkParameters.put("showRaceDetails", "true");
                    leaderboardGroupLinkParameters.put("leaderboardGroupName", leaderboardGroupNameParam);
                    String leaderBoardGroupLink = EntryPointLinkFactory.createLeaderboardGroupLink(leaderboardGroupLinkParameters);
                    regattaNameAnchor.setHref(leaderBoardGroupLink); 
                } else {
                    // fallback 
                    regattaNameAnchor.setHref("javascript:window.history.back();"); 
                }
            }
            regattaNameAnchor.setStyleName("RegattaName-Anchor");
            final Label raceTimeLabel = computeRaceInformation(raceColumn, fleet);
            raceTimeLabel.setStyleName("RaceTime-Label");
            regattaAndRaceTimeInformationHeader.clear();
            final FlowPanel helpButtonAndRaceTimePanel = new FlowPanel();
            helpButtonAndRaceTimePanel.setStyleName("Help-And-RaceTime");
            final HelpButton helpButton = new HelpButton(HelpButtonResources.INSTANCE,
                    stringMessages.videoGuide(), "https://wiki.sapsailing.com/wiki/howto/tutorials/sailinganalytics/tracking-race-player.md");
            if (!DeviceDetector.isMobile()) {
                helpButtonAndRaceTimePanel.add(helpButton);
            }
            helpButtonAndRaceTimePanel.add(raceTimeLabel);
            regattaAndRaceTimeInformationHeader.add(regattaNameAnchor);
            regattaAndRaceTimeInformationHeader.add(helpButtonAndRaceTimePanel);
            final DataByLogo dataByLogo = new DataByLogo();
            dataByLogo.setUp(trackingConnectorInfo == null ? Collections.emptySet()
                    : Collections.singleton(trackingConnectorInfo), /** colorIfPossible **/ false, /** enforceTextColor **/ true);
            if (dataByLogo.isVisible()) {
                regattaAndRaceTimeInformationHeader.addStyleName("RegattaAndRaceTime-Header_with_databy");
            }
            regattaAndRaceTimeInformationHeader.add(dataByLogo);
            currentRaceHasBeenSelectedOnce = true;
            taggingComponent.updateRace(leaderboardName, raceColumn, fleet);
        }
    }

    private String getLeaderboardGroupName() {
        return raceboardContextDefinition == null ? null : raceboardContextDefinition.getLeaderboardGroupName();
    }

    @Override
    public UIObject getXPositionUiObject() {
        return racetimePanel;
    }

    @Override
    public UIObject getYPositionUiObject() {
        return racetimePanel;
    }

    private Label computeRaceInformation(RaceColumnDTO raceColumn, FleetDTO fleet) {
        final Date startDate = raceColumn.getStartDate(fleet);
        Label raceInformationLabel = new Label();
        raceInformationLabel.setStyleName("Race-Time-Label");
        if (startDate != null) {
            DateTimeFormat formatter = DateTimeFormat.getFormat("E d/M/y");
            raceInformationLabel.setText(formatter.format(startDate));
        }
        return raceInformationLabel;
    }
    
    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public String getDependentCssClassName() {
        return "";
    }

    private void manageTimePanelToggleButton(boolean advanceTimePanelEnabled) {
        final Button toggleButton = getRaceTimePanel().getAdvancedToggleButton();
        if (advanceTimePanelEnabled) {
            toggleButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    boolean advancedModeShown = getRaceTimePanel().toggleAdvancedMode();
                    if (advancedModeShown) {
                        dockPanel.setWidgetSize(timePanelWrapper, TIMEPANEL_EXPANDED_HEIGHT);
                        toggleButton.removeStyleDependentName("Closed");
                        toggleButton.addStyleDependentName("Open");
                    } else {
                        dockPanel.setWidgetSize(timePanelWrapper, TIMEPANEL_COLLAPSED_HEIGHT);
                        toggleButton.addStyleDependentName("Closed");
                        toggleButton.removeStyleDependentName("Open");
                    }
                }
            });
        } else {
            toggleButton.setVisible(false);
        }
    }
    
    private ResizableFlowPanel createTimePanelLayoutWrapper() {
        ResizableFlowPanel timeLineInnerBgPanel = new ResizableFlowPanel();
        timeLineInnerBgPanel.addStyleName("timeLineInnerBgPanel");
        timeLineInnerBgPanel.add(getRaceTimePanel());
        ResizableFlowPanel timeLineInnerPanel = new ResizableFlowPanel();
        timeLineInnerPanel.add(timeLineInnerBgPanel);
        timeLineInnerPanel.addStyleName("timeLineInnerPanel");
        ResizableFlowPanel timelinePanel = new ResizableFlowPanel();
        timelinePanel.add(timeLineInnerPanel);
        timelinePanel.addStyleName("timeLinePanel");
        return timelinePanel;
    }

    @Override
    public SettingsDialogComponent<RaceBoardPerspectiveOwnSettings> getPerspectiveOwnSettingsDialogComponent() {
        return new RaceBoardPerspectiveSettingsDialogComponent(getPerspectiveSettings(), stringMessages);
    }

    @Override
    protected RaceBoardPerspectiveOwnSettings getPerspectiveSettings() {
        final RaceBoardPerspectiveOwnSettings initialSettings = super.getPerspectiveSettings();
        final CompetitorsFilterSets leaderboardFiterPanelFilterSets = competitorSearchTextBox
                .getCompetitorsFilterSets();
        final FilterSet<CompetitorDTO, FilterWithUI<CompetitorDTO>> activeFilterSet = leaderboardFiterPanelFilterSets
                .getActiveFilterSet();
        final String activeCompetitorsFilterSetName = activeFilterSet == null ? null : activeFilterSet.getName();
        final Set<String> selectedCompetitorIds = new HashSet<>();
        final Duration newInitialDurationAfterRaceStartInReplay;
        if (timer != null && racetimePanel != null) {
            final Date currentTimerDate = timer.getTime();
            final Date startOfRace = racetimePanel.getLastRaceTimesInfo().startOfRace;
            newInitialDurationAfterRaceStartInReplay = startOfRace == null ? null :
                new MillisecondsDurationImpl(currentTimerDate.getTime() - startOfRace.getTime());
        } else {
            newInitialDurationAfterRaceStartInReplay = initialSettings.getInitialDurationAfterRaceStartInReplay();
        }
        for (final CompetitorDTO competitorDTO : getCompetitorSelectionProvider().getSelectedCompetitors()) {
            selectedCompetitorIds.add(competitorDTO.getIdAsString());
        }
        final Pair<Date, Date> timeZoom = timeRangeWithZoomModel.getTimeZoom();
        Long zoomStartInMillis = null;
        Long zoomEndInMillis = null;
        if(timeZoom.getA() != null && timeZoom.getB() != null) {
            zoomStartInMillis = timeZoom.getA().getTime();
            zoomEndInMillis = timeZoom.getB().getTime();
        }
        final boolean isCompetitorChartVisible = competitorChart == null ? false : competitorChart.isVisible();
        final boolean isWindChartVisible = windChart == null ? false : windChart.isVisible();
        final boolean isManeuverTableVisible = maneuverTablePanel == null ? false : maneuverTablePanel.isVisible();
        final boolean autoExpandPreSelectedRace = leaderboardPanel.isAutoExpandPreSelectedRace();
        final RaceBoardPerspectiveOwnSettings raceBoardPerspectiveOwnSettings = new RaceBoardPerspectiveOwnSettings(
                activeCompetitorsFilterSetName, leaderboardPanel.isVisible(), isWindChartVisible,
                isCompetitorChartVisible, initialSettings.isCanReplayDuringLiveRaces(),
                newInitialDurationAfterRaceStartInReplay, /* legacy single selectedCompetitor */ null,
                selectedCompetitorIds, taggingComponent.isVisible(), isManeuverTableVisible,
                initialSettings.getJumpToTag(), zoomStartInMillis, zoomEndInMillis, autoExpandPreSelectedRace);
        return raceBoardPerspectiveOwnSettings;
    }
    
    public RaceBoardPerspectiveOwnSettings getOriginalPerspectiveSettings() {
        return super.getPerspectiveSettings();
    }
    
    @Override
    public boolean hasPerspectiveOwnSettings() {
        return true;
    }

    @Override
    public void onResize() {
        dockPanel.onResize();
    }

    @Override
    public String getId() {
        return RaceBoardPerspectiveLifecycle.ID;
    }

}

