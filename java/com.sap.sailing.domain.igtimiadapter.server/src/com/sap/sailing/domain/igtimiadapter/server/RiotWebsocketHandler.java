package com.sap.sailing.domain.igtimiadapter.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.Future;

import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.security.shared.impl.User;

/**
 * Exposes functionality of a Riot web socket connection's server side, in particular
 * the user that has been authenticated during establishing the web socket connection,
 * and the essential methods to send text and binary messages to the client.<p>
 * 
 * The {@link RiotServer} can use this to implement the heartbeat and forward protobuf
 * messages to the client.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface RiotWebsocketHandler {
    /**
     * Never {@code null}; may be the "<all>" user representing anonymous users
     */
    User getAuthenticatedUser();
    
    Set<String> getDeviceSerialNumbers();
    
    void sendBytes(ByteBuffer data) throws IOException;

    Future<Void> sendBytesByFuture(ByteBuffer data);

    void sendString(String text) throws IOException;

    Future<Void> sendStringByFuture(String text);

    void flush() throws IOException;

    void close(int statusCode, String reason);
}
