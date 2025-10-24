package com.sap.sse.replication.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rabbitmq.client.Channel;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.shared.util.impl.ApproximateTime;

/**
 * Output stream that can split its contents into messages for RabbitMQ. This stream creates a RabbitMQ queue and sends
 * a RabbitMQ message for a configurable number of bytes written to that stream or after a timeout of
 * {@link #DURATION_AFTER_TO_SYNC_DATA_TO_CHANNEL_AS_MILLIS } and send it to the queue whose name was provided to the
 * constructor.
 * <p>
 * 
 * Usage: call the constructor with a channel to a RabbitMQ connection. Then, extract the queue name assigned (see
 * {@link #getQueueName()} and get it to the client. On the client, use {@link RabbitInputStreamProvider}, passing the queue name
 * to the constructor. The client can then obtain an {@link InputStream} from the {@link RabbitInputStreamProvider} by calling
 * {@link RabbitInputStreamProvider#getInputStream()} from which the content can be read. The client will receive EOF at the end
 * of the stream.
 * 
 * @author Simon Marcel Pamies
 * @author Axel Uhl (d043530)
 */
public class RabbitOutputStream extends OutputStream {
    private static final Logger logger = Logger.getLogger(RabbitOutputStream.class.getName());

    static final byte TERMINATION_COMMAND[] = new byte[] { 2, 6, 0, 4, 1, 9, 8, 2, 0, 1, 4, 2 };

    public static final Duration DURATION_AFTER_TO_SYNC_DATA_TO_CHANNEL_AS_MILLIS = new MillisecondsDurationImpl(5000);
    private static final long DURATION_TO_PAUSE_SYNCER_THREAD_AS_MILLIS = 1000;

    private final Channel channel;
    private final String queueName;

    private boolean closed;

    private TimePoint timeLastDataHasBeenReceived;
    private int count;
    private final byte streamBuffer[];

    public RabbitOutputStream(int messageSizeInBytes, Channel channel, String queueName, boolean syncAfterTimeout) throws IOException {
        super();
        this.streamBuffer = new byte[messageSizeInBytes];
        this.count = 0;
        this.channel = channel;
        this.queueName = channel.queueDeclare(queueName, /* durable */ false, /* exclusive */ false, /* autoDelete */ false,
                /* args */ null).getQueue();
        this.closed = false;

        if (syncAfterTimeout) {
            new Thread("Timeout syncer for "+getClass().getSimpleName()+" on channel "+channel) {
                @Override
                public void run() {
                    while (!closed) {
                        synchronized (RabbitOutputStream.this) {
                            if (timeLastDataHasBeenReceived != null) {
                                TimePoint approximateNow = ApproximateTime.approximateNow();
                                // FIXME the timing should better be managed by a Timer instance with delays based on last send and wake-up / test time point
                                if (timeLastDataHasBeenReceived.until(approximateNow).compareTo(DURATION_AFTER_TO_SYNC_DATA_TO_CHANNEL_AS_MILLIS) > 0) {
                                    try {
                                        sendBuffer();
                                        timeLastDataHasBeenReceived = ApproximateTime.approximateNow(); // reset time to avoid unnecessary write attempt
                                    } catch (IOException e) {
                                        logger.log(Level.INFO, "Exception trying to send message. Aborting.", e);
                                        break;
                                    }
                                }
                            }
                        }
                        try {
                            Thread.sleep(DURATION_TO_PAUSE_SYNCER_THREAD_AS_MILLIS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
    }

    public String getQueueName() {
        return queueName;
    }
    
    /**
     * The method writes a byte to the buffer. If this fills up the buffer to its limit, the buffer is {@link #sendBuffer() sent}
     * as a message and cleared again. The {@link #timeLastDataHasBeenReceived} timestamp is updated to the current time.
     */
    @Override
    public synchronized void write(int b) throws IOException {
        assert count < streamBuffer.length;
        if (this.closed) {
            throw new IOException("This stream has been closed by an earlier operation.");
        }
        streamBuffer[count++] = (byte) b;
        if (count == streamBuffer.length) {
            sendBuffer();
        }
        timeLastDataHasBeenReceived = ApproximateTime.approximateNow();
    }

    @Override
    public void close() throws IOException {
        try {
            finish();
        } finally {
            // make sure to always set this stream to closed
            this.closed = true;
        }
    }

    @Override
    public void flush() throws IOException {
        sendBuffer();
    }

    private void finish() throws IOException {
        sendBuffer();
        this.channel.basicPublish(/* exchangeName */ "", /* routingKey */ queueName, /* properties */null, TERMINATION_COMMAND);
    }

    /**
     * Sends the buffer contents from index 0 to index {@link #count}-1 inclusive. {@link #count} is reset to 0 when done.
     * The method is synchronized as it needs exclusive and atomic access to {@link #count} and {@link #streamBuffer}.
     */
    private synchronized void sendBuffer() throws IOException {
        if (count > 0) {
            if (this.channel != null && this.channel.isOpen()) {
                final byte[] message;
                // check for the unlikely case that a client has coincidentally submitted the TERMINATION_COMMAND; we escape by
                // appending a random byte which makes the length differ; the input stream provider will skip one byte after receiving
                // the TERMINATION_COMMAND at the beginning of a message whose length is greater than that of the TERMINATION_COMMAND.
                if (startsWithTerminationCommand(streamBuffer, count)) {
                    message = new byte[count+1];
                } else {
                    message = new byte[count];
                }
                System.arraycopy(streamBuffer, 0, message, 0, count);
                this.channel.basicPublish(/* empty exchange name means the default exchange with direct routing;
                                             all queues by default bind to this exchange, using the queue name
                                             as the routing key.
                                             See also https://www.rabbitmq.com/tutorials/amqp-concepts#exchange-default
                                           */ "", /* routingKey */ queueName, /* properties */ null, message);
                count = 0;
            } else {
                this.closed = true;
                throw new IOException("AMPQ Channel seems to be closed!");
            }
        }
    }

    static boolean startsWithTerminationCommand(byte[] bytes, int count) {
        boolean result = true;
        if (count < TERMINATION_COMMAND.length) {
            result = false;
        } else {
            for (int i = 0; i < TERMINATION_COMMAND.length && result; i++) {
                result = bytes[i] == TERMINATION_COMMAND[i];
            }
        }
        return result;
    }
}
