package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;

public class RaceMapHelpLinesSettings extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = -3155593082712145485L;

    /**
     * Types of help lines on the map
     */
    public enum HelpLineTypes {
        STARTLINE, FINISHLINE, ADVANTAGELINE, COURSEMIDDLELINE, BUOYZONE, BOATTAILS, STARTLINETOFIRSTMARKTRIANGLE, COURSEGEOMETRY
    }
    
    private final Set<HelpLineTypes> visibleHelpLines;
    
    @Override
    protected void addChildSettings() {
        // TODO Auto-generated method stub
    }

    /**
     * Creates new RaceMapHelpLinesSettings with the {@link HelpLineTypes} <code>STARTLINE</code>,
     * <code>FINISHLINE</code> and <code>ADVANTAGELINE</code>.<br />
     */
    public RaceMapHelpLinesSettings() {
        visibleHelpLines = new HashSet<HelpLineTypes>();
        visibleHelpLines.add(HelpLineTypes.STARTLINE);
        visibleHelpLines.add(HelpLineTypes.FINISHLINE);
        visibleHelpLines.add(HelpLineTypes.ADVANTAGELINE);
        visibleHelpLines.add(HelpLineTypes.BOATTAILS);
    }
    
    public RaceMapHelpLinesSettings(Iterable<HelpLineTypes> visibleHelpLines) {
        this.visibleHelpLines = new HashSet<>();
        Util.addAll(visibleHelpLines, this.visibleHelpLines);
    }

    public boolean isVisible(HelpLineTypes helpLineType) {
        return visibleHelpLines.contains(helpLineType);
    }

    public Iterable<HelpLineTypes> getVisibleHelpLineTypes() {
        return Collections.unmodifiableCollection(visibleHelpLines);
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
        return !visibleHelpLines.isEmpty();
    }
}