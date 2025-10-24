package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.maps.client.controls.MapTypeStyle;
import com.google.gwt.maps.client.maptypes.MapTypeStyleElementType;
import com.google.gwt.maps.client.maptypes.MapTypeStyleFeatureType;
import com.google.gwt.maps.client.maptypes.MapTypeStyler;
import com.sap.sailing.gwt.ui.client.shared.racemap.ColorMapTypeStyler;
import com.sap.sse.common.Color;

public class GoogleMapStyleHelper {

    public static MapTypeStyle createHiddenStyle(MapTypeStyleFeatureType featureType) {
        return createElementStyleWithVisibility(featureType, MapTypeStyleElementType.ALL, "off");
    }

    public static MapTypeStyle createSimplifiedStyle(MapTypeStyleFeatureType featureType) {
        return createElementStyleWithVisibility(featureType, MapTypeStyleElementType.ALL, "simplified");
    }

    public static MapTypeStyle createColorStyle(MapTypeStyleFeatureType featureType, Color color, int saturation, int lightness) {
        return createElementStyleWithColor(featureType, MapTypeStyleElementType.ALL, color, saturation, lightness);
    }

    public static MapTypeStyle createColorStyle(MapTypeStyleFeatureType featureType, Color color) {
        MapTypeStyle result = MapTypeStyle.newInstance();
        result.setFeatureType(featureType);
        result.setElementType(MapTypeStyleElementType.ALL);
        MapTypeStyler[] typeStylers = new MapTypeStyler[1];
        typeStylers[0] = ColorMapTypeStyler.newColorStyler(color.getAsHtml()).cast();
        result.setStylers(typeStylers);
        return result;
    }

    public static MapTypeStyle createElementStyleWithColor(MapTypeStyleFeatureType featureType, MapTypeStyleElementType elementType,
            Color rgbColor, int saturation, int ligthness) {
        MapTypeStyle result = MapTypeStyle.newInstance();
        result.setFeatureType(featureType);
        result.setElementType(elementType);
        
        MapTypeStyler[] typeStylers = new MapTypeStyler[3];
        typeStylers[0] = MapTypeStyler.newHueStyler(rgbColor.getAsHtml());
        typeStylers[1] = MapTypeStyler.newSaturationStyler(saturation);
        typeStylers[2] = MapTypeStyler.newLightnessStyler(ligthness);
        result.setStylers(typeStylers);
        return result;
    }

    public static MapTypeStyle createElementStyleOnlyLightness(MapTypeStyleFeatureType featureType, MapTypeStyleElementType elementType, int ligthness) {
        MapTypeStyle result = MapTypeStyle.newInstance();
        result.setFeatureType(featureType);
        result.setElementType(elementType);
        
        MapTypeStyler[] typeStylers = new MapTypeStyler[1];
        typeStylers[0] = MapTypeStyler.newLightnessStyler(ligthness);
        result.setStylers(typeStylers);

        return result;
    }
    
    public static MapTypeStyle createElementStyleWithVisibility(MapTypeStyleFeatureType featureType, MapTypeStyleElementType elementType, String visibility) {
        MapTypeStyle result = MapTypeStyle.newInstance();
        result.setFeatureType(featureType);
        result.setElementType(elementType);
        
        MapTypeStyler[] typeStylers = new MapTypeStyler[1];
        typeStylers[0] = MapTypeStyler.newVisibilityStyler(visibility);
        result.setStylers(typeStylers);

        return result;
    }
}
