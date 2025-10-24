package com.sap.sailing.gwt.ui.simulator;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.events.center.CenterChangeMapEvent;
import com.google.gwt.maps.client.events.center.CenterChangeMapHandler;
import com.google.gwt.resources.client.ImageResource;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.client.shared.racemap.ImageTransformer;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;

public class ImageCanvasOverlay extends CanvasOverlayV3 {

    private int canvasWidth;
    private int canvasHeight;
    
    private int oLeft;
    private int oTop;

    private double bearing = 0.0;

    private ImageTransformer imgTrafo;
    
    public ImageCanvasOverlay(MapWidget map, int zIndex, ImageResource img, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        canvasWidth = 100;
        canvasHeight = 100;
        if (getCanvas() != null) {
            // setCanvasPosition(map.getAbsoluteLeft(), map.getAbsoluteTop());
            getCanvas().setWidth(String.valueOf(canvasWidth));
            getCanvas().setHeight(String.valueOf(canvasHeight));
            getCanvas().setCoordinateSpaceWidth(canvasWidth);
            getCanvas().setCoordinateSpaceHeight(canvasHeight);
        }
        imgTrafo = new ImageTransformer(img);
        getMap().addCenterChangeHandler(new CenterChangeMapHandler() {
            @Override
            public void onEvent(CenterChangeMapEvent event) {
                setPosition();
            };
        });
    }
    
    
    public void addToMap() {
        super.addToMap();
    }

    public void setPosition() {
        if (getMapProjection() != null) {
            Point sw = getMapProjection().fromLatLngToDivPixel(getMap().getBounds().getSouthWest());
            Point ne = getMapProjection().fromLatLngToDivPixel(getMap().getBounds().getNorthEast());
            double pLeft = Math.min(sw.getX(), ne.getX()) - 25 + oLeft;
            double pTop = Math.min(sw.getY(), ne.getY()) - 25 + oTop;
            setCanvasPosition(pLeft, pTop);
        }
    }
    
    public void setOffset(int offsetLeft, int offsetTop) {
    	this.oLeft = offsetLeft;
    	this.oTop = offsetTop;
    }
    
    @Override
    protected void draw() {
        if (getMapProjection() != null) {
            setPosition();
            imgTrafo.drawToCanvas(getCanvas(), bearing, 1.0);
        }
    }

    public void setBearing(double bearing) {
    	this.bearing = bearing;
    }
    
}
