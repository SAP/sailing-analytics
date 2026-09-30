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
    private final Position centerPosition;
    private final Distance radius;

    public CourseAreaImpl(String name, UUID id, Position centerPosition, Distance radius) {
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
    public Position getCenterPosition() {
        return centerPosition;
    }

    @Override
    public Distance getRadius() {
        return radius;
    }

    @Override
    public CourseArea resolve(SharedDomainFactory<?> domainFactory) {
        return domainFactory.getOrCreateCourseArea(id, getName(), getCenterPosition(), getRadius());
    }
}
