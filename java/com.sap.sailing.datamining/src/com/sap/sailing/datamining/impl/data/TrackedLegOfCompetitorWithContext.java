package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTackTypeSegmentContext;
import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Equality is based on the {@link #getTrackedLegOfCompetitor()} only. The {@link #getSliceNumber()} defaults to {@code 1} for this
 * special case of the entire leg. Start and finish times of this "slice" equal the start/finish times of the competitor in the leg.
 */
public class TrackedLegOfCompetitorWithContext extends AbstractTrackedLegSliceOfCompetitorWithContext implements HasTrackedLegOfCompetitorContext {
    private static final long serialVersionUID = 3966524650275310718L;

    public TrackedLegOfCompetitorWithContext(HasTrackedLegContext trackedLegContext, TrackedLegOfCompetitor trackedLegOfCompetitor, TackTypeSegmentsDataMiningSettings settings) {
        super(trackedLegContext, trackedLegOfCompetitor, settings, 1);
    }
    
    protected TimePoint getSliceStartTime() {
        return getTrackedLegOfCompetitor().getStartTime();
    }
    
    protected TimePoint getSliceFinishTime() {
        return getEffectiveEndOfLeg(getTrackedLegOfCompetitor().getFinishTime());
    }

    /**
     * For the whole leg the distance traveled can be calculated more easily, without subtracting the distance traveled in the leg from
     * the beginning of the slice.
     */
    @Override
    public Distance getDistanceTraveled() {
        return getSliceStartTime() == null ? null : getTrackedLegOfCompetitor().getDistanceTraveled(getSliceFinishTime());
    }
    
    /**
     * Can use {@link TrackedLegOfCompetitor#getAverageSpeedOverGround(TimePoint)} which computes efficiently
     * from the competitor's leg start time point.
     */
    @Override
    public Double getSpeedAverageInKnots() {
        final Double result;
        if (getTrackedRace() == null) {
            result = null;
        } else {
            if (getSliceStartTime() == null) {
                result = null;
            } else {
                final Speed averageSOG = getTrackedLegOfCompetitor().getAverageSpeedOverGround(getSliceFinishTime());
                result = averageSOG == null ? null : averageSOG.getKnots();
            }
        }
        return result;
    }

    /**
     * Shortcut for full leg: only the tack type segment's {@link HasTackTypeSegmentContext#getLegNumber() leg number}
     * must match this leg's leg number.
     */
    @Override
    protected boolean isTackTypeSegmentIntersectingWithLegSlice(HasTackTypeSegmentContext tackTypeSegmentContext) {
        return tackTypeSegmentContext.getLegNumber() == getTrackedLegContext().getLegNumber();
    }
}
