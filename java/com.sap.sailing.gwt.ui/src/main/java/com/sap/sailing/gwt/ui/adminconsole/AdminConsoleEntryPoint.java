package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.security.Permission;
import com.sap.sailing.domain.common.security.Roles;
import com.sap.sailing.domain.common.security.SailingPermissionsForRoleProvider;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.ui.client.AbstractSailingEntryPoint;
import com.sap.sailing.gwt.ui.client.LeaderboardGroupsDisplayer;
import com.sap.sailing.gwt.ui.client.LeaderboardGroupsRefresher;
import com.sap.sailing.gwt.ui.client.LeaderboardsDisplayer;
import com.sap.sailing.gwt.ui.client.LeaderboardsRefresher;
import com.sap.sailing.gwt.ui.client.MediaService;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.RegattasDisplayer;
import com.sap.sailing.gwt.ui.client.RemoteServiceMappingConstants;
import com.sap.sailing.gwt.ui.masterdataimport.MasterDataImportPanel;
import com.sap.sailing.gwt.ui.shared.BetterDateTimeBox;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SecurityStylesheetResources;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.adminconsole.AdminConsolePanel;
import com.sap.sse.gwt.adminconsole.DefaultRefreshableAdminConsolePanel;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.controls.filestorage.FileStoragePanel;
import com.sap.sse.gwt.resources.Highcharts;
import com.sap.sse.security.shared.Role;
import com.sap.sse.security.ui.authentication.decorator.AuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.decorator.WidgetFactory;
import com.sap.sse.security.ui.authentication.generic.GenericAuthentication;
import com.sap.sse.security.ui.authentication.generic.GenericAuthorizedContentDecorator;
import com.sap.sse.security.ui.authentication.generic.sapheader.SAPHeaderWithAuthentication;
import com.sap.sse.security.ui.client.component.UserManagementPanel;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class AdminConsoleEntryPoint extends AbstractSailingEntryPoint implements RegattaRefresher, LeaderboardsRefresher, LeaderboardGroupsRefresher {
    private Set<RegattasDisplayer> regattasDisplayers;
    private Set<LeaderboardsDisplayer> leaderboardsDisplayers;
    private Set<LeaderboardGroupsDisplayer> leaderboardGroupsDisplayers;

    private final MediaServiceAsync mediaService = GWT.create(MediaService.class);
    
    @Override
    protected void doOnModuleLoad() {
        Highcharts.ensureInjectedWithMore();
        super.doOnModuleLoad();
        EntryPointHelper.registerASyncService((ServiceDefTarget) mediaService, RemoteServiceMappingConstants.mediaServiceRemotePath);
        createUI();
    }
     
    private void createUI() {
        HeaderPanel headerPanel = new HeaderPanel();
        SAPHeaderWithAuthentication header = new SAPHeaderWithAuthentication(getStringMessages().sapSailingAnalytics(),
                getStringMessages().administration());
        GenericAuthentication genericSailingAuthentication = new FixedSailingAuthentication(getUserService(), header.getAuthenticationMenuView());
        AuthorizedContentDecorator authorizedContentDecorator = new GenericAuthorizedContentDecorator(genericSailingAuthentication);
        authorizedContentDecorator.setContentWidgetFactory(new WidgetFactory() {
            @Override
            public Widget get() {
                return createAdminConsolePanel();
            }
        });
        
        headerPanel.setHeaderWidget(header);
        headerPanel.setContentWidget(authorizedContentDecorator);
        RootLayoutPanel rootPanel = RootLayoutPanel.get();
        rootPanel.add(headerPanel);
    }
    
    private Widget createAdminConsolePanel() {
        AdminConsolePanel panel = new AdminConsolePanel(getUserService(), SailingPermissionsForRoleProvider.INSTANCE, 
                sailingService, getStringMessages().releaseNotes(), "/release_notes_admin.html", /* error reporter */ this, SecurityStylesheetResources.INSTANCE.css(), false);
        panel.addStyleName("adminConsolePanel");
        
        BetterDateTimeBox.initialize();
        regattasDisplayers = new HashSet<>();
        leaderboardsDisplayers = new HashSet<>();
        leaderboardGroupsDisplayers = new HashSet<>();

        final EventManagementPanel eventManagementPanel = new EventManagementPanel(sailingService, this, this, getStringMessages());
        panel.addToVerticalTabPanel(new DefaultRefreshableAdminConsolePanel<EventManagementPanel>(eventManagementPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                getWidget().fillEvents();
                fillLeaderboardGroups();
            }
        }, getStringMessages().events(), Permission.MANAGE_EVENTS);
        leaderboardGroupsDisplayers.add(eventManagementPanel);

        RegattaManagementPanel regattaManagementPanel = new RegattaManagementPanel(
                sailingService, this, getStringMessages(), this, eventManagementPanel);
        regattaManagementPanel.ensureDebugId("RegattaStructureManagement");
        panel.addToVerticalTabPanel(new DefaultRefreshableAdminConsolePanel<RegattaManagementPanel>(regattaManagementPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                fillRegattas();
            }
        }, getStringMessages().regattas(), Permission.MANAGE_REGATTAS);
        regattasDisplayers.add(regattaManagementPanel);
        
        /* LEADERBOARDS */
        
        final TabLayoutPanel leaderboardTabPanel = panel.addVerticalTab(getStringMessages().leaderboards(), "LeaderboardPanel");
        final LeaderboardConfigPanel leaderboardConfigPanel = new LeaderboardConfigPanel(sailingService, this, this,
                getStringMessages(), /* showRaceDetails */true, this);
        leaderboardConfigPanel.ensureDebugId("LeaderboardConfiguration");
        panel.addToTabPanel(leaderboardTabPanel, new DefaultRefreshableAdminConsolePanel<LeaderboardConfigPanel>(leaderboardConfigPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                fillLeaderboards();
            }
        }, getStringMessages().leaderboards(), Permission.MANAGE_LEADERBOARDS);
        regattasDisplayers.add(leaderboardConfigPanel);
        leaderboardsDisplayers.add(leaderboardConfigPanel);

        final LeaderboardGroupConfigPanel leaderboardGroupConfigPanel = new LeaderboardGroupConfigPanel(sailingService,
                this, this, this, this, getStringMessages());
        leaderboardGroupConfigPanel.ensureDebugId("LeaderboardGroupConfiguration");
        panel.addToTabPanel(leaderboardTabPanel, new DefaultRefreshableAdminConsolePanel<LeaderboardGroupConfigPanel>(leaderboardGroupConfigPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                fillLeaderboards();
                fillLeaderboardGroups();
            }
        }, getStringMessages().leaderboardGroups(), Permission.MANAGE_LEADERBOARD_GROUPS);
        regattasDisplayers.add(leaderboardGroupConfigPanel);
        leaderboardGroupsDisplayers.add(leaderboardGroupConfigPanel);
        leaderboardsDisplayers.add(leaderboardGroupConfigPanel);

        /* RACES */
        
        final TabLayoutPanel racesTabPanel = panel.addVerticalTab(getStringMessages().trackedRaces(), "RacesPanel");
        racesTabPanel.ensureDebugId("RacesPanel");
        TrackedRacesManagementPanel trackedRacesManagementPanel = new TrackedRacesManagementPanel(sailingService, this,
                this, getStringMessages());
        trackedRacesManagementPanel.ensureDebugId("TrackedRacesManagement");
        panel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<TrackedRacesManagementPanel>(trackedRacesManagementPanel),
                getStringMessages().trackedRaces(), Permission.SHOW_TRACKED_RACES);
        regattasDisplayers.add(trackedRacesManagementPanel);

        final CompetitorPanel competitorPanel = new CompetitorPanel(sailingService, getStringMessages(), this);
        competitorPanel.ensureDebugId("CompetitorPanel");
        panel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<CompetitorPanel>(competitorPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                getWidget().refreshCompetitorList();
            }
        }, getStringMessages().competitors(), Permission.MANAGE_ALL_COMPETITORS);

        RaceCourseManagementPanel raceCourseManagementPanel = new RaceCourseManagementPanel(sailingService, this, this, getStringMessages());
        panel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<RaceCourseManagementPanel>(raceCourseManagementPanel), getStringMessages().courseLayout(), Permission.MANAGE_COURSE_LAYOUT);
        regattasDisplayers.add(raceCourseManagementPanel);

        final AsyncActionsExecutor asyncActionsExecutor = new AsyncActionsExecutor();

        WindPanel windPanel = new WindPanel(sailingService, asyncActionsExecutor, this, this, getStringMessages());
        panel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<WindPanel>(windPanel), getStringMessages().wind(),
                Permission.MANAGE_WIND);
        regattasDisplayers.add(windPanel);

        final MediaPanel mediaPanel = new MediaPanel(regattasDisplayers, sailingService, this, mediaService, this, getStringMessages());
        panel.addToTabPanel(racesTabPanel, new DefaultRefreshableAdminConsolePanel<MediaPanel>(mediaPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                getWidget().onShow();
            }
        }, getStringMessages().mediaPanel(), Permission.MANAGE_MEDIA);

        /* RACE COMMITTEE APP */

        final TabLayoutPanel raceCommitteeTabPanel = panel.addVerticalTab(getStringMessages().raceCommitteeApp(), "RaceCommiteeAppPanel");
        final DeviceConfigurationUserPanel deviceConfigurationUserPanel = new DeviceConfigurationUserPanel(sailingService,
                getUserService(), getStringMessages(), this);
        panel.addToTabPanel(raceCommitteeTabPanel, new DefaultRefreshableAdminConsolePanel<DeviceConfigurationUserPanel>(deviceConfigurationUserPanel),
                getStringMessages().deviceConfiguration(), Permission.MANAGE_DEVICE_CONFIGURATION);
        
        /* CONNECTORS */
        
        final TabLayoutPanel connectorsTabPanel = panel.addVerticalTab(getStringMessages().connectors(), "TrackingProviderPanel");
        TracTracEventManagementPanel tractracEventManagementPanel = new TracTracEventManagementPanel(sailingService,
                this, this, getStringMessages());
        tractracEventManagementPanel.ensureDebugId("TracTracEventManagement");
        panel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<TracTracEventManagementPanel>(tractracEventManagementPanel),
                getStringMessages().tracTracEvents(), Permission.MANAGE_TRACKED_RACES);
        regattasDisplayers.add(tractracEventManagementPanel);

        SwissTimingReplayConnectorPanel swissTimingReplayConnectorPanel = new SwissTimingReplayConnectorPanel(
                sailingService, this, this, getStringMessages());
        panel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<SwissTimingReplayConnectorPanel>(swissTimingReplayConnectorPanel),
                getStringMessages().swissTimingArchiveConnector(), Permission.MANAGE_TRACKED_RACES);
        regattasDisplayers.add(swissTimingReplayConnectorPanel);

        SwissTimingEventManagementPanel swisstimingEventManagementPanel = new SwissTimingEventManagementPanel(
                sailingService, this, this, getStringMessages());
        panel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<SwissTimingEventManagementPanel>(swisstimingEventManagementPanel),
                getStringMessages().swissTimingEvents(), Permission.MANAGE_TRACKED_RACES);
        regattasDisplayers.add(swisstimingEventManagementPanel);

        final SmartphoneTrackingEventManagementPanel raceLogTrackingEventManagementPanel = new SmartphoneTrackingEventManagementPanel(
                sailingService, this, this, this, getStringMessages());
        raceLogTrackingEventManagementPanel.ensureDebugId("SmartphoneTrackingPanel");
        panel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<SmartphoneTrackingEventManagementPanel>(raceLogTrackingEventManagementPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                fillLeaderboards();
            }
        }, getStringMessages().smartphoneTracking(), Permission.MANAGE_TRACKED_RACES);
        regattasDisplayers.add(raceLogTrackingEventManagementPanel);
        leaderboardsDisplayers.add(raceLogTrackingEventManagementPanel);

        IgtimiAccountsPanel igtimiAccountsPanel = new IgtimiAccountsPanel(sailingService, this, getStringMessages());
        igtimiAccountsPanel.ensureDebugId("IgtimiAccounts");
        panel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<IgtimiAccountsPanel>(igtimiAccountsPanel),
                getStringMessages().igtimiAccounts(), Permission.MANAGE_IGTIMI_ACCOUNTS);
        
        ResultImportUrlsManagementPanel resultImportUrlsManagementPanel = new ResultImportUrlsManagementPanel(sailingService, this, getStringMessages());
        panel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<ResultImportUrlsManagementPanel>(resultImportUrlsManagementPanel),
                getStringMessages().resultImportUrls(), Permission.MANAGE_RESULT_IMPORT_URLS);
        
        StructureImportManagementPanel structureImportUrlsManagementPanel = new StructureImportManagementPanel(sailingService, this, getStringMessages(), this, eventManagementPanel);
        panel.addToTabPanel(connectorsTabPanel, new DefaultRefreshableAdminConsolePanel<StructureImportManagementPanel>(structureImportUrlsManagementPanel),
                getStringMessages().manage2Sail() + " " + getStringMessages().regattaStructureImport(), Permission.MANAGE_STRUCTURE_IMPORT_URLS);

        /* ADVANCED */
        
        final TabLayoutPanel advancedTabPanel = panel.addVerticalTab(getStringMessages().advanced(), "AdvancedPanel");
        final ReplicationPanel replicationPanel = new ReplicationPanel(sailingService, this, getStringMessages());
        panel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<ReplicationPanel>(replicationPanel) {
            @Override
            public void refreshAfterBecomingVisible() {
                replicationPanel.updateReplicaList();
            }
        }, getStringMessages().replication(), Permission.MANAGE_REPLICATION);

        final MasterDataImportPanel masterDataImportPanel = new MasterDataImportPanel(getStringMessages(), sailingService,
                this, eventManagementPanel, this, this);
        panel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<MasterDataImportPanel>(masterDataImportPanel),
                getStringMessages().masterDataImportPanel(), Permission.MANAGE_MASTERDATA_IMPORT);

        RemoteServerInstancesManagementPanel remoteServerInstancesManagementPanel = new RemoteServerInstancesManagementPanel(sailingService, this, getStringMessages());
        panel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<RemoteServerInstancesManagementPanel>(remoteServerInstancesManagementPanel),
                getStringMessages().remoteServerInstances(), Permission.MANAGE_SAILING_SERVER_INSTANCES);

        LocalServerManagementPanel localServerInstancesManagementPanel = new LocalServerManagementPanel(sailingService, this, getStringMessages());
        panel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<LocalServerManagementPanel>(localServerInstancesManagementPanel),
                getStringMessages().localServer(), Permission.MANAGE_LOCAL_SERVER_INSTANCE);

        final UserManagementPanel userManagementPanel = new UserManagementPanel(getUserService(), StringMessages.INSTANCE,
                SailingPermissionsForRoleProvider.INSTANCE, Arrays.<Role>asList(Roles.values()), Arrays.<com.sap.sse.security.shared.Permission>asList(Permission.values()));
        panel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<UserManagementPanel>(userManagementPanel),
                getStringMessages().userManagement(), Permission.MANAGE_USERS);
        
        final FileStoragePanel fileStoragePanel = new FileStoragePanel(sailingService, this);
        panel.addToTabPanel(advancedTabPanel, new DefaultRefreshableAdminConsolePanel<FileStoragePanel>(fileStoragePanel),
                getStringMessages().fileStorage(), Permission.MANAGE_FILE_STORAGE);

        panel.initUI();
        fillRegattas();
        fillLeaderboardGroups();
        fillLeaderboards();
        
        return panel;
    }

    @Override
    public void fillLeaderboards() {
        sailingService.getLeaderboards(new MarkedAsyncCallback<List<StrippedLeaderboardDTO>>(
                new AsyncCallback<List<StrippedLeaderboardDTO>>() {
                    @Override
                    public void onSuccess(List<StrippedLeaderboardDTO> leaderboards) {
                        for (LeaderboardsDisplayer leaderboardsDisplayer : leaderboardsDisplayers) {
                            leaderboardsDisplayer.fillLeaderboards(leaderboards);
                        }
                    }
        
                    @Override
                    public void onFailure(Throwable t) {
                        reportError("Error trying to obtain list of leaderboards: "+ t.getMessage());
                    }
                }));
    }
    
    @Override
    public void updateLeaderboards(Iterable<StrippedLeaderboardDTO> updatedLeaderboards, LeaderboardsDisplayer origin) {
        for (LeaderboardsDisplayer leaderboardsDisplayer : leaderboardsDisplayers) {
            if (leaderboardsDisplayer != origin) {
                leaderboardsDisplayer.fillLeaderboards(updatedLeaderboards);
            }
        }
    }

    @Override
    public void fillLeaderboardGroups() {
        sailingService.getLeaderboardGroups(false /*withGeoLocationData*/,
                new MarkedAsyncCallback<List<LeaderboardGroupDTO>>(
                        new AsyncCallback<List<LeaderboardGroupDTO>>() {
                            @Override
                            public void onSuccess(List<LeaderboardGroupDTO> groups) {
                                for (LeaderboardGroupsDisplayer leaderboardGroupsDisplayer : leaderboardGroupsDisplayers) {
                                    leaderboardGroupsDisplayer.fillLeaderboardGroups(groups);
                                }
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                reportError("Error trying to obtain list of leaderboard groups: " + t.getMessage());
                            }
                        }));
    }

    @Override
    public void updateLeaderboardGroups(Iterable<LeaderboardGroupDTO> updatedLeaderboardGroups,
            LeaderboardGroupsDisplayer origin) {
        for (LeaderboardGroupsDisplayer leaderboardGroupsDisplayer : leaderboardGroupsDisplayers) {
            if (leaderboardGroupsDisplayer != origin) {
                leaderboardGroupsDisplayer.fillLeaderboardGroups(updatedLeaderboardGroups);
            }
        }
    }

    @Override
    public void fillRegattas() {
        sailingService.getRegattas(new MarkedAsyncCallback<List<RegattaDTO>>(
                new AsyncCallback<List<RegattaDTO>>() {
                    @Override
                    public void onSuccess(List<RegattaDTO> result) {
                        for (RegattasDisplayer regattaDisplayer : regattasDisplayers) {
                            regattaDisplayer.fillRegattas(result);
                        }
                    }
        
                    @Override
                    public void onFailure(Throwable caught) {
                        reportError("Remote Procedure Call getRegattas() - Failure");
                    }
                }));
    }
}
