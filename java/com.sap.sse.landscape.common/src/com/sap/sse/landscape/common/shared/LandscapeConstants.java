package com.sap.sse.landscape.common.shared;

public interface LandscapeConstants {
    /**
     * A query parameter that can be used with the {@code /gwt/status} servlet to receive "healthy" only after race
     * loading has completed.
     */
    String WAIT_FOR_HEALTHY_UNTIL_RACES_LOADED = "waitUntilRacesLoaded";
}
