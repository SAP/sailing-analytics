package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.mousedown.MouseDownMapEvent;
import com.google.gwt.maps.client.events.mousedown.MouseDownMapHandler;
import com.google.gwt.maps.client.events.mousemove.MouseMoveMapEvent;
import com.google.gwt.maps.client.events.mousemove.MouseMoveMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapEvent;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapEvent;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapHandler;
import com.google.gwt.maps.client.events.mouseup.MouseUpMapEvent;
import com.google.gwt.maps.client.events.mouseup.MouseUpMapHandler;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.maps.client.overlays.PolylineOptions;

public class Hoverline {
    private static final double TRANSPARENT = 0;
    private static final double VISIBLE = 0.2d;
    
    private final Colorline hoverline;
    private final ColorlineOptions options;
    protected boolean doNotProcessMouseMoveOut;
    
    public Hoverline(final Polyline polyline, PolylineOptions polylineOptions, final RaceMap map) {   
        options = new ColorlineOptions();
        options.setClickable(polylineOptions.getClickable());
        options.setEditable(polyline.getEditable());
        options.setGeodesic(polylineOptions.getGeodesic());
        try {
            options.setZIndex(polylineOptions.getZindex());  // if the zindex is not set, this line throws an exception in dev mode
        } catch (Exception e) {
            // the Z-index of polylineOptions most likely was undefined and therefore cannot be copied (GWT DevMode problem, mostly)
        }
        options.setVisible(false);
        options.setColorMode(ColorlineMode.MONOCHROMATIC);
        options.setColorProvider(indexIntoTail -> polylineOptions.getStrokeColor());
        hoverline = new Colorline(options);
        hoverline.setMap(polyline.getMap());
        hoverline.setPath(polyline.getPath());
        polyline.addMouseOverHandler(new MouseOverMapHandler() {
            @Override
            public void onEvent(MouseOverMapEvent event) {
                options.setStrokeOpacity(map.getSettings().getTransparentHoverlines() ? TRANSPARENT : VISIBLE);
                options.setStrokeWeight(map.getSettings().getHoverlineStrokeWeight());
                options.setVisible(true);
                hoverline.setOptions(options);
            }
        });
        
        //Workaround for bug4480 (chrome does fire mouseOutMove on mouseclick)
        hoverline.addMouseDownHandler(new MouseDownMapHandler() {
            @Override
            public void onEvent(MouseDownMapEvent event) {
                doNotProcessMouseMoveOut = true;
            }
        });
        
        hoverline.addMouseUpHandler(new MouseUpMapHandler() {
            @Override
            public void onEvent(MouseUpMapEvent event) {
                doNotProcessMouseMoveOut = false;
            }
        });
        
        hoverline.addMouseOutMoveHandler(new MouseOutMapHandler() {
            @Override
            public void onEvent(MouseOutMapEvent event) {
                if (!doNotProcessMouseMoveOut) {
                    options.setVisible(false);
                    hoverline.setOptions(options);
                }
            }
        });
        map.getMap().addMouseMoveHandler(new MouseMoveMapHandler() {
            @Override
            public void onEvent(MouseMoveMapEvent event) {
                options.setVisible(false);
                hoverline.setOptions(options);
            }
        });
    }
    
    public Hoverline(final Colorline colorline,
            final ColorlineOptions colorlineOptions, final RaceMap map) {
        options = colorlineOptions;
        hoverline = new Colorline(options);
        hoverline.setMap(colorline.getMap());
        hoverline.setPath(MVCArray.newInstance(colorline.getPath().toArray(new LatLng[0])));
        colorline.addPathChangeListener(hoverline);
        options.setVisible(false);
        colorline.addMouseOverHandler(new MouseOverMapHandler() {
            @Override
            public void onEvent(MouseOverMapEvent event) {
                options.setStrokeOpacity(map.getSettings().getTransparentHoverlines() ? TRANSPARENT : VISIBLE);
                options.setStrokeWeight(map.getSettings().getHoverlineStrokeWeight());
                options.setVisible(true);
                hoverline.setOptions(options);
            }
        });
        // Workaround for bug4480 (chrome does fire mouseOutMove on mouseclick)
        hoverline.addMouseDownHandler(new MouseDownMapHandler() {
            @Override
            public void onEvent(MouseDownMapEvent event) {
                doNotProcessMouseMoveOut = true;
            }
        });
        hoverline.addMouseUpHandler(new MouseUpMapHandler() {
            @Override
            public void onEvent(MouseUpMapEvent event) {
                doNotProcessMouseMoveOut = false;
            }
        });
        hoverline.addMouseOutMoveHandler(new MouseOutMapHandler() {
            @Override
            public void onEvent(MouseOutMapEvent event) {
                if (!doNotProcessMouseMoveOut) {
                    options.setVisible(false);
                    hoverline.setOptions(options);
                }
            }
        });
        map.getMap().addMouseMoveHandler(new MouseMoveMapHandler() { // moved beyond the hoverline and onto the map
            @Override
            public void onEvent(MouseMoveMapEvent event) {
                options.setVisible(false);
                hoverline.setOptions(options);
            }
        });
    }


    public void addClickHandler(ClickMapHandler handler) {
        hoverline.addClickHandler(handler);
    }
    
    public void addMouseOutMoveHandler(MouseOutMapHandler handler) {
        hoverline.addMouseOutMoveHandler(handler);
    }
    
    public void addMouseOverHandler(MouseOverMapHandler handler) {
        hoverline.addMouseOverHandler(handler);
    }
}