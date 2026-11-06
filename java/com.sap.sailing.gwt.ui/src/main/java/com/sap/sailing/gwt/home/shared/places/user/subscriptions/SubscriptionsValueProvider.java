package com.sap.sailing.gwt.home.shared.places.user.subscriptions;

import static com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil.renderLabelType;
import static com.sap.sse.security.ui.client.i18n.subscription.SubscriptionStringConstants.INSTANCE;

import java.util.function.BiConsumer;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.communication.event.LabelType;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.ui.shared.subscription.SubscriptionDTO;

/**
 * Utility class providing convenient values to be used by UI components showing information about user
 * {@link SubscriptionDTO subscriptions}.
 */
public class SubscriptionsValueProvider {

    private final StringMessages i18n;

    public SubscriptionsValueProvider(final StringMessages i18n) {
        this.i18n = i18n;
    }

    public String getSubscriptionName(final SubscriptionDTO subscription) {
        return subscription == null ? "---" : INSTANCE.getString(subscription.getSubscriptionPlanNameMessageKey());
    }

    public LabelType getSubscriptionStatusLabelType(final SubscriptionDTO subscription) {
        if (subscription.isCancelled()) return LabelType.CANCELLED;
        if (subscription.isInTrial()) return LabelType.IN_TRIAL;
        if (subscription.isActive()) return LabelType.ACTIVE;
        return LabelType.UNKNOWN;
    }

    public SafeHtml getSubscriptionStatusLabel(final SubscriptionDTO subscription) {
        final Element labelElement = Document.get().createDivElement();
        labelElement.addClassName(SharedResources.INSTANCE.mainCss().label());
        renderLabelType(labelElement, getSubscriptionStatusLabelType(subscription));
        return () -> labelElement.getString();
    }

    public void configurePaymentStatusElement(final SubscriptionDTO subscription,
            final BiConsumer<String, String> textValueHandler,
            final BiConsumer<String, DataResource> imageValueHandler) {
        if (subscription.isActive()) {
            if (subscription.isPaymentSuccess()) {
                if (subscription.isRefunded()) {
                    textValueHandler.accept(i18n.paymentStatus(), i18n.refunded());
                } else {
                    imageValueHandler.accept(i18n.paymentStatus(), SharedHomeResources.INSTANCE.greenCheck());
                }
            } else if (subscription.isPaymentNoSuccess()) {
                imageValueHandler.accept(i18n.paymentStatus(), SharedHomeResources.INSTANCE.redDash());
            }
        } else {
            textValueHandler.accept(i18n.paymentStatus(), "---");
        }
    }

    public TimePoint getTermEnd(final SubscriptionDTO subscription) {
        return subscription.isInTrial() ? subscription.getTrialEnd() : subscription.getCurrentTermEnd();
    }

    public String getRecurringPayment(final SubscriptionDTO subscription) {
        return i18n.currencyValue(subscription.getReoccuringPaymentValue() / 100.0, subscription.getCurrencyCode());
    }

}
