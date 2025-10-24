package com.sap.sailing.server.gateway.subscription.chargebee;

import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.TimePoint;
import com.sap.sse.security.shared.subscription.Subscription;

/**
 * Wrapped class for WebHook event JSON object
 * 
 * @author Tu Tran
 */
public class SubscriptionWebHookEvent {
    private static final Logger logger = Logger.getLogger(SubscriptionWebHookEvent.class.getName());
    public static final String SUBSCRIPTION_STATUS_ACTIVE = "active";
    public static final String INVOICE_STATUS_PAID = "paid";

    private static final String CONTENT_JSON_OBJECT = "content";
    private static final String EVENT_ID = "id";
    private static final String EVENT_OCCURRED_AT = "occurred_at";
    private static final String EVENT_TYPE = "event_type";

    private static final String CUSTOMER_JSON_OBJECT = "customer";
    private static final String CUSTOMER_ID = "id";
    private static final String CUSTOMER_EMAIL = "email";

    private static final String SUBSCRIPTION_JSON_OBJECT = "subscription";
    private static final String SUBSCRIPTION_ID = "id";
    private static final String SUBSCRIPTION_PLAN_ID = "plan_id";
    private static final String SUBSCRIPTION_STATUS = "status";
    private static final String SUBSCRIPTION_TRIAL_START = "trial_start";
    private static final String SUBSCRIPTION_TRIAL_END = "trial_end";
    private static final String SUBSCRIPTION_CREATED_AT = "created_at";
    private static final String SUBSCRIPTION_UPDTAED_AT = "updated_at";

    private static final String TRANSACTION_JSON_OBJECT = "transaction";
    private static final String TRANSACTION_STATUS = "status";
    private static final String TRANSACTION_TYPE = "type";

    private static final String INVOICE_JSON_OBJECT = "invoice";
    private static final String INVOICE_ID = "id";
    private static final String INVOICE_CUSTOMER_ID = "customer_id";
    private static final String INVOICE_SUBSCRIPTION_ID = "subscription_id";
    private static final String INVOICE_STATUS = "status";
    
    private static final String NEXT_BILLING_AT = "next_billing_at";
    private static final String CURRENT_TERM_END = "current_term_end";
    private static final String CANCELLED_AT = "cancelled_at";
    private static final String SUBSCRIPTION_ACTIVATED_AT = "subscription_activated_at";
    private static final String CURRENCY_CODE = "currency_code";

    private final JSONObject eventJSON;
    private final String eventId;
    private final SubscriptionWebHookEventType eventType;
    private final JSONObject content;

    public SubscriptionWebHookEvent(JSONObject eventJSON) {
        this.eventJSON = eventJSON;
        eventId = getJsonValue(eventJSON, EVENT_ID);
        eventType = getEventType(eventJSON);
        content = getJsonValue(eventJSON, CONTENT_JSON_OBJECT);
    }

    public boolean isValidEvent() {
        return eventId != null;
    }

    public String getEventId() {
        return eventId;
    }

    private SubscriptionWebHookEventType getEventType(JSONObject eventJSON) {
        final String eventTypeAsUppercaseString = toUpperCase(getJsonValue(eventJSON, EVENT_TYPE));
        try {
            return SubscriptionWebHookEventType.valueOf(eventTypeAsUppercaseString);
        } catch (NullPointerException | IllegalArgumentException e) {
            logger.warning(() -> "Exception trying to extract subscription event type " + eventTypeAsUppercaseString
                    + ": " + e.getMessage());
            return null;
        }
    }

    public SubscriptionWebHookEventType getEventType() {
        return eventType;
    }

    public String getCustomerEmail() {
        return getJsonValue(content, CUSTOMER_JSON_OBJECT, CUSTOMER_EMAIL);
    }

    public String getCustomerId() {
        String customerId = getJsonValue(content, CUSTOMER_JSON_OBJECT, CUSTOMER_ID);
        if (StringUtils.isEmpty(customerId)) {
            customerId = getInvoiceCustomerId();
        }
        return customerId;
    }

