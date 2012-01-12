package com.sap.sailing.domain.base.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixTrack;

/**
 * Implements the (Ramer)-Douglas-Peucker algorithm on a segment of a {@link GPSFixTrack} with a configurable distance
 * threshold.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class DouglasPeucker<ItemType, FixType extends GPSFix> {
    private final GPSFixTrack<ItemType, FixType> track;
    
    private final Executor executor;

    public DouglasPeucker(GPSFixTrack<ItemType, FixType> track) {
        this.track = track;
        executor = null;
    }
    
    /**
     * Allows clients to specify a non-default, optionally multi-threaded executor that will be used to perform the
     * high-effort computations.
     */
    public DouglasPeucker(GPSFixTrack<ItemType, FixType> track, Executor executor) {
        this.track = track;
        this.executor = executor;
    }

    private Pair<GPSFix, Distance> getFixWithGreatestCrossTrackErrorInInterval(TimePoint from, TimePoint to) {
    Distance maxDistance = Distance.NULL;
        FixType firstFixAtOrAfter = track.getFirstFixAtOrAfter(from);
        Pair<GPSFix, Distance> result = null;
        if (firstFixAtOrAfter != null) {
            final Position fromPosition = firstFixAtOrAfter.getPosition();
            FixType toFix = track.getLastFixAtOrBefore(to);
            final Bearing bearing = fromPosition.getBearingGreatCircle(toFix.getPosition());
            synchronized (track) {
                Iterator<FixType> fixIter = track.getFixesIterator(from, /* inclusive */false);
                if (executor != null) {
                    result = getFixWithGreatestCrossTrackErrorUsingExecutor(to, maxDistance, 
                            fromPosition, bearing, fixIter);
                } else {
                    // avoid the extra effort of dealing with futures and executors
                    FixType fixFurthestAway = null;
                    while (fixIter.hasNext()) {
                        final FixType fix = fixIter.next();
                        if (fix.getTimePoint().compareTo(to) > 0) {
                            break;
                        }
                        Distance crossTrackError = fix.getPosition().crossTrackError(fromPosition, bearing);
                        if (crossTrackError.compareTo(maxDistance) > 0) {
                            maxDistance = crossTrackError;
                            fixFurthestAway = fix;
                        }
                    }
                    result = new Pair<GPSFix, Distance>(fixFurthestAway, maxDistance);
                }
            }
        }
        return result;
    }

    private Pair<GPSFix, Distance> getFixWithGreatestCrossTrackErrorUsingExecutor(TimePoint to, Distance maxDistance,
            final Position fromPosition, final Bearing bearing, Iterator<FixType> fixIter) {
        FixType fixFurthestAway = null;
        Pair<GPSFix, Distance> result;
        Collection<Future<Pair<FixType, Distance>>> crossTrackErrorFutures = new HashSet<Future<Pair<FixType, Distance>>>();
        while (fixIter.hasNext()) {
            final FixType fix = fixIter.next();
            if (fix.getTimePoint().compareTo(to) > 0) {
                break;
            }
            RunnableFuture<Pair<FixType, Distance>> crossTrackErrorFuture = new FutureTask<Pair<FixType, Distance>>(
                    new Callable<Pair<FixType, Distance>>() {
                        @Override
                        public Pair<FixType, Distance> call() throws Exception {
                            return new Pair<FixType, Distance>(fix, fix.getPosition().crossTrackError(
                                    fromPosition, bearing));
                        }
                    });
            executor.execute(crossTrackErrorFuture);
            crossTrackErrorFutures.add(crossTrackErrorFuture);
        }
        for (Future<Pair<FixType, Distance>> crossTrackErrorFuture : crossTrackErrorFutures) {
            Pair<FixType, Distance> fixAndCrossTrackError;
            try {
                fixAndCrossTrackError = crossTrackErrorFuture.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (fixAndCrossTrackError.getB().compareTo(maxDistance) > 0) {
                maxDistance = fixAndCrossTrackError.getB();
                fixFurthestAway = fixAndCrossTrackError.getA();
            }
        }
        result = new Pair<GPSFix, Distance>(fixFurthestAway, maxDistance);
        return result;
    }
    
    /**
     * @param maxDistance
     *            maximum error of the approximation; all fixes or the track are closer than this distance to the fix
     *            sequence returned
     * @return a sequence of fixes from the {@link #track} starting <em>after</em> time point <code>from</code> and
     *         including the last fix at or before time <code>to</code> that approximates the {@link #track} such that
     *         the maximum distance of any fix on the {@link #track} to the approximation is less than
     *         <code>maxDistance</code>. Always returns a non-<code>null</code> list which may, however, be empty.
     *         Note, that if the fixes contain bearing information, the bearing is not the bearing leading to the
     *         next fix of the approximation but the bearing the tracked item had at the point in time of the
     *         approximation fix.
     */
    public List<FixType> approximate(Distance maxDistance, TimePoint from, TimePoint to) {
        List<FixType> resultWithoutFirstFix = approximateWithoutFirst(maxDistance, from, to);
        List<FixType> result = new ArrayList<FixType>(resultWithoutFirstFix.size() + 1);
        FixType firstFixAtOrAfter = track.getFirstFixAtOrAfter(from);
        if (firstFixAtOrAfter != null) {
            result.add(firstFixAtOrAfter);
        }
        for (FixType f : resultWithoutFirstFix) {
            result.add(f);
        }
        return result;
    }
    
    /**
     * @return a non-<code>null</code> list which may be empty
     */
    private List<FixType> approximateWithoutFirst(Distance maxDistance, TimePoint from, TimePoint to) {
        List<FixType> result;
        Pair<GPSFix, Distance> fixAndDistance = getFixWithGreatestCrossTrackErrorInInterval(from, to);
        if (fixAndDistance == null || fixAndDistance.getB().compareTo(maxDistance) < 0) {
            // reached desired accuracy for interval from..to
            FixType lastFixAtOrBefore = track.getLastFixAtOrBefore(to);
            if (lastFixAtOrBefore == null || lastFixAtOrBefore.getTimePoint().compareTo(from) < 0) {
                result = Collections.emptyList();
            } else {
                result = Collections.singletonList(lastFixAtOrBefore);
            }
        } else {
            List<FixType> left = approximateWithoutFirst(maxDistance, from, fixAndDistance.getA().getTimePoint());
            List<FixType> right = approximateWithoutFirst(maxDistance, fixAndDistance.getA().getTimePoint(), to);
            result = new ArrayList<FixType>(left.size()+right.size());
            for (FixType fixFromLeft : left) {
                result.add(fixFromLeft);
            }
            for (FixType fixFromRight : right) {
                result.add(fixFromRight);
            }
        }
        return result;
    }
}
