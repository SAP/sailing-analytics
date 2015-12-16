package com.sap.sailing.gwt.home.desktop.app;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystemImpl;
import com.sap.sailing.gwt.home.desktop.places.error.TabletAndDesktopErrorView;
import com.sap.sailing.gwt.home.desktop.places.events.EventsView;
import com.sap.sailing.gwt.home.desktop.places.events.TabletAndDesktopEventsView;
import com.sap.sailing.gwt.home.desktop.places.searchresult.TabletAndDesktopSearchResultView;
import com.sap.sailing.gwt.home.desktop.places.solutions.SolutionsView;
import com.sap.sailing.gwt.home.desktop.places.solutions.TabletAndDesktopSolutionsView;
import com.sap.sailing.gwt.home.desktop.places.sponsoring.SponsoringView;
import com.sap.sailing.gwt.home.desktop.places.sponsoring.TabletAndDesktopSponsoringView;
import com.sap.sailing.gwt.home.desktop.places.start.StartView;
import com.sap.sailing.gwt.home.desktop.places.start.TabletAndDesktopStartView;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.TabletAndDesktopWhatsNewView;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewView;
import com.sap.sailing.gwt.home.shared.app.UserManagementContext;
import com.sap.sailing.gwt.home.shared.app.UserManagementContextImpl;
import com.sap.sailing.gwt.home.shared.framework.WrappedPlacesManagementController;
import com.sap.sailing.gwt.home.shared.framework.WrappedPlacesManagementController.StartPlaceActivityMapper;
import com.sap.sailing.gwt.home.shared.partials.busy.BusyViewImpl;
import com.sap.sailing.gwt.home.shared.places.searchresult.SearchResultView;
import com.sap.sailing.gwt.home.shared.places.solutions.SolutionsPlace.SolutionsNavigationTabs;
import com.sap.sailing.gwt.home.shared.usermanagement.RequiresLoggedInUser;
import com.sap.sailing.gwt.home.shared.usermanagement.UserManagementContextEvent;
import com.sap.sailing.gwt.home.shared.usermanagement.UserManagementRequestEvent;
import com.sap.sailing.gwt.home.shared.usermanagement.create.CreateAccountActivity;
import com.sap.sailing.gwt.home.shared.usermanagement.create.CreateAccountPlace;
import com.sap.sailing.gwt.home.shared.usermanagement.flyover.UserManagementFlyover;
import com.sap.sailing.gwt.home.shared.usermanagement.info.LoggedInUserInfoActivity;
import com.sap.sailing.gwt.home.shared.usermanagement.info.LoggedInUserInfoPlace;
import com.sap.sailing.gwt.home.shared.usermanagement.recovery.PasswordRecoveryActivity;
import com.sap.sailing.gwt.home.shared.usermanagement.recovery.PasswordRecoveryPlace;
import com.sap.sailing.gwt.home.shared.usermanagement.signin.SignInActivity;
import com.sap.sailing.gwt.home.shared.usermanagement.signin.SignInPlace;
import com.sap.sailing.gwt.ui.client.refresh.BusyView;
import com.sap.sse.security.ui.client.DefaultWithSecurityImpl;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.client.WithSecurity;
import com.sap.sse.security.ui.shared.UserDTO;


public class TabletAndDesktopApplicationClientFactory extends AbstractApplicationClientFactory<ApplicationTopLevelView<DesktopResettableNavigationPathDisplay>> implements DesktopClientFactory {
    private final SailingDispatchSystem dispatch = new SailingDispatchSystemImpl();
    private final WithSecurity securityProvider;
    private final WrappedPlacesManagementController userManagementWizardController;
    private UserManagementContext uCtx = new UserManagementContextImpl();
    
    public TabletAndDesktopApplicationClientFactory(boolean isStandaloneServer) {
        this(new SimpleEventBus(), isStandaloneServer);
    }
    
    private TabletAndDesktopApplicationClientFactory(EventBus eventBus, boolean isStandaloneServer) {
        this(eventBus, new PlaceController(eventBus), isStandaloneServer);
    }

    private TabletAndDesktopApplicationClientFactory(EventBus eventBus, PlaceController placeController, boolean isStandaloneServer) {
        this(eventBus, placeController, new DesktopPlacesNavigator(placeController, isStandaloneServer));
    }

