package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseListener;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.impl.NamedImpl;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.util.CourseAsWaypointList;
import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.NamedReentrantReadWriteLock;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;

public class CourseImpl extends NamedImpl implements Course {
    private static final long serialVersionUID = -4280487649617132403L;

    private static final Logger logger = Logger.getLogger(CourseImpl.class.getName());
    
    private final List<Waypoint> waypoints;
    private final Map<Waypoint, Integer> waypointIndexes;
    private final List<Leg> legs;
    private transient Set<CourseListener> listeners;
    private transient NamedReentrantReadWriteLock lock;
    
    public CourseImpl(String name, Iterable<Waypoint> waypoints) {
        super(name);
        lock = new NamedReentrantReadWriteLock("lock for CourseImpl "+name,
                /* fair */ true); // if non-fair, course update may need to wait forever for many concurrent readers
        listeners = new HashSet<CourseListener>();
        this.waypoints = new ArrayList<Waypoint>();
        waypointIndexes = new HashMap<Waypoint, Integer>();
        legs = new ArrayList<Leg>();
        Iterator<Waypoint> waypointIter = waypoints.iterator();
        int i=0;
        if (waypointIter.hasNext()) {
            Waypoint previous = waypointIter.next();
            this.waypoints.add(previous);
            waypointIndexes.put(previous, i++);
            while (waypointIter.hasNext()) {
                Waypoint current = waypointIter.next();
                this.waypoints.add(current);
                int indexOfStartWaypoint = i-1;
                waypointIndexes.put(current, i++);
                Leg leg = new LegImpl(this, indexOfStartWaypoint);
                legs.add(leg);
                previous = current;
            }
        }
        assert this.waypoints.size() == waypointIndexes.size();
    }
    
    @Override
    public void lockForRead() {
        LockUtil.lockForRead(lock);
    }

    @Override
    public void unlockAfterRead() {
        LockUtil.unlockAfterRead(lock);
    }
    
    public void lockForWrite() {
        LockUtil.lockForWrite(lock);
    }
    
    public void unlockAfterWrite() {
        LockUtil.unlockAfterWrite(lock);
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        listeners = new HashSet<>();
        lock = new NamedReentrantReadWriteLock("lock for CourseImpl "+this.getName(), /* fair */ true);
    }
    
    /**
     * Synchronize on this object to avoid concurrent modifications of the underlying waypoints list
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        lockForRead();
        try {
            s.defaultWriteObject();
        } finally {
            unlockAfterRead();
        }
    }
    
    /**
     * For access by {@link LegImpl}
     */
    Waypoint getWaypoint(int i) {
        return waypoints.get(i);
    }
    
