package com.sap.sailing.expeditionconnector.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.expeditionconnector.UDPMirror;

@Timeout(value = 60, unit=TimeUnit.SECONDS)
public class UDPMirrorTest {
    private static final int MIRROR_PORT = 33795;
    private static final int RECEIVING_PORT = 33796;
    private Thread mirrorThread;
    
    @BeforeEach
    public void startMirror() throws InterruptedException {
        mirrorThread = new Thread() {
            public void run() {
                try {
                    UDPMirror.main(new String[] { ""+MIRROR_PORT, "localhost", ""+RECEIVING_PORT });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        mirrorThread.start();
        Thread.sleep(1000); // ensure that thread has really started to listen
    }
    
    @Test
    public void testSimplePacket() throws InterruptedException, IOException {
        final byte[] buf1 = new byte[512];
        final byte[] buf2 = new byte[512];
        final boolean[] didReceive = new boolean[1];
        DatagramSocket sendingSocket = new DatagramSocket();
        DatagramPacket packetToSend = new DatagramPacket(buf2, buf2.length, InetAddress.getLocalHost(), MIRROR_PORT);
        new Thread("UDPMirror test receiver") {
            public void run() {
                try {
                    DatagramSocket udpSocket;
                    udpSocket = new DatagramSocket(RECEIVING_PORT);
                    DatagramPacket received = new DatagramPacket(buf1, buf1.length);
                    udpSocket.receive(received);
                    synchronized (didReceive) {
                        didReceive[0] = true;
                        didReceive.notifyAll();
                    }
                    
                    udpSocket.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
        Thread.sleep(300); // ensure the receiver thread has started
        Random r = new Random();
        r.nextBytes(buf2);
        sendingSocket.send(packetToSend);
        synchronized (didReceive) {
            while (!didReceive[0]) {
                didReceive.wait();
            }
        }
        sendingSocket.close();
        
        assertTrue(Arrays.equals(buf1, buf2));
    }
    
    @SuppressWarnings("deprecation")
    @AfterEach
    public void stopMirror() {
        mirrorThread.stop();
    }
}
