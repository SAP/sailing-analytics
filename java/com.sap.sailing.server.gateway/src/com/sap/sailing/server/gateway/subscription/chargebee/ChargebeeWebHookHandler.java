package com.sap.sailing.server.gateway.subscription.chargebee;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.sap.sailing.server.gateway.subscription.SubscriptionWebHookHandler;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;
import com.sap.sse.security.shared.subscription.chargebee.ChargebeeSubscription;

/**
 * Servlet for handling WebHook events, response with status 200 in success handling. The servlet has to be secured by
 * {@code ChargebeeSubscriptionPermissionsAuthorizationFilter}
 * 
 * {@link https://www.chargebee.com/docs/events_and_webhooks.html}
 * 
 * @author Tu Tran
 */
public class ChargebeeWebHookHandler extends SubscriptionWebHookHandler {
    private static final Logger logger = Logger.getLogger(ChargebeeWebHookHandler.class.getName());
    private static final String HANDLER_PATH = "chargebee";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        SubscriptionWebHookEvent event = null;
        try {
            event = (SubscriptionWebHookEvent) request.getAttribute("event");
            logger.log(Level.INFO, "Handling Webhook Event of type:" + event.getEventType());
            final User user = getUser(event.getCustomerId());
            if (user != null) {
                if (!isOutdatedEvent(event, user)) {
                    processEvent(event, user);
                }
                sendSuccess(response);
            } else {
                logger.warning("User "+event.getCustomerId()+" not found. Ignoring Chargebee webhook callback for that user.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to proccess Chargebee subscription webhook event "
                    + (event != null ? event.getEventType().getName() : ""), e);
            sendFail(response);
        }
    }

    @Override
    public String getHandlerPath() {
        return HANDLER_PATH;
    }

    /**
     * @return {@code true} if event occurred at time is less than latest user subscription updated time
     */
    private boolean isOutdatedEvent(SubscriptionWebHookEvent event, User user) {
        final TimePoint occuredAt = event.getEventOccurredAt();
        final String subscriptionId = event.getSubscriptionId();
        boolean isOutdated = false;
        if (StringUtils.isNotEmpty(subscriptionId)) {
            Subscription subscription = user.getSubscriptionById(subscriptionId);
            if (subscription != null) {
                isOutdated = isOutdatedEventTime(occuredAt, subscription);
            } else {
                Iterable<Subscription> subscriptions = user.getSubscriptions();
                if (subscriptions != null) {
                    for (Subscription sub : subscriptions) {
                        if (!sub.hasPlan() && isOutdatedEventTime(occuredAt, sub)) {
                            isOutdated = true;
                            break;
                        }
                    }
                }
            }
        } else {
            Iterable<Subscription> subscriptions = user.getSubscriptions();
            if (subscriptions != null) {
                for (Subscription subscription : subscriptions) {
                    if (isOutdatedEventTime(occuredAt, subscription)) {
                        isOutdated = true;
                        break;
                    }
                }
            }
        }
        if (isOutdated) {
            logger.info(() -> "Webhook event " + event.getEventType() + " is outdated and won't be processed");
        }
        return isOutdated;
    }

    private boolean isOutdatedEventTime(TimePoint occuredAt, Subscription subscription) {
        return occuredAt.before(subscription.getLatestEventTime())
                || occuredAt.before(subscription.getManualUpdatedAt());
    }

