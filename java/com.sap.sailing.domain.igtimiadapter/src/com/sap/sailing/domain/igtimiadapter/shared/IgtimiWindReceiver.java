package com.sap.sailing.domain.igtimiadapter.shared;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalablePosition;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableSpeed;
import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiverAdapter;
import com.sap.sailing.domain.igtimiadapter.IgtimiWindListener;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWA;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWS;
import com.sap.sailing.domain.igtimiadapter.datatypes.BatteryLevel;
import com.sap.sailing.domain.igtimiadapter.datatypes.COG;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsLatLong;
import com.sap.sailing.domain.igtimiadapter.datatypes.HDG;
import com.sap.sailing.domain.igtimiadapter.datatypes.HDGM;
import com.sap.sailing.domain.igtimiadapter.datatypes.SOG;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sailing.domain.igtimiadapter.websocket.WebSocketConnectionManager;
import com.sap.sailing.domain.tracking.DynamicTrackWithRemove;
import com.sap.sailing.domain.tracking.WindListener;
import com.sap.sailing.domain.tracking.impl.DynamicTrackWithRemoveImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

/**
 * Receives Igtimi {@link Fix}es and tries to generate a {@link Wind} object from each {@link AWS} fix. For this to
 * work, the time-wise adjacent {@link AWA}, {@link HDG}/{@link HDGM} and {@link GpsLatLong} fixes are used. If only a
 * magnetic heading ({@link HDGM}) is available, the {@link DeclinationService} is used to map that to a true heading.
 * The true wind direction is determined by adding the boat speed vector onto the apparent wind vector.
 * <p>
 * 
 * Use the class by hooking it up to a {@link WebWocketConnectionManager} using
 * {@link WebSocketConnectionManager#addListener(BulkFixReceiver)} and {@link #addListener(WindListener) add} a
 * {@link WindListener} to this instance.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class IgtimiWindReceiver implements BulkFixReceiver {
    private static final Logger logger = Logger.getLogger(IgtimiWindReceiver.class.getName());
    private final ConcurrentMap<String, DynamicTrackWithRemove<AWA>> awaTracks;
    private final ConcurrentMap<String, DynamicTrackWithRemove<AWS>> awsTracks;
    private final ConcurrentMap<String, DynamicTrackWithRemove<GpsLatLong>> gpsTracks;
    private final ConcurrentMap<String, DynamicTrackWithRemove<COG>> cogTracks;
    private final ConcurrentMap<String, DynamicTrackWithRemove<SOG>> sogTracks;
    private final ConcurrentMap<String, DynamicTrackWithRemove<HDG>> hdgTracks;
    private final ConcurrentMap<String, DynamicTrackWithRemove<HDGM>> hdgmTracks;
    private final ConcurrentMap<String, DynamicTrackWithRemove<BatteryLevel>> batteryLevelTracks;
    private final FixReceiver receiver;
    private final DeclinationService declinationService;
    private final ConcurrentMap<IgtimiWindListener, IgtimiWindListener> listeners;
    
    private class FixReceiver extends IgtimiFixReceiverAdapter {
        @Override
        public void received(AWA fix) {
            getAwaTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }

        @Override
        public void received(AWS fix) {
            getAwsTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }

        @Override
        public void received(GpsLatLong fix) {
            getGpsTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }

        @Override
        public void received(COG fix) {
            getCogTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }

        @Override
        public void received(SOG fix) {
            getSogTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }

        @Override
        public void received(HDG fix) {
            getHdgTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }

        @Override
        public void received(HDGM fix) {
            getHdgmTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }

        @Override
        public void received(BatteryLevel fix) {
            getBatteryLevelTrack(fix.getSensor().getDeviceSerialNumber()).add(fix);
        }
        
    }

    public IgtimiWindReceiver(DeclinationService declinationService) {
        receiver = new FixReceiver();
        this.declinationService = declinationService;
        listeners = new ConcurrentHashMap<>();
        awaTracks = new ConcurrentHashMap<>();
        awsTracks = new ConcurrentHashMap<>();
        gpsTracks = new ConcurrentHashMap<>();
        cogTracks = new ConcurrentHashMap<>();
        sogTracks = new ConcurrentHashMap<>();
        hdgTracks = new ConcurrentHashMap<>();
        hdgmTracks = new ConcurrentHashMap<>();
        batteryLevelTracks = new ConcurrentHashMap<>();
    }
    
    private <T extends Fix> DynamicTrackWithRemove<T> getTrack(String deviceSerialNumber, Map<String, DynamicTrackWithRemove<T>> tracksByDeviceSerialNumber) {
        DynamicTrackWithRemove<T> result = tracksByDeviceSerialNumber.get(deviceSerialNumber);
        if (result == null) {
            result = new DynamicTrackWithRemoveImpl<T>("Track for Igtimi wind track for device "+deviceSerialNumber);
            tracksByDeviceSerialNumber.put(deviceSerialNumber, result);
        }
        return result;
    }
    
    /**
     * Called when a batch of fixes was received, e.g., after loading from stored data or from a live web socket receiver.
     * The fixes are pumped into the tracks by the superclass implementation. Then, for all new {@link AWS} fixes, a
     * call to {@link IgtimiWindReceiver#getWind} is performed.
     */
    @Override
    public void received(Iterable<Fix> fixes) {
        final List<AWS> awsFixes = new ArrayList<>();
        for (Fix fix : fixes) {
            fix.notify(receiver);
            fix.notify(new IgtimiFixReceiverAdapter() {
                @Override
                public void received(AWS fix) {
                    awsFixes.add(fix);
                }
            });
        }
        logger.finest("Received "+Util.size(awsFixes)+" wind fixes");
        boolean loggedWindFixGenerationProblem = false;
        for (AWS aws : awsFixes) {
            try {
                final Pair<Wind, Set<Fix>> windAndFixesUsed = getWind(aws.getTimePoint(), aws.getSensor().getDeviceSerialNumber());
                if (windAndFixesUsed.getA() != null) {
                    notifyListeners(windAndFixesUsed.getA(), windAndFixesUsed.getB(), aws.getSensor().getDeviceSerialNumber());
                } else {
                    if (!loggedWindFixGenerationProblem) {
                        logger.info("Not enough information to build a Wind fix out of data provided by sensor "+aws.getSensor()+
                                ". AWS received but most probably HDG or HDGM not received (yet) - check your compass.");
                        loggedWindFixGenerationProblem = true;
                    }
                }
            } catch (ClassNotFoundException | IOException | ParseException e) {
                logger.log(Level.INFO, "Exception while trying to construct Wind fix from Igtimi fix " + aws, e);
            }
        }
    }

    public void addListener(IgtimiWindListener listener) {
        listeners.put(listener, listener);
    }
    
    private void notifyListeners(Wind wind, Set<Fix> fixesUsed, String deviceSerialNumber) {
        for (IgtimiWindListener listener : listeners.keySet()) {
            listener.windDataReceived(wind, fixesUsed, deviceSerialNumber);
        }
    }
    
    /**
     * Returns a wind fix produced out of an AWS/AWA and other fixes, as well as the fixes used for this (in case of
     * interpolation between two fixes, the older one of the two is returned), including a {@link BatteryLevel} fix, if
     * any was found, although this did technically not contribute to the production of the {@link Wind} object.
     */
    private Pair<Wind, Set<Fix>> getWind(final TimePoint timePoint, String deviceSerialNumber) throws ClassNotFoundException, IOException, ParseException {
        final Wind result;
        final Set<Fix> fixesUsed = new HashSet<>();
        final DynamicTrackWithRemove<BatteryLevel> batteryLevelTrack = getBatteryLevelTrack(deviceSerialNumber);
        addFixUsedIfNotNull(batteryLevelTrack, timePoint, fixesUsed);
        final DynamicTrackWithRemove<AWA> awaTrack = getAwaTrack(deviceSerialNumber);
        final Bearing awaFrom = awaTrack.getInterpolatedValue(timePoint, a->new ScalableBearing(a.getApparentWindAngle()));
        addFixUsedIfNotNull(awaTrack, timePoint, fixesUsed);
        final Bearing awa = awaFrom==null?null:awaFrom.reverse();
        final DynamicTrackWithRemove<AWS> awsTrack = getAwsTrack(deviceSerialNumber);
        final Speed aws = awsTrack.getInterpolatedValue(timePoint, a->new ScalableSpeed(a.getApparentWindSpeed()));
        addFixUsedIfNotNull(awsTrack, timePoint, fixesUsed);
        final DynamicTrackWithRemove<GpsLatLong> gpsTrack = getGpsTrack(deviceSerialNumber);
        final Position pos = gpsTrack.getInterpolatedValue(timePoint, g->new ScalablePosition(g.getPosition()));
        addFixUsedIfNotNull(gpsTrack, timePoint, fixesUsed);
        if (pos != null) {
            final Bearing heading = getHeading(timePoint, deviceSerialNumber, pos, fixesUsed);
            if (awa != null && aws != null && heading != null) {
                final Bearing apparentWindDirection = heading.add(awa);
                final SpeedWithBearing apparentWindSpeedWithDirection = new KnotSpeedWithBearingImpl(aws.getKnots(), apparentWindDirection);
                /*
                 * Hint from Brent Russell from Igtimi, at 2013-12-05 on the question whether to use GpsLatLong to
                 * improve precision of boat speed / course over SOG/COG measurements:
                 * 
                 * "Personally I would use COG/SOG exclusively, and if unhappy with the result add a small amount of
                 * smoothing and consider dropping samples as outliers if they cause a SOG discontinuity. The latter
                 * might happen as a satellite is dropped/acquired - and I'd expect to see a time correlated position
                 * jump as well. Probably not though a direction/speed correlation :)
                 * 
                 * All our GPS systems are using Doppler to calculate COG/SOG and this should be the most accurate
                 * measure. I don't believe that delta position really adds any more "truth" to the measurement of
                 * physical reality, if that makes sense. I'd trust d-p even less at low speeds, where you'll see the
                 * most disagreement. Also there is a significant quantisation noise error in the d-p calculations from
                 * the GPS resolution too, so you'd have to smooth it before averaging - possibly in a speed dependent
                 * way.
                 * 
                 * Remember that Doppler COG/SOG is using the same raw satellite measurements that are being used to
                 * calculate position, just the algorithm is different. I suspect that merging the two might be, in
                 * practice, just a slightly indirect way of averaging. If you like the central limit theorem in action
                 * over the set of algorithms!
                 * 
                 * So again, my personal preference would be to work with the data that should be the most accurate
                 * (COG/SOG) and consider algorithms that handle smoothing of that data best."
                 */
                final DynamicTrackWithRemove<SOG> sogTrack = getSogTrack(deviceSerialNumber);
                final Speed sog = sogTrack.getInterpolatedValue(timePoint, s->new ScalableSpeed(s.getSpeedOverGround()));
                addFixUsedIfNotNull(sogTrack, timePoint, fixesUsed);
                final DynamicTrackWithRemove<COG> cogTrack = getCogTrack(deviceSerialNumber);
                final Bearing cog = cogTrack.getInterpolatedValue(timePoint, c->new ScalableBearing(c.getCourseOverGround()));
                addFixUsedIfNotNull(cogTrack, timePoint, fixesUsed);
                if (sog != null && cog != null) {
                    SpeedWithBearing sogCog = new KnotSpeedWithBearingImpl(sog.getKnots(), cog);
                    SpeedWithBearing trueWindSpeedAndDirection = apparentWindSpeedWithDirection.add(sogCog);
                    result = new WindImpl(pos, timePoint, trueWindSpeedAndDirection);
                } else {
                    result = null;
                }
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return new Pair<>(result, fixesUsed);
    }

    /**
     * Searches for the last fix at or before {@code timePoint} in the given {@link trackToCleanUp}. If such a fix is
     * found, adds the given fix to {@code fixesUsed} and clears all fixes from the {@code trackToCleanUp} that are
     * earlier than the {@code timePoint} given. This in particular preserves at least one live fix which will "hide"
     * older, e.g., buffered fixes that may be received when a WindBot device sends them out-of-order. For old, buffered
     * fixes this may still mean that could be dropped prior to being converted into a {@link Wind} object with an older
     * time stamp, but usually---and particularly with buffered data from previous days---this shouldn't be a problem.
     * Also, old data can still be imported at a later point in time. 
     * <p>
     * 
     * This assumes that the {@code fix} passed has been "consumed" now, and earlier fixes will not be relevant anymore.
     * We don't expect significant out-of-order delivery of fixes here.
     */
    private <FixType extends Fix> void addFixUsedIfNotNull(DynamicTrackWithRemove<FixType> trackToCleanUp, TimePoint timePoint, Set<Fix> fixesUsed) {
        final FixType fix = trackToCleanUp.getLastFixAtOrBefore(timePoint);
        if (fix != null) {
            fixesUsed.add(fix);
            trackToCleanUp.removeAllUpToExcluding(fix);
        }
    }

    private Bearing getHeading(TimePoint timePoint, String deviceSerialNumber, Position position, Set<Fix> fixesUsed) throws ClassNotFoundException, IOException, ParseException {
        final Bearing trueHeading;
        final DynamicTrackWithRemove<HDG> hdgTrack = getHdgTrack(deviceSerialNumber);
        final DynamicTrackWithRemove<HDGM> hdgmTrack = getHdgmTrack(deviceSerialNumber);
        Bearing hdg = hdgTrack.getInterpolatedValue(timePoint, h->new ScalableBearing(h.getTrueHeading()));
        if (hdg != null) {
            trueHeading = hdg;
        } else {
            final Bearing hdgm = hdgmTrack.getInterpolatedValue(timePoint, h->new ScalableBearing(h.getMagneticHeading()));
            if (hdgm != null) {
                if (declinationService == null) {
                    trueHeading = hdgm;
                } else {
                    try {
                        Declination declination = declinationService.getDeclination(timePoint, position, /* timeoutForOnlineFetchInMilliseconds 5s */ 5000);
                        trueHeading = hdgm.add(declination.getBearingCorrectedTo(timePoint));
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Correction of declination was requested but unsuccessful. Can't correct heading "+
                                hdgm+"@"+timePoint+" by declination. Forwarding exception.");
                        throw e;
                    }
                }
            } else {
                trueHeading = null;
            }
        }
        addFixUsedIfNotNull(hdgTrack, timePoint, fixesUsed);
        addFixUsedIfNotNull(hdgmTrack, timePoint, fixesUsed);
        return trueHeading;
    }

    private DynamicTrackWithRemove<AWA> getAwaTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, awaTracks);
    }

    private DynamicTrackWithRemove<AWS> getAwsTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, awsTracks);
    }

    private DynamicTrackWithRemove<GpsLatLong> getGpsTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, gpsTracks);
    }

    private DynamicTrackWithRemove<COG> getCogTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, cogTracks);
    }

    private DynamicTrackWithRemove<SOG> getSogTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, sogTracks);
    }

    private DynamicTrackWithRemove<HDG> getHdgTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, hdgTracks);
    }

    private DynamicTrackWithRemove<HDGM> getHdgmTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, hdgmTracks);
    }
    
    private DynamicTrackWithRemove<BatteryLevel> getBatteryLevelTrack(String deviceSerialNumber) {
        return getTrack(deviceSerialNumber, batteryLevelTracks);
    }

    /**
     * Tells the set of types that this wind receiver is interested in. Can be used to subscribe for fixes from
     * resource data. See {@link IgtimiConnection#getResourceData(TimePoint, TimePoint, Iterable, Type...)}.
     */
    public Type[] getFixTypes() {
        return new Type[] { Type.AWA, Type.AWS, Type.HDG, Type.HDGM, Type.gps_latlong, Type.COG, Type.SOG };
    }
}
