package com.sap.sailing.gwt.autoplay.client.shared.header;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.shared.components.ComponentLifecycle;

public class SAPHeaderComponentLifecycle implements ComponentLifecycle<SAPHeaderComponentSettings, SAPHeaderComponentSettingsDialogComponent> {
    private final StringMessages stringMessages;
    private final String defaultTitle;
    
    public SAPHeaderComponentLifecycle(String defaultTitle, StringMessages stringMessages) {
        this.defaultTitle = defaultTitle;
        this.stringMessages = stringMessages;
    }
    
    @Override
    public SAPHeaderComponentSettingsDialogComponent getSettingsDialogComponent(SAPHeaderComponentSettings settings) {
        return new SAPHeaderComponentSettingsDialogComponent(settings, stringMessages);
    }

    @Override
    public SAPHeaderComponentSettings createDefaultSettings() {
        return new SAPHeaderComponentSettings(defaultTitle);
    }

    @Override
    public String getLocalizedShortName() {
        return "SAP Header";
    }

    @Override
    public boolean hasSettings() {
        return true;
    }
    
    @Override
    public String getComponentId() {
        return "SAPHeaderComponent";
    }

    @Override
    public SAPHeaderComponentSettings extractGlobalSettings(SAPHeaderComponentSettings settings) {
        return createDefaultSettings();
    }

    @Override
    public SAPHeaderComponentSettings extractContextSettings(SAPHeaderComponentSettings settings) {
        return createDefaultSettings();
    }
}