    public String getPlanId() {
        return getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_PLAN_ID);
    }
    
    public String getItemPriceId() {
        String itemPriceId = null;
        final JSONArray subscriptionItems = getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, "subscription_items");
        if(subscriptionItems != null) {
            for (int i = 0; i < subscriptionItems.size(); i++) {
                final String itemType = getJsonValue((JSONObject) subscriptionItems.get(i), "item_type");
                if("plan".equals(itemType)) {
                    itemPriceId = getJsonValue((JSONObject) subscriptionItems.get(i), "item_price_id");
                }
            }
        }
        return itemPriceId;
    }
 
    public String getSubscriptionId() {
        String subscriptionId = getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_ID);
        if (StringUtils.isEmpty(subscriptionId)) {
            subscriptionId = getInvoiceSubscriptionId();
        }
        return subscriptionId;
    }

    public String getSubscriptionStatus() {
        return toLowerCase(getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_STATUS));
    }

    public TimePoint getSubscriptionTrialStart() {
        final Long jsonValue = getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_TRIAL_START);
        return getTime(jsonValue);
    }

    public TimePoint getSubscriptionTrialEnd() {
        final Long jsonValue = getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_TRIAL_END);
        return getTime(jsonValue);
    }

    public TimePoint getSubscriptionCreatedAt() {
        return getTime(getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_CREATED_AT));
    }

    public TimePoint getSubscriptionUpdatedAt() {
        return getTime(getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_UPDTAED_AT));
    }

    public TimePoint getEventOccurredAt() {
        return getTime(getJsonValue(eventJSON, EVENT_OCCURRED_AT));
    }

    public String getTransactionStatus() {
        return toLowerCase(getJsonValue(content, TRANSACTION_JSON_OBJECT, TRANSACTION_STATUS));
    }

    public String getTransactionType() {
        return toLowerCase(getJsonValue(content, TRANSACTION_JSON_OBJECT, TRANSACTION_TYPE));
    }

    public String getInvoiceId() {
        return getJsonValue(content, INVOICE_JSON_OBJECT, INVOICE_ID);
    }

    public String getInvoiceCustomerId() {
        return getJsonValue(content, INVOICE_JSON_OBJECT, INVOICE_CUSTOMER_ID);
    }

    public String getInvoiceSubscriptionId() {
        return getJsonValue(content, INVOICE_JSON_OBJECT, INVOICE_SUBSCRIPTION_ID);
    }
    
    public String getInvoiceStatus() {
        return toLowerCase(getJsonValue(content, INVOICE_JSON_OBJECT, INVOICE_STATUS));
    }
    
    public TimePoint getBillingAt() {
        return getTime(getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, NEXT_BILLING_AT));
    }
    
    public TimePoint getCurrentTermEnd() {
        return getTime(getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, CURRENT_TERM_END));
    }
    
    public TimePoint getCancelledAt() {
        return getTime(getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, CANCELLED_AT));
    }
    
    public TimePoint getActivatedAt() {
        return getTime(getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, SUBSCRIPTION_ACTIVATED_AT));
    }
    
    public Integer getReocurringPaymentValue() {
        int reocurringPaymentValue = 0;
        final JSONArray subscriptionItems = getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, "subscription_items");
        if (subscriptionItems != null) {
            for (int i = 0; i < subscriptionItems.size(); i++) {
                final Long amount = getJsonValue((JSONObject) subscriptionItems.get(i), "amount");
                if (amount != null) {
                    reocurringPaymentValue += amount.intValue();
                }
            }
        }
        return reocurringPaymentValue;
    }
    
    public String getCurrencyCode() {
        return getJsonValue(content, SUBSCRIPTION_JSON_OBJECT, CURRENCY_CODE);
    }

    private TimePoint getTime(Long timestamp) {
        return timestamp == null ? Subscription.emptyTime() : Subscription.getTime(timestamp * 1000);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getJsonValue(JSONObject object, String... keys) {
        if (keys.length == 1) {
            return (T) object.get(keys[0]);
        } else {
            return (T) getNestedJsonValue(object, keys);
        }
    }

    private static <T> T getNestedJsonValue(JSONObject object, String... keys) {
        JSONObject tmp = object;
        T val = null;
        for (int i = 0; i < keys.length; i++) {
            if (i < keys.length - 1) {
                tmp = getJsonValue(tmp, keys[i]);
                if (tmp == null) {
                    break;
                }
            } else {
                val = getJsonValue(tmp, keys[i]);
            }
        }
        return val;
    }

    private String toLowerCase(String str) {
        return str != null ? str.toLowerCase() : null;
    }

    private String toUpperCase(String str) {
        return str != null ? str.toUpperCase() : null;
    }
}
