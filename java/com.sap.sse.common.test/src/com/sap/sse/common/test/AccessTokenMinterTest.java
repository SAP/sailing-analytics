package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.util.AccessTokenDTO;
import com.sap.sse.common.util.AccessTokenMinter;

/**
 * Unit tests for {@link AccessTokenMinter}, covering its two operating states (authentication disabled vs. enabled), the
 * secret/kid parsing pre-conditions, the NGINX {@code secure_link} MD5 signing, the bucketing post-conditions (expiry
 * window, refresh delay) and the per-bucket instance caching. The signing is checked both end-to-end (recomputing the
 * MD5 from the token's own expiry and the configured secret) and against a pinned golden vector so a change to the
 * algorithm cannot pass unnoticed.
 */
public class AccessTokenMinterTest {
    /**
     * A pinned {@code base64url(MD5("1700000000 golden-secret"))} without padding, produced independently with
     * {@code printf '%s' "1700000000 golden-secret" | openssl md5 -binary | openssl base64 | tr '+/' '-_' | tr -d '='},
     * so it fixes the wire format the NGINX side expects.
     */
    private static final String GOLDEN_MD5 = "szKP-UNYejfnSU64O63wsg";

    /**
     * Recomputes the expected {@code secure_link} signature exactly as {@link AccessTokenMinter} must, so a token's MD5
     * can be validated against the configured secret and expiry without reaching into the minter's private method.
     */
    private static String expectedMd5(final long expiresEpochSecond, final String secret) {
        final String result;
        try {
            final MessageDigest md5 = MessageDigest.getInstance(/* algorithm */ "MD5");
            final byte[] raw = md5.digest((expiresEpochSecond + " " + secret).getBytes(StandardCharsets.US_ASCII));
            result = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 MessageDigest algorithm unexpectedly unavailable", e);
        }
        return result;
    }

    @Test
    public void goldenVectorPinsSigningAlgorithm() {
        assertEquals(GOLDEN_MD5, expectedMd5(/* expiresEpochSecond */ 1700000000L, /* secret */ "golden-secret"));
    }

