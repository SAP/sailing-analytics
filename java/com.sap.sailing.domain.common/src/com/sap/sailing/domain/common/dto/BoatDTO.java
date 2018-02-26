package com.sap.sailing.domain.common.dto;

import java.io.Serializable;

import com.sap.sailing.domain.common.racelog.tracking.MappableToDevice;
import com.sap.sse.common.Color;

public class BoatDTO extends NamedDTO implements Serializable, MappableToDevice {
    private static final long serialVersionUID = -4076992788294272162L;

    private String idAsString;
    private BoatClassDTO boatClass;
    private String sailId;
    private Color color;

    // for GWT
    public BoatDTO() {}

    public BoatDTO(String idAsString, String name, BoatClassDTO boatClass, String sailId) {
        this(idAsString, name, boatClass, sailId, null);
    }

    public BoatDTO(String idAsString, String name, BoatClassDTO boatClass, String sailId, Color color) {
        super(name);
        this.idAsString = idAsString;
        this.boatClass = boatClass;
        this.sailId = sailId;
        this.color = color;
    }

    @Override
    public String getIdAsString() {
        // FIXME Bug 2822 -> proper implementation
        return null;
    }

    public Color getColor() {
        return color;
    }

    public String getSailId() {
        return sailId;
    }
    
    public String getIdAsString() {
        return idAsString;
    }

    public BoatClassDTO getBoatClass() {
        return boatClass;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((boatClass == null) ? 0 : boatClass.hashCode());
        result = prime * result + ((idAsString == null) ? 0 : idAsString.hashCode());
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result + ((sailId == null) ? 0 : sailId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        BoatDTO other = (BoatDTO) obj;
        if (boatClass == null) {
            if (other.boatClass != null)
                return false;
        } else if (!boatClass.equals(other.boatClass))
            return false;
        if (idAsString == null) {
            if (other.idAsString != null)
                return false;
        } else if (!idAsString.equals(other.idAsString))
            return false;
        if (color == null) {
            if (other.color != null)
                return false;
        } else if (!color.equals(other.color))
            return false;
        if (sailId == null) {
            if (other.sailId != null)
                return false;
        } else if (!sailId.equals(other.sailId))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return getName() == null ? (getBoatClass().getName() + " / " + getSailId()) : getName();
    }
}
