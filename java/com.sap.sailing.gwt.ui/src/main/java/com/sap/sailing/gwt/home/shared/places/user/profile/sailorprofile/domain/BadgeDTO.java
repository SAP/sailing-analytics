package com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.domain;

import java.util.UUID;

public class BadgeDTO {

    private final UUID key;
    private final String name;

    public BadgeDTO(UUID key, String name) {
        super();
        this.key = key;
        this.name = name;
    }

    public UUID getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
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
        BadgeDTO other = (BadgeDTO) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

}
