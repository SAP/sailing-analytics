package com.sap.sailing.gwt.ui.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sailing.gwt.ui.shared.racemap.MapProviderTypes;
import com.sap.sailing.gwt.ui.shared.racemap.MapTileAccessTokenDTO;
import com.sap.sailing.gwt.ui.shared.racemap.MapsLoader;

public class Activator implements BundleActivator {
    private static BundleContext context;
    private SailingServiceImpl sailingServiceToStopWhenStopping;
    private static Activator INSTANCE;

    private final static String MAP_PROVIDER_TYPE_PROPERTY_NAME = "map.provider.type";

    /**
     * Name of the system property that overrides the MapLibre vector style document URL used by the MapLibre map
     * provider (see {@code js/maps/maplibre-test-utils.js}, {@code createRaceStyle()}). The value must be a full
     * MapLibre style URL, e.g. {@code https://maptiles.sapsailing.com/styles/liberty} when pointing at a self-hosted
     * OpenFreeMap deployment. When unset (or blank) the public OpenFreeMap default
     * {@link #DEFAULT_MAP_TILESERVER_STYLE_URL} is used. Only consulted for the MapLibre provider; Google Maps is
     * unaffected.
     */
    private final static String MAP_TILESERVER_STYLE_URL_PROPERTY_NAME = "map.provider.tileserver";

    /**
     * Default MapLibre style document URL used when {@link #MAP_TILESERVER_STYLE_URL_PROPERTY_NAME} is not set: the
     * public OpenFreeMap "liberty" style.
     */
    public final static String DEFAULT_MAP_TILESERVER_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

    /**
     * Name of the system property carrying the tile-server auth secrets as a comma-separated {@code kid:secret} list
     * (e.g. {@code k1:secretA,k2:secretB}). The same secrets are configured in the tile server's NGINX
     * {@code secure_link} setup. When unset or blank, tile-server authentication is disabled and the client sends no
     * token headers.
     */
    private final static String MAP_TILESERVER_AUTH_SECRETS_PROPERTY_NAME = "map.provider.tileserver.auth.secrets";

    /**
     * Name of the system property carrying the id of the secret (a key from
     * {@link #MAP_TILESERVER_AUTH_SECRETS_PROPERTY_NAME}) that newly minted tile tokens are currently signed with. Older
     * ids stay configured on both sides until every token signed with them has expired, enabling zero-downtime rotation.
     */
    private final static String MAP_TILESERVER_AUTH_KID_PROPERTY_NAME = "map.provider.tileserver.auth.kid";

    /**
     * Name of the system property carrying the tile-token TTL / bucket length in seconds; the client refreshes at half
     * of it. Defaults to {@link #DEFAULT_MAP_TILESERVER_AUTH_TTL_SECONDS}.
     */
    private final static String MAP_TILESERVER_AUTH_TTL_PROPERTY_NAME = "map.provider.tileserver.auth.ttl";

    /**
     * Default tile-token TTL / bucket length in seconds when {@link #MAP_TILESERVER_AUTH_TTL_PROPERTY_NAME} is unset.
     */
    private final static long DEFAULT_MAP_TILESERVER_AUTH_TTL_SECONDS = 120L;
    /**
     * If the system property named after this constant is set, its value is used for Google Maps API authentication.
     * It takes precedence over the environment variable named after {@link #GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_ENV_VAR_NAME}.
     */
    private final static String GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_PROPERTY_NAME = "google.maps.authenticationparams";
    
    /**
     * If the system property named after {@link #GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_PROPERTY_NAME} is not set,
     * this environment variable is checked for Google Maps API authentication parameters.
     */
    private final static String GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_ENV_VAR_NAME = "GOOGLE_MAPS_AUTHENTICATION_PARAMS";
    
    /**
     * The system property named after this constant is expected to provide the YouTube V3 API key. Takes precedence over
     * the environment variable named after {@link #YOUTUBE_V3_API_KEY_ENV_VAR_NAME}.
     */
    private final static String YOUTUBE_V3_API_KEY_PROPERTY_NAME = "youtube.api.key";
    
