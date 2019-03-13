package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.mousedown.MouseDownMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapHandler;
import com.google.gwt.maps.client.events.mouseup.MouseUpMapHandler;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.Polyline;

/**
 * 
 * @author Tim Hessenm�ller (D062243)
 */
public class MultiColorPolyline {
    private MultiColorPolylineOptions options;
    
    private List<Polyline> polylines;
    
    private MapWidget map;
    
    private Set<MultiColorPolyline> pathChangeListeners;
    
    private Set<ClickMapHandler> clickMapHandlers;
    private Set<MouseOverMapHandler> mouseOverMapHandlers;
    private Set<MouseDownMapHandler> mouseDownMapHandlers;
    private Set<MouseUpMapHandler> mouseUpMapHandlers;
    private Set<MouseOutMapHandler> mouseOutMapHandlers;
    
    public MultiColorPolyline(MultiColorPolylineColorProvider colorProvider) {
        this(new MultiColorPolylineOptions(colorProvider));
    }
    public MultiColorPolyline(MultiColorPolylineOptions options) {
        this.options = options;
        polylines = new ArrayList<>();
        
        pathChangeListeners = new HashSet<>();
        
        clickMapHandlers = new HashSet<>();
        mouseOverMapHandlers = new HashSet<>();
        mouseDownMapHandlers = new HashSet<>();
        mouseUpMapHandlers = new HashSet<>();
        mouseOutMapHandlers = new HashSet<>();
    }
    
    /*public MultiColorPolylineOptions getOptions() {
        return options;
    }*/
    public void setOptions(final MultiColorPolylineOptions options) {
        if (this.options.getColorMode() != options.getColorMode()) {
            MVCArray<LatLng> path = MVCArray.newInstance(getPath().toArray(new LatLng[0]));
            this.options = options;
            setPath(path);
        } else {
            this.options = options;
        }
        for (int i = 0; i < polylines.size(); i++) {
            String color = options.getColorProvider().getColor(i);
            polylines.get(i).setOptions(options.newPolylineOptionsInstance(color));
        }
    }
    
    public List<LatLng> getPath() {
        final int cap = getLength();
        List<LatLng> path = new ArrayList<>(cap);
        if (cap > 0) {
            path.add(polylines.get(0).getPath().get(0));
            for (Polyline line : polylines) {
                for (int i = 1; i < line.getPath().getLength(); i++) {
                    path.add(line.getPath().get(i));
                }
            }
        }
        return path;
    }
    
    public void setPath(final MVCArray<LatLng> path) {
        for (MultiColorPolyline line : pathChangeListeners) {
            line.setPath(path);
        }
        clearPolylines();
        switch (options.getColorMode()) {
        case MONOCHROMATIC:
            polylines.add(createPolyline(path, 0));
            break;
        case POLYCHROMATIC:
            for (int i = 0; i < path.getLength() - 1; i++) {
                MVCArray<LatLng> subPath = MVCArray.newInstance();
                subPath.push(path.get(i));
                subPath.push(path.get(i + 1));
                polylines.add(createPolyline(subPath, i));
            }
            break;
        }
    }
    
    public void insertAt(int index, LatLng position) {
        if (position == null) throw new IllegalArgumentException("Cannot insert value: null");
        for (MultiColorPolyline line : pathChangeListeners) {
            line.insertAt(index, position);
        }
        switch (options.getColorMode()) {
        case MONOCHROMATIC:
            if (polylines.isEmpty()) {
                polylines.add(createPolyline(MVCArray.newInstance(), 0));   
            }
            polylines.get(0).getPath().insertAt(index, position);
            break;
        case POLYCHROMATIC:
            if (index == 0) {
                // Prepend a new Polyline
                if (polylines.isEmpty() || polylines.get(0).getPath().getLength() == 2) {
                    // There either is no Polyline or the existing Polyline at index 0 is completed
                    // Prepend a new Polyline
                    MVCArray<LatLng> path = MVCArray.newInstance();
                    path.push(position);
                    if (!polylines.isEmpty()) { // If we can connect the new Polyline to an existing one do so
                        path.push(polylines.get(0).getPath().get(0));
                    }
                    polylines.add(0, createPolyline(path, index));
                } else {
                    // The Polyline at index 0 is incomplete
                    // Complete it
                    polylines.get(0).getPath().insertAt(0, position);
                }
            } else if (index == getLength()) {
                if (index == 1 && polylines.get(0).getPath().getLength() == 1) {
                    // Finish first polyline
                    polylines.get(0).getPath().push(position);
                } else {
                    // Append a new Polyline
                    MVCArray<LatLng> path = MVCArray.newInstance();
                    path.push(polylines.get(index - 2).getPath().get(1));
                    path.push(position);
                    polylines.add(index - 1, createPolyline(path, index));
                }
            } else {
                // Split an existing Polyline into two
                LatLng end = polylines.get(index - 1).getPath().get(1);
                polylines.get(index - 1).getPath().setAt(1, position);
                MVCArray<LatLng> path = MVCArray.newInstance();
                path.push(position);
                path.push(end);
                polylines.add(index, createPolyline(path, index));
            }
            break;
        }
    }
    
