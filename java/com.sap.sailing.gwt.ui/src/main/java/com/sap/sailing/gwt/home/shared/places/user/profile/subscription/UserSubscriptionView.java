package com.sap.sailing.gwt.home.shared.places.user.profile.subscription;

import com.google.gwt.user.client.ui.IsWidget;
import com.sap.sailing.gwt.ui.shared.subscription.SubscriptionDTO;

/**
 * View for displaying user subscription information like plan, subscription status...In this view user is able to
 * subscribe to a plan or cancel current subscription.
 * 
 * @author Tu Tran
 */
public interface UserSubscriptionView extends IsWidget {
    /**
     * Called on start loading subscription data
     */
    public void onStartLoadSubscription();

    /**
     * Update the view with subscription data returned from back-end
     */
    public void updateView(SubscriptionDTO subscription);

    /**
     * Called on checkout modal is closed
     */
    public void onCloseCheckoutModal();

    /**
     * Called on checkout modal has errors
     */
    public void onOpenCheckoutError(String error);

    /**
     * Presenter for {@link UserSubscriptionView}
     */
    public interface Presenter {
        /**
         * Initialize
         */
        public void init();

        /**
         * Load user's subscription data
         */
        public void loadSubscription();

        /**
         * Open checkout modal from which user can create new subscription or change current subscription
         * 
         * @param planId
         *            Id of plan to subscribe to
         */
        public void openCheckout(String planId);

        public void setView(UserSubscriptionView view);

        /**
         * Request to cancel current user's subscription
         * 
         * @param planId
         *            Id of plan to cancel
         * @param provider
         *            subscription provider name
         */
        public void cancelSubscription(String planId, String provider);
    }
}
