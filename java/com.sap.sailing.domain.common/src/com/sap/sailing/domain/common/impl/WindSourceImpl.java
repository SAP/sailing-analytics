package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;

public class WindSourceImpl implements WindSource {
    private static final long serialVersionUID = 1695490791311456007L;

    private final WindSourceType type;

    public WindSourceImpl(WindSourceType type) {
        this.type = type;
    }

    @Override
    public WindSourceType getType() {
        return type;
    }

    @Override
    public boolean canBeStored() {
        return getType().canBeStored();
    }

    @Override
    public String name() {
        return getType().name();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WindSourceImpl other = (WindSourceImpl) obj;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public Object getId() {
        return null;
    }

    @Override
    public String toString() {
        return getTypeAndId();
    }
}
