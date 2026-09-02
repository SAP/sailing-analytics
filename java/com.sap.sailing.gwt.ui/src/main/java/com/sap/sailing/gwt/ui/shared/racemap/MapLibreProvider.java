package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.client.MapChooserAndAuthenticationParamsProviderAsync;

/**
 * {@link MapProvider} backed by MapLibre GL JS plus a Google-Maps-compatible facade. Unlike Google Maps, MapLibre can
 * rotate satellite imagery, so it supports satellite in a rotated (wind-up) view, and it provides nautical chart /
 * sea-mark overlays.
 * <p>
 * Loading needs no authentication. If MapLibre and the Google-Maps facade are already present in the window, the shared
 * {@link MapsLoader#callback()} fires right away; otherwise the MapLibre stylesheet and script are injected, followed by
 * the compatibility ES module which invokes the shared {@link MapsLoader#MAP_LOADED_CALLBACK_GLOBAL} global installed by
 * {@link MapsLoader}. Because {@link #isMapLibreReady()}, {@link #loadScript(String, Runnable)} and its
 * {@link #setOnLoad(ScriptElement, Runnable)} JSNI helper are used only for the MapLibre load sequence, they live here
 * as private members.
 * <p>
 * Before injecting anything, the configured tile-server style URL (system property {@code map.provider.tileserver},
 * surfaced through {@link MapChooserAndAuthenticationParamsProviderAsync#getMapTileServerStyleUrl(AsyncCallback)}) is
 * fetched and published to the {@value #TILE_SERVER_STYLE_URL_GLOBAL} {@code window} global, from which the ES module's
 * {@code createRaceStyle()} reads it. If the fetch fails or returns nothing, the global is left unset and the module
 * falls back to its built-in public OpenFreeMap default.
 */
public class MapLibreProvider implements MapProvider {
    /**
     * Name of the {@code window} global carrying the configured MapLibre style document URL. Read by
     * {@code createRaceStyle()} in {@code js/maps/maplibre-test-utils.js}; must be kept in sync with it.
     */
    static final String TILE_SERVER_STYLE_URL_GLOBAL = "__sapMapTileServerStyleUrl";

    private static final Logger logger = Logger.getLogger(MapLibreProvider.class.getName());

    private static final MapCapabilities CAPABILITIES = new MapCapabilities() {
        @Override
        public boolean supportsSatelliteInRotatedView() {
            return true;
        }

        @Override
        public boolean supportsNauticalChartOverlay() {
            return true;
        }
    };

    private final MapChooserAndAuthenticationParamsProviderAsync authProvider;

    public MapLibreProvider(final MapChooserAndAuthenticationParamsProviderAsync authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public String getName() {
        return "MapLibre";
    }

    @Override
    public MapCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public void load() {
        // Resolve the configured style URL first, publish it to the window global, then inject MapLibre. On failure we
        // simply proceed without the global so the ES module uses its built-in public OpenFreeMap default.
        authProvider.getMapTileServerStyleUrl(new AsyncCallback<String>() {
            @Override
            public void onFailure(final Throwable caught) {
                // Non-fatal: the map still loads, but it will use the ES module's client-side fallback default
                // (DEFAULT_RACE_VECTOR_STYLE_URL) instead of the server-configured "map.provider.tileserver" URL. Log it
                // so this silent degradation is visible when diagnosing why a self-hosted tile server is not being used.
                logger.log(Level.WARNING, "Could not retrieve the configured MapLibre tile-server style URL from the "
                        + "server; falling back to the client-side default style. The map will load, but the "
                        + "'map.provider.tileserver' configuration will not take effect for this page load.", caught);
                startTokenRefreshThenInject();
            }

            @Override
            public void onSuccess(final String styleUrl) {
                if (styleUrl != null && !styleUrl.isEmpty()) {
                    setTileServerStyleUrl(styleUrl);
                }
                startTokenRefreshThenInject();
            }
        });
    }

    /**
     * Starts the {@link MapTileTokenRefresher} (which mints and publishes the first tile-server access token) and only
     * once that first mint attempt has completed injects MapLibre, so the very first tile requests already carry a token
     * and do not race into a {@code 401}. The refresher then keeps the token fresh for the lifetime of the page.
     */
    private void startTokenRefreshThenInject() {
        MapTileTokenRefresher.get().start(authProvider, /* onFirstAttempt */ () -> injectMapLibre());
    }

    /**
     * Injects the MapLibre stylesheet, script and compatibility ES module (or fires the shared callback immediately if
     * both are already present). Split out of {@link #load()} so it can run after the configured style URL has been
     * published to the window global.
     */
    private void injectMapLibre() {
        if (isMapLibreReady()) {
            MapsLoader.callback();
        } else {
            final LinkElement css = Document.get().createLinkElement();
            css.setRel("stylesheet");
            css.setHref("./js/maps/vendor/maplibre-gl/" + MapsLoader.MAPLIBRE_VERSION + "/maplibre-gl.css");
            Document.get().getHead().appendChild(css);
            loadScript("./js/maps/vendor/maplibre-gl/" + MapsLoader.MAPLIBRE_VERSION + "/maplibre-gl.js", () -> {
                final ScriptElement module = Document.get().createScriptElement();
                module.setType("module");
                module.setText("import { installGwtMapsCompat } from '" + MapsLoader.MAPS_COMPAT_MODULE_URL + "';\n"
                        + "installGwtMapsCompat();\n" + "window." + MapsLoader.MAP_LOADED_CALLBACK_GLOBAL + "();\n");
                Document.get().getHead().appendChild(module);
            });
        }
    }

    /**
     * Publishes the configured MapLibre style document URL to the {@value #TILE_SERVER_STYLE_URL_GLOBAL} {@code window}
     * global so the compatibility ES module's {@code createRaceStyle()} can read it.
     */
    private static native void setTileServerStyleUrl(String styleUrl) /*-{
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapLibreProvider::TILE_SERVER_STYLE_URL_GLOBAL] = styleUrl;
    }-*/;

    /**
     * @return {@code true} if both MapLibre GL JS and the Google-Maps-compatible facade are already available on the
     * window, meaning no further script injection is required. Private because the readiness probe is specific to this
     * provider's load sequence.
     */
    private static native boolean isMapLibreReady() /*-{
        return !!($wnd.maplibregl && $wnd.maplibregl.Map &&
                $wnd.google && $wnd.google.maps && $wnd.google.maps.Map);
    }-*/;

    /**
     * Creates a {@link ScriptElement} for the given {@code src}, wires {@code onLoad} to its {@code onload} event and a
     * hard-failing handler to its {@code onerror} event, and appends it to the document head so that loading starts.
     */
    private static void loadScript(final String src, final Runnable onLoad) {
        final ScriptElement scriptElement = Document.get().createScriptElement();
        scriptElement.setSrc(src);
        setOnLoad(scriptElement, onLoad);
        Document.get().getHead().appendChild(scriptElement);
    }

    /**
     * Wires {@code onLoad} to the {@code onload} event of the given script element and installs an {@code onerror}
     * handler that throws so failures surface instead of being swallowed silently.
     */
    private static native void setOnLoad(ScriptElement scriptElement, Runnable onLoad) /*-{
        scriptElement.onload = $entry(function() {
            onLoad.@java.lang.Runnable::run()();
        });
        scriptElement.onerror = function() {
            throw new Error('Failed to load ' + scriptElement.src);
        };
    }-*/;
}
