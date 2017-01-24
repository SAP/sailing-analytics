package com.sap.sailing.gwt.ui.datamining.selection;

import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.gwt.client.shared.components.AbstractComponent;
import com.sap.sse.gwt.client.shared.components.Component;

public abstract class RetrieverLevelSettingsComponent extends AbstractComponent<SerializableSettings> {

    private final DataRetrieverLevelDTO retrieverLevel;
    private final String localizedName;
    private final String componentId;

    public RetrieverLevelSettingsComponent(Component<?> parent,DataRetrieverLevelDTO retrieverLevel, String componentId, String localizedName) {
        super(parent);
        this.retrieverLevel = retrieverLevel;
        this.localizedName = localizedName;
        this.componentId = componentId;
    }
    
    public DataRetrieverLevelDTO getRetrieverLevel() {
        return retrieverLevel;
    }

    @Override
    public String getLocalizedShortName() {
        return localizedName;
    }

    @Override
    public Widget getEntryWidget() {
        throw new RuntimeException("Virtual component doesn't have a widget of its own");
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean visibility) {
        throw new RuntimeException("Virtual component doesn't know how to make itself visible");
    }

    @Override
    public boolean hasSettings() {
        return retrieverLevel.hasSettings();
    }

    @Override
    public String getDependentCssClassName() {
        return null;
    }

    @Override
    public SerializableSettings getSettings() {
        return retrieverLevel.getDefaultSettings();
    }
    
    @Override
    public String getId() {
        return componentId;
    }

}
