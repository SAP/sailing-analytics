package com.sap.sailing.gwt.ui.shared;


/**
 * Equality and hash code are defined based on Java object identity
 * 
 * @author Axel Uhl (D043530)
 *
 */
public abstract class ControlPointDTO extends NamedDTO {
    private String idAsString;
    
    public ControlPointDTO() {}
    
    public ControlPointDTO(String idAsString, String name) {
        super(name);
        this.idAsString = idAsString;
    }
    
    public String getIdAsString() {
        return idAsString;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
    
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
    
    public abstract Iterable<MarkDTO> getMarks();
}
