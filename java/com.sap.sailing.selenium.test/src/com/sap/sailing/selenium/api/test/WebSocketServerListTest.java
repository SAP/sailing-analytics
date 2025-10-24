package com.sap.sailing.selenium.api.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Timeout;

import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.Util;

@Timeout(value = 2, unit = TimeUnit.MINUTES) // 2 minutes timeout for the test
public class WebSocketServerListTest extends AbstractSeleniumTest {
    @SeleniumTestCase
    public void testWebSocketServerList() throws Exception {
        final IgtimiConnectionFactory igtimiConnectionFactory = IgtimiConnectionFactory.create(
                new URL(getContextRoot()), /* defaultBearerToken */ null); // websocket servers list also for anonymous user
        final Iterable<URI> serverUris = igtimiConnectionFactory.getOrCreateConnection().getWebsocketServers();
        assertFalse(Util.isEmpty(serverUris));
        for (final URI uri : serverUris) {
            uri.getScheme().startsWith("ws");
        }
    }
}
