package com.sap.sailing.gwt.home.mobile.app;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystemImpl;
import com.sap.sailing.gwt.home.desktop.app.ApplicationTopLevelView;
import com.sap.sailing.gwt.home.mobile.places.error.ErrorViewImpl;
import com.sap.sailing.gwt.home.mobile.places.searchresult.SearchResultViewImpl;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.app.ResettableNavigationPathDisplay;
import com.sap.sailing.gwt.home.shared.partials.busy.BusyViewImpl;
import com.sap.sailing.gwt.home.shared.partials.dialog.whatsnew.WhatsNewDialogFactory;
import com.sap.sailing.gwt.home.shared.places.searchresult.SearchResultClientFactory;
import com.sap.sailing.gwt.home.shared.places.searchresult.SearchResultView;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sailing.gwt.home.shared.places.subscription.SubscriptionClientFactory;
import com.sap.sailing.gwt.home.shared.places.subscription.SubscriptionView;
import com.sap.sailing.gwt.home.shared.places.subscription.SubscriptionViewImpl;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationClientFactory;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationPlace;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationView;
import com.sap.sailing.gwt.home.shared.places.user.confirmation.ConfirmationViewImpl;
import com.sap.sailing.gwt.home.shared.places.user.passwordreset.PasswordResetClientFactory;
import com.sap.sailing.gwt.home.shared.places.user.passwordreset.PasswordResetView;
import com.sap.sailing.gwt.home.shared.places.user.passwordreset.PasswordResetViewImpl;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.ClientFactoryWithDispatchAndErrorAndUserService;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.refresh.BusyView;
import com.sap.sse.gwt.client.mvp.ErrorView;
import com.sap.sse.security.shared.subscription.InvalidSubscriptionProviderException;
import com.sap.sse.security.ui.authentication.AuthenticationManager;
import com.sap.sse.security.ui.authentication.AuthenticationManagerImpl;
import com.sap.sse.security.ui.authentication.WithAuthenticationManager;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.authentication.login.LoginHintContent;
import com.sap.sse.security.ui.client.SecureClientFactoryImpl;
import com.sap.sse.security.ui.client.i18n.StringMessages;

/**
 *
 * @author pgtaboada
 *
 */
public class MobileApplicationClientFactory extends
        SecureClientFactoryImpl<ApplicationTopLevelView<ResettableNavigationPathDisplay>> implements
        SubscriptionClientFactory, SearchResultClientFactory, ConfirmationClientFactory, PasswordResetClientFactory,
        WithAuthenticationManager, ClientFactoryWithDispatchAndErrorAndUserService {
    private final MobilePlacesNavigator navigator;
    private final SailingDispatchSystem dispatch = new SailingDispatchSystemImpl();
    private final AuthenticationManager authenticationManager;
    private final SailingServiceAsync sailingService;

    public MobileApplicationClientFactory(final boolean isStandaloneServer, final SailingServiceAsync sailingService) {
        this(new SimpleEventBus(), isStandaloneServer, sailingService);
    }

    private MobileApplicationClientFactory(final SimpleEventBus eventBus, final boolean isStandaloneServer,
            final SailingServiceAsync sailingService) {
        this(eventBus, new PlaceController(eventBus), isStandaloneServer, sailingService);
    }

    private MobileApplicationClientFactory(final EventBus eventBus, final PlaceController placeController,
            final boolean isStandaloneServer, final SailingServiceAsync sailingService) {
        this(eventBus, placeController, new MobilePlacesNavigator(placeController, isStandaloneServer), sailingService);
    }

    private MobileApplicationClientFactory(final EventBus eventBus, final PlaceController placeController,
            final MobilePlacesNavigator navigator, final SailingServiceAsync sailingService) {
        this(new MobileApplicationView(navigator, eventBus), eventBus, placeController, navigator, sailingService);
    }

    public MobileApplicationClientFactory(final MobileApplicationView root, final EventBus eventBus,
            final PlaceController placeController, final MobilePlacesNavigator navigator,
            final SailingServiceAsync sailingService) {
        super(root, eventBus, placeController);
        this.navigator = navigator;
        this.sailingService = sailingService;
        this.authenticationManager = new AuthenticationManagerImpl(this, eventBus, getNavigator()
                .getMailVerifiedConfirmationNavigation().getFullQualifiedUrl(), getNavigator()
                .getPasswordResetNavigation().getFullQualifiedUrl());
        WhatsNewDialogFactory.register(getUserService(), placeController);

        authenticationManager.checkNewUserPopup(() -> root.setSubHeaderContent(null), dismissCallback -> {
            final LoginHintContent content = new LoginHintContent(() -> {
                root.setSubHeaderContent(null);
                dismissCallback.run();
            }, () -> {
                root.setSubHeaderContent(null);
                dismissCallback.run();
                navigator.goToPlace(navigator.getMoreLoginInfo());
            },() -> {
                navigator.goToPlace(navigator.getSignInNavigation());
            });
            root.setSubHeaderContent(content);
        });
    }

    public MobilePlacesNavigator getNavigator() {
        return navigator;
    }

    @Override
    public SailingDispatchSystem getDispatch() {
        return dispatch;
    }

    @Override
    public Place getDefaultPlace() {
        return new StartPlace();
    }

    @Override
    public BusyView createBusyView() {
        return new BusyViewImpl();
    }

    @Override
    public ErrorView createErrorView(final String errorMessage, final Throwable errorReason) {
        return new ErrorViewImpl(errorMessage, errorReason, null);
    }

    @Override
    public SubscriptionView createSubscriptionsView() {
        getSubscriptionServiceFactory().initializeProviders();
        final SubscriptionView subscriptionView = new SubscriptionViewImpl();
        final SubscriptionView.Presenter presenter = new SubscriptionView.Presenter() {
            @Override
            public void startSubscription(final String priceId) {
                try {
                    getSubscriptionServiceFactory().getDefaultProvider().getSubscriptionViewPresenter()
                            .startCheckout(priceId, subscriptionView, () -> getUserService().updateUser(true));
                } catch (final InvalidSubscriptionProviderException e) {
                    subscriptionView.onOpenCheckoutError(e.toString());
                }
            }
            @Override
            public void manageSubscriptions() {
                navigator.goToPlace(navigator.getUserSubscriptionsNavigation());
            }
            @Override
            public void toggleAuthenticationFlyout() {
                navigator.goToPlace(navigator.getSignInNavigation());
            }
            @Override
            public AuthenticationContext getAuthenticationContext() {
                return authenticationManager.getAuthenticationContext();
            }
            @Override
            public SubscriptionClientFactory getClientFactory() {
                return MobileApplicationClientFactory.this;
            }
        };
        subscriptionView.setPresenter(presenter);
        return subscriptionView;
    }

    @Override
    public SearchResultView createSearchResultView() {
        return new SearchResultViewImpl(navigator);
    }

    public ResettableNavigationPathDisplay getNavigationPathDisplay() {
        return getTopLevelView().getNavigationPathDisplay();
    }

    @Override
    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    @Override
    public ConfirmationView createConfirmationView() {
        return new ConfirmationViewImpl(SharedResources.INSTANCE, StringMessages.INSTANCE.accountConfirmation());
    }

    @Override
    public PasswordResetView createPasswordResetView() {
        return new PasswordResetViewImpl();
    }

    @Override
    public PlaceNavigation<ConfirmationPlace> getPasswordResettedConfirmationNavigation(final String username) {
        return getNavigator().getPasswordResettedConfirmationNavigation(username);
    }

    @Override
    public SailingServiceAsync getSailingService() {
        return sailingService;
    }
}
