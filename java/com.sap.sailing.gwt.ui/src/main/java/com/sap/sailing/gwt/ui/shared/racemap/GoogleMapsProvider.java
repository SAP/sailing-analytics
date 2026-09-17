package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.function.Consumer;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.client.MapChooserAndAuthenticationParamsProviderAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;

/**
 * {@link MapProvider} backed by the Google Maps JavaScript API. Google Maps cannot rotate satellite imagery, so it does
 * not support satellite in a rotated (wind-up) view, and it has no nautical chart / sea-mark overlay support.
 * <p>
 * Loading requires authentication parameters that are fetched asynchronously via the injected
 * {@link MapChooserAndAuthenticationParamsProviderAsync}. On failure the injected {@link ErrorReporter} and {@link StringMessages}
 * are used to report the same user-facing message the call sites reported before this strategy was introduced. Once the
 * parameters arrive, the Maps API script is injected with a {@code &callback=} pointing at the shared global installed
 * by {@link MapsLoader}.
 */
public class GoogleMapsProvider implements MapProvider {
    private static final MapCapabilities CAPABILITIES = new MapCapabilities() {
        @Override
        public boolean supportsSatelliteInRotatedView() {
            return false;
        }

        @Override
        public boolean supportsNauticalChartOverlay() {
            return false;
        }
    };

    private static Runnable authFailureReporter;

    private final MapChooserAndAuthenticationParamsProviderAsync authProvider;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;

    public GoogleMapsProvider(final MapChooserAndAuthenticationParamsProviderAsync authProvider,
            final ErrorReporter errorReporter, final StringMessages stringMessages,
            Consumer<String> authFailureReporter) {
        this.authProvider = authProvider;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        GoogleMapsProvider.authFailureReporter = ()->
                    authFailureReporter.accept(stringMessages.errorGoogleMapsAuthenticationFailed());
    }

    @Override
    public String getName() {
        return "Google Maps";
    }

    @Override
    public MapCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public void load() {
        authProvider.getGoogleMapsLoaderAuthenticationParams(new AsyncCallback<String>() {
            @Override
            public void onFailure(final Throwable caught) {
                errorReporter.reportError(
                        stringMessages.errorNoAuthenticationParamsForGoogleMapsFound(caught.getMessage()));
            }

            @Override
            public void onSuccess(final String authenticationParams) {
                installAuthFailureCallback();
                final ScriptElement scriptElement = Document.get().createScriptElement();
                scriptElement.setSrc("https://maps.googleapis.com/maps/api/js?v=" + MapsLoader.API_VERSION + "&"
                        + authenticationParams + "&libraries=" + MapsLoader.LIBRARIES + "&callback="
                        + MapsLoader.MAP_LOADED_CALLBACK_GLOBAL);
                Document.get().getHead().appendChild(scriptElement);
            }
        });
    }
    
    private static void authFailed() {
        authFailureReporter.run();
    }

    private static native void installAuthFailureCallback() /*-{
        $wnd.gm_authFailure = $entry(function() {
            @com.sap.sailing.gwt.ui.shared.racemap.GoogleMapsProvider::authFailed()();
        });
    }-*/;
}