    private void processEvent(final SubscriptionWebHookEvent event, final User user) throws UserManagementException {
        final SubscriptionWebHookEventType eventType = event.getEventType();
        if (eventType != null) {
            logger.info(() -> "Start process webhook event \"" + eventType.getName() + "\" for user " + user.getName());
            final SecurityService securityService = context.getSecurityService();
            securityService.lockSubscriptionsForUser(user);
            try {
                final Subscription userSubscription = getCurrentUserSubscriptionFromEvent(user, event);
                switch (eventType) {
                case CUSTOMER_DELETED:
                    updateUserSubscription(user, buildEmptySubscription(userSubscription, event));
                    break;
                case SUBSCRIPTION_DELETED:
                    if (userSubscription != null && userSubscription.getSubscriptionId() != null
                            && userSubscription.getSubscriptionId().equals(event.getSubscriptionId())) {
                        updateUserSubscription(user, buildEmptySubscription(userSubscription, event));
                    }
                    break;
                case SUBSCRIPTION_CREATED:
                case SUBSCRIPTION_CHANGED:
                case SUBSCRIPTION_ACTIVATED:
                case PAYMENT_SUCCEEDED:
                case PAYMENT_FAILED:
                case SUBSCRIPTION_PAUSED:
                case SUBSCRIPTION_RESUMED:
                case SUBSCRIPTION_CANCELLED:
                    updateUserSubscription(user, buildSubscription(userSubscription, event));
                    break;
                case PAYMENT_REFUNDED:
                    if (userSubscription.getInvoiceId() != null
                            && userSubscription.getInvoiceId().equals(event.getInvoiceId())) {
                        updateUserSubscription(user, buildSubscription(userSubscription, event));
                    }
                    break;
                case INVOICE_GENERATED:
                    updateSubscriptionInvoice(user, userSubscription, event);
                    break;
                case INVOICE_UPDATED:
                    if (userSubscription != null && userSubscription.getInvoiceId() != null
                            && userSubscription.getInvoiceId().equals(event.getInvoiceId())) {
                        updateSubscriptionInvoice(user, userSubscription, event);
                    }
                    break;
                default:
                    logger.warning(
                            () -> "Webhook event type was unknown and will not be processed ");
                    break;
                }
            } finally {
                securityService.unlockSubscriptionsForUser(user);
            }
            logger.info(() -> "Webhook event \"" + eventType.getName() + "\" has been processed for user "
                    + user.getName());
        }
    }

    private Subscription getCurrentUserSubscriptionFromEvent(User user, SubscriptionWebHookEvent event) {
        final Subscription userSubscription;
        if (StringUtils.isNotEmpty(event.getPlanId())) {
            userSubscription = user.getSubscriptionByPlan(event.getPlanId());
        } else {
            userSubscription = user.getSubscriptionById(event.getSubscriptionId());
        }
        return userSubscription;
    }
    
    private String getPlanIdFromEvent(SubscriptionWebHookEvent event) {
        String planId = event.getPlanId();
        if(planId == null) {
            final String itemPriceId = event.getItemPriceId();
            final SubscriptionPlan plan = context.getSecurityService().getSubscriptionPlanByItemPriceId(itemPriceId);
            if(plan != null) {
                planId = plan.getId();
            }
        }
        return planId;
    }

    private Subscription buildEmptySubscription(Subscription currentSubscription, SubscriptionWebHookEvent event) {
        return ChargebeeSubscription.createEmptySubscription(event.getPlanId(), event.getEventOccurredAt(),
                currentSubscription != null ? currentSubscription.getManualUpdatedAt() : Subscription.emptyTime());
    }
    
