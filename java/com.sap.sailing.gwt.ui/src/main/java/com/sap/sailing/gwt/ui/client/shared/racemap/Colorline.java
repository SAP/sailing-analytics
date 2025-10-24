package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.ajaxloader.client.Properties;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.mousedown.MouseDownMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapEvent;
import com.google.gwt.maps.client.events.mouseover.MouseOverMapHandler;
import com.google.gwt.maps.client.events.mouseup.MouseUpMapHandler;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.Polyline;

/**
 * One or more {@link Polyline}s connected to form a longer polyline, with the possibility to change
 * styles from segment to segment. Two different modes currently exist: {@link ColorlineMode#MONOCHROMATIC}
 * and {@link ColorlineMode#POLYCHROMATIC}. In monochromatic mode the entire line is represented by a single
 * {@link Polyline} as no style changes are required. In polychromatic mode, each connection between two
 * dots is a separate {@link Polyline}, and the options for each segment---particularly their color---
 * is decided by a {@link ColorlineColorProvider} embedded in the {@link ColorlineOptions} used for this
 * color line.
 * 
 * @author Tim Hessenmüller (D062243)
 */
public class Colorline {
    private ColorlineOptions options;
    
    /**
     * Container for {@link Polyline}s which together build up the {@code Colorline}.
     */
    private final List<Polyline> polylines;
    
    private MapWidget map;
    
    /**
     * A {@link Set} of {@code Colorlines} which will receive all path changes applied to this object.
     */
    private Set<Colorline> pathChangeListeners;
    
    private final Set<ClickMapHandler> clickMapHandlers;
    private final Set<MouseOverMapHandler> mouseOverMapHandlers;
    private final Set<MouseOverLineHandler> mouseOverLineHandlers;
    private final Set<MouseDownMapHandler> mouseDownMapHandlers;
    private final Set<MouseUpMapHandler> mouseUpMapHandlers;
    private final Set<MouseOutMapHandler> mouseOutMapHandlers;
    
    public static class MouseOverLineEvent extends MouseOverMapEvent {
        private final int fixIndexInTail;
        private final Polyline line;

        public MouseOverLineEvent(Properties properties, Polyline line, int fixIndexInTail) {
            super(properties);
            this.line = line;
            this.fixIndexInTail = fixIndexInTail;
        }

        /**
         * @return {@code null} if this event was produced by hovering over a {@link Colorline} that was not in
         *         {@link ColorlineMode#POLYCHROMATIC} mode; otherwise the index of the fix representing the start of
         *         the line segment hovered over in the tail from {@link FixesAndTails}.
         */
        public int getFixIndexInTail() {
            return fixIndexInTail;
        }

        /**
         * @return {@code null} if this event was produced by hovering over a {@link Colorline} that was not in
         *         {@link ColorlineMode#POLYCHROMATIC} mode; otherwise the line segment hovered over
         */
        public Polyline getLine() {
            return line;
        }
    }
    
