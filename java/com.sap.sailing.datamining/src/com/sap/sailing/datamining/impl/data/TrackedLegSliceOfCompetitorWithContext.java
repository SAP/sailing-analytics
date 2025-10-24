package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.HasTrackedLegSliceOfCompetitorContext;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;

/**
 * Equality is based on the {@link #getTrackedLegOfCompetitor()} and the {@link #sliceNumber} only. The current slicing implementation
 * uses the time sailed in the leg as the basis for slicing. Other slicing criteria, such as windward distance traveled, would be much
 * harder to implement because we would need to find out iteratively, between which fixes the competitor sailed, say, one tenth of the
 * windward distance in the leg.
 */
public class TrackedLegSliceOfCompetitorWithContext extends TerminalAbstractTrackedLegSliceOfCompetitorWithContext implements HasTrackedLegSliceOfCompetitorContext {
    private static final long serialVersionUID = -8116119441445200029L;

    public TrackedLegSliceOfCompetitorWithContext(HasTrackedLegContext trackedLegContext, TrackedLegOfCompetitor trackedLegOfCompetitor, TackTypeSegmentsDataMiningSettings settings, int sliceNumber) {
        super(trackedLegContext, trackedLegOfCompetitor, settings, sliceNumber);
    }

    @Override
    public Integer getSliceNumber() {
        return super.getTheSliceNumber();
    }
}
