package com.sap.sailing.domain.tracking;

import java.util.Iterator;

import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.shared.tracking.MappedTrack;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;

/**
 * A track records the {@link GPSFix}es received for an object of type
 * <code>ItemType</code>. It allows clients to ask for a position at any given
 * {@link TimePoint} and interpolates the fixed positions to obtain an estimate
 * of the position at the time requested. The method used to interpolate may vary
 * between different implementation classes. The default implementation is to
 * fall back to the last fix at or before the time point requested.
 * 
 * @author Axel Uhl (d043530)
 * 
 * @param <ItemType>
 */
public interface GPSFixTrack<ItemType, FixType extends GPSFix> extends MappedTrack<ItemType, FixType> {
    static final long DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_SPEED = 10000; // makes for a 5s half-side interval
    static final Speed DEFAULT_MAX_SPEED_FOR_SMOOTHING = new KnotSpeedImpl(40);

    /**
     * A listener is notified whenever a new fix is added to this track
     */
    void addListener(GPSTrackListener<ItemType, FixType> listener);
    
    void removeListener(GPSTrackListener<ItemType, FixType> listener);
    
    /**
     * Computes the distance traveled on the smoothened track between the
     * {@link #getEstimatedPosition(TimePoint, boolean) estimated positions} at <code>from</code> and <code>to</code>.
     */
    Distance getDistanceTraveled(TimePoint from, TimePoint to);

    /**
     * Gets the longest duration between two smoothened GPS-fixes contained within the provided period within the track.
     * Returns {@code Duration.NULL} if there are less than 2 fixes contained within the track.
     */
    Duration getLongestIntervalBetweenTwoFixes(TimePoint from, TimePoint to);

    /**
     * Computes the distance traveled on the raw, unsmoothened track between the
     * {@link #getEstimatedPosition(TimePoint, boolean) estimated positions} at <code>from</code> and <code>to</code>.
     * This includes all zig-zagging caused by imprecise GPS measurements.
     */
    Distance getRawDistanceTraveled(TimePoint from, TimePoint to);

    /**
     * If the time point lies before the first fix recorded by this track, the first fix is returned, or
     * <code>null</code> if no fix at all exists yet. If a time point between two fixes of this track is chosen, the
     * path between the two fixes is interpolated along a great circle. The speed is estimated by using the time points
     * of the two fixes between which <code>timePoint</code> lies. If a time point after the last fix recorded so far is
     * provided and <code>extrapolate</code> is <code>true</code>, the {@link SpeedWithBearing} at the last point is
     * estimated, either from looking at the last two fixes in the track or, if only one fix exists, by using that
     * single fix's speed information if present (must be a {@link GPSFixMoving} for that). If <code>extrapolate</code>
     * is <code>false</code> and the <code>timePoint</code> is after the latest recorded fix, the latest recorded fix is
     * returned. If extrapolation is requested but not enough information is present to perform an estimation,
     * <code>null</code> is returned.
     * 
     * @param extrapolate
     *            only if <code>true</code> will the value for <code>timePoint</code> be computed by extrapolating
     *            beyond the time extent of this track; otherwise, the value closest to <code>timePoint</code> will be
     *            used instead.
     */
    Position getEstimatedPosition(TimePoint timePoint, boolean extrapolate);
    
    /**
     * Same as {@link #getEstimatedPosition(TimePoint, boolean)}, only that it works on the raw track
     * that has not been subject to smoothening.
     */
    Position getEstimatedRawPosition(TimePoint timePoint, boolean extrapolate);
    
    /**
     * @return <code>null</code> if <code>from</code> is before <code>to</code>
     */
    Util.Pair<FixType, Speed> getMaximumSpeedOverGround(TimePoint from, TimePoint to);

    /**
     * Using an averaging / smoothening algorithm, computes the estimated speed determined
     * by the GPS fixes in this track at time point <code>at</code>.
     */
    SpeedWithBearing getEstimatedSpeed(TimePoint at);
    
    /**
     * Estimates the tracked item's speed/bearing, returning the result as a value with confidence. The confidences of
     * the individual fixes contributing to the estimation are computed using the <code>weigher</code>. The result's
     * confidence is the average confidence of the fixes used. While this is probably not a mathematically meaningful
     * confidence value, it helps in comparing confidences of different estimations relative to each other.
     */
    SpeedWithBearingWithConfidence<TimePoint> getEstimatedSpeed(TimePoint at, Weigher<TimePoint> weigher);

    /**
     * Same as {@link #getEstimatedSpeed(TimePoint, Weigher)}, but using the raw fixes.
     */
    SpeedWithBearingWithConfidence<TimePoint> getRawEstimatedSpeed(TimePoint at, Weigher<TimePoint> weigher);

