package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.debug.client.DebugInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.client.MapChooserAndAuthenticationParamsProviderAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.PendingAjaxCallMarker;

/**
 * The {@link #load(Runnable, MapChooserAndAuthenticationParamsProviderAsync, ErrorReporter, StringMessages)} method can be used
 * by clients to request the loading of a map API. Which {@link MapProvider} is used is decided from the {@code ?maps=}
 * page URL parameter: {@code maplibre} selects {@link MapLibreProvider}, anything else selects
 * {@link GoogleMapsProvider}. The callback passed will be invoked immediately if the API has already been loaded (e.g.,
 * by another client call within the same frame / document); it will be queued for invocation otherwise. A single shared
 * {@code window} callback global is installed at most once when
 * {@link #load(Runnable, MapChooserAndAuthenticationParamsProviderAsync, ErrorReporter, StringMessages)} is invoked for the first
 * time; the selected provider's injected script triggers that global, which in turn invokes all queued callbacks via
 * {@link #callback()}.<p>
 */
public class MapsLoader {
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

    /**
     * The version of the vendored MapLibre GL JS distribution shipped under {@code js/maps/vendor/maplibre-gl/}.
     * Package-private so {@link MapLibreProvider} can read it.
     */
    final static String MAPLIBRE_VERSION = "5.9.0";

    /**
     * The relative URL of the Google-Maps-compatibility facade ES module. The query parameter is a cache-busting
     * marker that is bumped whenever the facade changes. Package-private so {@link MapLibreProvider} can read it.
     */
    final static String MAPS_COMPAT_MODULE_URL = "./js/maps/gwt-maps-maplibre-compat.js?v=race-map-feedback-19";

    /**
     * The name of the {@code window} global through which both the Google Maps API and the compatibility ES module
     * invoke {@link #callback()}. For the Google path it is passed as the {@code &callback=} URL parameter; for the
     * MapLibre path it is referenced from the injected module text. Only one path runs per page load, so a single
     * shared name suffices; it is installed via {@link #installGlobalCallback()} and removed via
     * {@link #clearGlobalCallback()}. Package-private so the providers can read it.
     */
    final static String MAP_LOADED_CALLBACK_GLOBAL = "mapLoadedCallback";

    /**
     * The pending-Ajax category (see {@code com.sap.sse.gwt.client.async.PendingAjaxCallMarker} and the consuming
     * {@code com.sap.sailing.selenium.pages.PageObject}) under which the whole asynchronous map-loading sequence is
     * marked as pending: from the moment {@link #load(Runnable, MapChooserAndAuthenticationParamsProviderAsync, ErrorReporter, StringMessages)}
     * kicks off its work until {@link #callback()} has run every queued completion callback (or a terminal failure
     * aborts the sequence). Selenium raceboard tests wait on this category so that they only proceed once the map API
     * (Google or MapLibre) is fully loaded and the map's UI - including the {@code moreOptionsButton} - has been built.
     * The mark is provider-agnostic on purpose: it brackets the shared loader sequence rather than any one provider's
     * protocol. Outside instrumented test runs (see {@link DebugInfo#isDebugIdEnabled()}) marking is a no-op.
     */
    public final static String MAP_LOAD_CATEGORY = "mapLoad";

    private static MapProvider currentProvider;
    private static boolean loading = false;
    private static boolean loaded = false;

    /**
     * Guards the {@link #MAP_LOAD_CATEGORY} pending-Ajax mark so that the increment performed when the loader starts
     * its asynchronous sequence is balanced by exactly one decrement, regardless of which of the several terminal exit
     * points ({@link #callback()}, the {@code getMapType} failure, or {@link #authFailed(ErrorReporter, String)}) is
     * reached first. {@code true} while the mark is outstanding.
     */
    private static boolean mapLoadPending = false;

    private static final Set<Runnable> callbacks = new HashSet<>();
    
    private MapsLoader() {
    }

