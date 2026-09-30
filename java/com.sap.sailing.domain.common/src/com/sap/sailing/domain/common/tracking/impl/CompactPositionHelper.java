package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;

/**
 * A utility class that "compact" fix implementations such as {@link VeryCompactGPSFixImpl} etc. can use
 * to obtain representations of latitudes, longitudes, knot speeds and degree bearings marshalled in
 * types that sacrifice very little accuracy from a real-world perspective, yet use a data type for
 * encoding that requires fewer bytes than a {@code double} value.<p>
 * 
 * Latitude and longitude values are represented using a signed {@code int} value, scaling their range (-90°..+90° and
 * -180°..+180°, respectively over the full {@code int} range. This gives a resolution of 4.6mm for latitudes,
 * and at the equator of 9.3mm for longitudes.<p>
 * 
 * Bearings / courses over ground are represented as a signed {@code short} value, mapping the range of -360°..+360° to the
 * value range of the {@code short} datatype. This results in a resolution of 0.01°.<p>
 * 
 * Speed values are represented in knots in their compact form; lossy compaction uses a signed {@code short} value for
 * the knot speeds, assuming a range of -500kts..+500kts, spreading this range over the value range of the {@code short}
 * datatype. This results in a resolution of 0.015kts.<p>
 * 
 * If an attempt is made to obtain a compact form of a knot speed or bearing where the value if out of the range described above,
 * a {@link CompactionNotPossibleException} will be thrown. Callers should then transparently resort to the precise
 * compaction counterparts for the respective type.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CompactPositionHelper {
    private static final double LAT_SCALE = 90. / (double) (1 << 31);  // int
    private static final double LNG_SCALE = 180. / (double) (1 << 31); // int
    
    private static final double KNOT_SPEED_SCALE = 500. /* knots; what that we track would ever be faster? */ / (double) (1<<15); // short
    private static final double DEGREE_BEARING_SCALE = 360. / (double) (1<<15); // short

    // Positions
    public static double getLatDeg(int latDegScaled) {
        return LAT_SCALE * latDegScaled;
    }

    public static double getLngDeg(int lngDegScaled) {
        return LNG_SCALE * lngDegScaled;
    }

    public static int getLngDegScaled(Position position) {
        return (int) (position.getLngDeg() / LNG_SCALE);
    }

    public static int getLatDegScaled(Position position) {
        return (int) (position.getLatDeg() / LAT_SCALE);
    }
    
    // Speeds / Bearings
    public static double getKnotSpeed(short knotSpeedScaled) {
        return KNOT_SPEED_SCALE * knotSpeedScaled;
    }

    public static double getDegreeBearing(short degreeBearingScaled) {
        return DEGREE_BEARING_SCALE * degreeBearingScaled;
    }

    public static short getKnotSpeedScaled(Speed speed) throws CompactionNotPossibleException {
        final double knotSpeedScaled = speed.getKnots() / KNOT_SPEED_SCALE;
        if (knotSpeedScaled > Short.MAX_VALUE || knotSpeedScaled < Short.MIN_VALUE) {
            throw new CompactionNotPossibleException("Speed "+speed+" cannot be compacted; "+speed.getKnots()+" does not fit into a signed short value");
        }
        return (short) knotSpeedScaled;
    }

    public static short getDegreeBearingScaled(Bearing bearing) {
        return (short) (bearing.getDegrees() / DEGREE_BEARING_SCALE);
    }
}
