package com.sap.sailing.gwt.ui.adminconsole.places;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.media.MediaTrackWithSecurityDTO;
import com.sap.sailing.gwt.ui.adminconsole.AdminConsoleClientFactory;
import com.sap.sailing.gwt.ui.adminconsole.places.refresher.AbstractRefresher;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.Refresher;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.ServerConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.adminconsole.AbstractAdminConsolePlace;
import com.sap.sse.gwt.adminconsole.AdminConsolePlace;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;

public class AdminConsoleActivity extends AbstractActivity implements AdminConsoleView.Presenter {
    private Logger logger = Logger.getLogger(getClass().getName());
    private AdminConsoleClientFactory clientFactory;
    private AdminConsoleView adminConsoleView;
    private MediaServiceWriteAsync mediaServiceWrite;
    private SailingServiceWriteAsync sailingService;
    private AbstractAdminConsolePlace defaultPlace;
    
    private final Refresher<StrippedLeaderboardDTO> leaderboardsRefresher;
    private final Refresher<LeaderboardGroupDTO> leaderboardGroupsRefresher;
    private final Refresher<RegattaDTO> regattasRefresher;
    private final Refresher<EventDTO> eventsRefresher;
    private final Refresher<MediaTrackWithSecurityDTO> mediaTracksRefresher;
    private final Refresher<CompetitorDTO> competitorsRefresher;
    private final Refresher<BoatDTO> boatsRefresher;
    