    /**
     * If the system property named after {@link #YOUTUBE_V3_API_KEY_PROPERTY_NAME} is not set, this environment variable
     * is checked for the YouTube V3 API key.
     */
    private final static String YOUTUBE_V3_API_KEY_ENV_VAR_NAME = "YOUTUBE_V3_API_KEY";
    
    /**
     * Required by {@link MapsLoader#load(Runnable, com.sap.sailing.gwt.ui.client.MapAuthenticationParamsProviderAsync, com.sap.sse.gwt.client.ErrorReporter, com.sap.sailing.gwt.ui.client.StringMessages)}
     * and to be provided through a system property named
     * after {@link GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_PROPERTY_NAME}. The value would be something like
     * {@code client=abcde&channel=fghij}.
     */
    private String googleMapsLoaderAuthenticationParams;
    
    /**
     * A secret for accessing the YouTube V3 API; provided through the system property named as specified by
     * {@link #YOUTUBE_V3_API_KEY_PROPERTY_NAME}.
     */
    private String youtubeApiKey;

    /**
     * Tile-server auth secrets keyed by id, parsed from {@link #MAP_TILESERVER_AUTH_SECRETS_PROPERTY_NAME}. Empty when
     * tile-server authentication is disabled.
     */
    private final Map<String, String> mapTileServerAuthSecrets = new HashMap<>();

    /**
     * Id of the secret currently used to sign new tile tokens; {@code null} when tile-server authentication is disabled.
     */
    private String mapTileServerAuthKid;

    /**
     * Tile-token TTL / bucket length in seconds.
     */
    private long mapTileServerAuthTtlSeconds = DEFAULT_MAP_TILESERVER_AUTH_TTL_SECONDS;

    /**
     * The token handed out when tile-server authentication is disabled; carries no signature so the client sends no
     * headers. Initialized eagerly so {@link #getMapTileAccessToken()} is safe even before {@link #start(BundleContext)}
     * has run.
     */
    private MapTileAccessTokenDTO disabledMapTileToken = new MapTileAccessTokenDTO(/* md5 */ null,
            /* expiresEpochSecond */ 0L, /* kid */ null,
            (DEFAULT_MAP_TILESERVER_AUTH_TTL_SECONDS * 1000L) / 2L);

    /**
     * The most recently minted bucket token together with the bucket it is valid for, so all callers within a bucket
     * share one instance (and hence one cached serialized payload).
     */
    private final AtomicReference<CachedMapTileToken> cachedMapTileToken = new AtomicReference<>();

    public Activator() {
        INSTANCE = this;
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        googleMapsLoaderAuthenticationParams = Optional
                .ofNullable(context.getProperty(GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_PROPERTY_NAME))
                .orElse(System.getenv(GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_ENV_VAR_NAME));
        youtubeApiKey = Optional
                .ofNullable(context.getProperty(YOUTUBE_V3_API_KEY_PROPERTY_NAME))
                .orElse(System.getenv(YOUTUBE_V3_API_KEY_ENV_VAR_NAME));
        // Tile-server access-token auth (NGINX secure_link): parse the "kid:secret,kid:secret" secret list, the current
        // signing key id, and the token TTL / bucket length. When no secrets or no current kid are configured, tile
        // requests stay unauthenticated and getMapTileAccessToken() hands out the auth-disabled token.
        parseMapTileServerAuthSecrets(context.getProperty(MAP_TILESERVER_AUTH_SECRETS_PROPERTY_NAME));
        mapTileServerAuthKid = Optional.ofNullable(context.getProperty(MAP_TILESERVER_AUTH_KID_PROPERTY_NAME))
                .map(String::trim).filter(kid -> !kid.isEmpty()).orElse(/* other */ null);
        mapTileServerAuthTtlSeconds = Optional.ofNullable(context.getProperty(MAP_TILESERVER_AUTH_TTL_PROPERTY_NAME))
                .map(String::trim).filter(ttl -> !ttl.isEmpty()).map(Long::parseLong)
                .orElse(DEFAULT_MAP_TILESERVER_AUTH_TTL_SECONDS);
        disabledMapTileToken = new MapTileAccessTokenDTO(/* md5 */ null, /* expiresEpochSecond */ 0L, /* kid */ null,
                (mapTileServerAuthTtlSeconds * 1000L) / 2L);
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        if (sailingServiceToStopWhenStopping != null) {
            sailingServiceToStopWhenStopping.stop();
        }
    }
    
