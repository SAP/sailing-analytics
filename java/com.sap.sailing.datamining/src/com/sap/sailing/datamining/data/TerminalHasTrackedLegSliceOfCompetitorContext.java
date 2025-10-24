package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * A terminal type on a retriever chain that is especially not use to extend the chain leading to other types that
 * implement {@link HasWindOnTrackedLegOfCompetitor}. This would otherwise duplicate the wind-based dimensions where the
 * finer-grained objects reached at the end of the chain should know better about their position, time point and wind
 * than the leg as such.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface TerminalHasTrackedLegSliceOfCompetitorContext
        extends HasTrackedLegOfCompetitorContext, HasWindOnTrackedLegOfCompetitor {
    @Override
    default TrackedRace getTrackedRace() {
        return HasTrackedLegOfCompetitorContext.super.getTrackedRace();
    }

    @Override
    default HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext() {
        return this;
    }
}
