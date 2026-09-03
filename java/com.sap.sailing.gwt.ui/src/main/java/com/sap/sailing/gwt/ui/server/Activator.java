package com.sap.sailing.gwt.ui.server;

import java.util.Optional;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sailing.gwt.ui.shared.racemap.MapProviderTypes;
import com.sap.sailing.gwt.ui.shared.racemap.MapsLoader;
import com.sap.sse.common.util.AccessTokenDTO;
import com.sap.sse.common.util.AccessTokenMinter;

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
     * (e.g. {@code k1:AbC-123_xyz,k2:Def-456_uvw}). The same string is configured on the tile server's NGINX
     * {@code secure_link} side (its {@code TILE_AUTH_SECRETS} {@code .env} value). Parsing, alphabet validation and
     * everything else about the tokens is handled by {@link AccessTokenMinter}; this activator only reads the raw
     * value and hands it over. When unset or blank, tile-server authentication is disabled and the client sends no
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
     * of it. Defaults to {@link AccessTokenMinter#DEFAULT_TTL_SECONDS}.
     */
    private final static String MAP_TILESERVER_AUTH_TTL_PROPERTY_NAME = "map.provider.tileserver.auth.ttl";
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
     * Mints and bucket-caches the short-lived tile-server access tokens. Initialized to a disabled minter so
     * {@link #getMapTileAccessToken()} is safe even before {@link #start(BundleContext)} has run, and replaced in
     * {@code start} with one configured from the tile-server auth properties.
     */
    private AccessTokenMinter mapTileAccessTokenMinter = new AccessTokenMinter(/* secretsConfig */ null,
            /* kidConfig */ null, /* ttlConfig */ null);

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
        // Tile-server access-token auth (NGINX secure_link): hand the raw secret list, current signing key id and token
        // TTL properties to the minter, which owns all parsing, validation, bucketing and signing. When no secrets or no
        // current kid are configured, the minter stays disabled and getMapTileAccessToken() hands out the auth-disabled
        // token so tile requests remain unauthenticated.
        mapTileAccessTokenMinter = new AccessTokenMinter(
                context.getProperty(MAP_TILESERVER_AUTH_SECRETS_PROPERTY_NAME),
                context.getProperty(MAP_TILESERVER_AUTH_KID_PROPERTY_NAME),
                context.getProperty(MAP_TILESERVER_AUTH_TTL_PROPERTY_NAME));
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
     * Mints (or returns the cached, bucket-shared) short-lived tile-server access token by delegating to the
     * {@link AccessTokenMinter}. When authentication is disabled, the auth-disabled token is returned and the
     * client sends no headers.
     */
    public AccessTokenDTO getMapTileAccessToken() {
        return mapTileAccessTokenMinter.getAccessToken();
    }
}
