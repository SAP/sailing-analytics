package com.sap.sailing.domain.base.impl;

import java.util.UUID;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.IsManagedBySharedDomainFactory;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.impl.NamedImpl;

public class CourseAreaImpl extends NamedImpl implements CourseArea {
    private static final long serialVersionUID = 5912385360170509150L;

    private final UUID id;

    public CourseAreaImpl(String name, UUID id) {
        super(name);
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public IsManagedBySharedDomainFactory resolve(SharedDomainFactory domainFactory) {
        return domainFactory.getOrCreateCourseArea(id, getName());
    }
}
