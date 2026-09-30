package com.sap.sailing.domain.tracking.impl;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.orc.ORCPerformanceCurve;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * This trivial "cache" implementation doesn't cache and may be used as a default for those cases where only few
 * calculations are to be done and creating the caching structure would cost more cycles than simply doing the
 * calculation.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class NoCachingWindLegTypeAndLegBearingCache implements WindLegTypeAndLegBearingAndORCPerformanceCurveCache {
    @Override
    public Wind getWind(TrackedRace trackedRace, Competitor competitor, TimePoint timePoint) {
        return trackedRace.getWind(trackedRace.getTrack(competitor).getEstimatedPosition(timePoint, false), timePoint);
    }

    @Override
    public LegType getLegType(TrackedLeg trackedLeg, TimePoint timePoint) throws NoWindException {
        return trackedLeg.getLegType(timePoint);
    }

    @Override
    public Bearing getLegBearing(TrackedLeg trackedLeg, TimePoint timePoint) {
        return trackedLeg.getLegBearing(timePoint);
    }

    @Override
    public ORCPerformanceCurveCourse getTotalCourse(TrackedRace raceContext, Supplier<ORCPerformanceCurveCourse> totalCourseSupplier) {
        return totalCourseSupplier.get();
    }

    @Override
    public Competitor getScratchBoat(TimePoint timePoint, TrackedRace raceContext, Function<TimePoint, Competitor> scratchBoatSupplier) {
        return scratchBoatSupplier.apply(timePoint);
    }

    @Override
    public ORCPerformanceCurve getPerformanceCurveForPartialCourse(TimePoint timePoint,
            TrackedRace raceContext, Competitor competitor, BiFunction<TimePoint, Competitor, ORCPerformanceCurve> performanceCurveSupplier) {
        return performanceCurveSupplier.apply(timePoint, competitor);
    }

    @Override
    public Speed getImpliedWind(TimePoint timePoint, TrackedRace raceContext, Competitor competitor, BiFunction<TimePoint, Competitor, Speed> impliedWindSupplier) {
        return impliedWindSupplier.apply(timePoint, competitor);
    }

    @Override
    public Duration getRelativeCorrectedTime(Competitor competitor, TrackedRace raceContext, TimePoint timePoint,
            BiFunction<Competitor, TimePoint, Duration> relativeCorrectedTimeSupplier) {
        return relativeCorrectedTimeSupplier.apply(competitor, timePoint);
    }

    @Override
    public Position getApproximatePosition(TrackedRace trackedRace, Waypoint waypoint, TimePoint timePoint) {
        return trackedRace.getApproximatePosition(waypoint, timePoint);
    }

    @Override
    public MarkPositionAtTimePointCache getMarkPositionAtTimePointCache(TimePoint markPositionTimePoint, TrackedRace trackedRace) {
        return new MarkPositionAtTimePointCacheImpl(trackedRace, markPositionTimePoint);
    }

    @Override
    public <L extends ORCPerformanceCurveLeg> Wind getAverageWind(L leg, Function<L, Wind> averageWindForLegSupplier) {
        return averageWindForLegSupplier.apply(leg);
    }
}
