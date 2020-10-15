package com.sap.sailing.gwt.ui.adminconsole.places;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.ui.adminconsole.AdminConsoleClientFactory;
import com.sap.sailing.gwt.ui.client.LeaderboardGroupsDisplayer;
import com.sap.sailing.gwt.ui.client.LeaderboardsDisplayer;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.RegattasDisplayer;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.ServerConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTOWithSecurity;
import com.sap.sse.gwt.adminconsole.AdminConsolePlace;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;

public class AdminConsoleActivity extends AbstractActivity implements AdminConsoleView.Presenter {
    
    private AdminConsoleClientFactory clientFactory;
    
    private HashSet<RegattasDisplayer> regattasDisplayers;
    private HashSet<LeaderboardsDisplayer<StrippedLeaderboardDTOWithSecurity>> leaderboardsDisplayers;
    private HashSet<LeaderboardGroupsDisplayer> leaderboardGroupsDisplayers;
    
    private AdminConsoleView adminConsoleView;
    
    private MediaServiceWriteAsync mediaServiceWrite;
    private SailingServiceWriteAsync sailingService;
    
    private static AdminConsoleActivity instance;
    
    private AdminConsolePlace defaultPlace;
    
    public static boolean instantiated() {
        return instance != null;
    }
    
    public static AdminConsoleActivity getInstance(final AdminConsoleClientFactory clientFactory) {
        if(instance == null) {
            instance = new AdminConsoleActivity(clientFactory);
        }
        return instance;
    }
    
    public static AdminConsoleActivity getInstance(final AdminConsoleClientFactory clientFactory, AdminConsolePlace defaultPlace) {
        if(instance == null) {
            instance = new AdminConsoleActivity(clientFactory);
            instance.setRedirectToPlace(defaultPlace);
        }
        return instance;
    }
    
    private AdminConsoleActivity(final AdminConsoleClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.mediaServiceWrite = clientFactory.getMediaServiceWrite();
        this.sailingService = clientFactory.getSailingService();
        
        regattasDisplayers = new HashSet<>();
        leaderboardsDisplayers = new HashSet<>();
        leaderboardGroupsDisplayers = new HashSet<>();
    }
    
    public AdminConsoleActivity(final AdminConsolePlace place, final AdminConsoleClientFactory clientFactory) {
        this(clientFactory); 
    }
    
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {     
        initView(); 
        containerWidget.setWidget(adminConsoleView.asWidget());    
    }
    
    public void setRedirectToPlace(AdminConsolePlace place) {
        this.defaultPlace = place;
    }
    
    public void goToMenuAndTab(AdminConsolePlace place) {
        initView(); 
        adminConsoleView.selectTabByPlace(place);       
    }
    
    private void initView() {
        if (adminConsoleView == null) {
            adminConsoleView = new AdminConsoleViewImpl();
            adminConsoleView.setPresenter(this);
            adminConsoleView.setRedirectToPlace(defaultPlace);
            clientFactory.getUserService().executeWithServerInfo(adminConsoleView::createUI);
            clientFactory.getUserService().addUserStatusEventHandler((u, p) -> checkPublicServerNonPublicUserWarning());
        }    
    }
    
    @Override
    public UserService getUserService() {
        return clientFactory.getUserService();
    }
    
    @Override
    public SailingServiceWriteAsync getSailingService() {
        return sailingService;
    }
    
    @Override
    public MediaServiceWriteAsync getMediaServiceWrite() {
        return mediaServiceWrite;
    }
    
    @Override
    public ErrorReporter getErrorReporter() {
        return clientFactory.getErrorReporter();
    }
    
    @Override
    public HashSet<RegattasDisplayer> getRegattasDisplayers() {
        return regattasDisplayers;
    }
    
    @Override
    public HashSet<LeaderboardsDisplayer<StrippedLeaderboardDTOWithSecurity>> getLeaderboardsDisplayer() {
        return leaderboardsDisplayers;
    }
    
