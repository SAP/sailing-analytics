package com.sap.sailing.gwt.ui.shared.racemap;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.client.MapChooserAndAuthenticationParamsProviderAsync;

/**
 * Client-side singleton that keeps a fresh, short-lived tile-server access token published to the {@code window} so the
 * MapLibre {@code transformRequest} hook (see {@code js/maps/maplibre-test-utils.js}) can attach it as the
 * {@code X-OFM-Md5} / {@code X-OFM-Expires} / {@code X-OFM-Kid} request headers on every tile-server request.
 * <p>
 * The token is minted (and bucket-shared) on the server via
 * {@link MapChooserAndAuthenticationParamsProviderAsync#getMapTileAccessToken(AsyncCallback)}. Because the server hands
 * every client in a time bucket the identical, result-cache-shared token, refreshing here is near-free even at large
 * fan-out, so a single dedicated timer is used rather than piggy-backing on the various entry points' polling calls.
 * After each successful mint the next refresh is scheduled at the server-suggested delay (half the token TTL); a
 * {@code visibilitychange} handler additionally forces an immediate refresh when a backgrounded tab (whose timers the
 * browser may have throttled) becomes visible again, and the {@value #TOKEN_REFRESH_GLOBAL} global lets the map error
 * handler trigger an on-demand refresh after a {@code 401}/{@code 403}.
 * <p>
 * When the server reports authentication is disabled, the globals are cleared so the hook sends no headers.
 */
public class MapTileTokenRefresher {
    /**
     * Name of the {@code window} global carrying the current token's base64url MD5 signature (the {@code X-OFM-Md5}
     * header value). Read by {@code mapTileServerTransformRequest()} in {@code js/maps/maplibre-test-utils.js}; must be
     * kept in sync with it. Absent/empty means "send no token headers".
     */
    static final String TOKEN_MD5_GLOBAL = "__sapMapTileTokenMd5";

    /**
     * Name of the {@code window} global carrying the current token's expiry in seconds since the epoch (the
     * {@code X-OFM-Expires} header value).
     */
    static final String TOKEN_EXPIRES_GLOBAL = "__sapMapTileTokenExpires";

    /**
     * Name of the {@code window} global carrying the current token's key id (the {@code X-OFM-Kid} header value).
     */
    static final String TOKEN_KID_GLOBAL = "__sapMapTileTokenKid";

    /**
     * Name of the {@code window} global holding a zero-argument function that forces an immediate token refresh. Called
     * by the map error handler in {@code js/maps/maplibre-test-utils.js} when a tile request fails with {@code 401}/
     * {@code 403}, so a stale token recovers without waiting for the next scheduled refresh.
     */
    static final String TOKEN_REFRESH_GLOBAL = "__sapMapTileTokenRefresh";

    /**
     * Lower bound for the scheduled refresh delay, guarding against a misconfigured tiny TTL causing a refresh storm.
     */
    private static final int MIN_REFRESH_DELAY_MILLIS = 5000;

    /**
     * Delay before retrying after a failed mint, so a transient RPC failure does not spin.
     */
    private static final int RETRY_DELAY_MILLIS = 10000;

    private static final Logger logger = Logger.getLogger(MapTileTokenRefresher.class.getName());

    private static final MapTileTokenRefresher INSTANCE = new MapTileTokenRefresher();

    private MapChooserAndAuthenticationParamsProviderAsync authProvider;

    private boolean started;

    private final Timer refreshTimer = new Timer() {
        @Override
        public void run() {
            refresh(/* onAttemptComplete */ null);
        }
    };

    private MapTileTokenRefresher() {
    }

    public static MapTileTokenRefresher get() {
        return INSTANCE;
    }

    /**
     * Starts (idempotently) keeping the tile-server token fresh, using the given async provider to mint it, and invokes
     * {@code onFirstAttempt} exactly once after the first mint attempt has completed (whether it succeeded or failed) so
     * the caller can defer injecting MapLibre until a first token attempt has been made, avoiding a first-paint
     * {@code 401}. On a repeated call (the singleton is already running) {@code onFirstAttempt} is invoked right away
     * because a token is already being managed.
     */
    public void start(final MapChooserAndAuthenticationParamsProviderAsync authProvider,
            final Runnable onFirstAttempt) {
        if (started) {
            if (onFirstAttempt != null) {
                onFirstAttempt.run();
            }
        } else {
            started = true;
            this.authProvider = authProvider;
            installRefreshGlobalAndVisibilityHandler();
            refresh(onFirstAttempt);
        }
    }

