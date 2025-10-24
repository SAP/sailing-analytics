package com.sap.sailing.gwt.ui.client;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.LongSetting;

public class TimePanelSettings extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = -4397130065617955193L;
    
    private LongSetting refreshInterval;
    
    @Override
    protected void addChildSettings() {
        refreshInterval = new LongSetting("", this, 1000l);
    }

    public TimePanelSettings(long refreshInterval) {
        this();
        this.refreshInterval.setValue(refreshInterval);
    }

    public TimePanelSettings() {
        super();
    }

    public long getRefreshInterval() {
        return refreshInterval.getValue();
    }
}
