package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.base.Size;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.racemap.BoatClassVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sse.common.Color;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;

/**
 * A google map overlay based on a HTML5 canvas for drawing boats (images)
 * The boats will be zoomed/scaled according to the current map state and rotated according to the bearing of the boat.
 */
public class BoatOverlay extends CanvasOverlayV3 {

    /** 
     * The boat class
     */
    private final BoatClassDTO boatClass;
    
    /**
     * The current GPS fix used to draw the boat.
     */
    private GPSFixDTOWithSpeedWindTackAndLegType boatFix;

    /** 
     * The rotation angle of the original boat image in degrees
     */
    private static double ORIGINAL_BOAT_IMAGE_ROTATIION_ANGLE = 90.0;

    private int canvasWidth;
    private int canvasHeight;

    private Color color; 

    private Map<Long, Util.Pair<Size, Size>> boatScaleAndSizePerWorldWidthCache; 

    private final BoatClassVectorGraphics boatVectorGraphics;

    private LegType lastLegType;
    private Tack lastTack;
    private Boolean lastSelected;
    private Integer lastWidth;
    private Integer lastHeight;
    private Size lastScale;
    private Color lastColor;
    private DisplayMode lastDisplayMode;
    
    public static enum DisplayMode { DEFAULT, SELECTED, NOT_SELECTED };
    private DisplayMode displayMode;

    public BoatOverlay(final MapWidget map, int zIndex, final BoatDTO boatDTO, Color color, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.boatClass = boatDTO.getBoatClass();
        this.color = color;
        boatScaleAndSizePerWorldWidthCache = new HashMap<>();
        boatVectorGraphics = BoatClassVectorGraphicsResolver.resolveBoatClassVectorGraphics(boatClass.getName());
    }
    
    @Override
    protected void draw() {
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
            if (needToDraw(boatFix.legType, boatFix.tack, isSelected(), canvasWidth, canvasHeight, boatSizeScaleFactor,
                    color, displayMode)) {
                boatVectorGraphics.drawBoatToCanvas(getCanvas().getContext2d(), boatFix.legType, boatFix.tack, getDisplayMode(), 
                        canvasWidth, canvasHeight, boatSizeScaleFactor, color);
                lastLegType = boatFix.legType;
                lastTack = boatFix.tack;
                lastSelected = isSelected();
                lastWidth = canvasWidth;
                lastHeight = canvasHeight;
                lastScale = boatSizeScaleFactor;
                lastColor = color;
                lastDisplayMode = displayMode;
            }
            LatLng latLngPosition = coordinateSystem.toLatLng(boatFix.position);
            Point boatPositionInPx = getMapProjection().fromLatLngToDivPixel(latLngPosition);
            setCanvasPosition(boatPositionInPx.getX() - getCanvas().getCoordinateSpaceWidth() / 2,
                    boatPositionInPx.getY() - getCanvas().getCoordinateSpaceHeight() / 2);
            // now rotate the canvas accordingly
            final double trueHeadingInDegrees = boatFix.optionalTrueHeading != null
                    ? boatFix.optionalTrueHeading.getDegrees()
                    : (boatFix.speedWithBearing == null ? 0 : boatFix.speedWithBearing.bearingInDegrees);
            updateDrawingAngleAndSetCanvasRotation(coordinateSystem.mapDegreeBearing(trueHeadingInDegrees - ORIGINAL_BOAT_IMAGE_ROTATIION_ANGLE));
        }
    }
    
    /**
     * Compares the drawing parameters to {@link #lastLegType} and the other <code>last...</code>. If anything has
     * changed, the result is <code>true</code>.
     */
    private boolean needToDraw(LegType legType, Tack tack, boolean isSelected, double width, double height,
            Size scaleFactor, Color color, DisplayMode displayMode) {
        return lastLegType == null || lastLegType != legType || lastTack == null || lastTack != tack
                || lastSelected == null || lastSelected != isSelected || lastWidth == null || lastWidth != width
                || lastHeight == null || lastHeight != height || lastScale == null || !lastScale.equals(scaleFactor)
                || lastColor == null || !lastColor.equals(color) || lastDisplayMode == null
                || !lastDisplayMode.equals(displayMode);
    }

    public void setBoatFix(GPSFixDTOWithSpeedWindTackAndLegType boatFix, long timeForPositionTransitionMillis) {
        updateTransition(timeForPositionTransitionMillis);
        this.boatFix = boatFix;
    }

    public Util.Pair<Size, Size> getBoatScaleAndSize(BoatClassDTO boatClass) {
        Size boatSizeInPixels = getCorrelatedBoatSize(boatClass.getHullLength(), boatClass.getHullBeam());
        double boatHullScaleFactor = boatSizeInPixels.getWidth() / (boatVectorGraphics.getHullLengthInPx());
        double boatBeamScaleFactor = boatSizeInPixels.getHeight() / (boatVectorGraphics.getBeamInPx());
        // as the canvas contains the whole boat the canvas size relates to the overall length, not the hull length
        double scaledWidthSize = (boatVectorGraphics.getOverallLengthInPx()) * boatHullScaleFactor;
        double scaledBeamSize = (boatVectorGraphics.getOverallLengthInPx()) * boatBeamScaleFactor;
        return new Util.Pair<Size, Size>(Size.newInstance(boatHullScaleFactor, boatBeamScaleFactor),
                Size.newInstance(scaledWidthSize + scaledWidthSize / 2.0, scaledBeamSize + scaledBeamSize / 2.0));
    }

    private Size getCorrelatedBoatSize(Distance hullLength, Distance hullBeam) {
        Size boatSizeInPixel = calculateBoundingBox(getMapProjection(), boatFix.position, hullLength, hullBeam);
        changeBoatSizeIfTooShortHull(boatSizeInPixel, hullLength, hullBeam);
        changeBoatSizeIfTooNarrowBeam(boatSizeInPixel, hullLength, hullBeam);
        return boatSizeInPixel;
    }

    private void changeBoatSizeIfTooShortHull(Size boatSizeInPixel, Distance hullLength, Distance hullBeam) {
        // the minimum boat length is related to the hull of the boat, not the overall length
        double minBoatHullLengthInPx = boatVectorGraphics.getMinHullLengthInPx();
        if (boatSizeInPixel.getWidth() < minBoatHullLengthInPx) {
            double ratioBeamHullLength = hullBeam.divide(hullLength);
            boatSizeInPixel.setHeight(minBoatHullLengthInPx * ratioBeamHullLength);
            boatSizeInPixel.setWidth(minBoatHullLengthInPx);
        }
    }

    private void changeBoatSizeIfTooNarrowBeam(Size boatSizeInPixel, Distance hullLength, Distance hullBeam) {
        // if the boat gets too narrow, use the minimum beam and scale the hull length according to aspect
        double minBoatBeamInPx = boatVectorGraphics.getMinBeamInPx();
        if (boatSizeInPixel.getHeight() < minBoatBeamInPx) {
            double ratioHullBeamLength = hullLength.divide(hullBeam);
            boatSizeInPixel.setWidth(minBoatBeamInPx * ratioHullBeamLength);
            boatSizeInPixel.setHeight(minBoatBeamInPx);
        }
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

}
