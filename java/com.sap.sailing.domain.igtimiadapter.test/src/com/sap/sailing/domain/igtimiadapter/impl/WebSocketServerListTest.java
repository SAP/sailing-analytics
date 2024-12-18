package com.sap.sailing.domain.igtimiadapter.impl;

import static org.junit.Assert.assertFalse;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.sap.sailing.domain.igtimiadapter.persistence.PersistenceFactory;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.common.Util;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;

public class WebSocketServerListTest {
    @Rule public Timeout AbstractTracTracLiveTestTimeout = Timeout.millis(2 * 60 * 1000);

    @Test
    public void testWebSocketServerList() throws Exception {
        final ClientImpl testAppClient = new ClientImpl("7fcdd217e0aa16090edb4ad55b09ec43b2021090e209541fc9b7003c2a2b70c6",
                "aa569cf4909bdc7b0e04b11873f3c4ea20687421e010fcc25b771cca9e6f3f9a", "http", "127.0.0.1", "8888", "/igtimi/oauth/v1/authorizationcallback");
        final MongoDBConfiguration mongoTestConfig = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoDBService mongoTestService = mongoTestConfig.getService();
        RiotServer riotServer = RiotServer.create(PersistenceFactory.INSTANCE.getDomainObjectFactory(mongoTestService),
                PersistenceFactory.INSTANCE.getMongoObjectFactory(mongoTestService));
        final IgtimiConnectionFactoryImpl igtimiConnectionFactory = new IgtimiConnectionFactoryImpl(testAppClient, defaultBearerToken); // TODO bug6059: connect to the RiotServer launched above
        final Iterable<URI> serverUris = igtimiConnectionFactory.getWebsocketServers();
        assertFalse(Util.isEmpty(serverUris));
        for (final URI uri : serverUris) {
            uri.getScheme().startsWith("ws");
        }
    }
}
