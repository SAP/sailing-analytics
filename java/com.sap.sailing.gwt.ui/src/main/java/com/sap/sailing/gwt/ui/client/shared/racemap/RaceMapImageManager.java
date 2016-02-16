package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.base.Size;
import com.google.gwt.maps.client.overlays.Marker;
import com.google.gwt.maps.client.overlays.MarkerImage;
import com.google.gwt.maps.client.overlays.MarkerOptions;
import com.google.gwt.resources.client.ImageResource;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sse.common.Util;

public class RaceMapImageManager {

    private static final int MARKER_HEIGHT = 26;

    private static final int MARKER_WIDTH = 26;

    /**
     * An arrow showing the combined wind depending on the wind direction 
     */
    protected ImageTransformer combinedWindIconTransformer;

    /**
     * An arrow showing the true north direction
     */
    protected ImageTransformer trueNorthIndicatorIconTransformer;

    /**
     * An arrow showing the wind provided by a wind sensor on a boat 
     */
    protected ImageTransformer windSensorIconTransformer;

    protected Map<Util.Pair<ManeuverType, ManeuverColor>, Marker> maneuverIconsForTypeAndDirectionIndicatingColor;

    private static RaceMapResources resources = GWT.create(RaceMapResources.class);

    public RaceMapImageManager() {
        maneuverIconsForTypeAndDirectionIndicatingColor = new HashMap<Util.Pair<ManeuverType, ManeuverColor>, Marker>();
        trueNorthIndicatorIconTransformer = new ImageTransformer(resources.trueNorthIndicatorIcon());
        combinedWindIconTransformer = new ImageTransformer(resources.combinedWindIcon());
        windSensorIconTransformer = new ImageTransformer(resources.expeditionWindIcon());
    }

    /**
     * Loads the map overlay icons The method can only be called after the map is loaded. The {@link #maneuverIconsForTypeAndDirectionIndicatingColor} map
     * is filled for all combinations of {@link ManeuverType maneuver types} and {@link ManeuverColor colors}.
     */
    public void loadMapIcons(MapWidget map) {
        if (map != null) {
            final List<ManeuverType> maneuvers = new ArrayList<ManeuverType>();
            for (ManeuverType type : ManeuverType.values()) {
                maneuvers.add(type);
            }
            Marker icon;
            for (ManeuverType maneuver : maneuvers) {
                icon = createMarker(resources.maneuverMarkerRed());
                maneuverIconsForTypeAndDirectionIndicatingColor
                        .put(new Util.Pair<ManeuverType, ManeuverColor>(maneuver, ManeuverColor.RED), icon);
                icon = createMarker(resources.maneuverMarkerGreen());
                maneuverIconsForTypeAndDirectionIndicatingColor
                        .put(new Util.Pair<ManeuverType, ManeuverColor>(maneuver, ManeuverColor.GREEN), icon);
            }
        }
    }

    private Marker createMarker(ImageResource ressource) {
        MarkerOptions options = MarkerOptions.newInstance();
        MarkerImage markerImage = MarkerImage.newInstance(ressource.getSafeUri().asString());
        markerImage.setAnchor(Point.newInstance(MARKER_WIDTH/2,  MARKER_HEIGHT/2));
        markerImage.setScaledSize(Size.newInstance(MARKER_WIDTH, MARKER_HEIGHT));
        options.setIcon(markerImage);
        Marker marker = Marker.newInstance(options);
        return marker;
    }
    
    public ImageTransformer getTrueNorthIndicatorIconTransformer() {
        return trueNorthIndicatorIconTransformer;
    }
    
    public ImageTransformer getCombinedWindIconTransformer() {
        return combinedWindIconTransformer;
    }

    public ImageTransformer getWindSensorIconTransformer() {
        return windSensorIconTransformer;
    }
}
