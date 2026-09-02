package com.sap.sailing.gwt.ui.client;

import com.sap.sailing.gwt.ui.shared.racemap.MapProviderTypes;
import com.sap.sailing.gwt.ui.shared.racemap.MapTileAccessTokenDTO;

/**
 * Shared synchronous GWT-RPC super-interface for services that can supply the authentication parameters needed to
 * load a map provider. It is the synchronous twin of {@link MapChooserAndAuthenticationParamsProviderAsync} and, like it, is
 * deliberately named without reference to a specific provider so that future providers requiring authentication can
 * share (and extend) it. Both {@link SailingService} and {@link SimulatorService} extend this interface instead of
 * declaring the method themselves.
 * <p>
 * Extracting the declaration into this common super-interface also lets the server-only
 * {@link com.sap.sailing.gwt.ui.server.MapChooserAndAuthenticationParamsSupport} mixin extend it and supply the shared
 * {@code default} body: because that mixin's declaration is more specific than the one inherited via
 * {@link SailingService}/{@link SimulatorService}, the two impls resolve to the single {@code default} implementation
 * without an inheritance conflict.
 * <p>
 * The method name is retained from the original {@link SailingService}/{@link SimulatorService} contract to avoid a
 * wider GWT-RPC serialization-policy change.
 */
public interface MapChooserAndAuthenticationParamsProvider {
    String getGoogleMapsLoaderAuthenticationParams();

    MapProviderTypes getMapType();

    /**
     * Returns the MapLibre vector style document URL configured on the server (system property
     * {@code map.provider.tileserver}), or the public OpenFreeMap default when unset. Only relevant when the
     * {@link MapProviderTypes#MAPLIBRE} provider is selected.
     */
    String getMapTileServerStyleUrl();

    /**
     * Mints (or returns a cached, bucket-shared) short-lived access token that the client attaches as request headers to
     * every tile request against the self-hosted MapLibre tile server, so the tile server's NGINX {@code secure_link}
     * check can authenticate it. The returned token has {@link MapTileAccessTokenDTO#isAuthenticationEnabled()} equal to
     * {@code false} when no tile-server auth secret is configured on the server, in which case the client sends no
     * headers. Only relevant when the {@link MapProviderTypes#MAPLIBRE} provider is selected.
     */
    MapTileAccessTokenDTO getMapTileAccessToken();
}
