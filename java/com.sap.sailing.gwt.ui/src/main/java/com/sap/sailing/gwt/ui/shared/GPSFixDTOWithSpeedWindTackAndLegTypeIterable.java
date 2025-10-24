package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

import com.google.gwt.core.shared.GwtIncompatible;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.core.com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegTypeIterable_CustomFieldSerializer;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.gwt.ui.server.SailingServiceImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Instances of this class can produce a sequence of {@link GPSFixDTOWithSpeedWindTackAndLegType} objects. Either this
 * sequence is produced by delegating to an {@link Iterable} stored in a field ({@link #list}), or this is a proxy
 * object equipped only with the parameters needed to construct the sequence on the fly.
 * <p>
 * 
 * The server side uses {@code @GwtIncompatible} fields and types and will use the proxy parameters and not hold the
 * full list of {@link GPSFixDTOWithSpeedWindTackAndLegType} objects. This will save memory on the server side (see also
 * <a href=
 * "https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5077">https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5077</a>)
 * because the DTOs will be short-lived, and the serialization stream writer used to send the object to the client will
 * only have to hold on to the small proxy object in its object table and not all the DTOs constructed on the fly. The
 * design goal is to preserve memory by only actually producing the {@link GPSFixDTOWithSpeedWindTackAndLegType} objects
 * during serialization into primitive values on the server side, on the fly, and then again
 * when de-serializing on the client. All fields are {@code transient}, and a custom field serializer is used for a
 * highly proprietary, very compact and memory-conserving serialization process.
 * <p>
 * 
 * On the client side it is expected that the custom field serializer assembles a {@link List} of the
 * {@link GPSFixDTOWithSpeedWindTackAndLegType} objects which is then available for quick operations such as
 * {@link #last}. On the client side, all fixes will be needed by the map anyhow, so memory savings by any form of
 * streaming are not to be expected.
 * <p>
 * 
 * @see GPSFixDTOWithSpeedWindTackAndLegTypeIterable_CustomFieldSerializer
 * @author Axel Uhl (d043530)
 *
 */
public class GPSFixDTOWithSpeedWindTackAndLegTypeIterable implements IsSerializable, Iterable<GPSFixDTOWithSpeedWindTackAndLegType> {
    private static final Logger logger = Logger.getLogger(GPSFixDTOWithSpeedWindTackAndLegTypeIterable.class.getName());

    private final transient Iterable<GPSFixDTOWithSpeedWindTackAndLegType> list;
    @GwtIncompatible
    private transient Competitor competitor;
    @GwtIncompatible
    private transient SailingServiceImpl sailingService;
    @GwtIncompatible
    private transient TrackedRace trackedRace;
    @GwtIncompatible
    private transient DetailType detailType;
    @GwtIncompatible
    private transient GPSFixTrack<Competitor, GPSFixMoving> track;
    @GwtIncompatible
    private transient TimePoint fromTimePoint;
    @GwtIncompatible
    private transient TimePoint toTimePointExcluding;
    @GwtIncompatible
    private transient boolean extrapolate;
    @GwtIncompatible
    private transient String leaderboardName;
    @GwtIncompatible
    private transient String leaderboardGroupName;
    @GwtIncompatible
    private transient UUID leaderboardGroupId;

    /**
     * Creates an instance with an explicit list of fixes
     */
    public GPSFixDTOWithSpeedWindTackAndLegTypeIterable(Iterable<GPSFixDTOWithSpeedWindTackAndLegType> list) {
        assert list != null;
        this.list = list;
    }
    
    /**
     * Creates a proxy instance where the iterator produces the DTOs on the fly based on the proxy parameters passed
     * here.
     */
    @GwtIncompatible
    public GPSFixDTOWithSpeedWindTackAndLegTypeIterable(Competitor competitor,
            SailingServiceImpl sailingService, TrackedRace trackedRace, DetailType detailType,
            GPSFixTrack<Competitor, GPSFixMoving> track, TimePoint fromTimePoint, TimePoint toTimePointExcluding, boolean extrapolate,
            String leaderboardName, String leaderboardGroupName, UUID leaderboardGroupId) {
        this.list = null;
        this.competitor = competitor;
        this.sailingService = sailingService;
        this.trackedRace = trackedRace;
        this.detailType = detailType;
        this.track = track;
        this.fromTimePoint = fromTimePoint;
        this.toTimePointExcluding = toTimePointExcluding;
        this.extrapolate = extrapolate;
        this.leaderboardName = leaderboardName;
        this.leaderboardGroupName = leaderboardGroupName;
        this.leaderboardGroupId = leaderboardGroupId;
    }

    public boolean isEmpty() {
        final boolean result;
        if (list != null) {
            result = Util.isEmpty(list);
        } else {
            result = iterator().hasNext();
        }
        return result;
    }

    @Override
    public Iterator<GPSFixDTOWithSpeedWindTackAndLegType> iterator() {
        final IteratorSelector selector;
        if (list != null) {
            selector = new IteratorSelector();
        } else {
            selector = new ProxyIteratorSelector();
        }
        return selector.getIterator();
    }

    /**
     * A little trick with regards to @GwtIncompatible and the {@link ProxyIterator} which cannot compile for GWT due to
     * its server-side dependencies: The default implementation of {@link #getIterator()} in this class assumes that
     * this is running on the GWT client side and therefore delivers the iterator based on the
     * {@link GPSFixDTOWithSpeedWindTackAndLegTypeIterable#list} field. A specialized class that itself is GWT
     * compatible then has an @Override of the {@link #getIterator()} method which is @GwtIncompatible and hence can
     * return an instance of {@link ProxyIterator}.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class IteratorSelector {
        Iterator<GPSFixDTOWithSpeedWindTackAndLegType> getIterator() {
            return list.iterator();
        }
    }
    
    private class ProxyIteratorSelector extends IteratorSelector {
        @Override
        @GwtIncompatible
        Iterator<GPSFixDTOWithSpeedWindTackAndLegType> getIterator() {
            return new ProxyIterator();
        }
    }
    
    /**
     * Assuming that this iterable is in "proxy" state ({@link GPSFixDTOWithSpeedWindTackAndLegTypeIterable#list} being {@code null}),
     * computes the fixes on the fly based on the proxy parameters.
     * 
     * @author Axel Uhl (d043530)
     */
    @GwtIncompatible
    private class ProxyIterator implements Iterator<GPSFixDTOWithSpeedWindTackAndLegType> {
        private final Iterator<GPSFixMoving> fixIter;
        private final Set<GPSFixMoving> extrapolatedFixes;
        private final Map<Pair<Leg, TimePoint>, LegType> legTypeCache;
        private final ConcurrentHashMap<TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache> cachesByTimePoint;
        private GPSFixMoving lastFix;
        private GPSFixMoving fix;
        
        ProxyIterator() {
            legTypeCache = new HashMap<>();
            cachesByTimePoint = new ConcurrentHashMap<>();
            // copy the fixes into a list while holding the monitor; then release the monitor to avoid deadlocks
            // during wind estimations required for tack determination
            List<GPSFixMoving> fixes = new ArrayList<GPSFixMoving>();
            track.lockForRead();
            try {
                Iterator<GPSFixMoving> fixIter = track.getFixesIterator(fromTimePoint, /* inclusive */true);
                while (fixIter.hasNext()) {
                    GPSFixMoving fix = fixIter.next();
                    if (fix.getTimePoint().before(toTimePointExcluding) ||
                            (extrapolate && fix.getTimePoint().equals(toTimePointExcluding))) {
                        logger.finest(()->""+competitor.getName()+": " + fix);
                        fixes.add(fix);
                    } else {
                        break;
                    }
                }
            } finally {
                track.unlockAfterRead();
            }
            if (fixes.isEmpty()) {
                // then there was no (smoothened) fix between fromTimePoint and toTimePointExcluding; estimate...
                TimePoint middle = new MillisecondsTimePoint((toTimePointExcluding.asMillis()+fromTimePoint.asMillis())/2);
                Position estimatedPosition = track.getEstimatedPosition(middle, extrapolate);
                SpeedWithBearing estimatedSpeed = track.getEstimatedSpeed(middle);
                if (estimatedPosition != null && estimatedSpeed != null) {
                    GPSFixMoving estimatedFix = new GPSFixMovingImpl(estimatedPosition, middle, estimatedSpeed, /* optionalTrueHeading */ null); // TODO bug5970: could try to estimate heading from neighboring fixes
                    if (logger.getLevel() != null && logger.getLevel().equals(Level.FINEST)) {
                        logger.finest(""+competitor.getName()+": " + estimatedFix+" (estimated)");
                    }
                    fixes.add(estimatedFix);
                    extrapolatedFixes = Collections.singleton(estimatedFix);
                } else {
                    extrapolatedFixes = Collections.emptySet();
                }
            } else {
                extrapolatedFixes = Collections.emptySet();
            }
            fixIter = fixes.iterator();
            if (fixIter.hasNext()) {
                fix = fixIter.next();
            } else {
                fix = null;
            }
        }
        
        @Override
        public boolean hasNext() {
            return (fix != null && (fix.getTimePoint().before(toTimePointExcluding) ||
                    (fix.getTimePoint().equals(toTimePointExcluding) && toTimePointExcluding.equals(fromTimePoint))))
                || isExtrapolate();
        }

        @Override
        public GPSFixDTOWithSpeedWindTackAndLegType next() {
            final GPSFixDTOWithSpeedWindTackAndLegType result;
            if (fix != null) {
                final Wind wind = trackedRace.getWind(fix.getPosition(), fix.getTimePoint());
                final SpeedWithBearing estimatedSpeed = track.getEstimatedSpeed(fix.getTimePoint());
                Tack tack = wind == null? null : trackedRace.getTack(estimatedSpeed, wind, fix.getTimePoint());
                final TrackedLegOfCompetitor trackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor,
                        fix.getTimePoint());
                LegType legType;
                if (trackedLegOfCompetitor != null && trackedLegOfCompetitor.getLeg() != null) {
                    TimePoint quantifiedTimePoint = quantifyTimePointWithResolution(fix.getTimePoint(), /* resolutionInMilliseconds */60000);
                    Pair<Leg, TimePoint> cacheKey = new Pair<Leg, TimePoint>(trackedLegOfCompetitor.getLeg(), quantifiedTimePoint);
                    legType = legTypeCache.get(cacheKey);
                    if (legType == null) {
                        try {
                            legType = trackedRace.getTrackedLeg(trackedLegOfCompetitor.getLeg()).getLegType(fix.getTimePoint());
                            legTypeCache.put(cacheKey, legType);
                        } catch (NoWindException nwe) {
                            // without wind, leave the leg type null, meaning "unknown"
                            legType = null;
                        }
                    }
                } else {
                    legType = null;
                }
                WindDTO windDTO = wind == null ? null : sailingService.createWindDTOFromAlreadyAveraged(wind, toTimePointExcluding);
                Double detailValue = null;
                if (detailType != null) {
                    MillisecondsTimePoint time = new MillisecondsTimePoint(fix.getTimePoint().asMillis());
                    WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = cachesByTimePoint.get(time);
                    if (cache == null) {
                        cache = new LeaderboardDTOCalculationReuseCache(time);
                        cachesByTimePoint.put(time, cache);
                    }
                    try {
                        detailValue = sailingService.getCompetitorRaceDataEntry(detailType, trackedRace, competitor,
                                fix.getTimePoint(), leaderboardGroupName, leaderboardGroupId, leaderboardName, cache);
                    } catch (NoWindException | NotEnoughDataHasBeenAddedException | MaxIterationsExceededException | FunctionEvaluationException nwe) {
                        detailValue = null;
                    }
                }
                result = sailingService.createGPSFixDTO(fix, estimatedSpeed, fix.getOptionalTrueHeading(), windDTO,
                        tack, legType, /* extrapolate */ extrapolatedFixes.contains(fix), detailValue);
            } else if (isExtrapolate()) {
                final TrackedLegOfCompetitor trackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor, lastFix.getTimePoint());
                Position position = track.getEstimatedPosition(toTimePointExcluding, extrapolate);
                Wind wind2 = trackedRace.getWind(position, toTimePointExcluding);
                SpeedWithBearing estimatedSpeed2 = track.getEstimatedSpeed(toTimePointExcluding);
                Tack tack2 = wind2 == null ? null : trackedRace.getTack(estimatedSpeed2, wind2, toTimePointExcluding);
                LegType legType2;
                if (trackedLegOfCompetitor != null && trackedLegOfCompetitor.getLeg() != null) {
                    TimePoint quantifiedTimePoint = quantifyTimePointWithResolution(
                            lastFix.getTimePoint(), /* resolutionInMilliseconds */
                            60000);
                    Pair<Leg, TimePoint> cacheKey = new Pair<Leg, TimePoint>(
                            trackedLegOfCompetitor.getLeg(), quantifiedTimePoint);
                    legType2 = legTypeCache.get(cacheKey);
                    if (legType2 == null) {
                        try {
                            legType2 = trackedRace.getTrackedLeg(trackedLegOfCompetitor.getLeg()).getLegType(lastFix.getTimePoint());
                            legTypeCache.put(cacheKey, legType2);
                        } catch (NoWindException nwe) {
                            // no wind information; leave leg type null, meaning "unknown"
                            legType2 = null;
                        }
                    }
                } else {
                    legType2 = null;
                }
                WindDTO windDTO2 = wind2 == null ? null : sailingService.createWindDTOFromAlreadyAveraged(wind2, toTimePointExcluding);
                result = new GPSFixDTOWithSpeedWindTackAndLegType(
                        toTimePointExcluding.asDate(), position==null?null:position,
                        estimatedSpeed2==null?null:sailingService.createSpeedWithBearingDTO(estimatedSpeed2), /* optionalTrueHeading */ null, // no heading for extrapolated fixes
                        windDTO2, tack2, legType2, /* extrapolated */ true);
            } else {
                throw new NoSuchElementException();
            }
            lastFix = fix; // will also set lastFix to null after extrapolating one last fix
            if (fixIter.hasNext()) {
                fix = fixIter.next();
            } else {
                fix = null;
            }
            return result;
        }
        
        private boolean isExtrapolate() {
            return lastFix != null && !lastFix.getTimePoint().equals(toTimePointExcluding) && extrapolate;
        }
    }

    private TimePoint quantifyTimePointWithResolution(TimePoint timePoint, long resolutionInMilliseconds) {
        return new MillisecondsTimePoint(timePoint.asMillis() / resolutionInMilliseconds * resolutionInMilliseconds);
    }

    public GPSFixDTOWithSpeedWindTackAndLegType last() {
        final GPSFixDTOWithSpeedWindTackAndLegType result;
        if (list != null) {
            result = Util.last(list);
        } else {
            result = Util.last(this);
        }
        return result;
    }
}
