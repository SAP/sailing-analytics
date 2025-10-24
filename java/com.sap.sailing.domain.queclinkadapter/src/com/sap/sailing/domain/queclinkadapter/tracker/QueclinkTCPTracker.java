package com.sap.sailing.domain.queclinkadapter.tracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.queclinkadapter.ByteStreamToMessageStreamConverter;
import com.sap.sailing.domain.queclinkadapter.FRIReport;
import com.sap.sailing.domain.queclinkadapter.Message;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogtracking.SmartphoneImeiIdentifier;

/**
 * Receives Queclink GL300 messages over TCP socket connections, parses them and sends position fixes such as from a
 * {@link FRIReport} message the results to a {@link SensorFixStore} so that they will be stored and forwarded to race
 * trackers registered on that store for fixes coming from matching devices. Once messages have been received from a
 * device and the socket connection with that device is still open, clients can use
 * {@link #sendToDevice(SmartphoneImeiIdentifier, Message)} to send messages to that device, for example to change the
 * device's configuration.
 * <p>
 * 
 * Listens on a given port for incoming TCP connections (use {@code 0} to let the networking sub-system pick a free
 * one), using an asynchronous server socket. When a connection is established, an asynchronous socket channel is used
 * for reading data from that socket until the socket is closed. The data read is assumed to consist of Queclink GL300
 * messages. When the device's IMEI is found in any of the messages received, the asynchronous socket channel through
 * which it was received is associated to that IMEI. This way, when clients would like to send messages to the device,
 * they can do so as long as the socket is still connected.
 * <p>
 * 
 * Calling the {@link #stop} method will close all existing socket connections and will stop listening for new incoming
 * connections. After that, this tracker cannot be used anymore.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class QueclinkTCPTracker implements MessageToDeviceSender {
    private static final Logger logger = Logger.getLogger(QueclinkTCPTracker.class.getName());

    private final AsynchronousServerSocketChannel serverSocketChannel;
    private final ConcurrentMap<String, AsynchronousSocketChannel> socketChannelsByImei;
    private final SensorFixStore sensorFixStore;
    private final Charset charset;
    private volatile boolean stopped;

    /**
     * Initializes a tracker using the {@code ISO-8859-1} charset as a default (the specification talks about only
     * printable ASCII characters being used; ISO-8859-1 seems to come closest here).
     * 
     * @param port when {@code 0}, any available port will be chosen; it can be queries using the {@link #getPort} method.
     */
    public QueclinkTCPTracker(int port, SensorFixStore sensorFixStore) throws IOException {
        this(port, Charset.forName("ISO-8859-1"), sensorFixStore);
    }
    
    /**
     * @param port when {@code 0}, any available port will be chosen; it can be queries using the {@link #getPort} method.
     */
    public QueclinkTCPTracker(int port, Charset charset, SensorFixStore sensorFixStore) throws IOException {
        this.sensorFixStore = sensorFixStore;
        this.charset = charset;
        socketChannelsByImei = new ConcurrentHashMap<>();
        serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        handleAccept(getPort());
    }
    
    public int getPort() throws IOException {
        return ((InetSocketAddress) serverSocketChannel.getLocalAddress()).getPort();
    }
    
    /**
     * Sends the {@code message} to the device identified by {@code deviceIdentifier}; this only works if messages
     * have already been received from that device by this tracker, and the socket connection is still open. Otherwise,
     * an {@link IllegalStateException} will be thrown.
     */
    @Override
    public void sendToDevice(SmartphoneImeiIdentifier deviceIdentifier, Message message) {
        final String imei = deviceIdentifier.getImei();
        final AsynchronousSocketChannel channel = socketChannelsByImei.get(imei);
        if (channel == null) {
            throw new IllegalStateException("No connection exists to the device with IMEI "+imei+"; cannot send message "+message.getMessageString());
        }
        channel.write(charset.encode(CharBuffer.wrap(message.getMessageString()))); // ignore the Future object returned
    }
    
    private void handleConnection(final AsynchronousSocketChannel socketChannel, final int port) {
        logger.info(()->{
            try {
                return "Received connection on port "+getPort()+" from remote address "+socketChannel.getRemoteAddress();
            } catch (IOException e) {
                return "Exception trying to compute log message: "+e.getMessage();
            }
        });
        final MessageVisitor<Void> storeFixVisitor = new MessageVisitorWithSensorFixStore<AsynchronousSocketChannel>(
                sensorFixStore, this, socketChannelsByImei, socketChannel);
        final ByteBuffer buf = ByteBuffer.allocateDirect(8192);
        final ByteStreamToMessageStreamConverter converter = ByteStreamToMessageStreamConverter.create();
        final CompletionHandler<Integer, Integer> connectionHandler = new CompletionHandler<Integer, Integer>() {
            @Override
            public void completed(Integer result, Integer port) {
                if (result != -1) {
                    buf.flip();
                    try {
                        converter.convert(charset.decode(buf)).forEach(message->{
                            logger.fine(()->"Processing message "+message);
                            message.accept(storeFixVisitor);
                        });
                    } catch (ParseException e) {
                        logger.log(Level.SEVERE, "Error trying to convert messages receive through TCP on port "+port, e);
                    }
                    buf.clear();
                    socketChannel.read(buf, /* attachment */ null, this);
                } else {
                    // EOF reached; close the channel
                    try {
                        socketChannel.close();
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error trying to close TCP connection on port "+port, e);
                    }
                }
            }

            @Override
            public void failed(Throwable exc, Integer port) {
                logger.log(Level.SEVERE, "Error trying to read from TCP connection on port "+port, exc);
            }
        };
        socketChannel.read(buf, /* attachment */ port, connectionHandler);
    }

    private void handleAccept(int port) {
        serverSocketChannel.accept(port, new CompletionHandler<AsynchronousSocketChannel, Integer>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Integer port) {
                handleAccept(port); // try to accept the next connection request asynchronously
                handleConnection(result, port);
            }

            @Override
            public void failed(Throwable exc, Integer port) {
                if (!stopped) {
                    logger.log(Level.SEVERE, "Error trying to accept TCP connection on port "+port, exc);
                }
            }
        });
    }

    /**
     * Stops this tracker, closes all socket connections and stops listening on the TCP port identified by {@link #getPort()}.
     */
    public void stop() throws IOException {
        stopped = true;
        for (final AsynchronousSocketChannel socketChannel : socketChannelsByImei.values()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error trying to close socket channel; continuing with other channels if any", e);
            }
        }
        serverSocketChannel.close();
    }
}
