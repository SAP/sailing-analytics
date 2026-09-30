package com.sap.sailing.domain.common.quadtree;

import java.io.Serializable;

import com.sap.sailing.domain.common.quadtree.impl.Node;
import com.sap.sailing.domain.common.quadtree.impl.Node.GetResult;
import com.sap.sse.common.Bounds;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.BoundsImpl;
import com.sap.sse.common.impl.DegreePosition;

/**
 * A spatial data structure that provides efficient (O(log n)) access to nearest neighbors and
 * to objects in a certain radius around another object. The location of objects is provided as
 * {@link Position}.
 * 
 * @param <T>
 *            type of object stored by coordinates
 * @author Axel Uhl (D043530)
 */
public class QuadTree<T> implements Serializable {
    private static final long serialVersionUID = -5500716775749946017L;

    private final Node<T> root;
    
    private final static int DEFAULT_MAX_NODE_ITEMS = 20;
    
    public QuadTree() {
        this(new BoundsImpl(new DegreePosition(-90.0, -180.0), new DegreePosition(90.0, 180.0)), DEFAULT_MAX_NODE_ITEMS);
    }

    public QuadTree(Position southWest, Position northEast, int maxItems) {
        this(new BoundsImpl(southWest, northEast), maxItems);
    }

    private QuadTree(Bounds bounds, int maxItems) {
        root = new Node<T>(bounds, maxItems);
    }

    public T put(Position point, T obj) {
        return root.put(point, obj);
    }

    public T remove(Position point, T obj) {
        return root.remove(point);
    }
    
    /**
     * Remove all elements from this tree.
     */
    public void clear() {
        root.clear();
    }

    /**
     * Get the value nearest to <code>point</code>. If the tree is empty, <code>null</code> is returned. Distance
     * is calculated using the method {@link #getLatLngDistance(Position, Position)} which is an approximation only,
     * based on Euklidian geometry with the latitude/longitude values.
     */
    public T get(Position point) {
        final GetResult<T> result = root.get(point);
        return result == null ? null : result.getValue();
    }

    /**
     * Get the value closest to <code>point</code>, within a maximum distance, where distance
     * is computed by the rules of {@link #getLatLngDistance(Position, Position)}. If no key
     * is found within that distance, <code>null</code> is returned.
     * 
     * @param withinDistance
     *            maximum get distance. The distance is given as the square root of the sum of the squares of the
     *            latitude and longitude differences, respectively. It therefore does not correspond to any distance in
     *            meters or any euclidian distance at all. However, it should be good enough (at least outside the polar
     *            regions, and in particular for smaller regions), and in particular to find <em>minimum</em> distances.
     *            See {@link #getLatLngDistance(Position, Position)}.
     * @return the object that was found, null if nothing is within the maximum distance.
     */
    public T get(Position point, double withinDistance) {
        final GetResult<T> result = root.get(point, withinDistance);
        return result == null ? null : result.getValue();
    }

    /**
     * Get all values withing the <code>bounds</code>
     */
    public Iterable<T> get(Bounds bounds) {
        return root.get(bounds);
    }

    /**
     * Calculates an approximated "distance" between two lat/lng points by interpreting the coordinates as a euclidian
     * and doing the "sqrt thing"
     */
    public static double getLatLngDistance(Position a, Position b) {
        final double dy = a.getLatDeg() - b.getLatDeg();
        final double dx = (a.getLngDeg() - b.getLngDeg());
        final double dxWrapped = Math.abs(dx) <= 180 ? dx : 360.-Math.abs(dx);
        double distance = Math.sqrt(dy * dy + dxWrapped * dxWrapped);
        return distance;
    }
}