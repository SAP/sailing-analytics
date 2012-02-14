package com.sap.sailing.gwt.ui.shared.components;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.DataEntryDialog;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class SettingsDialog<SettingsType> extends DataEntryDialog<SettingsType> {
    private final SettingsDialogComponent<SettingsType> settingsDialogComponent;
    
    public SettingsDialog(final Component<SettingsType> component, StringMessages stringConstants) {
        super(stringConstants.settingsForComponent(component.getLocalizedShortName()), null, stringConstants.ok(),
                stringConstants.cancel(), component.getSettingsDialogComponent().getValidator(),
                new AsyncCallback<SettingsType>() {
                    @Override
                    public void onFailure(Throwable t) {
                    }

                    @Override
                    public void onSuccess(SettingsType newSettings) {
                        component.updateSettings(newSettings);
                    }
                });
        this.settingsDialogComponent = component.getSettingsDialogComponent();
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
