package com.sap.sailing.gwt.ui.leaderboard;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.sap.sailing.gwt.ui.actions.AsyncActionsExecutor;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.TimeListener;
import com.sap.sailing.gwt.ui.client.Timer;
import com.sap.sailing.gwt.ui.client.shared.components.Component;
import com.sap.sailing.gwt.ui.client.shared.components.SettingsDialog;

/**
 * A base class for a leaderboard viewer.
 * @author Frank Mittag (c163874)
 *
 */
public abstract class AbstractLeaderboardViewer extends SimplePanel {
    protected final StringMessages stringMessages;
    protected final CompetitorSelectionModel competitorSelectionProvider;
    protected final AsyncActionsExecutor asyncActionsExecutor;

    private FlowPanel componentsNavigationPanel;

    protected final Timer timer;
    
    private final static String STYLE_VIEWER_TOOLBAR = "viewerToolbar";
    private final static String STYLE_VIEWER_TOOLBAR_INNERELEMENT = "viewerToolbar-innerElement";
    private final static String STYLE_VIEWER_TOOLBAR_SETTINGS_BUTTON = "viewerToolbar-settingsButton";

    public AbstractLeaderboardViewer(CompetitorSelectionModel competitorSelectionProvider, AsyncActionsExecutor asyncActionsExecutor,
            Timer timer, StringMessages stringMessages) {
        this.competitorSelectionProvider = competitorSelectionProvider;
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.stringMessages = stringMessages;
        this.timer = timer;
    }
    
    protected FlowPanel createViewerPanel() {
        FlowPanel mainPanel = new FlowPanel();
        mainPanel.setSize("100%", "100%");
        
        getElement().getStyle().setMarginLeft(12, Unit.PX);
        getElement().getStyle().setMarginRight(12, Unit.PX);

        componentsNavigationPanel = new FlowPanel();
        componentsNavigationPanel.addStyleName(STYLE_VIEWER_TOOLBAR);
        mainPanel.add(componentsNavigationPanel);
        
        return mainPanel;
    }
    
    protected <SettingsType> void addComponentToNavigationMenu(final Component<SettingsType> component, boolean isCheckboxEnabled, 
            String componentDisplayName, final boolean hasSettingsWhenComponentIsInvisible) {
        final String componentName = componentDisplayName != null ? componentDisplayName : component.getLocalizedShortName(); 
        final CheckBox checkBox= new CheckBox(componentName);
        final Button settingsButton = new Button("");
        
        checkBox.getElement().getStyle().setFloat(Style.Float.LEFT);
        
        checkBox.setEnabled(isCheckboxEnabled);
        checkBox.setValue(component.isVisible());
        checkBox.setTitle(stringMessages.showHideComponent(componentName));
        checkBox.addStyleName(STYLE_VIEWER_TOOLBAR_INNERELEMENT);

        checkBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> newValue) {
                boolean visible = checkBox.getValue();
                component.setVisible(visible);

                if (visible && component instanceof TimeListener) {
                    // trigger the component to update its data
                    ((TimeListener) component).timeChanged(timer.getTime());
                }
                if(component.hasSettings() && !hasSettingsWhenComponentIsInvisible) {
                    settingsButton.setEnabled(visible);
                }
            }
        });

        componentsNavigationPanel.add(checkBox);

        if(component.hasSettings()) {
            settingsButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    new SettingsDialog<SettingsType>(component, stringMessages).show();
                } 
            });
        } 
        
        settingsButton.setEnabled(component.hasSettings() && hasSettingsWhenComponentIsInvisible);
        settingsButton.addStyleName(STYLE_VIEWER_TOOLBAR_SETTINGS_BUTTON);
        settingsButton.getElement().getStyle().setFloat(Style.Float.LEFT);
        settingsButton.setTitle(stringMessages.settingsForComponent(componentName));
        
        componentsNavigationPanel.add(settingsButton);
    }
}

