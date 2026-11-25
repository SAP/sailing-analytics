package com.sap.sailing.domain.base.impl;

import java.util.UUID;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.NamedImpl;

public class CourseAreaImpl extends NamedImpl implements CourseArea {
    private static final long serialVersionUID = 5912385360170509150L;

    private final UUID id;
    private volatile Position centerPosition;
    private volatile Distance radius;
    // Keep NamedImpl and the original fields for compatibility with serialized course areas.
    // An older serialized instance has no updatedName and continues to use the inherited name.
    private volatile String updatedName;

    public CourseAreaImpl(final String name, final UUID id, final Position centerPosition, final Distance radius) {
        super(name);
        this.id = id;
        this.centerPosition = centerPosition;
        this.radius = radius;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return updatedName == null ? super.getName() : updatedName;
    }

    @Override
    public void update(final String name, final Position centerPosition, final Distance radius) {
        if (name == null) {
            throw new IllegalArgumentException("Course area name must not be null");
        }
        this.updatedName = name;
        this.centerPosition = centerPosition;
        this.radius = radius;
    }

    @Override
    public Position getCenterPosition() {
        return centerPosition;
    }

    @Override
    public Distance getRadius() {
        return radius;
    }

    @Override
    public CourseArea resolve(final SharedDomainFactory<?> domainFactory) {
        return domainFactory.getOrCreateCourseArea(id, getName(), getCenterPosition(), getRadius());
    }
}
