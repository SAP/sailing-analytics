package com.sap.sailing.gwt.home.shared.places.user.profile.subscriptions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.subscription.SubscriptionPlace;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.dialog.ConfirmationDialog;
import com.sap.sse.security.shared.subscription.InvalidSubscriptionProviderException;
import com.sap.sse.security.ui.authentication.WithUserService;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.WithSecurity;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;
import com.sap.sse.security.ui.client.subscription.SubscriptionWriteServiceAsync;
import com.sap.sse.security.ui.shared.subscription.SubscriptionListDTO;

/**
 * Implementation presenter of {@link UserSubscriptionsView.Presenter}
 *
 * @author Tu Tran
 */
public class UserSubscriptionsPresenter<C extends WithUserService & WithSecurity>
        implements UserSubscriptionsView.Presenter {

    private final SubscriptionServiceFactory factory;
    private final UserService userService;
    private final PlaceNavigation<SubscriptionPlace> subscribePlaceNavigation;

    private UserSubscriptionsView view;

    public UserSubscriptionsPresenter(final C clientFactory, final PlaceNavigation<SubscriptionPlace> subscribePlaceNavigation) {
        this.factory = clientFactory.getSubscriptionServiceFactory();
        this.userService = clientFactory.getUserService();
        this.subscribePlaceNavigation = subscribePlaceNavigation;
    }

    @Override
    public void init() {
        factory.initializeProviders();
    }

    @Override
    public void loadSubscription() {
        fetchSubscription();
    }

    @Override
    public void setView(final UserSubscriptionsView view) {
        this.view = view;
    }

    @Override
    public void cancelSubscription(final String planId, final String providerName) {
        ConfirmationDialog.create(StringMessages.INSTANCE.confirmCancelSubscriptionTitle(),
                StringMessages.INSTANCE.confirmCancelSubscriptionText(), StringMessages.INSTANCE.confirm(),
                StringMessages.INSTANCE.cancel(), (cancel) -> {
                    if (cancel) {
                        try {
                            final SubscriptionWriteServiceAsync<?, ?, ?> service = factory
                                    .getWriteAsyncServiceByProvider(providerName);
                            service.cancelSubscription(planId, new AsyncCallback<Boolean>() {
                                @Override
                                public void onSuccess(final Boolean result) {
                                    if (!result) {
                                        showError(StringMessages.INSTANCE.failedCancelSubscription());
                                    } else {
                                        userService.updateUser(true);
                                        fetchSubscription();
                                    }
                                }

                                @Override
                                public void onFailure(final Throwable caught) {
                                    showError(StringMessages.INSTANCE.errorCancelSubscription(caught.getMessage()));
                                }
                            });
                        } catch (final InvalidSubscriptionProviderException e) {
                            onInvalidSubscriptionProviderError(e);
                        }
                    }
                }).center();
    }

    private void fetchSubscription() {
        try {
            factory.getDefaultAsyncService().getSubscriptions(false, new AsyncCallback<SubscriptionListDTO>() {
                @Override
                public void onSuccess(final SubscriptionListDTO result) {
                    if (result != null && result.getError() != null && !result.getError().isEmpty()) {
                        showError(StringMessages.INSTANCE.errorLoadingUserSubscription(result.getError()));
                    } else {
                        view.updateView(result);
                    }
                }

                @Override
                public void onFailure(final Throwable caught) {
                    showError(StringMessages.INSTANCE.errorLoadingUserSubscription(caught.getMessage()));
                }
            });
        } catch (final InvalidSubscriptionProviderException e) {
            onInvalidSubscriptionProviderError(e);
        }
    }

    private void showError(final String message) {
        Notification.notify(message, NotificationType.ERROR);
    }

    private void onInvalidSubscriptionProviderError(final InvalidSubscriptionProviderException e) {
        showError(StringMessages.INSTANCE.errorInvalidSubscritionProvider(e.getMessage()));
    }

    @Override
    public void navigateToSubscribe() {
        subscribePlaceNavigation.goToPlace();
    }
}
