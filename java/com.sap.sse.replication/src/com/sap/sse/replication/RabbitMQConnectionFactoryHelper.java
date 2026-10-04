package com.sap.sse.replication;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConnectionFactoryHelper {
    /**
     * Creates a RabbitMQ {@link ConnectionFactory} that has a connection timeout of 30s, a network recovery interval of
     * 10s, no heartbeat interval (0), and that uses automatic recovery and topology recovery.
     */
    public static ConnectionFactory getConnectionFactory() {
        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setConnectionTimeout(30000);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);
        connectionFactory.setNetworkRecoveryInterval(10000);
        connectionFactory.setRequestedHeartbeat(24*3600); // no heartbeat in 24h allows for dropping connection
        return connectionFactory;
    }
}
