package com.sap.sailing.gwt.ui.adminconsole.places;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.security.SecuredDomainType.TrackedRaceActions;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.ui.adminconsole.AIAgentConfigurationPanel;
import com.sap.sailing.gwt.ui.adminconsole.AIAgentConfigurationPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.BoatPanel;
import com.sap.sailing.gwt.ui.adminconsole.BoatPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.CompetitorPanel;
import com.sap.sailing.gwt.ui.adminconsole.CompetitorPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.CourseTemplatePanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.DeviceConfigurationPanel;
import com.sap.sailing.gwt.ui.adminconsole.DeviceConfigurationPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.EventManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.EventManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.ExpeditionDeviceConfigurationsPanel;
import com.sap.sailing.gwt.ui.adminconsole.ExpeditionDeviceConfigurationsPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.FileStoragePanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.IgtimiDevicesPanel;
import com.sap.sailing.gwt.ui.adminconsole.IgtimiDevicesPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardConfigPanel;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardConfigPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardGroupConfigPanel;
import com.sap.sailing.gwt.ui.adminconsole.LeaderboardGroupConfigPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.LocalServerManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.LocalServerManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.MarkPropertiesPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.MarkRolePanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.MarkTemplatePanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.MasterDataImportPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.MediaPanel;
import com.sap.sailing.gwt.ui.adminconsole.MediaPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.RaceCourseManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.RaceCourseManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.RegattaManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.RegattaManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.RemoteServerInstancesManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.RemoteServerInstancesManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.ReplicationPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.ResultImportUrlsListComposite;
import com.sap.sailing.gwt.ui.adminconsole.ResultImportUrlsListCompositeSupplier;
import com.sap.sailing.gwt.ui.adminconsole.RoleDefinitionsPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.SmartphoneTrackingEventManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.SmartphoneTrackingEventManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.StructureImportManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.StructureImportManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.SwissTimingEventManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.SwissTimingEventManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.SwissTimingReplayConnectorPanel;
import com.sap.sailing.gwt.ui.adminconsole.SwissTimingReplayConnectorPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.TracTracEventManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.TracTracEventManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.TrackedRacesManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.TrackedRacesManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.UserGroupManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.UserManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.WindPanel;
import com.sap.sailing.gwt.ui.adminconsole.WindPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.YellowBrickEventManagementPanel;
import com.sap.sailing.gwt.ui.adminconsole.YellowBrickEventManagementPanelSupplier;
import com.sap.sailing.gwt.ui.adminconsole.coursecreation.CourseTemplatePanel;
import com.sap.sailing.gwt.ui.adminconsole.coursecreation.MarkPropertiesPanel;
import com.sap.sailing.gwt.ui.adminconsole.coursecreation.MarkRolePanel;
import com.sap.sailing.gwt.ui.adminconsole.coursecreation.MarkTemplatePanel;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.FileStoragePlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.LocalServerPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.MasterDataImportPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.RemoteServerInstancesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.ReplicationPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.RolesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.UserGroupManagementPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.UserManagementPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.aiagent.AIAgentConfigurationPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.ExpeditionDeviceConfigurationsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.IgtimiDevicesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.Manage2SailRegattaStructureImportPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.ResultImportUrlsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.SmartphoneTrackingPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.SwissTimingArchivedEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.SwissTimingEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.TracTracEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.YellowBrickEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.CourseTemplatesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.MarkPropertiesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.MarkRolesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.MarkTemplatesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.events.EventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.leaderboards.LeaderboardGroupsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.leaderboards.LeaderboardsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.racemanager.DeviceConfigurationPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.regattas.RegattasPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.AudioAndVideoPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.BoatsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.CompetitorsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.CourseLayoutPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.TrackedRacesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.WindPlace;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.masterdataimport.MasterDataImportPanel;
import com.sap.sailing.gwt.ui.shared.SecurityStylesheetResources;
import com.sap.sailing.landscape.ui.client.LandscapeManagementPanel;
import com.sap.sailing.landscape.ui.client.LandscapeManagementPanelSupplier;
import com.sap.sailing.landscape.ui.client.LandscapeManagementPlace;
import com.sap.sse.gwt.adminconsole.AbstractAdminConsolePlace;
import com.sap.sse.gwt.adminconsole.AdminConsolePanel;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.DefaultRefreshableAdminConsolePanel;
import com.sap.sse.gwt.adminconsole.ReplicationPanel;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.client.controls.filestorage.FileStoragePanel;
import com.sap.sse.gwt.client.panels.HorizontalTabLayoutPanel;
import com.sap.sse.landscape.common.shared.SecuredLandscapeTypes;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.ui.authentication.decorator.AuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.generic.GenericAuthorizedContentDecorator;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.RoleDefinitionsPanel;
import com.sap.sse.security.ui.client.component.UserGroupManagementPanel;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.client.usermanagement.UserManagementPanel;