    /**
     * Allows clients of the {@link Colorline} class to register more specific mouse hover handlers which,
     * in case of a {@link ColorlineMode#POLYCHROMATIC} setting, additionally receives the {@link Polyline}
     * segment actually hovered over, as well as the segment's start index in the {@link FixesAndTails}
     * construct. This way, the client can infer, e.g., the value visualized by the polychromatic line
     * at that point.<p>
     * 
     * Handlers implementing this interface are also compatible with the {@link MouseOverMapHandler}
     * interface. If used as such, the line and index information will always be undefined, just as if
     * a handler of this type is registered on a {@link Colorline} that is <em>not</em> using the 
     * polychromatic setting.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    @FunctionalInterface
    public static interface MouseOverLineHandler {
        void onEvent(MouseOverLineEvent event);
    }
    
    public Colorline(ColorlineColorProvider colorProvider) {
        this(new ColorlineOptions(colorProvider));
    }
    
    public Colorline(ColorlineOptions options) {
        this.options = options;
        polylines = new ArrayList<>();
        pathChangeListeners = new HashSet<>();
        clickMapHandlers = new HashSet<>();
        mouseOverMapHandlers = new HashSet<>();
        mouseOverLineHandlers = new HashSet<>();
        mouseDownMapHandlers = new HashSet<>();
        mouseUpMapHandlers = new HashSet<>();
        mouseOutMapHandlers = new HashSet<>();
    }
    
    public void setOptions(final ColorlineOptions options) {
        if (this.options.getColorMode() != options.getColorMode()) {
            // If colorMode changed the path needs to change from one polyline to multiple polylines and vice versa
            final List<LatLng> pathOfLatLngs = getPath();
            MVCArray<LatLng> path = MVCArray.newInstance(pathOfLatLngs.toArray(new LatLng[pathOfLatLngs.size()]));
            this.options = options;
            setPath(path);
        } else {
            this.options = options;
        }
        // Since colorMode and/or options have changed it's best to update the colors of all polylines
        for (int i = 0; i < polylines.size(); i++) {
            updatePolylineColor(i);
        }
    }
    
    private void updatePolylineColor(int fixIndexInTail) {
        String color = options.getColorProvider().getColor(fixIndexInTail);
        polylines.get(fixIndexInTail).setOptions(options.newPolylineOptionsInstance(color));
    }
    
    /**
     * Creates an ordered {@link List} of all {@link LatLng} vertices in this {@code Colorline}.
     * 
     * @return ordered {@link List} of {@link LatLng}.
     */
    public List<LatLng> getPath() {
        final int cap = getLength();
        final List<LatLng> path = new ArrayList<>(cap);
        if (cap > 0) {
            // In POLYCHROMATIC mode it is possible to have a single invisible vertex if the total path length is 1
            path.add(polylines.get(0).getPath().get(0));
            // In POLYCHROMATIC mode the last vertex of polyline n will be the same as the first vertex of polyline n+1
            // In MONOCHROMATIC mode there will be only 1 polyline and its first vertex was already added to path
            for (Polyline line : polylines) {
                for (int i = 1; i < line.getPath().getLength(); i++) {
                    path.add(line.getPath().get(i));
                }
            }
        }
        return path;
    }
    
