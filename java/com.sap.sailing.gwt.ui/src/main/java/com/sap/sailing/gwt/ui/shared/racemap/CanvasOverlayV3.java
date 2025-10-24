package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.base.Size;
import com.google.gwt.maps.client.events.MapHandlerRegistration;
import com.google.gwt.maps.client.events.click.ClickEventFormatter;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.dblclick.DblClickEventFormatter;
import com.google.gwt.maps.client.events.dblclick.DblClickMapHandler;
import com.google.gwt.maps.client.events.mousedown.MouseDownEventFormatter;
import com.google.gwt.maps.client.events.mousedown.MouseDownMapHandler;
import com.google.gwt.maps.client.events.mousemove.MouseMoveEventFormatter;
import com.google.gwt.maps.client.events.mousemove.MouseMoveMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutEventFormatter;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.events.mouseover.MouseOverEventFormatter;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapHandler;
import com.google.gwt.maps.client.events.mouseup.MouseUpEventFormatter;
import com.google.gwt.maps.client.events.mouseup.MouseUpMapHandler;
import com.google.gwt.maps.client.events.rightclick.RightClickEventFormatter;
import com.google.gwt.maps.client.events.rightclick.RightClickMapHandler;
import com.google.gwt.maps.client.overlays.MapCanvasProjection;
import com.google.gwt.maps.client.overlays.OverlayView;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewMethods;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnAddHandler;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnDrawHandler;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnRemoveHandler;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * The abstract base class for all canvas overlays.
 * @author Frank
 */
public abstract class CanvasOverlayV3 {
    private OverlayView customOverlayView;
    
    /**
     * The HTML5 canvas which can be used to draw arbitrary shapes on a google map.
     */
    protected Canvas canvas;

    /**
     * Indicates whether the canvas has been selected or not.
     */
    protected boolean isSelected;

    /**
     * The reference to the actual Google map.
     */
    protected MapWidget map;

    /**
     * The position of the canvas as a Latitude/Longitude position
     */
    protected LatLng latLngPosition;
    
    /**
     * the z-Index of the canvas
     */
    protected final int zIndex;

    protected final CoordinateSystem coordinateSystem;
    
    /**
     * The time in milliseconds that will be used to move the boat from its old position to the next.
     * <code>-1</code> means that currently no transition is set. Invariant: this field's value corresponds
     * with the time set on the canvas element style's <code>transition</code> CSS property.
     */
    private long transitionTimeInMilliseconds;
    
    /**
     * Remembers the old drawing angle as passed to {@link #setCanvasRotation()} to minimize rotation angle upon
     * the next update. The rotation property will always be animated according to the magnitude of the values. A
     * transition from 5 to 355 will go through 180 and not from 5 to 0==360 and back to 355! Therefore, with 5 being
     * the last rotation angle, the new rotation angle of 355 needs to be converted to -5 to ensure that the transition
     * goes through 0.<p>
     */
    private Double drawingAngle;

    public CanvasOverlayV3(MapWidget map, int zIndex, String canvasId, CoordinateSystem coordinateSystem) {
        this.transitionTimeInMilliseconds = -1; // no animated position transition initially
        this.map = map;
        this.zIndex = zIndex;
        this.coordinateSystem = coordinateSystem;
        canvas = Canvas.createIfSupported();
        canvas.getElement().getStyle().setZIndex(zIndex);
        canvas.getElement().getStyle().setCursor(Cursor.POINTER);
        canvas.getElement().getStyle().setPosition(com.google.gwt.dom.client.Style.Position.ABSOLUTE);
        if (canvasId != null) {
            canvas.getElement().setId(canvasId);
        }
        customOverlayView = OverlayView.newInstance(map, getOnDrawHandler(), getOnAddHandler(), getOnRemoveHandler());
    }

