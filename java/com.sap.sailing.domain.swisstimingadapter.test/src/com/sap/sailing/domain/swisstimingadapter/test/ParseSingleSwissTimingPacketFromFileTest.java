package com.sap.sailing.domain.swisstimingadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFormatException;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingMessage;

public class ParseSingleSwissTimingPacketFromFileTest {
    @Test
    public void readSinglePacketFromFile() throws IOException, SwissTimingFormatException {
        byte[] message = new byte[65536];
        InputStream is = getClass().getResourceAsStream("/SwissTimingExampleTrack.bin");
        is.read(message);
        SwissTimingMessage swissTimingMessage = SwissTimingFactory.INSTANCE.createMessageParser().parse(message);
        assertNotNull(swissTimingMessage);
        int lat = ((0x1e<<8 | 0x2c)<<8 | 0x31)<<8 | 0x14;
        int lng = ((0xfe << 8 | 0x97)<<8 | 0x1d)<<8 | 0x73;
        assertEquals((double) lat / 10000000., swissTimingMessage.getPosition().getLatDeg(), 0.0000000001);
        assertEquals((double) lng / 10000000., swissTimingMessage.getPosition().getLngDeg(), 0.0000000001);
    }
    
    @Test
    public void readAllPacketsFromFile() throws IOException, SwissTimingFormatException {
        byte[] message = new byte[65536];
        List<SwissTimingMessage> messages = new ArrayList<SwissTimingMessage>();
        InputStream is = getClass().getResourceAsStream("/SwissTimingExampleTrack.bin");
        int bytesRead = is.read(message);
        int offset = 0;
        int messageLength = 0;
        while (offset < bytesRead) {
            SwissTimingMessage swissTimingMessage = SwissTimingFactory.INSTANCE.createMessageParser().parse(message, offset, bytesRead-offset);
            assertNotNull(swissTimingMessage);
            messages.add(swissTimingMessage);
            messageLength = swissTimingMessage.length();
            offset += messageLength;
        }
        assertEquals(bytesRead/messageLength, messages.size());
    }
    
}
