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
    private EnumLinkedHashSetSetting<WindSourceType> directionAvgSources;
    private EnumLinkedHashSetSetting<WindSourceType> directionMinSources;
    private EnumLinkedHashSetSetting<WindSourceType> directionMaxSources;
    private BooleanSetting directionAvgBulk;
    private BooleanSetting directionMinBulk;
    private BooleanSetting directionMaxBulk;
    private EnumLinkedHashSetSetting<WindSourceType> speedAvgSources;
    private EnumLinkedHashSetSetting<WindSourceType> speedMinSources;
    private EnumLinkedHashSetSetting<WindSourceType> speedMaxSources;
    private BooleanSetting speedAvgBulk;
    private BooleanSetting speedMinBulk;
    private BooleanSetting speedMaxBulk;

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
        directionAvgSources = new EnumLinkedHashSetSetting<>("directionAvgSources", this, new LinkedHashSet<WindSourceType>(), WindSourceType::valueOf);
        directionMinSources = new EnumLinkedHashSetSetting<>("directionMinSources", this, new LinkedHashSet<WindSourceType>(), WindSourceType::valueOf);
        directionMaxSources = new EnumLinkedHashSetSetting<>("directionMaxSources", this, new LinkedHashSet<WindSourceType>(), WindSourceType::valueOf);
        directionAvgBulk = new BooleanSetting("directionAvgBulk", this, false);
        directionMinBulk = new BooleanSetting("directionMinBulk", this, false);
        directionMaxBulk = new BooleanSetting("directionMaxBulk", this, false);
        speedAvgSources = new EnumLinkedHashSetSetting<>("speedAvgSources", this, new LinkedHashSet<WindSourceType>(), WindSourceType::valueOf);
        speedMinSources = new EnumLinkedHashSetSetting<>("speedMinSources", this, new LinkedHashSet<WindSourceType>(), WindSourceType::valueOf);
        speedMaxSources = new EnumLinkedHashSetSetting<>("speedMaxSources", this, new LinkedHashSet<WindSourceType>(), WindSourceType::valueOf);
        speedAvgBulk = new BooleanSetting("speedAvgBulk", this, false);
        speedMinBulk = new BooleanSetting("speedMinBulk", this, false);
        speedMaxBulk = new BooleanSetting("speedMaxBulk", this, false);
    }

    public WindChartSettings() {
        super();
    }

    public WindChartSettings(boolean showWindSpeedSeries, Set<WindSourceType> windSpeedSourcesToDisplay,
            boolean showWindDirectionsSeries, Set<WindSourceType> windDirectionSourcesToDisplay, long resolutionInMilliseconds,
            Set<WindSourceType> directionAvgSources, Set<WindSourceType> directionMinSources, Set<WindSourceType> directionMaxSources,
            boolean directionAvgBulk, boolean directionMinBulk, boolean directionMaxBulk,
            Set<WindSourceType> speedAvgSources, Set<WindSourceType> speedMinSources, Set<WindSourceType> speedMaxSources,
            boolean speedAvgBulk, boolean speedMinBulk, boolean speedMaxBulk) {
        this();
        this.showWindSpeedSeries.setValue(showWindSpeedSeries);
        this.windSpeedSourcesToDisplay.setValues(windSpeedSourcesToDisplay);
        this.showWindDirectionsSeries.setValue(showWindDirectionsSeries);
        this.windDirectionSourcesToDisplay.setValues(windDirectionSourcesToDisplay);
        this.resolutionInMilliseconds.setValue(resolutionInMilliseconds);
        this.directionAvgSources.setValues(directionAvgSources);
        this.directionMinSources.setValues(directionMinSources);
        this.directionMaxSources.setValues(directionMaxSources);
        this.directionAvgBulk.setValue(directionAvgBulk);
        this.directionMinBulk.setValue(directionMinBulk);
        this.directionMaxBulk.setValue(directionMaxBulk);
        this.speedAvgSources.setValues(speedAvgSources);
        this.speedMinSources.setValues(speedMinSources);
        this.speedMaxSources.setValues(speedMaxSources);
        this.speedAvgBulk.setValue(speedAvgBulk);
        this.speedMinBulk.setValue(speedMinBulk);
        this.speedMaxBulk.setValue(speedMaxBulk);
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
        if (windDirectionSourcesToDisplay != null) {
            this.windDirectionSourcesToDisplay.setValues(windDirectionSourcesToDisplay);
        }
    }

    public void setWindSpeedSourcesToDisplay(Set<WindSourceType> windSpeedSourcesToDisplay) {
        if (windSpeedSourcesToDisplay != null) {
            this.windSpeedSourcesToDisplay.setValues(windSpeedSourcesToDisplay);
        }
    }

    public Set<WindSourceType> getDirectionAvgSources() {
        return Util.asSet(directionAvgSources.getValues());
    }

    public void setDirectionAvgSources(final Set<WindSourceType> sources) {
        directionAvgSources.setValues(sources);
    }

    public Set<WindSourceType> getDirectionMinSources() {
        return Util.asSet(directionMinSources.getValues());
    }

    public void setDirectionMinSources(final Set<WindSourceType> sources) {
        directionMinSources.setValues(sources);
    }

    public Set<WindSourceType> getDirectionMaxSources() {
        return Util.asSet(directionMaxSources.getValues());
    }

    public void setDirectionMaxSources(final Set<WindSourceType> sources) {
        directionMaxSources.setValues(sources);
    }

    public boolean isDirectionAvgBulk() {
        return directionAvgBulk.getValue();
    }

    public void setDirectionAvgBulk(final boolean bulk) {
        directionAvgBulk.setValue(bulk);
    }

    public boolean isDirectionMinBulk() {
        return directionMinBulk.getValue();
    }

    public void setDirectionMinBulk(final boolean bulk) {
        directionMinBulk.setValue(bulk);
    }

    public boolean isDirectionMaxBulk() {
        return directionMaxBulk.getValue();
    }

    public void setDirectionMaxBulk(final boolean bulk) {
        directionMaxBulk.setValue(bulk);
    }

    public Set<WindSourceType> getSpeedAvgSources() {
        return Util.asSet(speedAvgSources.getValues());
    }

    public void setSpeedAvgSources(final Set<WindSourceType> sources) {
        speedAvgSources.setValues(sources);
    }

    public Set<WindSourceType> getSpeedMinSources() {
        return Util.asSet(speedMinSources.getValues());
    }

    public void setSpeedMinSources(final Set<WindSourceType> sources) {
        speedMinSources.setValues(sources);
    }

    public Set<WindSourceType> getSpeedMaxSources() {
        return Util.asSet(speedMaxSources.getValues());
    }

    public void setSpeedMaxSources(final Set<WindSourceType> sources) {
        speedMaxSources.setValues(sources);
    }

    public boolean isSpeedAvgBulk() {
        return speedAvgBulk.getValue();
    }

    public void setSpeedAvgBulk(final boolean bulk) {
        speedAvgBulk.setValue(bulk);
    }

    public boolean isSpeedMinBulk() {
        return speedMinBulk.getValue();
    }

    public void setSpeedMinBulk(final boolean bulk) {
        speedMinBulk.setValue(bulk);
    }

    public boolean isSpeedMaxBulk() {
        return speedMaxBulk.getValue();
    }

    public void setSpeedMaxBulk(final boolean bulk) {
        speedMaxBulk.setValue(bulk);
    }
}
