package com.sap.sailing.gwt.ui.client.shared.racemap.windladder;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.Repetition;
import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewMethods;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnAddHandler;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;

/**
 * <p>The visible overlay used by {@link WindLadder} to draw the actual wind ladder to screen.</p>
 * <p>The image texture used for the pattern is loaded from {@link WindLadderResources} by
 * {@link ImageTileGenerator}.</p>
 * @author Tim Hessenmüller (D062243)
 */
public class WindLadderOverlay extends FullCanvasOverlay {
    protected static final WindLadderResources RESOURCES = GWT.create(WindLadderResources.class);
    protected static final double TEXTURE_ALPHA = 0.4d;
    protected static final double CANVAS_RESERVE = 0.5;

    protected WindLadder windLadder;

    protected ImageTileGenerator tileGen = new ImageTileGenerator(RESOURCES.windLadderTexture(), ()->{ redraw(); draw(); });

    protected Double windBearingRadians;
    protected Position fixPosition;

    protected Double drawnPatternSize;

    protected double previousFixPointWindwardDistance;
    protected double previousOnAxisOffset;

    protected boolean redraw = true;
    protected int transitionDisableCountdown;
    private final String name;

    public WindLadderOverlay(WindLadder windLadder, MapWidget map, int zIndex, CoordinateSystem coordinateSystem, String name) {
        super(map, zIndex, coordinateSystem);
        this.map.addZoomChangeHandler(event -> this.onZoomChange());
        this.windLadder = windLadder;
        getCanvas().getElement().setId("wind-ladder-display-"+name);
        this.name = name;
    }

    public boolean update(Double windBearingRadians, Position fixPosition, long timeForPositionTransitionMillis) {
        return this.update(windBearingRadians, fixPosition, timeForPositionTransitionMillis, false);
    }

