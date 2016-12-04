package com.sap.sse.gwt.client.shared.components;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class SettingsDialog<SettingsType extends Settings> extends DataEntryDialog<SettingsType> {
    private final SettingsDialogComponent<SettingsType> settingsDialogComponent;
    
    public SettingsDialog(final Component<SettingsType> component, StringMessages stringMessages) {
        this(component, stringMessages, /* animationEnabled */ true);
    }

    public SettingsDialog(final Component<SettingsType> component, StringMessages stringMessages, boolean animationEnabled) {
        this(component, component.getSettingsDialogComponent(), stringMessages, animationEnabled, null);
    }

    public SettingsDialog(final Component<SettingsType> component, StringMessages stringMessages, DialogCallback<SettingsType> callback) {
        this(component, component.getSettingsDialogComponent(), stringMessages, /* animationEnabled */ true, callback);
    }

    /**
     * Creates a settings button for <code>component</code> that, when clicked, opens a settings dialog for that
     * component and when confirmed, updates that component's settings. The button has no CSS style attached to
     * give callers full flexibility as to how to style the button.
     */
    public static <T extends Settings> Button createSettingsButton(final Component<T> component, final StringMessages stringMessages) {
        Button settingsButton = new Button();
        settingsButton.setTitle(stringMessages.settings());
        settingsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new SettingsDialog<T>(component, stringMessages).show();
            }
        });
        return settingsButton;
    }
    

    /**
     * This auxiliary constructor is required to avoid duplicate calls to {@link Component#getSettingsDialogComponent()}
     * which may choose to create a new instance each call. Such duplicate instances would cause the validator to
     * operate on a different instance as the one used for displaying, hence not allowing the validator to use the UI
     * elements, neither for update nor read.
     */
    private SettingsDialog(final Component<SettingsType> component,
            SettingsDialogComponent<SettingsType> dialogComponent, StringMessages stringMessages,
            boolean animationEnabled, final DialogCallback<SettingsType> callback) {
        super(stringMessages.settingsForComponent(component.getLocalizedShortName()), null, stringMessages.ok(),
                stringMessages.cancel(), dialogComponent.getValidator(), animationEnabled,
                    new DialogCallback<SettingsType>() {
                        @Override
                        public void cancel() {
                            if (callback != null) {
                                callback.cancel();
                            }
                        }
    
                        @Override
                        public void ok(SettingsType newSettings) {
                            component.updateSettings(newSettings);
                            if (callback != null) {
                                callback.ok(newSettings);
                            }
                        }
                    });
        this.settingsDialogComponent = dialogComponent;
        
        //TODO make all Components to have some ComponentContext
        if(component.getComponentTreeNodeInfo().getComponentContext() != null && component.getComponentTreeNodeInfo().getComponentContext().hasMakeCustomDefaultSettingsSupport(component)) {
            initMakeDefaultButtons(component, stringMessages);
        }
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
    protected FocusWidget getInitialFocusWidget() {
        return settingsDialogComponent.getFocusWidget();
    }
    
    private void initMakeDefaultButtons(final Component<SettingsType> component, StringMessages stringMessages) {
        Button makeDefaultButton = new Button(stringMessages.save());
        makeDefaultButton.getElement().getStyle().setMargin(3, Unit.PX);
        makeDefaultButton.ensureDebugId("SaveButton");
        getLeftButtonPannel().add(makeDefaultButton);
        makeDefaultButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                component.getComponentTreeNodeInfo().getComponentContext().makeSettingsDefault(component, getResult());
                //TODO i18n + use nice styled dialog
                Window.alert("Settings have been successfully saved");
            }
        });
    }
}
