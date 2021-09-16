package com.sap.sse.security.ui.client.premium;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Image;
import com.sap.sse.gwt.client.dialog.ConfirmationDialog;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.shared.subscription.SubscriptionPlanDTO;

public abstract class PremiumCheckBox extends PremiumUiElement implements HasValue<Boolean>, HasAllKeyHandlers {

    private static PremiumCheckBoxUiBinder uiBinder = GWT.create(PremiumCheckBoxUiBinder.class);

    interface PremiumCheckBoxUiBinder extends UiBinder<FocusPanel, PremiumCheckBox> {
    }

    interface Style extends CssResource {
        @ClassName("premium-container")
        String premiumContainer();

        @ClassName("premium-check-box")
        String premiumCheckBox();

        @ClassName("premium-permitted")
        String premiumPermitted();

        @ClassName("premium-icon")
        String premiumIcon();
    }

    @UiField
    StringMessages i18n;
    @UiField
    Style style;
    @UiField
    FocusPanel container;
    @UiField(provided = true)
    protected final Image image;
    @UiField(provided = true)
    protected final CheckBox checkBox;

    private final ConfirmationDialog subscribeDialog;

    /**
     * A Composite component, that includes a checkbox and an additional premium icon, indicating that the feature to be
     * enabled is a premium feature if the user does not have the permission.
     */
    protected PremiumCheckBox(final String label, final Action action, final PaywallResolver paywallResolver) {
        super(action, paywallResolver);
        this.image = createPremiumIcon();
        this.checkBox = new CheckBox(label);

        initWidget(uiBinder.createAndBindUi(this));

        this.subscribeDialog = ConfirmationDialog.create(i18n.subscriptionSuggestionTitle(),
                i18n.pleaseSubscribeToUse(), i18n.takeMeToSubscriptions(), i18n.cancel(),
                () -> paywallResolver.getUnlockingSubscriptionPlans(action, this::onSubscribeDialogConfirmation));
        updateUserPermission();
    }

    protected abstract void onSubscribeDialogConfirmation(Set<SubscriptionPlanDTO> unlockingPlans);

    @Override
    protected void onEnsureDebugId(final String baseID) {
        this.checkBox.ensureDebugId(baseID);
    }

    @UiHandler("container")
    void onContainerClicked(final ClickEvent event) {
        if (!hasPermission()) {
            this.updateUserPermission();
            subscribeDialog.center();
        }
    }

    @Override
    protected void onUserPermissionUpdate(final boolean isPermitted) {
        checkBox.setValue(getValue());
        checkBox.setEnabled(isEnabled() && isPermitted);
        checkBox.setStyleName(style.premiumPermitted(), isPermitted);
    }

    @Override
    public void setValue(final Boolean value) {
        this.checkBox.setValue(hasPermission() && value);
    }

    @Override
    public void setValue(final Boolean value, final boolean fireEvents) {
        this.checkBox.setValue(hasPermission() && value, fireEvents);
    }

    @Override
    public Boolean getValue() {
        return hasPermission() && checkBox.getValue();
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<Boolean> handler) {
        return checkBox.addValueChangeHandler(handler);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        this.checkBox.setEnabled(hasPermission() && enabled);
    }

    @Override
    public HandlerRegistration addKeyDownHandler(final KeyDownHandler handler) {
        return container.addKeyDownHandler(handler);
    }

    @Override
    public HandlerRegistration addKeyPressHandler(final KeyPressHandler handler) {
        return container.addKeyPressHandler(handler);
    }

    @Override
    public HandlerRegistration addKeyUpHandler(final KeyUpHandler handler) {
        return container.addKeyUpHandler(handler);
    }

    public FocusWidget getFocusWidget() {
        return this.checkBox;
    }
}
