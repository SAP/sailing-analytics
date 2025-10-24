package com.sap.sailing.udpconnector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A runnable received for UDP messages; when run, it starts listening for incoming UDP datagrams on the port specified to the
 * {@link #UDPReceiver(int) constructor}. Non-empty datagrams are then submitted to the {@link UDPMessageParser} returned by
 * {@link #getParser()} which concrete subclasses have to implement. The parser produces messages of type {@code MessageType}.
 * Those will be forwarded to {@link #addListener(UDPMessageListener, boolean) registered} {@link UDPMessageListener listeners}.
 * 
 * @author Axel Uhl (d043530)
 */
public abstract class UDPReceiver<MessageType extends UDPMessage, ListenerType extends UDPMessageListener<MessageType>> implements Runnable {
    private static final Logger logger = Logger.getLogger(UDPReceiver.class.getName());
    
    private boolean stopped = false;

    private final int listeningOnPort;

    private final DatagramSocket udpSocket;
    
    /**
     * This field is {@code null} if and only if no listener currently exists in {@link #listeners}. Modifications to
     * both have to occur while holding this object's monitor ({@code synchronized}).
     */
    private ToListenerDispatcher dispatchingToListenersThread;

    /**
     * For each listener remembers if the listener is only interested in valid messages.
     */
    private final ConcurrentMap<ListenerType, Boolean> listeners;

    /**
     * Maximum IP packet length
     */
    private static final int MAX_PACKET_SIZE = 65536;
    
    private class ToListenerDispatcher extends Thread {
        private final LinkedBlockingDeque<MessageType> queue;
        private boolean stopped;
        
        public ToListenerDispatcher() {
            super("UDPReceiver ToListenerDispatcher");
            queue = new LinkedBlockingDeque<MessageType>(/* capacity */ 10000);
            stopped = false;
        }
        
        /**
         * Offers the message to the queue used by this dispatcher. If the queue is full, the message is dropped without notice.
         */
        public void dispatch(MessageType message) {
            queue.offer(message);
        }
        
        public void halt() {
            stopped = true;
        }
        
        public void run() {
            MessageType message;
            try {
                message = queue.poll(/* timeout */ 10000, TimeUnit.MILLISECONDS);
                while (!stopped) {
                    if (message != null) {
                        for (final Entry<ListenerType, Boolean> listenerAndValidMessagesOnly : listeners.entrySet()) {
                            if (!listenerAndValidMessagesOnly.getValue() || message.isValid()) {
                                try {
                                    listenerAndValidMessagesOnly.getKey().received(message);
                                } catch (Exception e) {
                                    logger.log(Level.WARNING, "Exception "+e.getMessage()+
                                            " while trying to dispaatch UDP message "+message+
                                            " to listener "+listenerAndValidMessagesOnly.getKey(), e);
                                }
                            }
                        }
                    }
                    message = queue.poll(/* timeout */ 10000, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                // when interrupted, this means we'll terminate
                logger.warning("Listener thread got interrupted and will now terminate.");
            }
        }
    }
    
    /**
     * You need call {@link #run} to actually start receiving events. To do this asynchronously,
     * start this object in a new thread.
     */
    public UDPReceiver(int listeningOnPort) throws SocketException {
        udpSocket = listeningOnPort == 0 ? new DatagramSocket() : new DatagramSocket(listeningOnPort);
        listeners = new ConcurrentHashMap<>();
        this.listeningOnPort = udpSocket.getLocalPort();
        dispatchingToListenersThread = null;
    }
    
    public void stop() throws SocketException, IOException {
        stopped = true;
        synchronized (this) {
            if (dispatchingToListenersThread != null) {
                dispatchingToListenersThread.halt();
            }
        }
        byte[] buf = new byte[0];
        DatagramPacket stopPacket = new DatagramPacket(buf, 0, InetAddress.getLocalHost(), listeningOnPort);
        DatagramSocket stopper = new DatagramSocket();
        stopper.send(stopPacket);
        stopper.close();
        if (!udpSocket.isConnected()) {
            udpSocket.close();
        }
    }
    
    /**
     * If there are currently no listeners subscribed (see {@link #addListener(ListenerType, boolean)} and
     * {@link #removeListener(ListenerType)}), this receiver is {@link #stop() stopped} and <code>true</code>
     * is returned; otherwise, <code>false</code> is returned.
     */
    public synchronized boolean stopIfNoListeners() throws SocketException, IOException {
        boolean result = listeners.isEmpty();
        if (result) {
            stop();
        }
        return result;
    }
    
    public boolean isStopped() {
        return stopped;
    }

    public void run() {
        final byte[] buf = new byte[MAX_PACKET_SIZE];
        final DatagramPacket p = new DatagramPacket(buf, buf.length);
        while (!stopped) {
            try {
                udpSocket.receive(p);
                if (p.getLength() > 0) { // don't try to parse empty datagrams
                    MessageType msg = getParser().parse(p);
                    if (msg != null) {
                        synchronized (this) {
                            if (dispatchingToListenersThread != null) {
                                dispatchingToListenersThread.dispatch(msg);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Exception while receiving UDP packet", e);
            }
        }
        logger.info("Closing UDP socket on port "+udpSocket.getLocalPort());
        udpSocket.close();
        if (udpSocket.isConnected()) {
            udpSocket.disconnect();
        }
    }

    public synchronized void addListener(ListenerType listener, boolean validMessagesOnly) {
        listeners.put(listener, validMessagesOnly);
        if (dispatchingToListenersThread == null) {
            dispatchingToListenersThread = new ToListenerDispatcher();
            dispatchingToListenersThread.setDaemon(true);
            dispatchingToListenersThread.start();
        }
    }

    public synchronized void removeListener(ListenerType listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            dispatchingToListenersThread.halt();
            dispatchingToListenersThread = null;
        }
    }

    public int getPort() {
        return listeningOnPort;
    }
    
    public DatagramSocket getSocket() {
        return udpSocket;
    }

    protected abstract UDPMessageParser<MessageType> getParser();
}
