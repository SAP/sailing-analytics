package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.CacheableRPCResult;
import com.sap.sse.common.Util;

/**
 * Short-lived, bucket-shared access token that the browser attaches (as {@code X-OFM-Md5} / {@code X-OFM-Expires} /
 * {@code X-OFM-Kid} request headers) to every request against a self-hosted MapLibre tile server, so the tile server's
 * NGINX {@code secure_link} check can authenticate the request. The token is minted on the server (see the
 * {@code com.sap.sailing.gwt.ui.server.Activator}) and refreshed on the client by
 * {@link MapTileTokenRefresher}.
 * <p>
 * The token is deliberately identical for every client within one time bucket, which lets the server return the same
 * instance for all callers in that bucket; because this class is a {@link CacheableRPCResult}, the serialized payload is
 * then cached once per bucket by the result-caching RPC servlet. Keeping the token out of the request URL (it travels in
 * headers) preserves tile URL cacheability.
 * <p>
 * When no tile-server auth secret is configured on the server, {@link #isAuthenticationEnabled()} is {@code false} and
 * the client sends no headers.
 * <p>
 * The fields are intentionally non-{@code final}: GWT-RPC deserialization assigns them directly and cannot populate
 * {@code final} fields.
 */
public class MapTileAccessTokenDTO implements IsSerializable, CacheableRPCResult {
    private String md5;
    private long expiresEpochSecond;
    private String kid;
    private long refreshAfterMillis;

    /**
     * For GWT-RPC serialization only; application code uses
     * {@link #MapTileAccessTokenDTO(String, long, String, long)}.
     */
    MapTileAccessTokenDTO() {
    }

    public MapTileAccessTokenDTO(final String md5, final long expiresEpochSecond, final String kid,
            final long refreshAfterMillis) {
        this.md5 = md5;
        this.expiresEpochSecond = expiresEpochSecond;
        this.kid = kid;
        this.refreshAfterMillis = refreshAfterMillis;
    }

    /**
     * @return the base64url-without-padding MD5 signature to send in the {@code X-OFM-Md5} header, or {@code null} when
     * authentication is disabled.
     */
    public String getMd5() {
        return md5;
    }

    /**
     * @return the token expiry in seconds since the epoch, to send in the {@code X-OFM-Expires} header and matched by
     * NGINX against {@code $secure_link_expires}.
     */
    public long getExpiresEpochSecond() {
        return expiresEpochSecond;
    }

    /**
     * @return the id of the secret the {@link #getMd5() signature} was produced with, to send in the {@code X-OFM-Kid}
     * header so NGINX can select the matching secret during key rotation; {@code null} when authentication is disabled.
     */
    public String getKid() {
        return kid;
    }

    /**
     * @return the delay in milliseconds after which the client should mint the next token (typically half the token
     * TTL), staggered per client by its own load time so bucket rollovers do not cause a synchronized refresh burst.
     */
    public long getRefreshAfterMillis() {
        return refreshAfterMillis;
    }

    /**
     * @return {@code true} iff a signature is present, i.e. the server has tile-server authentication configured and the
     * client should attach the token headers to tile-server requests.
     */
    public boolean isAuthenticationEnabled() {
        return Util.hasLength(md5);
    }
}
