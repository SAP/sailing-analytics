package com.sap.sailing.gwt.home.desktop.partials.subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.AnimationType;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.resources.SharedDesktopResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;
import com.sap.sse.security.ui.client.i18n.subscription.SubscriptionStringConstants;

public class SubscriptionCardContainer extends Composite {

    private static final String SUPPORT_EMAIL = "support@sapsailing.com";
    private static SubscriptionContainerUiBinder uiBinder = GWT.create(SubscriptionContainerUiBinder.class);
    private static final SubscriptionStringConstants i18n = SubscriptionStringConstants.INSTANCE;
    private final static int TITLE_DESCRIPTION_ROW = 0;

    @UiField
    Button businessModelInfoButton;
    @UiField
    Button emailContact;
    @UiField
    FlowPanel container;
    @UiField
    FlowPanel features;

    final Grid featureGrid;

    interface SubscriptionContainerUiBinder extends UiBinder<Widget, SubscriptionCardContainer> {
    }

    public SubscriptionCardContainer() {
        initWidget(uiBinder.createAndBindUi(this));
        featureGrid = new Grid(2, SubscriptionPlan.PlanCategory.getCategoriesWithFeature().size() + 1);
        int rowIndex = 0;
        for (final SubscriptionPlan.PlanCategory category : SubscriptionPlan.PlanCategory.getCategoriesWithFeature()) {
            rowIndex++;
            final Label title = new Label(i18n.getString(category.getId() + "_name"));
            featureGrid.setWidget(0, rowIndex, title);
            featureGrid.getCellFormatter().addStyleName(0, rowIndex, SubscriptionCardResources.INSTANCE.css().featureHeader());
        }
        // add features to list
        for (final String featureId : SubscriptionPlan.PlanCategory.getAllFeatureIds()) {
            addFeature(featureId);
        }
        features.add(featureGrid);
        emailContact.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.Location
                        .assign("mailto:" + SUPPORT_EMAIL + "?subject=" + UriUtils.encode(i18n.support_subject()));
            }
        });
    }

    private void addFeature(final String featureId) {
        int currentRowIndex = featureGrid.getRowCount();
        // get size but index starts with 0 therefore row count is current index + 1
        featureGrid.resizeRows(currentRowIndex + 1);
        VerticalPanel line = new VerticalPanel();
        String titleString = i18n.getString(featureId + "_title");
        String descriptionString = i18n.getString(featureId + "_description");
        // title
        Label title = new Label(titleString);
        title.addStyleName(SubscriptionCardResources.INSTANCE.css().featureTitle());
        line.add(title);
        // description
        try {            
            String urlString = i18n.getString(featureId + "_url");
            // add a description with link
            SimplePanel descriptionWithLink = new SimplePanel();
            HTML exampleLink = new HTML(descriptionString + "&nbsp;<a href=\""
                    + new SafeHtmlBuilder().appendEscaped(urlString).toSafeHtml().asString() + "\" title=\""
                    + StringMessages.INSTANCE.moreInfo() + "\"" + " class=\""
                    + SubscriptionCardResources.INSTANCE.css().featureLink() + "\"" + "target=\"_blank\">"
                    + new SafeHtmlBuilder().appendEscaped("ⓘ").toSafeHtml().asString() + "</a>");
            descriptionWithLink.addStyleName(SubscriptionCardResources.INSTANCE.css().featureDescription());
            descriptionWithLink.add(exampleLink);
            line.add(descriptionWithLink);
        } catch (MissingResourceException e) {
            // add a description without link
            Label description = new Label(descriptionString);
            description.addStyleName(SubscriptionCardResources.INSTANCE.css().featureDescription());
            line.add(description);
        }
        featureGrid.setWidget(currentRowIndex, TITLE_DESCRIPTION_ROW, line);
        // create check marks
        int columnIndex = 0;
        for (final SubscriptionPlan.PlanCategory category : SubscriptionPlan.PlanCategory.getCategoriesWithFeature()) {
            columnIndex++;
            createCheckMark(currentRowIndex, columnIndex, category.containsFeature(featureId));
        }
    }

    /**
     * Paint a check icon or if state is false a X icon.
     * 
     * @param currentRowIndex
     *            the current row
     * @param column
     *            the column
     * @param checkState
     *            the state (true: check, false: X)
     */
    private void createCheckMark(int currentRowIndex, final int column, boolean checkState) {
        FlowPanel check = new FlowPanel();
        if (checkState) {
            check.add(new Image(SharedDesktopResources.INSTANCE.dropdownCheck().getSafeUri()));
            check.addStyleName(SubscriptionCardResources.INSTANCE.css().featureCheck());
        } else {
            check.add(new Label("-"));
            check.addStyleName(SubscriptionCardResources.INSTANCE.css().featureNone());
        }
        featureGrid.setWidget(currentRowIndex, column, check);
    }

    public void addSubscription(SubscriptionCard subscription) {
        if (!isSubscriptionPlanExisting(subscription.getSubscriptionGroupDTO().getSubscriptionGroupId())) {
            container.add(subscription);
        }
    }

    private boolean isSubscriptionPlanExisting(String planId) {
        boolean isExisting = false;
        for (int i = 0; i < container.getWidgetCount(); i++) {
            if (container.getWidget(i) instanceof SubscriptionCard) {
                SubscriptionCard card = (SubscriptionCard) container.getWidget(i);
                if (card.getSubscriptionGroupDTO().getSubscriptionGroupId().equals(planId)) {
                    isExisting = true;
                    break;
                }
            }
        }
        return isExisting;
    }

    public void resetSubscriptions() {
        List<SubscriptionCard> subscriptionCardsToRemove = new ArrayList<>();
        for (int i = 0; i < container.getWidgetCount(); i++) {
            if (container.getWidget(i) instanceof SubscriptionCard) {
                subscriptionCardsToRemove.add((SubscriptionCard) container.getWidget(i));
            }
        }
        for (SubscriptionCard card : subscriptionCardsToRemove) {
            container.remove(card);
        }
    }

    @UiHandler("businessModelInfoButton")
    void onClick(ClickEvent e) {
        VerticalPanel content = new VerticalPanel();
        HTMLPanel title = new HTMLPanel("h1", i18n.businessModelTitle());
        content.add(title);
        Label body = new Label(i18n.businessModelDescription());
        content.add(body);
        PopupPanel popup = new PopupPanel();
        popup.setWidget(content);
        popup.addStyleName(SubscriptionCardResources.INSTANCE.css().popupContent());
        popup.setGlassStyleName(SubscriptionCardResources.INSTANCE.css().popupGlass());
        popup.setAnimationEnabled(true);
        popup.setAnimationType(AnimationType.CENTER);
        popup.setGlassEnabled(true);
        popup.setModal(true);
        popup.setAutoHideEnabled(true);
        popup.addAutoHidePartner(businessModelInfoButton.getElement());
        popup.center();
    }
}