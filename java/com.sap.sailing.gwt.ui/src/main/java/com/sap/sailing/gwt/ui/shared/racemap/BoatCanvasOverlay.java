package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.geom.Size;
import com.google.gwt.maps.client.overlay.Overlay;
import com.sap.sailing.gwt.ui.shared.CompetitorDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;

/**
 * A google map overlay based on a HTML5 canvas for drawing boats (images)
 * The boats will be zoomed/scaled according to the current map state and rotated according to the bearing of the boat.
 */
public class BoatCanvasOverlay extends CanvasOverlay {

    /**
     * The competitor the boat belongs too.
     */
    private final CompetitorDTO competitorDTO;

    /**
     * The current GPS fix used to draw the boat.
     */
    private GPSFixDTO boatFix;

    private final RaceMapImageManager raceMapImageManager;

    public BoatCanvasOverlay(CompetitorDTO competitorDTO, RaceMapImageManager raceMapImageManager) {
        super();
        this.competitorDTO = competitorDTO;
        this.raceMapImageManager = raceMapImageManager;
    }

    @Override
    protected Overlay copy() {
        return new BoatCanvasOverlay(competitorDTO, raceMapImageManager);
    }

    @Override
    protected void redraw(boolean force) {
        if (boatFix != null) {
            ImageTransformer boatImageTransformer = raceMapImageManager.getBoatImageTransformer(boatFix, isSelected());
            double realBoatSizeScaleFactor = getRealBoatSizeScaleFactor(boatImageTransformer.getImageSize());
            boatImageTransformer.drawToCanvas(getCanvas(), boatFix.speedWithBearing.bearingInDegrees, realBoatSizeScaleFactor);
            LatLng latLngPosition = LatLng.newInstance(boatFix.position.latDeg, boatFix.position.lngDeg);
            Point boatPositionInPx = getMap().convertLatLngToDivPixel(latLngPosition);
            getPane().setWidgetPosition(getCanvas(), boatPositionInPx.getX() - getCanvas().getCoordinateSpaceWidth() / 2, boatPositionInPx.getY()
                    - getCanvas().getCoordinateSpaceHeight() / 2);
        }
    }

    public GPSFixDTO getBoatFix() {
        return boatFix;
    }

    public void setBoatFix(GPSFixDTO boatFix) {
        this.boatFix = boatFix;
    }
    
    public double getRealBoatSizeScaleFactor(Size imageSize) {
        // the possible zoom level range is 0 to 21 (zoom level 0 would show the whole world)
        int zoomLevel = map == null ? 1 : map.getZoomLevel();
        double minScaleFactor = 0.45;
        double maxScaleFactor = 2.0;
        double realBoatSizeScaleFactor = minScaleFactor;
        // here it would be better to get the boat length from the boat class -> for now we assume a length of 5m 
        double boatLengthInMeter = 5.0;
        // to scale the boats to a realistic size we need the length of the boat in pixel, 
        // but it does not work to just take the image size, because the images for the different boat states can be different
        int boatLengthInPixel = 50;
        if (zoomLevel > 5) {
            LatLngBounds bounds = map.getBounds();
            if (bounds != null) {
                LatLng upperRight = bounds.getNorthEast();
                LatLng bottomLeft = bounds.getSouthWest();
                LatLng upperLeft = LatLng.newInstance(upperRight.getLatitude(), bottomLeft.getLongitude());
                double distXInMeters = upperLeft.distanceFrom(upperRight);
                int widthInPixel = map.getSize().getWidth();
                double realBoatSizeInPixel  = (widthInPixel * boatLengthInMeter) / distXInMeters;
                realBoatSizeScaleFactor = realBoatSizeInPixel / (double) boatLengthInPixel;
                if (realBoatSizeScaleFactor < minScaleFactor) {
                    realBoatSizeScaleFactor = minScaleFactor;
                }
                if (realBoatSizeScaleFactor > maxScaleFactor) {
                    realBoatSizeScaleFactor = maxScaleFactor;
                }
            }
        }
        return realBoatSizeScaleFactor;
    }
}
