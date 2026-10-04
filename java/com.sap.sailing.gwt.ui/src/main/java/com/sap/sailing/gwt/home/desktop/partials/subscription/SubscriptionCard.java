package com.sap.sailing.gwt.home.desktop.partials.subscription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.shared.places.subscription.SailingSubscriptionStringConstants;
import com.sap.sailing.gwt.home.shared.places.subscription.SubscriptionGroupDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.security.shared.subscription.SubscriptionPrice;
import com.sap.sse.security.shared.subscription.SubscriptionPlan.PlanGroup;
import com.sap.sse.security.ui.authentication.AuthenticationContextEvent;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.client.i18n.subscription.SubscriptionStringConstants;

public class SubscriptionCard extends Composite {

    public static enum Type {
        FREE, OWNER, HIGHLIGHT, DEFAULT, UPGRADE, ONETIMELOCK
    }

    private static SubscriptionUiBinder uiBinder = GWT.create(SubscriptionUiBinder.class);
    private static final String FEATURE_STYLE = SubscriptionCardResources.INSTANCE.css().feature();
    private static final String HIGHLIGHT_STYLE = SubscriptionCardResources.INSTANCE.css().highlight();
    private static final String OWNED_STYLE = SubscriptionCardResources.INSTANCE.css().owned();
    private static final String SUBSCRIPTION_STYLE = SubscriptionCardResources.INSTANCE.css().subscription();
    private static final String FREE_STYLE = SubscriptionCardResources.INSTANCE.css().free();
    private static final String PRICE_STYLE = SubscriptionCardResources.INSTANCE.css().price();
    private static final String PRICE_DISABLED_STYLE = SubscriptionCardResources.INSTANCE.css().priceDisabled();
    private static final String PRICE_INFO_STYLE = SubscriptionCardResources.INSTANCE.css().priceInfo();
    private static final String SELECTED_STYLE = SubscriptionCardResources.INSTANCE.css().selected();
    private static final String BUTTON_WARNING = SubscriptionCardResources.INSTANCE.css().buttonWarning();

    interface SubscriptionUiBinder extends UiBinder<Widget, SubscriptionCard> {
    }

    @UiField
    Button button;
    @UiField
    HeadingElement title;
    @UiField
    HeadingElement description;
    @UiField
    HeadingElement info;
    @UiField
    FlowPanel prices;
    @UiField
    FlowPanel features;
    @UiField
    FlowPanel highlightHeader;

    @UiField
    StringMessages i18n;

    private final Consumer<SubscriptionPrice> subscriptionCallback;
    private SubscriptionPrice currentPrice;
    private final SubscriptionGroupDTO subscriptionGroupDTO;

