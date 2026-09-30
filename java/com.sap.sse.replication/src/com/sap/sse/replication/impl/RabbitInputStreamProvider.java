package com.sap.sse.replication.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import com.sap.sse.common.impl.NamedImpl;

/**
 * Input stream that reads messages from a RabbitMQ queue filled by a {@link RabbitOutputStream} and lets a client read
 * this stream from {@link #getInputStream()}.
 * 
 * @author Simon Marcel Pamies
 * @author Axel Uhl
 */
public class RabbitInputStreamProvider extends NamedImpl {
    private static final Logger logger = Logger.getLogger(RabbitInputStreamProvider.class.getName());
    private static final long serialVersionUID = 1342935135386887494L;

    private final PipedInputStream clientReadsFromThis;
    private final PipedOutputStream messagesAreWrittenToThis;
    
    public RabbitInputStreamProvider(Channel channel, String queueName) throws IOException {
        this(channel, queueName, /* name */ UUID.randomUUID().toString());
    }
    
    public RabbitInputStreamProvider(Channel channel, String queueName, String name) throws IOException {
        super(name);
        assert name != null;
        messagesAreWrittenToThis = new PipedOutputStream();
        clientReadsFromThis = new PipedInputStream(messagesAreWrittenToThis);
        final QueueingConsumer messageConsumer = new QueueingConsumer(channel);
        channel.basicQos(1); // with "manual" acknowledgement, process one message at a time, avoiding overloading inbound socket
        channel.basicConsume(queueName, /* auto-ack */ false, messageConsumer);
        new Thread(getClass().getSimpleName()) {
            @Override
            public void run() {
                while (true) {
                    try {
                        final Delivery delivery = messageConsumer.nextDelivery();
                        byte[] bytesFromMessage = delivery.getBody();
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), /* multiple */ false);
                        if (RabbitOutputStream.startsWithTerminationCommand(bytesFromMessage, bytesFromMessage.length)) {
                            if (bytesFromMessage.length == RabbitOutputStream.TERMINATION_COMMAND.length) {
                                // received exactly TERMINATION_COMMAND
                                messagesAreWrittenToThis.close();
                                break;
                            } else {
                                // more to come; skip one byte which is the "escape" symbol ensuring that the message
                                // sizes don't match
                                byte[] newBytesFromMessage = new byte[bytesFromMessage.length - 1];
                                System.arraycopy(bytesFromMessage, 0, newBytesFromMessage, 0,
                                        RabbitOutputStream.TERMINATION_COMMAND.length);
                                System.arraycopy(bytesFromMessage, RabbitOutputStream.TERMINATION_COMMAND.length + 1,
                                        newBytesFromMessage, RabbitOutputStream.TERMINATION_COMMAND.length,
                                        bytesFromMessage.length - RabbitOutputStream.TERMINATION_COMMAND.length - 1);
                                bytesFromMessage = newBytesFromMessage;
                            }
                        }
                        messagesAreWrittenToThis.write(bytesFromMessage);
                    } catch (ShutdownSignalException | ConsumerCancelledException e) {
                        logger.log(Level.INFO, "Problem with message queue "+getName(), e);
                        break;
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Reading of next message in stream "+getName()+" Interrupted; continuing", e);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public InputStream getInputStream() {
        return clientReadsFromThis;
    }
}