    public LatLng removeAt(int index) {
        if (index < 0 || index >= getLength()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + getLength());
        }
        for (MultiColorPolyline line : pathChangeListeners) {
            line.removeAt(index);
        }
        switch (options.getColorMode()) {
        case MONOCHROMATIC:
            return polylines.get(0).getPath().removeAt(index);
        case POLYCHROMATIC:
            LatLng removed;
            if (index == 0) {
                // Remove the Polyline connecting the first to the second fix
                removed = polylines.get(0).getPath().get(0);
                if (polylines.size() == 1 && polylines.get(0).getPath().getLength() == 2) {
                    // If there is only one polyline with two fixes left remove the first fix but keep the second
                    polylines.get(0).getPath().removeAt(0);
                } else {
                    polylines.get(0).setMap(null);
                    polylines.remove(0);
                }
            } else if (index == getLength() - 1) {
                // Remove the Polyline connecting the last two fixes
                removed = polylines.get(index - 1).getPath().get(1);
                if (getLength() > 2) {
                    polylines.get(index - 1).setMap(null);
                    polylines.remove(index - 1);
                } else {
                    polylines.get(0).getPath().removeAt(1);
                }
            } else {
                // Remove the Polyline ending at fix
                removed = polylines.get(index - 1).getPath().get(1);
                LatLng start = polylines.get(index - 1).getPath().get(0);
                polylines.get(index - 1).setMap(null);
                polylines.remove(index - 1);
                // and update the following Polyline to fill the gap
                polylines.get(index - 1).getPath().setAt(0, start);
            }
            return removed;
        }
        return null;
    }
    
    public void setAt(int index, LatLng position) {
        for (MultiColorPolyline line : pathChangeListeners) {
            line.setAt(index, position);
        }
        switch (options.getColorMode()) {
        case MONOCHROMATIC:
            polylines.get(0).getPath().setAt(index, position);
            break;
        case POLYCHROMATIC:
            if (index == 0) {
                polylines.get(0).getPath().setAt(0, position);
            } else if (index == getLength() - 1) {
                polylines.get(index - 1).getPath().setAt(1, position);
            } else {
                polylines.get(index - 1).getPath().setAt(1, position);
                polylines.get(index).getPath().setAt(0, position);
            }
            break;
        }
    }
    
    public MapWidget getMap() {
        return map;
    }
    
    public void setMap(MapWidget map) {
        this.map = map;
        for (int i = 0; i < polylines.size(); i++) {
            polylines.get(i).setMap(map);
        }
    }
    
    public void clear() {
        for (MultiColorPolyline line : pathChangeListeners) {
            line.clear();
        }
        clearPolylines();
    }

    private void clearPolylines() {
        for (Polyline l : polylines) {
            l.setMap(null);
        }
        polylines.clear();
    }
    
    public int getLength() {
        switch (options.getColorMode()) {
        case MONOCHROMATIC:
            return polylines.isEmpty() ? 0 : polylines.get(0).getPath().getLength();
        case POLYCHROMATIC:
            switch (polylines.size()) {
            case 0:
                return 0;
            case 1:
                return polylines.get(0).getPath().getLength();
            default:
                return 1 + polylines.size();
            }
        }
        return -1;
    }
    
    private Polyline createPolyline(MVCArray<LatLng> path, int colorIndex) {
        Polyline line = options.newPolylineInstance(colorIndex);
        line.setPath(path);
        if (map != null) {
            line.setMap(map);
        }
        for (ClickMapHandler h : clickMapHandlers) {
            line.addClickHandler(h);
        }
        for (MouseOverMapHandler h : mouseOverMapHandlers) {
            line.addMouseOverHandler(h);
        }
        for (MouseDownMapHandler h : mouseDownMapHandlers) {
            line.addMouseDownHandler(h);
        }
        for (MouseUpMapHandler h : mouseUpMapHandlers) {
            line.addMouseUpHandler(h);
        }
        for (MouseOutMapHandler h : mouseOutMapHandlers) {
            line.addMouseOutMoveHandler(h);
        }
        return line;
    }
    
    public void addPathChangeListener(MultiColorPolyline listener) {
        pathChangeListeners.add(listener);
    }
    public boolean removePathChangeListener(MultiColorPolyline listener) {
        return pathChangeListeners.remove(listener);
    }
    
    public void addClickHandler(ClickMapHandler handler) {
        clickMapHandlers.add(handler);
        // Add to already existing Polylines
        for (Polyline line : polylines) {
            line.addClickHandler(handler);
        }
    }
    
    public void addMouseOverHandler(MouseOverMapHandler handler) {
        mouseOverMapHandlers.add(handler);
        // Add to already existing Polylines
        for (Polyline line : polylines) {
            line.addMouseOverHandler(handler);
        }
    }
    
    public void addMouseDownHandler(MouseDownMapHandler handler) {
        mouseDownMapHandlers.add(handler);
        // Add to already existing Polylines
        for (Polyline line : polylines) {
            line.addMouseDownHandler(handler);
        }
    }
    
    public void addMouseUpHandler(MouseUpMapHandler handler) {
        mouseUpMapHandlers.add(handler);
        // Add to already existing Polylines
        for (Polyline line : polylines) {
            line.addMouseUpHandler(handler);
        }
    }
    
    public void addMouseOutMoveHandler(MouseOutMapHandler handler) {
        mouseOutMapHandlers.add(handler);
        // Add to already existing Polylines
        for (Polyline line : polylines) {
            line.addMouseOutMoveHandler(handler);
        }
    }
}