    long getMillisecondsOverWhichToAverageSpeed();
    
    /**
     * If and only if the {@link #getFixesIterator(TimePoint, boolean) smoothened track} has a direction change of at
     * least <code>minimumDegreeDifference</code> degrees within {@link #getMillisecondsOverWhichToAverageSpeed()}
     * milliseconds around the <code>at</code> time point, this method returns <code>true</code>.
     */
    boolean hasDirectionChange(TimePoint at, double minimumDegreeDifference);

    SpeedWithBearing getRawEstimatedSpeed(TimePoint at);

    /**
     * Finds out which position estimation time interval has been affected by inserting <code>fix</code>.
     * 
     * @param fix
     *            assumed to already have been inserted into this track, but it's OK to pass a fix that's not in the
     *            track yet
     * 
     * @return if no fix before <code>fix</code> is found, the first component is the beginning of the epoch (
     *         <code>new MillisecondsTimePoint(0)</code>). If no fix after <code>fix</code> is found, the second
     *         component is the end of time (<code>new MillisecondsTimePoint(Long.MAX_VALUE)</code>).
     */
    TimeRange getEstimatedPositionTimePeriodAffectedBy(GPSFix fix);

    /**
     * Same as {@link #getEstimatedPosition(TimePoint, boolean)}, but produces an iterator for all {@link Timed} objects
     * in <code>timeds</code> which need to be provided in ascending order of their {@link Timed#getTimePoint() time
     * points}. This will save lookup effort of the adjacent fixes, each requiring a logarithmic binary search.<p>
     * 
     * Callers need to hold the read lock when calling this method until done with the iterator over the iterator returned, as in
     * <pre>
     *          track.lockForRead();
     *          try {
     *              Iterator<Position> pIter = track.getEstimatedPositions(timeds, true);
     *              while (pIter.hasNext()) {
     *                  // ...do something with pIter.next()...
     *              }
     *          } finally {
     *              track.unlockAfterRead();
     *          }
     * </pre>
     * 
     * @param timeds
     *            required to be in ascending order
     * @param extrapolate
     *            see {@link #getEstimatedPosition(TimePoint, boolean)}
     * @return an iterator of the positions, in the same order as the <code>timeds</code> are provided
     */
    Iterator<Position> getEstimatedPositions(Iterable<Timed> timeds, boolean extrapolate);

    /**
     * When a {@link TrackedRace} moves into state {@link TrackedRaceStatusEnum#LOADING}, it shall call
     * this method on all its tracks to allow them to skip validity cache updates which, when done at massive
     * scale, are too expensive because they keep invalidating neighbors' validity and need some time to
     * find those neighbors. When leading state LOADING, {@link #resumeValidityAndMaxSpeedCaching()} must be called.
     */
    void suspendValidityAndMaxSpeedCaching();

    /**
     * When a {@link TrackedRace} moves out of state {@link TrackedRaceStatusEnum#LOADING}, it shall call
     * this method on all its tracks to allow them to invalidate all validity caching so far in order to
     * have everything re-calculated when needed.
     */
    void resumeValidityAndMaxSpeedCaching();
    
    /**
     * Gets a list of speed with bearing steps considering the provided time range. The bearings are retrieved by means
     * of {@link GPSFixTrack#getEstimatedSpeed(TimePoint)}. The steps are sampled at time points of non-raw GPS fixes.
     * When there is no non-raw fix contained at {@code fromTimePoint}, the time point of the first step will be the
     * time point of the first non-raw fix before {@code fromTimePoint}. Analogously, when there is no non-raw fix
     * contained at {@code toTimePoint}, the last step's time point will be the first non-raw fix after
     * {@code toTimePoint}. The idea of this concept is to produce at least two steps as part of the result, in order to
     * provide the caller at least a {@code |totalCourseChangeAngleInDegrees > 0|} between the given time range.
     * 
     * @param fromTimePoint
     *            The from time point (inclusive) for resulting bearing steps
     * @param toTimePoint
     *            The to time point (inclusive) for resulting bearing steps
     * @return The list of bearings between the provided time range
     */
    SpeedWithBearingStepsIterable getSpeedWithBearingSteps(TimePoint fromTimePoint, TimePoint toTimePoint);

    /**
     * For a fix that doesn't necessarily need to be part of this track already decides whether this fix is considered
     * valid in the scope of this track. This means that if the fix is part of the track it will be returned in
     * {@link #getFixes()} if and only if this method returns {@code true}.
     */
    boolean isValid(FixType e);
}
