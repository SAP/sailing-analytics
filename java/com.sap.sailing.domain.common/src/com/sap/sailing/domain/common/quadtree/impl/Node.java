package com.sap.sailing.domain.common.quadtree.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sailing.domain.common.quadtree.QuadTree;
import com.sap.sse.common.Bounds;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.BoundsImpl;
import com.sap.sse.common.impl.DegreePosition;

/**
 * A node in a {@link QuadTree}. There may be internal nodes that have no elements in them but have exactly
 * four child nodes, or leaf nodes that have no children but contain item elements.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <T>
 */
public class Node<T> {
    // TODO remove when done
    public static int visitedItems;
    public static int visitedNodes;
    public static int skippedNodes;
    
    /**
     * Either an array of exactly four child nodes which are then all non-<code>null</code>, or <code>null</code>,
     * meaning that this node is a child node, having no children but potentially having items. A node that has children
     * has no items. Quadrants are numbered as usual in geometry. See {@link #NE}, {@link #NW}, {@link #SW} and
     * {@link #SE}.
     */
    private Node<T>[] children;
    
    private Map<Position, T> items;
    
    private final Bounds bounds;
    
    /**
     * The maximum number of items to hold in {@link #items}. If {@link #put(Position, Object) adding} an item to this node
     * would increase the item collection's size beyond this number, the node is {@link #split() split} into four new leaf
     * nodes, and its items are distributed across those new leaf nodes. This number must be a positive integer.
     */
    private final int maxItems;
    
    /**
     * The quadrant index in {@link #children} for the north-east subtree
     */
    private final int NE = 0;

    /**
     * The quadrant index in {@link #children} for the north-west subtree
     */
    private final int NW = 1;
    
    /**
     * The quadrant index in {@link #children} for the south-west subtree
     */
    private final int SW = 2;
    
    /**
     * The quadrant index in {@link #children} for the south-east subtree
     */
    private final int SE = 3;

    /**
     * Creates a new node with the <code>bounds</code> as specified. The node starts out empty, as a leaf node that has
     * an empty set of items.
     * 
     * @param bounds
     *            must not have a north latitude less than the south latitude, or an {@link IllegalArgumentException}
     *            will result
     */
    public Node(Bounds bounds, int maxItems) {
        if (bounds.getNorthEast().getLatDeg() < bounds.getSouthEast().getLatDeg()) {
            throw new IllegalArgumentException("North border of bounds "+bounds+" is further south than its south border");
        }
        if (maxItems <= 0) {
            throw new IllegalArgumentException("Maximum number of items must be positive but was "+maxItems);
        }
        this.maxItems = maxItems;
        this.bounds = bounds;
        items = new HashMap<>(maxItems);
    }

    private void split() {
        assert children == null;
        assert items != null;
        createChildren();
        distributeItemsToChildren();
        assert children != null;
        assert items == null;
    }

    private void distributeItemsToChildren() {
        assert items != null;
        assert children != null;
        final Map<Position, T> myItems = items;
        items = null;
        for (final Entry<Position, T> item : myItems.entrySet()) {
            getChild(item.getKey()).put(item.getKey(), item.getValue());
        }
        assert items == null;
    }

    private Node<T> getChild(Position key) {
        assert children != null;
        assert items == null;
        assert bounds.contains(key);
        for (final Node<T> child : children) {
            if (child.bounds.contains(key)) {
                return child;
            }
        }
        throw new RuntimeException("Internal error: position "+key+" is within node bounds "+bounds+" but no child contains it");
    }
    
