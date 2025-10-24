package com.sap.sailing.gwt.ui.client.shared.charts;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.LongSetting;

public class ChartSettings extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = 7041836078658713937L;
    
    private LongSetting stepSizeInMillis;
    
    @Override
    protected void addChildSettings() {
        stepSizeInMillis = new LongSetting("stepSizeInMillis", this, AbstractCompetitorRaceChart.DEFAULT_STEPSIZE);
    }
    
    public ChartSettings() {
        super();
    }
    
    public ChartSettings(long stepSizeInMillis) {
        this();
        this.stepSizeInMillis.setValue(stepSizeInMillis);
    }

    public long getStepSizeInMillis() {
        return stepSizeInMillis.getValue();
    }
}
