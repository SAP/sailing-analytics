package com.sap.sailing.selenium.test.pages;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;

/** Forwards the real application while allowing a test to delay or reorder individual GWT RPC responses. */
final class GwtRpcResponseProxy implements Closeable {
    private final Server server;
    private final String upstreamRoot;
    private final List<ResponseGate> gates = new CopyOnWriteArrayList<>();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    GwtRpcResponseProxy(final String upstreamRoot) throws Exception {
        this.upstreamRoot = upstreamRoot;
        server = new Server(new InetSocketAddress("localhost", /* port */ 0));
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                    final HttpServletResponse response) throws IOException {
                try {
                    forward(request, response);
                } catch (final IOException | RuntimeException e) {
                    failure.compareAndSet(/* expectedValue */ null, e);
                    throw e;
                } finally {
                    baseRequest.setHandled(true);
                }
            }
        });
        server.start();
    }

    String getRoot() {
        return "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort() + "/";
    }

    ResponseGate hold(final String method) {
        return delay(method, /* requiredText */ "", /* delayMillis */ 0);
    }

    ResponseGate delay(final String method, final String requiredText, final long delayMillis) {
        final ResponseGate result = new ResponseGate(method, requiredText, delayMillis);
        gates.add(result);
        return result;
    }

    void assertHealthy() {
        if (failure.get() != null) {
            throw new AssertionError("RPC response proxy failed", failure.get());
        }
    }

    private void forward(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final byte[] body = readAll(request.getInputStream());
        final String query = request.getQueryString();
        final URL url = new URL(upstreamRoot + request.getRequestURI().substring(/* beginIndex */ 1)
                + (query == null ? "" : "?" + query));
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(request.getMethod());
        for (final String header : Collections.list(request.getHeaderNames())) {
            if (isForwardedHeader(header) && !"Host".equalsIgnoreCase(header) && !"Accept-Encoding".equalsIgnoreCase(header)) {
                connection.setRequestProperty(header, request.getHeader(header));
            }
        }
        connection.setRequestProperty("Accept-Encoding", "identity");
        try {
            if (body.length > 0) {
                connection.setDoOutput(true);
                connection.getOutputStream().write(body);
                connection.getOutputStream().close();
            }
            final int status = connection.getResponseCode();
            final byte[] reply;
            try (final InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                reply = input == null ? new byte[0] : readAll(input);
            }
            final String payload = new String(body, StandardCharsets.UTF_8);
            ResponseGate matchingGate = null;
            for (final ResponseGate gate : gates) {
                if (payload.contains("|" + gate.method + "|") && payload.contains(gate.requiredText)
                        && gate.claimed.compareAndSet(/* expectedValue */ false, /* newValue */ true)) {
                    matchingGate = gate;
                    gate.awaitRelease();
                    break;
                }
            }
            response.setStatus(status);
            for (final Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                if (header.getKey() != null && isForwardedHeader(header.getKey())) {
                    for (final String value : header.getValue()) {
                        response.addHeader(header.getKey(), value);
                    }
                }
            }
            response.getOutputStream().write(reply);
            response.flushBuffer();
            if (matchingGate != null) {
                matchingGate.delivered.set(true);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean isForwardedHeader(final String name) {
        return !"Connection".equalsIgnoreCase(name) && !"Transfer-Encoding".equalsIgnoreCase(name)
                && !"Content-Length".equalsIgnoreCase(name);
    }

    private static byte[] readAll(final InputStream input) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            result.write(buffer, /* off */ 0, count);
        }
        return result.toByteArray();
    }

    @Override
    public void close() throws IOException {
        gates.forEach(ResponseGate::release);
        try {
            server.stop();
        } catch (final Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IOException("Cannot stop RPC response proxy", e);
        }
    }

    static final class ResponseGate {
        private final String method;
        private final String requiredText;
        private final long delayMillis;
        private final AtomicBoolean claimed = new AtomicBoolean();
        private final AtomicBoolean delivered = new AtomicBoolean();
        private final CountDownLatch received = new CountDownLatch(/* count */ 1);
        private final CountDownLatch released = new CountDownLatch(/* count */ 1);

        private ResponseGate(final String method, final String requiredText, final long delayMillis) {
            this.method = method;
            this.requiredText = requiredText;
            this.delayMillis = delayMillis;
        }

        void awaitResponse() throws InterruptedException {
            if (!received.await(/* timeout */ 60, TimeUnit.SECONDS)) {
                throw new AssertionError("No response received for " + method);
            }
        }

        boolean isDelivered() {
            return delivered.get();
        }

        void release() {
            released.countDown();
        }

        private void awaitRelease() throws IOException {
            received.countDown();
            try {
                final boolean explicitlyReleased = released.await(delayMillis == 0 ? 60000 : delayMillis, TimeUnit.MILLISECONDS);
                if (!explicitlyReleased && delayMillis == 0) {
                    throw new IOException("Response gate was not released for " + method);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while holding " + method, e);
            }
        }
    }
}
