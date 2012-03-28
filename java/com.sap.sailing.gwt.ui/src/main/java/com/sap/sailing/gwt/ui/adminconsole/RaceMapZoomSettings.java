package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.maps.client.geom.LatLngBounds;

public class RaceMapZoomSettings {
    
    /**
     * The auto-zoom types for a {@link RaceMap}.<br />
     * Each zoom type has a {@link LatLngBoundsCalculator}, which calculates the new bounds for a map.
     * @author Lennart Hensler (D054527)
     *
     */
    public enum ZoomTypes {
        NONE(null), WINDSENSORS(new RaceMap.WindSensorsBoundsCalculater()), BOATS(new RaceMap.BoatsBoundsCalculater()), 
        TAILS(new RaceMap.TailsBoundsCalculater()), BUOYS(new RaceMap.BuoysBoundsCalculater());

        private LatLngBoundsCalculator calculater;

        private ZoomTypes(LatLngBoundsCalculator calculator) {
            this.calculater = calculator;
        }

        public LatLngBounds calculateNewBounds(RaceMap forMap) {
            return calculater.calculateNewBounds(forMap);
        }
    };
    
    private ArrayList<ZoomTypes> typesToConsiderOnZoom;
    private boolean zoomToSelectedCompetitors;

    /**
     * Creates new RaceMapZoomSettings with the {@link ZoomTypes} <code>BOATS</code> and <code>TAILS</code>.<br />
     * The attritbute <code>zoomToSelectedCompetitors</code> will be <code>false</code>.
     */
    public RaceMapZoomSettings() {
        typesToConsiderOnZoom = new ArrayList<ZoomTypes>();
        typesToConsiderOnZoom.add(ZoomTypes.BOATS);
        typesToConsiderOnZoom.add(ZoomTypes.TAILS);
        typesToConsiderOnZoom.add(ZoomTypes.BUOYS);
        //TODO reenable after disconnecting wind data from races
//        typesToConsiderOnZoom.add(ZoomTypes.WINDSENSORS);
        zoomToSelectedCompetitors = false;
    }
    
    public RaceMapZoomSettings(ArrayList<ZoomTypes> typesToConsiderOnZoom, boolean zoomToSelected) {
        this.typesToConsiderOnZoom = typesToConsiderOnZoom;
        this.zoomToSelectedCompetitors = zoomToSelected;
    }

    public LatLngBounds getNewBounds(RaceMap forMap) {
        LatLngBounds newBounds = null;
        if (typesToConsiderOnZoom != null) {
            for (ZoomTypes type : typesToConsiderOnZoom) {
                //Calculate the new bounds and extend the result
                LatLngBounds calculatedBounds = type.calculateNewBounds(forMap);
                if (calculatedBounds != null) {
                    if (newBounds == null) {
                        newBounds = calculatedBounds;
                    } else {
                        newBounds.extend(calculatedBounds.getNorthEast());
                        newBounds.extend(calculatedBounds.getSouthWest());
                    }
                }
            }
        }
        return newBounds;
    }
    
    public List<ZoomTypes> getTypesToConsiderOnZoom() {
        return typesToConsiderOnZoom;
    }
    
    public void setTypesToConsiderOnZoom(List<ZoomTypes> typesToConsiderOnZoom) {
        this.typesToConsiderOnZoom = new ArrayList<ZoomTypes>(typesToConsiderOnZoom);
    }
    
    public void setZoomToSelectedCompetitors(boolean zoomToSelectedCompetitors) {
        this.zoomToSelectedCompetitors = zoomToSelectedCompetitors;
    }
    
    public boolean isZoomToSelectedCompetitors() {
        return zoomToSelectedCompetitors;
    }

    public boolean contains(ZoomTypes zoomType) {
        return typesToConsiderOnZoom.contains(zoomType);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((typesToConsiderOnZoom == null) ? 0 : typesToConsiderOnZoom.hashCode());
        result = prime * result + (zoomToSelectedCompetitors ? 1231 : 1237);
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