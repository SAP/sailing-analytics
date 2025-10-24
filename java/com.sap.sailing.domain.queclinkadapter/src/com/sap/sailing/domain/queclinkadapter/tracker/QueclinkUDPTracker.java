package com.sap.sailing.domain.queclinkadapter.tracker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.sap.sailing.domain.queclinkadapter.ByteStreamToMessageStreamConverter;
import com.sap.sailing.domain.queclinkadapter.FRIReport;
import com.sap.sailing.domain.queclinkadapter.Message;
import com.sap.sailing.domain.queclinkadapter.MessageVisitor;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogtracking.SmartphoneImeiIdentifier;
import com.sap.sailing.udpconnector.UDPMessage;
import com.sap.sailing.udpconnector.UDPMessageListener;
import com.sap.sailing.udpconnector.UDPMessageParser;
import com.sap.sailing.udpconnector.UDPReceiver;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

/**
 * Receives Queclink GL300 messages via UCP packets, parses them and sends position fixes such as from a
 * {@link FRIReport} message to a {@link SensorFixStore} so that they will be stored and forwarded to race trackers
 * registered on that store for fixes coming from matching devices. Once messages have been received from a device and
 * the device is still using the same UDP port that it sent from, clients can use
 * {@link #sendToDevice(SmartphoneImeiIdentifier, Message)} to send messages to that device, for example to change the
 * device's configuration. This requires that the device's IP address is reachable via UDP from the server.
 * <p>
 * 
 * Listens on a given port for incoming UDP packets (use {@code 0} to let the networking sub-system pick a free one).
 * When a packet is received, the data is read and is assumed to consist of Queclink GL300 messages. When the device's
 * IMEI is found in any of the messages received, the UDP datagram socket through which it was received is associated to
 * that IMEI. This way, when clients would like to send messages to the device, they can do so as long as the socket is
 * still connected.
 * <p>
 * 
 * Calling the {@link #stop} method will close all existing socket connections and will stop listening for new incoming
 * connections. After that, this tracker cannot be used anymore.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class QueclinkUDPTracker implements UDPMessageListener<QueclinkUDPTracker.MessageAsUDPMessage>, MessageToDeviceSender {
    private static final Logger logger = Logger.getLogger(QueclinkUDPTracker.class.getName());

    private final UDPReceiver<QueclinkUDPTracker.MessageAsUDPMessage, QueclinkUDPTracker> udpReceiver;
    private final Charset charset;
    private ByteStreamToMessageStreamConverter converter;
    private final SensorFixStore sensorFixStore;
    private final ConcurrentMap<String, Pair<DatagramSocket, SocketAddress>> socketAddressesByImei;
    
    static class MessageAsUDPMessage implements UDPMessage {
        private final Iterable<Message> message;
        private final DatagramSocket datagramSocket;
        private final SocketAddress sender;
        
        public MessageAsUDPMessage(Iterable<Message> iterable, DatagramSocket datagramSocket, SocketAddress sender) {
            this.message = iterable;
            this.datagramSocket = datagramSocket;
            this.sender = sender;
        }

        Iterable<Message> getMessages() {
            return message;
        }
        
        SocketAddress getSender() {
            return sender;
        }
        
        DatagramSocket getSocket() {
            return datagramSocket;
        }
        
        @Override
        public boolean isValid() {
            return true;
        }
    }

    /**
     * @param port if {@code 0}, the system will pick a port; to find out which port this tracker listens
     * on, use {@link #getPort}. It uses the default charset {@code ISO-8859-1} for decoding the byte stream
     * into characters.
     */
    public QueclinkUDPTracker(int port, SensorFixStore sensorFixStore) throws SocketException {
        this(port, Charset.forName("ISO-8859-1"), sensorFixStore);
    }
    
    /**
     * @param port if {@code 0}, the system will pick a port; to find out which port this tracker listens
     * on, use {@link #getPort}.
     */
    public QueclinkUDPTracker(int port, Charset charset, SensorFixStore sensorFixStore) throws SocketException {
        this.charset = charset;
        this.sensorFixStore = sensorFixStore;
        socketAddressesByImei = new ConcurrentHashMap<>();
        converter = ByteStreamToMessageStreamConverter.create();
        udpReceiver = new UDPReceiver<QueclinkUDPTracker.MessageAsUDPMessage, QueclinkUDPTracker>(port) {
            @Override
            protected UDPMessageParser<QueclinkUDPTracker.MessageAsUDPMessage> getParser() {
                return new UDPMessageParser<QueclinkUDPTracker.MessageAsUDPMessage>() {
                    @Override
                    public QueclinkUDPTracker.MessageAsUDPMessage parse(DatagramPacket p) throws IOException {
                        logger.fine(()->"Received a packet of length "+p.getLength()+" from "+p.getAddress());
                        try {
                            return new QueclinkUDPTracker.MessageAsUDPMessage(converter.convert(charset.decode(ByteBuffer.wrap(p.getData(), 0, p.getLength()))), getSocket(), p.getSocketAddress());
                        } catch (ParseException e) {
                            throw new IOException(e);
                        }
                    }
                };
            }
        };
        udpReceiver.addListener(this, /* validMessagesOnly */ true);
        final Thread t = new Thread(udpReceiver, "Queclink UDP receiver on port "+getPort());
        t.setDaemon(true);
        logger.info("Starting Queclink UDP listener thread for port "+getPort());
        t.start();
    }

    /**
     * Sends the {@code message} to the device identified by {@code deviceIdentifier}; this only works if messages
     * have already been received from that device by this tracker, and the socket connection is still open. Otherwise,
     * an {@link IllegalStateException} will be thrown.
     */
    @Override
    public void sendToDevice(SmartphoneImeiIdentifier deviceIdentifier, Message message) throws IOException {
        final String imei = deviceIdentifier.getImei();
        final Pair<DatagramSocket, SocketAddress> socketAddress = socketAddressesByImei.get(imei);
        if (socketAddress == null) {
            throw new IllegalStateException("No connection exists to the device with IMEI "+imei+"; cannot send message "+message.getMessageString());
        }
        final ByteBuffer bytesToSend = charset.encode(message.getMessageString());
        final byte[] bytes = new byte[bytesToSend.limit()];
        bytesToSend.get(bytes);
        final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, socketAddress.getB());
        socketAddress.getA().send(packet);
    }
    
    @Override
    public void received(MessageAsUDPMessage message) {
        logger.fine(()->"Received a message with "+Util.size(message.getMessages())+" Queclink messages from "+message.getSender());
        final MessageVisitor<Void> storeFixVisitor = new MessageVisitorWithSensorFixStore<Pair<DatagramSocket, SocketAddress>>(
                sensorFixStore, this, socketAddressesByImei, new Pair<>(message.getSocket(), message.getSender()));
        message.getMessages().forEach(m->m.accept(storeFixVisitor));
    }

    /**
     * Tells the port on which this UDP listener is listening. This is particularly useful when {@code 0}
     * was passed to the {@link #QueclinkUDPTracker(int) constructor} to let the system pick any free port.
     */
    public int getPort() {
        return udpReceiver.getPort();
    }

    public void stop() throws SocketException, IOException {
        udpReceiver.stop();
    }
}
