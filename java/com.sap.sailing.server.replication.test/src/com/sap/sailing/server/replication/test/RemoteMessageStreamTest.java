package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sap.sse.replication.RabbitMQConnectionFactoryHelper;
import com.sap.sse.replication.impl.RabbitInputStreamProvider;
import com.sap.sse.replication.impl.RabbitOutputStream;

/**
 * A number of tests for the {@link OutputStream} / {@link InputStream} implementation using an underlying RabbitMQ message queue.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RemoteMessageStreamTest {
    private Channel channel;
    private RabbitOutputStream outputStream;
    private RabbitInputStreamProvider inputStream;
    
    @BeforeEach
    public void setUp() throws IOException, TimeoutException {
        ConnectionFactory factory = RabbitMQConnectionFactoryHelper.getConnectionFactory();
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
    }
    
    @Test
    public void testBasicConnectivityInOnePacket() throws IOException {
        setupStreams(/* messageSizeInBytes */ 8192, /* syncAfterTimeout */ false);
        final String message = "Hello World!";
        outputStream.write(message.getBytes());
        outputStream.close();
        byte[] buf = new byte[100];
        int numberOfReadBytes = inputStream.getInputStream().read(buf);
        assertEquals(message.getBytes().length, numberOfReadBytes);
        byte[] trimmedMessage = new byte[numberOfReadBytes];
        System.arraycopy(buf, 0, trimmedMessage, 0, numberOfReadBytes);
        String messageAsString = new String(trimmedMessage);
        assertEquals(message, messageAsString);
        assertEquals(-1, inputStream.getInputStream().read());
    }

    @Test
    public void testBasicConnectivityInMultiplePackets() throws IOException {
        setupStreams(/* messageSizeInBytes */ 5, /* syncAfterTimeout */ false);
        final String message = "Hello World!";
        outputStream.write(message.getBytes());
        outputStream.close();
        byte[] buf = new byte[100];
        int numberOfReadBytes = inputStream.getInputStream().read(buf);
        assertEquals(message.getBytes().length, numberOfReadBytes);
        byte[] trimmedMessage = new byte[numberOfReadBytes];
        System.arraycopy(buf, 0, trimmedMessage, 0, numberOfReadBytes);
        String messageAsString = new String(trimmedMessage);
        assertEquals(message, messageAsString);
        assertEquals(-1, inputStream.getInputStream().read());
    }

    @Test
    public void testDataIsSentAfterTimeout() throws IOException, InterruptedException {
        setupStreams(/* messageSizeInBytes */ 1000, /* syncAfterTimeout */ true);
        final String message = "Hello World!";
        outputStream.write(message.getBytes());
        // wait until timeout should cause bytes to be sent, plus a bit to give Rabbit some time
        Thread.sleep(RabbitOutputStream.DURATION_AFTER_TO_SYNC_DATA_TO_CHANNEL_AS_MILLIS.plus(1000).asMillis());
        byte[] buf = new byte[100];
        int numberOfReadBytes = inputStream.getInputStream().read(buf);
        assertEquals(message.getBytes().length, numberOfReadBytes);
        byte[] trimmedMessage = new byte[numberOfReadBytes];
        System.arraycopy(buf, 0, trimmedMessage, 0, numberOfReadBytes);
        String messageAsString = new String(trimmedMessage);
        assertEquals(message, messageAsString);
        final String message2 = "Hello Back!";
        outputStream.write(message2.getBytes());
        outputStream.close();
        int numberOfReadBytes2 = inputStream.getInputStream().read(buf);
        assertEquals(message2.getBytes().length, numberOfReadBytes2);
        byte[] trimmedMessage2 = new byte[numberOfReadBytes2];
        System.arraycopy(buf, 0, trimmedMessage2, 0, numberOfReadBytes2);
        String messageAsString2 = new String(trimmedMessage2);
        assertEquals(message2, messageAsString2);
    }

    @Test
    public void testSerializedObjectsCutInPieces() throws IOException, InterruptedException, ClassNotFoundException {
        setupStreams(/* messageSizeInBytes */ 10, /* syncAfterTimeout */ false);
        List<String> l = new ArrayList<>();
        final String message = "Hello World!";
        l.add(message);
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(l);
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(inputStream.getInputStream());
        assertEquals(l, ois.readObject());
    }

    @Test
    public void testEscapedTerminatorTransmission() throws IOException {
        setupStreams(/* messageSizeInBytes */ 20, /* syncAfterTimeout */ false);
        byte[] terminator = new byte[] { 2, 6, 0, 4, 1, 9, 8, 2, 0, 1, 4, 2 };
        outputStream.write(terminator);
        outputStream.close();
        byte[] buf = new byte[100];
        int numberOfReadBytes = inputStream.getInputStream().read(buf);
        assertEquals(terminator.length, numberOfReadBytes);
        byte[] trimmedMessage = new byte[numberOfReadBytes];
        System.arraycopy(buf, 0, trimmedMessage, 0, numberOfReadBytes);
        assertTrue(Arrays.equals(terminator, trimmedMessage));
        assertEquals(-1, inputStream.getInputStream().read());
    }

    
    private void setupStreams(int messageSizeInBytes, boolean syncAfterTimeout) throws IOException {
        outputStream = new RabbitOutputStream(messageSizeInBytes, channel, UUID.randomUUID().toString(), syncAfterTimeout);
        inputStream = new RabbitInputStreamProvider(channel, outputStream.getQueueName());
    }
}