    /**
     * Mints the next token, publishes (or clears) the {@code window} globals accordingly, schedules the following
     * refresh, and finally runs {@code onAttemptComplete} once if non-{@code null}. A {@code null} {@code md5} in the
     * result means authentication is disabled, in which case the globals are cleared so no headers are sent.
     */
    private void refresh(final Runnable onAttemptComplete) {
        authProvider.getMapTileAccessToken(new AsyncCallback<MapTileAccessTokenDTO>() {
            @Override
            public void onFailure(final Throwable caught) {
                // Keep any previously published token in place (it may still be valid) and retry shortly; log so that a
                // persistently failing mint is diagnosable rather than silently degrading tile access.
                logger.log(Level.WARNING, "Could not refresh the MapLibre tile-server access token; will retry.",
                        caught);
                refreshTimer.schedule(RETRY_DELAY_MILLIS);
                runOnce(onAttemptComplete);
            }

            @Override
            public void onSuccess(final MapTileAccessTokenDTO token) {
                if (token != null && token.isAuthenticationEnabled()) {
                    publishToken(token.getMd5(), Long.toString(token.getExpiresEpochSecond()), token.getKid());
                } else {
                    clearToken();
                }
                final long refreshAfterMillis = token == null ? MIN_REFRESH_DELAY_MILLIS
                        : token.getRefreshAfterMillis();
                final int delayMillis = (int) Math.max(MIN_REFRESH_DELAY_MILLIS, refreshAfterMillis);
                refreshTimer.schedule(delayMillis);
                runOnce(onAttemptComplete);
            }
        });
    }

    /**
     * Runs {@code onAttemptComplete} if non-{@code null}; extracted so both callback branches share one guarded call.
     */
    private void runOnce(final Runnable onAttemptComplete) {
        if (onAttemptComplete != null) {
            onAttemptComplete.run();
        }
    }

    /**
     * Entry point for the {@value #TOKEN_REFRESH_GLOBAL} global and the {@code visibilitychange} handler: forces an
     * immediate, out-of-schedule refresh (cancelling the pending timer first so the two do not overlap).
     */
    private void refreshFromNative() {
        refreshTimer.cancel();
        refresh(/* onAttemptComplete */ null);
    }

    /**
     * Publishes the current token's header values to the {@code window} globals read by the {@code transformRequest}
     * hook.
     */
    private static native void publishToken(String md5, String expires, String kid) /*-{
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::TOKEN_MD5_GLOBAL] = md5;
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::TOKEN_EXPIRES_GLOBAL] = expires;
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::TOKEN_KID_GLOBAL] = kid;
    }-*/;

    /**
     * Clears the token {@code window} globals so the {@code transformRequest} hook attaches no headers (authentication
     * disabled).
     */
    private static native void clearToken() /*-{
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::TOKEN_MD5_GLOBAL] = null;
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::TOKEN_EXPIRES_GLOBAL] = null;
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::TOKEN_KID_GLOBAL] = null;
    }-*/;

    /**
     * Installs the {@value #TOKEN_REFRESH_GLOBAL} global (an on-demand refresh trigger for the map error handler) and a
     * {@code visibilitychange} listener that forces a refresh when a backgrounded tab becomes visible again, both
     * routed through {@link #refreshFromNative()}.
     */
    private native void installRefreshGlobalAndVisibilityHandler() /*-{
        var self = this;
        var trigger = $entry(function() {
            self.@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::refreshFromNative()();
        });
        $wnd[@com.sap.sailing.gwt.ui.shared.racemap.MapTileTokenRefresher::TOKEN_REFRESH_GLOBAL] = trigger;
        if ($doc.addEventListener) {
            $doc.addEventListener('visibilitychange', function() {
                if (!$doc.hidden) {
                    trigger();
                }
            });
        }
    }-*/;
}
