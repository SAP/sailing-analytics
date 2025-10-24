package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.replication.RabbitMQConnectionFactoryHelper;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.impl.ReplicationMasterDescriptorImpl;

public class ConnectionResetAndReconnectTest extends AbstractServerReplicationTest {
    static final Logger logger = Logger.getLogger(ConnectionResetAndReconnectTest.class.getName());
    
    public ConnectionResetAndReconnectTest() {
        super(new ServerReplicationTestSetUp());
    }
    
    public static boolean forceStopDelivery = false;
    
    static class QueuingConsumerTest extends QueueingConsumer {
        public QueuingConsumerTest(Channel ch) {
            super(ch);
        }
        
        @Override
        public Delivery nextDelivery() throws ShutdownSignalException, ConsumerCancelledException, InterruptedException {
            if (forceStopDelivery) {
                throw new ShutdownSignalException(false, false, null, null);
            }
            return super.nextDelivery();
        }
    }
    
    static class MasterReplicationDescriptorMock extends ReplicationMasterDescriptorImpl {
        public MasterReplicationDescriptorMock(String messagingHost, String hostname, String exchangeName, int servletPort, int messagingPort, Iterable<Replicable<?, ?>> replicables) {
            super(messagingHost, exchangeName, messagingPort, UUID.randomUUID().toString(), hostname, servletPort, /* bearerToken */ null, replicables);
        }
        
        public static MasterReplicationDescriptorMock from(ReplicationMasterDescriptor obj) {
            return new MasterReplicationDescriptorMock(obj.getMessagingHostname(), obj.getHostname(), obj.getExchangeName(), obj.getServletPort(), obj.getMessagingPort(), obj.getReplicables());
        }
        
        @Override
        public QueueingConsumer getConsumer() throws IOException, TimeoutException {
            final ConnectionFactory connectionFactory = RabbitMQConnectionFactoryHelper.getConnectionFactory();
            connectionFactory.setHost(getMessagingHostname());
            int port = getMessagingPort();
            if (port != 0) {
                connectionFactory.setPort(port);
            }
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(getExchangeName(), "fanout");
            QueueingConsumer consumer = new QueuingConsumerTest(channel);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, getExchangeName(), "");
            channel.basicConsume(queueName, /* auto-ack */ true, consumer);
            return consumer;
        }
    }

    private static class ServerReplicationTestSetUp extends
            com.sap.sailing.server.replication.test.AbstractServerReplicationTest.ServerReplicationTestSetUp {
        @Override
        public void setUp() throws Exception {
            try {
                Pair<ReplicationServiceTestImpl<RacingEventService>, ReplicationMasterDescriptor> result = basicSetUp(
                        /* dropDB */true,
                        /* master=null means create a new one */null, /* replica=null means create a new one */null);
                masterDescriptor = MasterReplicationDescriptorMock.from(result.getB());
            } catch (Exception e) {
                e.printStackTrace();
                tearDown();
            }
        }
    }

    @Test
    public void testReplicaLoosingConnectionToExchangeQueue() throws Exception {
        assertNotSame(master, replica);
        assertEquals(Util.size(master.getAllRegattas()), Util.size(replica.getAllRegattas()));
        /* until here both instances should have the same in-memory state.
         * now lets add an event on master and stop the messaging queue. */
        stopMessagingExchange();
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        Event event = addEventOnMaster();
        Thread.sleep(1000); // wait for master queue to get filled
        assertNull(replica.getEvent(event.getId()));
        startMessagingExchange();
        Thread.sleep(10000); // wait for connection to recover
        assertNotNull(replica.getEvent(event.getId()));
    }
    
    private Event addEventOnMaster() {
        final String eventName = "ESS Masquat";
        final String venueName = "Masquat, Oman";
        final TimePoint eventStartDate = new MillisecondsTimePoint(new Date());
        final TimePoint eventEndDate = new MillisecondsTimePoint(new Date());
        final boolean isPublic = false;
        List<String> regattas = new ArrayList<String>();
        regattas.add("Day1");
        regattas.add("Day2");
        return master.addEvent(eventName, /* eventDescription */ null, eventStartDate, eventEndDate, venueName, isPublic, UUID.randomUUID());
    }
    
    private void stopMessagingExchange() {
        forceStopDelivery = true;
    }
    
    private void startMessagingExchange() {
        forceStopDelivery = false;
    }
}