    /**
     * Adds the <code>value</code> to this node and ensures that the node still meets the requirements regarding size.
     * If necessary, the node is split with its items distributed across the new children. If a value already existed at
     * position <code>key</code>, it is replaced.
     * 
     * @param key
     *            the position at which to insert <code>value</code>. Must be contained in this node's {@link #bounds}.
     *            If it is not, an {@link IllegalArgumentException} will be thrown. Must not be <code>null</code>.
     * @return the value previously at position <code>key</code> or <code>null</code> if there was no value at that
     *         position.
     */
    public T put(Position key, T value) {
        if (value == null) {
            throw new NullPointerException("Cannot insert null values into this node");
        }
        if (key == null) {
            throw new NullPointerException("null keys not allowed");
        }
        if (!bounds.contains(key)) {
            throw new IllegalArgumentException("key "+key+" must be within this node's bounds "+bounds);
        }
        final T result;
        if (items != null) {
            result = items.put(key, value);
            if (result == null) {
                // the size of this node has increased by one; check size constraint
                if (items.size() > maxItems) {
                    split();
                }
            }
        } else {
            result = getChild(key).put(key, value);
        }
        return result;
    }
    
    /**
     * Removes the element at position <code>key</code> from this node or any child nodes if such an element exists
     * 
     * @return the value removed, or <code>null</code> if no element existed at position <code>key</code> in this node
     *         or any of its children
     */
    public T remove(Position key) {
        final T result;
        if (key != null) {
            if (items != null) {
                result = items.remove(key);
            } else {
                result = getChild(key).remove(key);
            }
        } else {
            result = null;
        }
        return result;
    }

    private void createChildren() {
        assert children == null;
        @SuppressWarnings("unchecked")
        final Node<T>[] newChildren = (Node<T>[]) new Node<?>[4];
        children = newChildren;
        final Position middleWest = new DegreePosition((bounds.getSouthWest().getLatDeg() + bounds.getNorthEast().getLatDeg())/2.,
                bounds.getSouthWest().getLngDeg());
        final Position southCenter = new DegreePosition(bounds.getSouthWest().getLatDeg(),
                (bounds.getNorthEast().getLngDeg() + bounds.getSouthWest().getLngDeg()) / 2. -
                // adjust for date line crossing if necessary
                        (bounds.getNorthEast().getLngDeg() >= bounds.getSouthWest().getLngDeg() ? 0. : 360.));
        final Position middleCenter = new DegreePosition(middleWest.getLatDeg(), southCenter.getLngDeg());
        final Position middleEast = new DegreePosition(middleWest.getLatDeg(), bounds.getNorthEast().getLngDeg());
        final Position northCenter = new DegreePosition(bounds.getNorthEast().getLatDeg(), southCenter.getLngDeg());
        children[NE] = new Node<T>(new BoundsImpl(middleCenter, bounds.getNorthEast()), maxItems);
        children[NW] = new Node<T>(new BoundsImpl(middleWest, northCenter), maxItems);
        children[SW] = new Node<T>(new BoundsImpl(bounds.getSouthWest(), middleCenter), maxItems);
        children[SE] = new Node<T>(new BoundsImpl(southCenter, middleEast), maxItems);
        assert children != null;
        assert children.length == 4;
        assert children[NE] != null && children[NW] != null && children[SW] != null && children[SE] != null;
    }

    /**
     * Remove all elements from this node by either removing all its items locally or by removing all children
     * and converting this back into a leaf node.
     */
    public void clear() {
        if (items != null) {
            items.clear();
        } else {
            items = new HashMap<>();
            children = null;
        }
    }
    
    public static class GetResult<T> {
        private final Position position;
        private final T value;
        private final double distance;
        public GetResult(Position position, T value, double distance) {
            super();
            this.position = position;
            this.value = value;
            this.distance = distance;
        }
        public Position getPosition() {
            return position;
        }
        public T getValue() {
            return value;
        }
        public double getDistance() {
            return distance;
        }
        @Override
        public String toString() {
            return "GetResult [position=" + position + ", value=" + value + ", distance=" + distance + "]";
        }
    }

