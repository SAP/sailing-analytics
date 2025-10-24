package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.TerminalHasTrackedLegSliceOfCompetitorContext;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;

/**
 * Equality is based on the {@link #getTrackedLegOfCompetitor()} only. The {@link #getSliceNumber()} defaults to {@code 1} for this
 * special case of the entire leg. Start and finish times of this "slice" equal the start/finish times of the competitor in the leg.
 */
public class TerminalTrackedLegOfCompetitorWithContext extends TrackedLegOfCompetitorWithContext implements TerminalHasTrackedLegSliceOfCompetitorContext {
    private static final long serialVersionUID = 6464926225236850592L;
    private Wind wind;

    public TerminalTrackedLegOfCompetitorWithContext(HasTrackedLegContext trackedLegContext, TrackedLegOfCompetitor trackedLegOfCompetitor, TackTypeSegmentsDataMiningSettings settings) {
        super(trackedLegContext, trackedLegOfCompetitor, settings);
    }

    @Override
    public Wind getWindInternal() {
        return wind;
    }

    @Override
    public void setWindInternal(Wind wind) {
        this.wind = wind;
    }
}