    @Override
    public void addWaypoint(int zeroBasedPosition, Waypoint waypointToAdd) {
        LockUtil.lockForWrite(lock);
        try {
            assert !waypoints.contains(waypointToAdd); // no duplicate waypoints allowed
            logger.info("Adding waypoint " + waypointToAdd + " to course '" + getName() + "'");
            waypoints.add(zeroBasedPosition, waypointToAdd);
            Map<Waypoint, Integer> updatesToWaypointIndexes = new HashMap<Waypoint, Integer>();
            updatesToWaypointIndexes.put(waypointToAdd, zeroBasedPosition);
            for (Map.Entry<Waypoint, Integer> e : waypointIndexes.entrySet()) {
                if (e.getValue() >= zeroBasedPosition) {
                    updatesToWaypointIndexes.put(e.getKey(), e.getValue() + 1);
                }
            }
            waypointIndexes.putAll(updatesToWaypointIndexes);
            // legs are "virtual" in that they only contain a waypoint index; adding happens most conveniently by
            // appending a leg with its start waypoint index pointing to the last but one waypoint, leaving all others unchanged
            if (waypoints.size() > 1) {
                legs.add(new LegImpl(this, waypoints.size()-2));
            }
            logger.info("Waypoint " + waypointToAdd + " added to course '" + getName() + "', before notifying listeners");
            notifyListenersWaypointAdded(zeroBasedPosition, waypointToAdd);
            logger.info("Waypoint " + waypointToAdd + " added to course '" + getName() + "', after notifying listeners");
            assert waypoints.size() == waypointIndexes.size();
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }

    @Override
    public void removeWaypoint(int zeroBasedPosition) {
        if (zeroBasedPosition >= 0) {
            Waypoint removedWaypoint;
            LockUtil.lockForWrite(lock);
            try {
                removedWaypoint = waypoints.remove(zeroBasedPosition);
                logger.info("Removing waypoint " + removedWaypoint + " from course '" + getName() + "'");
                waypointIndexes.remove(removedWaypoint);
                Map<Waypoint, Integer> updatesToWaypointIndexes = new HashMap<Waypoint, Integer>();
                for (Map.Entry<Waypoint, Integer> e : waypointIndexes.entrySet()) {
                    if (e.getValue() > zeroBasedPosition) { // only > because the entry with == was just removed
                        updatesToWaypointIndexes.put(e.getKey(), e.getValue() - 1);
                    }
                }
                waypointIndexes.putAll(updatesToWaypointIndexes);
                // the legs are "virtual" only in that they contain a waypoint index; when removing, removing the last is most
                // convenient because all other legs' indices will still be contiguous
                if (!legs.isEmpty()) { // if we had only one waypoint, we didn't have any legs
                    // last waypoint was removed; remove last leg
                    legs.remove(legs.size() - 1);
                }
                logger.info("Waypoint " + removedWaypoint + " removed from course '" + getName() + "', before notifying listeners");
                notifyListenersWaypointRemoved(zeroBasedPosition, removedWaypoint);
                logger.info("Waypoint " + removedWaypoint + " removed from course '" + getName() + "', after notifying listeners");
                assert waypoints.size() == waypointIndexes.size();
            } finally {
                LockUtil.unlockAfterWrite(lock);
            }
        }
    }

    private void notifyListenersWaypointRemoved(int index, Waypoint waypointToRemove) {
        for (CourseListener listener : listeners) {
            try {
                listener.waypointRemoved(index, waypointToRemove);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception while notifying listener about waypoint " + waypointToRemove
                        + " that got removed from course " + this + ": " + e.getMessage());
                logger.throwing(CourseImpl.class.getName(), "notifyListenersWaypointRemoved", e);
            }
        }
    }

