package com.sap.sse.security.ui.client.premium;

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
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.i18n.StringMessages;
import com.sap.sse.security.ui.client.premium.settings.AbstractSecuredValueSetting;

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
    private final boolean defaultValue;

    private final ConfirmationDialog subscribeDialog;

    /**
     * A Composite component, that includes a checkbox and an additional premium icon, indicating that the feature to be
     * enabled is a premium feature if the user does not have the permission.
     */
    protected PremiumCheckBox(final String label, final Action action, final PaywallResolver paywallResolver, final SecuredDTO contextDTO, final boolean defaultValue) {
        super(action, paywallResolver, contextDTO);
        image = createPremiumIcon();
        this.defaultValue = defaultValue;
        checkBox = new CheckBox(label);
        checkBox.setValue(defaultValue);
        initWidget(uiBinder.createAndBindUi(this));
        this.subscribeDialog = ConfirmationDialog.create(i18n.subscriptionSuggestionTitle(),
                i18n.pleaseSubscribeToUse(), i18n.takeMeToSubscriptions(), i18n.cancel(),
                () -> paywallResolver.getUnlockingSubscriptionPlans(action, contextDTO, this::onSubscribeDialogConfirmation));
        updateUserPermission();
    }

    protected PremiumCheckBox(final String label, AbstractSecuredValueSetting<Boolean> setting) {
        this(label, setting.getAction(), setting.getPaywallResolver(), setting.getSecuredDTO(), setting.getValue());
    }

    protected abstract void onSubscribeDialogConfirmation(Iterable<String> unlockingPlans);

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
        container.setStyleName(style.premiumPermitted(), isPermitted);
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
        final Boolean result;
        if (hasPermission()) {
            result = checkBox.getValue();
        } else {
            result = defaultValue;
        }
        return result;
    }

    @Override
    public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<Boolean> handler) {
        return checkBox.addValueChangeHandler(handler);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (hasPermission()) {
            super.setEnabled(enabled);
            checkBox.setEnabled(enabled);
        }
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
    
    public CheckBox getCheckBox() {
        return this.checkBox;
    }
}
