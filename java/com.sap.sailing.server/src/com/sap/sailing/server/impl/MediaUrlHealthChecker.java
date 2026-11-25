package com.sap.sailing.server.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sap.sse.common.Duration;
import com.sap.sse.util.HttpUrlConnectionHelper;

class MediaUrlHealthChecker {
    private static final Logger logger = Logger.getLogger(MediaUrlHealthChecker.class.getName());
    private static final Duration DEFAULT_TIMEOUT = Duration.ONE_SECOND.times(10);
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final Duration timeout;

    MediaUrlHealthChecker() {
        this(DEFAULT_TIMEOUT);
    }

    MediaUrlHealthChecker(final Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * An empty result means that rate limiting prevented an availability verdict.
     */
    Optional<Boolean> isUrlReachable(final URL url) {
        URLConnection connection = null;
        Optional<Boolean> result;
        try {
            connection = HttpUrlConnectionHelper.redirectConnection(url, timeout,
                    urlConnection -> urlConnection.setConnectTimeout((int) timeout.asMillis()));
            result = getAvailability(connection);
        } catch (final IOException e) {
            result = Optional.of(/* value */ false);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
        return result;
    }

    /**
     * An empty result means that rate limiting prevented an image availability verdict.
     */
    Optional<Boolean> isImageAvailable(final URL imageUrl) {
        URLConnection connection = null;
        Optional<Boolean> result;
        try {
            connection = HttpUrlConnectionHelper.redirectConnection(imageUrl, timeout,
                    urlConnection -> urlConnection.setConnectTimeout((int) timeout.asMillis()));
            result = getAvailability(connection);
            if (result.orElse(/* other */ false)) {
                try (final InputStream inputStream = connection.getInputStream();
                        final ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
                    if (imageInputStream == null) {
                        result = Optional.of(/* value */ false);
                    } else {
                        final Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
                        if (!readers.hasNext()) {
                            result = Optional.of(/* value */ false);
                        } else {
                            final ImageReader reader = readers.next();
                            try {
                                reader.setInput(imageInputStream);
                                reader.getWidth(/* imageIndex */ 0);
                                reader.getHeight(/* imageIndex */ 0);
                                result = Optional.of(/* value */ true);
                            } finally {
                                reader.dispose();
                            }
                        }
                    }
                }
            }
        } catch (final IOException e) {
            result = Optional.of(/* value */ false);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
        return result;
    }

    private Optional<Boolean> getAvailability(final URLConnection connection) throws IOException {
        final Optional<Boolean> result;
        if (connection instanceof HttpURLConnection) {
            final int responseCode = ((HttpURLConnection) connection).getResponseCode();
            if (responseCode == HTTP_TOO_MANY_REQUESTS) {
                logger.info("HTTP 429 (Too Many Requests) checking media URL " + connection.getURL()
                        + "; no availability verdict reached");
                result = Optional.empty();
            } else {
                result = Optional.of(responseCode >= HttpURLConnection.HTTP_OK
                        && responseCode < HttpURLConnection.HTTP_MULT_CHOICE);
            }
        } else {
            result = Optional.of(/* value */ true);
        }
        return result;
    }
}