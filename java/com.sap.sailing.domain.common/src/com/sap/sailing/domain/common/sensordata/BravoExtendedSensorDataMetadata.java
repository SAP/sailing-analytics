package com.sap.sailing.domain.common.sensordata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sap.sailing.domain.common.tracking.DoubleVectorFix;

/**
 * Metadata that defines the column structure of BravoExtended fixes when imported as
 * {@link com.sap.sailing.domain.common.tracking.DoubleVectorFix}.
 * <p>
 * 
 * The current implementation only stores a subset of the information available during the import.
 * <p>
 * 
 * The first columns need to be consistent with {@link BravoSensorDataMetadata}. Furthermore,
 * once data has been stored in a sensor store, the order of literals must remain constant,
 * and no literals must be deleted anymore because the ordinal is used to determine the index
 * into the {@link DoubleVectorFix}.
 */
public enum BravoExtendedSensorDataMetadata implements ColumnMetadata {
    // START ----- DONT CHANGE UNLESS BravoSensorDataMetadata CHANGES TOO ------
    RIDE_HEIGHT_PORT_HULL("RideHeightPort", /* expectedInLogfile */ true), //
    RIDE_HEIGHT_STBD_HULL("RideHeightStbd", /* expectedInLogfile */ true), //
    HEEL("Heel", /* expectedInLogfile */ true), //
    PITCH("PitchRate", /* expectedInLogfile */ true), //
    // END   ----- DONT CHANGE UNLESS BravoSensorDataMetadata CHANGES TOO ------
    
    DB_RAKE_PORT("DaggerBoardRakeAnglePort", /* expectedInLogfile */ true), //
    DB_RAKE_STBD("DaggerBoardRakeAngleStbd", /* expectedInLogfile */ true), //
    RUDDER_RAKE_PORT("RudderRakeAnglePort", /* expectedInLogfile */ true), //
    RUDDER_RAKE_STBD("RudderRakeAngleStbd", /* expectedInLogfile */ true), //
    MAST_ROTATION("MastRotation", /* expectedInLogfile */ true), //
    LEEWAY(null, /* expectedInLogfile */ false), //
    SET(null, /* expectedInLogfile */ false), //
    DRIFT(null, /* expectedInLogfile */ false), //
    DEPTH(null, /* expectedInLogfile */ false), //
    RUDDER(null, /* expectedInLogfile */ false), //
    FORESTAY_LOAD(null, /* expectedInLogfile */ false), //
    FORESTAY_PRESSURE(null, /* expectedInLogfile */ false), //
    TACK_ANGLE(null, /* expectedInLogfile */ false), //
    RAKE_DEG(null, /* expectedInLogfile */ false), //
    DEFLECTOR_PERCENTAGE(null, /* expectedInLogfile */ false), //
    TARGET_HEEL(null, /* expectedInLogfile */ false), //
    DEFLECTOR_MILLIMETERS(null, /* expectedInLogfile */ false), //
    TARGET_BOATSPEED_P(null, /* expectedInLogfile */ false), //
    LAT("Lat", /* expectedInLogfile */ true), //
    LON("Lon", /* expectedInLogfile */ true), //
    COG("COG", /* expectedInLogfile */ true), //
    SOG("SOG", /* expectedInLogfile */ true), //
    EXPEDITION_AWA(null, /* expectedInLogfile */ false), //
    EXPEDITION_AWS(null, /* expectedInLogfile */ false), //
    EXPEDITION_TWA(null, /* expectedInLogfile */ false), //
    EXPEDITION_TWS(null, /* expectedInLogfile */ false), //
    EXPEDITION_TWD(null, /* expectedInLogfile */ false), //
    EXPEDITION_BSP(null, /* expectedInLogfile */ false), //
    EXPEDITION_BSP_TR(null, /* expectedInLogfile */ false), //
    EXPEDITION_SOG(null, /* expectedInLogfile */ false), //
    EXPEDITION_COG(null, /* expectedInLogfile */ false), //
    EXPEDITION_FORESTAY(null, /* expectedInLogfile */ false), //
    EXPEDITION_RAKE(null, /* expectedInLogfile */ false), //
    EXPEDITION_HDG(null, /* expectedInLogfile */ false), //
    EXPEDITION_HEEL(null, /* expectedInLogfile */ false), //
    EXPEDITION_TGHEEL(null, /* expectedInLogfile */ false), //
    EXPEDITION_TMTOGUN(null, /* expectedInLogfile */ false), //
    EXPEDITION_TMTOBURN(null, /* expectedInLogfile */ false),  //
    EXPEDITION_BELOWLN(null, /* expectedInLogfile */ false), //
    EXPEDITION_BARO(null, /* expectedInLogfile */ false), //
    EXPEDITION_LOAD_P(null, /* expectedInLogfile */ false), //
    EXPEDITION_LOAD_S(null, /* expectedInLogfile */ false), //
    EXPEDITION_JIB_CAR_PORT(null, /* expectedInLogfile */ false), //
    EXPEDITION_JIB_CAR_STBD(null, /* expectedInLogfile */ false), //
    EXPEDITION_MAST_BUTT(null, /* expectedInLogfile */ false), //
    EXPEDITION_RATE_OF_TURN(null, /* expectedInLogfile */ false), //
    EXPEDITION_KICKER_TENSION(null, /* expecedInLogfile */ false), //
    ;

    private final String columnName;
    
    /**
     * Some values seem to only appear in the UDP stream but so far not in the log file. Therefore,
     * those columns shall not be part of the response to {@link #getColumnNamesToIndexInDoubleFix()},
     * indicated by this field being {@code false} for those attributes.
     */
    private final boolean expectedInLogFile;

    private BravoExtendedSensorDataMetadata(String columnName, boolean expectedInLogfile) {
        this.columnName = columnName;
        this.expectedInLogFile = expectedInLogfile;
    }
    
    public String getColumnName() {
        return columnName;
    }

    public boolean isExpectedInLogFile() {
        return expectedInLogFile;
    }

    /**
     * The index in the {@link DoubleVectorFix} where this data item will be stored
     */
    public int getColumnIndex() {
        return this.ordinal();
    }

    public static final int HEADER_COLUMN_OFFSET = 3;

    public static BravoExtendedSensorDataMetadata byColumnName(String valueName) {
        BravoExtendedSensorDataMetadata[] values = BravoExtendedSensorDataMetadata.values();
        for (BravoExtendedSensorDataMetadata item : values) {
            if (Objects.equals(item.getColumnName(), valueName)) {
                return item;
            }
        }
        return null;
    }

    public static int getTrackColumnCount() {
        int result = 0;
        for (final BravoExtendedSensorDataMetadata m : BravoExtendedSensorDataMetadata.values()) {
            if (m.isExpectedInLogFile()) {
                result++;
            }
        }
        return result;
    }

    /**
     * @return namens of those columns with {@link #isExpectedInLogFile()} being {@code true}, mapped to their
     *         {@link #getColumnIndex() column index}
     */
    public static Map<String, Integer> getColumnNamesToIndexInDoubleFix() {
        final Map<String, Integer> columnNamesToIndexInDoubleFix = new HashMap<>();
        for (final BravoExtendedSensorDataMetadata column : BravoExtendedSensorDataMetadata.values()) {
            if (column.isExpectedInLogFile()) {
                columnNamesToIndexInDoubleFix.put(column.getColumnName(), column.getColumnIndex());
            }
        }
        return columnNamesToIndexInDoubleFix;
    }
}
