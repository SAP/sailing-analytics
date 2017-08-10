package com.sap.sailing.gwt.ui.datamining.developer;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.datamining.QueryDefinitionChangedListener;
import com.sap.sailing.gwt.ui.datamining.developer.QueryDefinitionParser.TypeToCodeStrategy;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.ComponentWithoutSettings;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

public class QueryDefinitionViewer extends ComponentWithoutSettings implements QueryDefinitionChangedListener {
    
    private static final String codeFormatRadioButtonGroup = "codeFormatRadioButtonGroup";
    
    private final StringMessages stringMessages;
    private final QueryDefinitionParser queryDefinitionParser;
    
    private final DockLayoutPanel dockPanel;
    private final TabLayoutPanel contentPanel;
    private final HTML detailsHtml;
    private final HTML codeHtml;
    private final RadioButton useClassGetNameRadioButton;
    private final RadioButton useStringLiteralsRadioButton;
    
    private StatisticQueryDefinitionDTO currentDefinition;

    public QueryDefinitionViewer(Component<?> parent, ComponentContext<?> context, StringMessages stringMessages) {
        super(parent, context);
        this.stringMessages = stringMessages;
        queryDefinitionParser = new QueryDefinitionParser();
        
        detailsHtml = new HTML();
        detailsHtml.setWordWrap(false);
        
        codeHtml = new HTML();
        codeHtml.setWordWrap(false);
        ValueChangeHandler<Boolean> typeStrategyChangedHandler = new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                updateCode();
            }
        };
        useClassGetNameRadioButton = new RadioButton(codeFormatRadioButtonGroup, stringMessages.useClassGetName());
        useClassGetNameRadioButton.setTitle(stringMessages.useClassGetNameTooltip());
        useClassGetNameRadioButton.setValue(true);
        useClassGetNameRadioButton.addValueChangeHandler(typeStrategyChangedHandler);
        useStringLiteralsRadioButton = new RadioButton(codeFormatRadioButtonGroup, stringMessages.useStringLiterals());
        useStringLiteralsRadioButton.setTitle(stringMessages.useStringLiteralsTooltip());
        useStringLiteralsRadioButton.addValueChangeHandler(typeStrategyChangedHandler);
        HorizontalPanel codeControlsPanel = new HorizontalPanel();
        codeControlsPanel.setSpacing(5);
        codeControlsPanel.add(useClassGetNameRadioButton);
        codeControlsPanel.add(useStringLiteralsRadioButton);
        DockLayoutPanel codeDockPanel = new DockLayoutPanel(Unit.PX);
        codeDockPanel.addNorth(codeControlsPanel, 30);
        ScrollPanel codeScrollPanel = new ScrollPanel(codeHtml);
        codeScrollPanel.getElement().addClassName("queryDefinitionViewerContent");
        codeDockPanel.add(codeScrollPanel);
        
        Button copyToClipboardButton = new Button(stringMessages.copyToClipboard(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                switch (contentPanel.getSelectedIndex()) {
                case 0:
                    copyToClipboard(queryDefinitionParser.parseToDetailsAsText(currentDefinition));
                    break;
                case 1:
                    copyToClipboard(queryDefinitionParser.parseToCodeAsText(currentDefinition, getTypeStrategy()));
                    break;
                }
            }
        });

        HorizontalPanel controlsPanel = new HorizontalPanel();
        controlsPanel.setSpacing(5);
        controlsPanel.add(copyToClipboardButton);
        
        contentPanel = new TabLayoutPanel(30, Unit.PX);
        ScrollPanel detailsScrollPanel = new ScrollPanel(detailsHtml);
        detailsScrollPanel.getElement().addClassName("queryDefinitionViewerContent");
        contentPanel.add(detailsScrollPanel, stringMessages.details());
        contentPanel.add(codeDockPanel, stringMessages.code());
        
        dockPanel = new DockLayoutPanel(Unit.PX);
        dockPanel.addSouth(controlsPanel, 45);
        dockPanel.add(contentPanel);
    }
    
    public static native void copyToClipboard(String text) /*-{
		window.prompt("Copy to clipboard: Ctrl+C, Enter", text);
    }-*/;
    
    @Override
    public void queryDefinitionChanged(StatisticQueryDefinitionDTO newQueryDefinition) {
        currentDefinition = newQueryDefinition;
        updateDetails();
        updateCode();
    }

    private void updateDetails() {
        detailsHtml.setHTML(queryDefinitionParser.parseToDetailsAsSafeHtml(currentDefinition));
    }

    private void updateCode() {
        codeHtml.setHTML(queryDefinitionParser.parseToCodeAsSafeHtml(currentDefinition, getTypeStrategy()));
    }

    private TypeToCodeStrategy getTypeStrategy() {
        if (useClassGetNameRadioButton.getValue()) {
            return TypeToCodeStrategy.CLASS_GET_NAME;
        }
        if (useStringLiteralsRadioButton.getValue()) {
            return TypeToCodeStrategy.STRING_LITERALS;
        }
        return TypeToCodeStrategy.CLASS_GET_NAME;
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.queryDefinitionViewer();
    }

    @Override
    public Widget getEntryWidget() {
        return dockPanel;
    }

    @Override
    public boolean isVisible() {
        return dockPanel.isVisible();
    }

    @Override
    public void setVisible(boolean visibility) {
        dockPanel.setVisible(visibility);
    }
        
    @Override
    public String getDependentCssClassName() {
        return "queryDefinitionViewer";
    }

    @Override
    public String getId() {
        return "QueryDefinitionViewer";
    }
}
