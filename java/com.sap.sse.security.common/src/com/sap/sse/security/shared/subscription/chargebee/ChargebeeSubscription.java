package com.sap.sse.security.shared.subscription.chargebee;

import com.sap.sse.common.TimePoint;
import com.sap.sse.security.shared.subscription.Subscription;

public class ChargebeeSubscription extends Subscription {
    public static final String SUBSCRIPTION_STATUS_TRIAL = "in_trial";
    public static final String SUBSCRIPTION_STATUS_ACTIVE = "active";
    public static final String SUBSCRIPTION_STATUS_CANCELLED = "cancelled";
    public static final String SUBSCRIPTION_STATUS_PAUSED = "paused";

    public static final String TRANSACTION_TYPE_PAYMENT = "payment";
    public static final String TRANSACTION_TYPE_REFUND = "refund";

    public static final String TRANSACTION_STATUS_SUCCESS = "success";

    private static final long serialVersionUID = -3682427457347116687L;

    public static Subscription createEmptySubscription(String planId, TimePoint latestEventTime,
            TimePoint manualUpdatedAt) {
        return new ChargebeeSubscription(null, planId, null, Subscription.emptyTime(), Subscription.emptyTime(), null,
                null, null, null, null, null, Subscription.emptyTime(), Subscription.emptyTime(), latestEventTime,
                manualUpdatedAt);
    }

    public ChargebeeSubscription(String subscriptionId, String planId, String customerId, TimePoint trialStart,
            TimePoint trialEnd, String subscriptionStatus, String paymentStatus, String transactionType,
            String transactionStatus, String invoiceId, String invoiceStatus, TimePoint subscriptionCreatedAt,
            TimePoint subscriptionUpdatedAt, TimePoint latestEventTime, TimePoint manualUpdatedAt) {
        super(subscriptionId, planId, customerId, trialStart, trialEnd, subscriptionStatus, paymentStatus,
                transactionType, transactionStatus, invoiceId, invoiceStatus, subscriptionCreatedAt,
                subscriptionUpdatedAt, latestEventTime, manualUpdatedAt,
                ChargeeSubscriptionProvider.getInstance().getProviderName());
    }

    public boolean isActiveSubscription() {
        String subscriptionStatus = getSubscriptionStatus();
        String paymentStatus = getPaymentStatus();
        String transactionType = getTransactionType();
        return subscriptionStatus != null && (subscriptionStatus.equals(SUBSCRIPTION_STATUS_TRIAL)
                || (subscriptionStatus.equals(SUBSCRIPTION_STATUS_ACTIVE) && paymentStatus != null
                        && paymentStatus.equals(PAYMENT_STATUS_SUCCESS) && transactionType != null
                        && transactionType.equals(TRANSACTION_TYPE_PAYMENT)));
    }
}
