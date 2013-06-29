package com.sap.sailing.server.replication.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.sap.sailing.server.replication.ReplicationMasterDescriptor;
import com.sap.sailing.util.BuildVersion;

public class ReplicationMasterDescriptorImpl implements ReplicationMasterDescriptor {
    private static final String REPLICATION_SERVLET = "/replication/replication";
    private final String hostname;
    private final String exchangeName;
    private final int servletPort;
    private final int messagingPort;
    private final String queueName;
    
    private QueueingConsumer consumer;
    
    /**
     * @param messagingPort 0 means use default port
     */
    public ReplicationMasterDescriptorImpl(String hostname, String exchangeName, int servletPort, int messagingPort, String queueName) {
        this.hostname = hostname;
        this.servletPort = servletPort;
        this.messagingPort = messagingPort;
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.consumer = null;
    }

    @Override
    public URL getReplicationRegistrationRequestURL(UUID uuid, String additional) throws MalformedURLException, UnsupportedEncodingException {
        return new URL("http", hostname, servletPort, REPLICATION_SERVLET + "?" + ReplicationServlet.ACTION + "="
                + ReplicationServlet.Action.REGISTER.name()
                + "&" + ReplicationServlet.SERVER_UUID + "=" + uuid.toString()
                + "&" + ReplicationServlet.ADDITIONAL_INFORMATION + "=" + java.net.URLEncoder.encode(BuildVersion.getBuildVersion(), "UTF-8"));
    }

    @Override
    public URL getReplicationDeRegistrationRequestURL(UUID uuid) throws MalformedURLException {
        return new URL("http", hostname, servletPort, REPLICATION_SERVLET + "?" + ReplicationServlet.ACTION + "="
                + ReplicationServlet.Action.DEREGISTER.name()
                + "&" + ReplicationServlet.SERVER_UUID + "=" + uuid.toString());
    }
    
    @Override
    public URL getInitialLoadURL() throws MalformedURLException {
        return new URL("http", hostname, servletPort, REPLICATION_SERVLET + "?" + ReplicationServlet.ACTION + "="
                + ReplicationServlet.Action.INITIAL_LOAD.name());
    }

    @Override
    public synchronized QueueingConsumer getConsumer() throws IOException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(getHostname());
        int port = getMessagingPort();
        if (port != 0) {
            connectionFactory.setPort(port);
        }
        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();
        
        /*
         * Connect a queue to the given exchange that has already
         * been created by the master server.
         */
        channel.exchangeDeclare(exchangeName, "fanout");
        QueueingConsumer consumer = new QueueingConsumer(channel);
        
        /*
         * The x-message-ttl argument to queue.declare controls for how long a message published to a queue can live before 
         * it is discarded. A message that has been in the queue for longer than the configured TTL is said to be dead. 
         * Note that a message routed to multiple queues can die at different times, or not at all, 
         * in each queue in which it resides. The death of a message in one queue has no impact on the life of the 
         * same message in other queues.
         */
        final Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-message-ttl", (60*30)*1000); // messages will live half an hour in queue before being deleted
        
        /*
         * The x-expires argument to queue.declare controls for how long a queue can be unused before it is automatically 
         * deleted. Unused means the queue has no consumers, the queue has not been redeclared, and basic.get has not 
         * been invoked for a duration of at least the expiration period.
         */
        args.put("x-expires", (60*60)*1000); // queue will live one hour before being deleted
        
        /*
         * The maximum length of a queue can be limited to a set number of messages by supplying the x-max-length queue 
         * declaration argument with a non-negative integer value. Queue length is a measure that takes into account 
         * ready messages, ignoring unacknowledged messages and message size. Messages will be dropped or dead-lettered 
         * from the front of the queue to make room for new messages once the limit is reached.
         */
        args.put("x-max-length", 3000000);
        
        // a server-named non-exclusive, non-durable queue
        // this queue will survive a connection drop (autodelete=false) and
        // will also support being reconnected (exclusive=false). it will
        // not survive a rabbitmq server restart (durable=false).
        String queueName = channel.queueDeclare(this.queueName, 
                /*durable*/ false, /*exclusive*/ false, /*auto-delete*/ false, args).getQueue();
        
        // from now on we get all new messages that the exchange is getting from producer
        channel.queueBind(queueName, exchangeName, "");
        channel.basicConsume(queueName, /* auto-ack */ true, consumer);
        this.consumer = consumer;
        return consumer;
    }
    
    @Override
    public synchronized void stopConnection() {
        try {
            if (consumer != null) {
                // make sure to remove queue in order to avoid any exchanges filling it with messages
                consumer.getChannel().queueUnbind(queueName, exchangeName, "");
                consumer.getChannel().queueDelete(queueName);
                consumer.getChannel().getConnection().close(1);
            }
        } catch (Exception ex) {
            // ignore any exception during abort. close can yield a broad
            // number of exceptions that we don't want to know or to log.
        }
    }

    /**
     * @return 0 means use default port
     */
    @Override
    public int getMessagingPort() {
        return messagingPort;
    }

    @Override
    public int getServletPort() {
        return servletPort;
    }

    @Override
    public String getHostname() {
        return hostname;
    }
    
    @Override
    public String getExchangeName() {
        return exchangeName;
    }
    
    public String toString() {
        return getHostname() + ":" + getServletPort() + "/" + getMessagingPort();
    }
    
}
