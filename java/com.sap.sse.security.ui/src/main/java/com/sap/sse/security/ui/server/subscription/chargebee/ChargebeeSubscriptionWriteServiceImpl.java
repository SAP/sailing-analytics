package com.sap.sse.security.ui.server.subscription.chargebee;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.chargebee.Result;
import com.chargebee.models.HostedPage;
import com.chargebee.models.HostedPage.CheckoutNewForItemsRequest;
import com.chargebee.models.HostedPage.Content;
import com.chargebee.models.Invoice;
import com.chargebee.models.Subscription.SubscriptionItem;
import com.chargebee.models.Subscription.SubscriptionItem.ItemType;
import com.chargebee.models.Transaction;
import com.sap.sse.ServerStartupConstants;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;
import com.sap.sse.security.shared.subscription.chargebee.ChargebeeSubscription;
import com.sap.sse.security.subscription.SubscriptionApiService;
import com.sap.sse.security.subscription.SubscriptionCancelResult;
import com.sap.sse.security.subscription.SubscriptionNonRenewingResult;
import com.sap.sse.security.subscription.chargebee.ChargebeeConfiguration;
import com.sap.sse.security.ui.client.subscription.chargebee.ChargebeeSubscriptionWriteService;
import com.sap.sse.security.ui.shared.subscription.SubscriptionListDTO;
import com.sap.sse.security.ui.shared.subscription.chargebee.ChargebeeConfigurationDTO;
import com.sap.sse.security.ui.shared.subscription.chargebee.FinishCheckoutDTO;
import com.sap.sse.security.ui.shared.subscription.chargebee.PrepareCheckoutDTO;

