package com.sap.sailing.domain.common.dto;

import java.util.UUID;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.security.shared.dto.NamedDTO;

/**
 * Equality and hash code are based on the course area's {@link #id}.
 */
public class CourseAreaDTO extends NamedDTO {
    private static final long serialVersionUID = -5279690838452265454L;
    private UUID id;
    private Position centerPosition;
    private Distance radius;
    
    @Deprecated
    CourseAreaDTO() {} // for GWT RPC serialization only

    public CourseAreaDTO(UUID id, String name) {
        this(id, name, /* centerPosition */ null, /* radius */ null);
    }
    
    public CourseAreaDTO(UUID id, String name, Position centerPosition, Distance radius) {
        super(name);
        this.id = id;
        this.centerPosition = centerPosition;
        this.radius = radius;
    }
    
    public UUID getId() {
        return id;
    }

    public Position getCenterPosition() {
        return centerPosition;
    }

    public Distance getRadius() {
        return radius;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime * ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        CourseAreaDTO other = (CourseAreaDTO) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
