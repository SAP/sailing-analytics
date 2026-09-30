package com.sap.sailing.domain.leaderboard.caching;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.math.ArgumentOutsideDomainException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveCourseImpl;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.leaderboard.impl.AbstractSimpleLeaderboardImpl;
import com.sap.sailing.domain.orc.ORCPerformanceCurve;
import com.sap.sailing.domain.orc.ORCPerformanceCurveCache;
import com.sap.sailing.domain.orc.impl.AbstractORCPerformanceCurveTwaLegAdapter;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveByImpliedWindRankingMetric;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * A cache structure that is used for a single call to
 * {@link AbstractSimpleLeaderboardImpl#computeDTO(com.sap.sse.common.TimePoint, java.util.Collection, boolean, boolean, com.sap.sailing.domain.tracking.TrackedRegattaRegistry, com.sap.sailing.domain.base.DomainFactory)}.
 * It is to be passed on to various query methods that may required common data that is expensive to compute and
 * depends on equal parameters, such as an equal time point. The underlying assumption is that during one leaderboard (re-)calculation cycle the
 * dynamic changes in the wind field and the mark positions can safely be ignored so that wind data for competitors and legs and the legs'
 * bearings only need to be calculated once.
 * <p>
 * 
 * This cache is equipped with the references necessary to compute the information if need be. The cache is thread safe. Note that it wouldn't be
 * a good idea to use {@link ThreadLocal}s for this because there is a lot of concurrency involved, and the {@link ThreadLocal} would have to be set
 * on each thread involved to be helpful which seems too complicated. 
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class LeaderboardDTOCalculationReuseCache implements WindLegTypeAndLegBearingAndORCPerformanceCurveCache {
    /**
     * The reference time point for all queries to which values will be cached here. Queries that don't match this time point are
     * neither cached nor looked up in the cache.
     */
    private final TimePoint timePoint;
    
    /**
     * the leg types at "timePoint"
     */
    final ConcurrentMap<Leg, LegType> legTypeCache;
    
    /**
     * the wind at competitor's position at timePoint; <code>null</code> values are represented by {@link #NULL_WIND}.
     */
    final ConcurrentHashMap<Triple<TrackedRace, Competitor, TimePoint>, Wind> windCache;
    
    /**
     * The average wind in a leg as defined by {@link TrackedLeg#getAverageWind(int)}. See {@link #getWindForLeg(TrackedLeg, Function)}.
     */
    private final ConcurrentHashMap<ORCPerformanceCurveLeg, Wind> trackedLegAverageWindCache;
    
    private static final Wind NULL_WIND = new WindImpl(/* position */ new DegreePosition(0, 0),
            /* time point */ MillisecondsTimePoint.now(), /* windSpeedWithBearing */ new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(0)));
    
    /**
     * the leg's bearing at timePoint; <code>null</code> values are represented by {@link #NULL_BEARING}.
     */
    final ConcurrentHashMap<Leg, Bearing> legBearingCache;

    /**
     * The course for a race ranked by an {@link ORCPerformanceCurveByImpliedWindRankingMetric} metric or subclasses
     * thereof. This course will have any tracked leg adapters replaced by fixed leg descriptions with a fixed TWA and
     * distance as obtained upon the first call for the {@link #timePoint} from the underlying {@link TrackedRace}.
     */
    private final ConcurrentHashMap<TrackedRace, ORCPerformanceCurveCourse> totalCourse;
    
    private final ConcurrentHashMap<Triple<TrackedRace, Waypoint, TimePoint>, Position> approximateWaypointPositions;
    
    /**
     * The scratch boat at {@link #timePoint}, once it has been requested, computed by a supplier that has
     * to be provided to {@link #getScratchBoat(TimePoint, TrackedRace, Supplier)}.
     */
    private final ConcurrentHashMap<Pair<TimePoint, TrackedRace>, Competitor> scratchBoat;
    
    private final ConcurrentHashMap<Triple<TimePoint, TrackedRace, Competitor>, ORCPerformanceCurve> performanceCurvesPerCompetitor;
    
    private final ConcurrentHashMap<Triple<TimePoint, TrackedRace, Competitor>, Speed> impliedWindPerCompetitor;
    
    private final ConcurrentHashMap<Triple<TimePoint, TrackedRace, Competitor>, Duration> relativeCorrectedTimePerCompetitor;
    
    private static final Bearing NULL_BEARING = new DegreeBearingImpl(0);
    
    private static final Speed NULL_IMPLIED_WIND = new KnotSpeedImpl(0);

    private static final Duration NULL_RELATIVE_CORRECTED_TIME = new MillisecondsDurationImpl(0);
    
    private static final ORCPerformanceCurve NULL_PERFORMANCE_CURVE = new ORCPerformanceCurve() {
        @Override
        public Speed getImpliedWind(Duration durationToCompleteCourse)
                throws MaxIterationsExceededException, FunctionEvaluationException {
            return null;
        }

        @Override
        public Duration getCalculatedTime(ORCPerformanceCurve referenceBoat, Duration durationToCompleteCourse)
                throws MaxIterationsExceededException, FunctionEvaluationException {
            return null;
        }

        @Override
        public Duration getAllowancePerCourse(Speed trueWindSpeed) throws ArgumentOutsideDomainException {
            return null;
        }

        @Override
        public ORCPerformanceCurveCourse getCourse() {
            return null;
        }};

    public LeaderboardDTOCalculationReuseCache(TimePoint timePoint) {
        legTypeCache = new ConcurrentHashMap<>();
        windCache = new ConcurrentHashMap<>();
        scratchBoat = new ConcurrentHashMap<>();
        legBearingCache = new ConcurrentHashMap<>();
        trackedLegAverageWindCache = new ConcurrentHashMap<>();
        relativeCorrectedTimePerCompetitor = new ConcurrentHashMap<>();
        approximateWaypointPositions = new ConcurrentHashMap<>();
        this.performanceCurvesPerCompetitor = new ConcurrentHashMap<>();
        this.impliedWindPerCompetitor = new ConcurrentHashMap<>();
        this.totalCourse = new ConcurrentHashMap<>();
        this.timePoint = timePoint;
    }
    
    public LegType getLegType(TrackedLeg trackedLeg, TimePoint timePoint) throws NoWindException {
        LegType result;
        if (Util.equalsWithNull(this.timePoint, timePoint)) {
            result = legTypeCache.get(trackedLeg.getLeg());
            if (result == null) {
                result = trackedLeg.getLegType(timePoint);
                legTypeCache.put(trackedLeg.getLeg(), result);
            }
        } else {
            result = trackedLeg.getLegType(timePoint); // different time point; don't cache
        }
        return result;
    }
    
    public Bearing getLegBearing(TrackedLeg trackedLeg, TimePoint timePoint) {
        Bearing result;
        if (Util.equalsWithNull(this.timePoint, timePoint)) {
            result = legBearingCache.get(trackedLeg.getLeg());
            if (result == null) {
                result = trackedLeg.getLegBearing(timePoint, getMarkPositionAtTimePointCache(timePoint, trackedLeg.getTrackedRace()));
                legBearingCache.put(trackedLeg.getLeg(), result == null ? NULL_BEARING : result);
            } else if (result == NULL_BEARING) {
                result = null;
            }
        } else {
            result = trackedLeg.getLegBearing(timePoint, getMarkPositionAtTimePointCache(timePoint, trackedLeg.getTrackedRace()));
        }
        return result;
    }
    
    @Override
    public Position getApproximatePosition(TrackedRace trackedRace, Waypoint waypoint, TimePoint timePoint) {
        final Triple<TrackedRace, Waypoint, TimePoint> cacheKey = new Triple<>(trackedRace, waypoint, timePoint);
        return approximateWaypointPositions.computeIfAbsent(cacheKey, 
                        // TODO bug5143: is it worth-while to pass through a MarkPositionAtTimePointCache here?
                        key->key.getA().getApproximatePosition(key.getB(), key.getC()));
    }

    /**
     * Determines the wind at the <code>competitor</code>'s {@link GPSFixTrack#getEstimatedPosition(TimePoint, boolean) estimated position} at
     * <code>timePoint</code>. The result is cached for subsequent calls with equal parameters.
     */
    public Wind getWind(TrackedRace trackedRace, Competitor competitor, TimePoint timePoint) {
        Triple<TrackedRace, Competitor, TimePoint> cacheKey = new Triple<>(trackedRace, competitor, timePoint);
        Wind result = windCache.get(cacheKey);
        if (result == null) {
            result = trackedRace.getWind(trackedRace.getTrack(competitor).getEstimatedPosition(timePoint, false), timePoint);
            windCache.put(cacheKey, result == null ? NULL_WIND : result);
        } else if (result == NULL_WIND) {
            result = null;
        }
        return result;
    }

    @Override
    public ORCPerformanceCurveCourse getTotalCourse(TrackedRace raceContext, Supplier<ORCPerformanceCurveCourse> totalCourseSupplier) {
        return totalCourse.computeIfAbsent(raceContext, key->fixORCPerformanceCurveCourse(totalCourseSupplier.get(), /* cache */ this));
    }

    private ORCPerformanceCurveCourse fixORCPerformanceCurveCourse(ORCPerformanceCurveCourse course, ORCPerformanceCurveCache cache) {
        final List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        boolean changed = false;
        for (final ORCPerformanceCurveLeg leg : course.getLegs()) {
            if (leg instanceof AbstractORCPerformanceCurveTwaLegAdapter) {
                final ORCPerformanceCurveLeg pcl;
                if (leg.getType() == ORCPerformanceCurveLegTypes.TWA) {
                    pcl = new ORCPerformanceCurveLegImpl(((AbstractORCPerformanceCurveTwaLegAdapter) leg).getLength(this), leg.getTwa(cache));
                } else {
                    pcl = new ORCPerformanceCurveLegImpl(((AbstractORCPerformanceCurveTwaLegAdapter) leg).getLength(this), leg.getType());
                }
                legs.add(pcl);
                changed = true;
            } else {
                legs.add(leg);
            }
        }
        final ORCPerformanceCurveCourse result;
        if (changed) {
            result = new ORCPerformanceCurveCourseImpl(legs);
        } else {
            result = course;
        }
        return result;
    }
    
    @Override
    public Competitor getScratchBoat(TimePoint timePoint, TrackedRace raceContext, Function<TimePoint, Competitor> scratchBoatSupplier) {
        return scratchBoat.computeIfAbsent(new Pair<>(timePoint, raceContext), timePointAndRaceContext->scratchBoatSupplier.apply(timePointAndRaceContext.getA()));
    }

    @Override
    public ORCPerformanceCurve getPerformanceCurveForPartialCourse(TimePoint timePoint,
            TrackedRace raceContext, Competitor competitor, BiFunction<TimePoint, Competitor, ORCPerformanceCurve> performanceCurveSupplier) {
        final ORCPerformanceCurve result = performanceCurvesPerCompetitor.computeIfAbsent(
                new Triple<>(timePoint, raceContext, competitor), timePointAndTrackedRaceAndCompetitor -> {
                    final ORCPerformanceCurve performanceCurve = performanceCurveSupplier.apply(
                            timePointAndTrackedRaceAndCompetitor.getA(), timePointAndTrackedRaceAndCompetitor.getC());
                    return performanceCurve == null ? NULL_PERFORMANCE_CURVE : performanceCurve;
                });
        return result == NULL_PERFORMANCE_CURVE ? null : result;
    }

    @Override
    public Speed getImpliedWind(TimePoint timePoint, TrackedRace raceContext, Competitor competitor, BiFunction<TimePoint, Competitor, Speed> impliedWindSupplier) {
        final Speed result = impliedWindPerCompetitor.computeIfAbsent(new Triple<>(timePoint, raceContext, competitor),
                timePointAndTrackedRaceAndCompetitor -> {
                    final Speed impliedWind = impliedWindSupplier.apply(timePointAndTrackedRaceAndCompetitor.getA(),
                            timePointAndTrackedRaceAndCompetitor.getC());
                    return impliedWind == null ? NULL_IMPLIED_WIND : impliedWind;
                });
        return result == NULL_IMPLIED_WIND ? null : result;
    }

    @Override
    public Duration getRelativeCorrectedTime(Competitor competitor, TrackedRace raceContext,
            TimePoint timePoint, BiFunction<Competitor, TimePoint, Duration> relativeCorrectedTimeSupplier) {
        final Duration result = relativeCorrectedTimePerCompetitor.computeIfAbsent(
                new Triple<>(timePoint, raceContext, competitor), timePointAndTrackedRaceAndCompetitor -> {
                    final Duration relativeCorrectedTime = relativeCorrectedTimeSupplier.apply(
                            timePointAndTrackedRaceAndCompetitor.getC(), timePointAndTrackedRaceAndCompetitor.getA());
                    return relativeCorrectedTime == null ? NULL_RELATIVE_CORRECTED_TIME : relativeCorrectedTime;
                });
        return result == NULL_RELATIVE_CORRECTED_TIME ? null : result;
    }

    @Override
    public MarkPositionAtTimePointCache getMarkPositionAtTimePointCache(final TimePoint markPositionTimePoint, final TrackedRace trackedRace) {
        return new MarkPositionAtTimePointCache() {
            @Override
            public Position getEstimatedPosition(Mark mark) {
                return getTrackedRace().getOrCreateTrack(mark).getEstimatedPosition(markPositionTimePoint, /* extrapolate */ false);
            }

            @Override
            public Position getApproximatePosition(Waypoint waypoint) {
                return LeaderboardDTOCalculationReuseCache.this.getApproximatePosition(getTrackedRace(), waypoint, markPositionTimePoint);
            }

            @Override
            public Bearing getLegBearing(TrackedLeg trackedLeg) {
                return LeaderboardDTOCalculationReuseCache.this.getLegBearing(trackedLeg, markPositionTimePoint);
            }

            @Override
            public TimePoint getTimePoint() {
                return markPositionTimePoint;
            }

            @Override
            public TrackedRace getTrackedRace() {
                return trackedRace;
            }
        };
    }

    @Override
    public <L extends ORCPerformanceCurveLeg> Wind getAverageWind(L leg,
            Function<L, Wind> averageWindForLegSupplier) {
        Wind result = trackedLegAverageWindCache.get(leg);
        if (result == null) {
            result = averageWindForLegSupplier.apply(leg);
            if (result != null) {
                trackedLegAverageWindCache.put(leg, result);
            }
        }
        return result;
    }
}
