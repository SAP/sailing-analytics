package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.datamining.data.HasBravoFixContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.impl.data.BravoFixWithContext;
import com.sap.sailing.datamining.impl.data.TrackedLegOfCompetitorWithSpecificTimePointWithContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;

public class BravoFixRetrievalProcessor extends AbstractRetrievalProcessor<HasTrackedLegOfCompetitorContext, HasBravoFixContext> {

    public BravoFixRetrievalProcessor(ExecutorService executor, Collection<Processor<HasBravoFixContext, ?>> resultReceivers, int retrievalLevel) {
        super(HasTrackedLegOfCompetitorContext.class, HasBravoFixContext.class, executor, resultReceivers, retrievalLevel);
    }

    @Override
    protected Iterable<HasBravoFixContext> retrieveData(HasTrackedLegOfCompetitorContext element) {
        Collection<HasBravoFixContext> bravoFixesWithContext = new ArrayList<>();
        BravoFixTrack<Competitor> bravoFixTrack = element.getTrackedLegContext().getTrackedRaceContext().getTrackedRace().getSensorTrack(element.getCompetitor(), BravoFixTrack.TRACK_NAME);
        if (bravoFixTrack != null) {
            bravoFixTrack.lockForRead();
            try {
                TrackedLegOfCompetitor trackedLegOfCompetitor = element.getTrackedLegOfCompetitor();
                if (trackedLegOfCompetitor.getStartTime() != null && trackedLegOfCompetitor.getFinishTime() != null) {
                    for (BravoFix bravoFix : bravoFixTrack.getFixes(trackedLegOfCompetitor.getStartTime(), true, trackedLegOfCompetitor.getFinishTime(), true)) {
                        BravoFixWithContext gpsFixWithContext = new BravoFixWithContext(
                                new TrackedLegOfCompetitorWithSpecificTimePointWithContext(
                                        element.getTrackedLegContext(), element.getTrackedLegOfCompetitor(), bravoFix.getTimePoint()), bravoFix);
                        bravoFixesWithContext.add(gpsFixWithContext);
                    }
                }
            } finally {
                bravoFixTrack.unlockAfterRead();
            }
        }
        return bravoFixesWithContext;
    }

}
