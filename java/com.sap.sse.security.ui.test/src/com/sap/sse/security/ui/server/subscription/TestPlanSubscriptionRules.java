package com.sap.sse.security.ui.server.subscription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.sap.sse.security.ui.shared.subscription.SubscriptionPlanDTO;

public class TestPlanSubscriptionRules {
    private final SubscriptionPlan dataMiningArchiveYearly = SailingSubscriptionPlan.DATA_MINING_ARCHIVE_YEARLY;
    private final SubscriptionPlan premiumYearly = SailingSubscriptionPlan.PREMIUM_YEARLY;
    private User user;
    private Subscription premiumYearlySubscription;
    private Subscription dataMiningArchiveYearlySubscription;
    private SecurityService securityService;
    private ChargebeeSubscriptionWriteServiceImpl service;

    @BeforeEach
    public void setUp() {
        user = mock(User.class);
        premiumYearlySubscription = new ChargebeeSubscription("subscriptionId1", premiumYearly.getId(), "customerId",
                TimePoint.now(), TimePoint.now(), /* subscriptionStatus */ "active", /* paymentStatus */ "paid", "transactionType", "transactionStatus", "invoiceId",
                "invoiceStatus", /* reoccuringPaymentValue */ 49, /* currencyCode */ "USD", TimePoint.now(), TimePoint.now(),
                TimePoint.now(), TimePoint.now(), TimePoint.now(), /* cancelledAt */ null, TimePoint.now(), /* manualUpdatedAt */ null);
        dataMiningArchiveYearlySubscription = new ChargebeeSubscription("subscriptionId2", dataMiningArchiveYearly.getId(), "customerId",
                TimePoint.now(), TimePoint.now(), /* subscriptionStatus */ "active", /* paymentStatus */ "paid", "transactionType", "transactionStatus", "invoiceId",
                "invoiceStatus", /* reoccuringPaymentValue */ 49, /* currencyCode */ "USD", TimePoint.now(), TimePoint.now(),
                TimePoint.now(), TimePoint.now(), TimePoint.now(), /* cancelledAt */ null, TimePoint.now(), /* manualUpdatedAt */ null);
        securityService = mock(SecurityService.class);
        when(securityService.getAllSubscriptionPlans()).thenReturn(SailingSubscriptionPlan.getAllInstances());
        service = new ChargebeeSubscriptionWriteServiceImpl() {
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
    public void testThatPemiumDoesNotImplyDataMiningByCategory() {
        when(user.getSubscriptionByPlan(premiumYearly.getId())).thenReturn(premiumYearlySubscription);
        final SubscriptionPlanDTO premiumYearlyDTO = service.convertToDto(premiumYearly);
        final SubscriptionPlanDTO dataMiningArchiveYearlyDTO = service.convertToDto(dataMiningArchiveYearly);
        assertTrue(premiumYearlyDTO.isUserSubscribedToPlan());
        assertTrue(premiumYearlyDTO.isUserSubscribedToAllPlanCategories());
        assertFalse(dataMiningArchiveYearlyDTO.isUserSubscribedToPlan());
        assertFalse(dataMiningArchiveYearlyDTO.isUserSubscribedToAllPlanCategories());
    }
    
    @Test
    public void testThatDataMiningImpliesPemiumByCategory() {
        when(user.getSubscriptionByPlan(dataMiningArchiveYearly.getId())).thenReturn(dataMiningArchiveYearlySubscription);
        final SubscriptionPlanDTO premiumYearlyDTO = service.convertToDto(premiumYearly);
        final SubscriptionPlanDTO dataMiningArchiveYearlyDTO = service.convertToDto(dataMiningArchiveYearly);
        assertFalse(premiumYearlyDTO.isUserSubscribedToPlan());
        assertTrue(premiumYearlyDTO.isUserSubscribedToAllPlanCategories());
        assertTrue(dataMiningArchiveYearlyDTO.isUserSubscribedToPlan());
        assertTrue(dataMiningArchiveYearlyDTO.isUserSubscribedToAllPlanCategories());
    }
}