public class AdminConsoleViewImpl extends Composite implements AdminConsoleView {

    interface AdminConsoleViewUiBinder extends UiBinder<Widget, AdminConsoleViewImpl> {
    }
    private static AdminConsoleViewUiBinder uiBinder = GWT.create(AdminConsoleViewUiBinder.class);
    private final AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
    
    private static final String ADVANCED = "AdvancedTab";
    private static final String CONNECTORS = "TrackingProviderPanel";
    private static final String COURSE_CREATION = "CourseCreationTab";
    private static final String LEADERBOARDS = "LeaderboardPanel";
    private static final String RACES = "RacesPanel";
    private static final String RACE_COMMITEE = "RaceCommiteeAppPanel";
    private static final String AI_AGENT = "AIAgentPanel";

    @UiField
    HeaderPanel headerPanel;
    
    private Presenter presenter;
    private final StringMessages stringMessages = StringMessages.INSTANCE;

    private UserService userService;
    private PaywallResolver paywallResolver;

    private ErrorReporter errorReporter;

    private AdminConsolePanel<AbstractAdminConsolePlace> adminConsolePanel;
    private PlaceController placeController;

    private AbstractAdminConsolePlace defaultPlace;

    public AdminConsoleViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    @Override
    public void setPresenter(final Presenter presenter) {
        this.presenter = presenter;
        this.userService = presenter.getUserService();
        this.errorReporter = presenter.getErrorReporter();
        this.placeController = presenter.getPlaceController();
        this.paywallResolver = new PaywallResolverImpl(this.userService, presenter.getSubscriptionServiceFactory());
    }

    @Override
    public HeaderPanel createUI(final ServerInfoDTO serverInfo) {
        SAPSailingHeaderWithAuthentication header = new SAPSailingHeaderWithAuthentication(stringMessages.administration());
        GenericAuthentication genericSailingAuthentication = new FixedSailingAuthentication(userService, paywallResolver, header.getAuthenticationMenuView());
        AuthorizedContentDecorator authorizedContentDecorator = new GenericAuthorizedContentDecorator(genericSailingAuthentication);
        authorizedContentDecorator.setContentWidgetFactory(() -> createAdminConsolePanel(serverInfo));
        headerPanel.setHeaderWidget(header);
        headerPanel.setContentWidget(authorizedContentDecorator);
        return headerPanel;
    }
    
    @Override
    public void selectTabByPlace(AbstractAdminConsolePlace place) {
        adminConsolePanel.selectTabByPlace(place, true);
    }
    
