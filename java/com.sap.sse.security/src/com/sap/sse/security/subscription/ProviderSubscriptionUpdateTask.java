package com.sap.sse.security.subscription;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.TimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;

/**
 * Perform fetching and updating user subscriptions of a provider service
 */
public class ProviderSubscriptionUpdateTask implements SubscriptionApiService.OnSubscriptionsResultListener {
    private static final Logger logger = Logger.getLogger(ProviderSubscriptionUpdateTask.class.getName());

    private final SubscriptionApiService apiService;
    private final Iterable<User> users;
    private final CompletableFuture<SecurityService> securityService;

    public ProviderSubscriptionUpdateTask(SubscriptionApiService apiService, Iterable<User> users,
            CompletableFuture<SecurityService> securityService) {
        this.apiService = apiService;
        this.users = users;
        this.securityService = securityService;
    }

    public void run() {
        try {
            apiService.getUserSubscriptions(this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fetch subscriptions failed, provider: " + apiService.getProviderName(), e);
        }
    }

    @Override
    public void onSubscriptionsResult(Map<String, List<Subscription>> subscriptions) {
        if (subscriptions != null) {
            for (User user : users) {
                try {
                    checkAndUpdateSubscriptions(user, subscriptions.get(user.getName()), apiService);
                } catch (UserManagementException e) {
                    logger.log(Level.SEVERE, "Update user subscriptions failed, provider: "
                            + apiService.getProviderName() + ", user: " + user.getName(), e);
                }
            }
        } else {
            logger.log(Level.SEVERE, "Update user subscriptions failed, provider: " + apiService.getProviderName());
        }
    }

    private void checkAndUpdateSubscriptions(User user, Iterable<Subscription> userSubscriptions,
            SubscriptionApiService provider) throws UserManagementException {
        logger.info(() -> "Subscriptions from provider " + provider.getProviderName() + " for user " + user.getName()
                + ": " + (userSubscriptions == null ? "empty" : userSubscriptions));
        getSecurityService().lockSubscriptionsForUser(user);
        try {
            Iterable<Subscription> currentSubscriptions = user.getSubscriptions();
            if ((userSubscriptions == null || !userSubscriptions.iterator().hasNext())
                    && (currentSubscriptions != null && currentSubscriptions.iterator().hasNext())) {
                // No subscriptions so we need to remove all current subscriptions of user for the provider
                Subscription emptySubscription = createEmptySubscription(provider, null);
                getSecurityService().updateUserSubscription(user.getName(), emptySubscription);
            } else if (userSubscriptions != null) {
                if (currentSubscriptions != null) {
                    Map<String, Boolean> existingPlans = getExistingPlans(userSubscriptions);
                    for (Subscription subscription : currentSubscriptions) {
                        if (subscription.hasPlan() && !existingPlans.containsKey(subscription.getPlanId())) {
                            // Current subscription plan doesn't exist in subscription list from provider, that means
                            // subscription for the plan has been deleted, then we need to remove the subscription from
                            // database
                            Subscription emptySubscription = createEmptySubscription(provider, subscription.getPlanId());
                            getSecurityService().updateUserSubscription(user.getName(), emptySubscription);
                        }
                    }
                }
                // Only one subscription per plan has to be processed.
                final Set<Subscription> skimmedSubscriptions = new HashSet<>();
                for (SubscriptionPlan subscriptionPlan : getSecurityService().getAllSubscriptionPlans().values()) {
                    Subscription planSubscription = null;
                    for(Subscription userSubscription : userSubscriptions) {
                        final boolean isSamePlan = subscriptionPlan.getId().equals(userSubscription.getPlanId());
                        if (isSamePlan && planSubscription == null || 
                                isSamePlan && userSubscription.isUpdatedMoreRecently(planSubscription)) {
                            planSubscription = userSubscription;
                        }
                    }
                    if (planSubscription != null) {
                        skimmedSubscriptions.add(planSubscription);
                    }
                }
                for (Subscription subscription : skimmedSubscriptions) {
                    getSecurityService().updateUserSubscription(user.getName(), subscription);
                }
            }
        } finally {
            getSecurityService().unlockSubscriptionsForUser(user);
        }
    }
    
    private Subscription createEmptySubscription(SubscriptionApiService provider, String planId) {
        return provider.getDataHandler().toSubscription(
                SubscriptionData.createEmptySubscriptionDataWithUpdateTimes(planId, TimePoint.now(), TimePoint.now()));
    }

    private Map<String, Boolean> getExistingPlans(Iterable<Subscription> subscriptions) {
        final Map<String, Boolean> existingPlans = new HashMap<String, Boolean>();
        for (Subscription subscription : subscriptions) {
            if (subscription.hasPlan()) {
                existingPlans.put(subscription.getPlanId(), true);
            }
        }
        return existingPlans;
    }

    private SecurityService getSecurityService() {
        try {
            return securityService.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.log(Level.SEVERE, "Failure to get SecurityService", e);
            throw new RuntimeException(e);
        }
    }
}
