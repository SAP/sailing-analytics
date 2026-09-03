package com.sap.sailing.gwt.ui.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.shared.racemap.MapProviderTypes;
import com.sap.sse.common.util.AccessTokenDTO;

/**
 * Shared async GWT-RPC super-interface for services that can supply the authentication parameters needed to load a
 * map provider. It is deliberately named without reference to a specific provider so that future providers requiring
 * authentication can share (and extend) it. Both {@link SailingServiceAsync} and {@link SimulatorServiceAsync} extend
 * this interface, which lets
 * {@code com.sap.sailing.gwt.ui.shared.racemap.MapsLoader#load(Runnable, MapAuthenticationParamsProviderAsync,
 * com.sap.sailing.gwt.ui.client.ErrorReporter, StringMessages)} accept either service.
 * <p>
 * The method name is retained from the original {@link SailingService}/{@link SimulatorService} contract to avoid a
 * wider GWT-RPC serialization-policy change.
 */
public interface MapChooserAndAuthenticationParamsProviderAsync {
    void getGoogleMapsLoaderAuthenticationParams(AsyncCallback<String> callback);

    void getMapType(AsyncCallback<MapProviderTypes> asyncCallback);

    /**
     * Asynchronously retrieves the MapLibre vector style document URL configured on the server (system property
     * {@code map.provider.tileserver}), or the public OpenFreeMap default when unset. Only relevant when the
     * {@link MapProviderTypes#MAPLIBRE} provider is selected.
     */
    void getMapTileServerStyleUrl(AsyncCallback<String> callback);

    /**
     * Asynchronously mints (or returns a cached, bucket-shared) short-lived tile-server access token; see
     * {@link MapChooserAndAuthenticationParamsProvider#getMapTileAccessToken()}.
     */
    void getMapTileAccessToken(AsyncCallback<AccessTokenDTO> callback);
}
