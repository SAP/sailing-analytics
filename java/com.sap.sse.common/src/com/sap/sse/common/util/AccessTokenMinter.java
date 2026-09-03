package com.sap.sse.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Mints and bucket-caches the short-lived, signed access tokens that the browser attaches to requests against a
 * resource protected by an NGINX {@code secure_link} check so that check can authenticate them. All the
 * token domain logic lives here (secret parsing/validation, bucketing, MD5 signing, caching), keeping the
 * caller responsible only for reading the configuration properties and handing them to this service.
 * <p>
 * The token is deliberately identical for every caller within one time bucket, so the same
 * {@link AccessTokenDTO} instance is returned across a bucket; because that DTO is a {@code CacheableRPCResult},
 * the result-caching RPC servlet then serializes it only once per bucket. A token is signed to expire one further
 * bucket length past the bucket boundary, giving every token between one and two bucket lengths of remaining validity,
 * so a client refreshing at half the TTL always holds a comfortably valid token.
 * <p>
 * When no secret or no current key id is configured, this minter is in the disabled state: it always returns the
 * {@link #disabledToken auth-disabled token}, whose {@link AccessTokenDTO#isAuthenticationEnabled()} is
 * {@code false}, so the client sends no token headers and the protected resource stays effectively public.
 */
public class AccessTokenMinter {
    private static final Logger logger = Logger.getLogger(AccessTokenMinter.class.getName());

    /**
     * Allowed characters for a kid and for a secret: the URL-safe base64url alphabet. Kept identical to the constraint
     * enforced on the NGINX side (openfreemap {@code ssh_lib/tasks.py} and {@code http_host_lib/nginx.py}), so a value
     * that is accepted here is also accepted there and vice versa. Generate secrets accordingly, e.g. with
     * {@code openssl rand -base64 48 | tr '+/' '-_' | tr -d '='}.
     */
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    /**
     * Default token TTL / bucket length in seconds when no TTL is configured.
     */
    public static final long DEFAULT_TTL_SECONDS = 120L;

    /**
     * Auth secrets keyed by id, parsed from the secrets configuration. Empty when authentication is
     * disabled.
     */
    private final Map<String, String> secretsByKid;

    /**
     * Id of the secret currently used to sign new tokens; {@code null} when authentication is disabled.
     */
    private final String currentKid;

    /**
     * Token TTL / bucket length in seconds.
     */
    private final long ttlSeconds;

    /**
     * The token handed out when authentication is disabled; carries no signature so the client sends no headers.
     */
    private final AccessTokenDTO disabledToken;

    /**
     * The most recently minted bucket token together with the bucket it is valid for, so all callers within a bucket
     * share one instance (and hence one cached serialized payload).
     */
    private final AtomicReference<CachedAccessToken> cachedToken = new AtomicReference<>();

    /**
     * Builds a minter from the raw configuration strings as read from the bundle/system properties by
     * the caller (for example an OSGi bundle activator that reads them from bundle/system properties).
     *
     * @param secretsConfig
     *            the comma-separated {@code kid:secret} list (e.g. {@code k1:AbC-123_xyz,k2:Def-456_uvw}), or
     *            {@code null}/blank to disable authentication. The comma separates entries and the first colon
     *            separates a kid from its secret, so both kids and secrets must match the URL-safe base64url alphabet
     *            {@link #ACCESS_TOKEN_PATTERN} (no commas, colons, spaces or quotes inside a secret; there is no
     *            escaping). Entries that are malformed or violate that alphabet are skipped with a warning rather than
     *            aborting server start-up.
     * @param kidConfig
     *            the id of the secret to sign new tokens with (a key from {@code secretsConfig}), or {@code null}/blank
     *            to disable authentication.
     * @param ttlConfig
     *            the token TTL / bucket length in seconds as a decimal string, or {@code null}/blank to use
     *            {@link #DEFAULT_TTL_SECONDS}.
     */
    public AccessTokenMinter(final String secretsConfig, final String kidConfig, final String ttlConfig) {
        secretsByKid = parseSecrets(secretsConfig);
        currentKid = Optional.ofNullable(kidConfig).map(String::trim).filter(kid -> !kid.isEmpty())
                .orElse(/* other */ null);
        ttlSeconds = Optional.ofNullable(ttlConfig).map(String::trim).filter(ttl -> !ttl.isEmpty())
                .map(Long::parseLong).orElse(DEFAULT_TTL_SECONDS);
        disabledToken = new AccessTokenDTO(/* md5 */ null, /* expiresEpochSecond */ 0L, /* kid */ null,
                (ttlSeconds * 1000L) / 2L);
    }

    /**
     * Mints (or returns the cached, bucket-shared) short-lived access token. All callers within one time
     * bucket receive the identical instance. When no secret or no current key id is configured, the auth-disabled token
     * is returned and the client sends no headers.
     */
    public AccessTokenDTO getAccessToken() {
        final AccessTokenDTO result;
        final String secret = currentKid == null ? null : secretsByKid.get(currentKid);
        if (secret == null || secret.isEmpty()) {
            result = disabledToken;
        } else {
            result = getOrMintBucketToken(currentKid, secret);
        }
        return result;
    }

    /**
     * Parses the {@code kid:secret,kid:secret} list into a {@code kid -> secret} map, enforcing the
     * {@link #ACCESS_TOKEN_PATTERN} alphabet on both kids and secrets. Malformed or alphabet-violating entries are
     * skipped with a warning; a {@code null} or blank configuration yields an empty map (authentication disabled). The
     * same constraint is enforced on the NGINX side, so an accepted value works identically on both ends.
     */
    private static Map<String, String> parseSecrets(final String secretsConfig) {
        final Map<String, String> result = new HashMap<>();
        if (secretsConfig != null) {
            final String trimmed = secretsConfig.trim();
            if (!trimmed.isEmpty()) {
                for (final String rawEntry : trimmed.split(",")) {
                    final String entry = rawEntry.trim();
                    final int colonIndex = entry.indexOf(':');
                    final String kid = colonIndex > 0 ? entry.substring(0, colonIndex) : "";
                    final String secret = colonIndex > 0 && colonIndex < entry.length() - 1
                            ? entry.substring(colonIndex + 1) : "";
                    final boolean valid = ACCESS_TOKEN_PATTERN.matcher(kid).matches()
                            && ACCESS_TOKEN_PATTERN.matcher(secret).matches();
                    if (valid) {
                        result.put(kid, secret);
                    } else if (!entry.isEmpty()) {
                        // Do not log the secret; only the (non-sensitive) kid portion, so a misconfiguration is
                        // diagnosable without leaking key material into the server log.
                        logger.log(Level.WARNING, "Skipping malformed auth secret entry for kid \"{0}\": "
                                + "kid and secret must both match [A-Za-z0-9_-] (no commas, colons, spaces or quotes).",
                                kid);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Fast path for {@link #getAccessToken()}: returns the cached token when it still belongs to the current
     * bucket and key id, otherwise mints (and caches) a new one under a lock.
     */
    private AccessTokenDTO getOrMintBucketToken(final String kid, final String secret) {
        final long nowEpochSecond = System.currentTimeMillis() / 1000L;
        final long bucketLengthSeconds = ttlSeconds;
        final long bucketEndEpochSecond = ((nowEpochSecond / bucketLengthSeconds) + 1L) * bucketLengthSeconds;
        final AccessTokenDTO result;
        final CachedAccessToken cached = cachedToken.get();
        if (cached != null && cached.getBucketEndEpochSecond() == bucketEndEpochSecond && kid.equals(cached.getKid())) {
            result = cached.getToken();
        } else {
            result = mintAndCacheBucketToken(kid, secret, bucketEndEpochSecond, bucketLengthSeconds);
        }
        return result;
    }

    /**
     * Mints a token for the given bucket and caches it, double-checking under the lock so concurrent callers in the same
     * bucket mint only once.
     */
    private synchronized AccessTokenDTO mintAndCacheBucketToken(final String kid, final String secret,
            final long bucketEndEpochSecond, final long bucketLengthSeconds) {
        final AccessTokenDTO result;
        final CachedAccessToken existing = cachedToken.get();
        if (existing != null && existing.getBucketEndEpochSecond() == bucketEndEpochSecond
                && kid.equals(existing.getKid())) {
            result = existing.getToken();
        } else {
            final long expiresEpochSecond = bucketEndEpochSecond + bucketLengthSeconds;
            final String md5 = computeSecureLinkMd5(expiresEpochSecond, secret);
            final long refreshAfterMillis = (bucketLengthSeconds * 1000L) / 2L;
            final AccessTokenDTO token = new AccessTokenDTO(md5, expiresEpochSecond, kid,
                    refreshAfterMillis);
            cachedToken.set(new CachedAccessToken(token, bucketEndEpochSecond, kid));
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
            final byte[] raw = md5.digest((expiresEpochSecond + " " + secret).getBytes(StandardCharsets.US_ASCII));
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
    private static final class CachedAccessToken {
        private final AccessTokenDTO token;
        private final long bucketEndEpochSecond;
        private final String kid;

        CachedAccessToken(final AccessTokenDTO token, final long bucketEndEpochSecond, final String kid) {
            this.token = token;
            this.bucketEndEpochSecond = bucketEndEpochSecond;
            this.kid = kid;
        }

        AccessTokenDTO getToken() {
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
