package com.sap.sailing.selenium.api.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.websocket.LiveDataConnectionWrapper;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.Util;

@Timeout(value = 1, unit = TimeUnit.MINUTES)
public class WebSocketTest extends AbstractSeleniumTest {
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
    
    @Disabled("echo.websocket.org is (2021-08-19) down; http://www.websocket.org/index.html says ''Service no longer available''")
    @SeleniumTestCase
    public void simpleWebSocketEchoTest() throws Exception {
        String destUri = "ws://echo.websocket.org"; // wss currently doesn't seem to work with Jetty 9.0.4 WebSocket implementation
        WebSocketClient client = new WebSocketClient();
        SimpleEchoTestSocket socket = new SimpleEchoTestSocket();
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
    }
    
    @SeleniumTestCase
    public void testWebSocketConnect() throws Exception {
        final List<Fix> allFixesReceived = new ArrayList<>();
        final IgtimiConnectionFactory igtimiConnectionFactory = IgtimiConnectionFactory.create(new URL(getContextRoot()),
                /* defaultBearerToken */ null);
        // the following is an access token for an account allowing axel.uhl@gmx.de to access
        // the data from baur@stg-academy.org, particularly containing the Berlin test data
        IgtimiConnection conn = igtimiConnectionFactory.getOrCreateConnection();
        LiveDataConnection liveDataConnection = conn.getOrCreateLiveConnection(Collections.singleton("GA-EN-AAEJ"));
        LiveDataConnection redundantSecondSharedConnection = conn.getOrCreateLiveConnection(Collections.singleton("GA-EN-AAEJ"));
        assertTrue(liveDataConnection instanceof LiveDataConnectionWrapper);
        assertTrue(redundantSecondSharedConnection instanceof LiveDataConnectionWrapper);
        assertSame(((LiveDataConnectionWrapper) liveDataConnection).getActualConnection(), ((LiveDataConnectionWrapper) redundantSecondSharedConnection).getActualConnection());
        liveDataConnection.addListener(new BulkFixReceiver() {
            @Override
            public void received(Iterable<Fix> fixes) {
                Util.addAll(fixes, allFixesReceived);
            }
        });
        assertNotNull(liveDataConnection);
        assertTrue(liveDataConnection.waitForConnection(5000l), "Connection handshake not successful within 5s");
        liveDataConnection.stop(); // this won't stop the actual connection because it's still shared with redundantSecondSharedConnection
        redundantSecondSharedConnection.stop(); // now this should stop the actual connection
        LiveDataConnection secondRedundantSecondSharedConnection = conn.getOrCreateLiveConnection(Collections.singleton("GA-EN-AAEJ"));
        assertTrue(secondRedundantSecondSharedConnection instanceof LiveDataConnectionWrapper);
        // a new actual connection is expected to have been created
        assertNotSame(((LiveDataConnectionWrapper) liveDataConnection).getActualConnection(), ((LiveDataConnectionWrapper) secondRedundantSecondSharedConnection).getActualConnection());
    }
}
