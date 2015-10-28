package com.sap.sse.datamining.shared.impl.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FunctionDTO implements Serializable, Comparable<FunctionDTO> {
    private static final long serialVersionUID = 4587389541910498505L;

    private boolean isDimension;
    private String functionName;
    private String sourceTypeName;
    private String returnTypeName;
    private List<String> parameterTypeNames;

    private String displayName;
    private int ordinal;

    /**
     * Constructor for the GWT-Serialization. Don't use this!
     */
    @Deprecated
    FunctionDTO() {
    }
    
    public FunctionDTO(boolean isDimension, String functionName, String sourceTypeName, String returnTypeName,
                           List<String> parameterTypeNames, String displayName, int ordinal) {
        this.isDimension = isDimension;
        this.functionName = functionName;
        this.sourceTypeName = sourceTypeName;
        this.returnTypeName = returnTypeName;
        this.parameterTypeNames = new ArrayList<String>(parameterTypeNames);
        
        this.displayName = displayName;
        this.ordinal = ordinal;
    }

    public String getSourceTypeName() {
        return sourceTypeName;
    }

    public String getReturnTypeName() {
        return returnTypeName;
    }

    public List<String> getParameterTypeNames() {
        return parameterTypeNames;
    }
    
    public String getFunctionName() {
        return functionName;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isDimension() {
        return isDimension;
    }
    
    public int getOrdinal() {
        return ordinal;
    }
    
    @Override
    public int compareTo(FunctionDTO f) {
        return Integer.compare(this.getOrdinal(), f.getOrdinal());
    }
    
    public String toString() {
        return (isDimension() ? "Dimension " : "Function ") + getSourceTypeName() + "." + getFunctionName() + " : " + getReturnTypeName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((functionName == null) ? 0 : functionName.hashCode());
        result = prime * result + (isDimension ? 1231 : 1237);
        result = prime * result + ((parameterTypeNames == null) ? 0 : parameterTypeNames.hashCode());
        result = prime * result + ((returnTypeName == null) ? 0 : returnTypeName.hashCode());
        result = prime * result + ((sourceTypeName == null) ? 0 : sourceTypeName.hashCode());
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
        FunctionDTO other = (FunctionDTO) obj;
        if (functionName == null) {
            if (other.functionName != null)
                return false;
        } else if (!functionName.equals(other.functionName))
            return false;
        if (isDimension != other.isDimension)
            return false;
        if (parameterTypeNames == null) {
            if (other.parameterTypeNames != null)
                return false;
        } else if (!parameterTypeNames.equals(other.parameterTypeNames))
            return false;
        if (returnTypeName == null) {
            if (other.returnTypeName != null)
                return false;
        } else if (!returnTypeName.equals(other.returnTypeName))
            return false;
        if (sourceTypeName == null) {
            if (other.sourceTypeName != null)
                return false;
        } else if (!sourceTypeName.equals(other.sourceTypeName))
            return false;
        return true;
    }

}
