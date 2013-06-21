package com.sap.sailing.server.replication.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.server.replication.ReplicationMasterDescriptor;
import com.sap.sailing.server.replication.impl.ReplicationMasterDescriptorImpl;

public class ConnectionResetAndReconnectTest extends AbstractServerReplicationTest {
    static final Logger logger = Logger.getLogger(ConnectionResetAndReconnectTest.class.getName());
    
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

        public MasterReplicationDescriptorMock(String hostname, String exchangeName, int servletPort, int messagingPort) {
            super(hostname, exchangeName, servletPort, messagingPort, "dummy");
        }
        
        public static MasterReplicationDescriptorMock from(ReplicationMasterDescriptor obj) {
            return new MasterReplicationDescriptorMock(obj.getHostname(), obj.getExchangeName(), obj.getServletPort(), obj.getMessagingPort());
        }
        
        @Override
        public QueueingConsumer getConsumer() throws IOException {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost(getHostname());
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

    private MasterReplicationDescriptorMock masterReplicationDescriptor;
    private ReplicationServiceTestImpl replicaReplicationDescriptor;

    @Before
    public void setUp() throws Exception {
        try {
            Pair<ReplicationServiceTestImpl, ReplicationMasterDescriptor> result = basicSetUp(
                    true, /* master=null means create a new one */ null,
            /* replica=null means create a new one */null);
            masterReplicationDescriptor = MasterReplicationDescriptorMock.from(result.getB());
            replicaReplicationDescriptor = result.getA();
        } catch (Exception e) {
            e.printStackTrace();
            tearDown();
        }
    }

    @Test
    public void testReplicaLoosingConnectionToExchangeQueue() throws Exception {
        assertNotSame(master, replica);
        assertEquals(Util.size(master.getAllRegattas()), Util.size(replica.getAllRegattas()));
        
        /* until here both instances should have the same in-memory state.
         * now lets add an event on master and stop the messaging queue. */
        stopMessagingExchange();
        replicaReplicationDescriptor.startToReplicateFrom(masterReplicationDescriptor);
        Event event = addEventOnMaster();
        Thread.sleep(1000); // wait for master queue to get filled
        assertNull(replica.getEvent(event.getId()));
        startMessagingExchange();
        Thread.sleep(3000); // wait for connection to recover
        assertNotNull(replica.getEvent(event.getId()));
    }
    
    private Event addEventOnMaster() {
        final String eventName = "ESS Masquat";
        final String venueName = "Masquat, Oman";
        final String publicationUrl = "http://ess40.sapsailing.com";
        final boolean isPublic = false;
        List<String> regattas = new ArrayList<String>();
        regattas.add("Day1");
        regattas.add("Day2");
        return master.addEvent(eventName, venueName, publicationUrl, isPublic, UUID.randomUUID(), regattas);
    }
    
    private void stopMessagingExchange() {
        forceStopDelivery = true;
    }
    
    private void startMessagingExchange() {
        forceStopDelivery = false;
    }
}
