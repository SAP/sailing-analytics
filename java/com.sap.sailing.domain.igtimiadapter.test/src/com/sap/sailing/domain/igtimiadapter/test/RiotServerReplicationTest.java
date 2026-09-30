package com.sap.sailing.domain.igtimiadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.igtimi.IgtimiDevice.DeviceManagement;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.igtimiadapter.server.Activator;
import com.sap.sailing.domain.igtimiadapter.server.RiotWebsocketHandler;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.User;

public class RiotServerReplicationTest extends AbstractRiotServerReplicationTest {
    @BeforeEach
    public void clear() {
        master.clear();
        replica.clear();
    }
    
    @BeforeAll
    static public void setTestSecurityServiceOnActivator() {
        final SecurityService mockedSecurityService = mock(SecurityService.class);
        Activator.getInstance().setSecurityService(mockedSecurityService);
    }
    
    @Test
    public void simpleDeviceCreationTest() throws IllegalAccessException, InterruptedException {
        final String deviceSerialNumber = "12345";
        assertNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
        master.createDevice(deviceSerialNumber);
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(1000); // wait for replica to receive and apply after replicator has emptied its queue
        assertNotNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
    }
    
    @Test
    public void triggerDeviceCreationWithMessagesTest() throws IllegalAccessException, InterruptedException, InvalidProtocolBufferException {
        final String deviceSerialNumber = "12345";
        master.createDataAccessWindow(deviceSerialNumber, TimePoint.BeginningOfTime, TimePoint.EndOfTime);
        replicaReplicator.waitUntilQueueIsEmpty();
        final ByteBuffer[] bytesSent = new ByteBuffer[1];
        replica.addWebSocketClient(new RiotWebsocketHandler() {
            @Override
            public Set<String> getDeviceSerialNumbers() {
                return Collections.singleton(deviceSerialNumber);
            }
            
            @Override
            public User getAuthenticatedUser() {
                final User user = mock(User.class);
                when(user.getPermissions()).thenReturn(Collections.singleton(new WildcardPermission("*")));
                return user;
            }
            
            @Override
            public Future<Void> sendBytesByFuture(ByteBuffer data) {
                synchronized (bytesSent) {
                    bytesSent[0] = data;
                }
                return null;
            }

            @Override
            public void sendBytes(ByteBuffer data) throws IOException {
                synchronized (bytesSent) {
                    bytesSent[0] = data;
                }
            }

            @Override public Future<Void> sendStringByFuture(String text) { return null; }
            @Override public void sendString(String text) throws IOException {}
            @Override public void flush() throws IOException {}
            @Override public void close(int statusCode, String reason) {}
        });
        final Msg message = Msg.newBuilder().setDeviceManagement(DeviceManagement.newBuilder().setSerialNumber(deviceSerialNumber)).build();
        assertNull(master.getDeviceBySerialNumber(deviceSerialNumber));
        assertNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
        replica.internalNotifyListeners(message, deviceSerialNumber); // assuming that this will lead to device creation on both ends, and buffering
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000); // wait for replica to receive and apply after replicator has emptied its queue
        assertNotNull(master.getDeviceBySerialNumber(deviceSerialNumber));
        assertNotNull(replica.getDeviceBySerialNumber(deviceSerialNumber));
        assertEquals(master.getDeviceBySerialNumber(deviceSerialNumber).getId(), replica.getDeviceBySerialNumber(deviceSerialNumber).getId());
        synchronized (bytesSent) {
            assertNotNull(bytesSent[0]);
            final Msg messageReceived = Msg.parseFrom(bytesSent[0]);
            assertEquals(deviceSerialNumber, messageReceived.getDeviceManagement().getSerialNumber());
        }
    }
}
