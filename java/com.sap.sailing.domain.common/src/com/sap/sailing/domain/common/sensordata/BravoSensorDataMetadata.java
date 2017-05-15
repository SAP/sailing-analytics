package com.sap.sailing.domain.common.sensordata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Metadata that defines the column structure of {@link com.sap.sailing.domain.common.tracking.DoubleVectorFix}es when
 * imported.
 * 
 * The current implementation only stores a subset of the information available during the import.
 */
public enum BravoSensorDataMetadata {

    RIDE_HEIGHT_PORT_HULL("RideHeightPortHull"), //
    RIDE_HEIGHT_STBD_HULL("RideHeightStbdHull"), //
    HEEL("Heel"), //
    PITCH("ImuSensor_Pitch");


    private String columnName;
    
    private BravoSensorDataMetadata(String columnName) {
        this.columnName = columnName;
    }
    
    public String getColumnName() {
        return columnName;
    }

    public int getColumnIndex() {
        return this.ordinal();
    }

    public static final int HEADER_COLUMN_OFFSET = 3;

    public static BravoSensorDataMetadata byColumnName(String valueName) {
        BravoSensorDataMetadata[] values = BravoSensorDataMetadata.values();
        for (BravoSensorDataMetadata item : values) {
            if (Objects.equals(item.getColumnName(), valueName)) {
                return item;
            }
        }
        return null;
    }

    public static int getTrackColumnCount() {
        return BravoSensorDataMetadata.values().length;
    }

    public static List<String> getTrackColumnNames() {
        ArrayList<String> colNames = new ArrayList<>(getTrackColumnCount());
        for (BravoSensorDataMetadata item : BravoSensorDataMetadata.values()) {
            colNames.add(item.columnName);
        }
        return colNames;
    }
}
