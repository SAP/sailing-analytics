package com.sap.sailing.domain.igtimiadapter.impl;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;

public class WebSocketServerListTest {
    @Test
    public void testWebSocketServerList() throws IllegalStateException, ClientProtocolException, IOException, ParseException, URISyntaxException {
        final ClientImpl testAppClient = new ClientImpl("7fcdd217e0aa16090edb4ad55b09ec43b2021090e209541fc9b7003c2a2b70c6",
                "aa569cf4909bdc7b0e04b11873f3c4ea20687421e010fcc25b771cca9e6f3f9a", "http://127.0.0.1:8888/igtimi/oauth/v1/authorizationcallback");
        final IgtimiConnectionFactory igtimiConnectionFactory = new IgtimiConnectionFactoryImpl(testAppClient);
        Iterable<URI> serverUris = igtimiConnectionFactory.getWebsocketServers();
        assertFalse(Util.isEmpty(serverUris));
        for (URI uri : serverUris) {
            uri.getScheme().startsWith("ws");
        }
    }
}