    /**
     * Get the value nearest to <code>point</code>. If the node is empty, <code>null</code> is returned. Distance is
     * calculated using the method {@link QuadTree#getLatLngDistance(Position, Position)} which is an approximation
     * only, based on Euklidian geometry with the latitude/longitude values.
     * <p>
     * 
     * If this is a leaf node, the nearest key by the definition above is used to determine the corresponding value and
     * return it. Otherwise, the children are traversed. For the first child, the nearest key is determined recursively.
     * Other children only need to be traversed if their bounds are closer to <code>point</code> than the key found so
     * far.
     * 
     * @param withinDistance
     *            a key is only considered if it has less than this {@link QuadTree#getLatLngDistance(Position, Position)
     *            distance} to <code>point</code>
     */
    public GetResult<T> get(Position point, double withinDistance) {
        assert (items == null) != (children == null);
        visitedNodes++;
        GetResult<T> result = null;
        double minDistance = withinDistance;
        if (items != null) {
            for (final Entry<Position, T> item : items.entrySet()) {
                double itemDistanceToPoint = QuadTree.getLatLngDistance(point, item.getKey());
                visitedItems++;
                if (itemDistanceToPoint < minDistance) {
                    result = new GetResult<T>(item.getKey(), item.getValue(), itemDistanceToPoint);
                    minDistance = itemDistanceToPoint;
                }
            }
        } else {
            // Traverse children in order of ascending distance, making it more likely to find good, near
            // candidates early, hence enabling skipping more children, investigating fewer items. However,
            // doing the full java.util.Collections / Arrays sort thing with Comparators and such is way too
            // expensive as measurements have shown, and the overhead for just the four elements is too high.
            // It is much more efficient to traverse the small children array multiple times and in each pass
            // remembering the next minimal child distance so that during the next pass those children can be
            // inspected. This process repeats until all children have been handled, taking at most four passes.
            final double[] childDistanceToPoint = new double[4];
            double minChildDistanceToPoint = Double.MAX_VALUE; // minimal distance of those children not yet done
            for (int i=0; i<4; i++) {
                childDistanceToPoint[i] = children[i].getDistance(point);
                if (childDistanceToPoint[i] < minChildDistanceToPoint) {
                    minChildDistanceToPoint = childDistanceToPoint[i];
                }
            }
            final boolean done[] = new boolean[4]; // have we handled that child already (skipped or searched)?
            int doneCount = 0;
            while (doneCount < 4) {
                double nextMinChildDistanceToPoint = Double.MAX_VALUE;
                for (int i=0; doneCount<4 && i<4; i++) {
                    if (!done[i]) {
                        if (childDistanceToPoint[i] == minChildDistanceToPoint) {
                            final Node<T> child = children[i];
                            // If we have a possible result already, only investigate the child node if it is nearer to point
                            // than the result candidate and not an empty leaf node (which can be checked more quickly than
                            // performing the rather expensive distance calculation)
                            if (!child.isEmptyLeaf() && childDistanceToPoint[i] < minDistance) {
                                assert minDistance <= withinDistance;
                                final GetResult<T> childResult = child.get(point, minDistance);
                                if (childResult != null) {
                                    if (childResult.getDistance() < minDistance) {
                                        result = childResult;
                                        minDistance = childResult.getDistance();
                                    }
                                }
                            } else {
                                skippedNodes++;
                            }
                            done[i] = true;
                            doneCount++;
                        } else {
                            if (childDistanceToPoint[i] < nextMinChildDistanceToPoint) {
                                nextMinChildDistanceToPoint = childDistanceToPoint[i]; // the distance to handle in the next pass
                            }
                        }
                    }
                }
                minChildDistanceToPoint = nextMinChildDistanceToPoint;
            }
        }
        return result;
    }

    /**
     * Get the value nearest to <code>point</code>. If the node is empty, <code>null</code> is returned. Distance is
     * calculated using the method {@link QuadTree#getLatLngDistance(Position, Position)} which is an approximation
     * only, based on Euklidian geometry with the latitude/longitude values.
     * <p>
     * 
     * If this is a leaf node, the nearest key by the definition above is used to determine the corresponding value and
     * return it. Otherwise, the children are traversed. For the first child, the nearest key is determined recursively.
     * Other children only need to be traversed if their bounds are closer to <code>point</code> than the key found so
     * far.
     */
    public GetResult<T> get(Position point) {
        return get(point, Double.MAX_VALUE);
    }
    