    @Override
    public HashSet<LeaderboardGroupsDisplayer> getLeaderboardGroupsDisplayer() {
        return leaderboardGroupsDisplayers;
    }
    
    @Override
    public void fillLeaderboards() {
        sailingService.getLeaderboardsWithSecurity(new MarkedAsyncCallback<List<StrippedLeaderboardDTOWithSecurity>>(
                new AsyncCallback<List<StrippedLeaderboardDTOWithSecurity>>() {
                    @Override
                    public void onSuccess(List<StrippedLeaderboardDTOWithSecurity> leaderboards) {
                        for (LeaderboardsDisplayer<StrippedLeaderboardDTOWithSecurity> leaderboardsDisplayer : leaderboardsDisplayers) {
                            leaderboardsDisplayer.fillLeaderboards(leaderboards);
                        }
                    }
        
                    @Override
                    public void onFailure(Throwable t) {
                        clientFactory.getErrorReporter().reportError("Error trying to obtain list of leaderboards: "+ t.getMessage());
                    }
                }));
    }
    
    @Override
    public void updateLeaderboards(Iterable<StrippedLeaderboardDTOWithSecurity> updatedLeaderboards,
            LeaderboardsDisplayer<StrippedLeaderboardDTOWithSecurity> origin) {
        for (LeaderboardsDisplayer<StrippedLeaderboardDTOWithSecurity> leaderboardsDisplayer : leaderboardsDisplayers) {
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
                                clientFactory.getErrorReporter().reportError("Error trying to obtain list of leaderboard groups: " + t.getMessage());
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
                        clientFactory.getErrorReporter().reportError("Remote Procedure Call getRegattas() - Failure");
                    }
                }));
    }
    
    @Override
    public void setupLeaderboardGroups(LeaderboardGroupsDisplayer displayer, Map<String, String> params) {
        displayer.setupLeaderboardGroups(params);
    }

 
    protected void checkPublicServerNonPublicUserWarning() {
        sailingService.getServerConfiguration(new AsyncCallback<ServerConfigurationDTO>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ServerConfigurationDTO result) {
                if (Boolean.TRUE.equals(result.isPublic())) {
                    StrippedUserGroupDTO currentTenant = clientFactory.getUserService().getCurrentTenant();
                    StrippedUserGroupDTO serverTenant = result.getServerDefaultTenant();
                    if (!serverTenant.equals(currentTenant) && clientFactory.getUserService().getCurrentUser() != null) {
                        if (clientFactory.getUserService().getCurrentUser().getUserGroups().contains(serverTenant)) {
                            // The current user is in server tenant group and so his default tenant could be changed.
                            if (Window.confirm(StringMessages.INSTANCE.serverIsPublicButTenantIsNotAndCouldBeChanged())) {
                                // change the default tenant
                                changeDefaultTenantForCurrentUser(serverTenant);
                            }
                        } else {
                            // The current user is not in the server tenant group so his default tenant cannot be
                            // changed.
                            Window.alert(StringMessages.INSTANCE.serverIsPublicButTenantIsNot());
                        }
                    }
                }
            }

            /** Changes the default tenant for the current user. */
            private void changeDefaultTenantForCurrentUser(final StrippedUserGroupDTO serverTenant) {
                final UserDTO user = clientFactory.getUserService().getCurrentUser();
                clientFactory.getUserManagementWriteService().updateUserProperties(user.getName(), user.getFullName(),
                        user.getCompany(), user.getLocale(), serverTenant.getId().toString(),
                        new AsyncCallback<UserDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                Window.alert(caught.getMessage());
                            }

                            @Override
                            public void onSuccess(UserDTO result) {
                                user.setDefaultTenantForCurrentServer(serverTenant);
                            }
                        });
            }
        }); 
    }

    @Override
    public PlaceController getPlaceController() {
        return clientFactory.getPlaceController();
    }


 
}
