package com.sap.sailing.gwt.managementconsole.app;

import static com.sap.sailing.gwt.ui.client.StringMessages.INSTANCE;
import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.managementconsole.mvp.ViewFactory;
import com.sap.sailing.gwt.managementconsole.partials.authentication.signin.SignInPresenter;
import com.sap.sailing.gwt.managementconsole.services.EventService;
import com.sap.sailing.gwt.managementconsole.services.RegattaService;
import com.sap.sailing.gwt.ui.client.MediaServiceWrite;
import com.sap.sailing.gwt.ui.client.MediaServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.shared.ServerConfigurationDTO;
import com.sap.sailing.landscape.common.RemoteServiceMappingConstants;
import com.sap.sse.gwt.client.DefaultErrorReporter;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.authentication.AuthenticationManager;
import com.sap.sse.security.ui.authentication.AuthenticationManagerImpl;
import com.sap.sse.security.ui.authentication.AuthenticationRequestEvent;
import com.sap.sse.security.ui.client.DefaultWithSecurityImpl;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.UserManagementWriteServiceAsync;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.WithSecurity;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;

public class ManagementConsoleClientFactoryImpl implements ManagementConsoleClientFactory {

    private final WithSecurity securityProvider = new DefaultWithSecurityImpl();
    private final ErrorReporter errorReporter = new DefaultErrorReporter<>(INSTANCE);
    private final MediaServiceWriteAsync mediaServiceWrite = GWT.create(MediaServiceWrite.class);
    private final PlaceController placeController;;
    private final SailingServiceWriteAsync sailingService;
    private final EventService eventService;
    private final RegattaService regattaService;
    private final AuthenticationManager authenticationManager;
    private final ViewFactory viewFactory;

    public ManagementConsoleClientFactoryImpl(final EventBus eventBus, final SailingServiceWriteAsync sailingService) {
        this.placeController = new PlaceController(eventBus);
        this.sailingService = sailingService;
        EntryPointHelper.registerASyncService((ServiceDefTarget) mediaServiceWrite,
                RemoteServiceMappingConstants.mediaServiceRemotePath, HEADER_FORWARD_TO_MASTER);
        getUserService().addUserStatusEventHandler((u, p) -> checkPublicServerNonPublicUserWarning());
        this.eventService = new EventService(sailingService, errorReporter, eventBus);
        this.regattaService = new RegattaService(sailingService, errorReporter, eventBus);
        // TODO: Provide URLs for email confirmation and password reset:
        this.authenticationManager = new AuthenticationManagerImpl(this, eventBus, "", "");
        this.viewFactory = new ViewFactory();

        eventBus.addHandler(AuthenticationRequestEvent.TYPE, new SignInPresenter(ManagementConsoleClientFactoryImpl.this));
    }

    @Override
    public PlaceController getPlaceController() {
        return placeController;
    }

    @Override
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    @Override
    public UserManagementServiceAsync getUserManagementService() {
        return securityProvider.getUserManagementService();
    }

    @Override
    public UserManagementWriteServiceAsync getUserManagementWriteService() {
        return securityProvider.getUserManagementWriteService();
    }

    @Override
    public UserService getUserService() {
        return securityProvider.getUserService();
    }

    @Override
    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
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
    public EventService getEventService() {
        return eventService;
    }
    
    @Override
    public RegattaService getRegattaService() {
        return regattaService;
    }

    @Override
    public ViewFactory getViewFactory() {
        return viewFactory;
    }

    protected void checkPublicServerNonPublicUserWarning() {
        sailingService.getServerConfiguration(new AsyncCallback<ServerConfigurationDTO>() {
            @Override
            public void onFailure(final Throwable caught) {
            }

            @Override
            public void onSuccess(final ServerConfigurationDTO result) {
                if (Boolean.TRUE.equals(result.isPublic())) {
                    final StrippedUserGroupDTO currentTenant = getUserService().getCurrentTenant();
                    final StrippedUserGroupDTO serverTenant = result.getServerDefaultTenant();
                    if (!serverTenant.equals(currentTenant) && getUserService().getCurrentUser() != null) {
                        if (getUserService().getCurrentUser().getUserGroups().contains(serverTenant)) {
                            if (Window.confirm(INSTANCE.serverIsPublicButTenantIsNotAndCouldBeChanged())) {
                                changeDefaultTenantForCurrentUser(serverTenant);
                            }
                        } else {
                            Window.alert(INSTANCE.serverIsPublicButTenantIsNot());
                        }
                    }
                }
            }

            private void changeDefaultTenantForCurrentUser(final StrippedUserGroupDTO serverTenant) {
                final UserDTO user = getUserService().getCurrentUser();
                getUserManagementWriteService().updateUserProperties(user.getName(), user.getFullName(),
                        user.getCompany(), user.getLocale(), user.getDidOptOutOfFeatureAndCommunityEmails(),
                        serverTenant.getId().toString(), new AsyncCallback<UserDTO>() {
                            @Override
                            public void onFailure(final Throwable caught) {
                                Window.alert(caught.getMessage());
                            }

                            @Override
                            public void onSuccess(final UserDTO result) {
                                user.setDefaultTenantForCurrentServer(serverTenant);
                            }
                        });
            }
        });
    }

    @Override
    public SubscriptionServiceFactory getSubscriptionServiceFactory() {
        return securityProvider.getSubscriptionServiceFactory();
    }

}
