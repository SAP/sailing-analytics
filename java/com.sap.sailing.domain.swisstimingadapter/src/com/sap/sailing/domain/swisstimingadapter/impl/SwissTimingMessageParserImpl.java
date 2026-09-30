package com.sap.sailing.domain.swisstimingadapter.impl;

import java.net.DatagramPacket;

import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFormatException;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingMessage;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingMessageParser;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class SwissTimingMessageParserImpl implements SwissTimingMessageParser {

    @Override
    public SwissTimingMessage parse(byte[] message) throws SwissTimingFormatException {
        return parse(message, 0, message.length);
    }
    
    @Override
    public SwissTimingMessage parse(byte[] message, int offset, int length) throws SwissTimingFormatException {
        int i = offset;
        while (i<offset+length-1 && (message[i] != 0x10 || message[i+1] != 0x00)) {
            i++;
        }
        if (i>=offset+length-1 || message[i] != 0x10 || message[i+1] != 0x00) {
            throw new SwissTimingFormatException("Didn't find start marker 0x1000", message);
        }
        i+=2;
        String raceID = new String(message, i, 9);
        i+=9;
        int packetID = parseInt(message, i);
        i+=4;
        long timestamp = parseLong(message, i);
        i+=8;
        byte packetType = message[i];
        if (packetType != 0x02) {
            throw new SwissTimingFormatException("Expected to find package type 0x02", message);
        }
        i+=1;
        short gpsID = parseShort(message, i);
        i+=2;
        int longitude = parseInt(message, i);
        i+=4;
        int latitude = parseInt(message, i);
        i+=4;
        int speedInDecimetersPerSecond = 0xFF & message[i];
        i+=1;
        int course = parseShort(message, i);
        i+=2;
        int numberOfSatellites = (message[i] & 0xF0) >> 4;
        int batteryPercent = 10 * (message[i] & 0x0F);
        i+=1;
        if (message[i] != 0x10 || message[i+1] != 0x30) {
            throw new SwissTimingFormatException("Didn't find end marker 0x1030", message);
        }
        i+=2;
        return new SwissTimingMessageImpl(raceID, packetID, new MillisecondsTimePoint(timestamp), gpsID,
                new DegreePosition((double) latitude / 10000000., (double) longitude / 10000000.),
                new KilometersPerHourSpeedWithBearingImpl(0.36*speedInDecimetersPerSecond, new DegreeBearingImpl(course)),
                numberOfSatellites, batteryPercent, i-offset);
    }

    private short parseShort(byte[] message, int offset) {
        short result = 0;
        for (int b=0; b<2; b++) {
            result <<= 8;
            result += (0xFF & message[offset+b]);
        }
        return result;
    }

    private long parseLong(byte[] message, int offset) {
        long result = 0l;
        for (int b=0; b<8; b++) {
            result <<= 8;
            result += (0xFF & message[offset+b]);
        }
        return result;
    }

    private int parseInt(byte[] message, int offset) {
        int result = 0;
        for (int b=0; b<4; b++) {
            result <<= 8;
            result += (0xFF & message[offset+b]);
        }
        return result;
    }

    @Override
    public SwissTimingMessage parse(DatagramPacket p) throws SwissTimingFormatException {
        return parse(p.getData(), p.getOffset(), p.getLength());
    }

}
