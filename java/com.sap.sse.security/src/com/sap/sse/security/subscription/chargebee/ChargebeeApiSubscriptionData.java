package com.sap.sse.security.subscription.chargebee;

import java.sql.Timestamp;

import com.chargebee.models.Invoice;
import com.chargebee.models.Subscription;
import com.chargebee.models.Subscription.SubscriptionItem;
import com.chargebee.models.Subscription.SubscriptionItem.ItemType;
import com.chargebee.models.Transaction;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.shared.SubscriptionPlanProvider;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;
import com.sap.sse.security.shared.subscription.SubscriptionPrice;
import com.sap.sse.security.shared.subscription.chargebee.ChargebeeSubscription;

public class ChargebeeApiSubscriptionData {
    private final Subscription subscription;
    private final Invoice invoice;
    private final Transaction transaction;

    public ChargebeeApiSubscriptionData(Subscription subscription, Invoice invoice,
            Transaction transaction) {
        this.subscription = subscription;
        this.invoice = invoice;
        this.transaction = transaction;
    }

    public ChargebeeSubscription toSubscription(SubscriptionPlanProvider subscriptionPlanProvider) {
        String subscriptionStatus = null;
        if (subscription.status() != null) {
            subscriptionStatus = stringToLowerCase(subscription.status().name());
        }
        String transactionType = null;
        String transactionStatus = null;
        if (transaction != null) {
            transactionType = stringToLowerCase(transaction.type().name());
            transactionStatus = stringToLowerCase(transaction.status().name());
        }
        String invoiceId = null;
        String invoiceStatus = null;
        if (invoice != null) {
            invoiceId = invoice.id();
            invoiceStatus = stringToLowerCase(invoice.status().name());
        }
        final String paymentStatus = ChargebeeSubscription.determinePaymentStatus(transactionType, transactionStatus,
                invoiceStatus);
        final String planId = getPlanId(subscriptionPlanProvider);
        final int reoccuringPaymentValue = calculateReoccuringPaymentValue();
        return new ChargebeeSubscription(subscription.id(), planId, subscription.customerId(),
                getTime(subscription.trialStart()), getTime(subscription.trialEnd()), subscriptionStatus, paymentStatus,
                transactionType, transactionStatus, invoiceId, invoiceStatus, reoccuringPaymentValue,
                subscription.currencyCode(), getTime(subscription.createdAt()), getTime(subscription.updatedAt()),
                getTime(subscription.activatedAt()), getTime(subscription.nextBillingAt()),
                getTime(subscription.currentTermEnd()), getTime(subscription.cancelledAt()), TimePoint.now(),
                com.sap.sse.security.shared.subscription.Subscription.emptyTime());
    }

    private int calculateReoccuringPaymentValue() {
        int reoccuringPaymentValue = 0;
        for (SubscriptionItem item : subscription.subscriptionItems()){
            if (item.amount() != null) {
                reoccuringPaymentValue += item.amount();
            }
        }
        return reoccuringPaymentValue;
    }
    
    private String getPlanId(SubscriptionPlanProvider subscriptionPlanProvider) {
        for(SubscriptionItem item : subscription.subscriptionItems()) {
            if(item.itemType().equals(ItemType.PLAN)) {
                final String itemPriceId = item.itemPriceId();
                for(SubscriptionPlan plan : subscriptionPlanProvider.getAllSubscriptionPlans().values()) {
                    for(SubscriptionPrice price : plan.getPrices()) {
                        if(price.getPriceId().equals(itemPriceId)){
                            return plan.getId();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private TimePoint getTime(Timestamp millis) {
        return millis == null ? com.sap.sse.security.shared.subscription.Subscription.emptyTime()
                : TimePoint.of(millis);
    }

    private String stringToLowerCase(String str) {
        return str == null ? null : str.toLowerCase();
    }
}
