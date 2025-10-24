package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.EnumLinkedHashSetSetting;
import com.sap.sse.common.settings.generic.LongSetting;

public class WindChartSettings extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = -3250243915670349222L;

    public static final long DEFAULT_RESOLUTION_IN_MILLISECONDS = 10000;

    private EnumLinkedHashSetSetting<WindSourceType> windDirectionSourcesToDisplay;
    private EnumLinkedHashSetSetting<WindSourceType> windSpeedSourcesToDisplay;
    
    private LongSetting resolutionInMilliseconds;

    private BooleanSetting showWindSpeedSeries;
    private BooleanSetting showWindDirectionsSeries;
    
    @Override
    protected void addChildSettings() {
        Set<WindSourceType> defaultWindDirectionSourcesToDisplay = new LinkedHashSet<WindSourceType>();
        defaultWindDirectionSourcesToDisplay.add(WindSourceType.COMBINED);
        windDirectionSourcesToDisplay = new EnumLinkedHashSetSetting<>("windDirectionSourcesToDisplay", this, defaultWindDirectionSourcesToDisplay, WindSourceType::valueOf);
        
        Set<WindSourceType> defaultWindSpeedSourcesToDisplay = new LinkedHashSet<>();
        defaultWindSpeedSourcesToDisplay.add(WindSourceType.COMBINED);
        windSpeedSourcesToDisplay = new EnumLinkedHashSetSetting<>("windSpeedSourcesToDisplay", this, defaultWindSpeedSourcesToDisplay, WindSourceType::valueOf);
        
        resolutionInMilliseconds = new LongSetting("resolutionInMilliseconds", this, DEFAULT_RESOLUTION_IN_MILLISECONDS);
        showWindSpeedSeries = new BooleanSetting("showWindSpeedSeries", this, true);
        showWindDirectionsSeries = new BooleanSetting("showWindDirectionsSeries", this, true);
    }

    /**
     *  The default settings
     */
    public WindChartSettings() {
        super();
    }
    
    public WindChartSettings(boolean showWindSpeedSeries, Set<WindSourceType> windSpeedSourcesToDisplay, 
            boolean showWindDirectionsSeries, Set<WindSourceType> windDirectionSourcesToDisplay, long resolutionInMilliseconds) {
        this();
        this.showWindSpeedSeries.setValue(showWindSpeedSeries);
        this.windSpeedSourcesToDisplay.setValues(windSpeedSourcesToDisplay);
        this.showWindDirectionsSeries.setValue(showWindDirectionsSeries);
        this.windDirectionSourcesToDisplay.setValues(windDirectionSourcesToDisplay);
        this.resolutionInMilliseconds.setValue(resolutionInMilliseconds);
    }
    
    public Set<WindSourceType> getWindDirectionSourcesToDisplay() {
        return Util.asSet(windDirectionSourcesToDisplay.getValues());
    }

    public Set<WindSourceType> getWindSpeedSourcesToDisplay() {
        return Util.asSet(windSpeedSourcesToDisplay.getValues());
    }

    public long getResolutionInMilliseconds() {
        return resolutionInMilliseconds.getValue();
    }

    public boolean isShowWindSpeedSeries() {
        return showWindSpeedSeries.getValue();
    }

    public boolean isShowWindDirectionsSeries() {
        return showWindDirectionsSeries.getValue();
    }

    public void setResolutionInMilliseconds(long resolutionInMilliseconds) {
        this.resolutionInMilliseconds.setValue(resolutionInMilliseconds);
    }

    public void setShowWindSpeedSeries(boolean showWindSpeedSeries) {
        this.showWindSpeedSeries.setValue(showWindSpeedSeries);
    }

    public void setShowWindDirectionsSeries(boolean showWindDirectionsSeries) {
        this.showWindDirectionsSeries.setValue(showWindDirectionsSeries);
    }

    public void setWindDirectionSourcesToDisplay(Set<WindSourceType> windDirectionSourcesToDisplay) {
        if(windDirectionSourcesToDisplay != null) {
            this.windDirectionSourcesToDisplay.setValues(windDirectionSourcesToDisplay);
        }
    }

    public void setWindSpeedSourcesToDisplay(Set<WindSourceType> windSpeedSourcesToDisplay) {
        if(windSpeedSourcesToDisplay != null) {
            this.windSpeedSourcesToDisplay.setValues(windSpeedSourcesToDisplay);
        }
    }
}
