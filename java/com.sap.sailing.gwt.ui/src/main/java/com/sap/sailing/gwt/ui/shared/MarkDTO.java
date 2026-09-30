package com.sap.sailing.gwt.ui.shared;

import java.util.Collections;

import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.racelog.tracking.MappableToDevice;
import com.sap.sse.common.Color;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;

public class MarkDTO extends ControlPointDTO implements MappableToDevice {
    private static final long serialVersionUID = -8031499997812618751L;
    public Position position;
    public Color color;
    public String shape;
    public String pattern;
    public MarkType type;

    @Deprecated
    MarkDTO() {} // for GWT RPC serialization only
    
    public MarkDTO(String idAsString, String name, String shortName, double latDeg, double lngDeg) {
        super(idAsString, name, shortName);
        this.position = new DegreePosition(latDeg, lngDeg);
    }

    public MarkDTO(String idAsString, String name, String shortName) {
        super(idAsString, name, shortName);
    }
    
    @Override
    public Iterable<MarkDTO> getMarks() {
        return Collections.singleton(this);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof MarkDTO) {
            MarkDTO other = (MarkDTO) o;
            return getIdAsString() != null && getIdAsString().equals(other.getIdAsString());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return getIdAsString().hashCode();
    }
}