public class ChargebeeSubscriptionWriteServiceImpl extends ChargebeeSubscriptionServiceImpl
        implements ChargebeeSubscriptionWriteService {

    private static final long serialVersionUID = 3058555834123504387L;
    private static final String SUBSCRIPTION_STATUS_ACTIVE = "active";
    private static final String SUBSCRIPTION_NON_RENEWING = "non_renewing";

    private static final Logger logger = Logger.getLogger(ChargebeeSubscriptionWriteServiceImpl.class.getName());

    @Override
    public SubscriptionListDTO finishCheckout(FinishCheckoutDTO data) {
        logger.info("finishCheckout hostedPageId: " + data.getHostedPageId());
        SubscriptionListDTO subscriptionDto;
        try {
            final User user = getCurrentUser();
            final SecurityService securityService = getSecurityService();
            securityService.lockSubscriptionsForUser(user);
            try {
                final Result result = HostedPage.acknowledge(data.getHostedPageId()).request();
                final Content content = result.hostedPage().content();
                final String customerId = content.customer().id();
                if (customerId != null && !customerId.equals(user.getName())) {
                    throw new UserManagementException("User does not match!");
                }
                final String transactionType;
                final String transactionStatus;
                final Transaction transaction = content.transaction();
                if (transaction != null) {
                    transactionType = transaction.type().name().toLowerCase();
                    transactionStatus = transaction.status().name().toLowerCase();
                } else {
                    transactionType = null;
                    transactionStatus = null;
                }
                final Invoice invoice = content.invoice();
                final String invoiceId;
                final String invoiceStatus;
                if (invoice != null) {
                    invoiceId = invoice.id();
                    invoiceStatus = invoice.status().name().toLowerCase();
                } else {
                    invoiceId = null;
                    invoiceStatus = null;
                }
                com.chargebee.models.Subscription contentSubscription = content.subscription();
                final Timestamp trialStart = contentSubscription.trialStart();
                final Timestamp trialEnd = contentSubscription.trialEnd();
                // TODO: bug5510 this ignores potential Addons or Charges contained in the Subscription
                SubscriptionPlan plan = null;
                for (SubscriptionItem item : contentSubscription.subscriptionItems()) {
                    if(item.itemType().equals(ItemType.PLAN)) {
                        final String itemPriceId = item.itemPriceId();
                        plan = getSecurityService().getSubscriptionPlanByItemPriceId(itemPriceId);
                        break;
                    }
                }
                if (plan == null) {
                    throw new IllegalArgumentException("No such Subscriptionplan");
                }
                final String paymentStatus = ChargebeeSubscription.determinePaymentStatus(transactionType, transactionStatus, invoiceStatus);
                final Subscription subscription = new ChargebeeSubscription(contentSubscription.id(), plan.getId(),
                        customerId, trialStart == null ? Subscription.emptyTime() : TimePoint.of(trialStart),
                        trialStart == null ? Subscription.emptyTime() : TimePoint.of(trialEnd),
                        contentSubscription.status().name().toLowerCase(), paymentStatus, transactionType, transactionStatus,
                        invoiceId, invoiceStatus, contentSubscription.mrr(), contentSubscription.currencyCode(), getTime(contentSubscription.createdAt()),
                        getTime(contentSubscription.updatedAt()), getTime(contentSubscription.activatedAt()),
                        getTime(contentSubscription.nextBillingAt()), getTime(contentSubscription.currentTermEnd()),
                        getTime(contentSubscription.cancelledAt()), Subscription.emptyTime(), Subscription.emptyTime());
                updateUserSubscription(user, subscription);
                cancelOldSubscriptionIfCoveredByNewOne(user.getSubscriptions(), subscription);
                subscriptionDto = getSubscriptions(true);
            } finally {
                securityService.unlockSubscriptionsForUser(user);
            }
        } catch(UserManagementException e) {
            logger.log(Level.FINE, "No user is logged in.");
            subscriptionDto = new SubscriptionListDTO(null, e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in saving subscription", e);
            subscriptionDto = new SubscriptionListDTO(null, e.getMessage());
        }
        return subscriptionDto;
    }

    private void cancelOldSubscriptionIfCoveredByNewOne(Iterable<Subscription> oldSubsciptions, Subscription newSubscription) {
        // check only active subscriptions (state active or non_renewing)
        if (isActive(newSubscription)) {
            SubscriptionPlan newSubscribedPlan = this.getSecurityService().getSubscriptionPlanById(newSubscription.getPlanId());
            for (Subscription oldSubscription : oldSubsciptions) {
                // ignore equal ones and old inactive subscriptions
                if (oldSubscription.getSubscriptionId() != null 
                        && !oldSubscription.getSubscriptionId().equals(newSubscription.getSubscriptionId())
                        && isActive(oldSubscription)) {
                    SubscriptionPlan oldSubscribedPlan = this.getSecurityService().getSubscriptionPlanById(oldSubscription.getPlanId());
                    // check if old subscription is covered by new one
                    if (Util.containsAll(newSubscribedPlan.getPlanCategories(), oldSubscribedPlan.getPlanCategories())) {
                        logger.info("An old subscription is totally covered by new one. We can cancel the old one: " + oldSubscribedPlan.toString());
                        cancelSubscription(oldSubscribedPlan.getId());
                    }
                }
            }
        }
    }

    private boolean isActive(Subscription subscription) {
        return SUBSCRIPTION_STATUS_ACTIVE.equals(subscription.getSubscriptionStatus()) || SUBSCRIPTION_NON_RENEWING.equals(subscription.getSubscriptionStatus());
    }
    
    private TimePoint getTime(Timestamp timeStamp) {
        return timeStamp == null ? com.sap.sse.security.shared.subscription.Subscription.emptyTime()
                : TimePoint.of(timeStamp);
    }
    
    @Override
    public ChargebeeConfigurationDTO getConfiguration() {
        final ChargebeeConfiguration configuration = ChargebeeConfiguration.getInstance();
        final ChargebeeConfigurationDTO result;
        if (configuration != null) {
            result = new ChargebeeConfigurationDTO(configuration.getSite());
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public PrepareCheckoutDTO prepareCheckout(final String priceId) {
        final PrepareCheckoutDTO response = new PrepareCheckoutDTO();
        try {
            final User user = getCurrentUser();
            if(!ServerStartupConstants.SUBSCRIPTIONS_DISABLE_EMAIL_VERIFICATION_REQUIRED && !user.isEmailValidated()) {
                throw new IllegalArgumentException("User mail must be validated.");
            }
            final SubscriptionPlan planForPrice = getSecurityService().getSubscriptionPlanByItemPriceId(priceId);
            if(planForPrice == null) {
                throw new IllegalArgumentException("No matching subscription plan found for given price id.");
            } else if(planForPrice.getIsOneTimePlan() && user.hasAnySubscription(planForPrice.getId())) {
                throw new IllegalArgumentException("Plan can only be subscribed once.");
            } else if(isNewPlanCompletelyIncludedInCurrentPlan(user, planForPrice)) {
                throw new IllegalArgumentException("User has already subscribed to plan which covers the new one.");
            } else {
                final Pair<String, String> usernames = getUserFirstAndLastName(user);
                final String locale = user.getLocaleOrDefault().getLanguage();
                final CheckoutNewForItemsRequest requestBuilder = HostedPage.checkoutNewForItems()
                        .subscriptionItemItemPriceId(0, priceId)
                        .subscriptionItemQuantity(0,1)
                        .customerId(user.getName()).customerEmail(user.getEmail())
                        .customerFirstName(usernames.getA()).customerLastName(usernames.getB());
                if (isChargebeeSupportedLocale(locale)) {
                    requestBuilder.customerLocale(locale);
                }
                final Result result = requestBuilder.request();
                response.setHostedPageJSONString(result.hostedPage().toJson());
            }
        } catch(final UserManagementException e) {
            logger.log(Level.FINE, "No user is logged in.");
            response.setError("You have to login as user to use this function.");
        } catch(final IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Error occured while preparing Chargebee checkout. " + e.getMessage());
            response.setError("Error occured while preparing Chargebee checkout. " + e.getMessage());
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Error in generating Chargebee hosted page data ", e);
            response.setError("Unexpected error occurred while generating Chargebee hosted page.");
        }
        return response;
    }

    /**
     * Checks whether {@code locale} is supported as defined by Chargbee here: https://www.chargebee.com/docs/supported-locales.html
     */
    private boolean isChargebeeSupportedLocale(String locale) {
        final Set<String> supportedLocales = new HashSet<>(Arrays.asList("en", "fr", "de", "it", "pt", "es"));
        return supportedLocales.contains(locale);
    }

    @Override
    public boolean cancelSubscription(String planId) {
        boolean result;
        try {
            User user = getCurrentUser();
            final SecurityService securityService = getSecurityService();
            securityService.lockSubscriptionsForUser(user);
            try {
                Subscription subscription = user.getSubscriptionByPlan(planId);
                if (isValidSubscription(subscription)) {
                    logger.info(() -> "Cancel user subscription, user " + user.getName() + ", plan " + planId);
                    SubscriptionApiService apiService = getApiService();
                    if (apiService != null) {
                        SubscriptionCancelResult cancelResult = requestCancelSubscription(apiService,
                                subscription.getSubscriptionId()).get();
                        if (cancelResult.isSuccess()) {
                            logger.info(() -> "Cancel subscription successful");
                            result = true;
                            if (cancelResult.getSubscription() != null) {
                                updateUserSubscription(user, cancelResult.getSubscription());
                            }
                        } else {
                            result = false;
                            if (cancelResult.isDeleted()) {
                                logger.info(() -> "Subscription for plan was deleted");
                                Subscription emptySubscription = ChargebeeSubscription.createEmptySubscription(planId,
                                        subscription.getLatestEventTime(), TimePoint.now());
                                updateUserSubscription(user, emptySubscription);
                            } else {
                                logger.info(() -> "Cancel subscription failed");
                            }
                        }
                    } else {
                        logger.info(() -> "No active api service found");
                        result = false;
                    }
                } else {
                    logger.info(() -> "Invalid subscription");
                    result = false;
                }
            } finally {
                securityService.unlockSubscriptionsForUser(user);
            }
        } catch(UserManagementException e) {
            logger.log(Level.FINE, "No user is logged in.");
            result = false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in cancel subscription ", e);
            result = false;
        }
        return result;
    }

    @Override
    public boolean nonRenewingSubscription(String planId) {
        boolean result;
        try {
            User user = getCurrentUser();
            final SecurityService securityService = getSecurityService();
            securityService.lockSubscriptionsForUser(user);
            try {
                final Subscription subscription = user.getSubscriptionByPlan(planId);
                if (isValidSubscription(subscription)) {
                    logger.info(() -> "Set user subscription to non renewing, user " + user.getName() + ", plan " + planId);
                    SubscriptionApiService apiService = getApiService();
                    if (apiService != null) {
                        SubscriptionNonRenewingResult nonRenewingResult = requestNonRenewingSubscription(apiService,
                                subscription.getSubscriptionId()).get();
                        if (nonRenewingResult.isSuccess()) {
                            logger.info(() -> "Setting subscription to non renewing successful");
                            result = true;
                            if (nonRenewingResult.getSubscription() != null) {
                                updateUserSubscription(user, nonRenewingResult.getSubscription());
                            }
                        } else {
                            result = false;
                            if (nonRenewingResult.isDeleted()) {
                                logger.info(() -> "Subscription for plan was deleted");
                                final Subscription emptySubscription = ChargebeeSubscription.createEmptySubscription(planId,
                                        subscription.getLatestEventTime(), TimePoint.now());
                                updateUserSubscription(user, emptySubscription);
                            } else {
                                logger.info(() -> "Setting subscription to non renewing failed");
                            }
                        }
                    } else {
                        logger.info(() -> "No active api service found");
                        result = false;
                    }
                } else {
                    logger.info(() -> "Invalid subscription");
                    result = false;
                }
            } finally {
                securityService.unlockSubscriptionsForUser(user);
            }
        } catch(UserManagementException e) {
            logger.log(Level.FINE, "No user is logged in.");
            result = false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in setting subscription to non renewing ", e);
            result = false;
        }
        return result;
    }

    private Future<SubscriptionNonRenewingResult> requestNonRenewingSubscription(SubscriptionApiService apiService,
            String subscriptionId) {
        CompletableFuture<SubscriptionNonRenewingResult> result = new CompletableFuture<SubscriptionNonRenewingResult>();
        apiService.nonRenewingSubscription(subscriptionId, new SubscriptionApiService.OnNonRenewingSubscriptionResultListener() {
            @Override
            public void onNonRenewingResult(SubscriptionNonRenewingResult nonRenewingResult) {
                result.complete(nonRenewingResult);
            }
        });
        return result;
    }

    private Future<SubscriptionCancelResult> requestCancelSubscription(SubscriptionApiService apiService,
            String subscriptionId) {
        CompletableFuture<SubscriptionCancelResult> result = new CompletableFuture<SubscriptionCancelResult>();
        apiService.cancelSubscription(subscriptionId, new SubscriptionApiService.OnCancelSubscriptionResultListener() {
            @Override
            public void onCancelResult(SubscriptionCancelResult cancelResult) {
                result.complete(cancelResult);
            }
        });
        return result;
    }
}
