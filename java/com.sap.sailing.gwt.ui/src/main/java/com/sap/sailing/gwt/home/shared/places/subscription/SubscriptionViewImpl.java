package com.sap.sailing.gwt.home.shared.places.subscription;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Composite;
import com.sap.sailing.gwt.home.desktop.partials.subscription.SubscriptionCard;
import com.sap.sailing.gwt.home.desktop.partials.subscription.SubscriptionCardContainer;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;

public class SubscriptionViewImpl extends Composite implements SubscriptionView {

    private final SubscriptionCardContainer container = new SubscriptionCardContainer();
    private Presenter presenter;

    public SubscriptionViewImpl() {
        initWidget(container);
    }

    @Override
    public void setPresenter(final Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void addSubscriptionCategory(final SubscriptionGroupDTO category, final SubscriptionCard.Type type,
            final EventBus eventBus) {
        switch (type) {
        case HIGHLIGHT:
        case DEFAULT:
            container.addSubscription(new SubscriptionCard(category, type, (price) -> {
                final AuthenticationContext authenticationContext = presenter.getAuthenticationContext();
                if (!authenticationContext.isLoggedIn()) {
                    onOpenCheckoutError(StringMessages.INSTANCE.notLoggedIn());
                    presenter.toggleAuthenticationFlyout();
                } else if(presenter.isMailVerificationRequired() && !authenticationContext.getCurrentUser().isEmailValidated()) {
                    onOpenCheckoutError(StringMessages.INSTANCE.mailNotValidated());
                    presenter.toggleAuthenticationFlyout();
                }else {
                    if (price != null) {
                        presenter.startSubscription(price.getPriceId());
                    }
                }
            }, eventBus, presenter.getAuthenticationContext().isLoggedIn()));
            break;
        case OWNER:
            container.addSubscription(new SubscriptionCard(category, type, price -> presenter.manageSubscriptions(),
                    eventBus, presenter.getAuthenticationContext().isLoggedIn()));
            break;
        case ONETIMELOCK:
            container.addSubscription(new SubscriptionCard(category, type, price -> {},
                    eventBus, presenter.getAuthenticationContext().isLoggedIn()));
            break;
        case FREE:
            container.addSubscription(new SubscriptionCard(category, type, price -> presenter.toggleAuthenticationFlyout(),
                    eventBus, presenter.getAuthenticationContext().isLoggedIn()));
            break;
        default:
            break;
        }
    }

    @Override
    public void onCloseCheckoutModal() {
        // FIXME Is any action required in this case?
    }

    @Override
    public void onOpenCheckoutError(final String error) {
        Notification.notify(error, NotificationType.ERROR);
    }
    
    @Override
    public void onFinishedPayment(final String message) {
        Notification.notify(message, NotificationType.SUCCESS);
    }
    
    @Override
    public void onUnfinishedPayment(final String message) {
        Notification.notify(message, NotificationType.WARNING);
    }
    @Override
    public void resetSubscriptions() {
        container.resetSubscriptions();
    }

}