    /**
     * Sets the displayed path.
     * @param path ordered {@link MVCArray} of {@link LatLng}.
     */
    public void setPath(final MVCArray<LatLng> path) {
        for (Colorline line : pathChangeListeners) {
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
    
    /**
     * Inserts a vertex at a specified position in the path.
     * 
     * @param fixIndexInTail
     *            {@code int} indicating the insertion position.
     * @param position
     *            {@link LatLng} the vertex to insert.
     * @throws IllegalArgumentException
     *             if {@code position} is {@code null}.
     * @throws IndexOutOfBoundsException
     *             if {@code index} is not in bounds of path.
     */
    public void insertAt(int fixIndexInTail, LatLng position) throws IllegalArgumentException, IndexOutOfBoundsException {
        if (position == null) throw new IllegalArgumentException("Cannot insert value: null");
        for (Colorline line : pathChangeListeners) {
            line.insertAt(fixIndexInTail, position);
        }
        switch (options.getColorMode()) {
        case MONOCHROMATIC:
            if (polylines.isEmpty()) {
                polylines.add(createPolyline(MVCArray.newInstance(), 0));   
            }
            polylines.get(0).getPath().insertAt(fixIndexInTail, position);
            break;
        case POLYCHROMATIC:
            if (fixIndexInTail == 0) {
                // Prepend a new Polyline
                if (polylines.isEmpty() || polylines.get(0).getPath().getLength() == 2) {
                    // There either is no Polyline or the existing Polyline at index 0 is completed
                    // Prepend a new Polyline
                    MVCArray<LatLng> path = MVCArray.newInstance();
                    path.push(position);
                    if (!polylines.isEmpty()) { // If we can connect the new Polyline to an existing one do so
                        path.push(polylines.get(0).getPath().get(0));
                    }
                    polylines.add(0, createPolyline(path, fixIndexInTail));
                } else {
                    // The Polyline at index 0 is incomplete
                    // Complete it
                    polylines.get(0).getPath().insertAt(0, position); // FIXME bug5921: what does this do to the polyline's color? Shouldn't the color always be determined by the first of the two points in the segment?
                }
            } else if (fixIndexInTail == getLength()) {
                if (fixIndexInTail == 1 && polylines.get(0).getPath().getLength() == 1) {
                    // Finish first polyline
                    polylines.get(0).getPath().push(position);
                } else {
                    // Append a new Polyline
                    MVCArray<LatLng> path = MVCArray.newInstance();
                    path.push(polylines.get(fixIndexInTail - 2).getPath().get(1));
                    path.push(position);
                    polylines.add(fixIndexInTail - 1, createPolyline(path, fixIndexInTail));
                }
            } else {
                // Split an existing Polyline into two
                LatLng end = polylines.get(fixIndexInTail - 1).getPath().get(1);
                polylines.get(fixIndexInTail - 1).getPath().setAt(1, position);
                MVCArray<LatLng> path = MVCArray.newInstance();
                path.push(position);
                path.push(end);
                polylines.add(fixIndexInTail, createPolyline(path, fixIndexInTail));
            }
            break;
        }
    }
    
    /**
     * Removes a vertex at a specified position from the displayed path. If the removed vertex was not at one of the
     * ends the two adjacent vertices will now directly connect to each other.
     * 
     * @param index
     *            {@code int} indication the vertex to be removed from path.
     * @return {@link LatLng} vertex that was removed.
     * @throws IndexOutOfBoundsException
     *             if {@code index} is not in bounds of path.
     */
    public LatLng removeAt(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= getLength()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + getLength());
        }
        for (Colorline line : pathChangeListeners) {
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
    
    /**
     * Sets a specified vertex.
     * 
     * @param index
     *            {@code int} vertex to set.
     * @param position
     *            {@link LatLng} to set vertex to.
     * @throws IndexOutOfBoundsException
     *             if {@code index} is not in bounds of path.
     */
    public void setAt(int index, LatLng position) throws IndexOutOfBoundsException {
        for (Colorline line : pathChangeListeners) {
            line.setAt(index, position);
        }
        switch (options.getColorMode()) {
        case MONOCHROMATIC:
            polylines.get(0).getPath().setAt(index, position);
            break;
        case POLYCHROMATIC:
            if (index == 0) { // Set the very first vertex
                polylines.get(0).getPath().setAt(0, position);
            } else if (index == getLength() - 1) { // Set the very last vertex
                polylines.get(index - 1).getPath().setAt(1, position);
            } else { // Set a vertex somewhere in the middle which affects 2 polylines
                polylines.get(index - 1).getPath().setAt(1, position);
                polylines.get(index).getPath().setAt(0, position);
                updatePolylineColor(index);
            }
            break;
        }
    }
    
    /**
     * Gets the map this {@code Colorline} is displayed on.
     * @return {@link MapWidget}
     */
    public MapWidget getMap() {
        return map;
    }
    
    /**
     * Sets the map this {@code Colorline} is displayed on.
     * @param map {@link MapWidget}
     */
    public void setMap(MapWidget map) {
        this.map = map;
        for (int i = 0; i < polylines.size(); i++) {
            polylines.get(i).setMap(map);
        }
    }
    
    /**
     * Clears the {@code Colorline} and all its {@link #pathChangeListeners}.
     */
    public void clear() {
        for (Colorline line : pathChangeListeners) {
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
    
    /**
     * Gets the amount of individual vertices in path.
     * @return {@code int} amount.
     */
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
    
    private Polyline createPolyline(MVCArray<LatLng> path, int fixIndexInTail) {
        final Polyline line = options.newPolylineInstance(fixIndexInTail);
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
        for (MouseOverLineHandler h : mouseOverLineHandlers) {
            line.addMouseOverHandler(createMouseOverMapHandlerFromMouseOverLineHandler(h, line));
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
    
    public void addPathChangeListener(Colorline listener) {
        pathChangeListeners.add(listener);
    }
    public boolean removePathChangeListener(Colorline listener) {
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
    
    public void addMouseOverLineHandler(MouseOverLineHandler handler) {
        mouseOverLineHandlers.add(handler);
        // Add to already existing Polylines
        for (Polyline line : polylines) {
            line.addMouseOverHandler(createMouseOverMapHandlerFromMouseOverLineHandler(handler, line));
        }
    }

    private MouseOverMapHandler createMouseOverMapHandlerFromMouseOverLineHandler(MouseOverLineHandler handler, Polyline line) {
        return e->handler.onEvent(new MouseOverLineEvent(e.getProperties(), line,
                options.getColorMode() == ColorlineMode.POLYCHROMATIC ? getFixIndexInTail(line) : -1));
    }
    
    private int getFixIndexInTail(Polyline line) {
        return polylines.indexOf(line); // TODO bug6090: accelerate by maintaining this mapping in a HashMap?
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