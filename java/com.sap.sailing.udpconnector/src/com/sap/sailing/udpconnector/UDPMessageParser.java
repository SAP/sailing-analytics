package com.sap.sailing.udpconnector;

import java.io.IOException;
import java.net.DatagramPacket;

@FunctionalInterface
public interface UDPMessageParser<MessageType> {
    MessageType parse(DatagramPacket p) throws IOException;
}