    /**
     * Requests the loading of the map API through the {@link MapProvider} selected from the {@code ?maps=} page URL
     * parameter. The shared callback global is installed once, then the provider's {@link MapProvider#load()} is
     * invoked; the provider's injected script eventually triggers {@link #callback()}, which fires all queued
     * callbacks.
     *
     * @param callback
     *            must not be {@code null}.
     * @param authProvider
     *            supplies the Google Maps authentication parameters; only used when {@link GoogleMapsProvider} is
     *            selected.
     * @param errorReporter
     *            used to report an authentication failure; only used when {@link GoogleMapsProvider} is selected.
     * @param stringMessages
     *            supplies the user-facing authentication-failure message; only used when {@link GoogleMapsProvider} is
     *            selected.
     */
    public static void load(final Runnable callback, final MapChooserAndAuthenticationParamsProviderAsync authProvider,
            final ErrorReporter errorReporter, final StringMessages stringMessages) {
        if (loaded) {
            Scheduler.get().scheduleDeferred(() -> callback.run());
        } else {
            callbacks.add(callback);
            if (!loading) {
                loading = true;
                markMapLoadPending();
                authProvider.getMapType(new AsyncCallback<MapProviderTypes>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        unmarkMapLoadPending();
                        errorReporter.reportError(stringMessages.errorObtainingMapType(caught.getMessage()), /* silentMode */ true);
                    }

                    @Override
                    public void onSuccess(MapProviderTypes result) {
                        currentProvider = getMapsProviderOfType(result, authProvider, errorReporter, stringMessages);
                        installGlobalCallback();
                        currentProvider.load();
                    }
                });
            }
        }
    }

    private static MapProvider getMapsProviderOfType(MapProviderTypes type,
            final MapChooserAndAuthenticationParamsProviderAsync authProvider, final ErrorReporter errorReporter,
            final StringMessages stringMessages) {
        final MapProvider result;
        switch (type) {
        case GOOGLE:
            result = new GoogleMapsProvider(authProvider, errorReporter, stringMessages,
                            (String errorMessage)->authFailed(errorReporter, errorMessage));
            break;
        case MAPLIBRE:
            result = new MapLibreProvider(authProvider);
            break;
        default:
            throw new IllegalArgumentException("Unknown map provider type: "+type);
        }
        return result;
    }
    
    /**
     * @return the {@link MapProvider} selected by the most recent
     *         {@link #load(Runnable, MapChooserAndAuthenticationParamsProviderAsync, ErrorReporter, StringMessages)} call, so
     *         callers can query its {@link MapProvider#getCapabilities() capabilities} instead of checking the provider
     *         identity directly.
     * @throws IllegalStateException
     *             if no provider has been selected yet because {@code load(...)} has not been called.
     */
    public static MapProvider getProvider() {
        final MapProvider result;
        if (currentProvider == null) {
            throw new IllegalStateException("Map not loaded yet - call load() first");
        } else {
            result = currentProvider;
        }
        return result;
    }

    /**
     * Installs {@link #callback()} as the {@value #MAP_LOADED_CALLBACK_GLOBAL} {@code window} global, wrapped via
     * {@code $entry} so that it enters the GWT event loop correctly. Used by both the Google Maps API callback and the
     * compatibility ES module callback, which reach {@link #callback()} through this same global.
     */
    private static native void installGlobalCallback() /*-{
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapsLoader::MAP_LOADED_CALLBACK_GLOBAL] = $entry(function() {
            @com.sap.sailing.gwt.ui.shared.racemap.MapsLoader::callback()();
        });
    }-*/;

    /**
     * Fires all queued callbacks and clears the shared callback global. Package-private so {@link MapLibreProvider}'s
     * already-loaded fast path can invoke it directly (nothing is injected in that case, so nothing else would fire
     * it).
     */
    static void callback() {
        loaded = true;
        loading = false;
        callbacks.forEach(Runnable::run);
        callbacks.clear();
        clearGlobalCallback();
        unmarkMapLoadPending();
    }

    private static void authFailed(ErrorReporter errorReporter, String errorMessage) {
        loading = false;
        unmarkMapLoadPending();
        errorReporter.reportError(errorMessage);
    }

    /**
     * Marks the whole asynchronous map-loading sequence as a pending Ajax call under {@link #MAP_LOAD_CATEGORY} so that
     * Selenium raceboard tests can wait for the map (and the map UI built by the completion callbacks) to be ready.
     * Guarded by {@link DebugInfo#isDebugIdEnabled()} so it is a no-op outside instrumented test runs, and by
     * {@link #mapLoadPending} so the mark is only ever incremented once per loader sequence.
     */
    private static void markMapLoadPending() {
        if (DebugInfo.isDebugIdEnabled() && !mapLoadPending) {
            mapLoadPending = true;
            PendingAjaxCallMarker.incrementPendingAjaxCalls(MAP_LOAD_CATEGORY);
        }
    }

    /**
     * Balances the {@link #markMapLoadPending() mark} at whichever terminal exit point of the loader sequence is
     * reached first ({@link #callback()}, the {@code getMapType} failure, or {@link #authFailed(ErrorReporter, String)}).
     * The {@link #mapLoadPending} guard ensures that only the first such call decrements the counter.
     */
    private static void unmarkMapLoadPending() {
        if (mapLoadPending) {
            mapLoadPending = false;
            PendingAjaxCallMarker.decrementPendingAjaxCalls(MAP_LOAD_CATEGORY);
        }
    }
    
    /**
     * Removes the {@value #MAP_LOADED_CALLBACK_GLOBAL} {@code window} global installed by
     * {@link #installGlobalCallback()}.
     */
    private static native void clearGlobalCallback() /*-{
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapsLoader::MAP_LOADED_CALLBACK_GLOBAL] = null;
    }-*/;
}
