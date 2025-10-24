package com.google.gwt.maps.client;

public enum RenderingType {
    /**
     * Indicates that the map is a raster map.
     */
    RASTER,
    
    /**
     * Indicates that it is unknown yet whether the map is vector or raster, because the map has not finished initializing yet.
     */
    UNINITIALIZED,
    
    /**
     * Indicates that the map is a vector map.
     */
    VECTOR;
}
