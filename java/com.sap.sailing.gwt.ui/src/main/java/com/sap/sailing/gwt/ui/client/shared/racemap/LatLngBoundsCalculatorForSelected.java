package com.sap.sailing.gwt.ui.client.shared.racemap;


public abstract class LatLngBoundsCalculatorForSelected implements PixelBoundsCalculator {

    public boolean isZoomOnlyToSelectedCompetitors(RaceMap forMap) {
        return forMap.getSettings().getZoomSettings().isZoomToSelectedCompetitors();
    }

}