    private TabletAndDesktopApplicationClientFactory(EventBus eventBus, PlaceController placeController, DesktopPlacesNavigator placesNavigator) {
        super(new TabletAndDesktopApplicationView(placesNavigator, eventBus), eventBus, placeController, placesNavigator);
        securityProvider = new DefaultWithSecurityImpl();
        securityProvider.getUserService().addUserStatusEventHandler(new UserStatusEventHandler() {
            @Override
            public void onUserStatusChange(UserDTO user) {
                uCtx = new UserManagementContextImpl(user);
                getEventBus().fireEvent(new UserManagementContextEvent(uCtx));
            }
        });
        
        final UserManagementFlyover userManagementDisplay = new UserManagementFlyover();
        this.userManagementWizardController = new WrappedPlacesManagementController(
                new DesktopUserManagementStartPlaceActivityMapper(), userManagementDisplay);
        getEventBus().addHandler(UserManagementRequestEvent.TYPE, new UserManagementRequestEvent.Handler() {
            @Override
            public void onUserManagementRequestEvent(UserManagementRequestEvent event) {
                if (userManagementDisplay.isShowing()) {
                    userManagementDisplay.hide();
                } else {
                    userManagementDisplay.setWidget(createBusyView());
                    userManagementDisplay.show();
                    userManagementWizardController.start();
                }
            }
        });
        getEventBus().addHandler(UserManagementContextEvent.TYPE, new UserManagementContextEvent.Handler() {
            @Override
            public void onUserChangeEvent(UserManagementContextEvent event) {
                userManagementWizardController.fireEvent(event);
            }
        });
    }
    
    @Override
    public DesktopResettableNavigationPathDisplay getNavigationPathDisplay() {
        return getTopLevelView().getNavigationPathDisplay();
    }

    @Override
    public TabletAndDesktopErrorView createErrorView(String errorMessage, Throwable errorReason) {
        return new TabletAndDesktopErrorView(errorMessage, errorReason, null);
    }

    @Override
    public EventsView createEventsView() {
        return new TabletAndDesktopEventsView(getHomePlacesNavigator());
    }

    @Override
    public StartView createStartView() {
        return new TabletAndDesktopStartView(getHomePlacesNavigator());
    }

    @Override
    public SponsoringView createSponsoringView() {
        return new TabletAndDesktopSponsoringView();
    }

    @Override
    public SolutionsView createSolutionsView(SolutionsNavigationTabs navigationTab) {
        return new TabletAndDesktopSolutionsView(navigationTab, getHomePlacesNavigator());
    }

    @Override
    public SearchResultView createSearchResultView() {
        return new TabletAndDesktopSearchResultView(getHomePlacesNavigator());
    }

    @Override
    public WhatsNewView createWhatsNewView(WhatsNewNavigationTabs navigationTab) {
        return new TabletAndDesktopWhatsNewView(navigationTab, getHomePlacesNavigator());
    }

    @Override
    public SailingDispatchSystem getDispatch() {
        return dispatch;
    }

    @Override
    public BusyView createBusyView() {
        return new BusyViewImpl();
    }

    @Override
    public UserManagementServiceAsync getUserManagement() {
        return securityProvider.getUserManagementService();
    }

    @Override
    public UserManagementContext getUserManagementContext() {
        return uCtx;
    }
    
    @Override
    public void resetUserManagementContext() {
        uCtx = new UserManagementContextImpl();
        securityProvider.getUserService().updateUser(true);
        getEventBus().fireEvent(new UserManagementRequestEvent());
    }

    @Override
    public void didLogin(UserDTO user) {
        uCtx = new UserManagementContextImpl(user);
        securityProvider.getUserService().updateUser(true);
        getEventBus().fireEvent(new UserManagementContextEvent(uCtx));
    }

    private class DesktopUserManagementStartPlaceActivityMapper implements StartPlaceActivityMapper {
        private PlaceController placeController;

        @Override
        public Activity getActivity(final Place requestedPlace) {
            final Place placeToUse;
            if (requestedPlace instanceof RequiresLoggedInUser && !uCtx.isLoggedIn()) {
                placeToUse = getStartPlace();
            } else {
                placeToUse = requestedPlace;
            }

            final TabletAndDesktopApplicationClientFactory cf = TabletAndDesktopApplicationClientFactory.this;
            if (placeToUse instanceof SignInPlace) {
                return new SignInActivity((SignInPlace) placeToUse, cf, placeController);
            } else if (placeToUse instanceof CreateAccountPlace) {
                return new CreateAccountActivity((CreateAccountPlace) placeToUse, cf, 
                	getHomePlacesNavigator().getCreateConfirmationNavigation(), placeController);
            } else if (placeToUse instanceof PasswordRecoveryPlace) {
                return new PasswordRecoveryActivity<TabletAndDesktopApplicationClientFactory>(
                        (PasswordRecoveryPlace) placeToUse, cf, placeController);
            } else if (placeToUse instanceof LoggedInUserInfoPlace) {
                return new LoggedInUserInfoActivity((LoggedInUserInfoPlace) placeToUse, cf, placeController);
            }
            
            return new SignInActivity(new SignInPlace(new LoggedInUserInfoPlace()), cf, placeController);
        }
        
        @Override
        public Place getStartPlace() {
            if (uCtx.isLoggedIn()) {
                return new LoggedInUserInfoPlace();
            } else {
                return new SignInPlace(new LoggedInUserInfoPlace());
            }
        }

        @Override
        public void setPlaceController(PlaceController placeController) {
            this.placeController = placeController;
        }
    }

}
