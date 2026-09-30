package com.sap.sailing.expeditionconnector.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.expeditionconnector.ExpeditionMessage;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ExpeditionMessageImpl implements ExpeditionMessage {
    private static final Logger logger = Logger.getLogger(ExpeditionMessageImpl.class.getName());

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
        this(boatID, values, valid, /* defaultTimePoint */ null, originalMessage, /* unused */ true);
    }
    
    /**
     * @param defaultTimePoint
     *            a non-<code>null</code> default time point to use in case the message received does not carry a time
     *            stamp
     */
    public ExpeditionMessageImpl(int boatID, Map<Integer, Double> values, boolean valid, TimePoint defaultTimePoint, String originalMessage) {
        this(boatID, values, valid, defaultTimePoint, originalMessage, /* unused */ true);
        if (defaultTimePoint == null) {
            throw new IllegalArgumentException("defaultTimePoint for this ExpeditionMessageImpl constructor must not be null");
        }
    }
    
    private ExpeditionMessageImpl(int boatID, Map<Integer, Double> values, boolean valid, TimePoint defaultTimePoint, String originalMessage, boolean unused) {
        this.boatID = boatID;
        // ensure that nobody can manipulate the map used by this message object from outside
        this.values = new HashMap<Integer, Double>(values);
        this.valid = valid;
        this.originalMessage = originalMessage;
        this.createdAtMillis = System.currentTimeMillis();
        if (hasValue(ID_GPS_TIME)) {
            final double gpsTimeValue = getValue(ID_GPS_TIME);
            final long referenceTime;
            if (gpsTimeValue <= 1.0) { // "Phoenix" law: if between 0 and 1 then it's likely a day-relative time point
                // so add the millis of today midnight UTC
                Calendar todayMidnightUTC = new GregorianCalendar();
                todayMidnightUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
                todayMidnightUTC.set(Calendar.HOUR_OF_DAY, 0);
                todayMidnightUTC.set(Calendar.MINUTE, 0);
                todayMidnightUTC.set(Calendar.SECOND, 0);
                todayMidnightUTC.set(Calendar.MILLISECOND, 0);
                referenceTime = todayMidnightUTC.getTimeInMillis();
            } else {
                referenceTime = cal.getTimeInMillis();
            }
            timePoint = new MillisecondsTimePoint((long)
                    (gpsTimeValue*24*3600*1000) +   // this is the milliseconds since 31.12.1899 0:00:00 UTC
                    referenceTime);
        } else if (defaultTimePoint == null) {
            timePoint = new MillisecondsTimePoint(createdAtMillis);
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
        final GPSFixMoving result;
        if (hasValue(ID_GPS_LAT) && hasValue(ID_GPS_LNG)) {
            final SpeedWithBearing cogSog = getSpeedOverGround();
            if (cogSog != null) {
                final DegreePosition position = new DegreePosition(getValue(ID_GPS_LAT), getValue(ID_GPS_LNG));
                Bearing optionalTrueHeading;
                if (hasValue(ID_HEADING)) {
                    try {
                        optionalTrueHeading = new DegreeBearingImpl(getValue(ID_HEADING)).add(
                                DeclinationService.INSTANCE.getDeclination(getTimePoint(), position, /* timeoutForOnlineFetchInMilliseconds */ 1000).getBearingCorrectedTo(getTimePoint()));
                    } catch (IOException | ParseException e) {
                        logger.log(Level.WARNING, "Problem obtaining the magnetic declination for Expedition fix", e);
                        optionalTrueHeading = null;
                    }
                } else {
                    optionalTrueHeading = null;
                }
                result = new GPSFixMovingImpl(position, getTimePoint(),
                        getSpeedOverGround(), optionalTrueHeading);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public SpeedWithBearing getTrueWind() {
        final SpeedWithBearing result;
        // FIXME bug 5223: prefer own TWD calculation over reported TWD/TWS because that may still contain a current-based correction
        if (hasValue(ID_GWD) && hasValue(ID_GWS)) {
            result = new KnotSpeedWithBearingImpl(getValue(ID_GWS), getTrueWindBearing());
        } else if (hasValue(ID_TWD) && hasValue(ID_TWS)) {
            result = new KnotSpeedWithBearingImpl(getValue(ID_TWS), getTrueWindBearing());
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public Bearing getTrueWindBearing() {
        final Bearing result;
        if (hasValue(ID_TWD)) { // TWD represents the "from" direction and need to be reversed to obtain the "to" bearing
            result = new DegreeBearingImpl(getValue(ID_TWD)).reverse();
        } else if (hasValue(ID_GWD)) {
            result = new DegreeBearingImpl(getValue(ID_GWD)).reverse();
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public SpeedWithBearing getSpeedOverGround() {
        final SpeedWithBearing result;
        if (hasValue(ID_GPS_COG)) {
            // We're assuming that if SOG is not part of the message but COG is then
            // the SOG was probably so low that it's fair to approximate it as 0.
            final double sogInKnots = hasValue(ID_GPS_SOG) ? getValue(ID_GPS_SOG) : 0;
            result = new KnotSpeedWithBearingImpl(sogInKnots, getCourseOverGround());
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public Bearing getCourseOverGround() {
        final Bearing result;
        if (hasValue(ID_GPS_COG)) {
            result = new DegreeBearingImpl(getValue(ID_GPS_COG));
        } else {
            result = null;
        }
        return result;
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
