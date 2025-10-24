package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.Collections;

import com.sap.sailing.domain.common.NonCardinalBounds;
import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.EnumSetSetting;

/**
 * @author Lennart Hensler (D054527)
 */
public class RaceMapZoomSettings extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = 7283052942434130497L;

    /**
     * The auto-zoom types for a {@link RaceMap}.<br />
     * Each zoom type has a {@link NonCardinalBoundsCalculator}, which calculates the new bounds for a map.
     */
    public enum ZoomTypes {
        NONE(null), WINDSENSORS(new RaceMap.WindSensorsBoundsCalculator()), BOATS(new RaceMap.BoatsBoundsCalculator()), 
        TAILS(new RaceMap.TailsBoundsCalculator()), BUOYS(new RaceMap.CourseMarksBoundsCalculator());

        private NonCardinalBoundsCalculator calculator;

        private ZoomTypes(NonCardinalBoundsCalculator calculator) {
            this.calculator = calculator;
        }

        public NonCardinalBounds calculateNewBounds(RaceMap forMap) {
            return calculator == null ? null : calculator.calculateNewBounds(forMap);
        }
    };

    private EnumSetSetting<ZoomTypes> typesToConsiderOnZoom;
    private BooleanSetting zoomToSelectedCompetitors;
    
    @Override
    protected void addChildSettings() {
        typesToConsiderOnZoom = new EnumSetSetting<>("typesToConsiderOnZoom", this, Collections.singleton(ZoomTypes.BUOYS), ZoomTypes::valueOf);
        zoomToSelectedCompetitors = new BooleanSetting("zoomToSelectedCompetitors", this, false);
    }
    
    /**
     * Creates default RaceMapZoomSettings with the {@link ZoomTypes} <code>BUOYS</code>.<br />
     * The attribute <code>zoomToSelectedCompetitors</code> will be <code>false</code>.
     */
    public RaceMapZoomSettings() {
        super();
    }

    /**
     * Creates default RaceMapZoomSettings with the {@link ZoomTypes} <code>BUOYS</code>.<br />
     * The attribute <code>zoomToSelectedCompetitors</code> will be <code>false</code>.
     */
    public RaceMapZoomSettings(String propertyName, AbstractGenericSerializableSettings parentSettings) {
        super(propertyName, parentSettings);
    }
    
    public RaceMapZoomSettings(Iterable<ZoomTypes> typesToConsider, boolean zoomToSelected) {
        this();
        this.typesToConsiderOnZoom.setValues(typesToConsider);
        this.zoomToSelectedCompetitors.setValue(zoomToSelected);
    }
    
    protected void init(RaceMapZoomSettings settings) {
        if (settings != null) {
            this.typesToConsiderOnZoom.setValues(settings.getTypesToConsiderOnZoom());
            this.zoomToSelectedCompetitors.setValue(settings.isZoomToSelectedCompetitors());
        }
    }
    
    public NonCardinalBounds getNewBounds(RaceMap forMap) {
        NonCardinalBounds newBounds = null;
        if (typesToConsiderOnZoom != null) {
            for (final ZoomTypes type : typesToConsiderOnZoom.getValues()) {
                // Calculate the new bounds and extend the result
                final NonCardinalBounds calculatedBounds = type.calculateNewBounds(forMap);
                if (calculatedBounds != null) {
                    if (newBounds == null) {
                        newBounds = calculatedBounds;
                    } else {
                        newBounds = newBounds.extend(calculatedBounds);
                    }
                }
            }
        }
        return newBounds;
    }
    
    public Iterable<ZoomTypes> getTypesToConsiderOnZoom() {
        return typesToConsiderOnZoom.getValues();
    }
    
    public boolean isZoomToSelectedCompetitors() {
        return zoomToSelectedCompetitors.getValue();
    }

    public boolean containsZoomType(ZoomTypes zoomType) {
        return Util.contains(typesToConsiderOnZoom.getValues(), zoomType);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((typesToConsiderOnZoom == null) ? 0 : typesToConsiderOnZoom.hashCode());
        result = prime * result + (zoomToSelectedCompetitors.getValue() ? 1231 : 1237);
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
        RaceMapZoomSettings other = (RaceMapZoomSettings) obj;
        if (typesToConsiderOnZoom == null) {
            if (other.typesToConsiderOnZoom != null)
                return false;
        } else if (!typesToConsiderOnZoom.equals(other.typesToConsiderOnZoom))
            return false;
        if (zoomToSelectedCompetitors != other.zoomToSelectedCompetitors)
            return false;
        return true;
    }
}