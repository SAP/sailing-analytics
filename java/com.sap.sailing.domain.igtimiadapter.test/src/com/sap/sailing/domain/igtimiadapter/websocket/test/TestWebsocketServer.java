package com.sap.sailing.domain.igtimiadapter.websocket.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.jupiter.api.Test;

import com.igtimi.IgtimiStream.ChannelManagement;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sse.common.Duration;
import com.sap.sse.shared.util.Wait;

public class TestWebsocketServer {
    private static final Logger logger = Logger.getLogger(TestWebsocketServer.class.getName());

    public static class ServerWebSocket extends WebSocketAdapter {
        @Override
        public void onWebSocketConnect(Session session) {
            super.onWebSocketConnect(session);
            logger.info("New connection: " + session.getRemoteAddress());
        }

        @Override
        public void onWebSocketText(String message) {
            logger.info("Received message: " + message);
            // Echo the message back to the client
            getRemote().sendStringByFuture(getEchoMessage(message));
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int len) {
            // assumes to receive a simple heartbeat and responds with the heartbeat code
            // incremented by one
            final ByteBuffer bb = ByteBuffer.wrap(payload, offset, len);
            try {
                final Msg message = Msg.parseFrom(bb);
                assertTrue(message.hasChannelManagement());
                final Msg response = message.toBuilder().setChannelManagement(message.getChannelManagement().toBuilder().setHeartbeat(message.getChannelManagement().getHeartbeat() + 1).build()).build();
                getRemote().sendBytes(ByteBuffer.wrap(response.toByteArray()));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error trying to parse or send bytes received on web socket", e);
                throw new RuntimeException(e);
            }
        }

        String getEchoMessage(String message) {
            return "Echo: " + message;
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            logger.info("Closed with statusCode: " + statusCode + " reason: " + reason);
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            cause.printStackTrace();
        }
    }
    
    public static class ClientWebSocket extends WebSocketAdapter {
        private final StringBuilder sb;
        private Msg response;
        
        /**
         * @param sb text messages received through {@link #onWebSocketText(String)} are appended to
         * this string builder
         */
        ClientWebSocket(StringBuilder sb) {
            this.sb = sb;
        }
        
        @Override
        public void onWebSocketText(String message) {
            sb.append(message);
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int len) {
            final ByteBuffer bb = ByteBuffer.wrap(payload, offset, len);
            try {
                response = Msg.parseFrom(bb);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error trying to parse or send bytes received on web socket", e);
                throw new RuntimeException(e);
            }
        }

        Msg getResponse() {
            return response;
        }
    }
    
    // WebSocketServlet implementation
    public static class MyWebSocketServlet extends WebSocketServlet {
        private static final long serialVersionUID = 8904970922157313677L;

        @Override
        public void configure(WebSocketServletFactory factory) {
            factory.register(ServerWebSocket.class); // Register the WebSocket handler class
        }
    }
    
    @Test
    public void testWebsocketServer() throws Exception {
        // Create Jetty server on a randomly allocated port
        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        // Create a context handler for WebSocket connections
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setConnectors(new Connector[] { connector });
        final String PATH = "/websocket";
        // Add WebSocket handler to the context
        context.addServlet(new ServletHolder(new MyWebSocketServlet()), PATH+"/*");
        server.setHandler(context);
        // Start the server
        server.start();
        final URI uri = server.getURI();
        final URI wsUri = new URI("ws", uri.getAuthority(), PATH, uri.getQuery(), uri.getFragment());
        final int port = uri.getPort();
        logger.info("WebSocket Server listening on port "+port);
        final WebSocketClient client = new WebSocketClient();
        client.start();
        final StringBuilder echo = new StringBuilder();
        final ClientWebSocket currentSocket = new ClientWebSocket(echo);
        client.connect(currentSocket, wsUri, new ClientUpgradeRequest());
        try {
            Wait.wait(()->currentSocket.getRemote() != null, Optional.of(Duration.ONE_SECOND.times(5)), Duration.ONE_SECOND.divide(3));
            final String text = "Hello world";
            currentSocket.getRemote().sendString(text);
            currentSocket.getRemote().sendBytes(ByteBuffer.wrap(Msg.newBuilder().setChannelManagement(ChannelManagement.newBuilder().setHeartbeat(42).build()).build().toByteArray()));
            final String expectedEcho = new ServerWebSocket().getEchoMessage(text);
            Wait.wait(()->echo.length() > 0, Optional.of(Duration.ONE_SECOND), Duration.ONE_SECOND.divide(3));
            assertEquals(expectedEcho, echo.toString());
            Wait.wait(()->currentSocket.getResponse() != null, Optional.of(Duration.ONE_SECOND), Duration.ONE_SECOND.divide(3));
            assertEquals(43, currentSocket.getResponse().getChannelManagement().getHeartbeat());
        } finally {
            client.stop();
            client.destroy();
            server.stop();
            server.join();
        }
    }
}
