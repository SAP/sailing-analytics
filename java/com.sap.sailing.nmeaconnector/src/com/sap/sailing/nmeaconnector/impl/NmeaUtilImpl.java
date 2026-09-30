package com.sap.sailing.nmeaconnector.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.nmeaconnector.NmeaUtil;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.parser.SentenceParser;
import net.sf.marineapi.nmea.sentence.MWVSentence;
import net.sf.marineapi.nmea.util.Units;

public class NmeaUtilImpl implements NmeaUtil {
    private static Map<String, Class<? extends SentenceParser>> proprietaryParsers;
    
    static {
        proprietaryParsers = new HashMap<>();
        proprietaryParsers.put("BAT", BATParser.class);
        proprietaryParsers.put("AAM", AAMParser.class);
        proprietaryParsers.put("GLC", GLCParser.class);
        proprietaryParsers.put("BWC", BWCParser.class);
        proprietaryParsers.put("BWR", BWRParser.class);
    }
    
    @Override
    public void registerAdditionalParsers() {
        SentenceFactory sentenceFactory = SentenceFactory.getInstance();
        for (Entry<String, Class<? extends SentenceParser>> e : proprietaryParsers.entrySet()) {
            if (!sentenceFactory.hasParser(e.getKey())) {
                sentenceFactory.registerParser(e.getKey(), e.getValue());
            }
        }
    }
    
    @Override
    public void unregisterAdditionalParsers() {
        SentenceFactory sentenceFactory = SentenceFactory.getInstance();
        for (Entry<String, Class<? extends SentenceParser>> e : proprietaryParsers.entrySet()) {
            if (!sentenceFactory.hasParser(e.getKey())) {
                sentenceFactory.unregisterParser(e.getValue());
            }
        }
    }
    
    @Override
    public Wind getWind(TimePoint timePoint, Position position, MWVSentence mwvSentence) {
        return new WindImpl(position, timePoint, new KnotSpeedWithBearingImpl(mwvSentence.getSpeed(),
                new DegreeBearingImpl(mwvSentence.getAngle())));
    }
    
    @Override
    public Speed getSpeed(double magnitude, Units unit) {
        switch (unit) {
        case KMH:
            return new KilometersPerHourSpeedImpl(magnitude);
        case KNOT:
            return new KnotSpeedImpl(magnitude);
        case METER:
            return new KilometersPerHourSpeedImpl(magnitude * 3.6);
        default:
            throw new IllegalArgumentException("Unit "+unit+" not understood for a speed"); 
        }
    }

    @Override
    public SpeedWithBearing getSpeedWithBearing(double speedMagnitude, Units speedUnit, double bearingInDegrees) {
        Bearing bearing = new DegreeBearingImpl(bearingInDegrees);
        switch (speedUnit) {
        case KMH:
            return new KilometersPerHourSpeedWithBearingImpl(speedMagnitude, bearing);
        case KNOT:
            return new KnotSpeedWithBearingImpl(speedMagnitude, bearing);
        case METER:
            return new KilometersPerHourSpeedWithBearingImpl(speedMagnitude * 3.6, bearing);
        default:
            throw new IllegalArgumentException("Unit "+speedUnit+" not understood for a speed"); 
        }
    }

    @Override
    public String replace(String nmeaSentence, String regexToFind, String replaceWith) {
        while (nmeaSentence.matches(".*" + regexToFind + ".*")) {
            final String resultWithOldChecksum = nmeaSentence.replaceFirst(regexToFind, replaceWith);
            if (resultWithOldChecksum.matches(".*\\*[0-9a-fA-F][0-9a-fA-F]$")) {
                // found checksum
                int checksum = Integer.valueOf(resultWithOldChecksum.substring(resultWithOldChecksum.length() - 2), 16);
                for (char oldChar : regexToFind.toCharArray()) {
                    checksum ^= (byte) oldChar;
                }
                for (char newChar : replaceWith.toCharArray()) {
                    checksum ^= (byte) newChar;
                }
                String newHexChecksum = Integer.toHexString(checksum).toUpperCase();
                if (newHexChecksum.length() == 1) {
                    newHexChecksum = "0" + newHexChecksum;
                }
                nmeaSentence = resultWithOldChecksum.substring(0, resultWithOldChecksum.length() - 2) + newHexChecksum;
            } else {
                nmeaSentence = resultWithOldChecksum;
            }
        }
        return nmeaSentence;
    }
    
}
