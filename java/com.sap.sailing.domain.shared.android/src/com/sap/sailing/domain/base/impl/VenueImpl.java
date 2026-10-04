package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.VenueListener;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

public class VenueImpl implements Venue {
    private static final long serialVersionUID = 6854152040737643290L;
    private String name;
    private transient Set<VenueListener> listeners;
    
    /**
     * The course areas are ordered because they typically follow an ordered naming pattern borrowed from the
     * "NATO alphabet" (Alpha, Bravo, Charlie, ...).
     */
    private final List<CourseArea> courseAreas;
    
    private NamedReentrantReadWriteLock courseAreasLock;

    public VenueImpl(String name) {
        this.name = name;
        this.listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
        courseAreas = new ArrayList<CourseArea>();
        courseAreasLock = createCourseAreasLock(name);
    }

    @Override
    public void addListener(VenueListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(VenueListener listener) {
        listeners.remove(listener);
    }

    private NamedReentrantReadWriteLock createCourseAreasLock(String name) {
        return new NamedReentrantReadWriteLock("Course Areas for venue "+name, /* fair */ false);
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (courseAreasLock == null) {
            courseAreasLock = createCourseAreasLock(getName());
        }
        this.listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    @Override
    public Iterable<CourseArea> getCourseAreas() {
        LockUtil.lockForRead(courseAreasLock);
        try {
            return Collections.unmodifiableList(courseAreas);
        } finally {
            LockUtil.unlockAfterRead(courseAreasLock);
        }
    }

    @Override
    public void addCourseArea(CourseArea courseArea) {
        LockUtil.lockForWrite(courseAreasLock);
        try {
            courseAreas.add(courseArea);
        } finally {
            LockUtil.unlockAfterWrite(courseAreasLock);
        }
        for (VenueListener listener : listeners) {
            listener.courseAreaAdded(this, courseArea);
        }
    }

    @Override
    public void removeCourseArea(CourseArea courseArea) {
        LockUtil.lockForWrite(courseAreasLock);
        try {
            courseAreas.remove(courseArea);
        } finally {
            LockUtil.unlockAfterWrite(courseAreasLock);
        }
        for (VenueListener listener : listeners) {
            listener.courseAreaRemoved(this, courseArea);
        }
    }
    
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param newName must not be <code>null</code>
     */
    public void setName(String newName) {
        if (newName == null) {
            throw new IllegalArgumentException("An venue name must not be null");
        }
        this.name = newName;
    }
}
