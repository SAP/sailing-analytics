package com.sap.sailing.gwt.ui.client.shared.racemap;

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
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MeterDistance;

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

    // MapCanvasProjection exposes points but no local transform, so nearby probes approximate its affine basis.
    // Keep this distance short: long probes sample perspective curvature and visibly distort boat proportions.
    private static final Distance SCREEN_HEADING_PROBE_DISTANCE = new MeterDistance(1);

    private int canvasWidth;
    private int canvasHeight;

    private Color color; 

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
        getCanvas().getElement().setAttribute("data-map-oriented", "true");
        boatVectorGraphics = BoatClassVectorGraphicsResolver.resolveBoatClassVectorGraphics(boatClass.getName());
    }
    
    @Override
    protected void draw() {
        if (getMapProjection() != null && boatFix != null) {
            final LatLng latLngPosition = coordinateSystem.toLatLng(boatFix.position);
            final Point boatPositionInPx = getMapProjection().fromLatLngToDivPixel(latLngPosition);
            final double trueHeadingInDegrees = boatFix.optionalTrueHeading != null
                    ? boatFix.optionalTrueHeading.getDegrees()
                    : (boatFix.speedWithBearing == null ? 0 : boatFix.speedWithBearing.bearingInDegrees);
            final Point headingReferenceInPx = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(
                    boatFix.position.translateRhumb(new DegreeBearingImpl(trueHeadingInDegrees), SCREEN_HEADING_PROBE_DISTANCE)));
            final Point starboardReferenceInPx = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(
                    boatFix.position.translateRhumb(new DegreeBearingImpl(trueHeadingInDegrees + 90), SCREEN_HEADING_PROBE_DISTANCE)));
            final double forwardX = headingReferenceInPx.getX() - boatPositionInPx.getX();
            final double forwardY = headingReferenceInPx.getY() - boatPositionInPx.getY();
            final double starboardX = starboardReferenceInPx.getX() - boatPositionInPx.getX();
            final double starboardY = starboardReferenceInPx.getY() - boatPositionInPx.getY();
            final double sumOfSquares = forwardX * forwardX + forwardY * forwardY +
                    starboardX * starboardX + starboardY * starboardY;
            final double determinant = forwardX * starboardY - forwardY * starboardX;
            final double normalization = Math.sqrt((sumOfSquares + Math.sqrt(Math.max(0,
                    sumOfSquares * sumOfSquares - 4 * determinant * determinant))) / 2);
            if (normalization > 0.000001) {
                final Util.Pair<Size, Size> boatScaleAndSize = getBoatScaleAndSize(boatClass, normalization);
                final Size boatSizeScaleFactor = boatScaleAndSize.getA();
                canvasWidth = (int) boatScaleAndSize.getB().getWidth();
                canvasHeight = (int) boatScaleAndSize.getB().getHeight();
                if (lastWidth == null || canvasWidth != lastWidth || lastHeight == null || canvasHeight != lastHeight) {
                    setCanvasSize(canvasWidth, canvasHeight);
                }
                if (needToDraw(boatFix.legType, boatFix.tack, isSelected(), canvasWidth, canvasHeight,
                        boatSizeScaleFactor, color, displayMode)) {
                    boatVectorGraphics.drawBoatToCanvas(getCanvas().getContext2d(), boatFix.legType, boatFix.tack,
                            getDisplayMode(), canvasWidth, canvasHeight, boatSizeScaleFactor, color);
                    lastLegType = boatFix.legType;
                    lastTack = boatFix.tack;
                    lastSelected = isSelected();
                    lastWidth = canvasWidth;
                    lastHeight = canvasHeight;
                    lastScale = boatSizeScaleFactor;
                    lastColor = color;
                    lastDisplayMode = displayMode;
                }
                updateDrawingMatrixAndSetCanvasTransform(forwardX / normalization, forwardY / normalization,
                        starboardX / normalization, starboardY / normalization);
            } else {
                final double screenDrawingAngle = Math.toDegrees(Math.atan2(forwardY, forwardX));
                updateDrawingAngleAndSetCanvasRotation(screenDrawingAngle);
            }
            setCanvasPosition(boatPositionInPx.getX() - getCanvas().getCoordinateSpaceWidth() / 2,
                    boatPositionInPx.getY() - getCanvas().getCoordinateSpaceHeight() / 2);
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

    public Util.Pair<Size, Size> getBoatScaleAndSize(BoatClassDTO boatClass, double pixelsPerMeter) {
        final double naturalHullLengthInPixels = boatClass.getHullLength().getMeters() * pixelsPerMeter;
        final double naturalBeamInPixels = boatClass.getHullBeam().getMeters() * pixelsPerMeter;
        final double minimumMultiplier = Math.max(1.0, Math.max(
                boatVectorGraphics.getMinHullLengthInPx() / naturalHullLengthInPixels,
                boatVectorGraphics.getMinBeamInPx() / naturalBeamInPixels));
        final double boatHullScaleFactor = naturalHullLengthInPixels * minimumMultiplier /
                boatVectorGraphics.getHullLengthInPx();
        final double boatBeamScaleFactor = naturalBeamInPixels * minimumMultiplier /
                boatVectorGraphics.getBeamInPx();
        final double scaledWidthSize = boatVectorGraphics.getOverallLengthInPx() * boatHullScaleFactor;
        final double scaledBeamSize = boatVectorGraphics.getOverallLengthInPx() * boatBeamScaleFactor;
        return new Util.Pair<Size, Size>(Size.newInstance(boatHullScaleFactor, boatBeamScaleFactor),
                Size.newInstance(scaledWidthSize * 1.5, scaledBeamSize * 1.5));
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

}
