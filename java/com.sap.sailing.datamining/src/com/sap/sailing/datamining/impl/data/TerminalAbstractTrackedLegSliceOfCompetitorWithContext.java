package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.data.TerminalHasTrackedLegSliceOfCompetitorContext;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;

/**
 * Abstract base class of those tracked legs of competitor in Data Mining that are returned as terminal
 * objects of a retriever chain and are not extended for a drill-down to more fine-grained objects. This
 * allows us to specify average wind, position and time point data for the competitor's leg and not duplicate
 * those dimensions for subordinate fine-grained objects such as GPS fixes or maneuvers.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class TerminalAbstractTrackedLegSliceOfCompetitorWithContext extends
        AbstractTrackedLegSliceOfCompetitorWithContext implements TerminalHasTrackedLegSliceOfCompetitorContext {
    private static final long serialVersionUID = 8362729373776291159L;
    private Wind wind;

    public TerminalAbstractTrackedLegSliceOfCompetitorWithContext(HasTrackedLegContext trackedLegContext, TrackedLegOfCompetitor trackedLegOfCompetitor, TackTypeSegmentsDataMiningSettings settings, int sliceNumber) {
        super(trackedLegContext, trackedLegOfCompetitor, settings, sliceNumber);
    }
    
    @Override
    public HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext() {
        return this;
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
