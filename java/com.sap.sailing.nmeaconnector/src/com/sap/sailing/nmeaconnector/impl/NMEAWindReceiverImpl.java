package com.sap.sailing.nmeaconnector.impl;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.WindListener;
import com.sap.sailing.domain.tracking.impl.DynamicTrackImpl;
import com.sap.sailing.nmeaconnector.NMEAWindReceiver;
import com.sap.sailing.nmeaconnector.TimedBearing;
import com.sap.sailing.nmeaconnector.TimedSpeedWithBearing;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MeterPerSecondSpeedImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;
import com.sap.sse.common.scalablevalue.impl.ScalablePosition;
import com.sap.sse.common.scalablevalue.impl.ScalableSpeedWithBearing;

import net.sf.marineapi.nmea.event.AbstractSentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.sentence.DateSentence;
import net.sf.marineapi.nmea.sentence.HeadingSentence;
import net.sf.marineapi.nmea.sentence.MDASentence;
import net.sf.marineapi.nmea.sentence.MWDSentence;
import net.sf.marineapi.nmea.sentence.MWVSentence;
import net.sf.marineapi.nmea.sentence.PositionSentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.TimeSentence;
import net.sf.marineapi.nmea.sentence.VTGSentence;
import net.sf.marineapi.nmea.sentence.VWRSentence;
import net.sf.marineapi.nmea.sentence.ZDASentence;
import net.sf.marineapi.nmea.util.CompassPoint;
import net.sf.marineapi.nmea.util.Date;
import net.sf.marineapi.nmea.util.Direction;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.nmea.util.Units;

public class NMEAWindReceiverImpl implements NMEAWindReceiver {
    private static final Logger logger = Logger.getLogger(NMEAWindReceiverImpl.class.getName());
    private final ConcurrentHashMap<WindListener, WindListener> listeners;
    private final DynamicTrack<TimedSpeedWithBearing> trueWind;
    private final DynamicTrack<TimedSpeedWithBearing> apparentWind;
    private final DynamicTrack<GPSFix> sensorPositions;
    private final DynamicTrack<TimedSpeedWithBearing> sensorSpeeds;
    private final DynamicTrack<TimedBearing> trueHeadings;
    
    /**
     * Don't use the {@link GregorianCalendar#getTime() time point} of this calendar unless a {@link DateSentence} and a
     * {@link TimeSentence} have been received which is indicated by the {@link #dateReceived} and the
     * {@link #timeReceived} fields, respectively.
     */
    private final GregorianCalendar utcCalendar;
    
    private final DeclinationService declinationService = DeclinationService.INSTANCE;
    
    /**
     * Until a {@link DateSentence} has been received, the {@link #utcCalendar}'s {@link GregorianCalendar#getTime()}
     * must not be used.
     */
    private boolean dateReceived;
    
    private boolean timeReceived;
    
    private class MWVSentenceListener extends AbstractSentenceListener<MWVSentence> {
        @Override
        public void sentenceRead(MWVSentence sentence) {
            final TimePoint timePoint = getLastTimePoint();
            if (timePoint != null) {
                try {
                    final Speed speed = createSpeed(sentence.getSpeedUnit(), sentence.getSpeed());
                    final KnotSpeedWithBearingAndTimepoint fix;
                    if (sentence.isTrue()) {
                        final Bearing trueHeading = trueHeadings.getInterpolatedValue(getLastTimePoint(), f->new ScalableBearing(f));
                        if (trueHeading != null) {
                            // need to take heading into account as MWV is still relative to bow, even if "true" is provided;
                            // it is what you would see on the wind display when set to "true wind"
                            fix = new KnotSpeedWithBearingAndTimepoint(timePoint,
                                    speed.getKnots(), new DegreeBearingImpl(sentence.getAngle()).reverse().add(trueHeading));
                            trueWind.add(fix);
                            tryToCreateWindFixFromTrueWind(fix);
                        }
                    } else {
                        fix = new KnotSpeedWithBearingAndTimepoint(timePoint,
                                speed.getKnots(), new DegreeBearingImpl(sentence.getAngle()).reverse());
                        apparentWind.add(fix);
                        tryToCreateWindFixFromApparentWind(fix);
                    }
                    // use the newly gained wind data to try to assemble a Wind fix if
                    // other values such as heading and position are known
                } catch (DataNotAvailableException e) {
                    // can happen... simply ignore this incomplete sentence
                }
            }
        }
    }

