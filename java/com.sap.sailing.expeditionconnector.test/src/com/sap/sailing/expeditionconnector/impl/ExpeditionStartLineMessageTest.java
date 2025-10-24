package com.sap.sailing.expeditionconnector.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.SocketException;

import org.junit.jupiter.api.Test;

import com.sap.sailing.expeditionconnector.UDPExpeditionReceiver;

public class ExpeditionStartLineMessageTest {
    @Test
    public void testCreatePingStartLineMessages() throws SocketException {
        final String portMessageString = "#L,P,45.327781,-0.775225*07";
        final String starboardMessageString = "#L,S,45.337712,-0.747560*08";
        final ExpeditionMessageParser parser = new ExpeditionMessageParser(new UDPExpeditionReceiver(/* listening on port */ 0));
        final int checksumPort = parser.computeChecksum(portMessageString);
        assertEquals(7, checksumPort);
        final int checksumStarboard = parser.computeChecksum(starboardMessageString);
        assertEquals(8, checksumStarboard);
    }
}
