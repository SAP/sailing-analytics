package com.sap.sailing.expeditionconnector.impl;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.sailing.expeditionconnector.ExpeditionMessage;
import com.sap.sailing.expeditionconnector.UDPExpeditionReceiver;
import com.sap.sailing.udpconnector.UDPMessageParser;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ExpeditionMessageParser implements UDPMessageParser<ExpeditionMessage> {
    private static final Logger logger = Logger.getLogger(ExpeditionMessageParser.class.getName());

    private UDPExpeditionReceiver receiver;

    public ExpeditionMessageParser(UDPExpeditionReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public ExpeditionMessage parse(DatagramPacket p) {
        String packetAsString = new String(p.getData(), p.getOffset(), p.getLength()).trim().replaceAll(" ", "");
        if (packetAsString.length() > 0) {
            Pattern completeLinePattern = Pattern
                    .compile("#([0-9]*)((,([0-9][0-9]*),(-?[0-9]*(\\.[0-9]*)?))*)\\*X?([0-9a-fA-F][0-9a-fA-F]*)");
            Matcher m = completeLinePattern.matcher(packetAsString);
            boolean valid = m.matches();
            if (valid) {
                int boatID = Integer.valueOf(m.group(1));
                String variableValuePairs = m.group(2).trim().substring(",".length()); // skip the leading ","
                Map<Integer, Double> values = new HashMap<Integer, Double>();
                String[] variablesAndValuesInterleaved = variableValuePairs.split(",");
                long now = System.currentTimeMillis();
                Long diff = receiver.getLastKnownMessageDelayInMillis(boatID);
                TimePoint defaultForMessageTimePoint;
                if (diff != null) {
                    // compute a reasonable default for a time stamp in case message doesn't provide one
                    // by subtracting the last diff from now
                    defaultForMessageTimePoint = new MillisecondsTimePoint(now - diff);
                } else {
                    defaultForMessageTimePoint = null;
                }
                for (int i = 0; i < variablesAndValuesInterleaved.length; i++) {
                    int variableID = Integer.valueOf(variablesAndValuesInterleaved[i++]);
                    double variableValue = Double.valueOf(variablesAndValuesInterleaved[i]);
                    values.put(variableID, variableValue);
                }
                int checksum = Integer.valueOf(m.group(m.groupCount()), 16);
                valid = valid && checksumOk(checksum, packetAsString);
                ExpeditionMessage result;
                if (defaultForMessageTimePoint == null) {
                    result = new ExpeditionMessageImpl(boatID, values, valid, packetAsString);
                } else {
                    result = new ExpeditionMessageImpl(boatID, values, valid, defaultForMessageTimePoint, packetAsString);
                }
                if (result.hasValue(ExpeditionMessage.ID_GPS_TIME)) {
                    // an original GPS time stamp; then remember the difference between now and the time stamp
                    receiver.updateLastKnownMessageDelay(boatID, now - result.getTimePoint().asMillis());
                }
                return result;
            } else {
                logger.warning("Unparsable expedition message: " + packetAsString);
                return null; // couldn't even parse
            }
        } else {
            return null; // empty package
        }
    }

    private boolean checksumOk(int checksum, String packetAsString) {
        int b = computeChecksum(packetAsString);
        return b == checksum;
    }

    int computeChecksum(String packetAsString) {
        int b = 0;
        int posOfLastCharToAddToChecksum = packetAsString.lastIndexOf('*');
        if (packetAsString.length() > posOfLastCharToAddToChecksum && packetAsString.charAt(posOfLastCharToAddToChecksum+1) == 'X') {
            posOfLastCharToAddToChecksum++;
        }
        String checksumString = packetAsString.substring(0, posOfLastCharToAddToChecksum);
        for (byte stringByte : checksumString.getBytes()) {
            b ^= stringByte;
        }
        return b;
    }

}