    private AdminConsolePanel<AbstractAdminConsolePlace> createAdminConsolePanel(final ServerInfoDTO serverInfo) {
        adminConsolePanel = new AdminConsolePanel<>(userService,
                serverInfo, stringMessages.releaseNotes(), "/release_notes_admin.html", null, errorReporter,
                SecurityStylesheetResources.INSTANCE.css(), stringMessages, placeController);
        adminConsolePanel.addStyleName("adminConsolePanel");
        /* EVENTS */
        final EventManagementPanelSupplier eventManagementPanelSupplier = new EventManagementPanelSupplier(stringMessages, presenter, placeController);
        adminConsolePanel.addToVerticalTabPanel(new DefaultRefreshableAdminConsolePanel<EventManagementPanel>(eventManagementPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                presenter.getEventsRefresher().callFillAndReloadInitially(getWidget().getEventsDisplayer());
            }
        }, stringMessages.events(), new EventsPlace((String) null /* no place token */), SecuredDomainType.EVENT.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* REGATTAS */
        final RegattaManagementPanelSupplier regattaManagementPanelSupplier = new RegattaManagementPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToVerticalTabPanel(new DefaultRefreshableAdminConsolePanel<RegattaManagementPanel>(regattaManagementPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                presenter.getRegattasRefresher().callFillAndReloadInitially(getWidget().getRegattasDisplayer());
            }
        }, stringMessages.regattas(), new RegattasPlace((String) null /* no place token */), SecuredDomainType.REGATTA.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* LEADERBOARDS */
        final HorizontalTabLayoutPanel leaderboardTabPanel = adminConsolePanel.addVerticalTab(stringMessages.leaderboards(), LEADERBOARDS);
        /* Leaderboard */
        final LeaderboardConfigPanelSupplier leaderboardConfigPanelSupplier = new LeaderboardConfigPanelSupplier(
                stringMessages, presenter, true);
        adminConsolePanel.addToTabPanel(leaderboardTabPanel, new DefaultRefreshableAdminConsolePanel<LeaderboardConfigPanel>(leaderboardConfigPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                if (getWidget() != null) {
                    presenter.getLeaderboardsRefresher().callFillAndReloadInitially(getWidget().getLeaderboardsDisplayer());
                }
            }
        }, stringMessages.leaderboards(), new LeaderboardsPlace((String) null /* no place token */), SecuredDomainType.LEADERBOARD.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* Leaderboard Group */
        final LeaderboardGroupConfigPanelSupplier leaderboardGroupConfigPanelSupplier = new LeaderboardGroupConfigPanelSupplier(
                stringMessages, presenter);
        adminConsolePanel.addToTabPanel(leaderboardTabPanel, new DefaultRefreshableAdminConsolePanel<LeaderboardGroupConfigPanel>(leaderboardGroupConfigPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                presenter.getLeaderboardGroupsRefresher().callFillAndReloadInitially(getWidget().getLeaderboardGroupsDisplayer());
            }
        }, stringMessages.leaderboardGroups(), new LeaderboardGroupsPlace((String) null /* no place token */), SecuredDomainType.LEADERBOARD_GROUP.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* RACES */
        final HorizontalTabLayoutPanel racesTabPanel = adminConsolePanel.addVerticalTab(stringMessages.trackedRaces(), RACES);
        /* Tracked races */
        final TrackedRacesManagementPanelSupplier trackedRacesManagementPanelSupplier = new TrackedRacesManagementPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(racesTabPanel,
                new DefaultRefreshableAdminConsolePanel<TrackedRacesManagementPanel>(trackedRacesManagementPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        presenter.getRegattasRefresher().callFillAndReloadInitially(getWidget().getRegattasDisplayer());
                    }
                }, stringMessages.trackedRaces(), new TrackedRacesPlace((String) null /* no place token */),
                SecuredDomainType.TRACKED_RACE.getPermission(TrackedRaceActions.MUTATION_ACTIONS));
        /* Competitor */
        final CompetitorPanelSupplier competitorPanelSupplier = new CompetitorPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<CompetitorPanel>(competitorPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                if (getWidget() != null) {
                    presenter.getCompetitorsRefresher().callFillAndReloadInitially(getWidget().getCompetitorsDisplayer());
                }
            }
        }, stringMessages.competitors(), new CompetitorsPlace(null),
                SecuredDomainType.COMPETITOR.getPermission(DefaultActions.MUTATION_ACTIONS_FOR_NON_DELETABLE_TYPES));
        /* Boat */
        final BoatPanelSupplier boatPanelSupplier = new BoatPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<BoatPanel>(boatPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                if (getWidget() != null) {
                    presenter.getBoatsRefresher().callFillAndReloadInitially(getWidget().getBoatsDisplayer());
                }
            }
        }, stringMessages.boats(), new BoatsPlace((String) null /* no place token */),
                SecuredDomainType.BOAT.getPermission(DefaultActions.MUTATION_ACTIONS_FOR_NON_DELETABLE_TYPES));
        /* Race */
        final RaceCourseManagementPanelSupplier raceCourseManagementPanelSupplier =
                new RaceCourseManagementPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(racesTabPanel,
                new DefaultRefreshableAdminConsolePanel<RaceCourseManagementPanel>(raceCourseManagementPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        presenter.getRegattasRefresher().callFillAndReloadInitially(getWidget().getRegattasDisplayer());
                    }
                }, stringMessages.courseLayout(), new CourseLayoutPlace((String) null /* no place token */),
                SecuredDomainType.TRACKED_RACE.getPermission(DefaultActions.UPDATE));
        /* Wind */
        final WindPanelSupplier windPanelSupplier = new WindPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<WindPanel>(windPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                presenter.getRegattasRefresher().callFillAndReloadInitially(getWidget().getRegattasDisplayer());
            }
        }, stringMessages.wind(), new WindPlace((String) null /* no place token */), SecuredDomainType.TRACKED_RACE.getPermission(DefaultActions.UPDATE));
        /* Media */
        final MediaPanelSupplier mediaPanelSupplier = new MediaPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<MediaPanel>(mediaPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                if (getWidget() != null) {
                    getWidget().onShow();
                }
            }
        }, stringMessages.mediaPanel(), new AudioAndVideoPlace((String) null /* no place token */),
                SecuredDomainType.MEDIA_TRACK.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* RACE COMMITTEE APP */
        final HorizontalTabLayoutPanel raceCommitteeTabPanel = adminConsolePanel.addVerticalTab(stringMessages.raceCommitteeApp(), RACE_COMMITEE);
        /* Device Configuration User */
        final DeviceConfigurationPanelSupplier deviceConfigurationUserPanelSupplier = new DeviceConfigurationPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(raceCommitteeTabPanel,
                new DefaultRefreshableAdminConsolePanel<DeviceConfigurationPanel>(deviceConfigurationUserPanelSupplier),
                stringMessages.deviceConfiguration(), new DeviceConfigurationPlace((String) null /* no place token */),
                SecuredDomainType.RACE_MANAGER_APP_DEVICE_CONFIGURATION.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* CONNECTORS */
        final HorizontalTabLayoutPanel connectorsTabPanel = adminConsolePanel.addVerticalTab(stringMessages.connectors(), CONNECTORS);
        /* TracTrac Event Management */
        final TracTracEventManagementPanelSupplier tracTracEventManagementPanelSupplier =
                new TracTracEventManagementPanelSupplier(stringMessages, presenter, tableResources);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<TracTracEventManagementPanel>(tracTracEventManagementPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().refreshTracTracConnectors();
                        }
                    }
                },
                stringMessages.tracTracEvents(), new TracTracEventsPlace((String) null /* no place token */),
                SecuredDomainType.TRACTRAC_ACCOUNT.getPermission(DefaultActions.values()));
        /* YellowBrick Event Management */
        final YellowBrickEventManagementPanelSupplier yellowBrickEventManagementPanelSupplier =
                new YellowBrickEventManagementPanelSupplier(stringMessages, presenter, tableResources);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<YellowBrickEventManagementPanel>(yellowBrickEventManagementPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().refreshYellowBrickConnectors();
                        }
                    }
                },
                stringMessages.yellowBrickEvents(), new YellowBrickEventsPlace((String) null /* no place token */),
                SecuredDomainType.YELLOWBRICK_ACCOUNT.getPermission(DefaultActions.values()));
        /* Swiss Timing Replay Connector */
        final SwissTimingReplayConnectorPanelSupplier swissTimingReplayConnectorPanelSupplier =
                new SwissTimingReplayConnectorPanelSupplier(stringMessages, presenter, tableResources);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<SwissTimingReplayConnectorPanel>(
                        swissTimingReplayConnectorPanelSupplier),
                stringMessages.swissTimingArchiveConnector(), new SwissTimingArchivedEventsPlace((String) null /* no place token */),
                SecuredDomainType.SWISS_TIMING_ARCHIVE_ACCOUNT.getPermission(DefaultActions.values()));
        /* Swiss Timing Event Management */
        final SwissTimingEventManagementPanelSupplier swissTimingEventManagementPanelSupplier =
                new SwissTimingEventManagementPanelSupplier(stringMessages, presenter, tableResources);
        adminConsolePanel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<SwissTimingEventManagementPanel>(swissTimingEventManagementPanelSupplier),
                stringMessages.swissTimingEvents(), new SwissTimingEventsPlace((String) null /* no place token */),
                SecuredDomainType.SWISS_TIMING_ACCOUNT.getPermission(DefaultActions.values()));
        /* Smartphone Tracking Event Management */
        final SmartphoneTrackingEventManagementPanelSupplier trackingEventManagementPanelSupplier =
                new SmartphoneTrackingEventManagementPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<SmartphoneTrackingEventManagementPanel>(
                        trackingEventManagementPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        presenter.getLeaderboardsRefresher().callFillAndReloadInitially(getWidget().getLeaderboardsDisplayer());
                        presenter.getRegattasRefresher().callFillAndReloadInitially(getWidget().getRegattasDisplayer());
                    }
                }, stringMessages.smartphoneTracking(), new SmartphoneTrackingPlace((String) null /* no place token */),
                SecuredDomainType.LEADERBOARD.getPermission(DefaultActions.UPDATE, DefaultActions.DELETE));
        /* Igtimi Accounts */
        final IgtimiDevicesPanelSupplier accountsPanelSupplier = new IgtimiDevicesPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<IgtimiDevicesPanel>(accountsPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().refreshDevices();
                        }
                    }
                }, stringMessages.igtimiDevices(), new IgtimiDevicesPlace((String) null /* no place token */),
                SecuredDomainType.IGTIMI_DEVICE.getPermission(DefaultActions.values()));
        /* Expedition Device Configurations */
        final ExpeditionDeviceConfigurationsPanelSupplier expeditionDeviceConfigurationsPanelSupplier =
                new ExpeditionDeviceConfigurationsPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<ExpeditionDeviceConfigurationsPanel>(expeditionDeviceConfigurationsPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                if (getWidget() != null) {
                    getWidget().refresh();
                }
            }
        }, stringMessages.expeditionDeviceConfigurations(), new ExpeditionDeviceConfigurationsPlace((String) null /* no place token */),
                SecuredDomainType.EXPEDITION_DEVICE_CONFIGURATION.getPermission(DefaultActions.values()));
        /* Result Import Urls List */
        final ResultImportUrlsListCompositeSupplier urlsListCompositeSupplier = new ResultImportUrlsListCompositeSupplier(
                stringMessages, presenter);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<ResultImportUrlsListComposite>(urlsListCompositeSupplier),
                stringMessages.resultImportUrls(), new ResultImportUrlsPlace((String) null /* no place token */),
                SecuredDomainType.RESULT_IMPORT_URL.getPermission(DefaultActions.values()));
        /* Structure Import Management */
        final StructureImportManagementPanelSupplier structureImportManagementPanelSupplier = new StructureImportManagementPanelSupplier(
                stringMessages, presenter);
        adminConsolePanel.addToTabPanel(connectorsTabPanel,
                new DefaultRefreshableAdminConsolePanel<StructureImportManagementPanel>(
                        structureImportManagementPanelSupplier),
                stringMessages.manage2Sail() + " " + stringMessages.regattaStructureImport(),
                new Manage2SailRegattaStructureImportPlace((String) null /* no place token */),
                SecuredDomainType.REGATTA.getPermission(DefaultActions.CREATE));
        /* ADVANCED */
        final HorizontalTabLayoutPanel advancedTabPanel = adminConsolePanel.addVerticalTab(stringMessages.advanced(),
                ADVANCED);
        /* Replication */
        final ReplicationPanelSupplier replicationPanelSupplier = new ReplicationPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<ReplicationPanel>(replicationPanelSupplier) {
            @Override
            public void refreshAfterBecomingVisible() {
                if (getWidget() != null) {
                    getWidget().updateReplicaList();
                }
            }
                }, stringMessages.replication(), new ReplicationPlace((String) null /* no place token */),
                () -> userService.hasAnyServerPermission(ServerActions.REPLICATE, ServerActions.START_REPLICATION,
                        ServerActions.READ_REPLICATOR));
        /* Master Data */
        final MasterDataImportPanelSupplier masterDataImportPanelSupplier = new MasterDataImportPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<MasterDataImportPanel>(masterDataImportPanelSupplier),
                stringMessages.masterDataImportPanel(), new MasterDataImportPlace((String) null /* no place token */), SecuredSecurityTypes.SERVER.getPermissionForObject(
                        SecuredSecurityTypes.ServerActions.CAN_IMPORT_MASTERDATA, serverInfo));
        /* Remote Server Instance Manager */
        final RemoteServerInstancesManagementPanelSupplier remoteServerInstancesManagementPanelSupplier =
                new RemoteServerInstancesManagementPanelSupplier(stringMessages, presenter, tableResources);
        adminConsolePanel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<RemoteServerInstancesManagementPanel>(remoteServerInstancesManagementPanelSupplier),
                stringMessages.remoteServerInstances(), new RemoteServerInstancesPlace((String) null /* no place token */),
                SecuredSecurityTypes.SERVER.getPermissionForObject(
                        SecuredSecurityTypes.ServerActions.CONFIGURE_REMOTE_INSTANCES, serverInfo));
        /* Local Server Management */
        final LocalServerManagementPanelSupplier localServerInstancesManagementPanelSupplier =
                new LocalServerManagementPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(advancedTabPanel,
                new DefaultRefreshableAdminConsolePanel<LocalServerManagementPanel>(
                        localServerInstancesManagementPanelSupplier) {
                            @Override
                            public void refreshAfterBecomingVisible() {
                                if (getWidget() != null) {
                                    getWidget().refreshServerConfiguration();
                                }
                            }
        }, stringMessages.localServer(), new LocalServerPlace((String) null /* no place token */),
                // We explicitly use a different permission check here.
                // Most panels show a list of domain objects which means we check if the user has permissions for any
                // potentially existing object to decide about the visibility.
                // The local server tab is about the specific server object. This check needs to contain the ownership
                // information to work as intended.
                () -> userService.hasAnyServerPermission(ServerActions.CONFIGURE_LOCAL_SERVER,
                        DefaultActions.CHANGE_OWNERSHIP, DefaultActions.CHANGE_ACL));
        /* User Management */
        final UserManagementPanelSupplier userManagementPanelSupplier = new UserManagementPanelSupplier(presenter, tableResources);
        adminConsolePanel.addToTabPanel(advancedTabPanel,
                new DefaultRefreshableAdminConsolePanel<UserManagementPanel<AdminConsoleTableResources>>(userManagementPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().updateUsers();
                            getWidget().refreshSuggests();
                        }
                    }
                }, stringMessages.userManagement(), new UserManagementPlace((String) null /* no place token */), SecuredSecurityTypes.USER.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* Role Definition */
        final RoleDefinitionsPanelSupplier roleDefinitionsPanelSupplier = new RoleDefinitionsPanelSupplier(presenter, tableResources);
        adminConsolePanel.addToTabPanel(advancedTabPanel,
                new DefaultRefreshableAdminConsolePanel<RoleDefinitionsPanel>(roleDefinitionsPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().updateRoleDefinitions();
                        }
                    }
                }, stringMessages.roles(), new RolesPlace((String) null /* no place token */),
                SecuredSecurityTypes.ROLE_DEFINITION.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* User Group Management */
        final UserGroupManagementPanelSupplier userGroupManagementPanelSupplier = new UserGroupManagementPanelSupplier(presenter, tableResources);
        adminConsolePanel.addToTabPanel(advancedTabPanel,
                new DefaultRefreshableAdminConsolePanel<UserGroupManagementPanel>(userGroupManagementPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().updateUserGroups();
                            getWidget().refreshSuggests();
                        }
                    }
                }, stringMessages.userGroupManagement(), new UserGroupManagementPlace((String) null /* no place token */), SecuredSecurityTypes.USER_GROUP.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* File Storage */
        final FileStoragePanelSupplier fileStoragePanelSupplier = new FileStoragePanelSupplier(presenter);
        adminConsolePanel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<FileStoragePanel>(fileStoragePanelSupplier),
                stringMessages.fileStorage(), new FileStoragePlace(null),
                SecuredSecurityTypes.SERVER.getPermissionForObject(
                        SecuredSecurityTypes.ServerActions.CONFIGURE_FILE_STORAGE, serverInfo));
        /* Landscape Management */
        final LandscapeManagementPanelSupplier landscapeManagementPanelSupplier = new LandscapeManagementPanelSupplier(presenter, tableResources);
        adminConsolePanel.addToTabPanel(advancedTabPanel,
                new DefaultRefreshableAdminConsolePanel<LandscapeManagementPanel>(landscapeManagementPanelSupplier),
                stringMessages.landscape(), new LandscapeManagementPlace((String) null /* no place token */),
                SecuredLandscapeTypes.LANDSCAPE.getPermissionForTypeRelativeIdentifier(SecuredLandscapeTypes.LandscapeActions.MANAGE,
                        new TypeRelativeObjectIdentifier("AWS")));
        /* COURSE CREATION */
        final HorizontalTabLayoutPanel courseCreationTabPanel = adminConsolePanel
                .addVerticalTab(stringMessages.courseCreation(), COURSE_CREATION);
        /* Mark Template */
        final MarkTemplatePanelSupplier markTemplatePanelSupplier = new MarkTemplatePanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(courseCreationTabPanel,
                new DefaultRefreshableAdminConsolePanel<MarkTemplatePanel>(markTemplatePanelSupplier) {
                @Override
                public void refreshAfterBecomingVisible() {
                    if (getWidget() != null) {
                        getWidget().refreshMarkTemplates();
                    }
                }
            }, stringMessages.markTemplates(), new MarkTemplatesPlace((String) null /* no place token */),
            SecuredDomainType.MARK_TEMPLATE.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* Mark Properties */
        final MarkPropertiesPanelSupplier markPropertiesPanelSupplier = new MarkPropertiesPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(courseCreationTabPanel,
                new DefaultRefreshableAdminConsolePanel<MarkPropertiesPanel>(markPropertiesPanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().refreshMarkProperties();
                        }
                    }
                }, stringMessages.markProperties(), new MarkPropertiesPlace((String) null /* no place token */),
                SecuredDomainType.MARK_PROPERTIES.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* Course Template */
        final CourseTemplatePanelSupplier courseTemplatePanelSupplier = new CourseTemplatePanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(courseCreationTabPanel,
                new DefaultRefreshableAdminConsolePanel<CourseTemplatePanel>(courseTemplatePanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().refreshCourseTemplates();
                        }
                    }
                }, stringMessages.courseTemplates(), new CourseTemplatesPlace((String) null /* no place token */),
                SecuredDomainType.COURSE_TEMPLATE.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* Mark Role */
        final MarkRolePanelSupplier markRolePanelSupplier = new MarkRolePanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(courseCreationTabPanel,
                new DefaultRefreshableAdminConsolePanel<MarkRolePanel>(markRolePanelSupplier) {
                    @Override
                    public void refreshAfterBecomingVisible() {
                        if (getWidget() != null) {
                            getWidget().refreshMarkRoles();
                        }
                    }
                }, stringMessages.markRoles(), new MarkRolesPlace((String) null /* no place token */),
                SecuredDomainType.MARK_ROLE.getPermission(DefaultActions.MUTATION_ACTIONS));
        /* AI Agent */
        final HorizontalTabLayoutPanel aiAgentTabPanel = adminConsolePanel.addVerticalTab(stringMessages.aiAgent(), AI_AGENT);
        /* Device Configuration User */
        final AIAgentConfigurationPanelSupplier aiAgentConfigurationUserPanelSupplier = new AIAgentConfigurationPanelSupplier(stringMessages, presenter);
        adminConsolePanel.addToTabPanel(aiAgentTabPanel,
                new DefaultRefreshableAdminConsolePanel<AIAgentConfigurationPanel>(aiAgentConfigurationUserPanelSupplier),
                stringMessages.aiAgentConfiguration(), new AIAgentConfigurationPlace((String) null /* no place token */),
                SecuredSecurityTypes.SERVER.getPermissionsForTypeRelativeIdentifier(new Action[] { SecuredSecurityTypes.ServerActions.CONFIGURE_AI_AGENT }, serverInfo.getTypeRelativeObjectIdentifier()));
        adminConsolePanel.initUI(defaultPlace);
        return adminConsolePanel;
    }

    @Override
    public void setRedirectToPlace(AbstractAdminConsolePlace redirectoPlace) {
        this.defaultPlace = redirectoPlace;
    }
}