    private void notifyListenersWaypointAdded(int zeroBasedPosition, Waypoint waypointToAdd) {
        for (CourseListener listener : listeners) {
            try {
                listener.waypointAdded(zeroBasedPosition, waypointToAdd);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception while notifying listener about waypoint " + waypointToAdd
                        + " that got added to course " + this + ": " + e.getMessage());
                logger.throwing(CourseImpl.class.getName(), "notifyListenersWaypointAdded", e);
            }
        }
    }

    @Override
    public Leg getFirstLeg() {
        return legs.get(0);
    }

    @Override
    public List<Leg> getLegs() {
        lockForRead();
        try {
            return new ArrayList<Leg>(legs);
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public Iterable<Waypoint> getWaypoints() {
        lockForRead();
        try {
            return new ArrayList<Waypoint>(waypoints);
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public String toString() {
        lockForRead();
        try {
            StringBuilder result = new StringBuilder(getName());
            result.append(": ");
            boolean first = true;
            for (Waypoint waypoint : getWaypoints()) {
                if (!first) {
                    result.append(" -> ");
                } else {
                    first = false;
                }
                result.append(waypoint);
            }
            return result.toString();
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public int getIndexOfWaypoint(Waypoint waypoint) {
        lockForRead();
        try {
            int result = -1;
            Integer indexEntry = waypointIndexes.get(waypoint);
            if (indexEntry != null) {
                result = indexEntry;
            }
            return result;
        } finally {
            unlockAfterRead();
        }
    }
    
    private Set<ControlPoint> getControlPoints() {
        lockForRead();
        try {
            Set<ControlPoint> result = new HashSet<ControlPoint>();
            for (Waypoint waypoint : getWaypoints()) {
                result.add(waypoint.getControlPoint());
            }
            return result;
        } finally {
            unlockAfterRead();
        }
    }
    
    private ControlPoint getControlPointForMark(Mark mark) {
        lockForRead();
        try {
            for (ControlPoint controlPoint : getControlPoints()) {
                for (Mark controlPointMark : controlPoint.getMarks()) {
                    if (mark == controlPointMark) {
                        return controlPoint;
                    }
                }
            }
            return null;
        } finally {
            unlockAfterRead();
        }
    }
    
    @Override
    public Iterable<Leg> getLegsAdjacentTo(Mark mark) {
        lockForRead();
        try {
            Set<Leg> result = new HashSet<Leg>();
            ControlPoint controlPointForMark = getControlPointForMark(mark);
            if (controlPointForMark != null) {
                boolean first = true;
                for (Leg leg : getLegs()) {
                    if (first) {
                        if (leg.getFrom().getControlPoint() == controlPointForMark) {
                            result.add(leg);
                        }
                        first = false;
                    }
                    if (leg.getTo().getControlPoint() == controlPointForMark) {
                        result.add(leg);
                    }
                }
            }
            return result;
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public synchronized Waypoint getWaypointForControlPoint(ControlPoint controlPoint, int start) {
        lockForRead();
        try {
            if (start > legs.size()) {
                throw new IllegalArgumentException("Starting to search beyond end of course: " + start + " vs. "
                        + (legs.size() + 1));
            }
            int i = 0;
            for (Waypoint waypoint : getWaypoints()) {
                if (i >= start && waypoint.getControlPoint() == controlPoint) {
                    return waypoint;
                }
                i++;
            }
            return null;
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public Waypoint getFirstWaypoint() {
        lockForRead();
        try {
            return waypoints.isEmpty() ? null : waypoints.get(0);
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public Waypoint getLastWaypoint() {
        lockForRead();
        try {
            return waypoints.isEmpty() ? null : waypoints.get(waypoints.size() - 1);
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    public void addCourseListener(CourseListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeCourseListener(CourseListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void update(List<Pair<ControlPoint, NauticalSide>> newControlPoints, DomainFactory baseDomainFactory) throws PatchFailedException {
        LockUtil.lockForWrite(lock);
        try {
            Iterable<Waypoint> courseWaypoints = getWaypoints();
            List<Waypoint> newWaypointList = new LinkedList<Waypoint>();
            // key existing waypoints by control points and re-use each one at most once during construction of the
            // new waypoint list; since several waypoints can have the same control point, the map goes from
            // control point to List<Waypoint>. The waypoints in the lists are held in the order of their
            // occurrence in courseToUpdate.getWaypoints().
            Map<com.sap.sailing.domain.base.ControlPoint, List<Waypoint>> existingWaypointsByControlPoint =
                    new HashMap<com.sap.sailing.domain.base.ControlPoint, List<Waypoint>>();
            for (Waypoint waypoint : courseWaypoints) {
                List<Waypoint> wpl = existingWaypointsByControlPoint.get(waypoint.getControlPoint());
                if (wpl == null) {
                    wpl = new ArrayList<Waypoint>();
                    existingWaypointsByControlPoint.put(waypoint.getControlPoint(), wpl);
                }
                wpl.add(waypoint);
            }
            for (Pair<ControlPoint, NauticalSide> newDomainControlPoint : newControlPoints) {
                List<Waypoint> waypoints = existingWaypointsByControlPoint.get(newDomainControlPoint.getA());
                Waypoint waypoint;
                if (waypoints == null || waypoints.isEmpty()) {
                    // must be a new control point for which we don't have a waypoint yet
                    waypoint = baseDomainFactory.createWaypoint(newDomainControlPoint.getA(), newDomainControlPoint.getB());
                } else {
                    waypoint = waypoints.remove(0); // take the first from the list
                }
                newWaypointList.add(waypoint);
            }
            Patch<Waypoint> patch = DiffUtils.diff(courseWaypoints, newWaypointList);
            if (!patch.isEmpty()) {
                logger.info("applying course update " + patch + " to course " + this);
                CourseAsWaypointList courseAsWaypointList = new CourseAsWaypointList(this);
                patch.applyToInPlace(courseAsWaypointList);
            }
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
    }
    
}
