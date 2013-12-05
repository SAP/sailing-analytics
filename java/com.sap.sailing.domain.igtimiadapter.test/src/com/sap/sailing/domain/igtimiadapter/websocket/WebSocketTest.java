package com.sap.sailing.domain.igtimiadapter.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Test;

import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.igtimiadapter.Account;
import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Client;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.impl.ClientImpl;
import com.sap.sailing.domain.igtimiadapter.impl.IgtimiConnectionFactoryImpl;
import com.sap.sailing.domain.igtimiadapter.persistence.PersistenceFactory;
import com.sap.sailing.mongodb.MongoDBConfiguration;
import com.sap.sailing.mongodb.MongoDBService;

public class WebSocketTest {
    private static final Logger logger = Logger.getLogger(WebSocketTest.class.getName());

    @WebSocket
    public class SimpleEchoTestSocket {
        private final CountDownLatch closeLatch;
        private Session session;
        private final List<String> stringsReceived;
     
        public SimpleEchoTestSocket() {
            this.closeLatch = new CountDownLatch(1);
            stringsReceived = new ArrayList<>();
        }
     
        public Session getSession() {
            return session;
        }
        
        public void sendAndWait(String msg) throws InterruptedException, ExecutionException, TimeoutException {
            logger.info("Sending "+msg);
            Future<Void> future = session.getRemote().sendStringByFuture(msg);
            future.get(2, TimeUnit.SECONDS);
            logger.info("Done sending "+msg);
        }

        public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
            return this.closeLatch.await(duration, unit);
        }
     
        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            logger.info("Connection closed: "+statusCode+" - "+reason);
            this.closeLatch.countDown();
        }
     
        @OnWebSocketConnect
        public void onConnect(Session session) {
            logger.info("Got connect: "+session);
            synchronized (this) {
                this.session = session;
                notifyAll();
            }
        }

        public void closeSession() throws IOException {
            session.close(StatusCode.NORMAL, "I'm done");
        }
        
        @OnWebSocketError
        public void onError(Throwable cause) {
            logger.log(Level.SEVERE, "Error with web socket connection", cause);
        }
        
        @OnWebSocketMessage
        public void onMessage(String msg) {
            logger.info("Received "+msg);
            synchronized (this) {
                stringsReceived.add(msg);
                notifyAll();
            }
        }


        public List<String> getStringsReceived() {
            return stringsReceived;
        }
    }
    
    @Test
    public void simpleWebSockeEchoTest() throws Exception {
        String destUri = "ws://echo.websocket.org"; // wss currently doesn't seem to work with Jetty 9.0.4 WebSocket implementation
        WebSocketClient client = new WebSocketClient();
        SimpleEchoTestSocket socket = new SimpleEchoTestSocket();
        try {
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
            logger.info("Connecting to : "+echoUri);
            synchronized (socket) {
                while (socket.getSession() == null) {
                    socket.wait();
                }
            }
            socket.sendAndWait("Humba Humba");
            synchronized (socket) {
                if (socket.getStringsReceived().isEmpty()) {
                    socket.wait(2000); // wait for 2s for the echo response to arrive
                }
            }
            socket.closeSession();
            socket.awaitClose(5, TimeUnit.SECONDS);
            assertEquals(1, socket.getStringsReceived().size());
            assertEquals("Humba Humba", socket.getStringsReceived().get(0));
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Test
    public void testWebSocketConnect() throws Exception {
        final List<Fix> allFixesReceived = new ArrayList<>();
        final Client client = new ClientImpl("7fcdd217e0aa16090edb4ad55b09ec43b2021090e209541fc9b7003c2a2b70c6",
                "aa569cf4909bdc7b0e04b11873f3c4ea20687421e010fcc25b771cca9e6f3f9a", "http://127.0.0.1:8888/igtimi/oauth/v1/authorizationcallback");
        MongoDBConfiguration mongoTestConfig = MongoDBConfiguration.getDefaultTestConfiguration();
        MongoDBService mongoTestService = mongoTestConfig.getService();
        final IgtimiConnectionFactory igtimiConnectionFactory = new IgtimiConnectionFactoryImpl(client, PersistenceFactory.INSTANCE.getDomainObjectFactory(mongoTestService),
                PersistenceFactory.INSTANCE.getMongoObjectFactory(mongoTestService));
        Account account = igtimiConnectionFactory.registerAccountForWhichClientIsAuthorized("3b6cbd0522423bb1ac274ddb9e7e579c4b3be6667622271086c4fdbf30634ba9");
        IgtimiConnection conn = igtimiConnectionFactory.connect(account);
        LiveDataConnection liveDataConnection = conn.createLiveConnection(Collections.singleton("GA-EN-AAEJ"), /* receiver */ new BulkFixReceiver() {
            @Override
            public void received(Iterable<Fix> fixes) {
                Util.addAll(fixes, allFixesReceived);
            }
        });
        assertNotNull(liveDataConnection);
        assertTrue("Connection handshake not successful within 5s", liveDataConnection.waitForConnection(5000l));
        liveDataConnection.disconnect();
    }
}