    public AdminConsoleActivity(final AdminConsoleClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.mediaServiceWrite = clientFactory.getMediaServiceWrite();
        this.sailingService = clientFactory.getSailingService();
        leaderboardsRefresher = new AbstractRefresher<StrippedLeaderboardDTO>() {
            @Override
            public void reload(AsyncCallback<Iterable<StrippedLeaderboardDTO>> callback) {
                sailingService.getLeaderboardsWithSecurity(new MarkedAsyncCallback<List<StrippedLeaderboardDTO>>(
                        new AsyncCallback<List<StrippedLeaderboardDTO>>() {
                            @Override
                            public void onSuccess(List<StrippedLeaderboardDTO> result) {
                                logger.log(Level.FINE, "reload LeaderboardDTO - success");
                                callback.onSuccess(new ArrayList<StrippedLeaderboardDTO>(result));
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                getErrorReporter().reportError("Error trying to obtain list of leaderboards from server " + t.getMessage());
                                logger.log(Level.SEVERE, "Error trying to obtain list of leaderboards from server.", t);
                                callback.onFailure(t);
                            }
                        }));
            }
        };
        leaderboardGroupsRefresher = new AbstractRefresher<LeaderboardGroupDTO>() {
            @Override
            public void reload(AsyncCallback<Iterable<LeaderboardGroupDTO>> callback) {
                sailingService.getLeaderboardGroups(false /* withGeoLocationData */,
                        new MarkedAsyncCallback<List<LeaderboardGroupDTO>>(new AsyncCallback<List<LeaderboardGroupDTO>>() {
                            @Override
                            public void onSuccess(List<LeaderboardGroupDTO> result) {
                                logger.log(Level.FINE, "reload LeaderboardGroupDTO - success");
                                callback.onSuccess(new ArrayList<LeaderboardGroupDTO>(result));
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                getErrorReporter().reportError("Error trying to obtain list of leaderboards groups from server " + t.getMessage());
                                logger.log(Level.SEVERE, "Error trying to obtain list of leaderboards groups from server.", t);
                                callback.onFailure(t);
                            }
                        }));
            }
        };
        regattasRefresher = new AbstractRefresher<RegattaDTO>() {
            @Override
            public void reload(AsyncCallback<Iterable<RegattaDTO>> callback) {
                sailingService.getRegattas(new MarkedAsyncCallback<List<RegattaDTO>>(
                        new AsyncCallback<List<RegattaDTO>>() {
                            @Override
                            public void onSuccess(List<RegattaDTO> result) {
                                logger.log(Level.FINE, "reload RegattaDTO - success");
                                callback.onSuccess(new ArrayList<RegattaDTO>(result));
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                getErrorReporter().reportError("Error trying to obtain list of regattas from server " + t.getMessage());
                                logger.log(Level.SEVERE, "Error trying to obtain list of regattas from server.", t);
                                callback.onFailure(t);
                            }
                        }));
            }
        };
        eventsRefresher = new AbstractRefresher<EventDTO>() {
            @Override
            public void reload(AsyncCallback<Iterable<EventDTO>> callback) {
                sailingService.getEvents(new AsyncCallback<List<EventDTO>>() {
                    @Override
                    public void onSuccess(List<EventDTO> result) {
                        logger.log(Level.FINE, "reload EventDTO - success");
                        callback.onSuccess(new ArrayList<EventDTO>(result));
                    }
                    
                    @Override
                    public void onFailure(Throwable t) {
                        getErrorReporter().reportError("Error trying to obtain list of events from server " + t.getMessage());
                        logger.log(Level.SEVERE, "Error trying to obtain list of events from server.", t);
                        callback.onFailure(t);
                    }
                });
            }
        };
        mediaTracksRefresher = new AbstractRefresher<MediaTrackWithSecurityDTO>() {
            @Override
            public void reload(AsyncCallback<Iterable<MediaTrackWithSecurityDTO>> callback) {
                mediaServiceWrite.getAllMediaTracks(new AsyncCallback<Iterable<MediaTrackWithSecurityDTO>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        getErrorReporter().reportError("Error trying to obtain list of media tracks from server " + t.getMessage());
                        logger.log(Level.SEVERE, "Error trying to obtain list of media tracks from server.", t);
                        callback.onFailure(t);
                    }

                    @Override
                    public void onSuccess(Iterable<MediaTrackWithSecurityDTO> result) {
                        logger.log(Level.FINE, "reload MediaTrackWithSecurityDTO - success");
                        List<MediaTrackWithSecurityDTO> list = new ArrayList<MediaTrackWithSecurityDTO>();
                        result.forEach(mediaTrackDto -> list.add(mediaTrackDto));
                        callback.onSuccess(list);
                    }
                });
            }
        };
        competitorsRefresher = new AbstractRefresher<CompetitorDTO>() {
            @Override
            public void reload(AsyncCallback<Iterable<CompetitorDTO>> callback) {
                sailingService.getCompetitors(/* ignoreCompetitorsWithBoat */ false,
                        /* ignoreCompetitorsWithoutBoat */ false, new MarkedAsyncCallback<Iterable<CompetitorDTO>>(
                        new AsyncCallback<Iterable<CompetitorDTO>>() {
                            @Override
                            public void onSuccess(Iterable<CompetitorDTO> result) {
                                logger.log(Level.FINE, "reload CompetitorDTO - success");
                                callback.onSuccess(result);
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                getErrorReporter().reportError("Error trying to obtain list of leaderboards from server " + t.getMessage());
                                logger.log(Level.SEVERE, "Error trying to obtain list of leaderboards from server.", t);
                                callback.onFailure(t);
                            }
                        }));
            }
        };
        boatsRefresher = new AbstractRefresher<BoatDTO>() {
            @Override
            public void reload(AsyncCallback<Iterable<BoatDTO>> callback) {
                sailingService.getAllBoats(new MarkedAsyncCallback<Iterable<BoatDTO>>(
                        new AsyncCallback<Iterable<BoatDTO>>() {
                            @Override
                            public void onSuccess(Iterable<BoatDTO> result) {
                                logger.log(Level.FINE, "reload BoatDTO - success");
                                callback.onSuccess(result);
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                getErrorReporter().reportError("Error trying to obtain list of leaderboards from server " + t.getMessage());
                                logger.log(Level.SEVERE, "Error trying to obtain list of leaderboards from server.", t);
                                callback.onFailure(t);
                            }
                        }));
            }
        };
    }

    @Override
    public Refresher<StrippedLeaderboardDTO> getLeaderboardsRefresher() {
        return leaderboardsRefresher;
    }
    
    @Override
    public Refresher<LeaderboardGroupDTO> getLeaderboardGroupsRefresher() {
        return leaderboardGroupsRefresher;
    }

    @Override
    public Refresher<RegattaDTO> getRegattasRefresher() {
        return regattasRefresher;
    }

    @Override
    public Refresher<EventDTO> getEventsRefresher() {
        return eventsRefresher;
    }
    
    @Override
    public Refresher<MediaTrackWithSecurityDTO> getMediaTracksRefresher() {
        return mediaTracksRefresher;
    }
    
    @Override
    public Refresher<CompetitorDTO> getCompetitorsRefresher() {
        return competitorsRefresher;
    }
    
    @Override
    public Refresher<BoatDTO> getBoatsRefresher() {
        return boatsRefresher;
    }
    
    public AdminConsoleActivity(final AdminConsolePlace place, final AdminConsoleClientFactory clientFactory) {
        this(clientFactory); 
    }
    
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {     
        initView(); 
        containerWidget.setWidget(adminConsoleView.asWidget());    
    }
    
    public void setRedirectToPlace(AbstractAdminConsolePlace place) {
        this.defaultPlace = place;
    }
    
    public void goToMenuAndTab(AbstractAdminConsolePlace place) {
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
    public SubscriptionServiceFactory getSubscriptionServiceFactory() {
        return clientFactory.getSubscriptionServiceFactory();
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
                        user.getCompany(), user.getLocale(), user.getDidOptOutOfFeatureAndCommunityEmails(),
                        serverTenant.getId().toString(), new AsyncCallback<UserDTO>() {
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
