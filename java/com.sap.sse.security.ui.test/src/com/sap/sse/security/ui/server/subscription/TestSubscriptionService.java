package com.sap.sse.security.ui.server.subscription;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.subscription.SailingSubscriptionPlan;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;
import com.sap.sse.security.shared.subscription.chargebee.ChargebeeSubscription;
import com.sap.sse.security.ui.server.subscription.chargebee.ChargebeeSubscriptionWriteServiceImpl;

public class TestSubscriptionService {

    private final SubscriptionPlan dataMiningAllYearly = SailingSubscriptionPlan.DATA_MINING_ALL_YEARLY;
    private final SubscriptionPlan dataMiningArchiveYearly = SailingSubscriptionPlan.DATA_MINING_ARCHIVE_YEARLY;
    private final SubscriptionPlan premiumYearly = SailingSubscriptionPlan.PREMIUM_YEARLY;
    private User user;
    private SecurityService securityService;
    private Subscription premiumYearlySubscription;
    private Subscription dataMiningArchiveYearlySubscription;
    private Subscription dataMiningAllYearlySubscription;
    private SubscriptionServiceImpl serviceImpl;

    @BeforeEach
    public void setUp() {
        user = mock(User.class);
        premiumYearlySubscription = new ChargebeeSubscription("subscriptionId1", premiumYearly.getId(), "customerId",
                TimePoint.now(), TimePoint.now(), /* subscriptionStatus */ "active", /* paymentStatus */ "paid",
                "transactionType", "transactionStatus", "invoiceId", "invoiceStatus", /* reoccuringPaymentValue */ 49,
                /* currencyCode */ "USD", TimePoint.now(), TimePoint.now(), TimePoint.now(), TimePoint.now(),
                TimePoint.now(), /* cancelledAt */ null, TimePoint.now(), /* manualUpdatedAt */ null);
        dataMiningArchiveYearlySubscription = new ChargebeeSubscription("subscriptionId2",
                dataMiningArchiveYearly.getId(), "customerId", TimePoint.now(), TimePoint.now(),
                /* subscriptionStatus */ "active", /* paymentStatus */ "paid", "transactionType", "transactionStatus",
                "invoiceId", "invoiceStatus", /* reoccuringPaymentValue */ 49, /* currencyCode */ "USD",
                TimePoint.now(), TimePoint.now(), TimePoint.now(), TimePoint.now(), TimePoint.now(),
                /* cancelledAt */ null, TimePoint.now(), /* manualUpdatedAt */ null);
        dataMiningAllYearlySubscription = new ChargebeeSubscription("subscriptionId2",
                dataMiningAllYearly.getId(), "customerId", TimePoint.now(), TimePoint.now(),
                /* subscriptionStatus */ "active", /* paymentStatus */ "paid", "transactionType", "transactionStatus",
                "invoiceId", "invoiceStatus", /* reoccuringPaymentValue */ 49, /* currencyCode */ "USD",
                TimePoint.now(), TimePoint.now(), TimePoint.now(), TimePoint.now(), TimePoint.now(),
                /* cancelledAt */ null, TimePoint.now(), /* manualUpdatedAt */ null);
        securityService = mock(SecurityService.class);
        when(securityService.getAllSubscriptionPlans()).thenReturn(SailingSubscriptionPlan.getAllInstances());
        when(securityService.getSubscriptionPlanById("yearly_data_mining_all")).thenReturn(dataMiningAllYearly);
        when(securityService.getSubscriptionPlanById("yearly_data_mining_archive")).thenReturn(dataMiningArchiveYearly);
        when(securityService.getSubscriptionPlanById("yearly_premium")).thenReturn(premiumYearly);
        serviceImpl = new ChargebeeSubscriptionWriteServiceImpl() {
            private static final long serialVersionUID = 1L;

            @Override
            protected User getCurrentUser() {
                return user;
            }

            @Override
            protected SecurityService getSecurityService() {
                return securityService;
            }
        };
    }

    @Test
    public void testUpgradeFromPremium() {
        when(user.getSubscriptions())
                .thenReturn(Stream.of(new Subscription[] { premiumYearlySubscription }).collect(Collectors.toList()));
        boolean result = serviceImpl.isNewPlanCompletelyIncludedInCurrentPlan(user, dataMiningArchiveYearly);
        Assertions.assertFalse(result);
    }

    @Test
    public void testDowngradeToPremium() {
        when(user.getSubscriptions()).thenReturn(
                Stream.of(new Subscription[] { dataMiningArchiveYearlySubscription }).collect(Collectors.toList()));
        boolean result = serviceImpl.isNewPlanCompletelyIncludedInCurrentPlan(user, premiumYearly);
        Assertions.assertTrue(result);
    }

    @Test
    public void testUpgradeToDataMiningAll() {
        when(user.getSubscriptions()).thenReturn(
                Stream.of(new Subscription[] { dataMiningArchiveYearlySubscription }).collect(Collectors.toList()));
        boolean result = serviceImpl.isNewPlanCompletelyIncludedInCurrentPlan(user, dataMiningAllYearly);
        Assertions.assertFalse(result);
    }

    @Test
    public void testDowngradeFromDataMiningAll() {
        when(user.getSubscriptions()).thenReturn(
                Stream.of(new Subscription[] { dataMiningAllYearlySubscription }).collect(Collectors.toList()));
        boolean result = serviceImpl.isNewPlanCompletelyIncludedInCurrentPlan(user, premiumYearly);
        Assertions.assertTrue(result);
    }

}
