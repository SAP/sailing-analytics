package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.sap.sse.replication.RabbitMQConnectionFactoryHelper;

public class SimpleRabbitMQTest {
    private final static String QUEUE_NAME = "hello";
    
    private ConcurrentMap<Consumer, String> received;
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    
    @BeforeEach
    public void setUp() throws IOException, TimeoutException {
        received = new ConcurrentHashMap<Consumer, String>();
        factory = RabbitMQConnectionFactoryHelper.getConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
    }
    
    @AfterEach
    public void tearDown() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
    
    @Test
    public void testSendReceiveHelloWorld() throws IOException, InterruptedException, TimeoutException {
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        final Consumer consumer = new QueueConsumer(QUEUE_NAME);
        new Thread(consumer).start();
        Thread.sleep(100); // make sure both consumers are waiting
        String message = "Hello World!";
        channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
        consumer.waitUntilReceived(1000);
        assertEquals(message, received.get(consumer));
    }
    
    @Test
    public void testSendReceiveHelloWorldToTwoNodes() throws IOException, InterruptedException, TimeoutException {
        final String EXCHANGE_NAME = "updates";
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        final Consumer consumer1 = new ExchangeConsumer(EXCHANGE_NAME);
        final Consumer consumer2 = new ExchangeConsumer(EXCHANGE_NAME);
        new Thread(consumer1).start();
        new Thread(consumer2).start();
        Thread.sleep(100); // make sure both consumers are waiting
        String message = "Hello World!";
        channel.basicPublish(EXCHANGE_NAME, /* queue name */ "", null, message.getBytes());
        Thread.sleep(1000);
        consumer1.waitUntilReceived(1000);
        consumer2.waitUntilReceived(1000);
        assertEquals(message, received.get(consumer1));
        assertEquals(message, received.get(consumer2));
    }
    
    private abstract class Consumer implements Runnable {
        private final Connection connection;
        private final Channel channel;
        private final QueueingConsumer consumer;
        private boolean receivedFinished;
        
        protected Consumer() throws IOException, TimeoutException {
            ConnectionFactory factory = RabbitMQConnectionFactoryHelper.getConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            consumer = new QueueingConsumer(channel);
        }
        
        public synchronized void waitUntilReceived(long timeoutInMillis) throws InterruptedException {
            while (!receivedFinished) {
                wait(timeoutInMillis);
            }
        }
        
        protected abstract String getQueueName();
        
        protected Channel getChannel() {
            return channel;
        }
        
        @Override
        public void run() {
            try {
                channel.basicConsume(getQueueName(), /* auto-ack */ true, consumer);
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                final String s = new String(delivery.getBody());
                received.put(this, s);
                synchronized (this) {
                    receivedFinished = true;
                    notifyAll();
                }
                System.out.println("received "+s+" in "+this);
                channel.close();
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ShutdownSignalException e) {
                e.printStackTrace();
            } catch (ConsumerCancelledException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class QueueConsumer extends Consumer {
        private final String queueName;

        public QueueConsumer(String queueName) throws IOException, TimeoutException {
            super();
            this.queueName = queueName;
            getChannel().queueDeclare(queueName, false, false, false, null);
        }

        @Override
        protected String getQueueName() {
            return queueName;
        }
    }
    
    private class ExchangeConsumer extends Consumer {
        private final String queueName;

        public ExchangeConsumer(String exchangeName) throws IOException, TimeoutException {
            super();
            this.queueName = getChannel().queueDeclare().getQueue();
            getChannel().exchangeDeclare(exchangeName, "fanout");
            getChannel().queueBind(getQueueName(), exchangeName, "");
        }

        @Override
        protected String getQueueName() {
            return queueName;
        }
    }
}
