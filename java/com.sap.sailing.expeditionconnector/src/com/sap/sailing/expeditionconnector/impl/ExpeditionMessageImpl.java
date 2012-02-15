package com.sap.sailing.expeditionconnector.impl;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.expeditionconnector.ExpeditionMessage;

public class ExpeditionMessageImpl implements ExpeditionMessage {
    private final String originalMessage;
    private final int boatID;
    private final Map<Integer, Double> values;
    private final boolean valid;
    private final long createdAtMillis;
    private final TimePoint timePoint;
    
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC") == null ? TimeZone.getTimeZone("GMT") : TimeZone.getTimeZone("UTC");
    private static final GregorianCalendar cal = new GregorianCalendar(UTC);
    
    static {
        cal.set(1899, 11, 30, 0, 0, 0);
    }

    /**
     * Creates a message instance based on <code>values</code>. If there is no timestamp delivered by
     * <code>values</code>, the {@link System#currentTimeMillis() current time} is used as the message's
     * {@link #getTimePoint() time point}.
     */
    public ExpeditionMessageImpl(int boatID, Map<Integer, Double> values, boolean valid, String originalMessage) {
        this.boatID = boatID;
        // ensure that nobody can manipulate the map used by this message object from outside
        this.values = new HashMap<Integer, Double>(values);
        this.valid = valid;
        this.originalMessage = originalMessage;
        this.createdAtMillis = System.currentTimeMillis();
        if (hasValue(ID_GPS_TIME)) {
            timePoint = new MillisecondsTimePoint((long)
                    (getValue(ID_GPS_TIME)*24*3600*1000) +   // this is the milliseconds since 31.12.1899 0:00:00 UTC
                    cal.getTimeInMillis());
        } else {
            timePoint = new MillisecondsTimePoint(createdAtMillis);
        }
    }
    
    /**
     * @param defaultTimePoint
     *            a non-<code>null</code> default time point to use in case the message received does not carry a time
     *            stamp
     */
    public ExpeditionMessageImpl(int boatID, Map<Integer, Double> values, boolean valid, TimePoint defaultTimePoint, String originalMessage) {
        if (defaultTimePoint == null) {
            throw new IllegalArgumentException("defaultTimePoint for ExpeditionMessageImpl constructor must not be null");
        }
        this.boatID = boatID;
        // ensure that nobody can manipulate the map used by this message object from outside
        this.values = new HashMap<Integer, Double>(values);
        this.valid = valid;
        this.originalMessage = originalMessage;
        this.createdAtMillis = System.currentTimeMillis();
        if (hasValue(ID_GPS_TIME)) {
            timePoint = new MillisecondsTimePoint((long)
                    (getValue(ID_GPS_TIME)*24*3600*1000) +   // this is the milliseconds since 31.12.1899 0:00:00 UTC
                    cal.getTimeInMillis());
        } else {
            timePoint = defaultTimePoint;
        }
    }
    
    @Override
    public String getOriginalMessage() {
        return originalMessage;
    }
    
    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public int getBoatID() {
        return boatID;
    }

    @Override
    public Set<Integer> getVariableIDs() {
        return Collections.unmodifiableSet(values.keySet());
    }

    @Override
    public boolean hasValue(int variableID) {
        return values.containsKey(variableID);
    }

    @Override
    public double getValue(int variableID) {
        if (!hasValue(variableID)) {
            throw new IllegalArgumentException("Variable ID "+variableID+" not present in message");
        }
        return values.get(variableID);
    }

    @Override
    public GPSFix getGPSFix() {
        if (hasValue(ID_GPS_LAT) && hasValue(ID_GPS_LNG)) {
            return new GPSFixImpl(new DegreePosition(getValue(ID_GPS_LAT), getValue(ID_GPS_LNG)), getTimePoint());
        } else {
            return null;
        }
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    @Override
    public TimePoint getCreatedAt() {
        return new MillisecondsTimePoint(createdAtMillis);
    }

    @Override
    public GPSFixMoving getGPSFixMoving() {
        if (hasValue(ID_GPS_LAT) && hasValue(ID_GPS_LNG)) {
            return new GPSFixMovingImpl(new DegreePosition(getValue(ID_GPS_LAT), getValue(ID_GPS_LNG)), getTimePoint(),
                    getSpeedOverGround());
        } else {
            return null;
        }
    }

    @Override
    public SpeedWithBearing getTrueWind() {
        if (hasValue(ID_TWD) && hasValue(ID_TWS)) {
            return new KnotSpeedWithBearingImpl(getValue(ID_TWS), getTrueWindBearing());
        } else if (hasValue(ID_GWD) && hasValue(ID_GWS)) {
            return new KnotSpeedWithBearingImpl(getValue(ID_GWS), getTrueWindBearing());
        } else {
            return null;
        }
    }
    
    @Override
    public Bearing getTrueWindBearing() {
        if (hasValue(ID_TWD)) { // TWD represents the "from" direction and need to be reversed to obtain the "to" bearing
            return new DegreeBearingImpl(getValue(ID_TWD)).reverse();
        } else if (hasValue(ID_GWD)) {
                return new DegreeBearingImpl(getValue(ID_GWD)).reverse();
        } else {
            return null;
        }
    }

    @Override
    public SpeedWithBearing getSpeedOverGround() {
        if (hasValue(ID_GPS_COG) && hasValue(ID_GPS_SOG)) {
            return new KnotSpeedWithBearingImpl(getValue(ID_GPS_SOG), getCourseOverGround());
        } else {
            return null;
        }
    }
    
    @Override
    public Bearing getCourseOverGround() {
        if (hasValue(ID_GPS_COG)) {
            return new DegreeBearingImpl(getValue(ID_GPS_COG));
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Boat #");
        result.append(getBoatID());
        result.append(": ");
        TreeSet<Integer> ids = new TreeSet<Integer>(values.keySet());
        boolean first = true;
        for (Integer id : ids) {
            if (!first) {
                result.append(", ");
            } else {
                first = false;
            }
            result.append(id);
            result.append(":");
            result.append(getValue(id));
        }
        return result.toString();
    }
}
