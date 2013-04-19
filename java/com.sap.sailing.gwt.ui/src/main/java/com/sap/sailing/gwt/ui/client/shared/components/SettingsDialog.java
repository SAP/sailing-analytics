package com.sap.sailing.gwt.ui.client.shared.components;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.DataEntryDialog;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class SettingsDialog<SettingsType> extends DataEntryDialog<SettingsType> {
    private final SettingsDialogComponent<SettingsType> settingsDialogComponent;
    
    public SettingsDialog(final Component<SettingsType> component, StringMessages stringMessages) {
        this(component, stringMessages, /* animationEnabled */ true);
    }

    /**
     * This auxiliary constructor is required to avoid duplicate calls to {@link Component#getSettingsDialogComponent()}
     * which may choose to create a new instance each call. Such duplicate instances would cause the validator to
     * operate on a different instance as the one used for displaying, hence not allowing the validator to use the UI
     * elements, neither for update nor read.
     */
    private SettingsDialog(final Component<SettingsType> component,
            SettingsDialogComponent<SettingsType> dialogComponent, StringMessages stringMessages,
            boolean animationEnabled) {
        super(stringMessages.settingsForComponent(component.getLocalizedShortName()), null, stringMessages.ok(),
                stringMessages.cancel(), dialogComponent.getValidator(), animationEnabled,
                new DialogCallback<SettingsType>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(SettingsType newSettings) {
                        component.updateSettings(newSettings);
                    }
                });
        this.settingsDialogComponent = dialogComponent;
    }

    public SettingsDialog(final Component<SettingsType> component, StringMessages stringMessages, boolean animationEnabled) {
        this(component, component.getSettingsDialogComponent(), stringMessages, animationEnabled);
    }

    @Override
    protected Widget getAdditionalWidget() {
        return settingsDialogComponent.getAdditionalWidget(this);
    }

    @Override
    protected SettingsType getResult() {
        return settingsDialogComponent.getResult();
    }

    @Override
    public void show() {
        super.show();
        FocusWidget focusWidget = settingsDialogComponent.getFocusWidget();
        if (focusWidget != null) {
            focusWidget.setFocus(true);
        }
    }
    
}