    public CanvasOverlayV3(MapWidget map, int zIndex, CoordinateSystem coordinateSystem) {
        this(map, zIndex, null, coordinateSystem);
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public boolean isVisible() {
        return getCanvas() != null && getCanvas().isVisible();
    }

    public void setVisible(boolean isVisible) {
        if (getCanvas() != null) {
            getCanvas().setVisible(isVisible);
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public LatLng getLatLngPosition() {
        return latLngPosition;
    }

    public void addToMap() {
        customOverlayView.setMap(map);
    }
    
    public void removeFromMap() {
        customOverlayView.setMap(null);
    }

    /**
     * This event is fired when the DOM click event is fired on the Canvas.
     * 
     * @param handler
     */
    public final HandlerRegistration addClickHandler(ClickMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), ClickEvent.getType(), 
                handler, new ClickEventFormatter(), true);
    }

    /**
     * This event is fired when the DOM dblclick event is fired on the Canvas.
     * 
     * @param handler
     */
    public final HandlerRegistration addDblClickHandler(DblClickMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), DoubleClickEvent.getType(), 
                handler, new DblClickEventFormatter(), true);
    }

    /**
     * This event is fired when the DOM mousedown event is fired on the Canvas.
     * 
     * @param handler
     */
    public final HandlerRegistration addMouseDownHandler(MouseDownMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), MouseDownEvent.getType(), 
                handler, new MouseDownEventFormatter(), true);
    }

    /**
     * This event is fired when the DOM mousemove event is fired on the Canvas.
     * 
     * @param handler
     */
    public final HandlerRegistration addMouseMoveHandler(MouseMoveMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), MouseMoveEvent.getType(), 
                handler, new MouseMoveEventFormatter(), true);
    }

    /**
     * This event is fired on Canvas mouseout.
     * 
     * @param handler
     */
    public final HandlerRegistration addMouseOutMoveHandler(MouseOutMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), MouseOutEvent.getType(), 
                handler, new MouseOutEventFormatter(), true);
    }

    /**
     * This event is fired on Canvas mouseover.
     * 
     * @param handler
     */
    public final HandlerRegistration addMouseOverHandler(MouseOverMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), MouseOverEvent.getType(), 
                handler, new MouseOverEventFormatter(), true);
    }

    /**
     * This event is fired when the DOM mouseup event is fired on the Canvas.
     * 
     * @param handler
     */
    public final HandlerRegistration addMouseUpHandler(MouseUpMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), MouseUpEvent.getType(), 
                handler, new MouseUpEventFormatter(), true);
    }

    /**
     * This event is fired when the Canvas is right-clicked on.
     * 
     * @param handler
     */
    public final HandlerRegistration addRightClickHandler(RightClickMapHandler handler) {
        return MapHandlerRegistration.addEventListener(getCanvas().getCanvasElement(), ContextMenuEvent.getType(), 
                handler, new RightClickEventFormatter(), true);
    }
    
    protected void setLatLngPosition(LatLng latLngPosition) {
        this.latLngPosition = latLngPosition;
    }

    public MapWidget getMap() {
        return map;
    }

    protected abstract void draw();

    protected void onAttach() {
    }

    protected OverlayViewOnAddHandler getOnAddHandler() {
        OverlayViewOnAddHandler result = new OverlayViewOnAddHandler() {
            @Override
            public void onAdd(OverlayViewMethods methods) {
                methods.getPanes().getMapPane().appendChild(canvas.getElement());
                CanvasOverlayV3.this.onAttach();
                methods.getPanes().getOverlayMouseTarget().appendChild(canvas.getElement());
            }
        };
        return result;
    }
     
    protected OverlayViewOnDrawHandler getOnDrawHandler() {
        return new OverlayViewOnDrawHandler() {
            @Override
            public void onDraw(OverlayViewMethods methods) {
                draw();
            }
        };
    }
    
    protected OverlayViewOnRemoveHandler getOnRemoveHandler() {
        OverlayViewOnRemoveHandler result = new OverlayViewOnRemoveHandler() {
            @Override
            public void onRemove(OverlayViewMethods methods) {
                // remove the canvas from the parent widget
                canvas.getElement().removeFromParent();
            }
        };
        return result;
    }
    
    public void setCanvasPositionAndRotationTransition(long durationInMilliseconds) {
        if (durationInMilliseconds != transitionTimeInMilliseconds) {
            for (String browserTypePrefix : getBrowserTypePrefixes()) {
                StringBuilder transformPropertyList = new StringBuilder();
                transformPropertyList.append("left ");
                transformPropertyList.append(durationInMilliseconds);
                transformPropertyList.append("ms linear, top ");
                transformPropertyList.append(durationInMilliseconds);
                transformPropertyList.append("ms linear, ");
                transformPropertyList.append(getBrowserSpecificDashedPropertyName(browserTypePrefix, "transform"));
                transformPropertyList.append(' ');
                transformPropertyList.append(durationInMilliseconds);
                transformPropertyList.append("ms linear");
                canvas.getElement().getStyle().setProperty(getBrowserSpecificPropertyName(browserTypePrefix, "transition"), transformPropertyList.toString());
            }
            transitionTimeInMilliseconds = durationInMilliseconds;
        }
    }
    
    public void removeCanvasPositionAndRotationTransition() {
        if (transitionTimeInMilliseconds != -1) {
            setProperty(canvas.getElement().getStyle(), "transition", "none");
            transitionTimeInMilliseconds = -1;
        }
    }
    
    private void setProperty(Style style, String baseCamelCasePropertyName, String value) {
        for (String browserTypePrefix : getBrowserTypePrefixes()) {
            style.setProperty(getBrowserSpecificPropertyName(browserTypePrefix, baseCamelCasePropertyName), value);
        }
    }
    
    /**
     * @return the prefixes required for new CSS3-style elements such as "transition" or "@keyframe", including the
     *         empty string, "moz" and "webkit" as well as others
     */
    private String[] getBrowserTypePrefixes() {
        return new String[] { "", /* Firefox */ "moz", /* IE */ "ms", /* Opera */ "o", /* Chrome and Mobile */ "webkit" };
    }
    
    /**
     * Converts something like "transformOrigin" to "-<code>browserType</code>-transform-origin"
     * 
     * @param browserType
     *            a browser type string as received from {@link #getBrowserTypePrefixes()}. If empty or null, the
     *            original property name is returned unchanged
     * @param camelCaseString
     *            the original camel-cased property name, such as "transformOrigin"
     */
    private String getBrowserSpecificDashedPropertyName(String browserType, String camelCaseString) {
        StringBuilder result = new StringBuilder();
        if (browserType != null && !browserType.isEmpty()) {
            result.append('-');
            result.append(browserType);
            result.append('-');
        }
        for (int i=0; i<camelCaseString.length(); i++) {
            final char c = camelCaseString.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('-');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    private String getBrowserSpecificPropertyName(String browserType, String basePropertyName) {
        final String result;
        if (browserType == null || browserType.isEmpty()) {
                result = basePropertyName;
        } else {
                result = browserType+basePropertyName.substring(0, 1).toUpperCase()+basePropertyName.substring(1);
        }
        return result;
    }

    protected void setCanvasPosition(double x, double y) {
        canvas.getElement().getStyle().setLeft(x, Unit.PX);
        canvas.getElement().getStyle().setTop(y, Unit.PX);
    }

    /**
     * Updates {@link #drawingAngle} so that the CSS transition from the old {@link #drawingAngle} to
     * <code>newBoatDrawingAngle</code> is minimal.
     */
    protected void updateDrawingAngleAndSetCanvasRotation(double newBoatDrawingAngle) {
        if (drawingAngle == null) {
            drawingAngle = newBoatDrawingAngle;
        } else {
            drawingAngle = getNewRotationWithMinimalDiff(newBoatDrawingAngle);
        }
        setCanvasRotation();
    }
    
    private void setCanvasRotation() {
        setProperty(canvas.getElement().getStyle(), "transformOrigin", "50% 50%");
        setProperty(canvas.getElement().getStyle(), "transform", "translateZ(0) rotate(" + drawingAngle + "deg)");
    }

    protected double getNewRotationWithMinimalDiff(double desiredAngle) {
        double desiredMinusCurrent;
        double result = desiredAngle;
        while (Math.abs(desiredMinusCurrent = result - drawingAngle) > 180) {
            result -= Math.signum(desiredMinusCurrent)*360;
        }
        return drawingAngle+desiredMinusCurrent;
    }

    protected double calculateRadiusOfBoundingBoxInPixels(MapCanvasProjection projection, Position centerPosition, Distance length) {
        Position translateRhumbX = centerPosition.translateRhumb(new DegreeBearingImpl(90), length);
        Position translateRhumbY = centerPosition.translateRhumb(new DegreeBearingImpl(0), length);
        LatLng posWithDistanceX = coordinateSystem.toLatLng(translateRhumbX);
        LatLng posWithDistanceY = coordinateSystem.toLatLng(translateRhumbY);
        Point pointCenter = projection.fromLatLngToDivPixel(coordinateSystem.toLatLng(centerPosition));
        Point pointX = projection.fromLatLngToDivPixel(posWithDistanceX);
        Point pointY = projection.fromLatLngToDivPixel(posWithDistanceY);
        double diffX = getPointDistance(pointX, pointCenter);
        double diffY = getPointDistance(pointY, pointCenter);
        return Math.min(diffX, diffY);  
    }
    
    /**
     * Computes the Euklidian distance between the two points in their coordinate system
     */
    private double getPointDistance(Point a, Point b) {
        return Math.sqrt((b.getX()-a.getX())*(b.getX()-a.getX()) + (b.getY()-a.getY())*(b.getY()-a.getY()));
    }

    protected double calculateDistanceAlongX(MapCanvasProjection projection, Position pos, Distance distanceX) {
        Position translateRhumbX = pos.translateRhumb(new DegreeBearingImpl(90), distanceX);
        LatLng posWithDistanceX = coordinateSystem.toLatLng(translateRhumbX);
        Point point = projection.fromLatLngToDivPixel(coordinateSystem.toLatLng(pos));
        Point pointX =  projection.fromLatLngToDivPixel(posWithDistanceX);
        return Math.abs(pointX.getX() - point.getX());
    }
    
    protected Size calculateBoundingBox(MapCanvasProjection projection, Position pos, Distance distanceX, Distance distanceY) {
        Position translateRhumbX = pos.translateRhumb(new DegreeBearingImpl(90), distanceX);
        Position translateRhumbY = pos.translateRhumb(new DegreeBearingImpl(0), distanceY);
        LatLng posWithDistanceX = coordinateSystem.toLatLng(translateRhumbX);
        LatLng posWithDistanceY = coordinateSystem.toLatLng(translateRhumbY);
        Point pointCenter = projection.fromLatLngToDivPixel(coordinateSystem.toLatLng(pos));
        Point pointX = projection.fromLatLngToDivPixel(posWithDistanceX);
        Point pointY = projection.fromLatLngToDivPixel(posWithDistanceY);
        return Size.newInstance(Math.abs(pointX.getX() - pointCenter.getX()), Math.abs(pointY.getY() - pointCenter.getY()));
    }
    
    protected void setCanvasSize(int newWidthInPx, int newHeightInPx) {
        if (getCanvas() != null) {
            getCanvas().setWidth(newWidthInPx + "px");
            getCanvas().setHeight(newHeightInPx + "px");
            getCanvas().setCoordinateSpaceWidth(newWidthInPx);
            getCanvas().setCoordinateSpaceHeight(newHeightInPx);
        }
    }

    protected void updateTransition(long timeForPositionTransitionMillis) {
        if (timeForPositionTransitionMillis == -1) {
            removeCanvasPositionAndRotationTransition();
        } else {
            setCanvasPositionAndRotationTransition(timeForPositionTransitionMillis);
        }
    }

    public int getZIndex() {
        return zIndex;
    }

    public MapCanvasProjection getMapProjection() {
        return customOverlayView.getProjection();
    }
}
