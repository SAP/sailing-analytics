package com.sap.sailing.server.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.sap.sse.common.Duration;
import com.sap.sse.util.HttpUrlConnectionHelper;

class ImageUrlHealthChecker {
    private static final Duration DEFAULT_TIMEOUT = Duration.ONE_SECOND.times(10);

    private final Duration timeout;

    ImageUrlHealthChecker() {
        this(DEFAULT_TIMEOUT);
    }

    ImageUrlHealthChecker(Duration timeout) {
        this.timeout = timeout;
    }

    boolean isUrlReachable(URL url) {
        URLConnection connection = null;
        boolean result;
        try {
            connection = HttpUrlConnectionHelper.redirectConnection(url, timeout, urlConnection -> urlConnection.setConnectTimeout((int) timeout.asMillis()));
            if (connection instanceof HttpURLConnection) {
                final int responseCode = ((HttpURLConnection) connection).getResponseCode();
                result = responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE;
            } else {
                result = true;
            }
        } catch (IOException e) {
            result = false;
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
        return result;
    }

    boolean isImageAvailable(URL imageUrl) {
        URLConnection connection = null;
        boolean result;
        try {
            connection = HttpUrlConnectionHelper.redirectConnection(imageUrl, timeout,
                    urlConnection -> urlConnection.setConnectTimeout((int) timeout.asMillis()));
            if (connection instanceof HttpURLConnection) {
                final int responseCode = ((HttpURLConnection) connection).getResponseCode();
                if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                    result = false;
                }
            }
            try (InputStream inputStream = connection.getInputStream();
                    ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
                if (imageInputStream == null) {
                    result = false;
                } else {
                    final Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
                    if (!readers.hasNext()) {
                        result = false;
                    } else {
                        final ImageReader reader = readers.next();
                        try {
                            reader.setInput(imageInputStream);
                            reader.getWidth(0);
                            reader.getHeight(0);
                            result = true;
                        } finally {
                            reader.dispose();
                        }
                    }
                }
            }
        } catch (IOException e) {
            result = false;
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
        return result;
    }
}