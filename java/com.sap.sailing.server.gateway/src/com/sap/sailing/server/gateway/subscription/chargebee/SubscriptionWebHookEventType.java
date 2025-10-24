package com.sap.sailing.server.gateway.subscription.chargebee;

/**
 * Enum for payment service WebHook event types
 * 
 * {@link https://apidocs.chargebee.com/docs/api/events#event_types}
 * 
 * @author Tu Tran
 */
public enum SubscriptionWebHookEventType {
    CUSTOMER_DELETED("customer_deleted"),
    SUBSCRIPTION_DELETED("subscription_deleted"),
    SUBSCRIPTION_CREATED("subscription_created"),
    SUBSCRIPTION_CHANGED("subscription_changed"),
    SUBSCRIPTION_CANCELLED("subscription_cancelled"),
    PAYMENT_SUCCEEDED("payment_succeeded"),
    PAYMENT_FAILED("payment_failed"),
    SUBSCRIPTION_ACTIVATED("subscription_activated"),
    INVOICE_GENERATED("invoice_generated"),
    INVOICE_UPDATED("invoice_updated"),
    PAYMENT_REFUNDED("payment_refunded"),
    SUBSCRIPTION_PAUSED("subscription_paused"),
    SUBSCRIPTION_RESUMED("subscription_resumed"),
    SUBSCRIPTION_CANCELLATION_REMINDER("subscription_cancellation_reminder");

    private final String name;

    SubscriptionWebHookEventType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
