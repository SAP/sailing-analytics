package com.sap.sailing.nmeaconnector;

import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.MWVSentence;
import net.sf.marineapi.nmea.util.Units;

public interface NmeaUtil {
    Wind getWind(TimePoint timePoint, Position position, MWVSentence mwvSentence);

    Speed getSpeed(double magnitude, Units unit);

    SpeedWithBearing getSpeedWithBearing(double speedMagnitude, Units speedUnit, double bearingInDegrees);

    /**
     * Replaces all occurrences of <code>sequenceToFind</code> by <code>replaceWith</code>, as {@link String#replace(CharSequence, CharSequence)},
     * but assuming that <code>nmeaSentence</code> has a trailing NMEA checksum, adjusts the checksum accordingly. This will turn a valid
     * checksum into a valid checksum again, and an invalid checksum into an invalid checksum.
     */
    String replace(String nmeaSentence, String sequenceToFind, String replaceWith);

    /**
     * Call this to register proprietary parsers provided by this bundle with the
     * {@link SentenceFactory} {@link SentenceFactory#getInstance() default instance}.
     */
    void registerAdditionalParsers();

    /**
     * Call this to unregister proprietary parsers provided by this bundle from the
     * {@link SentenceFactory} {@link SentenceFactory#getInstance() default instance}.
     */
    void unregisterAdditionalParsers();
}
