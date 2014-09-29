package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.datamining.data.HasGPSFixContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.impl.data.GPSFixWithContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractSimpleRetrievalProcessor;
import com.sap.sse.datamining.shared.annotations.DataRetriever;

@DataRetriever(dataType=HasGPSFixContext.class,
               groupName=Activator.dataRetrieverGroupName,
               level=5)
public class GPSFixRetrievalProcessor extends AbstractSimpleRetrievalProcessor<HasTrackedLegOfCompetitorContext, HasGPSFixContext> {

    public GPSFixRetrievalProcessor(ExecutorService executor, Collection<Processor<HasGPSFixContext>> resultReceivers) {
        super(HasTrackedLegOfCompetitorContext.class, executor, resultReceivers);
    }

    @Override
    protected Iterable<HasGPSFixContext> retrieveData(HasTrackedLegOfCompetitorContext element) {
        Collection<HasGPSFixContext> gpsFixesWithContext = new ArrayList<>();
        GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = element.getTrackedLegContext().getTrackedRaceContext().getTrackedRace().getTrack(element.getCompetitor());
        competitorTrack.lockForRead();
        try {
            TrackedLegOfCompetitor trackedLegOfCompetitor = element.getTrackedLegOfCompetitor();
            if (trackedLegOfCompetitor.getStartTime() != null && trackedLegOfCompetitor.getFinishTime() != null) {
                for (GPSFixMoving gpsFix : competitorTrack.getFixes(trackedLegOfCompetitor.getStartTime(), true, trackedLegOfCompetitor.getFinishTime(), true)) {
                    HasGPSFixContext gpsFixWithContext = new GPSFixWithContext(element, gpsFix);
                    gpsFixesWithContext.add(gpsFixWithContext);
                }
            }
        } finally {
            competitorTrack.unlockAfterRead();
        }
        return gpsFixesWithContext;
    }

}
