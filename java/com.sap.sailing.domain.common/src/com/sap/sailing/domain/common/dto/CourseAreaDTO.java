package com.sap.sailing.domain.common.dto;

import java.util.UUID;

import com.sap.sse.security.shared.dto.NamedDTO;

/**
 * Equality and hash code are based on the course area's {@link #id}.
 */
public class CourseAreaDTO extends NamedDTO {
    private static final long serialVersionUID = -5279690838452265454L;
    public UUID id;
    
    @Deprecated
    CourseAreaDTO() {} // for GWT RPC serialization only

    public CourseAreaDTO(String name) {
        super(name);
    }
    
    public UUID getId() {
        return id;
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