    /**
     * @return {@code true} if this canvas can perform the requested update
     */
    public boolean update(Double windBearingRadians, Position fixPosition, long timeForPositionTransitionMillis, boolean redraw) {
        boolean canAnimate = true;
        if (windBearingRadians != null) {
            this.windBearingRadians = windBearingRadians;
        }
        if (fixPosition != null ) {
            this.fixPosition = fixPosition;
        }
        if (getMapProjection() != null && this.windBearingRadians != null && this.fixPosition != null && tileGen.getReady()) {
            // Rotation
            updateDrawingAngleAndSetCanvasRotation(Math.toDegrees(this.windBearingRadians));
            // Offset from centered position
            Point fixPointInMap = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(this.fixPosition));
            Point windUnitVector = Point.newInstance(-Math.sin(-this.windBearingRadians), -Math.cos(-this.windBearingRadians));
            // Dot product of the two vectors above
            final double fixPointWindwardDistance = fixPointInMap.getX() * windUnitVector.getX() + fixPointInMap.getY() * windUnitVector.getY();
            final double fixPointWindwardDistanceChange = fixPointWindwardDistance - previousFixPointWindwardDistance;
            previousFixPointWindwardDistance = fixPointWindwardDistance;
            double onAxisOffset;
            if (redraw) {
                onAxisOffset = fixPointWindwardDistance % drawnPatternSize;
                onAxisOffset = onAxisOffset < 0.0 ? onAxisOffset + drawnPatternSize : onAxisOffset; // Full modulus instead of remainder
                onAxisOffset = onAxisOffset > drawnPatternSize / 2.0 ? onAxisOffset - drawnPatternSize : onAxisOffset; // Center around 0
            } else {
                onAxisOffset = previousOnAxisOffset + fixPointWindwardDistanceChange;
            }
            previousOnAxisOffset = onAxisOffset;
            Point offsetVector = Point.newInstance(onAxisOffset * windUnitVector.getX(), onAxisOffset * windUnitVector.getY());
            canAnimate = isInBounds(this.windBearingRadians, offsetVector);
            setCanvasPosition(getWidgetPosLeft() + offsetVector.getX(), getWidgetPosTop() + offsetVector.getY());
            if (transitionDisableCountdown > 0) {
                transitionDisableCountdown -= 1;
            } else {
                updateTransition(timeForPositionTransitionMillis);
            }
            if (redraw) {
                redraw();
            }
            draw();
        }
        return canAnimate;
    }

    protected boolean isInBounds(double rotation, Point translation) {
        // The rectangular canvas will serve as our reference frame which has the viewport/map usually located within
        // the bounds of the canvas. 0, 0 will be the center of the canvas.
        final double outerHalfWidth = getCanvas().getElement().getClientWidth() / 2.0;
        final double outerHalfHeight = getCanvas().getElement().getClientHeight() / 2.0;
        final double innerHalfWidth = getMapWidth() / 2.0;
        final double innerHalfHeight = getMapHeight() / 2.0;
        Point[] innerCorners = new Point[4];
        innerCorners[0] = Point.newInstance(-innerHalfWidth, -innerHalfHeight); // TL
        innerCorners[1] = Point.newInstance(innerHalfWidth, -innerHalfHeight); // TR
        innerCorners[2] = Point.newInstance(-innerHalfWidth, innerHalfHeight); // BL
        innerCorners[3] = Point.newInstance(innerHalfWidth, innerHalfHeight); // BR
        final double s = Math.sin(rotation);
        final double c = Math.cos(rotation);
        for (Point corner : innerCorners) {
            double x = corner.getX();
            double y = corner.getY();
            // Rotate around center of canvas (clockwise)
            double xn = x * c + y * s;
            double yn = -x * s + y * c;
            // Translate
            xn -= translation.getX();
            yn -= translation.getY();
            // Check bounds
            boolean inXBounds = -outerHalfWidth < xn && xn < outerHalfWidth;
            boolean inYBounds = -outerHalfHeight < yn && yn < outerHalfHeight;
            if (!inXBounds || !inYBounds) {
                return false;
            }
        }
        return true;
    }

    protected void redraw() {
        redraw = true;
    }

    private double calculatePatternScale(int patternSize) {
        Position pos1 = coordinateSystem.getPosition(getMapProjection().fromDivPixelToLatLng(Point.newInstance(0, 0)));
        Position pos2 = coordinateSystem
                .getPosition(getMapProjection().fromDivPixelToLatLng(Point.newInstance(patternSize, 0)));
        final double patternSizeMeters = pos1.getDistance(pos2).getMeters();
        //final double pixelsPerMeter = patternSize / patternSizeMeters;
        //TODO Use multiple of boat size instead?

        // Changes at what zoom levels we will jump to the next length
        // (increase when zooming out tends to generate patterns that are too small)
        final double mult = 1.8; 
        // Gets the nearest power of 10
        double wantedLength = Math.pow(10.0, Math.round(Math.log10(patternSizeMeters * mult)));
        wantedLength = Math.max(wantedLength, 10.0); // Limit to sizes >= 10 m
        wantedLength *= 2.0; // The complete pattern is made up of 2 bars (one visible, one not)
        double scale = wantedLength / patternSizeMeters;
        return scale;
    }

    @Override
    protected void draw() {
        if (redraw) {
            updateTransition(0);
            setCanvasSettings();
            drawToCanvas();
            redraw = false;
            transitionDisableCountdown = 3;
        }
    }

    /**
     * Resets the canvas into a neutral position and rotation
     */
    @Override
    public void setCanvasSettings() {
        mapWidth = getMap().getDiv().getClientWidth();
        mapHeight = getMap().getDiv().getClientHeight();
        int size = Math.max(mapWidth, mapHeight);
        int reserve = (int) (size * CANVAS_RESERVE);
        int sizeWithReserve = size + reserve;
        canvas.setWidth(String.valueOf(sizeWithReserve));
        canvas.setHeight(String.valueOf(sizeWithReserve));
        canvas.setCoordinateSpaceWidth(sizeWithReserve);
        canvas.setCoordinateSpaceHeight(sizeWithReserve);
        int widthReserve = sizeWithReserve - mapWidth;
        int heightReserve = sizeWithReserve - mapHeight;
        if (getMapProjection() != null) {
            final Point upperLeftCorner = getMapProjection().fromLatLngToDivPixel(getMapProjection().fromContainerPixelToLatLng(Point.newInstance(0, 0)));
            setWidgetPosLeft(Math.round(upperLeftCorner.getX()) - widthReserve / 2);
            setWidgetPosTop(Math.round(upperLeftCorner.getY()) - heightReserve / 2);
            setCanvasPosition(getWidgetPosLeft(), getWidgetPosTop());
        }
        updateDrawingAngleAndSetCanvasRotation(windBearingRadians==null?0.0:Math.toDegrees(windBearingRadians));
        redraw();
    }

    /**
     * Draws a grid pattern onto the canvas by using the image from {@link #tileGen} at a scale given by
     * {@link #calculatePatternScale}.
     */
    protected void drawToCanvas() {
        // Prepare canvas
        final int canvasWidth = canvas.getCoordinateSpaceWidth();
        final int canvasHeight = canvas.getCoordinateSpaceHeight();
        final int tileSize = 16; // tileGen.getHeight(); //TODO returns a wrong number at startup
        final double patternScale = calculatePatternScale(tileSize);
        Context2d ctx = canvas.getContext2d();
        ctx.clearRect(0, 0, canvasWidth, canvasHeight);
        // Change composite mode
        ctx.save();
        ctx.setGlobalAlpha(TEXTURE_ALPHA);
        //ctx.setGlobalCompositeOperation(Composite.SOURCE_IN);
        // Prepare pattern texture
        ctx.setFillStyle(ctx.createPattern(tileGen.getTile(), Repetition.REPEAT));
        ctx.rect(0, 0, canvasWidth, canvasHeight);
        ctx.translate(canvasWidth / 2, canvasHeight / 2);
        ctx.scale(patternScale, patternScale);
        // Draw pattern onto mask
        ctx.fill();
//        // DEBUG Draw a debug square in the center of the canvas
//        ctx.restore();
//        ctx.beginPath();
//        ctx.translate(canvasWidth / 2, canvasHeight / 2);
//        ctx.rect(-2, -2, 4, 4);
//        ctx.setFillStyle("red");
//        ctx.fill();
        drawnPatternSize = tileSize * patternScale;
    }

    @Override
    public void onResize() {
        windLadder.forceSwap();
        super.onResize();
    }

    @Override
    protected void drawCenterChanged() {
        windLadder.swap();
        update(null, null, -1);
    }

    protected void onZoomChange() {
        windLadder.forceSwap();
    }

    @Override
    protected OverlayViewOnAddHandler getOnAddHandler() {
        return new OverlayViewOnAddHandler() {
            @Override
            public void onAdd(OverlayViewMethods methods) {
                methods.getPanes().getMapPane().appendChild(canvas.getElement());
                WindLadderOverlay.this.onAttach();
            }
        };
    }

    protected int getMapWidth() {
        if (mapWidth == null) {
            mapWidth = getMap().getDiv().getClientWidth();
        }
        return mapWidth;
    }

    protected int getMapHeight() {
        if (mapHeight == null) {
            mapHeight = getMap().getDiv().getClientHeight();
        }
        return mapHeight;
    }
    
    @Override
    public String toString() {
        return super.toString()+" "+name;
    }
}
