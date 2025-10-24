package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.base.Size;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.ui.shared.SpeedWithBearingDTO;
import com.sap.sailing.gwt.ui.shared.racemap.RowingBoatClassVectorGraphics;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;

/**
 * A google map overlay based on a HTML5 canvas for drawing rowing boats (images)
 * The rowing boats will be zoomed/scaled according to the current map state and rotated according to the bearing of the rowing boat.
 */
public class RowingBoatOverlay extends BoatOverlay {

    private final RowingBoatClassVectorGraphics rowingBoatVectorGraphics;

    private Integer lastWidth;
    private Integer lastHeight;
    private boolean isPullingOars;

    public RowingBoatOverlay(final MapWidget map, int zIndex, final CompetitorDTO competitorDTO, Color color, CoordinateSystem coordinateSystem) {
        super(map, zIndex, competitorDTO.hasBoat() ? ((CompetitorWithBoatDTO) competitorDTO).getBoat() : null, color, coordinateSystem);
        rowingBoatVectorGraphics = RowingBoatClassVectorGraphicsResolver.resolveRowingBoatClassVectorGraphics(boatClass.getName());
    }
    
    @Override
    public void draw() {
        if (getMapProjection() != null && boatFix != null) {
            // the possible zoom level range is 0 to 21 (zoom level 0 would show the whole world)
            final long worldWidth = (long) getMapProjection().getWorldWidth();
            final Util.Pair<Size, Size> boatScaleAndSize = boatScaleAndSizePerWorldWidthCache.computeIfAbsent(worldWidth, z->getBoatScaleAndSize(boatClass));
            final Size boatSizeScaleFactor = boatScaleAndSize.getA();
            canvasWidth = (int) (boatScaleAndSize.getB().getWidth());
            canvasHeight = (int) (boatScaleAndSize.getB().getHeight());
            if (lastWidth == null || canvasWidth != lastWidth || lastHeight == null || canvasHeight != lastHeight) {
                setCanvasSize(canvasWidth, canvasHeight);
            }
            isPullingOars = !isPullingOars;
            rowingBoatVectorGraphics.drawRowingBoatToCanvas(getCanvas().getContext2d(), isPullingOars, isSelected(), 
                    canvasWidth, canvasHeight, boatSizeScaleFactor, color);
            
            LatLng latLngPosition = coordinateSystem.toLatLng(boatFix.position);
            Point boatPositionInPx = getMapProjection().fromLatLngToDivPixel(latLngPosition);
            setCanvasPosition(boatPositionInPx.getX() - getCanvas().getCoordinateSpaceWidth() / 2,
                    boatPositionInPx.getY() - getCanvas().getCoordinateSpaceHeight() / 2);
            // now rotate the canvas accordingly
            SpeedWithBearingDTO speedWithBearing = boatFix.speedWithBearing;
            if (speedWithBearing == null) {
                speedWithBearing = new SpeedWithBearingDTO(0, 0);
            }
            updateDrawingAngleAndSetCanvasRotation(coordinateSystem.mapDegreeBearing(speedWithBearing.bearingInDegrees - ORIGINAL_BOAT_IMAGE_ROTATIION_ANGLE));
        }
    }
}