    private class MWDSentenceListener extends AbstractSentenceListener<MWDSentence> {
        @Override
        public void sentenceRead(MWDSentence sentence) {
            // the message provides data about the true wind direction (TWD) and true wind speed (TWS);
            // here, "true" means "not apparent"; the direction may still be provided in degrees true
            // or degrees magnetic, and we need to find out which is available
            final TimePoint timePoint = getLastTimePoint();
            if (timePoint != null) {
                final com.sap.sse.common.Position position = getPosition(timePoint);
                if (position != null) {
                    final Speed speed;
                    if (!Double.isNaN(sentence.getWindSpeed())) {
                        speed = new MeterPerSecondSpeedImpl(sentence.getWindSpeed());
                    } else if (!Double.isNaN(sentence.getWindSpeedKnots())) {
                        speed = new KnotSpeedImpl(sentence.getWindSpeedKnots());
                    } else {
                        speed = null;
                    }
                    if (speed != null) {
                        final Bearing direction;
                        if (!Double.isNaN(sentence.getTrueWindDirection())) {
                            direction = new DegreeBearingImpl(sentence.getTrueWindDirection());
                        } else {
                            Declination declination;
                            try {
                                declination = declinationService.getDeclination(timePoint, position, /* timeoutForOnlineFetchInMilliseconds */ 1000);
                                if (declination != null) {
                                    direction = new DegreeBearingImpl(sentence.getMagneticWindDirection()).add(declination.getBearingCorrectedTo(timePoint));
                                } else {
                                    direction = null;
                                }
                                if (direction != null) {
                                    final KnotSpeedWithBearingAndTimepoint fix = new KnotSpeedWithBearingAndTimepoint(timePoint,
                                            speed.getKnots(), direction);
                                    trueWind.add(fix);
                                    tryToCreateWindFixFromTrueWind(fix);
                                }
                            } catch (ParseException | IOException e) {
                                logger.log(Level.WARNING, "Couldn't correct magnetic TWD reading by declination; ignoring", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private class VTGSentenceListener extends AbstractSentenceListener<VTGSentence> {
        @Override
        public void sentenceRead(VTGSentence sentence) {
            try {
                sensorSpeeds.add(new KnotSpeedWithBearingAndTimepoint(getLastTimePoint(), sentence.getSpeedKnots(), new DegreeBearingImpl(sentence.getTrueCourse())));
            } catch (DataNotAvailableException e) {
                // no sensor speed will be recorded if data is not available
            }
        }
    }
    
    private class VWRSentenceListener extends AbstractSentenceListener<VWRSentence> {
        @Override
        public void sentenceRead(VWRSentence sentence) {
            apparentWind.add(new KnotSpeedWithBearingAndTimepoint(getLastTimePoint(), sentence.getSpeedKnots(),
                    new DegreeBearingImpl(sentence.getWindAngle() * (sentence.getDirectionLeftRight()==Direction.LEFT ? -1 : 1))));
        }
    }

    private class MDASentenceListener extends AbstractSentenceListener<MDASentence> {
        @Override
        public void sentenceRead(MDASentence sentence) {
            if (!Double.isNaN(sentence.getTrueWindDirection())) {
                if (!Double.isNaN(sentence.getWindSpeed())) {
                    trueWind.add(new KnotSpeedWithBearingAndTimepoint(getLastTimePoint(), new MeterPerSecondSpeedImpl(sentence.getWindSpeed()).getKnots(),
                            new DegreeBearingImpl(sentence.getTrueWindDirection())));
                } else if (!Double.isNaN(sentence.getWindSpeedKnots())) {
                    trueWind.add(new KnotSpeedWithBearingAndTimepoint(getLastTimePoint(), sentence.getWindSpeedKnots(),
                            new DegreeBearingImpl(sentence.getTrueWindDirection())));
                }
            }
        }
    }

    private class HeadingSentenceListener extends AbstractSentenceListener<HeadingSentence> {
        @Override
        public void sentenceRead(HeadingSentence sentence) {
            if (getLastTimePoint() != null) {
                if (sentence.isTrue()) {
                    try {
                        trueHeadings.add(new DegreeBearingWithTimepoint(getLastTimePoint(), sentence.getHeading()));
                    } catch (DataNotAvailableException e) {
                        // ignore; in this case, no true heading will be recorded
                    }
                } else {
                    final Position position = getPosition(getLastTimePoint());
                    if (position != null) {
                        try {
                            final Declination declination = declinationService.getDeclination(getLastTimePoint(), position, /* timeoutForOnlineFetchInMilliseconds */ 1000);
                            if (declination != null) {
                                try {
                                    trueHeadings.add(new DegreeBearingWithTimepoint(getLastTimePoint(),
                                        new DegreeBearingImpl(sentence.getHeading()).add(declination.getBearingCorrectedTo(getLastTimePoint())).getDegrees()));
                                } catch (DataNotAvailableException e) {
                                    // no true heading will be created if data is not available
                                }
                            }
                        } catch (IOException | ParseException e) {
                            logger.log(Level.WARNING, "Error trying to obtain magnetic declination while converting magnetic heading to true heading", e);
                        }
                    }
                }
            }
        }
    }
    
    private class DateListener extends AbstractSentenceListener<DateSentence> {
        @Override
        public void sentenceRead(DateSentence sentence) {
            try {
                final Date date = sentence.getDate();
                utcCalendar.set(date.getYear(), date.getMonth() - 1, date.getDay());
                dateReceived = true;
            } catch (DataNotAvailableException | IllegalArgumentException e) {
                // can happen; ignore sentence
            }
        }
    }
    
    private class TimeListener extends AbstractSentenceListener<TimeSentence> {
        @Override
        public void sentenceRead(TimeSentence sentence) {
            try {
                final Time time = sentence.getTime();
                utcCalendar.set(utcCalendar.get(Calendar.YEAR), utcCalendar.get(Calendar.MONTH), utcCalendar.get(Calendar.DAY_OF_MONTH),
                        time.getHour(), time.getMinutes(), (int) time.getSeconds());
                final int millisInSecond = (int) ((time.getSeconds()-(int) time.getSeconds())*1000);
                utcCalendar.set(Calendar.MILLISECOND, millisInSecond);
                timeReceived = true;
            } catch (DataNotAvailableException e) {
                // can happen; ignore sentence
            }
        }
    }
    
    private class PositionListener extends AbstractSentenceListener<PositionSentence> {
        @Override
        public void sentenceRead(PositionSentence sentence) {
            if (sentence instanceof TimeSentence) { // all PositionSentence implementations currently also implement TimeSentence
                TimeSentence timeSentence = (TimeSentence) sentence;
                // now make sure we work with the latest time from this record:
                new TimeListener().sentenceRead(timeSentence);
                final TimePoint timePoint = getLastTimePoint();
                if (timePoint != null) {
                    try {
                        final GPSFix fix = new GPSFixImpl(getPosition(sentence), timePoint);
                        sensorPositions.add(fix);
                    } catch (DataNotAvailableException e) {
                        // no sensor position is recorded in this case
                    }
                }
            }
        }

        private Position getPosition(PositionSentence sentence) throws DataNotAvailableException {
            return new DegreePosition(
                    Math.abs(sentence.getPosition().getLatitude())*(sentence.getPosition().getLatitudeHemisphere()==CompassPoint.NORTH?1:-1),
                    Math.abs(sentence.getPosition().getLongitude())*(sentence.getPosition().getLongitudeHemisphere()==CompassPoint.EAST?1:-1));
        }
    }

    /**
     * @param sentenceReader
     *            The reader that this receiver will listen to for those NMEA sentences of interest for the construction
     *            of wind fixes
     */
    public NMEAWindReceiverImpl(SentenceReader sentenceReader) {
        super();
        utcCalendar = new GregorianCalendar();
        utcCalendar.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
        this.listeners = new ConcurrentHashMap<>();
        this.trueWind = new DynamicTrackImpl<>("trueWind in "+getClass().getName());
        this.apparentWind = new DynamicTrackImpl<>("apparentWind in "+getClass().getName());
        this.sensorPositions = new DynamicTrackImpl<>("measurementPositions in "+getClass().getName());
        this.sensorSpeeds = new DynamicTrackImpl<>("sensorSpeeds in "+getClass().getName());
        this.trueHeadings = new DynamicTrackImpl<>("trueHeadings in "+getClass().getName());
        sentenceReader.addSentenceListener(new MWVSentenceListener(), SentenceId.MWV);
        sentenceReader.addSentenceListener(new MWDSentenceListener(), SentenceId.MWD);
        sentenceReader.addSentenceListener(new VWRSentenceListener(), SentenceId.VWR);
        sentenceReader.addSentenceListener(new MDASentenceListener(), SentenceId.MDA);
        sentenceReader.addSentenceListener(new VTGSentenceListener(), SentenceId.VTG);
        sentenceReader.addSentenceListener(new HeadingSentenceListener());
        sentenceReader.addSentenceListener(new DateListener());
        sentenceReader.addSentenceListener(new TimeListener());
        sentenceReader.addSentenceListener(new PositionListener());
    }
    
    private Speed createSpeed(Units speedUnit, double amount) {
        final Speed result;
        switch (speedUnit) {
        case KMH:
            result = new KilometersPerHourSpeedImpl(amount);
            break;
        case KNOT:
            result = new KnotSpeedImpl(amount);
            break;
        case METER:
            result = new MeterPerSecondSpeedImpl(amount);
            break;
        default:
            throw new RuntimeException("Unknown speed unit: "+speedUnit);
        }
        return result;
    }
    
    /**
     * To be called when new true wind evidence has been received for {@code timePoint}. Creates a {@link Wind} fix when
     * enough data is available, in particular position data. If this is the case, the wind fix generated will be passed
     * to {@link #notifyListeners(Wind)}.
     */
    private void tryToCreateWindFixFromTrueWind(TimedSpeedWithBearing trueWind) {
        final com.sap.sse.common.Position position = getPosition(trueWind.getTimePoint());
        if (position != null) {
            final Wind wind = new WindImpl(position, trueWind.getTimePoint(), trueWind);
            notifyListeners(wind);
        }
    }

    /**
     * To be called when new apparent wind evidence has been received for {@code timePoint}. Creates a {@link Wind} fix
     * when enough data is available, such as motion and heading data required to turn the apparent wind fix into a true
     * wind fix, as well as position data, motion and heading. If this is the case, the wind fix generated will be
     * passed to {@link #notifyListeners(Wind)}.
     */
    private void tryToCreateWindFixFromApparentWind(TimedSpeedWithBearing apparentWind) {
        final com.sap.sse.common.Position position = getPosition(apparentWind.getTimePoint());
        if (position != null) {
            final SpeedWithBearing sensorSpeed = sensorSpeeds.getInterpolatedValue(getLastTimePoint(), fix->new ScalableSpeedWithBearing(fix));
            if (sensorSpeed != null) {
                final Bearing trueHeading = trueHeadings.getInterpolatedValue(getLastTimePoint(), fix->new ScalableBearing(fix));
                if (trueHeading != null) {
                    final SpeedWithBearing apparentWindTrue = new KnotSpeedWithBearingImpl(apparentWind.getKnots(), apparentWind.getBearing().add(trueHeading));
                    final SpeedWithBearing trueWind = apparentWindTrue.add(sensorSpeed); // this subtracts the induced wind which is the sensorSpeed reversed
                    final Wind wind = new WindImpl(position, apparentWind.getTimePoint(), trueWind);
                    notifyListeners(wind);
                }
            }
        }
    }

    /**
     * Based on the sequence of messages received from the reader, a "current" time point is approximated whenever a
     * {@link TimeSentence} or {@link DateSentence} or some time zone information as in a {@link ZDASentence} is
     * received. Several messages that are of interest for wind fix construction do not carry their own time stamp
     * information. For those, the time point inferred from the last {@link TimeSentence} is used which is what this
     * method returns.
     * 
     * @return the last {@link TimePoint} inferred from the combination of {@link TimeSentence}, {@link DateSentence}
     *         and {@link ZDASentence}, or {@code null} in case not enough information about timing has been received
     *         yet.
     */
    private TimePoint getLastTimePoint() {
        final TimePoint result;
        if (dateReceived && timeReceived) {
            result = new MillisecondsTimePoint(utcCalendar.getTime());
        } else {
            result = null;
        }
        return result;
    }

    private com.sap.sse.common.Position getPosition(TimePoint timePoint) {
        return sensorPositions.getInterpolatedValue(timePoint, gpsFix->new ScalablePosition(gpsFix.getPosition()));
    }

    @Override
    public void addWindListener(WindListener listener) {
        listeners.put(listener, listener);
    }

    @Override
    public void removeWindListener(WindListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Wind wind) {
        for (WindListener listener : listeners.values()) {
            listener.windDataReceived(wind);
        }
    }
}
