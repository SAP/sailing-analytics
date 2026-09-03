package com.sap.sailing.gwt.ui.server;

import com.sap.sailing.gwt.ui.client.MapChooserAndAuthenticationParamsProvider;
import com.sap.sailing.gwt.ui.shared.racemap.MapProviderTypes;
import com.sap.sse.common.util.AccessTokenDTO;

/**
 * Server-only mixin that supplies the shared implementation of the map authentication-params getter. Both
 * {@link SailingServiceImpl} and {@link SimulatorServiceImpl} delegate to the same {@link Activator} singleton, but
 * their servlet superclass chains diverge (only {@code SailingServiceImpl} needs the result-caching servlet), so a
 * shared base class is impossible under single inheritance. This {@code default}-method interface carries the common
 * body instead; a future impl that needs a different source simply overrides it.
 * <p>
 * It extends the client-side sync twin {@link MapChooserAndAuthenticationParamsProvider} (the
 * counterpart of {@code MapAuthenticationParamsProviderAsync}) so that its {@code default} body is the most-specific
 * declaration of the method the two impls also inherit abstractly via {@code SailingService}/{@code SimulatorService},
 * which resolves the otherwise-conflicting inheritance without an explicit override. Being server-only, this interface
 * can hold the {@link Activator} call that a GWT-translated {@code RemoteService} interface could not.
 */
public interface MapChooserAndAuthenticationParamsSupport extends MapChooserAndAuthenticationParamsProvider {
    @Override
    default String getGoogleMapsLoaderAuthenticationParams() {
        return Activator.getInstance().getGoogleMapsLoaderAuthenticationParams();
    }
    
    @Override
    default MapProviderTypes getMapType() {
        return Activator.getInstance().getMapProviderType();
    }

    @Override
    default String getMapTileServerStyleUrl() {
        return Activator.getInstance().getMapTileServerStyleUrl();
    }

    @Override
    default AccessTokenDTO getMapTileAccessToken() {
        return Activator.getInstance().getMapTileAccessToken();
    }
}
