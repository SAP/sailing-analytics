package com.sap.sse.common.util;


public class RoundingUtil {
    public static final RoundingUtil distanceDecimalFormatter = new RoundingUtil(2);
    public static final RoundingUtil knotsDecimalFormatter = new RoundingUtil(2);
    public static final RoundingUtil bearingDecimalFormatter = new RoundingUtil(1);
    public static final RoundingUtil speedDecimalFormatter = new RoundingUtil(1);
    public static final RoundingUtil latLngDecimalFormatter = new RoundingUtil(7);
    
    private final double shiftFactor; 
    
    private RoundingUtil(int decimals) {
        shiftFactor = getShiftFactor(decimals);
    }

    private static double getShiftFactor(int decimals) {
        return Math.pow(10, decimals);
    }

    public double format(double value) {
        return ((double) Math.round(value * shiftFactor)) / shiftFactor;
    }
    
    public static double format(double value, int decimals) {
        final double shiftFactor = getShiftFactor(decimals);
        return ((double) Math.round(value * shiftFactor)) / shiftFactor;
    }
}
