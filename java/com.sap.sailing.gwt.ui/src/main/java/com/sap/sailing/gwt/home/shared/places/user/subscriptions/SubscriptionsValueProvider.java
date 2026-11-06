package com.sap.sailing.gwt.home.shared.places.user.subscriptions;

import static com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil.renderLabelType;
import static com.sap.sse.security.ui.client.i18n.subscription.SubscriptionStringConstants.INSTANCE;

import java.util.function.BiConsumer;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.CurrencyData;
import com.google.gwt.i18n.client.CurrencyList;
import com.google.gwt.i18n.client.NumberFormat;
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

    /**
     * Formats the recurring payment amount according to the current locale's conventions for the subscription's
     * currency, e.g. "€54.78" in English or "54,78 €" in German. For a currency code unknown to GWT, the amount is
     * followed by the plain currency code, separated by a no-break space, rather than failing.
     */
    public String getRecurringPayment(final SubscriptionDTO subscription) {
        final double value = subscription.getReoccuringPaymentValue() / 100.0;
        final String currencyCode = subscription.getCurrencyCode();
        final CurrencyData currencyData = currencyCode == null ? null : CurrencyList.get().lookup(currencyCode);
        final String result;
        if (currencyData != null) {
            result = NumberFormat.getCurrencyFormat(currencyData).format(value);
        } else {
            result = NumberFormat.getFormat("#0.00").format(value) + (currencyCode == null ? "" : "\u00a0" + currencyCode);
        }
        return result;
    }

}