    @Test
    public void disabledWhenSecretsConfigNull() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ null, /* kidConfig */ "k1",
                /* ttlConfigInSeconds */ null);
        final AccessTokenDTO token = minter.getAccessToken();
        assertFalse(token.isAuthenticationEnabled());
        assertNull(token.getMd5());
        assertNull(token.getKid());
    }

    @Test
    public void disabledWhenSecretsConfigBlank() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "   ", /* kidConfig */ "k1",
                /* ttlConfigInSeconds */ null);
        assertFalse(minter.getAccessToken().isAuthenticationEnabled());
    }

    @Test
    public void disabledWhenKidConfigNull() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ null, /* ttlConfigInSeconds */ null);
        assertFalse(minter.getAccessToken().isAuthenticationEnabled());
    }

    @Test
    public void disabledWhenKidConfigBlank() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "  ", /* ttlConfigInSeconds */ null);
        assertFalse(minter.getAccessToken().isAuthenticationEnabled());
    }

    @Test
    public void disabledWhenCurrentKidNotAmongSecrets() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "k2", /* ttlConfigInSeconds */ null);
        assertFalse(minter.getAccessToken().isAuthenticationEnabled());
    }

    @Test
    public void disabledTokenStillCarriesConfiguredRefreshDelay() {
        // A disabled minter must still tell the client when to poll again, using half the configured TTL.
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ null, /* kidConfig */ null,
                /* ttlConfigInSeconds */ "60");
        assertEquals(/* expected */ 30000L, minter.getAccessToken().getRefreshAfterMillis());
    }

    @Test
    public void mintsEnabledTokenForValidConfig() {
        final String secret = "good_secret-123";
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:" + secret,
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ null);
        final AccessTokenDTO token = minter.getAccessToken();
        assertTrue(token.isAuthenticationEnabled());
        assertEquals(/* expected */ "k1", token.getKid());
        assertEquals(expectedMd5(token.getExpiresEpochSecond(), secret), token.getMd5());
    }

    @Test
    public void expiryLiesBetweenOneAndTwoBucketLengthsAhead() {
        final long ttl = AccessTokenMinter.DEFAULT_TTL_SECONDS;
        final long nowBefore = System.currentTimeMillis() / 1000L;
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ null);
        final AccessTokenDTO token = minter.getAccessToken();
        final long nowAfter = System.currentTimeMillis() / 1000L;
        // The token is signed to expire between one and two bucket lengths ahead, and always on a bucket boundary.
        assertTrue(token.getExpiresEpochSecond() > nowBefore + ttl);
        assertTrue(token.getExpiresEpochSecond() <= nowAfter + 2L * ttl);
        assertEquals(/* expected */ 0L, token.getExpiresEpochSecond() % ttl);
    }

    @Test
    public void usesDefaultTtlWhenTtlConfigNull() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ null);
        final long expectedRefreshMillis = (AccessTokenMinter.DEFAULT_TTL_SECONDS * 1000L) / 2L;
        assertEquals(expectedRefreshMillis, minter.getAccessToken().getRefreshAfterMillis());
    }

    @Test
    public void usesDefaultTtlWhenTtlConfigBlank() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ "  ");
        final long expectedRefreshMillis = (AccessTokenMinter.DEFAULT_TTL_SECONDS * 1000L) / 2L;
        assertEquals(expectedRefreshMillis, minter.getAccessToken().getRefreshAfterMillis());
    }

    @Test
    public void honoursConfiguredTtl() {
        final long ttl = 60L;
        final long nowBefore = System.currentTimeMillis() / 1000L;
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ Long.toString(ttl));
        final AccessTokenDTO token = minter.getAccessToken();
        final long nowAfter = System.currentTimeMillis() / 1000L;
        assertEquals(/* expected */ (ttl * 1000L) / 2L, token.getRefreshAfterMillis());
        assertTrue(token.getExpiresEpochSecond() > nowBefore + ttl);
        assertTrue(token.getExpiresEpochSecond() <= nowAfter + 2L * ttl);
        assertEquals(/* expected */ 0L, token.getExpiresEpochSecond() % ttl);
    }

    @Test
    public void skipsSecretViolatingAlphabetButKeepsValidEntries() {
        // The second entry's secret contains a space and must be dropped, while the first stays usable.
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret,k2:bad secret",
                /* kidConfig */ "k2", /* ttlConfigInSeconds */ null);
        assertFalse(minter.getAccessToken().isAuthenticationEnabled());
        final AccessTokenMinter usingValid = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret,k2:bad secret",
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ null);
        assertTrue(usingValid.getAccessToken().isAuthenticationEnabled());
    }

    @Test
    public void skipsEntryWithIllegalKid() {
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k 1:good_secret",
                /* kidConfig */ "k 1", /* ttlConfigInSeconds */ null);
        assertFalse(minter.getAccessToken().isAuthenticationEnabled());
    }

    @Test
    public void trimsSurroundingWhitespaceAroundEntries() {
        final String secret = "secret_one-2";
        final AccessTokenMinter minter = new AccessTokenMinter(
                /* secretsConfig */ "  k1:" + secret + " , k2:secrettwo ", /* kidConfig */ "k1", /* ttlConfigInSeconds */ null);
        final AccessTokenDTO token = minter.getAccessToken();
        assertTrue(token.isAuthenticationEnabled());
        assertEquals(expectedMd5(token.getExpiresEpochSecond(), secret), token.getMd5());
    }

    @Test
    public void lastDuplicateKidWins() {
        final String winningSecret = "secrettwo";
        final AccessTokenMinter minter = new AccessTokenMinter(
                /* secretsConfig */ "k1:secretone,k1:" + winningSecret, /* kidConfig */ "k1", /* ttlConfigInSeconds */ null);
        final AccessTokenDTO token = minter.getAccessToken();
        assertEquals(expectedMd5(token.getExpiresEpochSecond(), winningSecret), token.getMd5());
    }

    @Test
    public void mintsIdenticalTokenTwiceWithinSameBucket() {
        // Two mints a moment apart fall in the same (default 120s) bucket, so the minter must hand back the very same
        // instance, and hence the same expiry and signature, letting the result-caching RPC servlet serialize it once.
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ null);
        final AccessTokenDTO first = minter.getAccessToken();
        final AccessTokenDTO second = minter.getAccessToken();
        assertSame(first, second);
        assertEquals(first.getExpiresEpochSecond(), second.getExpiresEpochSecond());
        assertEquals(first.getMd5(), second.getMd5());
    }

    @Test
    public void mintsFreshTokenAfterBucketRollover() throws InterruptedException {
        // With a one-second bucket, waiting until the wall clock has entered the next bucket must yield a brand-new
        // token instance whose expiry has advanced by at least one bucket length, proving the cache keys on the bucket.
        final long ttl = 1L;
        final AccessTokenMinter minter = new AccessTokenMinter(/* secretsConfig */ "k1:good_secret",
                /* kidConfig */ "k1", /* ttlConfigInSeconds */ Long.toString(ttl));
        final AccessTokenDTO firstBucketToken = minter.getAccessToken();
        final long firstBucketEndEpochSecond = firstBucketToken.getExpiresEpochSecond() - ttl;
        // Sleep in short steps only until the current second reaches the next bucket boundary, i.e. at most one TTL.
        while (System.currentTimeMillis() / 1000L < firstBucketEndEpochSecond) {
            Thread.sleep(/* millis */ 25L);
        }
        final AccessTokenDTO secondBucketToken = minter.getAccessToken();
        assertNotSame(firstBucketToken, secondBucketToken);
        assertTrue(secondBucketToken.getExpiresEpochSecond() >= firstBucketToken.getExpiresEpochSecond() + ttl);
        assertNotEquals(firstBucketToken.getMd5(), secondBucketToken.getMd5());
    }
}