    /**
     * Build new {@code Subscription} instance from current user subscription and webhook event
     * {@code SubscriptionWebHookEvent}
     */
    private Subscription buildSubscription(Subscription currentSubscription, SubscriptionWebHookEvent event) {
        String paymentStatus = null;
        String subscriptionStatus = event.getSubscriptionStatus();
        String transactionType = null;
        String transactionStatus = null;
        String invoiceId = null;
        String invoiceStatus = null;
        if (subscriptionStatus != null) {
            transactionType = event.getTransactionType();
            if (transactionType == null && currentSubscription != null) {
                transactionType = currentSubscription.getTransactionType();
            }
            transactionStatus = event.getTransactionStatus();
            if (transactionStatus == null && currentSubscription != null) {
                transactionStatus = currentSubscription.getTransactionStatus();
            }
            Pair<String, String> invoice = getInvoiceData(currentSubscription, event);
            invoiceId = invoice.getA();
            invoiceStatus = invoice.getB();
            paymentStatus = ChargebeeSubscription.determinePaymentStatus(transactionType, transactionStatus,
                    invoiceStatus);
            if (paymentStatus == null && currentSubscription != null) {
                paymentStatus = currentSubscription.getPaymentStatus();
            }
        }
        return new ChargebeeSubscription(event.getSubscriptionId(), getPlanIdFromEvent(event), event.getCustomerId(),
                event.getSubscriptionTrialStart(), event.getSubscriptionTrialEnd(), subscriptionStatus, paymentStatus,
                transactionType, transactionStatus, invoiceId, invoiceStatus, event.getReocurringPaymentValue(),
                event.getCurrencyCode(), event.getSubscriptionCreatedAt(), event.getSubscriptionUpdatedAt(),
                event.getActivatedAt(), event.getBillingAt(), event.getCurrentTermEnd(), event.getCancelledAt(),
                event.getEventOccurredAt(),
                currentSubscription != null ? currentSubscription.getManualUpdatedAt() : Subscription.emptyTime());
    }

    /**
     * Update user subscription from invoice webhook events {@code SubscriptionWebHookEventType#INVOICE_GENERATED}
     * {@code SubscriptionWebHookEventType#INVOICE_UPDATED}
     */
    private void updateSubscriptionInvoice(User user, Subscription currentSubscription, SubscriptionWebHookEvent event)
            throws UserManagementException {
        if (currentSubscription != null && StringUtils.isNotEmpty(currentSubscription.getSubscriptionId())
                && currentSubscription.getSubscriptionId().equals(event.getInvoiceSubscriptionId())
                && StringUtils.isNotEmpty(currentSubscription.getCustomerId())
                && currentSubscription.getCustomerId().equals(event.getInvoiceCustomerId())) {
            Pair<String, String> invoice = getInvoiceData(null, event);
            String invoiceId = invoice.getA();
            String invoiceStatus = invoice.getB();
            String paymentStatus = ChargebeeSubscription.determinePaymentStatusFromInvoiceStatus(invoiceStatus);
            Subscription newSubscription = new ChargebeeSubscription(currentSubscription.getSubscriptionId(),
                    currentSubscription.getPlanId(), currentSubscription.getCustomerId(),
                    currentSubscription.getTrialStart(), currentSubscription.getTrialEnd(),
                    currentSubscription.getSubscriptionStatus(), paymentStatus,
                    currentSubscription.getTransactionType(), currentSubscription.getTransactionStatus(), invoiceId,
                    invoiceStatus, currentSubscription.getReoccuringPaymentValue(),
                    currentSubscription.getCurrencyCode(), currentSubscription.getSubscriptionCreatedAt(),
                    currentSubscription.getSubscriptionUpdatedAt(), currentSubscription.getSubscriptionActivatedAt(),
                    currentSubscription.getNextBillingAt(), currentSubscription.getCurrentTermEnd(),
                    currentSubscription.getCancelledAt(), event.getEventOccurredAt(),
                    currentSubscription.getManualUpdatedAt());
            updateUserSubscription(user, newSubscription);
        }
    }

    /**
     * Get subscription invoice id and invoice status. If the data do not exist in webhook event then they will get
     * value from user current subscription if it's not null
     * 
     * @return subscription invoice id and invoice status
     */
    private Pair<String, String> getInvoiceData(Subscription currentSubscription, SubscriptionWebHookEvent event) {
        String invoiceId = event.getInvoiceId();
        if (invoiceId == null && currentSubscription != null) {
            invoiceId = currentSubscription.getInvoiceId();
        }
        String invoiceStatus = event.getInvoiceStatus();
        if (invoiceStatus == null && currentSubscription != null) {
            invoiceStatus = currentSubscription.getInvoiceStatus();
        }
        return new Pair<String, String>(invoiceId, invoiceStatus);
    }
}
