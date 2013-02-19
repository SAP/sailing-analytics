package com.sap.sailing.gwt.ui.shared.racemap;

/**
 * Defines constants for the Z-Indexes for all race map canvas overlays to achieve the right z ordering of the layers.
 * Attention: We also need to incorporate about the z-Indexes of the map markers.
 * @author Frank
 *
 */
public interface RaceMapOverlaysZIndexes {
    public static int BASE_ZINDEX = 110;
    
    public static int BOATTAILS_ZINDEX = BASE_ZINDEX + 1;
    public static int COURSEMARK_ZINDEX = BASE_ZINDEX + 2;
    public static int WINDSENSOR_ZINDEX = BASE_ZINDEX + 3;
    public static int BOATS_ZINDEX = BASE_ZINDEX + 4;
    public static int COMPETITOR_INFO_ZINDEX = BASE_ZINDEX + 5;

    /** the z-index of the info window should always be higher than the rest */
    public static int INFO_WINDOW_ZINDEX = BASE_ZINDEX + 11;
}