    /**
     * To avoid expensive distance checks, a shortcut can be to check whether this node is an empty
     * leaf node. This is the case if {@link #items} is not <code>null</code> (indicating a leaf node)
     * but empty.
     */
    private boolean isEmptyLeaf() {
        return items != null && items.isEmpty();
    }

    /**
     * Computes a "distance" of this node's {@link #bounds} to the <code>point</code> based on the distance definition
     * provided by {@link QuadTree#getLatLngDistance(Position, Position)}. If <code>point</code> is
     * {@link Bounds#contains(Position) contained} in this node's {@link #bounds}, the distance returned is
     * <code>0</code>. Otherwise, the distance to the nearest border of this node's {@link #bounds} is determined.
     */
    protected double getDistance(Position point) {
        final double result;
        if (bounds.contains(point)) {
            result = 0;
        } else {
            // Find out the point on the border defined by bounds that is nearest to point.
            final double latDegNearestOnBorder =
                    point.getLatDeg() > bounds.getNorthEast().getLatDeg() ? bounds.getNorthEast().getLatDeg() :
                        point.getLatDeg() < bounds.getSouthWest().getLatDeg() ? bounds.getSouthWest().getLatDeg() : point.getLatDeg();
            // Now construct a Bounds object of zero latitude span at point's latitude and with the longitude span
            // copied from this Node's bounds. If these bounds contain point, use point's longitude as it is spanned by
            // this node's bounds. Otherwise, extend the bounds to contain point, then check whether the west or east border
            // was changed to contain point; point was closer to the border that was changed to include point
            final Bounds boundsAtPointLat = new BoundsImpl(new DegreePosition(point.getLatDeg(), bounds.getSouthWest().getLngDeg()),
                                                       new DegreePosition(point.getLatDeg(), bounds.getNorthEast().getLngDeg()));
            final double lngDegNearestOnBorder;
            if (boundsAtPointLat.contains(point)) {
                lngDegNearestOnBorder = point.getLngDeg();
            } else {
                final Bounds boundsAtPointLatExtendedToIncludePoint = boundsAtPointLat.extend(point);
                if (boundsAtPointLatExtendedToIncludePoint.getSouthWest().getLngDeg() == bounds.getSouthWest().getLngDeg()) {
                    lngDegNearestOnBorder = bounds.getNorthEast().getLngDeg();
                } else {
                    lngDegNearestOnBorder = bounds.getSouthWest().getLngDeg();
                }
            }
            result = QuadTree.getLatLngDistance(point, new DegreePosition(latDegNearestOnBorder, lngDegNearestOnBorder));
        }
        return result;
    }

    /**
     * Fetches all values within the <code>bounds</code> specified. Always returns a non-<code>null</code> iterable
     * which may, however, be empty.
     * 
     * @param bounds
     *            It is permissible for <code>bounds</code> to not even intersect with this node's {@link #bounds}.
     *            Then, however, the result will be empty.
     */
    public Iterable<T> get(Bounds bounds) {
        final Iterable<T> result;
        if (this.bounds.intersects(bounds)) {
            Set<T> preResult = new HashSet<>();
            if (items != null) {
                if (bounds.contains(this.bounds)) {
                    // all items must be contained, so no per-item check is required
                    preResult.addAll(items.values());
                } else {
                    for (final Entry<Position, T> item : items.entrySet()) {
                        if (bounds.contains(item.getKey())) {
                            preResult.add(item.getValue());
                        }
                    }
                }
            } else {
                for (final Node<T> child : children) {
                    Util.addAll(child.get(bounds), preResult);
                }
            }
            result = preResult;
        } else {
            result = Collections.emptySet();
        }
        return result;
    }
    
    @Override
    public String toString() {
        return ""+bounds+": "+(items==null?0:items.size())+" items in node, "+
                (children==null?0:children.length)+" child nodes";
    }
}