    public static Activator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Activator();
        }
        return INSTANCE;
    }
    
    public static BundleContext getDefault() {
        return context;
    }
    
    /**
     * Returns a URL parameter string, e.g., like {@code client=abcde&channel=fghij}, provided to this activator through
     * a system property named after {@link GOOGLE_MAPS_LOADER_AUTHENTICATION_PARAMS_PROPERTY_NAME}. Won't be {@code null}
     * because the entire bundle won't activate if not set.
     */
    public String getGoogleMapsLoaderAuthenticationParams() {
        return googleMapsLoaderAuthenticationParams;
    }
    
    public String getYoutubeApiKey() {
        return youtubeApiKey;
    }

    public void setSailingService(SailingServiceImpl sailingServiceImpl) {
        sailingServiceToStopWhenStopping = sailingServiceImpl;
    }

    public MapProviderTypes getMapProviderType() {
        return Optional.ofNullable(context.getProperty(MAP_PROVIDER_TYPE_PROPERTY_NAME))
                .map(mapTypeName -> MapProviderTypes.valueOf(mapTypeName)).orElse(MapProviderTypes.GOOGLE);
    }

    /**
     * Returns the MapLibre vector style document URL to use, taken from the system property named after
     * {@link #MAP_TILESERVER_STYLE_URL_PROPERTY_NAME} when set to a non-blank value, otherwise
     * {@link #DEFAULT_MAP_TILESERVER_STYLE_URL} (the public OpenFreeMap "liberty" style). Never {@code null}.
     */
    public String getMapTileServerStyleUrl() {
        return Optional.ofNullable(context.getProperty(MAP_TILESERVER_STYLE_URL_PROPERTY_NAME))
                .map(String::trim).filter(url -> !url.isEmpty()).orElse(DEFAULT_MAP_TILESERVER_STYLE_URL);
    }

    /**
     * Mints (or returns the cached, bucket-shared) short-lived tile-server access token. All callers within one time
     * bucket receive the identical instance, so the result-caching RPC servlet serializes it only once per bucket. When
     * no secret or no current key id is configured, the auth-disabled token is returned and the client sends no headers.
     */
    public MapTileAccessTokenDTO getMapTileAccessToken() {
        final MapTileAccessTokenDTO result;
        final String currentKid = mapTileServerAuthKid;
        final String secret = currentKid == null ? null : mapTileServerAuthSecrets.get(currentKid);
        if (secret == null || secret.isEmpty()) {
            result = disabledMapTileToken;
        } else {
            result = getOrMintBucketToken(currentKid, secret);
        }
        return result;
    }

    /**
     * Parses the {@code kid:secret,kid:secret} secret list into {@link #mapTileServerAuthSecrets}. Malformed or empty
     * entries are skipped; a {@code null} or blank configuration leaves the map empty (authentication disabled).
     */
    private void parseMapTileServerAuthSecrets(final String secretsConfig) {
        mapTileServerAuthSecrets.clear();
        if (secretsConfig != null) {
            final String trimmed = secretsConfig.trim();
            if (!trimmed.isEmpty()) {
                for (final String entry : trimmed.split(",")) {
                    final int colonIndex = entry.indexOf(':');
                    if (colonIndex > 0 && colonIndex < entry.length() - 1) {
                        final String kid = entry.substring(0, colonIndex).trim();
                        final String secret = entry.substring(colonIndex + 1).trim();
                        if (!kid.isEmpty() && !secret.isEmpty()) {
                            mapTileServerAuthSecrets.put(kid, secret);
                        }
                    }
                }
            }
        }
    }

    /**
     * Fast path for {@link #getMapTileAccessToken()}: returns the cached token when it still belongs to the current
     * bucket and key id, otherwise mints (and caches) a new one under a lock.
     */
    private MapTileAccessTokenDTO getOrMintBucketToken(final String kid, final String secret) {
        final long nowEpochSecond = System.currentTimeMillis() / 1000L;
        final long bucketLengthSeconds = mapTileServerAuthTtlSeconds;
        final long bucketEndEpochSecond = ((nowEpochSecond / bucketLengthSeconds) + 1L) * bucketLengthSeconds;
        final MapTileAccessTokenDTO result;
        final CachedMapTileToken cached = cachedMapTileToken.get();
        if (cached != null && cached.getBucketEndEpochSecond() == bucketEndEpochSecond && kid.equals(cached.getKid())) {
            result = cached.getToken();
        } else {
            result = mintAndCacheBucketToken(kid, secret, bucketEndEpochSecond, bucketLengthSeconds);
        }
        return result;
    }

    /**
     * Mints a token for the given bucket and caches it, double-checking under the lock so concurrent callers in the same
     * bucket mint only once. The token is signed to expire one further bucket length past the bucket boundary, giving
     * every token between one and two bucket lengths of remaining validity so a client refreshing at half the TTL always
     * holds a comfortably valid token.
     */
    private synchronized MapTileAccessTokenDTO mintAndCacheBucketToken(final String kid, final String secret,
            final long bucketEndEpochSecond, final long bucketLengthSeconds) {
        final MapTileAccessTokenDTO result;
        final CachedMapTileToken existing = cachedMapTileToken.get();
        if (existing != null && existing.getBucketEndEpochSecond() == bucketEndEpochSecond
                && kid.equals(existing.getKid())) {
            result = existing.getToken();
        } else {
            final long expiresEpochSecond = bucketEndEpochSecond + bucketLengthSeconds;
            final String md5 = computeSecureLinkMd5(expiresEpochSecond, secret);
            final long refreshAfterMillis = (bucketLengthSeconds * 1000L) / 2L;
            final MapTileAccessTokenDTO token = new MapTileAccessTokenDTO(md5, expiresEpochSecond, kid,
                    refreshAfterMillis);
            cachedMapTileToken.set(new CachedMapTileToken(token, bucketEndEpochSecond, kid));
            result = token;
        }
        return result;
    }

    /**
     * Computes the NGINX {@code secure_link} MD5 for the given expiry and secret. The signed bytes are the decimal
     * expiry, a single space, then the secret (matching the {@code secure_link_md5 "$secure_link_expires $ofm_secret"}
     * expression); the result is base64url without padding (matching {@code openssl base64 | tr +/ -_ | tr -d =}).
     */
    private static String computeSecureLinkMd5(final long expiresEpochSecond, final String secret) {
        final String result;
        try {
            final MessageDigest md5 = MessageDigest.getInstance(/* algorithm */ "MD5");
            final byte[] raw = md5
                    .digest((expiresEpochSecond + " " + secret).getBytes(StandardCharsets.US_ASCII));
            result = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (final NoSuchAlgorithmException e) {
            // MD5 is a mandatory MessageDigest algorithm on every conformant JRE, so this cannot occur in practice.
            throw new IllegalStateException("MD5 MessageDigest algorithm unexpectedly unavailable", e);
        }
        return result;
    }

    /**
     * Immutable holder pairing a minted token with the bucket boundary and key id it was minted for, used to decide
     * whether the cached token can be reused.
     */
    private static final class CachedMapTileToken {
        private final MapTileAccessTokenDTO token;
        private final long bucketEndEpochSecond;
        private final String kid;

        CachedMapTileToken(final MapTileAccessTokenDTO token, final long bucketEndEpochSecond, final String kid) {
            this.token = token;
            this.bucketEndEpochSecond = bucketEndEpochSecond;
            this.kid = kid;
        }

        MapTileAccessTokenDTO getToken() {
            return token;
        }

        long getBucketEndEpochSecond() {
            return bucketEndEpochSecond;
        }

        String getKid() {
            return kid;
        }
    }
}
