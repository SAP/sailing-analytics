package com.sap.sailing.nmeaconnector.impl;

import net.sf.marineapi.nmea.sentence.MWVSentence;
import net.sf.marineapi.nmea.util.Units;

import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.nmeaconnector.NmeaUtil;

public class NmeaUtilImpl implements NmeaUtil {

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
    public String replace(String nmeaSentence, String sequenceToFind, String replaceWith) {
        final String result;
        final String resultWithOldChecksum = nmeaSentence.replace(sequenceToFind, replaceWith);
        if (resultWithOldChecksum.matches(".*\\*[0-9a-fA-F][0-9a-fA-F]$")) {
            // found checksum
            int checksum = Integer.valueOf(resultWithOldChecksum.substring(resultWithOldChecksum.length()-2), 16);
            for (char oldChar : sequenceToFind.toCharArray()) {
                checksum ^= (byte) oldChar;
            }
            for (char newChar : replaceWith.toCharArray()) {
                checksum ^= (byte) newChar;
            }
            String newHexChecksum = Integer.toHexString(checksum).toUpperCase();
            if (newHexChecksum.length() == 1) {
                newHexChecksum = "0" + newHexChecksum;
            }
            result = resultWithOldChecksum.substring(0, resultWithOldChecksum.length()-2)+newHexChecksum;
        } else {
            result = resultWithOldChecksum;
        }
        return result;
    }
    
}
