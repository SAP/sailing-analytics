package com.sap.sailing.domain.orc.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.orc.AverageWindOnLegCache;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.NoCachingWindLegTypeAndLegBearingCache;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Abstract base class for adapting a {@link TrackedLeg} to the {@link ORCPerformanceCurveLeg} interface. If wind
 * information is known, the leg will be of type {@link ORCPerformanceCurveLegTypes#TWA} using as the {@link #getTwa()
 * TWA} the angular difference between this leg's bearing and the TWA as obtained from the
 * {@link TrackedRace#getWind(com.sap.sse.common.Position, TimePoint) TrackedRace} of which the
 * {@link TrackedLeg} passed to the constructor is a part. If no wind information is known, the leg is emulated to be of
 * type {@link ORCPerformanceCurveLegTypes#LONG_DISTANCE}, and {@link #getTwa()} will return {@code null}.
 * <p>
 * 
 * Subclasses have to define the {@link #getLength()} method which could, e.g., return a constant {@link Distance}
 * obtained from some other definition, or the tracked windward distance of the leg.
 */
public abstract class AbstractORCPerformanceCurveTwaLegAdapter implements ORCPerformanceCurveLeg {
    private static final long serialVersionUID = -6432064480098807397L;
    private static final int DEFAULT_NUMBER_OF_TIME_POINTS_AND_POSITIONS_FOR_AVERAGE_WIND = 10;
    private final TrackedLeg trackedLeg;
    private final int numParts;
    
    /**
     * A wrapper around the enclosing object that delegates all methods to the enclosing instance but defines equality /
     * hash code based on the {@link AbstractORCPerformanceCurveTwaLegAdapter#getTrackedLeg()} result only.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class EqualityByTrackedLegWrapper implements ORCPerformanceCurveLeg {
        private static final long serialVersionUID = 2580336707637358724L;

        @Override
        public boolean equals(Object o) {
            return o instanceof EqualityByTrackedLegWrapper && getTrackedLeg() == ((EqualityByTrackedLegWrapper) o).getTrackedLeg();
        }
        
        @Override
        public int hashCode() {
            return getTrackedLeg().hashCode();
        }
        
        @Override
        public Distance getLength() {
            return AbstractORCPerformanceCurveTwaLegAdapter.this.getLength();
        }

        @Override
        public Bearing getTwa() {
            return AbstractORCPerformanceCurveTwaLegAdapter.this.getTwa();
        }

        @Override
        public Bearing getTwa(AverageWindOnLegCache cache) {
            return AbstractORCPerformanceCurveTwaLegAdapter.this.getTwa(cache);
        }

        @Override
        public ORCPerformanceCurveLegTypes getType() {
            return AbstractORCPerformanceCurveTwaLegAdapter.this.getType();
        }

        @Override
        public ORCPerformanceCurveLeg scale(double share) {
            return AbstractORCPerformanceCurveTwaLegAdapter.this.scale(share);
        }
        
        public TrackedLeg getTrackedLeg() {
            return AbstractORCPerformanceCurveTwaLegAdapter.this.getTrackedLeg();
        }
    }
    
    public AbstractORCPerformanceCurveTwaLegAdapter(TrackedLeg trackedLeg) {
        this(trackedLeg, DEFAULT_NUMBER_OF_TIME_POINTS_AND_POSITIONS_FOR_AVERAGE_WIND);
    }
    
    public AbstractORCPerformanceCurveTwaLegAdapter(TrackedLeg trackedLeg, int numParts) {
        this.trackedLeg = trackedLeg;
        this.numParts = numParts;
    }
    
    protected TrackedLeg getTrackedLeg() {
        return trackedLeg;
    }

    @Override
    public Bearing getTwa() {
        return getTwa(new NoCachingWindLegTypeAndLegBearingCache());
    }
    
    @Override
    public Bearing getTwa(AverageWindOnLegCache cache) {
        final EqualityByTrackedLegWrapper wrapper = new EqualityByTrackedLegWrapper();
        final Wind wind = cache.getAverageWind(wrapper, legAdapter->{
            final WindWithConfidence<Pair<Position, TimePoint>> averageWind = legAdapter.getTrackedLeg().getAverageWind(numParts);
            return averageWind == null ? null : averageWind.getObject();
        });
        final Bearing result;
        if (wind == null) {
            result = null;
        } else {
            final TimePoint referenceTimePoint = trackedLeg.getReferenceTimePoint();
            final Bearing bearing = trackedLeg.getLegBearing(referenceTimePoint);
            result = bearing != null ? bearing.getDifferenceTo(wind.getFrom()) : null;
        }
        return result;
    }

    @Override
    public ORCPerformanceCurveLegTypes getType() {
        final ORCPerformanceCurveLegTypes result;
        if (!hasWind()) {
            result = ORCPerformanceCurveLegTypes.LONG_DISTANCE;
        } else {
            result = ORCPerformanceCurveLegTypes.TWA;
        }
        return result;
    }

    private boolean hasWind() {
        final TimePoint referenceTimePoint = trackedLeg.getReferenceTimePoint();
        final Wind result = trackedLeg.getTrackedRace().getWind(trackedLeg.getMiddleOfLeg(referenceTimePoint), referenceTimePoint);
        return result != null;
    }

    /**
     * The TWA calculation must not be affected by scaling a leg because otherwise competitors who sailed different
     * ratios of the same leg may get different {@link #getWind()} results.
     */
    @Override
    public ORCPerformanceCurveLeg scale(final double share) {
        return new AbstractORCPerformanceCurveTwaLegAdapter(trackedLeg, numParts) {
            private static final long serialVersionUID = -6724721873285438431L;

            @Override
            public Distance getLength() {
                return AbstractORCPerformanceCurveTwaLegAdapter.this.getLength().scale(share);
            }

            @Override
            public Distance getLength(
                    WindLegTypeAndLegBearingAndORCPerformanceCurveCache leaderboardDTOCalculationReuseCache) {
                return AbstractORCPerformanceCurveTwaLegAdapter.this.getLength(leaderboardDTOCalculationReuseCache).scale(share);
            }
        };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" for "+trackedLeg.getLeg()+": length="+getLength().getNauticalMiles()+"NM, TWA="+getTwa();
    }

    public abstract Distance getLength(WindLegTypeAndLegBearingAndORCPerformanceCurveCache leaderboardDTOCalculationReuseCache);
}
