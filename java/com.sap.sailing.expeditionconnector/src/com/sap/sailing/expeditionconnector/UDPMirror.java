package com.sap.sailing.expeditionconnector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Receives UDP packets on a port and sends the out, unmodified, to a list of host/ports again.
 * This can be used, e.g., as a hub for several Expedition clients that are to be interlinked,
 * or for collecting and forwarding messages from multiple Expedition clients. 
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class UDPMirror {

    /**
     * @param args 0: the port to listen to; 2*i-1, 2*i for i>0: host/port to which to forward
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
        } else {
            int c = 0;
            boolean verbose = false;
            if (args[c].equals("-v")) {
                c++;
                verbose = true;
            }
            int listeningOnPort = Integer.valueOf(args[c++]);
            byte[] buf = new byte[65536];
            DatagramSocket udpSocket = new DatagramSocket(listeningOnPort);
            DatagramPacket received = new DatagramPacket(buf, buf.length);
            DatagramSocket[] sendingSockets = new DatagramSocket[(args.length - 1) / 2];
            DatagramPacket[] mirroredPackets = new DatagramPacket[(args.length - 1) / 2];
            while (c < args.length - 1) {
                sendingSockets[(c - 1) / 2] = new DatagramSocket();
                mirroredPackets[(c - 1) / 2] = new DatagramPacket(buf, buf.length, InetAddress.getByName(args[c]),
                        Integer.valueOf(args[c + 1]));
                c += 2;
            }
            while (true) {
                udpSocket.receive(received);
                if (verbose) {
                    String packetAsString = new String(received.getData(), received.getOffset(), received.getLength()).trim();
                    System.out.println(packetAsString);
                }
                for (int i = 0; i < mirroredPackets.length; i++) {
                    mirroredPackets[i].setLength(received.getLength());
                    sendingSockets[i].send(mirroredPackets[i]);
                }
            }
        }
    }
    
    private static void usage() {
        System.out.println("Usage: java "+UDPMirror.class.getName()+" [-v] <listeningport> hostname1 port1 [hostname2 port2]*");
        System.out.println("  -v\tPrint packets received to stdout");
    }
}
