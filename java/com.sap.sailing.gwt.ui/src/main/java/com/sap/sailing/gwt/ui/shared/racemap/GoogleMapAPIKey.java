package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.maps.client.LoadApi;

public class GoogleMapAPIKey {
    // Emergency patch: problems with gme-sapglobalmarketing key?
    public static final String V3_APIKey = "AIzaSyD1Se4tIkt-wglccbco3S7twaHiG20hR9E";

    /**
     * These are the additional parameters to give to a
     * {@link LoadApi#go(Runnable, java.util.ArrayList, boolean, String)} call for our Google Maps API usage.
     */
    public static final String V3_PARAMS = "client=gme-sapglobalmarketing&channel=sapsailing.com";
}
