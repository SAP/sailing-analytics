package com.sap.sailing.gwt.ui.shared;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sap.sailing.domain.common.racelog.tracking.MappableToDevice;

public class TypedDeviceMappingDTO extends DeviceMappingDTO {
    
    public String dataType;
    
    @Deprecated // for GWT serialization only
    protected TypedDeviceMappingDTO() {}
    
    public TypedDeviceMappingDTO(DeviceIdentifierDTO deviceId, Date from, Date to, MappableToDevice mappedTo,
            List<UUID> originalRaceLogEventIds, String dataType) {
        super(deviceId, from, to, mappedTo, originalRaceLogEventIds);
        this.dataType = dataType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
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
        TypedDeviceMappingDTO other = (TypedDeviceMappingDTO) obj;
        if (dataType != other.dataType)
            return false;
        return true;
    }
    
}
