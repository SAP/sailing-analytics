# Communication with Subscription Provider

## API Communication
The Application is able to communicate with the subscription provider through web API calls.

### SubscriptionApiService
The SubscriptionAPIService is responsible for any communication initiated by the application targeting the respective subscription provider of the underlying implementation. 
The implementations are build in such a way, that even if no immediate response is received, retries are made. Further they can handle multi-part responses and the like. 

Currently the API is used to 
1. Get Subscriptions. Hence retrieve all Subscriptions stored on the subscription provider, to be synced with the local data.
2. Cancel Subscription. Send a cancel request to the subscription provider.
3. Retrieve Self service session. Retrieves a self service session, with which the user can access his/her personal subscription provider page, to manage their subscriptions.
4. Fetch Item Prices. Fetches all subscription plan prices to be synced with the static prices given in the code.

### API key and Sitename - VM Arguments for Chargebee
We need to provide these system properties as VM arguments:

- chargebee.site: Site name

For test, it should be "syrf-test". And value should be "syrf" for a live / production site.

- chargebee.apikey: API Key

Go into API Key setting page at [https://app.chargebee.com/](https://app.chargebee.com/), from Dashboard -> Settings -> Configure Chargebee -> API Keys (API keys tab).
If there's no keys yet then press on "Add new key" to create a new one.
Copy the value of the key.

## Webhook
Through webhook interaction with the subscription provider, the application is able to react to any events that occur outside of the applications influence.
Such events are for example payment status updates or subscription cancellations. 
It is imperative to have this second communication line, as the subscription provider is responsible for "everything payment" related in this subscription model. Hence any updates that occur on their side must be reflected by the applications data asap.

### Webhook Configuration under Chargebee
Configure a webhook to enable Chargebee webhook events listening in application.

Go to Dashboard -> Settings -> Configure Chargebee -> API Keys (Webhooks tab).
Press on the "Add webhook" button and fill the popup form. Webhook URL is `{host}/sailingserver/subscription/hooks/chargebee`.
Turn on the "Protect webhook URL with basic authentication" option, fill username and password. We also need to create a user in the application with the same username and password so basic authentication would work correctly. The user for this account requires the permission `USER:ADD_SUBSCRIPTION` on any user the web hook shall be able to manage subscriptions for.

If all configurations are provided then Chargebee service will be active in the application, otherwise it will be inactive.

### ChargebeeWebHookHandler
Whenever a webhook event by chargebee.com arrives it is handled as defined in ChargebeeWebHookHandler.
Events handled are defined in SubscriptionWebHookEventType. See a current list as follows: 
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
    SUBSCRIPTION_RESUMED("subscription_resumed");
For a detailed description on how the body of such an event looks like and what it semmantically entails see https://apidocs.chargebee.com/docs/api/events?prod_cat_ver=2
Make sure to use the correct version of the API documentation. As of the time of writing this guide, version 2.0 was used.

## Chargebee HostedPage
In case of chargebee, in addition to server side communication, the GWT client application also communicates with the subscription provider using hosted pages.
These are prebuild popups, which can be configured on the chargebee site under https://sailytics-test.chargebee.com/checkout_and_portal_settings/configuration.
They portray the checkout process. The result of this checkout process is then relayed back to the application's master server to process. See com.sap.sse.security.ui.server.subscription.chargebee.ChargebeeSubscriptionWriteServiceImpl.finishCheckout(FinishCheckoutDTO) for details on how it is handled in code.
To use these, first com.sap.sse.security.ui.client.subscription.chargebee.Chargebee has to be initialized on the site. This usually happens in any given entry point where chargebee is used.

# Subscription Model

## Subscription Plan
Subscription plans are the basis of the subscription model. They are defined in com.sap.sse.security.shared.subscription.SubscriptionPlan for all sse relevant plans and in com.sap.sailing.domain.common.subscription.SailingSubscriptionPlan for all sailing related plans.
Below see the constructor of SailingSubscriptionPlan:
```
    private SailingSubscriptionPlan(String id, Set<SubscriptionPrice> prices, Set<PlanCategory> excludesPlanIds,
            Boolean isOneTimePlan, PlanGroup group, SubscriptionPlanRole... roles)
```

Each subscription plan also has to be mirrored chargebee.com. The ID given to a plan on chargebee.com has to match with the id given to the plan in the applications code.
Subscriptions are synced on server startup with chargebee by SubscriptionPlanUpdateTask.

Further, each plan is assigned a set of SubscriptionPrices, which indicate the price which the user must pay to subscribe to a plan for a certain interval. For technical reasons, the plans were split into seperate plans for each interval, so each subscription plan only has one SubscriptionPrice.
As an example, there is a weekly and a yearly premium plan.
The SubscriptionUpdateTask will override the prices given in the code, since the prices set on chargebee.com will be the ones that are actually required to be paid by the user.

To ensure a user is not able to purchase mutually exclusive plans, a set of PlanCategory is given. A plan is mutually exclusive when they have at least one of the same categories.

To portrait the concept of trials and one time plans a simple boolean flag is used. Hence if the user has had a subscription to this plan once, he can not aquire it again. This is checked against all subscriptions that the user has ever had.

The PlanGroup parameter is used to group SubscriptionPlans together for a more uniform display on the client side.

Finally, plans contain roles, which represent the permissions that users get when aquiring the plan with a subscription.

## Subscription
A Subscription represents a users aquisition of a subscription plan. 
A new Subscription can be acquired /gwt/Home.html#/subscription/:.
All subscriptions a user has ever had are stored within the User object.
Chargebee Subscriptions are synced by a scheduled SubscriptionUpdateTask that occurs in set intervals, the first starting around 2 minutes after server startup.
They can be managed by the user under user profile->subscriptions.

# Chargebee Subscription Manual Upgrade
Upgrading a subscription to a higher tier is currently not implemented. Hence upgrades have to be performed manually.

1. Go to Subscriptions -> Select relevant subscription of user -> cancel subscription (on the right) -> immediately
2. Go to Product Catalog -> Coupons -> Create Coupon -> create a coupon equivalent to amount to be refunded (One time use). This may be the full amount paid for the original subscription or less depending on circumstances.
3. Send Coupon code to user
