package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;

/**
 * The {@link #load(Runnable, String)} method can be used by clients to request the loading of the Google Maps API.
 * The callback passed will be invoked immediately if the API has already been loaded (e.g., by another
 * client call to the {@link #load(Runnable, String)} method within the same frame / document); it will be queued
 * for invocation by a Google Maps API callback function registered otherwise. This callback function
 * is injected at most once when the {@link #load(Runnable, String)} method is invoked for the first time and
 * will trigger all callbacks registered through the {@link #load(Runnable, String)} method until the maps API
 * invokes the callback registered.
 */
public class GoogleMapsLoader {
    /**
     * Note: If you use 3, it will take the newest stable available. We want that, although we didn't test with that yet!
     * Google Release notes: https://developers.google.com/maps/documentation/javascript/releases.
     * Subscribe to https://groups.google.com/forum/#!forum/google-maps-js-api-v3-notify for change notifications.
     */
    public final static String API_VERSION = "3";
    
    /**
     * The required Google Maps libraries; a comma-separated list. See https://developers.google.com/maps/documentation/javascript/libraries
     * for more details. Examples: <tt>drawing,geometry,places,visualization</tt>
     */
    public final static String LIBRARIES = "drawing,geometry";
    
    private static boolean loading = false;
    private static boolean loaded = false;
    private static final Set<Runnable> callbacks = new HashSet<>();
    private static Runnable authFailureListener;

    private GoogleMapsLoader() {
    }

    /**
     * Registers a listener that is invoked when the Google Maps JavaScript API rejects the request, i.e. when the
     * API calls its {@code gm_authFailure} hook. This happens for an invalid or missing key, a referrer that is not
     * allowed, an API that is not enabled, or a quota / billing problem. Unlike a timeout this fires only on an actual
     * authentication failure and therefore never misfires on a slow but otherwise valid load. Without such a listener
     * an authentication failure leaves a blank map with no controls and no diagnostic.
     *
     * @param listener the listener to notify on an authentication failure; may be {@code null} to clear it.
     */
    public static void setAuthFailureListener(final Runnable listener) {
        authFailureListener = listener;
    }

    /**
     * @param callback must not be {@code null}.
     */
    public static void load(Runnable callback, String authenticationParams) {
        if (loaded) {
            Scheduler.get().scheduleDeferred(() -> callback.run());
        } else {
            callbacks.add(callback);
            if (!loading) {
                loading = true;
                installCallback();
                installAuthFailureCallback();
                final ScriptElement scriptElement = Document.get().createScriptElement();
                scriptElement.setSrc("https://maps.googleapis.com/maps/api/js?v="+API_VERSION+"&" + authenticationParams
                        + "&libraries="+LIBRARIES+"&callback=googleMapsLoadedCallback");
                Document.get().getHead().appendChild(scriptElement);
            }
        }
    }
    
    private static void callback() {
        loaded = true;
        loading = false;
        callbacks.forEach(Runnable::run);
        callbacks.clear();
        clearCallback();
    }

    private static void authFailed() {
        loading = false;
        if (authFailureListener != null) {
            authFailureListener.run();
        }
    }
    
    private static native void installCallback() /*-{
        $wnd.googleMapsLoadedCallback = $entry(function() {
            @com.sap.sailing.gwt.ui.shared.racemap.GoogleMapsLoader::callback()();
        });
    }-*/;

    private static native void installAuthFailureCallback() /*-{
        $wnd.gm_authFailure = $entry(function() {
            @com.sap.sailing.gwt.ui.shared.racemap.GoogleMapsLoader::authFailed()();
        });
    }-*/;
    
    private static native void clearCallback() /*-{
        $wnd.googleMapsLoadedCallback = null;
    }-*/;
}
