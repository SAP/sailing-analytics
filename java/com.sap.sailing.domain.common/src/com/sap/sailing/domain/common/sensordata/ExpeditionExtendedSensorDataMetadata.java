package com.sap.sailing.domain.common.sensordata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * The first columns need to be consistent with {@link BravoSensorDataMetadata}.
 */
public enum ExpeditionExtendedSensorDataMetadata {
    HEEL("heel", BravoExtendedSensorDataMetadata.HEEL), //
    TRIM("trim", BravoExtendedSensorDataMetadata.PITCH), //
    LEEWAY("leeway", BravoExtendedSensorDataMetadata.LEEWAY), //
    SET("set", BravoExtendedSensorDataMetadata.SET), //
    DRIFT("drift", BravoExtendedSensorDataMetadata.DRIFT), //
    DEPTH("depth", BravoExtendedSensorDataMetadata.DEPTH), //
    RUDDER("rudder", BravoExtendedSensorDataMetadata.RUDDER), //
    FORESTAY_LOAD("forestay", BravoExtendedSensorDataMetadata.FORESTAY_LOAD), //
    FORESTAY_PRESSURE("forestaypres", BravoExtendedSensorDataMetadata.FORESTAY_PRESSURE), //
    TACK_ANGLE("tackangle", BravoExtendedSensorDataMetadata.TACK_ANGLE), //
    RAKE_DEG("rakedeg", BravoExtendedSensorDataMetadata.RAKE_DEG), //
    DEFLECTOR_PERCENTAGE("dflectrpp", BravoExtendedSensorDataMetadata.DEFLECTOR_PERCENTAGE), //
    TARGET_HEEL("tg Heell", BravoExtendedSensorDataMetadata.TARGET_HEEL), //
    DEFLECTOR_MILLIMETERS("dflectrmm", BravoExtendedSensorDataMetadata.DEFLECTOR_MILLIMETERS), //
    TARGET_BOATSPEED_P("bsptr", BravoExtendedSensorDataMetadata.TARGET_BOATSPEED_P), //
    EXPEDITION_AWA("awa", BravoExtendedSensorDataMetadata.EXPEDITION_AWA), //
    EXPEDITION_AWS("aws", BravoExtendedSensorDataMetadata.EXPEDITION_AWS), //
    EXPEDITION_TWA("twa", BravoExtendedSensorDataMetadata.EXPEDITION_TWA), //
    EXPEDITION_TWS("tws", BravoExtendedSensorDataMetadata.EXPEDITION_TWS), //
    EXPEDITION_TWD("twd", BravoExtendedSensorDataMetadata.EXPEDITION_TWD), //
    EXPEDITION_BSP("bsp", BravoExtendedSensorDataMetadata.EXPEDITION_BSP), //
    EXPEDITION_SOG("sog", BravoExtendedSensorDataMetadata.EXPEDITION_SOG), //
    EXPEDITION_COG("cog", BravoExtendedSensorDataMetadata.EXPEDITION_COG), //
    EXPEDITION_RAKE("rake", BravoExtendedSensorDataMetadata.EXPEDITION_RAKE), //
    EXPEDITION_HDG("hdg", BravoExtendedSensorDataMetadata.EXPEDITION_HDG), //
    EXPEDITION_TMTOGUN("tmtogun", BravoExtendedSensorDataMetadata.EXPEDITION_TMTOGUN), //
    EXPEDITION_TMTOBURN("tmtoburn", BravoExtendedSensorDataMetadata.EXPEDITION_TMTOBURN), //
    EXPEDITION_BELOWLN("belowln", BravoExtendedSensorDataMetadata.EXPEDITION_BELOWLN), //
    EXPEDITION_RATE_OF_TURN("rot", BravoExtendedSensorDataMetadata.EXPEDITION_RATE_OF_TURN), //
    EXPEDITION_BARO("baro", BravoExtendedSensorDataMetadata.EXPEDITION_BARO), //
    EXPEDITION_LOAD_P("load p", BravoExtendedSensorDataMetadata.EXPEDITION_LOAD_P), //
    EXPEDITION_LOAD_S("load s", BravoExtendedSensorDataMetadata.EXPEDITION_LOAD_S), //
    EXPEDITION_JIB_CAR_PORT("jibcarport", BravoExtendedSensorDataMetadata.EXPEDITION_JIB_CAR_PORT), //
    EXPEDITION_JIB_CAR_STBD("jibcarstbd", BravoExtendedSensorDataMetadata.EXPEDITION_JIB_CAR_STBD), //
    EXPEDITION_MAST_BUTT("mastbutt", BravoExtendedSensorDataMetadata.EXPEDITION_MAST_BUTT), //
    EXPEDITION_KICKER_TENSION("kickertension", BravoExtendedSensorDataMetadata.EXPEDITION_KICKER_TENSION), //
    ;

    private String columnName;
    
    /**
     * Expedition extended fixes are mapped to Bravo extended fixes whose metadata is described
     * in {@link BravoExtendedSensorDataMetadata}. When parsing an Expedition file, the columns
     * in the resulting {@link DoubleVectorFix} must be chosen such that they match up with the
     * corresponding fields as described by {@link BravoExtendedSensorDataMetadata}. This turns
     * the ordinals of this enum's literals insignificant; the position is taken solely from
     * the Bravo metadata.
     */
    private final BravoExtendedSensorDataMetadata mappedToBravoField;

    private final boolean expectedInLogfile;
    
    private ExpeditionExtendedSensorDataMetadata(String columnName, BravoExtendedSensorDataMetadata mappedToBravoField) {
        this(columnName, mappedToBravoField, mappedToBravoField.isExpectedInLogFile());
    }
    
    private ExpeditionExtendedSensorDataMetadata(String columnName, BravoExtendedSensorDataMetadata mappedToBravoField, boolean expectedInLogfile) {
        this.columnName = columnName.toLowerCase();
        this.mappedToBravoField = mappedToBravoField;
        this.expectedInLogfile = expectedInLogfile;
    }
    
    public String getColumnName() {
        return columnName;
    }

    /**
     * The index in the {@link DoubleVectorFix} where this data item will be stored
     */
    public int getColumnIndex() {
        return mappedToBravoField.getColumnIndex();
    }

    public boolean isExpectedInLogFile() {
        return expectedInLogfile;
    }

    public static ExpeditionExtendedSensorDataMetadata byColumnName(String valueName) {
        ExpeditionExtendedSensorDataMetadata[] values = ExpeditionExtendedSensorDataMetadata.values();
        for (ExpeditionExtendedSensorDataMetadata item : values) {
            if (Objects.equals(item.getColumnName(), valueName)) {
                return item;
            }
        }
        return null;
    }

    public static int getTrackColumnCount() {
        return ExpeditionExtendedSensorDataMetadata.values().length;
    }

    public static List<String> getTrackColumnNames() {
        ArrayList<String> colNames = new ArrayList<>(getTrackColumnCount());
        for (ExpeditionExtendedSensorDataMetadata item : ExpeditionExtendedSensorDataMetadata.values()) {
            colNames.add(item.getColumnName());
        }
        return colNames;
    }

    public static Map<String, Integer> getColumnNamesToIndexInDoubleFix() {
        final Map<String, Integer> columnNamesToIndexInDoubleFix = new HashMap<>();
        for (final ExpeditionExtendedSensorDataMetadata column : ExpeditionExtendedSensorDataMetadata.values()) {
            columnNamesToIndexInDoubleFix.put(column.getColumnName(), column.getColumnIndex());
        }
        return columnNamesToIndexInDoubleFix;
    }
}
