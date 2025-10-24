package com.sap.sailing.gwt.autoplay.client.nodes.base;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;

public abstract class FiresPlaceNode extends AutoPlayNodeBase {
    private Place placeToGo;

    public FiresPlaceNode(String name) {
        super(name);
    }
    
    public void onStart() {
        firePlaceChangeAndStartTimer();
    }

    public void setPlaceToGo(Place placeToGo) {
        this.placeToGo = placeToGo;
    }

    protected Place getPlaceToGo() {
        return placeToGo;
    }

    protected void firePlaceChangeAndStartTimer() {
        if (placeToGo != null) {
            getBus().fireEvent(new PlaceChangeEvent(placeToGo));
        }
    }
}