    public <T> SubscriptionCard(SubscriptionGroupDTO subscriptionGroupDTO, Type type, Consumer<SubscriptionPrice> subscriptionCallback, EventBus eventBus, boolean loggedIn, boolean emailValidated) {
        this.subscriptionCallback = subscriptionCallback;
        this.subscriptionGroupDTO = subscriptionGroupDTO;
        SubscriptionCardResources.INSTANCE.css().ensureInjected();
        SharedResources.INSTANCE.mediaCss().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        addStyleName(SUBSCRIPTION_STYLE);
        final List<SubscriptionPrice> priceList = new ArrayList<SubscriptionPrice>(subscriptionGroupDTO.getPrices());
        priceList.sort(new Comparator<SubscriptionPrice>() {
            @Override
            public int compare(SubscriptionPrice o1, SubscriptionPrice o2) {
                return o1.getPaymentInterval().compareTo(o2.getPaymentInterval());
            }
        });
        if (priceList.size() > 0) {
            currentPrice = priceList.get(0);
        } else {
            currentPrice = null;
        }
        final SailingSubscriptionStringConstants subscriptionStringConstants = SailingSubscriptionStringConstants.INSTANCE;
        if (priceList != null && !priceList.isEmpty()) {
            for (SubscriptionPrice subscriptionPrice: priceList) {
                FocusPanel price = new FocusPanel();
                price.addStyleName(PRICE_STYLE);
                String intervalTranslated = SubscriptionStringConstants.INSTANCE.getString("payment_interval_" + subscriptionPrice.getPaymentInterval().name());
                String priceText =  NumberFormat.getCurrencyFormat("USD").format(subscriptionPrice.getPrice())
                        + " / " + intervalTranslated;
                price.add(new Label(priceText));
                if (subscriptionPrice.equals(currentPrice)) {
                    price.addStyleName(SELECTED_STYLE);
                }
                price.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        for (int i=0; i<prices.getWidgetCount(); i++) {
                            prices.getWidget(i).removeStyleName(SELECTED_STYLE);
                        }
                        currentPrice = subscriptionPrice;
                        price.addStyleName(SELECTED_STYLE);
                        final boolean isButtonAvailable = button.isVisible();
                        if (isButtonAvailable) {
                            triggerSubscription();
                        }
                    }
                });
                if (!subscriptionPrice.getDisablePlan()) {
                    prices.add(price);
                }
                Label priceInfo = new Label(subscriptionStringConstants.getString(subscriptionGroupDTO.getSubscriptionGroupPriceInfoMessageKey()));
                priceInfo.addStyleName(PRICE_INFO_STYLE);
                if (Boolean.TRUE.equals(subscriptionPrice.getIsOneTimePayment())
                        || PlanGroup.TRIAL == subscriptionGroupDTO.getGroup()) {
                    prices.add(priceInfo);
                }
            }
        } else {
            final String priceText;
            if (type == Type.FREE) {
                priceText = i18n.free();
            } else {
                priceText = i18n.price() + ": - ";
            }
            FlowPanel price = new FlowPanel();
            price.addStyleName(PRICE_STYLE);
            price.add(new Label(priceText));
            prices.add(price);
        }
        switch (type) {
        case HIGHLIGHT:
            addStyleName(HIGHLIGHT_STYLE);
            highlightHeader.add(new Label(i18n.subscriptionHighlightText()));
            button.setText(i18n.subscribe());
            break;
        case DEFAULT:
            if (emailValidated) {
                button.setText(i18n.subscribe());
            } else {
                button.setText(i18n.emailNotValidated());
                button.addStyleName(BUTTON_WARNING);
            }
            break;
        case UPGRADE:
            if (emailValidated) {
                button.setText(i18n.upgrade());
            } else {
                button.setText(i18n.emailNotValidated());
                button.addStyleName(BUTTON_WARNING);
            }
            break;
        case OWNER:
            addStyleName(OWNED_STYLE);
            highlightHeader.add(new Label(i18n.subscriptionOwnerHeaderText()));
            button.setText(i18n.userManagement());
            prices.addStyleName(PRICE_DISABLED_STYLE);
            break;
        case ONETIMELOCK:
            addStyleName(OWNED_STYLE);
            highlightHeader.add(new Label(i18n.subscriptionOneTimePlanLockedText()));
            button.setVisible(false);
            break;
        case FREE:
            addStyleName(FREE_STYLE);
            button.setText(i18n.signInOrUp());
            button.setVisible(!loggedIn);
            eventBus.addHandler(AuthenticationContextEvent.TYPE, event->{
                final AuthenticationContext authContext = event.getCtx();
                // make it point to the current server if the user has CREATE_OBJECT permission there
                button.setVisible(!authContext.isLoggedIn());
            });
        default:
            break;
        }
        title.setInnerText(subscriptionStringConstants.getString(subscriptionGroupDTO.getSubscriptionGroupNameMessageKey()));
        description.setInnerText(subscriptionStringConstants.getString(subscriptionGroupDTO.getSubscriptionGroupDescMessageKey()));
        info.setInnerText(subscriptionStringConstants.getString(subscriptionGroupDTO.getSubscriptionGroupInfoMessageKey()));
        final String[] featureStrings = subscriptionStringConstants.getString(subscriptionGroupDTO.getSubscriptionGroupFeatureMessageKey()).split("[|]");
        for (String featureString : featureStrings) {
            if(featureString != "") {
                FlowPanel feature = new FlowPanel();
                feature.addStyleName(FEATURE_STYLE);
                feature.add(new Label(featureString));
                features.add(feature);
            }
        }
    }
    
    public SubscriptionGroupDTO getSubscriptionGroupDTO() {
        return subscriptionGroupDTO;
    }

    @UiHandler("button")
    void onClick(ClickEvent e) {
        triggerSubscription();
    }

    private void triggerSubscription() {
        subscriptionCallback.accept(currentPrice);
    }

}
