package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.HashSet;
import java.util.Set;

import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.EnumSetSetting;

public class RaceMapHelpLinesSettings extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = -3155593082712145485L;

    /**
     * Types of help lines on the map
     */
    public enum HelpLineTypes {
        STARTLINE, FINISHLINE, ADVANTAGELINE, COURSEMIDDLELINE, BUOYZONE, BOATTAILS, STARTLINETOFIRSTMARKTRIANGLE, COURSEGEOMETRY, COURSEAREACIRCLES
    }
    
    private EnumSetSetting<HelpLineTypes> visibleHelpLines;
    
    @Override
    protected void addChildSettings() {
        Set<HelpLineTypes> defaultVisibleHelpLines = new HashSet<HelpLineTypes>();
        defaultVisibleHelpLines.add(HelpLineTypes.STARTLINE);
        defaultVisibleHelpLines.add(HelpLineTypes.FINISHLINE);
        defaultVisibleHelpLines.add(HelpLineTypes.ADVANTAGELINE);
        defaultVisibleHelpLines.add(HelpLineTypes.BOATTAILS);
        
        visibleHelpLines = new EnumSetSetting<>("visibleHelpLines", this, defaultVisibleHelpLines, HelpLineTypes::valueOf);
    }

    /**
     * Creates new RaceMapHelpLinesSettings with the {@link HelpLineTypes} <code>STARTLINE</code>,
     * <code>FINISHLINE</code> and <code>ADVANTAGELINE</code>.<br />
     */
    public RaceMapHelpLinesSettings() {
        super();
    }
    
    /**
     * Creates new RaceMapHelpLinesSettings with the {@link HelpLineTypes} <code>STARTLINE</code>,
     * <code>FINISHLINE</code> and <code>ADVANTAGELINE</code>.<br />
     */
    public RaceMapHelpLinesSettings(String propertyName, AbstractGenericSerializableSettings parentSettings) {
        super(propertyName, parentSettings);
    }
    
    /**
     * Creates new RaceMapHelpLinesSettings with the {@link HelpLineTypes} <code>STARTLINE</code>,
     * <code>FINISHLINE</code> and <code>ADVANTAGELINE</code>.<br />
     */
     protected void init(RaceMapHelpLinesSettings settings) {
        if (settings != null) {
            this.visibleHelpLines.setValues(settings.getVisibleHelpLineTypes());
        }
     }
    
    public RaceMapHelpLinesSettings(Iterable<HelpLineTypes> visibleHelpLines) {
        this();
        this.visibleHelpLines.setValues(visibleHelpLines);
    }

    public boolean isVisible(HelpLineTypes helpLineType) {
        return Util.contains(visibleHelpLines.getValues(), helpLineType);
    }

    public Iterable<HelpLineTypes> getVisibleHelpLineTypes() {
        return visibleHelpLines.getValues();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((visibleHelpLines == null) ? 0 : visibleHelpLines.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RaceMapHelpLinesSettings other = (RaceMapHelpLinesSettings) obj;
        if (visibleHelpLines == null) {
            if (other.visibleHelpLines != null)
                return false;
        } else if (!visibleHelpLines.equals(other.visibleHelpLines))
            return false;
        return true;
    }

    public boolean isShowAnyHelperLines() {
        return !Util.isEmpty(visibleHelpLines.getValues());
    }
}