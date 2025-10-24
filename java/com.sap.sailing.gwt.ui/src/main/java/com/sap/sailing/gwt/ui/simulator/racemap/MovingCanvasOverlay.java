package com.sap.sailing.gwt.ui.simulator.racemap;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.simulator.streamlets.Vector;

/**
 * This class extends {@link FullCanvasOverlay} to provide the functionality that the canvas covers the
 * full viewable area of the map and moves previously drawn content according to drag and zoom.
 * 
 * @author Christopher Ronnewinkel (D036654)
 *
 */
public abstract class MovingCanvasOverlay extends FullCanvasOverlay {

    private LatLng upperLeftCornerLatLng;

    public MovingCanvasOverlay(MapWidget map, int zIndex, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
    }
    
    @Override
    public void setCanvasSettings() {
        // do nothing, if mapProjection is not available
        if (getMapProjection() != null) {
            final int canvasWidth = getMap().getDiv().getClientWidth();
            final int canvasHeight = getMap().getDiv().getClientHeight();
            // calculate pixel-positions of old and new canvas-bounds using the same, current mapProjection
            // start from LatLng, as the canvas might have jumped to new bounds that do not intersect with the old bounds
            final Point nwOldDivPx;
            if (upperLeftCornerLatLng == null) {
                nwOldDivPx = null;
            } else {
                nwOldDivPx = getMapProjection().fromLatLngToDivPixel(upperLeftCornerLatLng);
            }
            upperLeftCornerLatLng = getMapProjection().fromContainerPixelToLatLng(Point.newInstance(0, 0));
            final Point nwNewDivPx = getMapProjection().fromLatLngToDivPixel(upperLeftCornerLatLng);
            widgetPosLeft = Math.round(nwNewDivPx.getX());
            widgetPosTop = Math.round(nwNewDivPx.getY());
            // calculate the translation-vector between old and new origin in pixels
            if (nwOldDivPx == null) {
                diffPx = new Vector(0, 0);
            } else {
                double oldPosLeft = Math.round(nwOldDivPx.getX());
                double oldPosTop = Math.round(nwOldDivPx.getY());
                diffPx = new Vector(oldPosLeft - widgetPosLeft, oldPosTop - widgetPosTop);
            }
            // store canvas-content, because setWidth() and setHeight() will clear canvas and change Context2d 
            Context2d ctxt = canvas.getContext2d();
            final int clientWidth = canvas.getElement().getClientWidth();
            final int clientHeight = canvas.getElement().getClientHeight();
            ImageData canvasContent = null;
            if (clientWidth > 0 && clientHeight > 0) {
                canvasContent = ctxt.getImageData(0, 0, canvas.getElement().getClientWidth(), canvas.getElement().getClientHeight());
            }
            canvas.setWidth(String.valueOf(Math.max(1, canvasWidth)));
            canvas.setHeight(String.valueOf(Math.max(1, canvasHeight)));
            canvas.setCoordinateSpaceWidth(Math.max(1, canvasWidth));
            canvas.setCoordinateSpaceHeight(Math.max(1, canvasHeight));
            if (clientWidth > 0 && clientHeight > 0) {
                // get updated Context2d and restore canvas-content moved by translation-vector
                ctxt = canvas.getContext2d();
                ctxt.putImageData(canvasContent, diffPx.x, diffPx.y);
            }
            // update canvas position
            setCanvasPosition(widgetPosLeft, widgetPosTop);
        }
    }

    public void initCanvasOrigin() {
        upperLeftCornerLatLng = null;
    }
}
