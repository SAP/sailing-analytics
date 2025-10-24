package com.sap.sailing.gwt.home.mobile.places.user.profile.subscriptions;

import static com.sap.sse.gwt.client.DateAndTimeFormatterUtil.formatDateAndTime;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.mobile.partials.section.MobileSection;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent;
import com.sap.sailing.gwt.home.shared.places.user.profile.subscriptions.UserSubscriptionsView;
import com.sap.sailing.gwt.home.shared.places.user.subscriptions.SubscriptionsValueProvider;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.ui.shared.subscription.SubscriptionDTO;

class UserSubscriptionItem extends Composite {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<MobileSection, UserSubscriptionItem> {
    }

    @UiField SharedResources res;
    @UiField StringMessages i18n;

    @UiField SectionHeaderContent sectionHeaderUi;
    @UiField HTMLPanel contentContainerUi;
    @UiField Element subscriptionInfoUi;
    @UiField Button cancelControlUi;

    private final SubscriptionsValueProvider valueProvider;
    private final Runnable nonRenewingCallback;

    UserSubscriptionItem(final SubscriptionDTO subscription, final UserSubscriptionsView.Presenter presenter) {
        final MobileSection mobileSection = uiBinder.createAndBindUi(this);
        mobileSection.setEdgeToEdgeContent(true);
        initWidget(mobileSection);
        this.valueProvider = new SubscriptionsValueProvider(i18n);
        if (subscription.isCancelled() || !subscription.isRenewing()) {
            this.nonRenewingCallback = () -> {};
            cancelControlUi.setEnabled(false);
        } else {
            this.nonRenewingCallback = () -> presenter.nonRenewingSubscription(subscription.getSubscriptionPlanId(),
                    subscription.getProvider());
        }
        sectionHeaderUi.setSectionTitle(valueProvider.getSubscriptionName(subscription));
        sectionHeaderUi.setLabelType(valueProvider.getSubscriptionStatusLabelType(subscription));
        addInfo(i18n.createdAt(), subscription.getCreatedAt());
        addInfo(i18n.currentTermEnd(), valueProvider.getTermEnd(subscription));
        valueProvider.configurePaymentStatusElement(subscription, this::addInfo, this::addInfo);
        if (!subscription.isInTrial() && (subscription.isCancelled() || !subscription.isRenewing())) {
            addInfo(i18n.cancelledAt(), subscription.getCancelledAt());
        } else if (subscription.isRenewing()) {
            addInfo(i18n.nextBillingAt(), subscription.getNextBillingAt());
            addInfo("", valueProvider.getRecurringPayment(subscription));
        }
        sectionHeaderUi.initCollapsibility(contentContainerUi.getElement(), false);
    }

    @UiHandler("cancelControlUi")
    void onCancelControlClicked(final ClickEvent event) {
        this.nonRenewingCallback.run();
    }

    private void addInfo(final String label, final String value) {
        subscriptionInfoUi.appendChild(new UserSubscriptionItemInfo(label, value).getElement());
    }

    private void addInfo(final String label, final DataResource value) {
        subscriptionInfoUi.appendChild(new UserSubscriptionItemInfo(label, value).getElement());
    }

    private void addInfo(final String label, final TimePoint value) {
        addInfo(label, formatDateAndTime(value.asDate()));
    }

}
